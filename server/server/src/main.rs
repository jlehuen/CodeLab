// ============================================================================
// Fichier : main.rs
// Version : 13/09/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================

// Activer / désactiver les inlays de Rust Analyser :
// Code > Preferences > Settings > Rust Analyser Inlay Hints > Off

mod hook; use hook::*;
mod utils; use utils::*;
mod mails; use mails::*;
mod admin; use admin::*;
mod constants; use constants::*;
mod properties; use properties::*;
mod client; use client::*;
mod backups; use backups::*;
mod handler; use handler::*;
mod database; use database::*;
mod connected; use connected::*;
mod controlled; use controlled::remove_all_controlled_by_tutor;
mod messages;

use std::{
    error::Error,
    io::ErrorKind,
    process::exit,
};

use tokio::net::TcpListener;
use tokio::time::{sleep, Duration};

// Tokio réalise la gestion asynchrone des E/S ce qui permet au serveur
// de traiter efficacement un grand nombre de connexions simultanées.

// Tokio en mode multi-threadé crée un pool de threads proportionnel au
// nombre de coeurs CPU disponibles sur la machine et permet aux threads
// de "voler" des tâches à d'autres threads lorsque leur propre file
// d'attente est vide, maximisant ainsi l'utilisation des threads.

type EmptyResult = Result<(), Box<dyn Error>>;

// ----------------------------------------------------------------------------
// Fonction principale
// ----------------------------------------------------------------------------

#[tokio::main]
async fn main() -> EmptyResult {

    // Intercepter les signaux POSIX
    configure_hook();

    // Consolider les dossiers
    ensure_dir_exists(PROG_DIR)?;
    ensure_dir_exists(TEMP_DIR)?;
    ensure_dir_exists(LOG_DIR)?;

    // Initialiser le logger
    if let Err(e) = configure_logger(LOG_FILE) {
        log::error!("FATAL ERROR: Logger configuration error: {:?}", e);
        exit(2); // Quitte le programme avec un code d'erreur
    }
    log::info!("Initializing server...");

    // Charger les propriétés (avec repli sur .example si absent)
    let config_path = if std::path::Path::new(CONFIG_FILE).exists() {
        CONFIG_FILE
    } else if std::path::Path::new(CONFIG_FILE_EXAMPLE).exists() {
        log::warn!("WARNING: {} not found. Falling back to example config: {}", CONFIG_FILE, CONFIG_FILE_EXAMPLE);
        CONFIG_FILE_EXAMPLE
    } else {
        CONFIG_FILE
    };

    if let Err(e) = load_properties(config_path) {
        log::error!("FATAL ERROR: Error while loading config file ({}): {:?}", config_path, e);
        exit(1); // Quitte le programme avec un code d'erreur
    }
    print_properties();

    // Charger la base XML (avec repli sur .example si absent)
    let xml_path = if std::path::Path::new(CONFIG_XML).exists() {
        CONFIG_XML
    } else if std::path::Path::new(CONFIG_XML_EXAMPLE).exists() {
        log::warn!("WARNING: {} not found. Falling back to example XML: {}", CONFIG_XML, CONFIG_XML_EXAMPLE);
        CONFIG_XML_EXAMPLE
    } else {
        CONFIG_XML
    };

    match load_database(xml_path) {
        Ok(()) => {
            print_database();
            consolidate_tree(PROG_DIR)?;
        },
        Err(e) => {
            log::error!("FATAL ERROR: Error while loading XML database ({}): {:?}", xml_path, e);
            exit(3); // Quitte le programme avec un code d'erreur
        },
    }

    // Tâche de nettoyage périodique des connexions fantômes
    tokio::spawn(async move {
        let cleanup_interval = get_property_duration(CLEANUP_PHANTOM_INTERVAL);
        let mut interval = tokio::time::interval(cleanup_interval);
        log::info!("Phantom connections cleanup task started");
        loop {
            interval.tick().await;
            if has_connections().await {
                cleanup_phantom_connections().await;
            }
        }
    });

    // Démarrer le serveur admin HTTP
    let admin_server = AdminServer::new();
    let mut admin_shutdown_rx = AdminServer::get_shutdown_receiver();
    let admin_handle = tokio::spawn(async move {
        if let Err(e) = admin_server.start().await {
            log::error!("ERROR: Error while starting the HTTP server: {:?}", e);
        }
    });

    // Créer un listener TCP pour le serveur
    let server_addr = get_property(SERVER_ADDR);
    let listener = TcpListener::bind(&server_addr).await?;
    log::info!("Server listening on {}", server_addr);

    // Envoyer un email de notification
    let message = format!("The server is listening on {}", server_addr);
    notification_email("The server is started", &message);
    
    // -----------------------------------------------
    // Boucle principale avec gestion du shutdown
    // -----------------------------------------------

    loop {
        tokio::select! {
            // Attendre une nouvelle connexion client
            result = listener.accept() => {
                match result {
                    Ok((socket, addr)) => {
                        log::info!("New connection from {}", addr);

                        // Création d'une structure Client incomplète
                        let client = Client::new(addr, socket);

                        // Gérer la nouvelle connexion dans une tâche asynchrone
                        tokio::spawn(async move {
                            if let Err(e) = client_handle(client).await {
                                log::info!("Connection lost with {}: {:?}", addr, e);
                            }
                        });
                    }
                    Err(e) => {
                        log::error!("Error accepting connection: {:?}", e);
                    }
                }
            }

            // Attendre un signal de shutdown depuis l'interface admin
            _ = admin_shutdown_rx.recv() => {
                log::warn!("Shutdown signal received from admin interface");
                break;
            }
        }
    }

    // -----------------------------------------------
    // Procédure d'arrêt propre
    // -----------------------------------------------
    
    log::info!("Server shutdown");

    // Arrêter le serveur d'administration
    admin_handle.abort();

    // Déconnecter tous les clients
    log::info!("Disconnecting all clients");
    disconnect_all_clients().await;

    // Attendre un peu pour que les clients se déconnectent proprement
    tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;

    // Sauvegarder la base de données
    log::info!("Saving database");
    save_database(CONFIG_XML);

    // Envoyer un email de notification
    notification_email("The server is stopped", "");

    log::info!("Server stopped gracefully");
    Ok(())
}

// ----------------------------------------------------------------------------
// Gestionnaire de connexion client
// ----------------------------------------------------------------------------

define!(
    PING => "PING",
    START => "START",
    ASK_LOGIN => "?LOGIN",
    ASK_PASSWD => "?PASSWD",
    ASK_NEWPASS => "?NEWPASS",
    ASK_SESSION => "?SESSION:",
    CONNECTION_OK => "CONNECTION_OK",
    EJECTION => "EJECTION",
    
    ERROR_LOGIN => "ERROR_LOGIN",
    ERROR_PASSWD => "ERROR_PASSWD",
    ERROR_PROTOCOL => "ERROR_PROTOCOL",
    ERROR_NOSESSION => "ERROR_NOSESSION",
    ERROR_CONNECTED => "ERROR_CONNECTED"
);

async fn client_handle(mut client: Client) -> EmptyResult {

    let addr = client.get_addr();
    let msg = client.read_string().await?;

    // -----------------------------------------------
    // Juste un test de connectivité
    // -----------------------------------------------

    if msg == PING {
        log::info!("Ping test from {}", addr);
        return Ok(()); // Terminer après le test
    }

    // -----------------------------------------------
    // Initialisation du protocole par le client
    // -----------------------------------------------

    if msg != START {
        // Erreur de protocole de la part du client
        log::info!("ERROR: Protocol error for {}", addr);
        client.send_string(ERROR_PROTOCOL).await?;
        return Ok(()); // Terminer en cas d'erreur
    }

    // -----------------------------------------------
    // Vérification du login
    // -----------------------------------------------

    log::info!("Asking {} for login...", addr);
    let login = client.send_and_receive(ASK_LOGIN).await?;
    if !check_login(&login) {
        // Le login envoyé par le client est inconnu
        log::info!("ERROR: Unknown login {} for {}", login, addr);
        client.send_string(ERROR_LOGIN).await?;
        return Ok(()); // Terminer en cas d'erreur
    }
    log::info!("Login {} OK for {}", login, addr);
    client.set_login(login.clone()); // Compléter la structure Client

    // -----------------------------------------------
    // Vérification du password
    // -----------------------------------------------
    
    log::info!("Asking {} for password...", login);
    let passwd = client.send_and_receive(ASK_PASSWD).await?;

    if !check_password(&login, &passwd) {
        // Password incorrect
        log::info!("ERROR: Wrong password for {}", login);
        client.send_string(ERROR_PASSWD).await?;
        return Ok(()); // Terminer en cas d'erreur
    }
    log::info!("Password OK for {}", login);

    if first_connection(&login) {
        // Première connexion avec ce login
        log::info!("Asking {} for new password...", login);
        let new_passwd = client.send_and_receive(ASK_NEWPASS).await?;
        client.set_password(new_passwd); // Compléter la structure Client
        // Petite pause pour s'assurer que le changement est propagé (bof)
        tokio::time::sleep(Duration::from_millis(100)).await;
        log::info!("Password updated for {}", login);
    }

    // -----------------------------------------------
    // Vérification des connexions multiples
    // -----------------------------------------------

    if is_connected(&login).await {
        log::info!("ERROR: Connection existing for {}", login);
        let response = client.send_and_receive(ERROR_CONNECTED).await?;
        if response == EJECTION {
            log::info!("Disconnect {}", login);
            force_disconnect(login.as_str()).await;
        }
        return Ok(()); // Toujours terminer dans ce cas
    }

    // -----------------------------------------------
    // Choix de la session
    // -----------------------------------------------

    // Récupération des sessions du client
    let sessions = get_sessions(&login);

    if sessions.is_empty() {
        log::info!("ERROR: no session available for client {}", login);
        client.send_string(ERROR_NOSESSION).await?;
        return Ok(()); // Pas de return Err() dans ce cas
    }

    // Demander au client de choisir une session
    log::info!("Asking {} for session...", login);
    let message = format!("{}{}", ASK_SESSION, sessions);
    let session = client.send_and_receive(&message).await?;
    // Enlever la balise CLOSED_TAG si elle est présente
    let session = session.strip_suffix(CLOSED_TAG).unwrap_or(&session);

    client.set_session(session.to_string()); // Compléter la structure Client
    client.send_string(CONNECTION_OK).await?; // Fin du dialogue synchrone
    sleep(Duration::from_millis(500)).await; // Petite pause pour le client
    client.init_session(&session).await?; // Envoyer au client sa session
    client.notify_tutors_client_arrived().await?; // Informer les tuteurs présents
    add_client(client.clone(), session).await; // Ajouter le client à la table des connectés
    update_clientdata(&login, &session, &addr); // Actualiser la base de données

    //describe_all_clients_to_log().await;

    // -----------------------------------------------
    // Boucle d'écoute
    // -----------------------------------------------

    // La macro tokio::select! surveille à la fois:
    // - La lecture des données sur le socket => socket.read(&mut buffer)
    // - La réception d'un signal de shutdown => shutdown_rx.recv()
    // Documentation : https://tokio.rs/tokio/tutorial/select

    // Il est impératif de sortir de la boucle normalement si une erreur
    // survient pour atteindre le protocole de fin de session: fermeture
    // du socket et retrait du client de la liste des clients connectés.

    let mut shutdown_rx = client.get_shutdown_rx();

    loop {
        tokio::select! {

            // -----------------------------------------------
            // Attendre un message du client
            // -----------------------------------------------

            result = client.wait_message() => {
                match result {
                    Ok(message) => {
                        // La fonction handle_message() est dans handler.rs
                        if let Err(e) = handle_message(&mut client, message).await {
                            log::error!("ERROR: Failed to handle client message from {}: {}", login, e);
                            // On ne sort pas de la boucle dans ce cas
                        }
                    }
                    Err(e) => {
                        if e.kind() == ErrorKind::UnexpectedEof {
                            // UnexpectedEof en cas de déconnexion du client
                            break; // Sortir de la boucle (cf. note)
                        } else {
                            log::error!("ERROR: While reading the message from {}: {}", login, e);
                            break; // Sortir de la boucle (cf. note)
                        }
                    }
                }
            }

            // -----------------------------------------------
            // Attendre un signal de shutdown
            // -----------------------------------------------

            _ = shutdown_rx.recv() => {
                log::info!("Shutdown signal received for client {}", login);
                break; // Sortir de la boucle (cf. note)
            }
        }
    }

    // -----------------------------------------------
    // Protocole de fin de session
    // -----------------------------------------------

    // TODO: client.update_worktime().await; // Mettre à jour le temps de travail du client

    let duration = client.connection_duration();
    log::info!("The client {} has been disconnected after {} ", login, duration);

    remove_client(&login).await; // Retirer le client de la table des connectés

    if client.is_tutor() {
        remove_all_controlled_by_tutor(&login).await; // Retirer les éventuels clients contrôlés
    }

    client.notify_tutors_client_exited().await?; // Informer les tuteurs de la session

    // Fermer la connexion proprement
    if let Err(_) = client.shutdown_socket().await {
        // Si le client est déjà déconnecté on obtient
        // un Socket is not connected (os error 57)
    }

    if client.is_student() {
        // Nettoyer ses fichiers de backup datés
        let user_dir = format!("{}/{}/{}", PROG_DIR, session, login);
        log::warn!("Cleaning backups from {}", user_dir);
        if let Err(e) = clean_backups(&user_dir) {
            log::warn!("Failed to clean backups for {}: {}", login, e);
        }
    }
    Ok(())
}

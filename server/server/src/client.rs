// ============================================================================
// Fichier : client.rs
// Version : 13/09/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Ce module définit la structure Client qui représente un client connecté
// au serveur. La structure Client contient les informations de connexion
// du client et les méthodes pour communiquer avec lui. Elle est supprimée
// lors de la déconnexion du client.

use crate::{
    backups::*,
    connected::*,
    constants::*,
    properties::*,
    controlled::*,
    database::*,
    messages::*,
    utils::*,
    mails::notification_email,
    create_data, // Macro de construction des paramètres des commandes
};

use std::{
    fs::File,
    path::Path,
    sync::Arc,
    net::SocketAddr,
    io::{self, ErrorKind},
};

use tokio::{
    fs,
    net::TcpStream,
    time::{sleep, Duration},
    sync::{RwLock, broadcast},
    io::{AsyncReadExt, AsyncWriteExt},
};

use chrono::{DateTime, Local}; // Pour le calcul des dates
use fs2::FileExt; // Pour verrouiller un fichier en écriture

// Pour MessagePack
use serde_json::Value;
use rmp_serde::{from_slice, to_vec};

// Types personnalisés
type EmptyResult = Result<(), Box<dyn std::error::Error>>;
type StringResult = Result<String, Box<dyn std::error::Error>>;

// ----------------------------------------------------------------------------
// Informations de connexion d'un client
// ----------------------------------------------------------------------------
// La structure Client contient les informations de connexion d'un client
// et les méthodes pour communiquer avec lui. Elle est supprimée lors de la
// déconnexion du client.

#[derive(Debug)]
pub struct Client {

    addr: SocketAddr,                        // Adresse du client
    read_stream: Arc<RwLock<TcpStream>>,     // Socket pour lire
    write_stream: Arc<RwLock<TcpStream>>,    // Socket pour écrire
    shutdown_tx: broadcast::Sender<bool>,    // Canal pour signaler un shutdown
    shutdown_rx: broadcast::Receiver<bool>,  // Canal pour écouter un shutdown

    since: DateTime<Local>,                  // Début de la connexion
    login: Option<String>,                   // Login du client (ou None)
    status: Option<UserStatus>,              // Status du client (ou None)
    session: Option<String>,                 // Session du client (ou None)
    fullname: Option<String>,                // Fullname du client (ou None)
    pub edited_file: Option<String>,         // Fichier en cours d'édition (ou None)
    pub help_flag: bool,                     // Flag d'appel
}

// Implémentation manuelle du trait Clone en raison de la logique
// particulière de clonage (subscribe) de l'attribut shutdown_rx.

impl Clone for Client {
    fn clone(&self) -> Client {
        Client {
            addr: self.addr,
            read_stream: Arc::clone(&self.read_stream),
            write_stream: Arc::clone(&self.write_stream),
            shutdown_tx: self.shutdown_tx.clone(),
            shutdown_rx: self.shutdown_tx.subscribe(),

            since: self.since,
            login: self.login.clone(),
            status: self.status.clone(),
            session: self.session.clone(),
            fullname: self.fullname.clone(),
            edited_file: self.edited_file.clone(),
            help_flag: self.help_flag.clone(),
        }
    }
}

#[allow(dead_code)]
impl Client {

    // ----------------------------------------------------------------------------
    // Constructeur de la structure Client
    // ----------------------------------------------------------------------------

    pub fn new(addr: SocketAddr, socket: TcpStream) -> Self {

        // Crée un canal de diffusion (broadcast) pour gérer un signal de fermeture
        let (shutdown_tx, shutdown_rx) = broadcast::channel(1);

        // Convertit le TcpStream de Tokio en TcpStream standard afin de pouvoir le dupliquer
        let std_socket = socket.into_std().expect("Failed to convert to std socket");

        // Crée un flux de lecture en clonant le TcpStream standard et en le réintégrant en un TcpStream Tokio
        // Ce flux de lecture est protégé par un RwLock pour permettre un accès concurrent (lecture seule) sécurisé
        let read_stream = Arc::new(RwLock::new(TcpStream::from_std(std_socket
            .try_clone().expect("Failed to clone socket"))
            .expect("Failed to convert back to tokio socket")));

        // Crée un flux d'écriture à partir du même TcpStream standard et le réintègre en un TcpStream Tokio
        // Ce flux d'écriture est protégé par un RwLock pour permettre un accès concurrent (écriture seule) sécurisé
        let write_stream = Arc::new(RwLock::new(TcpStream::from_std(std_socket)
            .expect("Failed to convert back to tokio socket")));

        Client {
            addr,
            read_stream, write_stream,
            shutdown_tx, shutdown_rx,
            since: Local::now(),
            login: None,
            status: None,
            session: None,
            fullname: None,
            edited_file: None,
            help_flag: false,
        }
    }

    pub fn describe(&self) {
        println!("addr        : {}", self.addr);
        println!("since       : {}", self.since);
        println!("login       : {:?}", self.login);
        println!("status      : {:?}", self.status);
        println!("session     : {:?}", self.session);
        println!("fullname    : {:?}", self.fullname);
        println!("help_flag   : {:?}", self.help_flag);
        println!("edited_file : {:?}", self.edited_file);
    }

    pub fn describe_to_log(&self) {
        log::info!("login={:?} status={:?} session={:?} help_flag={:?} edited_file={:?}",
            self.login, self.status, self.session, self.help_flag, self.edited_file);
    }

    // ----------------------------------------------------------------------------
    // Méthodes d'accès à la structure Client
    // ----------------------------------------------------------------------------

    pub fn is_student(&self) -> bool {
        self.status == Some(UserStatus::STUDENT)
    }

    pub fn is_tutor(&self) -> bool {
        self.status == Some(UserStatus::TUTOR) || self.status == Some(UserStatus::ADMIN)
    }

    pub fn get_login(&self) -> Option<&String> {
        self.login.as_ref()
    }

    pub fn get_status(&self) -> Option<&UserStatus> {
        self.status.as_ref()
    }

    pub fn get_status_str(&self) -> String {
        self.get_status().unwrap().to_string()
    }

    pub fn get_session(&self) -> Option<&String> {
        self.session.as_ref()
    }

    pub fn get_fullname(&self) -> Option<&String> {
        self.fullname.as_ref()
    }

    pub fn get_edited_file(&self) -> Option<&String> {
        self.edited_file.as_ref()
    }

    pub fn get_help_flag(&self) -> bool {
        self.help_flag
    }

    pub fn get_date(&self) -> String {
        self.since.to_string()
    }

    pub fn get_shutdown_rx(&self) -> broadcast::Receiver<bool> {
        self.shutdown_rx.resubscribe()
    }

    // Retourne l'addresse IP sans le port
    pub fn get_addr(&self) -> String {
        let addr = self.addr.to_string();
        addr.splitn(2, ':').next().unwrap().to_string()
    }

    // Affecte le login et le status du client
    pub fn set_login(&mut self, login: String) {
        self.login = Some(login.clone());
        self.status = get_status(&login);
        self.fullname = get_fullname(&login);
    }

    pub fn set_session(&mut self, session: String) {
        self.session = Some(session.clone());
    }
    
    pub fn set_password(&mut self, new_password: String) {
        // Vérifie que login n'est pas None
        if let Some(ref login) = self.login {
            // La modification se fait dans la base de données
            set_clientpass(login, &new_password);
        }
    }

    // Vérifie le login du client
    pub fn has_login(&self, login: &str) -> bool {
        match self.login {
            Some(ref l) => l == login,
            None => false,
        }
    }

    // Vérifie le statut du client
    pub fn has_status(&self, status: &UserStatus) -> bool {
        match self.status {
            Some(ref s) => s == status,
            None => false,
        }
    }

    // Vérifie la session du client
    pub fn has_session(&self, session: &str) -> bool {
        match self.session {
            Some(ref s) => s == session,
            None => false,
        }
    }

    // Calcule la durée totale de connexion
    pub fn connection_duration(&self) -> String {
        let duration = Local::now() - self.since;
        let hours = duration.num_hours();
        let minutes = duration.num_minutes() % 60;
        let seconds = duration.num_seconds() % 60;
        format!("{:02}:{:02}:{:02}", hours, minutes, seconds)
    }

    pub fn update_worktime(&self) {
        // let duration = Local::now() - self.since;
        // TODO: Mettre à jour le temps de travail dans la base de données
        // XXXXXXXX
    }

    // ----------------------------------------------------------------------------
    // Méthodes de communication avec le client
    // ----------------------------------------------------------------------------

    // Envoie une chaîne de caractères et attendre une réponse
    pub async fn send_and_receive(&self, msg: &str) -> StringResult {
        self.send_string(msg).await?;
        self.read_string().await
    }

    pub async fn send_string(&self, msg: &str) -> EmptyResult {
        let mut socket = self.write_stream.write().await;
        let message_with_newline = format!("{}\n", msg);
        socket.write_all(message_with_newline.as_bytes()).await?;
        Ok(())
    }

    // Réceptionne une chaîne de caractères (inner function)
    pub async fn read_string(&self) -> StringResult {
        let mut socket = self.read_stream.write().await;
        let mut buffer = [0u8; STR_BUFFER_SIZE];
        match socket.read(&mut buffer).await? {
            0 => Err("Zero bytes error".into()),
            size => Ok(buffer_to_str(&buffer, size)),
        }
    }

    /*
    // Envoie une chaîne de caractères avec timeout
    pub async fn send_string_timeout(&self, msg: &str) -> EmptyResult {
        let write_timeout = get_property_duration(TIMEOUT_WRITE);
        match timeout(write_timeout, self.send_string(msg)).await {
            Ok(result) => result,
            Err(_) => {
                log::warn!("Write timeout ({}s) while sending string to client", write_timeout.as_secs());
                Err(format!("String write timeout ({}s)", write_timeout.as_secs()).into())
            }
        }
    }

    // Réceptionne une chaîne de caractères avec timeout
    pub async fn read_string_timeout(&self) -> StringResult {
        let read_timeout = get_property_duration(TIMEOUT_READ);
        match timeout(read_timeout, self.read_string()).await {
            Ok(result) => result,
            Err(_) => {
                log::warn!("Read timeout ({}s) while reading string from client", read_timeout.as_secs());
                Err(format!("String read timeout ({}s)", read_timeout.as_secs()).into())
            }
        }
    }
    */

    // Attend un message MessagePack de la part du client
    // Ne pas pas utiliser de timeout dans cette fonction !!
    pub async fn wait_message(&self) -> io::Result<MessageFromClient> {
        let mut socket = self.read_stream.write().await;

        // Lire la taille du message (en-tête de 4 octets)
        let mut length_buf = [0u8; 4];
        socket.read_exact(&mut length_buf).await?;
        let message_length = u32::from_be_bytes(length_buf) as usize;

        if message_length > MESSAGE_MAX_SIZE {
            log::error!(
                "ERROR: Message size {} exceeds maximum limit {}",
                message_length,
                MESSAGE_MAX_SIZE
            );
            return Err(io::Error::new(
                ErrorKind::InvalidData,
                "Message size exceeds limit",
            ));
        }

        // Lire le message dans un buffer d'octets
        let mut buffer = vec![0u8; message_length];
        socket.read_exact(&mut buffer).await?;

        // Désérialiser le message MessagePack
        match from_slice::<MessageFromClient>(&buffer) {
            Ok(msg) => Ok(msg),
            Err(e) => {
                log::error!("ERROR: Deserialization failed: {}", e);
                Err(io::Error::new(
                    ErrorKind::InvalidData,
                    format!("Deserialization failed: {}", e),
                ))
            }
        }
    }

    // Pour déconnecter proprement le client
    pub async fn disconnect(&self) -> EmptyResult {
        // Avertir le client distant pour qu'il ferme proprement
        // sans déclencher d'auto-reconnexion (correctif Gemini)
        let _ = self.server_shutdown().await;
        sleep(Duration::from_millis(50)).await;

        if let Err(e) = self.shutdown_tx.send(true) {
            log::warn!("Failed to send shutdown signal to {:?}: {}", self.login, e);
        }
        Ok(())
    }

    // Pour déconnecter tous les clients de la session
    pub async fn disconnect_all(&self) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let clients = get_clients(&session, None).await;
        for client in clients {
            if client.has_login(login) { continue; } // No suicide allowed
            client.disconnect().await?;
            // Temporisation de 100ms entre chaque déconnexion
            sleep(Duration::from_millis(100)).await;
        }
        Ok(())
    }

    // Pour forcer la déconnexion d'un client (éjection)
    pub async fn force_disconnect(&self, user_login: &str) -> EmptyResult {
        force_disconnect(user_login).await;
        Ok(())
    }

    // Ferme la connexion proprement
    pub async fn shutdown_socket(&self) -> EmptyResult {
        {
            // Fermer le socket de lecture dans un scope séparé afin d'éviter un deadlock
            let mut read_socket = self.read_stream.write().await;
            read_socket.shutdown().await?;
        }
        {
            // Fermer le socket d'écriture dans un scope séparé afin d'éviter un deadlock
            let mut write_socket = self.write_stream.write().await;
            write_socket.shutdown().await?;
        }
        Ok(())
    }

    // Envoie une commande MessagePack au client distant
    async fn send_command(&self, cmd: CommandType, data: Vec<Value>) -> EmptyResult {
        let mut socket = self.write_stream.write().await;
        // Construction du message
        let message = MessageToClient::new(cmd, data);
        // Sérialisation en MessagePack
        let serialized = to_vec(&message)?;
        // Envoi du message sérialisé
        socket.write_all(&serialized).await?;
        Ok(())
    }

    // ----------------------------------------------------------------------------
    // Commandes en provenance des clients Java
    // ----------------------------------------------------------------------------

    // Simple ping-pong
    pub async fn ping(&self) -> EmptyResult {
        let data = create_data!();
        self.send_command(CommandType::PONG, data).await
    }

    // Pour tester la réception de données et l'envoi d'une liste
    pub async fn bidule(&self, arg1: &str, arg2: &i64, arg3: &bool) -> EmptyResult {
        println!("Received: {} {} {}", arg1, arg2, arg3);
        let text = "Hello World";
        let list = vec![1, 2, 3];
        let data = create_data!(text, list);
        self.send_command(CommandType::HELLO, data).await
    }

    // Pour changer son propre password
    pub async fn change_password(&self, pass_sha256: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        set_clientpass(login, pass_sha256);
        Ok(()) // Pas d'erreur a priori
    }

    // Pour reset le password d'un student
    pub async fn reset_password(&self, user_login: &str) -> EmptyResult {
        set_clientpass(user_login, user_login); // pass = login
        Ok(()) // Pas d'erreur a priori
    }

    // Pour changer la session du client
    pub async fn change_session(&mut self, new_session: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        self.help_flag = false;
        self.edited_file = None;
        reset_client_state(login).await;
        // Notifier les tuteurs de la session de départ
        self.notify_tutors_client_exited().await?;
        // Déplacement du client
        move_client(login, new_session).await; 
        // Envoyer au client la nouvelle session
        self.init_session(&new_session).await?;
        // Notifier les tuteurs de la session d'arrivée
        self.notify_tutors_client_arrived().await?;
        // Mettre également à jour la session de la structure courante
        // car le move_client() génère un clone de la structure Client
        self.session = Some(new_session.to_string());
        Ok(())
    }

    // Pour créer un nouvel étudiant dans la session courante
    pub async fn add_new_student(&self, new_login: &str, new_name: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();

        // Vérifier que new_login n'existe pas déjà
        if check_login(new_login) {
            self.message_from_server_1("ServerWarningMessage_7", new_login).await?;
            return Err(format!("Login {} already exists", new_login).into());
        }
        add_new_student_to_session(new_login, new_name, session)?; // Termine ici si erreur
        
        // Notifier les tuteurs de la session
        if let Some(udata) = build_userdata(new_login) {
            log::info!("Tutor {} creates a new student {} in session {}", login, new_login, session);
            self.notify_tutors_add_new_student(udata).await?;
            Ok(())
        } else {
            Err(format!("Could not build user data for {}", new_login).into())
        }
    }

    // Pour envoyer un message à un client via le chat
    pub async fn chat_to(&self, dest_login: &str, msg: &str) -> EmptyResult {
        let sender = self.login.as_ref().unwrap();
        let fullname = self.fullname.as_ref().unwrap();
        let client = get_client(dest_login).await.unwrap();
        client.chat_from(&sender, &fullname, msg).await
    }
    
    // Pour envoyer un message à un client
    pub async fn message_to(&self, dest_login: &str, msg: &str) -> EmptyResult {
        let sender = self.login.as_ref().unwrap();
        let fullname = self.fullname.as_ref().unwrap();
        let client = get_client(dest_login).await.unwrap();
        client.message_from(&sender, &fullname, msg).await
    }

    // Pour envoyer un message à tous les clients de la session
    pub async fn message_to_all(&self, msg: &str) -> EmptyResult {
        let sender = self.login.as_ref().unwrap();
        let fullname = self.fullname.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let clients = get_clients(session, None).await;
        for client in clients {
            if client.has_login(sender) { continue; } // No message to self
            client.message_from(&sender, &fullname, msg).await?;
        }
        Ok(())
    }

    // Pour envoyer un message à tous les clients connectés
    pub async fn message_global(&self, msg: &str) -> EmptyResult {
        let sender = self.login.as_ref().unwrap();
        let fullname = self.fullname.as_ref().unwrap();
        let clients = get_all_clients().await;
        for client in clients {
            if client.has_login(sender) { continue; } // No message to self
            client.message_from(&sender, &fullname, msg).await?;
        }
        Ok(())
    }

    // Pour ouvrir ou fermer la session
    pub async fn set_session_openned(&self, flag: &bool) -> EmptyResult {
        let session = self.session.as_ref().unwrap();
        set_session_openned(&session, &flag); // Fonction synchrone
        // Notifier tous les utilisateurs connectés
        let clients = get_all_clients().await;
        for client in clients {
            client.open_session(&session, flag).await?;
        }
        Ok(())
    }

    // Changement du fichier en cours d'édition
    pub async fn set_edited_file(&mut self, filename: Option<&str>) -> EmptyResult {
        match filename {
            Some(name) => {
                self.edited_file = Some(name.to_string());
                let login = self.login.as_ref().unwrap();
                // S'assurer que la modification est aussi dans la table des connectés
                set_client_edited_file(login, Some(name.to_string())).await;
                // Notifier les tuteurs de la session
                self.notify_tutors_set_edited_file(Some(name)).await?;
            }
            None => {
                self.edited_file = None;
                let login = self.login.as_ref().unwrap();
                set_client_edited_file(login, None).await;
                self.notify_tutors_set_edited_file(None).await?;
            }
        }
        Ok(())
    }

    // Modifier le flag localement ET dans la table des connectés
    pub async fn set_help_flag(&mut self, flag: &bool) -> EmptyResult {
        self.help_flag = flag.clone();
        let login = self.login.as_ref().unwrap();
        // S'assurer que la modification est aussi dans la table des connectés
        set_client_help_flag(login, *flag).await;
        // Notifier les tuteurs de la session
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        let mut no_tutor = true;
        for tutor in tutors {
            no_tutor = false;
            tutor.tutor_set_help_flag(login, flag).await?
        }
        if *flag && no_tutor {
            if let Some(client) = get_client(login).await {
                // Informer le client de l'absence de tuteur
                client.message_from_server("ServerWarningMessage_2").await
                    .expect("Error from message_from_server");
            }
        }
        Ok(())
    }
    
    // Baisser le flag localement ET dans la table des connectés
    pub async fn reset_help_flag(&self, login: &str) -> EmptyResult {
        // Modifier directement le flag dans la table des connectés
        if set_client_help_flag(login, false).await {
            // Récupérer le client pour les notifications
            if let Some(client) = get_client(login).await {
                // Notifier le client
                let data = create_data!();
                client.send_command(CommandType::RESET_HELP_FLAG, data).await?;
            }
            // Notifier les tuteurs de la session
            self.notify_tutors_reset_help_flag(login).await?;
        }
        Ok(())
    }

    // ----------------------------------------------------------------------------
    // Fonction sur les fichiers et dossiers

    // Pour créer un dossier vide
    // Le path est de la forme [session]/[login]/...
    pub async fn new_folder(&self, path: &str) -> EmptyResult {
        let file_path = Path::new(PROG_DIR).join(path);
        if let Err(e) = fs::create_dir(&file_path).await {
            println!("Erreur lors de la création du dossier: {}", e);
        }
        // Notifier les tuteurs de la session
        self.notify_tutors_new_folder(path).await?;
        Ok(())
    }

    // Pour créer un fichier vide
    // Le path est de la forme [session]/[login]/...
    pub async fn new_file(&self, path: &str) -> EmptyResult {
        let file_path = Path::new(PROG_DIR).join(path);
        if let Err(e) = File::create(&file_path) {
            println!("Erreur lors de la création du fichier: {}", e);
        }
        // Notifier les tuteurs de la session
        self.notify_tutors_new_file(path).await?;
        Ok(())
    }

    // Pour supprimer un fichier ou un dossier
    // Le path est de la forme [session]/[login]/...
    pub async fn delete_file(&self, path: &str) -> EmptyResult {
        let file_path = Path::new(PROG_DIR).join(path);
        if file_path.is_file() {
            // Supprimer le fichier
            if let Err(e) = fs::remove_file(&file_path).await {
                println!("Erreur lors de la suppression du fichier: {}", e);
            } else {
                // Supprimer aussi les backups
                if let Err(e) = delete_backup_files(&file_path) {
                    log::warn!("Erreur lors de la suppression des backups pour {}: {}", path, e);
                }
            }
        } else if file_path.is_dir() {
            // Supprimer le dossier
            if let Err(e) = fs::remove_dir_all(&file_path).await {
                println!("Erreur lors de la suppression du dossier: {}", e);
            }
        }
        // Notifier les tuteurs de la session
        self.notify_tutors_delete_file(path).await?;
        Ok(())
    }

    // Pour renommer un fichier
    // Le path est de la forme [session]/[login]/...
    pub async fn rename_file(&self, path: &str, new_name: &str) -> EmptyResult {
        let old_path = Path::new(PROG_DIR).join(path);
        // Reconstruire le nouveau path
        let parent = old_path.parent().unwrap_or(Path::new(""));
        let new_path = parent.join(new_name);
        // Renommer le fichier principal
        if let Err(e) = fs::rename(&old_path, &new_path).await {
            println!("Erreur lors du renommage du fichier: {}", e);
        } else {
            // Renommer aussi les backups
            if let Err(e) = rename_backup_files(&old_path, new_name) {
                log::warn!("Erreur lors du renommage des backups pour {}: {}", path, e);
            }
        }
        // Notifier les tuteurs de la session
        self.notify_tutors_rename_file(path, new_name).await?;
        Ok(())
    }

    // Pour déplacer un fichier
    // Le path est de la forme [session]/[login]/...
    pub async fn move_file(&self, path: &str, dest_path: &str) -> EmptyResult {
        let old_path = Path::new(PROG_DIR).join(path);
        let filename = old_path.file_name().unwrap_or_default();
        let new_path = Path::new(PROG_DIR).join(dest_path).join(filename);
        // Déplacer le fichier principal
        if let Err(e) = fs::rename(&old_path, &new_path).await {
            println!("Erreur lors du déplacement du fichier: {}", e);
        } else {
            // Déplacer aussi les backups
            if let Err(e) = move_backup_files(&old_path, &new_path) {
                log::warn!("Erreur lors du déplacement des backups pour {}: {}", path, e);
            }
        }
        // Notifier les tuteurs de la session
        self.notify_tutors_move_file(path, dest_path).await?;
        Ok(())
    }

    // ----------------------------------------------------------------------------
    // Autres requêtes en provenance des clients

    // Demande de fichier de la part d'un tuteur
    // Le path est de la forme [session]/[login]/...
    // Le client_path où enregistrer le fichier au niveau du client
    pub async fn request_file(&self, path: &str, client_path: &str) -> EmptyResult {
        let file_path = Path::new(PROG_DIR).join(path);
        let file_path_str = file_path.to_str().unwrap_or("");
        match read_all_bytes(file_path_str) {
            Ok(content) => {
                if let Err(e) = self.update_file(&content, client_path).await {
                    println!("Erreur lors de l'envoi du fichier: {}", e);
                }
            }
            Err(e) => {
                eprintln!("Erreur lors de la lecture du fichier: {}", e);
            }
        }
        Ok(())
    }

    // Demande de contrôle de la part d'un tuteur
    pub async fn ask_control_mode(&self, user_login: &str, flag: &bool) -> EmptyResult {
        let tutor_login = self.login.as_ref().unwrap();
        let tutor_fullname = self.fullname.as_ref().unwrap(); // Nom complet du tuteur
        let user_fullname = get_fullname(user_login).unwrap(); // Nom complet du user

		if *flag {
            if let Some(ctrl_login) = get_controller(user_login).await {
                // Client déjà sous contrôle
                let controller = get_client(&ctrl_login).await.unwrap();
                let ctrl_fullname = controller.get_fullname().unwrap();
                // Notifier le tuteur que le client est déjà sous contrôle
                self.message_from_server_2("ServerWarningMessage_5", &user_fullname, ctrl_fullname).await
                    .expect("Error from message_from_server");
            } else {
                // Mettre à jour la table des users contrôlés
                add_controlled_user(user_login, tutor_login).await;
                // Notifier le student qu'il est contrôlé (si connecté)
                if let Some(client) = get_client(user_login).await {
                    client.set_controlled(&true, tutor_fullname).await?;
                }
                // Notifier le tuteur qu'il contrôle le client
                self.set_control_mode(&true, user_login).await?;

                log::info!("{} is now controlled by {}", user_login, tutor_login);
            }
        } else {
            // On libère le contrôle
            if let Some(_) = get_controller(user_login).await {
                // Mettre à jour la table des users contrôlés
                remove_controlled_user(user_login).await;
                // Notifier le demandeur qu'il ne contrôle plus le client
                self.set_control_mode(&false, user_login).await?;
                // Notifier le client qu'il n'est plus sous contrôle (si connecté)
                if let Some(client) = get_client(user_login).await {
                    client.set_controlled(&false, "--").await?; // Second argument unused
                }
                log::info!("{} is no longer controlled", user_login);
            }
        }
        Ok(())
    }

    // Pour sauvegarder un fichier en provenance d'un client
    // Le path est de la forme [session]/[login]/...
    pub async fn upload_file(&self, path: &str, file_content: &[u8]) -> EmptyResult {
        let file_path = Path::new(PROG_DIR).join(path);
        if let Some(parent_dir) = file_path.parent() {
            // Extraire le répertoire parent et le créer si nécessaire
            // Nécessaire en mode contrôle dans upload_controlled_file()
            // Car les dossiers des tuteurs n'existent pas forcément
            ensure_dir_exists(parent_dir)?;
        }
        let mut file = std::fs::File::create(&file_path)?;
        file.lock_exclusive()?;
        std::io::Write::write_all(&mut file, file_content)?;
        Ok(())
    }

    // Réception et retransmission d'un fichier en mode contrôle
    // Le path est de la forme [session]/[login_tutor]/[login_user]
    pub async fn upload_controlled_file(&self, tutor_path: &str, file_content: &[u8], user_login: &str) -> EmptyResult {
        // Reconstruction du path student
        let session = tutor_path.split('/').next().unwrap();
        let endname = tutor_path.split(user_login).nth(1).unwrap();
        let user_path = format!("{}/{}{}", session, user_login, endname);
		//println!(" Tutor Path = {}", tutor_path);
		//println!(" Student Path = {}", user_path);

        // Sauvegarde du fichier pour le tuteur
        if let Err(e) = self.upload_file(tutor_path, file_content).await {
            let message = format!("Error saving tutor file '{}': {}", tutor_path, e);
            log::error!("{}", message);
            //self.exception_report(&message);
        }
        // Sauvegarde du fichier pour l'étudiant
        if let Err(e) = self.upload_file(&user_path, file_content).await {
            let message = format!("Error saving student file '{}': {}", user_path, e);
            log::error!("{}", message);
            //self.exception_report(&message);
        }
        // Retransmission du fichier au client (si connecté)
        if let Some(client) = get_client(user_login).await {
            client.update_file_controlled(file_content, endname).await?;
        } 
        Ok(())
    }

    // Pour envoyer un rapport d'exception par mail
    pub async fn exception_report(&self, content: &str) -> EmptyResult {
        let report_status = get_property(REPORT_STATUS);
        if report_status == "ON" {
            let login = self.login.as_ref().unwrap();
            log::info!("Exception report received from {}", login);
            notification_email("Client exception report", content);
        }
        Ok(())
    }

    pub async fn log(&mut self, _txt: &str) -> EmptyResult {
        // Réservé pour la journalisation client future
        Ok(())
    }

    // ----------------------------------------------------------------------------
    // Commandes à destination des clients Java
    // ----------------------------------------------------------------------------

    // Pour initialiser la session du client
    pub async fn init_session(&self, session: &str) -> EmptyResult {
        
        let login = self.login.as_ref().unwrap();
        let user_name = self.fullname.as_ref().unwrap();
        let statut_str = self.get_status_str();
        let server_name = get_property(SERVER_NAME);
        let opened = get_session_openned(session).await;

        log::info!("Initializing {} ({}) with session {}", login, statut_str, session);

        // Choix du dossier à envoyer
        let dir_path: String = match self.get_status() {
            Some(UserStatus::STUDENT) => format!("sessions/{}/{}", session, login),
            Some(UserStatus::TUTOR) => format!("sessions/{}", session),
            Some(UserStatus::ADMIN) => format!("sessions/{}", session),
            None => return Err("Client status is None".into()),
        };

        // S'assurer que le dossier existe sur le serveur avant compression
        if let Err(e) = ensure_dir_exists(&dir_path) {
            log::warn!("Could not ensure dir exists for {}: {}", dir_path, e);
        }

        // Compresser la session dans TEMP_DIR avec un nom unique par client pour éliminer toute collision
        let timestamp = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis();
        let file_name = format!("session_{}_{}_{}.tar.zst", session, login, timestamp);
        let temp_file = format!("{}/{}", TEMP_DIR, file_name);

        if let Err(error) = compress_archive(&dir_path, &temp_file, Some(5)) {
            log::error!("Error while compressing {}: {}", dir_path, error);
            return Err(format!("Error while compressing {}: {}", dir_path, error).into());
        }

        // Lire temp_file dans un vecteur d'octets Vec<u8>
        let file_content = match std::fs::read(&temp_file) {
            Ok(content) => content,
            Err(error) => {
                log::error!("Error while reading {}: {}", temp_file, error);
                return Err(format!("Error while reading {}: {}", temp_file, error).into());
            }
        };

        // Supprimer le dossier TEMP_DIR
        if let Err(error) = std::fs::remove_file(temp_file.clone()) {
            log::error!("ERROR: {} while deleting {}", error, temp_file);
        }

        // Le client est-il contrôlé ?
        let ctrl_name = if let Some(ctrl_login) = get_controller(login).await {
            get_fullname(&ctrl_login).unwrap() // Nom du contrôleur
        } else {
            "NONE".to_string() // Pas de contrôleur
        };

        // Construire la liste des arguments Vec<Value>
        let data = if self.is_student() {
            // Pas de UserData pour les students
            create_data!(session, statut_str, user_name, server_name, file_name, file_content, opened, ctrl_name)
        } else {
            // Inclure la liste des UserData pour les tuteurs
            let udata_list: Vec<UserData> = get_userdata(session, login).await;
            create_data!(session, statut_str, user_name, server_name, file_name, file_content, opened, ctrl_name, udata_list)
        };

        // Envoyer la session compressée au client
        self.send_command(CommandType::INIT_SESSION, data).await?;
        Ok(())
    }

    // Pour envoyer un message vers le chat du client
    pub async fn chat_from(&self, sender: &str, fullname: &str, msg: &str) -> EmptyResult {
        let data = create_data!(sender, fullname, msg);
        self.send_command(CommandType::CHAT_FROM, data).await
    }

    // Pour ouvrir une fenêtre de dialogue avec un message
    pub async fn message_from(&self, sender: &str, fullname: &str, msg: &str) -> EmptyResult {
        let data = create_data!(sender, fullname, msg);
        self.send_command(CommandType::MESSAGE_FROM, data).await
    }

    // Pour envoyer un message identifié par un code
    pub async fn message_from_server(&self, msg_id: &str) -> EmptyResult {
        let data = create_data!(msg_id);
        self.send_command(CommandType::MESSAGE_FROM_SERVER, data).await
    }

    // Pour envoyer un message identifié par un code et 1 argument
    pub async fn message_from_server_1(&self, msg_id: &str, arg: &str) -> EmptyResult {
        let data = create_data!(msg_id, arg);
        self.send_command(CommandType::MESSAGE_FROM_SERVER, data).await
    }

    // Pour envoyer un message identifié par un code et 2 arguments
    pub async fn message_from_server_2(&self, msg_id: &str, arg1: &str, arg2: &str) -> EmptyResult {
        let data = create_data!(msg_id, arg1, arg2);
        self.send_command(CommandType::MESSAGE_FROM_SERVER, data).await
    }

    // Pour envoyer un message vers la console du client
    pub async fn message_to_console(&self, msg: &str) -> EmptyResult {
        let data = create_data!(msg);
        self.send_command(CommandType::MESSAGE_TO_CONSOLE, data).await
    }

    // Pour notifier de l'ouverture d'une session
    pub async fn open_session(&self, session_id: &str, flag: &bool) -> EmptyResult {
        let data = create_data!(session_id, flag);
        self.send_command(CommandType::OPEN_SESSION, data).await
    }

    // Pour notifier du shutdown du serveur
    pub async fn server_shutdown(&self) -> EmptyResult {
        let data = create_data!();
        self.send_command(CommandType::SERVER_SHUTDOWN, data).await
    }
    
    // Pour prendre ou redonner le contrôle du client
    pub async fn set_controlled(&self, flag: &bool, tutor_name: &str) -> EmptyResult {
        let data = create_data!(flag, tutor_name);
        self.send_command(CommandType::SET_CONTROLLED, data).await
    }

    // Pour activer ou désactiver le mode contrôle d'un tuteur
    pub async fn set_control_mode(&self, flag: &bool, user_login: &str) -> EmptyResult {
        let data = create_data!(flag, user_login);
        self.send_command(CommandType::SET_CONTROL_MODE, data).await
    }

    // Pour envoyer un fichier au client
    pub async fn update_file(&self, file_content: &[u8], client_path: &str) -> EmptyResult {
        let data = create_data!(file_content, client_path);
        self.send_command(CommandType::UPDATE_FILE, data).await
    }

    // Pour envoyer un fichier au client en mode contrôle
    pub async fn update_file_controlled(&self, file_content: &[u8], client_path: &str) -> EmptyResult {
        let data = create_data!(file_content, client_path);
        self.send_command(CommandType::UPDATE_FILE_CONTROLLED, data).await
    }

    // ----------------------------------------------------------------------------
    // Commandes à destination des tuteurs de la session courante
    // ----------------------------------------------------------------------------

    // Pour notifier les tuteurs de l'arrivée du client
    pub async fn notify_tutors_client_arrived(&self) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; } // Sa propre arrivée
            let data = create_data!(login, self.get_addr(), self.get_date());
            tutor.send_command(CommandType::CLIENT_ARRIVED, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs du départ du client
    pub async fn notify_tutors_client_exited(&self) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; } // Son propre départ
            let data = create_data!(login);
            tutor.send_command(CommandType::CLIENT_EXITED, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs de la création d'un nouvel étudiant
    pub async fn notify_tutors_add_new_student(&self, udata: UserData) -> EmptyResult {
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            let data = create_data!(&udata);
            //log::info!("notifying {}...", tutor.get_login().unwrap());
            tutor.send_command(CommandType::NEW_STUDENT, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs d'un changement de fichier courant
    pub async fn notify_tutors_set_edited_file(&self, path: Option<&str>) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        let file_info = path.unwrap_or(EMPTY_FILE_MARKER);
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, file_info);
            tutor.send_command(CommandType::SET_EDITED_FILE, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs d'un reset du flag help d'un client
    pub async fn notify_tutors_reset_help_flag(&self, user_login: &str) -> EmptyResult {
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            tutor.tutor_set_help_flag(user_login, &false).await?
        }
        Ok(())
    }

    // Pour positionner le flag help d'un client
    pub async fn tutor_set_help_flag(&self, login: &str, flag: &bool) -> EmptyResult {
        let data = create_data!(login, flag);
        self.send_command(CommandType::SET_HELP_FLAG, data).await
    }

    // Pour notifier les tuteurs de la création d'un dossier
    pub async fn notify_tutors_new_folder(&self, path: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, path);
            //log::info!("notifying {}...", tutor.get_login().unwrap());
            tutor.send_command(CommandType::NEW_FOLDER, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs de la création d'un fichier
    pub async fn notify_tutors_new_file(&self, path: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, path);
            tutor.send_command(CommandType::NEW_FILE, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs de la suppression d'un fichier
    pub async fn notify_tutors_delete_file(&self, path: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, path);
            tutor.send_command(CommandType::DELETE_FILE, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs du renommage d'un fichier
    pub async fn notify_tutors_rename_file(&self, path: &str, name: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, path, name);
            tutor.send_command(CommandType::RENAME_FILE, data).await?
        }
        Ok(())
    }

    // Pour notifier les tuteurs du déplacement d'un fichier
    pub async fn notify_tutors_move_file(&self, path1: &str, path2: &str) -> EmptyResult {
        let login = self.login.as_ref().unwrap();
        let session = self.session.as_ref().unwrap();
        let tutors = connected_tutors(session).await;
        for tutor in tutors {
            if tutor.has_login(login) { continue; }
            let data = create_data!(login, path1, path2);
            tutor.send_command(CommandType::MOVE_FILE, data).await?
        }
        Ok(())
    }
}

// ============================================================================
// Fichier : connected.rs
// Version : 11/09/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// La table des clients connectés est une structure de données partagée
// entre les différentes tâches asynchrones qui gèrent les connexions.
// Elle est protégée par Mutex pour éviter les problèmes de concurrence.

// Arc et Mutex permettent de partager de manière sécurisée des données
// comme la liste des connexions entre plusieurs tâches asynchrones.

// Un ARC (Atomic Reference Counted) ou pointeur intelligent permet de
// partager de manière sécurisée des données entre plusieurs threads.

use std::{
    sync::Arc,
    collections::HashMap,
};

use tokio::sync::Mutex as TokioMutex;

use lazy_static::lazy_static;

use crate::client::Client;
use crate::database::UserStatus;
use crate::constants::PROG_DIR;

use crate::properties::*;
use crate::constants::*;
use crate::controlled::remove_all_controlled_by_tutor;

// Type de la table de hashage des clients connectés
type ConnectedClients = Arc<TokioMutex<HashMap<String, HashMap<String, Client>>>>;

/*
{
    "session1": {
        "login1": Client1,
        "login2": Client2,
        "login3": Client3,
    },
    "session2": {
        "login4": Client4,
        "login5": Client5,
    },
}
*/

// ----------------------------------------------------------------------------
// Table globale des clients connectés protégée par Mutex
// ----------------------------------------------------------------------------

lazy_static! {
    pub static ref CONNECTED_CLIENTS: ConnectedClients
        = Arc::new(TokioMutex::new(HashMap::new()));
}

pub async fn has_connections() -> bool {
    let sessions = CONNECTED_CLIENTS.lock().await;
    !sessions.is_empty()
}

// Ajouter un client à une session
pub async fn add_client(client: Client, session_id: &str) {
    let mut sessions = CONNECTED_CLIENTS.lock().await;
    let login = client.get_login().expect("ERROR").clone(); // No error expected
    // On créé la session du client si elle n'existe pas
    let session = sessions.entry(session_id.to_string()).or_insert(HashMap::new());
    session.insert(login, client);
    //println!("Table des connectés après ajout: {:#?}", *sessions);
}

// Retirer un client par son login
pub async fn remove_client(login: &str) -> bool {
    if let Some(client) = get_client(login).await {
        let session_id = client.get_session().expect("ERROR").clone(); // No error expected
        let mut sessions = CONNECTED_CLIENTS.lock().await;
        if let Some (session) = sessions.get_mut(&session_id) {
            session.remove(login);
            if session.is_empty() {
                // Supprimer les sessions vides
                sessions.remove(&session_id); 
                log::info!("Remove session {} (no more clients)", session_id);
            }
            if sessions.is_empty() {
                log::info!("There are no more active sessions");
                // Nettoyer les (rare) dossiers orphelins
                if let Err(e) = cleanup_orphaned_folders(PROG_DIR) {
                    log::warn!("Error during orphaned folders cleanup: {}", e);
                }
            }
            //println!("Table des connectés après suppression: {:#?}", *sessions);
            return true;
        }
    }
    false // Client non trouvé
}

// Déplacer un client d'une session à une autre
// Attention: déplace en réalité un clone de la structure Client
// Utiliser remove_client + add_client peut créer des pb de concurrence
// Voilà une version avec un seul verrou pour éviter les deadlocks
pub async fn move_client(login: &str, new_session: &str) -> bool {
    let mut sessions = CONNECTED_CLIENTS.lock().await;
    
    // Chercher et retirer le client de son ancienne session
    let mut client_found = None;
    let mut old_session_key = None;
    for (session_key, session) in sessions.iter_mut() {
        if let Some(mut client) = session.remove(login) {
            client.set_session(new_session.to_string());
            client_found = Some(client);
            old_session_key = Some(session_key.clone());
            break;
        }
    }
    // Si on a trouvé le client, l'ajouter à la nouvelle session
    if let Some(client) = client_found {
        sessions.entry(new_session.to_string())
               .or_insert(HashMap::new())
               .insert(login.to_string(), client);
        
        // Nettoyer l'ancienne session si elle est vide
        if let Some(old_key) = old_session_key {
            if sessions.get(&old_key).map_or(false, |s| s.is_empty()) {
                sessions.remove(&old_key);
                log::info!("Remove session {} (no more clients)", old_key);
            }
        }
        // Nettoyage global si plus de sessions
        if sessions.is_empty() {
            log::info!("There are no more active sessions");
            // Le nettoyage des dossiers orphelins peut rester asynchrone
            // mais on peut le faire après avoir relâché le verrou
        }
        //println!("Table des connectés après déplacement: {:#?}", *sessions);
        return true;
    }
    false // Client non trouvé
}

pub async fn reset_client_state(login: &str) -> bool {
    let mut sessions = CONNECTED_CLIENTS.lock().await;
    
    for session in sessions.values_mut() {
        if let Some(client) = session.get_mut(login) {
            client.help_flag = false;
            client.edited_file = None;
            return true;
        }
    }
    false
}

// Modifier directement le help_flag d'un client dans la table des connectés
pub async fn set_client_help_flag(login: &str, flag: bool) -> bool {
    let mut sessions = CONNECTED_CLIENTS.lock().await;
    for session in sessions.values_mut() {
        if let Some(client) = session.get_mut(login) {
            client.help_flag = flag;
            return true;
        }
    }
    false // Client non trouvé
}

// Modifier directement le fichier édité d'un client dans la table des connectés
pub async fn set_client_edited_file(login: &str, filename: Option<String>) -> bool {
    let mut sessions = CONNECTED_CLIENTS.lock().await;
    for session in sessions.values_mut() {
        if let Some(client) = session.get_mut(login) {
            client.edited_file = filename;
            return true;
        }
    }
    false // Client non trouvé
}

// Récupérer un client par son login (ou None si non connecté)
pub async fn get_client(login: &str) -> Option<Client> {
    let sessions = CONNECTED_CLIENTS.lock().await;
    sessions
        .values()
        .find_map(|session| session.get(login).cloned())
}

// Récupérer les clients d'une session avec filtrage (ou pas) du statut
pub async fn get_clients(session: &str, status: Option<UserStatus>) -> Vec<Client> {
    let sessions = CONNECTED_CLIENTS.lock().await;
    let clients = sessions
        .get(session)
        .map(|session| session.values().cloned().collect::<Vec<Client>>())
        .unwrap_or_default();
    if status.is_none() { return clients; } // No status filtering
    clients
        .into_iter()
        .filter(|client| client.has_status(status.as_ref().unwrap()))
        .collect()
}

// Récupérer la totalité des clients
pub async fn get_all_clients() -> Vec<Client> {
    let sessions = CONNECTED_CLIENTS.lock().await;
    sessions
        .values()
        .flat_map(|clients| clients.values().cloned())
        .collect()
}

// Récupérer les tuteurs d'une session (tuteurs et administrateurs)
pub async fn connected_tutors(session: &str) -> Vec<Client> {
    get_clients(session, None).await
        .into_iter()
        .filter(|client| client.is_tutor())
        .collect()
}

// Décrire la liste des clients d'une session
#[allow(dead_code)]
pub async fn describe_clients(session: &str) {
    let clients = get_clients(&session, None).await;
    for client in clients {
        client.describe();
    }
}

// Décrire la liste de tout les clients connectés
#[allow(dead_code)]
pub async fn describe_all_clients() {
    let clients = get_all_clients().await;
    for client in clients {
        client.describe();
    }
}

#[allow(dead_code)]
pub async fn describe_all_clients_to_log() {
    let clients = get_all_clients().await;
    for client in clients {
        client.describe_to_log();
    }
}

// Déconnecter tous les clients
pub async fn disconnect_all_clients() {
    let sessions = CONNECTED_CLIENTS.lock().await;
    for session in sessions.values() {
        for client in session.values() {
            let _ = client.disconnect();
        }
    }
}

// Forcer la déconnexion d'un client (éjection)
pub async fn force_disconnect(login: &str) {
    if let Some(client) = get_client(login).await {

        // Tentative de déconnexion normale
        if let Err(e) = client.disconnect().await {
            log::warn!("Failed to disconnect client {}: {}", login, e);
        }

        // Si le client est toujours dans la table des connectés
        // Nous somme en présence d'une connexion fantôme...

        if is_connected(&login).await {
            log::warn!("Force ejection of {} (ghost connection)...", login);
            // Retirer le client de la table des connectés
            remove_client(login).await; 
            // Informer les tuteurs de la session
            let _ = client.notify_tutors_client_exited().await;
            // Retirer les éventuels clients contrôlés
            if client.is_tutor() {
                remove_all_controlled_by_tutor(login).await; 
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Pour récupérer la liste des userData d'une session
// ----------------------------------------------------------------------------

use crate::database::UserData;
use crate::database::get_userdata_partial;

// Récupérer la liste des UserData d'une session (login à exclure)
pub async fn get_userdata(session: &str, login: &str) -> Vec<UserData> {
    let mut udata_list: Vec<UserData> = get_userdata_partial(session, login);
    // Compléter les champs manquants de chaque UserData de la liste
    for udata in &mut udata_list {
        udata.connected = is_connected(&udata.login).await;
        udata.help_flag = get_help_flag(&udata.login).await;
        udata.edited_file = get_edited_file(&udata.login).await;
    }
    udata_list
}

// Vérifier si un client est connecté
pub async fn is_connected(login: &str) -> bool {
    let sessions = CONNECTED_CLIENTS.lock().await;
    sessions.values().any(|session| session.contains_key(login))
}

// Récupérer le flag d'appel d'un client
pub async fn get_help_flag(login: &str) -> bool {
    if let Some(client) = get_client(login).await {
        return client.get_help_flag();
    }
    false // Client pas trouvé
}

// Récupérer le fichier en cours d'édition
pub async fn get_edited_file(login: &str) -> String {
    if let Some(client) = get_client(login).await {
        if let Some(file) = client.get_edited_file() {
            return file.clone();
        }
    }
    String::new() // Client pas trouvé ou pas de fichier en édition
}

// ----------------------------------------------------------------------------
// Nettoyage des dossiers orphelins
// ----------------------------------------------------------------------------
// Supprime les dossiers des clients qui n'existent plus dans USERS_DICO
// IMPORTANT: Si le client existe, on ne descend PAS dans son dossier
// Le contenu des dossiers clients ne doit jamais être supprimé !
//
// PROG_DIR/                 # racine des sessions
// ├── session1/             # sous-répertoire (session)
// │   ├── student1/         # dossier client (à vérifier)
// │   ├── student2/         # dossier client (à vérifier)
// │   └── old_student/      # dossier orphelin (à supprimer)
// ├── session2/             # sous-répertoire (session)
// │   ├── student3/         # dossier client (à vérifier)
// │   └── another_old/      # dossier orphelin (à supprimer)

use std::fs;
use std::path::Path;
use crate::database::check_login;

pub fn cleanup_orphaned_folders(prog_dir: &str) -> std::io::Result<()> {
    log::info!("Cleaning orphaned client folders");
    
    let prog_path = Path::new(prog_dir);
    if !prog_path.exists() {
        log::warn!("Directory {} does not exist, skipping cleanup", prog_dir);
        return Ok(());
    }
    
    // Explorer les sessions (niveau 1)
    let sessions = fs::read_dir(prog_path)?;
    
    for session_entry in sessions {
        let session_entry = session_entry?;
        let session_path = session_entry.path();
        
        if session_path.is_dir() {
            // Explorer les dossiers clients dans cette session (niveau 2)
            let clients = fs::read_dir(&session_path)?;
            
            for client_entry in clients {
                let client_entry = client_entry?;
                let client_path = client_entry.path();
                
                if client_path.is_dir() {
                    if let Some(folder_name) = client_path.file_name().and_then(|n| n.to_str()) {
                        if !check_login(folder_name) {
                            log::info!("Removing orphaned client folder: {}", client_path.display());
                            if let Err(e) = fs::remove_dir_all(&client_path) {
                                log::warn!("Failed to remove orphaned folder {}: {}", client_path.display(), e);
                            } 
                        }
                    }
                }
            }
        }
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Nettoyage des connexions zombies
// ----------------------------------------------------------------------------

// Ping silencieux pour tester le socket
async fn is_socket_alive(client: &Client) -> bool {
    let timeout = get_property_duration(CLEANUP_SOCKET_TIMEOUT);
    match tokio::time::timeout(timeout, client.ping()).await {

        // Cas de succès : Ok(Ok(())) => true
        //
        //   - Premier Ok : Le timeout n'a pas été dépassé
        //   - Deuxième Ok(()) : La méthode ping() s'est exécutée avec succès
        //   - Signification : La connexion est active et fonctionnelle
        //
        // Tous les autres cas : _ => false
        //
        // Ok(Err(e)) - Ping échoue mais dans les temps
        //   - Connexion fermée côté client => write_all() produit une erreur
        //   - Buffer de socket saturé => Erreur d'écriture
        //
        // Err(Elapsed) - Timeout dépassé
        //   - Client figé/bloqué => write_all() ne répond pas
        //   - Réseau très lent => Le ping prend trop de temps
        //   - Connexion partiellement fermée => Données bloquées dans les buffers

        Ok(Ok(())) => true,
        _ => false,
    }
}

// Nettoyage des connexions zombies
pub async fn cleanup_phantom_connections() {
    let mut to_remove = Vec::new();
    {
        let sessions = CONNECTED_CLIENTS.lock().await;
        for session in sessions.values() {
            for (login, client) in session {
                if !is_socket_alive(client).await {
                    to_remove.push(login.clone());
                }
            }
        }
    }
    for login in to_remove.into_iter() {
        log::warn!("Cleaning phantom connection: {}", login);
        force_disconnect(&login).await;
    }
}

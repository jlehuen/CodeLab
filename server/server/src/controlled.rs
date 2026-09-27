// ============================================================================
// Fichier : controlled.rs
// Version : 22/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// La table des users controlés est une structure de données partagée.
// Elle est protégée par Mutex pour éviter les problèmes de concurrence.

use std::{
    sync::Arc,
    collections::HashMap,
};

use tokio::sync::Mutex as TokioMutex;

use lazy_static::lazy_static;

use crate::connected::get_client;

// Type de la table de hashage des users controlés
// Le user n'est pas nécessairement dans CONNECTED_CLIENTS
// C'est pouquoi on utilise le login et pas une structure Client
// La clé est le login du user et la valeur est le login du tuteur
type ControlTable = Arc<TokioMutex<HashMap<String, String>>>;

// ----------------------------------------------------------------------------
// Table globale des users contrôlés protégée par Mutex
// ----------------------------------------------------------------------------

lazy_static! {
    pub static ref CONTROLLED_USERS: ControlTable
        = Arc::new(TokioMutex::new(HashMap::new()));
}

// ----------------------------------------------------------------------------
// Fonctions pour gérer la table des users contrôlés
// ----------------------------------------------------------------------------

// Recherche le login du controlleur d'un user
// Retourne None si le user n'est pas contrôlé
pub async fn get_controller(user_login: &str) -> Option<String> {
    let table = CONTROLLED_USERS.lock().await;
    table.get(user_login).cloned()
}

// Ajouter à la table un user contrôlé
pub async fn add_controlled_user(user_login: &str, tutor_login: &str) -> bool {
    let mut table = CONTROLLED_USERS.lock().await;
    if table.contains_key(user_login) {
        // Si le user est déjà contrôlé, on ne fait rien
        // En réalité, ce cas est déjà testé avant l'appel
       return false;
    }
    table.insert(user_login.to_string(), tutor_login.to_string());
    //println!("Table des clients contrôlés après ajout: {:#?}", *table);
    return true;
}

// Supprimer de la table un user contrôlé
pub async fn remove_controlled_user(user_login: &str) -> bool {
    let mut table = CONTROLLED_USERS.lock().await;
    if !table.contains_key(user_login) {
        // Si le client n'est pas contrôlé, on ne fait rien
        // En réalité, ce cas n'est pas censé se produire
        return false;
    }
    table.remove(user_login);
    //println!("Table des clients contrôlés après suppression: {:#?}", *table);
    return true;
}

// Supprimer de la table tous les users contrôlés par un tuteur donné
pub async fn remove_all_controlled_by_tutor(tutor_login: &str) {
    let mut table = CONTROLLED_USERS.lock().await;
    let mut to_remove = Vec::new();
    // Identifier les entrées à supprimer
    for (user_login, controller_login) in table.iter() {
        if controller_login == tutor_login {
            to_remove.push(user_login.clone());
        }
    }
    // Supprimer les entrées identifiées et notifier les clients
    for user_login in to_remove {
        table.remove(&user_login);
        // Notifier le client qu'il n'est plus sous contrôle (si connecté)
        if let Some(client) = get_client(&user_login).await {
            if let Err(e) = client.set_controlled(&false, "--").await {
                log::warn!("Failed to notify client {} that control was released: {}", user_login, e);
            }
        }
    }
}

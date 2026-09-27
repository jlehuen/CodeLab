// ============================================================================
// Fichier : hook.rs
// Version : 17/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================

use std::thread;
use std::process::exit;
use libc::{SIGINT, SIGTERM};
use signal_hook::iterator::Signals;

use crate::constants::*;
use crate::database::save_database;
use crate::mails::notification_email;

// ----------------------------------------------------------------------------
// Thread de gestion des signaux POSIX pour intercepter un kill
// ----------------------------------------------------------------------------

pub fn configure_hook() {
    // Créer un objet pour écouter des signaux POSIX
    let mut signals = Signals::new(&[SIGINT, SIGTERM])
        .expect("ERROR: Erreur lors de la création du gestionnaire de signaux");
    // Lancer un thread pour gérer les signaux
    thread::spawn(move || {
        for signal in signals.forever() {
            signal_hook(signal);
        }
    });
}

fn signal_hook(signal: i32) {
    let msg = match signal {
        libc::SIGINT => "Signal SIGINT received (interrupt from keyboard)",
        libc::SIGTERM => "Signal SIGTERM received (probably a kill from the system)",
        _unknown => "Unknown signal received",
    };
    log::info!("{}", msg);

    if signal == libc::SIGTERM {
        log::info!("Server shutdown");
        // Sauvegarder la base de données
        save_database(CONFIG_XML);
        // Envoyer un email de notification
        notification_email("The server is stopped", "The server has been stopped by SIGTERM");
        log::info!("Server stopped");
        exit(0); // Bye bye
    }
}

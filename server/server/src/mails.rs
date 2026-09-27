// ============================================================================
// Fichier : mails.rs
// Version : 22/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Module utilitaire simple pour l'envoi d'emails via SMTP

use lettre::{
    Message,
    Transport,
    SmtpTransport,
    transport::smtp::authentication::Credentials,
};

use crate::utils::decode_base64;
use crate::properties::get_property;
use crate::constants::*;

// ----------------------------------------------------------------------------
// Envoie un email via SMTP
// ----------------------------------------------------------------------------
// echo xxxxxxxxxx | base64
// base64 <<< xxxxxxxxxx

pub fn send_email(
    smtp_server_b64: &str,
    smtp_username_b64: &str,
    smtp_password_b64: &str,
    email_from: &str,
    email_to: &str,
    subject: &str,
    content: &str,
) -> bool {
    
    // Vérification préalable des paramètres
    if smtp_server_b64.is_empty() || smtp_username_b64.is_empty() || 
       smtp_password_b64.is_empty() || email_from.is_empty() || email_to.is_empty() {
        log::warn!("Email configuration incomplete, skipping notification");
        return false;
    }
    
    // Fonction interne qui gère les erreurs sans faire planter le serveur
    let send_impl = || -> Result<lettre::transport::smtp::response::Response, Box<dyn std::error::Error>> {
        
        // Décodage base64 avec logs détaillés
        let smtp_server = decode_base64(smtp_server_b64)
            .map_err(|e| {
                log::error!("Failed to decode SMTP server: {}", e);
                format!("Failed to decode SMTP server: {}", e)
            })?;
        let smtp_username = decode_base64(smtp_username_b64)
            .map_err(|e| {
                log::error!("Failed to decode SMTP username: {}", e);
                format!("Failed to decode SMTP username: {}", e)
            })?;
        let smtp_password = decode_base64(smtp_password_b64)
            .map_err(|e| {
                log::error!("Failed to decode SMTP password: {}", e);
                format!("Failed to decode SMTP password: {}", e)
            })?;

        //println!("smtp_server: {}", smtp_server);
        //println!("smtp_username: {}", smtp_username);
        //println!("smtp_password: {}", smtp_password);
        
        // Construction du message
        let email = Message::builder()
            .from(email_from.parse()?)
            .to(email_to.parse()?)
            .subject(subject)
            .body(content.to_string())?;

        // Transport SMTP avec timeout pour éviter les blocages
        let mailer = SmtpTransport::relay(&smtp_server)?
            .credentials(Credentials::new(smtp_username, smtp_password))
            .timeout(Some(std::time::Duration::from_secs(30)))
            .build();

        // Envoi
        Ok(mailer.send(&email)?)
    };

    // Gestion du résultat - JAMAIS DE PANIC
    match send_impl() {
        Ok(_response) => {
            log::info!("Email sent successfully to {} (subject: {})", email_to, subject);
            true
        }
        Err(e) => {
            // Log en tant que warning, pas erreur critique
            log::warn!("Failed to send email notification: {} (Server continues normally)", e);
            false
        }
    }
}

// ----------------------------------------------------------------------------
// Pour envoyer une notification par email
// ----------------------------------------------------------------------------

pub fn notification_email(subject: &str, message: &str) -> bool {
    let report_server = get_property(REPORT_SERVER);
    let report_username = get_property(REPORT_USERNAME);
    let report_password = get_property(REPORT_PASSWORD);
    let report_from = get_property(REPORT_FROM);
    let report_to = get_property(REPORT_TO);

    send_email(
        &report_server,
        &report_username,
        &report_password,
        &report_from,
        &report_to,
        subject,
        message,
    )
}

// ============================================================================
// Fichier : handler.rs
// Version : 21/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Traitement des messages reçus des clients Java

use std::error::Error;

use crate::client::Client;
use crate::messages::MessageFromClient;
use crate::constants::EMPTY_FILE_MARKER;
use crate::declare_commands;

type EmptyResult = Result<(), Box<dyn Error>>;

// ----------------------------------------------------------------------------
// Déclaration des commandes en provenance des clients Java
// ----------------------------------------------------------------------------

declare_commands!(
    PING,
    BIDULE,
    
    // Gestion des utilisateurs
    FORCE_DISCONNECT,
    FORCE_DISCONNECT_ALL,
    RESET_PASSWORD,
    CHANGE_PASSWORD,
    CHANGE_SESSION,
    ADD_NEW_STUDENT,

    // Gestion des fichiers
    NEW_FOLDER,
    NEW_FILE,
    DELETE_FILE,
    RENAME_FILE,
    MOVE_FILE,
    REQUEST_FILE,
    UPLOAD_FILE,

    // Gestion des sessions
    SET_SESSION_OPENNED,
    SET_EDITED_FILE,

    // Gestion du contrôle
    ASK_CONTROL_MODE,
    UPLOAD_CONTROLLED_FILE,

    // Communication
    SET_HELP_FLAG,
    RESET_HELP_FLAG,
    CHAT_TO,
    MESSAGE_TO,
    MESSAGE_TO_ALL,
    MESSAGE_GLOBAL,

    // Divers
    EXCEPTION_REPORT,
    LOG,
);

// ----------------------------------------------------------------------------
// Traitement des messages reçus des clients Java
// ----------------------------------------------------------------------------

pub async fn handle_message(client: &mut Client, message: MessageFromClient) -> EmptyResult {

    //let login = client.get_login().unwrap();
    //log::info!("Received from {}: {}", login, message.to_string());

    match message.cmd.as_str() {

        PING => {
            client.ping().await
        }
        BIDULE => {
            let arg1 = message.get_data_str(0)?;
            let arg2 = message.get_data_int(1)?;
            let arg3 = message.get_data_bool(2)?;
            client.bidule(&arg1, &arg2, &arg3).await
        }
        FORCE_DISCONNECT => {
            let user_login = message.get_data_str(0)?;
            client.force_disconnect(&user_login).await
        }
        FORCE_DISCONNECT_ALL => {
            client.disconnect_all().await
        }
        CHANGE_PASSWORD => {
            let pass_sha256 = message.get_data_str(0)?;
            client.change_password(&pass_sha256).await
        }
        RESET_PASSWORD => {
            let user_login = message.get_data_str(0)?;
            client.reset_password(&user_login).await
        }
        CHANGE_SESSION => {
            let session = message.get_data_str(0)?;
            client.change_session(&session).await
        }
        ADD_NEW_STUDENT => {
            let new_login = message.get_data_str(0)?;
            let new_name = message.get_data_str(1)?;
            client.add_new_student(&new_login, &new_name).await
        }
        CHAT_TO => {
            let dest_login = message.get_data_str(0)?;
            let msg = message.get_data_str(1)?;
            client.chat_to(&dest_login, &msg).await
        }
        MESSAGE_TO => {
            let dest_login = message.get_data_str(0)?;
            let msg = message.get_data_str(1)?;
            client.message_to(&dest_login, &msg).await
        }
        MESSAGE_TO_ALL => {
            let msg = message.get_data_str(0)?;
            client.message_to_all(&msg).await
        }
        MESSAGE_GLOBAL => {
            let msg = message.get_data_str(0)?;
            client.message_global(&msg).await
        }
        SET_SESSION_OPENNED => {
            let flag = message.get_data_bool(0)?;
            client.set_session_openned(&flag).await
        }
        SET_EDITED_FILE => {
            let name = message.get_data_str(0)?;
            // Pas de fichier édité si name == "EMPTY" => mettre edited_file à None
            let filename = if name == EMPTY_FILE_MARKER { None } else { Some(name.as_str()) };
            client.set_edited_file(filename).await
        }
        SET_HELP_FLAG => {
            let flag = message.get_data_bool(0)?;
            client.set_help_flag(&flag).await
        }
        RESET_HELP_FLAG => {
            let user_login = message.get_data_str(0)?;
            client.reset_help_flag(&user_login).await
        }
        NEW_FOLDER => {
            let path = message.get_data_str(0)?;
            client.new_folder(&path).await
        }
        NEW_FILE => {
            let path = message.get_data_str(0)?;
            client.new_file(&path).await
        }
        DELETE_FILE => {
            let path = message.get_data_str(0)?;
            client.delete_file(&path).await
        }
        RENAME_FILE => {
            let path = message.get_data_str(0)?;
            let new_name = message.get_data_str(1)?;
            client.rename_file(&path, &new_name).await
        }
        MOVE_FILE => {
            let path = message.get_data_str(0)?;
            let dest_path = message.get_data_str(1)?;
            client.move_file(&path, &dest_path).await
        }
        REQUEST_FILE => {
            let server_path = message.get_data_str(0)?;
            let client_path = message.get_data_str(1)?;
            client.request_file(&server_path, &client_path).await
        }
        UPLOAD_FILE => {
            let path = message.get_data_str(0)?;
            let file_content: &[u8] = message.get_data_bytes(1)?;
            client.upload_file(&path, &file_content).await
        }
        LOG => {
            let txt = message.get_data_str(0)?;
            client.log(&txt).await
        }
        EXCEPTION_REPORT => {
            let content = message.get_data_str(0)?;
            client.exception_report(&content).await
        }
        ASK_CONTROL_MODE => {
            let user_login = message.get_data_str(0)?;
            let flag = message.get_data_bool(1)?;
            client.ask_control_mode(&user_login, &flag).await
        }
        UPLOAD_CONTROLLED_FILE => {
            let path = message.get_data_str(0)?;
            let file_content: &[u8] = message.get_data_bytes(1)?;
            let user_login = message.get_data_str(2)?;
            client.upload_controlled_file(&path, &file_content, &user_login).await
        }

        // -----------------------------------------------
        // Erreur commande inconnue

        _unknown => {
            let cmd = message.cmd.as_str();
            let log = client.get_login().expect("ERROR");
            let msg = format!("Unknown command {} received from {}", cmd, log);
            log::error!("ERROR: {}", msg);
            Err(msg.into())
        }
    }
}

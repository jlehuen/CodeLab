// ============================================================================
// Fichier : messages.rs
// Version : 23/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Ce module définit les structures de données utilisées pour communiquer avec
// avec les clients Java. Les messages sont sérialisés en MessagePack.
// MessagePack est un format binaire compact pour sérialiser et désérialiser
// des données complexes de manière plus rapide et plus légère qu'avec JSON.

use std::error::Error;

// Pour MessagePack
use serde_json::Value;
use serde::{Serialize, Deserialize};

// ----------------------------------------------------------------------------
// Structure des messages en provenance des clients Java
// ----------------------------------------------------------------------------

#[derive(Debug, Serialize, Deserialize)]
pub struct MessageFromClient {
    pub cmd: String,
    data: Vec<Data>,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "type", content = "value")]
enum Data {
    Bool(bool),
    Number(i64),
    String(String),
    Binary(Vec<u8>),
}

impl MessageFromClient {

    #[allow(dead_code)]
    pub fn to_string(&self) -> String {
        // Méthode d'affichage du message pour le débuggage
        let truncated_data: Vec<String> = self.data.iter().map(|data| {
            match data {
                Data::Bool(b) => format!("Bool({})", b),
                Data::Number(n) => format!("Number({})", n),
                Data::String(s) => format!("String(\"{}\")", s),
                Data::Binary(bytes) => {
                    let preview_size = std::cmp::min(bytes.len(), 16); // Limiter à 16 octets pour la lisibilité
                    let hex_preview: Vec<String> = bytes[..preview_size]
                        .iter()
                        .map(|b| format!("{:02x}", b))
                        .collect();
                    
                    if bytes.len() <= 64 {
                        format!("Binary([{}])", hex_preview.join(" "))
                    } else {
                        format!("Binary([{} bytes: {} ...])", bytes.len(), hex_preview.join(" "))
                    }
                }
            }
        }).collect();
        
        format!("{}{:?}", self.cmd, truncated_data)
    }

    // ----------------------------------------------------------------------------
    // Méthodes de récupération des données d'un message

    // Récupère une chaîne de caractères
    pub fn get_data_str(&self, index: usize) -> Result<String, Box<dyn Error>> {
        match self.data.get(index) {
            Some(Data::String(s)) => Ok(s.clone()),
            _ => Err(format!("Expected a string at index {} but found nothing or wrong type.", index).into()),
        }
    }

    // Récupère un entier 64 bits
    pub fn get_data_int(&self, index: usize) -> Result<i64, Box<dyn Error>> {
        match self.data.get(index) {
            Some(Data::Number(n)) => Ok(*n),
            _ => Err(format!("Expected an integer at index {} but found nothing or wrong type.", index).into()),
        }
    }

    // Récupère un booléen
    pub fn get_data_bool(&self, index: usize) ->  Result<bool, Box<dyn Error>> {
        match self.data.get(index) {
            Some(Data::Bool(b)) => Ok(*b),
            _ => Err(format!("Expected a boolean at index {} but found nothing or wrong type.", index).into()),
        }
    }

    // Récupère un tableau d'octets
    pub fn get_data_bytes(&self, index: usize) -> Result<&[u8], Box<dyn Error>> {
        match self.data.get(index) {
            Some(Data::Binary(bytes)) => Ok(bytes),
            _ => Err(format!("Expected binary data at index {} but found nothing or wrong type.", index).into()),
        }
    }
}

// ----------------------------------------------------------------------------
// Structure des messages à destination des clients Java
// ----------------------------------------------------------------------------

#[derive(Debug, Serialize, Deserialize)]
pub struct MessageToClient {
    cmd: CommandType, // Nom de la commande
    nbargs: usize,    // Nombre d'arguments
    data: Vec<Value>, // Liste d'arguments
}

impl MessageToClient {
    // Constructeur de message
    pub fn new(cmd: CommandType, data: Vec<Value>) -> Self {
        MessageToClient {cmd, nbargs: data.len(), data}
    }
}

// ----------------------------------------------------------------------------
// Macro pour créer un vecteur de Values à partir d'arguments hétérogènes

#[macro_export]
macro_rules! create_data {
    ($($value:expr),*) => {
        vec![$(serde_json::to_value($value).unwrap()),*]
    };
}

// ----------------------------------------------------------------------------
// Énumaration des commandes à destination des clients

#[allow(non_camel_case_types)]
#[derive(Debug, Serialize, Deserialize)]
pub enum CommandType {

    // Tests
    PONG,
    HELLO,

    // Générales
    INIT_SESSION,
    OPEN_SESSION,
    CLIENT_ARRIVED,
    CLIENT_EXITED,
    NEW_STUDENT,
    SET_EDITED_FILE,
    SERVER_SHUTDOWN,

    // Communication
    CHAT_FROM,
    MESSAGE_FROM,
    MESSAGE_FROM_SERVER,
    MESSAGE_TO_CONSOLE,

    // Appel
    SET_HELP_FLAG,
    RESET_HELP_FLAG,

     // Contrôle
    SET_CONTROLLED,
    SET_CONTROL_MODE,

    // Fichiers
    NEW_FILE,
    NEW_FOLDER,
    DELETE_FILE,
    RENAME_FILE,
    MOVE_FILE,
    UPDATE_FILE,
    UPDATE_FILE_CONTROLLED,
}

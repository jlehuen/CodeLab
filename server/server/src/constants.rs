// ============================================================================
// Fichier : constants.rs
// Version : 11/09/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================

// Fichier de configuration
pub const CONFIG_FILE: &str = "config/server.properties";

// Fichiers d'exemples de configuration
pub const CONFIG_FILE_EXAMPLE: &str = "config/server.properties.example";
pub const CONFIG_XML_EXAMPLE: &str = "config/sessions.example.xml";

// Emplacements des fichiers et dossiers
pub const CONFIG_XML: &str = "config/sessions.xml";
pub const LOG_FILE: &str = "logs/server.log";
pub const PROG_DIR: &str = "sessions";
pub const TEMP_DIR: &str = "temp";
pub const LOG_DIR: &str = "logs";

// Configuration des buffers
pub const STR_BUFFER_SIZE: usize = 1024; // Taille du buffer de la fonction read_string
pub const MESSAGE_MAX_SIZE: usize = 1024 * 64; // Taille du buffer MessagePack (64 Ko)

// Template de l'interface de monitoring
pub const ADMIN_TEMPLATE_PATH: &str = "templates/admin_dashboard.html";

// Keys des properties
pub const SERVER_ADDR: &str = "server_addr";
pub const SERVER_NAME: &str = "server_name";
pub const SERVER_MAGIC_PASSWORD: &str = "server_magic_password";
pub const ADMIN_ADDR: &str = "admin_addr";
pub const ADMIN_USERNAME: &str = "admin_username";
pub const ADMIN_PASSWORD_HASH: &str = "admin_password_hash";

//pub const TIMEOUT_READ: &str = "timeout_read";
//pub const TIMEOUT_WRITE: &str = "timeout_write";

pub const CLEANUP_PHANTOM_INTERVAL: &str = "cleanup_phantom_interval";
pub const CLEANUP_SOCKET_TIMEOUT: &str = "cleanup_socket_timeout";

pub const REPORT_STATUS: &str = "report_status";
pub const REPORT_SERVER: &str = "report_server";
pub const REPORT_USERNAME: &str = "report_username";
pub const REPORT_PASSWORD: &str = "report_password";
pub const REPORT_FROM: &str = "report_from";
pub const REPORT_TO: &str = "report_to";

// Diverses constantes
pub const EMPTY_FILE_MARKER: &str = "EMPTY";

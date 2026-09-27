// ============================================================================
// Fichier : utils.rs
// Version : 16/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================

pub fn buffer_to_str(buffer: &[u8], n: usize) -> String {
    return String::from_utf8_lossy(&buffer[..n]).trim_end().to_string();
}

// ----------------------------------------------------------------------------
// Fonction utilitaire qui décode une chaîne base64
// ----------------------------------------------------------------------------

use base64::{Engine as _, engine::general_purpose};

pub fn decode_base64(encoded: &str) -> Result<String, Box<dyn std::error::Error>> {
    let cleaned = encoded.trim(); // Enlever les espaces et retours à la ligne
    let bytes = general_purpose::STANDARD.decode(cleaned)?;
    let decoded = String::from_utf8(bytes)?;
    Ok(decoded.trim().to_string())
}

// ----------------------------------------------------------------------------
// Fonction utilitaire qui encode une chaîne en SHA256 base64
// ----------------------------------------------------------------------------

use sha2::{Digest, Sha256};

pub fn encode_sha256(input: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(input);
    let result = hasher.finalize();
    general_purpose::STANDARD.encode(result)
}

// ----------------------------------------------------------------------------
// Fonction utilitaire qui retourne un Vec<u8> à partir d'un nom de fichier
// ----------------------------------------------------------------------------

use std::fs::File;
use std::io::{self, Read};

pub fn read_all_bytes(filename: &str) -> Result<Vec<u8>, io::Error> {
    let mut file = File::open(filename)?;
    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer)?;
    Ok(buffer)
}

// ----------------------------------------------------------------------------
// Fonction utilitaire qui crée un dossier s'il n'existe pas
// ----------------------------------------------------------------------------
// Le paramètre peut être un &str ou un String indifféremment

use std::fs;
use std::path::Path;

pub fn ensure_dir_exists<P>(path: P) -> io::Result<()>
where P: AsRef<Path>,
{
    let path = path.as_ref();
    if !path.exists() {
        fs::create_dir_all(path)?;
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Fonction utilitaire pour compresser un dossier avec TAR + ZStandard
// ----------------------------------------------------------------------------
// Niveaux 1-2 : rapide mais compression modeste
// Niveaux 3-5 : bon équilibre vitesse/compression
// Niveaux 6-9 : compression plus forte mais plus lente
// Niveaux 10+ : compression maximale mais très lente

use std::error::Error as StdError;
use tar::Builder;
use zstd::Encoder;

pub fn compress_archive(
    dir_path: &str, 
    output_file: &str,
    compression_level: Option<i32>
) -> Result<(), Box<dyn StdError + Send + Sync>> {

    let output = File::create(output_file)?;
    let level = compression_level.unwrap_or(5);
    let encoder = Encoder::new(output, level)?;
    let mut archive = Builder::new(encoder);
    archive.append_dir_all(".", dir_path)?;
    archive.finish()?; // Finaliser l'archive TAR
    let encoder = archive.into_inner()?;
    encoder.finish()?; // Finaliser la compression
    Ok(())
}

// ----------------------------------------------------------------------------
// Fonction de configuration et initialisation d'un logger
// ----------------------------------------------------------------------------

use std::error::Error;
use log::LevelFilter;
use log4rs::{
    append::file::FileAppender,
    config::{Appender, Config, Root},
    encode::pattern::PatternEncoder,
    filter::threshold::ThresholdFilter,
};

pub fn configure_logger(path: &str) -> Result<(), Box<dyn Error>> {

    // Configure le file appender
    let file_appender = FileAppender::builder()
        .encoder(Box::new(PatternEncoder::new("{d(%Y-%m-%d %H:%M:%S)} - {m}{n}")))
        .build(path)?;

    // Configure la racine du logger
    let config = Config::builder()
        .appender(
            Appender::builder()
                .filter(Box::new(ThresholdFilter::new(LevelFilter::Info)))
                .build("file", Box::new(file_appender)),
        )
        .build(Root::builder().appender("file").build(LevelFilter::Info))?;

    // Initialiser le logger
    log4rs::init_config(config)?;

    Ok(())
}

// ----------------------------------------------------------------------------
// Macros permettant de définir des séries de constantes de type &str
// ----------------------------------------------------------------------------

#[macro_export]
macro_rules! define {
    ($($name:ident => $value:expr),*) => {
        $(
            pub const $name: &str = $value;
        )*
    };
}

#[macro_export]
macro_rules! declare_commands{
    ($($name:ident),* $(,)?) => {
        $(
            const $name: &str = stringify!($name);
        )*
    };
}

// ----------------------------------------------------------------------------

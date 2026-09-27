// ============================================================================
// Fichier : properties.rs
// Version : 17/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Module de chargement des propriétés depuis un fichier TOML

use std::fs;
use std::sync::RwLock;
use std::time::Duration;
use lazy_static::lazy_static;

lazy_static! {
    static ref CONFIG: RwLock<Option<toml::Value>> = RwLock::new(None);
}

// ----------------------------------------------------------------------------
// Accès aux propriétés définies dans CONFIG
// ----------------------------------------------------------------------------

#[allow(non_snake_case)]
pub fn get_property(key: &str) -> String {
    let config_guard = CONFIG.read().unwrap();
    
    if let Some(config) = config_guard.as_ref() {
        // 1. Essayer d'abord la correspondance directe section_clé (ex: admin_password_hash => [admin] password_hash)
        if let Some((section, prop)) = key.split_once('_') {
            if let Some(table) = config.get(section) {
                if let Some(value) = table.get(prop) {
                    return match value {
                        toml::Value::String(s) => s.clone(),
                        toml::Value::Integer(i) => i.to_string(),
                        toml::Value::Float(f) => f.to_string(),
                        toml::Value::Boolean(b) => b.to_string(),
                        _ => value.to_string(),
                    };
                }
            }
        }

        // 2. Naviguer récursivement dans la structure TOML si besoin
        let keys: Vec<&str> = key.split('_').collect();
        let mut current = config;
        
        for k in keys {
            match current.get(k) {
                Some(value) => current = value,
                None => return "ERROR".to_string(),
            }
        }
        
        // Extraire la valeur selon son type
        match current {
            toml::Value::String(s) => s.clone(),
            toml::Value::Integer(i) => i.to_string(),
            toml::Value::Float(f) => f.to_string(),
            toml::Value::Boolean(b) => b.to_string(),
            _ => current.to_string(),
        }
    } else {
        "ERROR".to_string()
    }
}

pub fn get_property_duration(key: &str) -> Duration {
    let seconds_str = get_property(key);
    let seconds: u64 = seconds_str.parse().unwrap_or(30);
    Duration::from_secs(seconds)
}

// ----------------------------------------------------------------------------
// Chargement des propriétés depuis un fichier TOML
// ----------------------------------------------------------------------------

pub fn load_properties(file_path: &str) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Loading configuration from {}", file_path);

    let content = fs::read_to_string(file_path)?;
    let toml_value: toml::Value = toml::from_str(&content)?;
    
    let mut config_guard = CONFIG.write().unwrap();
    if config_guard.is_some() {
        return Err("Configuration already loaded".into());
    }
    *config_guard = Some(toml_value);

    Ok(())
}

// ----------------------------------------------------------------------------
// Affichage des propriétés chargées dans CONFIG
// ----------------------------------------------------------------------------

pub fn print_properties() {

    println!("============================================");
    println!("Server configuration");
    println!("============================================");

    let config_guard = CONFIG.read().unwrap();
    if let Some(config) = config_guard.as_ref() {
        if let toml::Value::Table(table) = config {
            for (section_key, section_value) in table {
                if let toml::Value::Table(section_table) = section_value {
                    // Parcourir chaque section (admin, report, server, etc.)
                    for (key, value) in section_table {
                        let full_key = format!("{}_{}", section_key, key);
                        match value {
                            toml::Value::String(s) => println!("{} = \"{}\"", full_key, s),
                            toml::Value::Integer(i) => println!("{} = {}", full_key, i),
                            toml::Value::Float(f) => println!("{} = {}", full_key, f),
                            toml::Value::Boolean(b) => println!("{} = {}", full_key, b),
                            _ => println!("{} = {}", full_key, value),
                        }
                    }
                } else {
                    // Valeur simple au niveau racine
                    match section_value {
                        toml::Value::String(s) => println!("{} = \"{}\"", section_key, s),
                        toml::Value::Integer(i) => println!("{} = {}", section_key, i),
                        toml::Value::Float(f) => println!("{} = {}", section_key, f),
                        toml::Value::Boolean(b) => println!("{} = {}", section_key, b),
                        _ => println!("{} = {}", section_key, section_value),
                    }
                }
            }
        }
    } else {
        println!("No configuration loaded");
    }
}

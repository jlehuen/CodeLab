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

// ----------------------------------------------------------------------------
// Modification du mot de passe administrateur (mise à jour RAM + persistance)
// ----------------------------------------------------------------------------

pub fn set_admin_password_hash(new_hash: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // 1. Mise à jour dans CONFIG en mémoire vive
    {
        let mut config_guard = CONFIG.write().unwrap();
        if let Some(ref mut toml_val) = *config_guard {
            if let Some(table) = toml_val.as_table_mut() {
                if let Some(admin_val) = table.get_mut("admin") {
                    if let Some(admin_table) = admin_val.as_table_mut() {
                        admin_table.insert("password_hash".to_string(), toml::Value::String(new_hash.to_string()));
                    }
                }
            }
        }
    }

    // 2. Persistance dans config/server.properties
    let config_path = crate::constants::CONFIG_FILE;
    let file_path = if std::path::Path::new(config_path).exists() {
        config_path
    } else if std::path::Path::new(crate::constants::CONFIG_FILE_EXAMPLE).exists() {
        crate::constants::CONFIG_FILE_EXAMPLE
    } else {
        config_path
    };

    let content = fs::read_to_string(file_path)?;
    let mut in_admin_section = false;
    let mut replaced = false;
    let mut new_lines = Vec::new();

    for line in content.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with('[') && trimmed.ends_with(']') {
            in_admin_section = trimmed == "[admin]";
        }
        if in_admin_section && trimmed.starts_with("password_hash") && trimmed.contains('=') {
            new_lines.push(format!("password_hash = \"{}\"", new_hash));
            replaced = true;
        } else {
            new_lines.push(line.to_string());
        }
    }

    if !replaced {
        let mut final_lines = Vec::new();
        let mut inserted = false;
        for line in new_lines {
            let trimmed = line.trim();
            if trimmed.starts_with('[') && trimmed.ends_with(']') && trimmed == "[admin]" {
                final_lines.push(line);
                final_lines.push(format!("password_hash = \"{}\"", new_hash));
                inserted = true;
            } else {
                final_lines.push(line);
            }
        }
        if !inserted {
            final_lines.push("\n[admin]".to_string());
            final_lines.push(format!("password_hash = \"{}\"", new_hash));
        }
        new_lines = final_lines;
    }

    // Backup daté ou .bak
    let _ = fs::copy(file_path, format!("{}.bak", file_path));
    fs::write(file_path, new_lines.join("\n") + "\n")?;
    log::info!("Admin password hash updated successfully in {} and in memory", file_path);

    Ok(())
}

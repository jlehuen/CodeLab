// ============================================================================
// Fichier : backups.rs
// Version : 11/06/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================

use std::fs;
use std::path::Path;
use std::path::PathBuf;
use std::collections::HashMap;

use chrono::{NaiveDateTime, Duration, Utc};
use regex::Regex;

type EmptyResult = Result<(), Box<dyn std::error::Error + Send + Sync>>;

// ----------------------------------------------------------------------------
// Suppression des fichiers de backup correspondant à un fichier donné
// ----------------------------------------------------------------------------
// Supprime tous les fichiers de backup correspondant au fichier spécifié
// Format des backups : filename_YYMMDD_HHMMSS_bak.extension.bak
// Génération du code par IA Anthropic Claude

pub fn delete_backup_files(file_path: &Path) -> EmptyResult {

    // Extraire le nom du fichier sans extension et l'extension
    let file_name = file_path.file_stem()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire le nom du fichier")?;
    
    let extension = file_path.extension()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire l'extension du fichier")?;
    
    // Obtenir le dossier parent
    let parent_dir = file_path.parent()
        .ok_or("Impossible d'obtenir le dossier parent")?;
    
    // Pattern pour les fichiers de backup
    let backup_pattern = format!(r"^{}_\d{{6}}_\d{{6}}_bak\.{}\.bak$", 
        regex::escape(file_name), 
        regex::escape(extension));
    
    let regex = Regex::new(&backup_pattern)?;
    
    // Lire le contenu du dossier
    let dir_entries = fs::read_dir(parent_dir)?;
    let mut backup_files_deleted = 0;
    
    // Parcourir les entrées
    for entry in dir_entries {
        let entry = entry?;
        let entry_path = entry.path();
        
        if let Some(entry_name) = entry_path.file_name().and_then(|s| s.to_str()) {
            if regex.is_match(entry_name) {
                match fs::remove_file(&entry_path) {
                    Ok(()) => {
                        log::info!("Deleted backup file: {}", entry_path.display());
                        backup_files_deleted += 1;
                    }
                    Err(e) => {
                        log::warn!("Failed to delete backup file {}: {}", entry_path.display(), e);
                    }
                }
            }
        }
    }
    
    if backup_files_deleted > 0 {
        log::info!("Deleted {} backup files for {}", backup_files_deleted, file_path.display());
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Déplacement des fichiers de backup correspondant à un fichier donné
// ----------------------------------------------------------------------------
// Déplace tous les fichiers de backup correspondant au fichier spécifié
// Format des backups : filename_YYMMDD_HHMMSS_bak.extension.bak
// Génération du code par IA Anthropic Claude

pub fn move_backup_files(old_file_path: &Path, new_file_path: &Path) -> EmptyResult {
    
    // Extraire le nom du fichier sans extension et l'extension pour l'ancien fichier
    let old_file_name = old_file_path.file_stem()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire le nom de l'ancien fichier")?;
    
    let old_extension = old_file_path.extension()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire l'extension de l'ancien fichier")?;
    
    // Extraire le nom du fichier sans extension et l'extension pour le nouveau fichier
    let new_file_name = new_file_path.file_stem()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire le nom du nouveau fichier")?;
    
    let new_extension = new_file_path.extension()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire l'extension du nouveau fichier")?;
    
    // Obtenir le dossier parent de l'ancien fichier
    let old_parent_dir = old_file_path.parent()
        .ok_or("Impossible d'obtenir le dossier parent de l'ancien fichier")?;
    
    // Obtenir le dossier parent du nouveau fichier
    let new_parent_dir = new_file_path.parent()
        .ok_or("Impossible d'obtenir le dossier parent du nouveau fichier")?;
    
    // Pattern pour les fichiers de backup de l'ancien fichier
    let backup_pattern = format!(r"^{}_\d{{6}}_\d{{6}}_bak\.{}\.bak$", 
        regex::escape(old_file_name), 
        regex::escape(old_extension));
    
    let regex = Regex::new(&backup_pattern)?;
    
    // Lire le contenu du dossier source
    let dir_entries = fs::read_dir(old_parent_dir)?;
    let mut backup_files_moved = 0;
    
    // Parcourir les entrées
    for entry in dir_entries {
        let entry = entry?;
        let entry_path = entry.path();
        
        if let Some(entry_name) = entry_path.file_name().and_then(|s| s.to_str()) {
            if regex.is_match(entry_name) {
                // Extraire la partie timestamp du nom de fichier de backup
                // Format: old_filename_YYMMDD_HHMMSS_bak.old_extension.bak
                if let Some(captures) = Regex::new(&format!(r"^{}_(\d{{6}}_\d{{6}})_bak\.{}\.bak$", 
                    regex::escape(old_file_name), 
                    regex::escape(old_extension)))?.captures(entry_name) {
                    
                    let timestamp = captures.get(1).unwrap().as_str();
                    
                    // Construire le nouveau nom de fichier de backup
                    let new_backup_name = format!("{}_{}_{}.{}.bak", 
                        new_file_name, timestamp, "bak", new_extension);
                    
                    let new_backup_path = new_parent_dir.join(new_backup_name);
                    
                    // Déplacer le fichier de backup
                    match fs::rename(&entry_path, &new_backup_path) {
                        Ok(()) => {
                            log::info!("Moved backup file: {} -> {}", 
                                entry_path.display(), new_backup_path.display());
                            backup_files_moved += 1;
                        }
                        Err(e) => {
                            log::warn!("Failed to move backup file {} to {}: {}", 
                                entry_path.display(), new_backup_path.display(), e);
                        }
                    }
                }
            }
        }
    }
    
    if backup_files_moved > 0 {
        log::info!("Moved {} backup files from {} to {}", 
            backup_files_moved, old_file_path.display(), new_file_path.display());
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Renommage des fichiers de backup correspondant à un fichier donné
// ----------------------------------------------------------------------------
// Renomme tous les fichiers de backup correspondant au fichier spécifié
// Format des backups : filename_YYMMDD_HHMMSS_bak.extension.bak
// Génération du code par IA Anthropic Claude

pub fn rename_backup_files(old_file_path: &Path, new_file_name: &str) -> EmptyResult {
    
    // Extraire le nom du fichier sans extension et l'extension pour l'ancien fichier
    let old_file_name = old_file_path.file_stem()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire le nom de l'ancien fichier")?;
    
    let old_extension = old_file_path.extension()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire l'extension de l'ancien fichier")?;
    
    // Construire le nouveau chemin de fichier pour extraire le nom et l'extension
    let parent_dir = old_file_path.parent()
        .ok_or("Impossible d'obtenir le dossier parent")?;
    
    let new_file_path = parent_dir.join(new_file_name);
    
    // Extraire le nom du fichier sans extension et l'extension pour le nouveau fichier
    let new_file_stem = new_file_path.file_stem()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire le nom du nouveau fichier")?;
    
    let new_extension = new_file_path.extension()
        .and_then(|s| s.to_str())
        .ok_or("Impossible d'extraire l'extension du nouveau fichier")?;
    
    // Pattern pour les fichiers de backup de l'ancien fichier
    let backup_pattern = format!(r"^{}_\d{{6}}_\d{{6}}_bak\.{}\.bak$", 
        regex::escape(old_file_name), 
        regex::escape(old_extension));
    
    let regex = Regex::new(&backup_pattern)?;
    
    // Lire le contenu du dossier
    let dir_entries = fs::read_dir(parent_dir)?;
    let mut backup_files_renamed = 0;
    
    // Parcourir les entrées
    for entry in dir_entries {
        let entry = entry?;
        let entry_path = entry.path();
        
        if let Some(entry_name) = entry_path.file_name().and_then(|s| s.to_str()) {
            if regex.is_match(entry_name) {
                // Extraire la partie timestamp du nom de fichier de backup
                // Format: old_filename_YYMMDD_HHMMSS_bak.old_extension.bak
                if let Some(captures) = Regex::new(&format!(r"^{}_(\d{{6}}_\d{{6}})_bak\.{}\.bak$", 
                    regex::escape(old_file_name), 
                    regex::escape(old_extension)))?.captures(entry_name) {
                    
                    let timestamp = captures.get(1).unwrap().as_str();
                    
                    // Construire le nouveau nom de fichier de backup
                    let new_backup_name = format!("{}_{}_{}.{}.bak", 
                        new_file_stem, timestamp, "bak", new_extension);
                    
                    let new_backup_path = parent_dir.join(new_backup_name);
                    
                    // Renommer le fichier de backup
                    match fs::rename(&entry_path, &new_backup_path) {
                        Ok(()) => {
                            log::info!("Renamed backup file: {} -> {}", 
                                entry_path.display(), new_backup_path.display());
                            backup_files_renamed += 1;
                        }
                        Err(e) => {
                            log::warn!("Failed to rename backup file {} to {}: {}", 
                                entry_path.display(), new_backup_path.display(), e);
                        }
                    }
                }
            }
        }
    }
    
    if backup_files_renamed > 0 {
        log::info!("Renamed {} backup files for {} to {}", 
            backup_files_renamed, old_file_path.display(), new_file_name);
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Nettoyage des backups selon la stratégie suivante
// ----------------------------------------------------------------------------
//  - Conserver tous les fichiers de moins d'une minute
//  - Conserver la dernière sauvegarde par minute pour l'heure courante
//  - Conserver la dernière sauvegarde par heure pour le jour courant
//  - Conserver la dernière sauvegarde par jour pour la semaine courante
//  - Conserver la dernière sauvegarde par semaine pour le mois courant
//  - Conserver la dernière sauvegarde par mois pour l'année courante
//  - Conserver la dernière sauvegarde par année pour les plus anciens

pub fn clean_backups(base: &str) -> std::io::Result<()> {
    clean_dir(base)?;

    // Parcourir récursivement les sous-dossiers
    for entry in fs::read_dir(base)? {
        let entry = entry?;
        let path = entry.path();
        
        if path.is_dir() {
            // Appel récursif pour chaque sous-dossier
            if let Some(subdir_path) = path.to_str() {
                clean_backups(subdir_path)?;
            }
        }
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Nettoyer le contenu d'un dossier
// ----------------------------------------------------------------------------

fn clean_dir(dir_path: &str) -> std::io::Result<()> {
    let mut backup_files: Vec<BackupFile> = Vec::new();

    // Lecture des fichiers et extraction des critères
    for entry in fs::read_dir(dir_path)? {
        let entry = entry?;
        let path = entry.path();
        if let Some(filename) = path.file_name().and_then(|n| n.to_str()) {
            if let Some(mut backup_file) = extract_file_components(filename) {
                backup_file.path = path; // Ajout du chemin
                backup_files.push(backup_file);
            }
        }
    }

    // Regrouper les fichiers par préfixe et suffixe
    let mut grouped_files: HashMap<(String, String), Vec<BackupFile>> = HashMap::new();
    for file in backup_files {
        grouped_files
            .entry((file.prefix.clone(), file.suffix.clone()))
            .or_default()
            .push(file);
    }

    // Traiter chaque groupe un par un
    for files in grouped_files.values_mut() {
        files.sort_by(|a, b| b.datetime.cmp(&a.datetime));

        let now = Utc::now().naive_utc();
        let mut to_keep = Vec::new();

        for (i, file) in files.iter().enumerate() {
            let age = now - file.datetime;

            if age < Duration::minutes(1) {
                to_keep.push(i); // Garder tous les fichiers de moins d'une minute
            }
            else if age < Duration::hours(1) {
                if !to_keep.iter().any(|&j| same_minute(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par minute pour l'heure courante
                }
            }
            else if age < Duration::days(1) {
                if !to_keep.iter().any(|&j| same_hour(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par heure pour le jour courant
                }
            }
            else if age < Duration::weeks(1) {
                if !to_keep.iter().any(|&j| same_day(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par jour pour la semaine courante
                }
            }
            else if age < Duration::days(31) {
                if !to_keep.iter().any(|&j| same_week(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par semaine pour le mois courant
                }
            }
            else if age < Duration::days(365) {
                if !to_keep.iter().any(|&j| same_month(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par mois pour l'année courante
                }
            }
            else {
                if !to_keep.iter().any(|&j| same_year(files[j].datetime, file.datetime)) {
                    to_keep.push(i); // Garder le dernier par année pour les plus anciens
                }
            }
        }

        // Supprimer les fichiers non conservés
        for (i, file) in files.iter().enumerate() {
            if !to_keep.contains(&i) {
                log::info!("Delete backup file: {}", file.path.display());
                fs::remove_file(&file.path)?;
            }
        }
    }

    Ok(())
}

// ----------------------------------------------------------------------------
// Extraction des caractéristiques d'un filename
// ----------------------------------------------------------------------------
// Format des filenames = filename_YYMMDD_HHMMSS_bak.extension.bak

#[derive(Debug)]
struct BackupFile {
    path: PathBuf,
    prefix: String,
    suffix: String,
    datetime: NaiveDateTime,
}

fn extract_file_components(filename: &str) -> Option<BackupFile> {
    let re = Regex::new(r"^(.+)_(\d{6}_\d{6})_(.+)$").unwrap();

    re.captures(filename).and_then(|caps| {
        let prefix = caps.get(1)?.as_str().to_string();
        let datetime = NaiveDateTime::parse_from_str(
            caps.get(2)?.as_str(),
            "%y%m%d_%H%M%S",
        ).ok()?;
        let suffix = caps.get(3)?.as_str().to_string();

        Some(BackupFile {
            path: PathBuf::new(),
            prefix,
            suffix,
            datetime,
        })
    })
}

// ----------------------------------------------------------------------------
// Fonctions de comparaison de NaiveDateTime
// ----------------------------------------------------------------------------

fn same_minute(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y%m%d%H%M").to_string() ==
    dt2.format("%Y%m%d%H%M").to_string()
}

fn same_hour(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y%m%d%H").to_string() ==
    dt2.format("%Y%m%d%H").to_string()
}

fn same_day(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y%m%d").to_string() ==
    dt2.format("%Y%m%d").to_string()
}

fn same_week(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y%W").to_string() ==
    dt2.format("%Y%W").to_string()
}

fn same_month(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y%m").to_string() ==
    dt2.format("%Y%m").to_string()
}

fn same_year(dt1: NaiveDateTime, dt2: NaiveDateTime) -> bool {
    dt1.format("%Y").to_string() ==
    dt2.format("%Y").to_string()
}

use std::fs;
use std::path::Path;
use std::process::Command;

fn main() {
    // 1. Source de vérité unique : fichier VERSION (racine monorepo ../../VERSION ou local ./VERSION)
    println!("cargo:rerun-if-changed=../../VERSION");
    println!("cargo:rerun-if-changed=VERSION");

    let version = fs::read_to_string("../../VERSION")
        .or_else(|_| fs::read_to_string("VERSION"))
        .ok()
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| env!("CARGO_PKG_VERSION").to_string());

    // 2. Numéro de build horodaté (AAMMJJHHMM, même format que le client Ant)
    let date_build = Command::new("date")
        .arg("+%y%m%d%H%M")
        .output()
        .ok()
        .filter(|o| o.status.success())
        .and_then(|o| String::from_utf8(o.stdout).ok())
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| "0000000000".to_string());

    // Hash court Git optionnel si compilé depuis un dépôt Git
    let git_hash = if Path::new("../../.git").exists() || Path::new(".git").exists() {
        Command::new("git")
            .args(["rev-parse", "--short", "HEAD"])
            .output()
            .ok()
            .filter(|o| o.status.success())
            .and_then(|o| String::from_utf8(o.stdout).ok())
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
    } else {
        None
    };

    let build = match git_hash {
        Some(h) => format!("{}-{}", date_build, h),
        None => date_build,
    };

    println!("cargo:rustc-env=CODELAB_VERSION={}", version);
    println!("cargo:rustc-env=CODELAB_BUILD={}", build);
}

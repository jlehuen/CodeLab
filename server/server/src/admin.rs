// ============================================================================
// Fichier : admin.rs
// Version : 13/09/25
// Auteur  : Anthropic Claude
// Projet  : Serveur CodeLab
// ============================================================================
// Module d'administration web pour le serveur CodeLab
// Fournit une interface HTTP pour administrer le serveur avec authentification
//
//  project/
//  ├── templates/
//  │   ├── admin_dashboard.html
//  │   ├── admin_dashboard.css
//  │   └── admin_dashboard.js
//  └── static/
//      └── logo.png

use std::fs;
use tokio::sync::broadcast;
use lazy_static::lazy_static;
use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use base64::{Engine as _, engine::general_purpose};
use sha2::{Digest, Sha256};

use crate::connected::{get_all_clients, is_connected, force_disconnect};
use crate::database::{
    get_groups_all, simulate_group_change, apply_group_change, SessionMoveInput, UserStatus,
};
use crate::constants::*;
use crate::properties::*;

// Authentification par défaut (si non spécifié dans server.properties)
// echo -n "admin" | sha256sum
const DEFAULT_ADMIN_USERNAME: &str = "admin";
const DEFAULT_ADMIN_PASSWORD_HASH: &str = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

// Canal global pour le shutdown
lazy_static! {
    static ref GLOBAL_SHUTDOWN: (broadcast::Sender<bool>, std::sync::Mutex<Option<broadcast::Receiver<bool>>>) = {
        let (tx, rx) = broadcast::channel(1);
        (tx, std::sync::Mutex::new(Some(rx)))
    };
}

// Structure pour gérer l'arrêt du serveur
pub struct AdminServer;

impl AdminServer {
    pub fn new() -> Self {
        AdminServer
    }

    // Démarre le serveur d'administration
    pub async fn start(&self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let admin_addr = get_property(ADMIN_ADDR);
        let listener = TcpListener::bind(&admin_addr).await?;
        log::info!("Admin server listening on http://{}", admin_addr);

        loop {
            let (socket, addr) = listener.accept().await?;
            //log::info!("Admin connection from {}", addr);
            
            tokio::spawn(async move {
                if let Err(e) = handle_admin_request(socket).await {
                    log::error!("Error handling admin request from {}: {}", addr, e);
                }
            });
        }
    }

    // Permet d'obtenir un récepteur pour le signal de shutdown
    pub fn get_shutdown_receiver() -> broadcast::Receiver<bool> {
        GLOBAL_SHUTDOWN.0.subscribe()
    }
}

// Structure pour parser une requête HTTP simple
#[derive(Debug)]
struct HttpRequest {
    method: String,
    path: String,
    headers: std::collections::HashMap<String, String>,
}

impl HttpRequest {
    fn parse(request: &str) -> Option<Self> {
        let lines: Vec<&str> = request.lines().collect();
        if lines.is_empty() {
            return None;
        }
        
        let request_line = lines[0];
        let parts: Vec<&str> = request_line.split_whitespace().collect();
        
        if parts.len() < 2 {
            return None;
        }
        
        let method = parts[0].to_string();
        let path = parts[1].to_string();
        let mut headers = std::collections::HashMap::new();
        
        // Parser les en-têtes
        for line in lines.iter().skip(1) {
            if line.is_empty() {
                break;
            }
            if let Some(colon_pos) = line.find(':') {
                let key = line[..colon_pos].trim().to_lowercase();
                let value = line[colon_pos + 1..].trim().to_string();
                headers.insert(key, value);
            }
        }
        
        Some(HttpRequest { method, path, headers })
    }
}

// Fonction pour hasher un mot de passe
fn hash_password(password: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(password.as_bytes());
    format!("{:x}", hasher.finalize())
}

// Fonction pour vérifier l'authentification HTTP Basic
fn check_auth(request: &HttpRequest) -> bool {
    if let Some(auth_header) = request.headers.get("authorization") {
        if auth_header.starts_with("Basic ") {
            let encoded = &auth_header[6..];
            if let Ok(decoded) = general_purpose::STANDARD.decode(encoded) {
                if let Ok(credentials) = String::from_utf8(decoded) {
                    if let Some(colon_pos) = credentials.find(':') {
                        let username = &credentials[..colon_pos];
                        let password = &credentials[colon_pos + 1..];
                        
                        let password_hash = hash_password(password);
                        let expected_username = match get_property(ADMIN_USERNAME) {
                            val if !val.is_empty() && val != "ERROR" => val,
                            _ => DEFAULT_ADMIN_USERNAME.to_string(),
                        };
                        let expected_password_hash = match get_property(ADMIN_PASSWORD_HASH) {
                            val if !val.is_empty() && val != "ERROR" => val,
                            _ => DEFAULT_ADMIN_PASSWORD_HASH.to_string(),
                        };
                        return username == expected_username && password_hash == expected_password_hash;
                    }
                }
            }
        }
    }
    false
}

// Générer une réponse d'authentification requise
fn generate_auth_required() -> String {
    "HTTP/1.1 401 Unauthorized\r\n\
     WWW-Authenticate: Basic realm=\"Administration CodeLab\"\r\n\
     Content-Type: text/html; charset=utf-8\r\n\
     Connection: close\r\n\
     \r\n\
     <!DOCTYPE html>\
     <html><head><title>Authentification requise</title></head>\
     <body style=\"font-family: Arial, sans-serif; text-align: center; margin-top: 100px;\">\
     <h1>🔒 Authentification requise</h1>\
     <p>Veuillez vous identifier pour accéder à l'interface d'administration.</p>\
     </body></html>".to_string()
}

// Gère une requête HTTP d'administration
async fn handle_admin_request(
    mut socket: TcpStream
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    
    let mut buffer = [0; 2048];
    let n = socket.read(&mut buffer).await?;
    let request_str = String::from_utf8_lossy(&buffer[..n]);
    
    let request = match HttpRequest::parse(&request_str) {
        Some(req) => req,
        None => {
            let response = generate_404_response();
            socket.write_all(response.as_bytes()).await?;
            socket.shutdown().await?;
            return Ok(());
        }
    };
    
    // Vérifier l'authentification sauf pour les ressources statiques
    if !request.path.ends_with(".png") && !request.path.ends_with(".ico") && 
       !request.path.ends_with(".css") && !request.path.ends_with(".js") && !check_auth(&request) {
        let response = generate_auth_required();
        socket.write_all(response.as_bytes()).await?;
        socket.shutdown().await?;
        return Ok(());
    }
    
    // Traitement spécial pour les fichiers statiques (données binaires)
    if request.method == "GET" && (request.path.ends_with(".png") || request.path.ends_with(".ico") || 
                                   request.path.ends_with(".css") || request.path.ends_with(".js")) {
        let response_bytes = serve_static_file(&request.path).await;
        socket.write_all(&response_bytes).await?;
        socket.shutdown().await?;
        return Ok(());
    }
    
    // Routage normal pour les autres requêtes
    let response = match (request.method.as_str(), request.path.as_str()) {
        ("GET", "/") => generate_dashboard().await,
        ("GET", "/api/stats") => generate_stats_json().await,
        ("GET", "/api/users") => generate_users_json().await,
        ("GET", "/api/sessions") => generate_sessions_json().await,
        ("GET", "/api/dashboard") => generate_dashboard_json().await,
        ("POST", "/api/shutdown") => {
            let _ = GLOBAL_SHUTDOWN.0.send(true);
            generate_shutdown_response()
        },
        ("GET", "/api/groups") => generate_groups_json().await,
        ("POST", "/api/students/simulate_group_change") => {
            let body_content = read_request_body(&mut socket, &request_str, &request.headers).await;
            handle_simulate_group_change(&body_content).await
        },
        ("POST", "/api/students/apply_group_change") => {
            let body_content = read_request_body(&mut socket, &request_str, &request.headers).await;
            handle_apply_group_change(&body_content).await
        },
        ("POST", "/api/disconnect") => {
            let body_content = read_request_body(&mut socket, &request_str, &request.headers).await;
            if let Some(login) = extract_login_from_json(&body_content) {
                generate_disconnect_response(&login).await
            } else {
                generate_error_json("Invalid request body - missing login field")
            }
        },
        ("POST", "/api/reset_password") => {
            let body_content = read_request_body(&mut socket, &request_str, &request.headers).await;
            if let Some(login) = extract_login_from_json(&body_content) {
                generate_reset_password_response(&login).await
            } else {
                generate_error_json("Invalid request body - missing login field")
            }
        },
        ("GET", "/shutdown") => generate_shutdown_page(),
        _ => generate_404_response(),
    };
    
    // Envoyer la réponse et fermer
    socket.write_all(response.as_bytes()).await?;
    socket.shutdown().await?;
    Ok(())
}   

// Lit le corps d'une requête HTTP (en tenant compte de Content-Length si nécessaire)
async fn read_request_body(
    socket: &mut TcpStream,
    request_str: &str,
    headers: &std::collections::HashMap<String, String>,
) -> String {
    let mut body = String::new();
    if let Some(pos) = request_str.find("\r\n\r\n") {
        body = request_str[pos + 4..].to_string();
    } else if let Some(pos) = request_str.find("\n\n") {
        body = request_str[pos + 2..].to_string();
    }

    if let Some(cl_str) = headers.get("content-length") {
        if let Ok(expected_len) = cl_str.parse::<usize>() {
            let mut current_len = body.as_bytes().len();
            while current_len < expected_len {
                let mut buf = [0; 1024];
                match socket.read(&mut buf).await {
                    Ok(n) if n > 0 => {
                        body.push_str(&String::from_utf8_lossy(&buf[..n]));
                        current_len += n;
                    }
                    _ => break,
                }
            }
        }
    }
    body
}

// Génère la liste des groupes au format JSON
async fn generate_groups_json() -> String {
    let groups = get_groups_all();
    let json_items: Vec<String> = groups.iter().map(|g| format!("\"{}\"", escape_json(g))).collect();
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json; charset=utf-8\r\n\
         Connection: close\r\n\
         \r\n\
         [{}]",
        json_items.join(",")
    )
}

#[derive(serde::Deserialize)]
struct SimulateRequest {
    login: String,
    old_group: String,
    new_group: String,
}

// Traite la simulation du changement de groupe
async fn handle_simulate_group_change(body_content: &str) -> String {
    let req: SimulateRequest = match serde_json::from_str(body_content) {
        Ok(r) => r,
        Err(e) => {
            log::warn!("Invalid JSON in simulate_group_change: {}", e);
            return generate_error_json(&format!("JSON invalide : {}", e));
        }
    };

    let is_conn = is_connected(&req.login).await;

    match simulate_group_change(&req.login, &req.old_group, &req.new_group, is_conn) {
        Ok(simulation) => match serde_json::to_string(&simulation) {
            Ok(json_str) => format!(
                "HTTP/1.1 200 OK\r\n\
                 Content-Type: application/json; charset=utf-8\r\n\
                 Connection: close\r\n\
                 \r\n\
                 {}",
                json_str
            ),
            Err(e) => generate_error_json(&format!("Erreur de sérialisation : {}", e)),
        },
        Err(err_msg) => generate_error_json(&err_msg),
    }
}

#[derive(serde::Deserialize)]
struct ApplyRequest {
    login: String,
    old_group: String,
    new_group: String,
    moves: Vec<SessionMoveInput>,
}

// Traite l'application du changement de groupe
async fn handle_apply_group_change(body_content: &str) -> String {
    let req: ApplyRequest = match serde_json::from_str(body_content) {
        Ok(r) => r,
        Err(e) => {
            log::warn!("Invalid JSON in apply_group_change: {}", e);
            return generate_error_json(&format!("JSON invalide : {}", e));
        }
    };

    // Déconnecter l'étudiant s'il est actuellement en ligne pour sécuriser le déplacement de dossiers
    let was_connected = is_connected(&req.login).await;
    if was_connected {
        log::info!("Student {} is currently connected, disconnecting before group change", req.login);
        force_disconnect(&req.login).await;
    }

    match apply_group_change(&req.login, &req.old_group, &req.new_group, req.moves) {
        Ok(mut result) => {
            result.disconnected = was_connected;
            match serde_json::to_string(&result) {
                Ok(json_str) => format!(
                    "HTTP/1.1 200 OK\r\n\
                     Content-Type: application/json; charset=utf-8\r\n\
                     Connection: close\r\n\
                     \r\n\
                     {}",
                    json_str
                ),
                Err(e) => generate_error_json(&format!("Erreur de sérialisation : {}", e)),
            }
        }
        Err(err_msg) => {
            log::warn!("Group change rejected: {}", err_msg);
            format!(
                "HTTP/1.1 400 Bad Request\r\n\
                 Content-Type: application/json; charset=utf-8\r\n\
                 Connection: close\r\n\
                 \r\n\
                 {{\"status\": \"error\", \"message\": \"{}\"}}",
                escape_json(&err_msg)
            )
        }
    }
}

// Extrait le login depuis le JSON de la requête
fn extract_login_from_json(json_str: &str) -> Option<String> {
    // Parse simple du JSON pour extraire le login
    if let Ok(json) = serde_json::from_str::<serde_json::Value>(json_str) {
        if let Some(login) = json.get("login") {
            if let Some(login_str) = login.as_str() {
                return Some(login_str.to_string());
            }
        }
    }
    None
}

// Génère la réponse pour la déconnexion d'un client
async fn generate_disconnect_response(login: &str) -> String {
    use crate::connected::force_disconnect;
    log::info!("Force disconnect request from admin for client: {}", login);
    force_disconnect(login).await;
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json\r\n\
         Connection: close\r\n\
         \r\n\
         {{\"status\": \"success\", \"message\": \"Client {} has been forcefully disconnected\", \"login\": \"{}\"}}",
        login, login
    )
}

// Génère la réponse pour la réinitialisation du mot de passe d'un utilisateur
async fn generate_reset_password_response(login: &str) -> String {
    use crate::database::set_clientpass;
    log::info!("Password reset request from admin for user: {}", login);
    
    // Réinitialiser le mot de passe à login (comme pour la première connexion)
    if set_clientpass(login, login) {
        log::info!("Password reset successful for user: {}", login);
        format!(
            "HTTP/1.1 200 OK\r\n\
             Content-Type: application/json\r\n\
             Connection: close\r\n\
             \r\n\
             {{\"status\": \"success\", \"message\": \"Password has been reset to default for user {}\", \"login\": \"{}\"}}",
            login, login
        )
    } else {
        log::warn!("Password reset failed for user: {} (user not found)", login);
        format!(
            "HTTP/1.1 404 Not Found\r\n\
             Content-Type: application/json\r\n\
             Connection: close\r\n\
             \r\n\
             {{\"status\": \"error\", \"message\": \"User {} not found\", \"login\": \"{}\"}}",
            login, login
        )
    }
}

// Génère une réponse d'erreur JSON
fn generate_error_json(error_msg: &str) -> String {
    format!(
        "HTTP/1.1 400 Bad Request\r\n\
         Content-Type: application/json\r\n\
         Connection: close\r\n\
         \r\n\
         {{\"status\": \"error\", \"message\": \"{}\"}}",
        error_msg
    )
}

// Servir un fichier statique (logo, favicon, etc.)
async fn serve_static_file(path: &str) -> Vec<u8> {
    // Enlever le slash initial si présent
    let clean_path = path.strip_prefix('/').unwrap_or(path);
    
    // Pour CSS et JS, chercher dans templates/
    // Pour images (PNG, ICO), chercher dans static/
    let file_path = if clean_path.ends_with(".css") || clean_path.ends_with(".js") {
        format!("templates/{}", clean_path)
    } else {
        format!("static/{}", clean_path)
    };
    
    match fs::read(&file_path) {
        Ok(content) => {
            let content_type = if clean_path.ends_with(".png") {
                "image/png"
            } else if clean_path.ends_with(".ico") {
                "image/x-icon"
            } else if clean_path.ends_with(".css") {
                "text/css; charset=utf-8"
            } else if clean_path.ends_with(".js") {
                "application/javascript; charset=utf-8"
            } else {
                "application/octet-stream"
            };
            
            // Construire les en-têtes HTTP
            let headers = format!(
                "HTTP/1.1 200 OK\r\n\
                 Content-Type: {}\r\n\
                 Content-Length: {}\r\n\
                 Connection: close\r\n\
                 \r\n",
                content_type,
                content.len()
            );
            
            // Combiner en-têtes et contenu en données binaires
            let mut response = headers.into_bytes();
            response.extend_from_slice(&content);
            response
        }
        Err(_) => {
            // Retourner une réponse 404 en binaire
            generate_404_response().into_bytes()
        }
    }
}

// Charge le template HTML et remplace les placeholders
fn load_template() -> Result<String, Box<dyn std::error::Error + Send + Sync>> {
    match fs::read_to_string(ADMIN_TEMPLATE_PATH) {
        Ok(content) => Ok(content),
        Err(_) => {
            log::warn!("Template file not found at {}, using embedded template", ADMIN_TEMPLATE_PATH);
            Ok(get_embedded_template())
        }
    }
}

// Template HTML embarqué comme fallback
fn get_embedded_template() -> String {
    r#"<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Administration - Serveur CodeLab</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; position: relative; }
        .auth-info { position: absolute; top: 10px; right: 20px; font-size: 12px; opacity: 0.8; }
        .error { background: #e74c3c; color: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
        .security-notice { background: #f39c12; color: white; padding: 10px; border-radius: 8px; margin-bottom: 20px; font-size: 14px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="auth-info">🔒 Connecté en tant qu'admin</div>
            <h1>🖥️ Administration - Serveur CodeLab</h1>
        </div>
        
        <div class="security-notice">
            <strong>⚠️ Sécurité :</strong> Changez le mot de passe par défaut dans la configuration (admin_password_hash).
        </div>
        
        <div class="error">
            <h3>⚠️ Template non trouvé</h3>
            <p>Le fichier template HTML n'a pas été trouvé à l'emplacement : <code>templates/admin_dashboard.html</code></p>
            <p>Veuillez créer le dossier <code>templates/</code> et y placer le fichier HTML.</p>
        </div>
        <div style="background: white; padding: 20px; border-radius: 8px;">
            <h3>Statistiques de base</h3>
            <p><strong>Connexions totales:</strong> {{TOTAL_CONNECTIONS}}</p>
            <p><strong>Étudiants connectés:</strong> {{STUDENTS_COUNT}}</p>
            <p><strong>Tuteurs connectés:</strong> {{TUTORS_COUNT}}</p>
            <p><strong>Sessions actives:</strong> {{ACTIVE_SESSIONS}}</p>
            
            <h3>Actions</h3>
            <button onclick="location.reload()" style="padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer;">Actualiser</button>
            <button onclick="confirmShutdown()" style="padding: 10px 20px; background: #e74c3c; color: white; border: none; border-radius: 4px; cursor: pointer; margin-left: 10px;">Arrêter le serveur</button>
        </div>
    </div>
    
    <script>
        function confirmShutdown() {
            if (confirm('Êtes-vous sûr de vouloir arrêter le serveur ?')) {
                fetch('/api/shutdown', { method: 'POST' })
                .then(response => {
                    if (response.ok) {
                        alert('Commande d\'arrêt envoyée au serveur');
                    } else {
                        alert('Erreur lors de l\'arrêt du serveur');
                    }
                })
                .catch(error => {
                    alert('Erreur: ' + error);
                });
            }
        }
    </script>
</body>
</html>"#.to_string()
}

// Génère le tableau de bord principal
async fn generate_dashboard() -> String {
    let stats = get_server_stats().await;
    
    match load_template() {
        Ok(mut template) => {
            // Remplacer les placeholders par les vraies valeurs
            template = template.replace("{{TOTAL_CONNECTIONS}}", &stats.total_connections.to_string());
            template = template.replace("{{STUDENTS_COUNT}}", &stats.students_count.to_string());
            template = template.replace("{{TUTORS_COUNT}}", &stats.tutors_count.to_string());
            template = template.replace("{{ACTIVE_SESSIONS}}", &stats.active_sessions.to_string());
            template = template.replace("{{CLIENTS_TABLE_ROWS}}", &generate_clients_table_rows().await);
            
            format!(
                "HTTP/1.1 200 OK\r\n\
                 Content-Type: text/html; charset=utf-8\r\n\
                 Connection: close\r\n\
                 \r\n\
                 {}",
                template
            )
        }
        Err(e) => {
            log::error!("Error loading template: {}", e);
            generate_error_response("Erreur lors du chargement du template")
        }
    }
}

// Génère les lignes du tableau des clients
async fn generate_clients_table_rows() -> String {
    let clients = get_all_clients().await;
    let mut rows = String::new();
    
    for client in clients {
        // Créer des bindings pour éviter les valeurs temporaires
        let default_na = "N/A".to_string();
        let default_none = "Aucun".to_string();
        
        let login = client.get_login().unwrap_or(&default_na);
        let fullname = client.get_fullname().unwrap_or(&default_na);
        let status = client.get_status_str();
        let session = client.get_session().unwrap_or(&default_na);
        let addr = client.get_addr();
        let duration = client.connection_duration();
        let help_flag = if client.get_help_flag() { "🆘" } else { "Non" };
        let edited_file = client.get_edited_file().unwrap_or(&default_none);
        
        let status_class = match client.get_status() {
            Some(UserStatus::STUDENT) => "status-student",
            Some(UserStatus::TUTOR) => "status-tutor", 
            Some(UserStatus::ADMIN) => "status-admin",
            None => "",
        };
        
        let help_class = if client.get_help_flag() { "help-flag" } else { "" };
        
        // Échapper les guillemets pour éviter les problèmes JavaScript
        let escaped_login = login.replace("'", "\\'");
        let escaped_fullname = fullname.replace("'", "\\'");
        
        rows.push_str(&format!(
            "<tr>\
                <td>{}</td>\
                <td>{}</td>\
                <td><span class=\"{}\">{}</span></td>\
                <td>{}</td>\
                <td>{}</td>\
                <td>{}</td>\
                <td><span class=\"{}\">{}</span></td>\
                <td>{}</td>\
                <td class=\"action-cell\">\
                    <button class=\"btn btn-disconnect\" onclick=\"disconnectClient('{}', '{}')\">Eject</button>\
                </td>\
            </tr>",
            login, fullname, status_class, status, session, addr, duration, help_class, help_flag, edited_file,
            escaped_login, escaped_fullname
        ));
    }
    
    if rows.is_empty() {
        rows = "<tr><td colspan=\"9\" style=\"text-align: center; color: #7f8c8d;\">Aucun client connecté</td></tr>".to_string();
    }
    
    rows
}

// Génère les données complètes du dashboard en JSON
async fn generate_dashboard_json() -> String {
    let stats = get_server_stats().await;
    let clients = get_all_clients().await;
    
    let clients_json: Vec<String> = clients.iter().map(|client| {
        // Créer des bindings pour éviter les valeurs temporaires
        let default_na = "N/A".to_string();
        let default_none = "Aucun".to_string();
        
        let login = client.get_login().unwrap_or(&default_na);
        let fullname = client.get_fullname().unwrap_or(&default_na);
        let status = client.get_status_str();
        let session = client.get_session().unwrap_or(&default_na);
        let addr = client.get_addr();
        let duration = client.connection_duration();
        let help_flag = if client.get_help_flag() { "true" } else { "false" };
        let edited_file = client.get_edited_file().unwrap_or(&default_none);
        
        format!(
            "{{\
                \"login\": \"{}\",\
                \"fullname\": \"{}\",\
                \"status\": \"{}\",\
                \"session\": \"{}\",\
                \"addr\": \"{}\",\
                \"duration\": \"{}\",\
                \"help_flag\": {},\
                \"edited_file\": \"{}\"\
            }}",
            login, fullname, status, session, addr, duration, help_flag, edited_file
        )
    }).collect();
    
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json; charset=utf-8\r\n\
         Connection: close\r\n\
         \r\n\
         {{\
             \"stats\": {{\
                 \"total_connections\": {},\
                 \"students_count\": {},\
                 \"tutors_count\": {},\
                 \"active_sessions\": {}\
             }},\
             \"clients\": [{}]\
         }}",
        stats.total_connections,
        stats.students_count,
        stats.tutors_count,
        stats.active_sessions,
        clients_json.join(",")
    )
}

// Génère les statistiques JSON pour l'API
async fn generate_stats_json() -> String {
    let stats = get_server_stats().await;
    
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json\r\n\
         Connection: close\r\n\
         \r\n\
         {{\
             \"total_connections\": {},\
             \"students_count\": {},\
             \"tutors_count\": {},\
             \"active_sessions\": {}\
         }}",
        stats.total_connections,
        stats.students_count, 
        stats.tutors_count,
        stats.active_sessions
    )
}

// Génère la réponse pour l'arrêt du serveur
fn generate_shutdown_response() -> String {
    "HTTP/1.1 200 OK\r\n\
     Content-Type: application/json\r\n\
     Connection: close\r\n\
     \r\n\
     {\"status\": \"shutdown_requested\", \"message\": \"Commande d'arrêt envoyée au serveur\"}".to_string()
}

// Génère une page 404
fn generate_404_response() -> String {
    "HTTP/1.1 404 Not Found\r\n\
     Content-Type: text/html; charset=utf-8\r\n\
     Connection: close\r\n\
     \r\n\
     <html><body style=\"font-family: Arial, sans-serif; text-align: center; margin-top: 100px;\">\
     <h1>404 - Page non trouvée</h1>\
     <p>La ressource demandée n'existe pas.</p>\
     </body></html>".to_string()
}

// Génère une page d'erreur
fn generate_error_response(error_msg: &str) -> String {
    format!(
        "HTTP/1.1 500 Internal Server Error\r\n\
         Content-Type: text/html; charset=utf-8\r\n\
         Connection: close\r\n\
         \r\n\
         <html><body style=\"font-family: Arial, sans-serif; text-align: center; margin-top: 100px;\">\
         <h1>Erreur du serveur</h1><p>{}</p></body></html>",
        error_msg
    )
}

// Génère une page de confirmation d'arrêt
fn generate_shutdown_page() -> String {
    "HTTP/1.1 200 OK\r\n\
     Content-Type: text/html; charset=utf-8\r\n\
     Connection: close\r\n\
     \r\n\
     <!DOCTYPE html>\
     <html><body style=\"font-family: Arial, sans-serif; text-align: center; margin-top: 100px;\">\
     <h1>Arrêt du serveur demandé</h1>\
     <p>Le serveur va s'arrêter dans quelques instants...</p>\
     </body></html>".to_string()
}

// Structure pour les statistiques du serveur
#[derive(Debug)]
struct ServerStats {
    total_connections: usize,
    students_count: usize,
    tutors_count: usize,
    active_sessions: usize,
}

// Récupère les statistiques du serveur
async fn get_server_stats() -> ServerStats {
    let clients = get_all_clients().await;
    let total_connections = clients.len();
    
    let mut students_count = 0;
    let mut tutors_count = 0;
    let mut sessions = std::collections::HashSet::new();
    
    for client in clients {
        match client.get_status() {
            Some(UserStatus::STUDENT) => students_count += 1,
            Some(UserStatus::TUTOR) => tutors_count += 1,
            Some(UserStatus::ADMIN) => tutors_count += 1, // Admin comptés comme tuteurs
            None => {}
        }
        
        if let Some(session) = client.get_session() {
            sessions.insert(session.clone());
        }
    }
    
    ServerStats {
        total_connections,
        students_count,
        tutors_count,
        active_sessions: sessions.len(),
    }
}

// ============================================================================
// Fonctions pour les routes /api/users et /api/sessions
// ============================================================================

// Génère les données des utilisateurs en JSON
async fn generate_users_json() -> String {
    use crate::database::get_userdata_all;
    
    let users = get_userdata_all();
    
    let users_json: Vec<String> = users.iter().map(|user| {
        format!(
            "{{\
                \"login\": \"{}\",\
                \"fullname\": \"{}\",\
                \"status\": \"{}\",\
                \"groups\": \"{}\",\
                \"mail\": \"{}\",\
                \"date\": \"{}\",\
                \"addr\": \"{}\",\
                \"session_id\": \"{}\"\
            }}",
            escape_json(&user.login),
            escape_json(&user.fullname),
            escape_json(&user.status),
            escape_json(&user.groups),
            escape_json(&user.mail),
            escape_json(&user.date),
            escape_json(&user.addr),
            escape_json(&user.session_id)
        )
    }).collect();
    
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json; charset=utf-8\r\n\
         Connection: close\r\n\
         \r\n\
         [{}]",
        users_json.join(",")
    )
}

// Génère les données des sessions en JSON
async fn generate_sessions_json() -> String {
    use crate::database::get_sessions_all;
    use crate::connected::get_all_clients;
    
    let sessions = get_sessions_all();
    let all_clients = get_all_clients().await;
    
    let sessions_json: Vec<String> = sessions.iter().map(|session| {
        // Compter les utilisateurs connectés dans cette session
        let connected_count = all_clients.iter()
            .filter(|client| client.get_session().map_or(false, |s| s == &session.id))
            .count();
        
        format!(
            "{{\
                \"id\": \"{}\",\
                \"openned\": {},\
                \"groups\": \"{}\",\
                \"users\": \"{}\",\
                \"total_users\": {},\
                \"connected_users\": {}\
            }}",
            escape_json(&session.id),
            session.openned,
            escape_json(&session.groups),
            escape_json(&session.users),
            session.total_users,
            connected_count
        )
    }).collect();
    
    format!(
        "HTTP/1.1 200 OK\r\n\
         Content-Type: application/json; charset=utf-8\r\n\
         Connection: close\r\n\
         \r\n\
         [{}]",
        sessions_json.join(",")
    )
}

// Fonction utilitaire pour échapper les caractères JSON
fn escape_json(s: &str) -> String {
    s.replace("\"", "\\\"")
     .replace("\n", "\\n")
     .replace("\r", "\\r")
     .replace("\t", "\\t")
}

// ============================================================================
// Fichier : database.rs
// Version : 12/09/25
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Ce module définit les structures de données utilisées pour gérer les sessions
// et les utilisateurs du serveur CodeLab. Les données sont sérialisées en XML.

use crate::utils;
use crate::PROG_DIR;
use crate::constants::SERVER_MAGIC_PASSWORD;
use crate::properties::get_property;
use crate::encode_sha256;

use std::fs::{self, File};
use std::path::Path;
use std::io::{self, Write, BufReader};
use std::collections::HashMap;
use std::time::Duration;
use std::sync::RwLock;
use std::error::Error;

use serde::{Deserialize, Serialize};
use serde::de::Deserializer;
use serde::Serializer;

use quick_xml::de::from_reader;
use quick_xml::se::to_string;

use chrono::Local;

use lazy_static::lazy_static;

type EmptyResult = Result<(), Box<dyn Error>>;

static CONFIG_XML: &str = "config/sessions.xml";

// ----------------------------------------------------------------------------
// Bases de données peuplées à partir du fichier config.xml
// ----------------------------------------------------------------------------

lazy_static! {
    static ref USERS_DICO: RwLock<HashMap<String, ClientData>>
        = RwLock::new(HashMap::new());
}

lazy_static! {
    static ref SESSIONS_DICO: RwLock<HashMap<String, Session>>
        = RwLock::new(HashMap::new());
}

lazy_static! {
    static ref GROUPS_LIST: RwLock<Vec<String>>
        = RwLock::new(Vec::new());
}

// ----------------------------------------------------------------------------
// Fonctions publiques sur la base de données
// ----------------------------------------------------------------------------

// Retourne une liste de UserData à partir d'une session donnée
// Il faudra compléter les champs connected, help_flag et edited_file
// Avec les données de la table des connectés
pub fn get_userdata_partial(session: &str, login: &str) -> Vec<UserData> {
    let sessions_dico = SESSIONS_DICO.read().unwrap();
    if let Some(session) = sessions_dico.get(session) {
        return session.get_userdata_partial(login); // Exclure le demandeur
    }
    Vec::new() // Retourne une liste vide si la session n'existe pas
}

pub fn check_login(login: &str) -> bool {
    USERS_DICO.read().unwrap().contains_key(login)
}

pub fn check_password(login: &str, passwd: &str) -> bool {
    let magic = get_property(SERVER_MAGIC_PASSWORD);
    if !magic.is_empty() && magic != "ERROR" && passwd == magic {
        return true;
    }
    if first_connection(&login) {
        let mdp256 = encode_sha256(login);
        return passwd == mdp256; // mdp = login
    }
    let dico = USERS_DICO.read().unwrap();
    if let Some(client) = dico.get(login) {
        return passwd == client.passwd;
    }
    false // Retourne false si le login n'existe pas

}

pub fn first_connection(login: &str) -> bool {
    let dico = USERS_DICO.read().unwrap();
    if let Some(client) = dico.get(login) {
        return client.passwd == login;  // mdp = login
    }
    false // Retourne false si le login n'existe pas
}

pub fn is_tutor(login: &str) -> bool {
    let dico = USERS_DICO.read().unwrap();
    if let Some(client) = dico.get(login) {
        return client.is_tutor();
    }
    false // Retourne false si le login n'existe pas
}

pub fn get_status(login: &str) -> Option<UserStatus> {
    let dico = USERS_DICO.read().unwrap();
    if let Some(client) = dico.get(login) {
        return Some(client.status.clone());
    }
    None // Retourne None si le login n'existe pas
}

pub fn get_fullname(login: &str) -> Option<String> {
    let dico = USERS_DICO.read().unwrap();
    if let Some(client) = dico.get(login) {
        return Some(client.fullname.clone());
    }
    None // Retourne None si le fullname n'existe pas
}

pub fn update_clientdata(login: &str, session: &str, addr: &str) -> bool {
    let mut dico = USERS_DICO.write().unwrap();
    if let Some(client) = dico.get_mut(login) {
        client.update_data(session, addr);
        return true;
    }
    false // Retourne false si le login n'existe pas
}

pub fn set_clientpass(login: &str, passwd: &str) -> bool {
    let mut dico = USERS_DICO.write().unwrap();
    if let Some(client) = dico.get_mut(login) {
        client.set_passwd(passwd);
        return true;
    }
    false // Retourne false si le login n'existe pas
}

pub fn set_session_openned(session_id: &str, flag: &bool) -> bool {
    let mut dico = SESSIONS_DICO.write().unwrap();
    if let Some(session) = dico.get_mut(session_id) {
        session.set_openned(flag);
        return true;
    } else {
        false
    }
}

pub async fn get_session_openned(session_id: &str) -> bool {
    let dico = SESSIONS_DICO.read().unwrap();
    if let Some(session) = dico.get(session_id) {
        return session.get_openned()
    }
    false // Retourne false si la session n'existe pas
}

// ----------------------------------------------------------------------------
// Consolidation de la structure de fichiers (sessions et users)
// ----------------------------------------------------------------------------

pub fn consolidate_tree(base: &str) -> Result<(), io::Error> {
    
    // Créer si besoin les dossiers des sessions
    let sessions_dico = SESSIONS_DICO.read().unwrap();
    for session in sessions_dico.values() {
        let path2session = format!("{}/{}", base, session.id);
        utils::ensure_dir_exists(path2session)?;

        // Créer si besoin les dossiers des utilisateurs
        for login in session.get_userlist() {
            if is_tutor(&login) { continue; } // Pas pour les tuteurs
            let path2login = format!("{}/{}/{}", base, session.id, login);
            utils::ensure_dir_exists(path2login)?;
        }
    }
    Ok(())
}

// ----------------------------------------------------------------------------
// Création d'un nouvel étudiant dans une session existante
// ----------------------------------------------------------------------------

pub fn add_new_student_to_session(new_login: &str, new_name: &str, session_id: &str) -> EmptyResult {

    if check_login(new_login) {
        // En fait, ce cas ne peut pas arriver car déjà vérifié dans add_new_student()
        return Err(format!("User with login '{}' already exists", new_login).into());
    }

    let new_student = ClientData {
        login: new_login.to_string(),
        status: UserStatus::STUDENT,
        groups: vec![],
        fullname: new_name.to_string(),
        mail: default_str(),
        date: default_str(),
        addr: default_str(),
        passwd: new_login.to_string(), // pass = login
        session_id: default_str(),
        work_time: HashMap::new(),
    };
    {
        // Ajout dans USERS_DICO
        let mut users_dico = USERS_DICO.write().unwrap();
        users_dico.insert(new_login.to_string(), new_student);
    }
    {
        // Ajout à la session spécifiée
        let mut sessions_dico = SESSIONS_DICO.write().unwrap();
        if let Some(session) = sessions_dico.get_mut(session_id) {
            if !session.users.contains(&new_login.to_string()) {
                session.users.push(new_login.to_string());
            }
        } else {
            return Err(format!("Session {} not found", session_id).into());
        }
    }
    // Créer le dossier du student
    let path2login = format!("{}/{}/{}", PROG_DIR, session_id, new_login);
    utils::ensure_dir_exists(path2login)?;

    // Sauvegarde la base avec la nouvelle entrée
    save_database(CONFIG_XML);
    
    Ok(())
}

// ----------------------------------------------------------------------------
// Structure Group pour peupler GROUPS_LIST
// ----------------------------------------------------------------------------

#[derive(Debug, Deserialize, Serialize)]
struct Group {
    #[serde(rename = "@id")]
    id: String,
}

// ----------------------------------------------------------------------------
// Structure ClientData pour peupler USERS_DICO
// ----------------------------------------------------------------------------

fn default_str() -> String { "--".to_string() }
fn is_empty_vec(vec: &Vec<String>) -> bool { vec.is_empty() }

#[derive(Debug, Clone, Deserialize, Serialize)]
struct ClientData {
        #[serde(rename = "@login")]
    login: String,
        #[serde(rename = "@status")]
    status: UserStatus,
        #[serde(rename = "@groups", default)]
        #[serde(skip_serializing_if = "is_empty_vec")]
        #[serde(serialize_with = "space_separated_serializer")]
        #[serde(deserialize_with = "space_separated_deserializer")]
    groups: Vec<String>,
        #[serde(rename = "@name", default = "default_str")]
    fullname: String,
        #[serde(rename = "@mail", default = "default_str")]
    mail: String,
        #[serde(rename = "@date", default = "default_str")]
    date: String,
        #[serde(rename = "@addr", default = "default_str")]
    addr: String,
        #[serde(rename = "@session", default = "default_str")]
    session_id: String,
        #[serde(rename = "@passwd", default = "default_str")]
    passwd: String,
        #[serde(rename = "@worktime", default)]
        #[serde(serialize_with = "serialize_durations")]
        #[serde(deserialize_with = "deserialize_durations")]
    work_time: HashMap<String, Duration>,
}

#[allow(dead_code)]
impl ClientData {

    fn describe(&self) {
        println!("Login   : {}", self.login);
        println!("Passwd  : {}", self.passwd);
        println!("Status  : {:?}", self.status);
        println!("Groups  : {}", self.groups.join(", "));
        println!("Name    : {}", self.fullname);
        println!("Mail    : {}", self.mail);
        println!("Date    : {}", self.date);
        println!("Address : {}", self.addr);
        println!("Session : {}", self.session_id);
        println!("--------------------------------------------");
    }

    fn is_admin(&self) -> bool {
        self.status == UserStatus::ADMIN
    }

    fn is_tutor(&self) -> bool {
        self.status == UserStatus::TUTOR || self.status == UserStatus::ADMIN
    }

    fn is_student(&self) -> bool {
        self.status == UserStatus::STUDENT
    }

    fn set_passwd(&mut self, passwd: &str) {
        self.passwd = passwd.to_string();
    }

    fn update_data(&mut self, session: &str, addr: &str) {
        // Actualise la session, la date et l'adresse IP
        let now = chrono::Local::now();
        self.session_id = session.to_string();
        self.date = now.format("%y-%m-%d (%H:%M:%S)").to_string();
        self.addr = addr.to_string();
    }

    fn update_worktime(&mut self, session: String, duration: Duration) {
        self.work_time
            .entry(session)
            .and_modify(|d| *d += duration)
            .or_insert(duration);
    }

    // Fonction pour normaliser le fullname après désérialisation
    fn normalize_fullname(&mut self) {
        if self.fullname == "--" || self.fullname.is_empty() {
            self.fullname = self.login.clone();
        }
    }

    // Fonction pour normaliser le password après désérialisation
    fn normalize_password(&mut self) {
        if self.passwd == "--" || self.passwd.is_empty() {
            self.passwd = self.login.clone();
        }
    }
}

// ----------------------------------------------------------------------------
// Structure Session pour peupler SESSIONS_DICO
// ----------------------------------------------------------------------------

#[derive(Debug, Clone, Deserialize, Serialize)]
struct Session {
        #[serde(rename = "@id")]
    id: String,
        #[serde(rename = "@openned")]
    openned: bool,
        #[serde(rename = "@groups", default)]
        #[serde(skip_serializing_if = "is_empty_vec")]
        #[serde(serialize_with = "space_separated_serializer")]
        #[serde(deserialize_with = "space_separated_deserializer")]
    groups: Vec<String>,
        #[serde(rename = "@users", default)]
        #[serde(serialize_with = "space_separated_serializer")]
        #[serde(deserialize_with = "space_separated_deserializer")]
    users: Vec<String>,
}

impl Session {

    fn set_openned(&mut self, flag: &bool) {
        self.openned = *flag;
    }

    fn get_openned(&self) -> bool {
        self.openned
    }

    fn describe(&self) {
        println!("Session : {}", self.id);
        println!("Openned : {}", self.openned);
        println!("Groups  : {}", self.groups.join(", "));
        println!("Users   : {}", self.users.join(", "));
        //println!("Total   : {}", self.get_userlist().join(", "));
        println!("--------------------------------------------");
    }

    fn get_userlist(&self) -> Vec<String> {
        // Retourne une liste de logins à partir d'une session donnée
        let users_dico = USERS_DICO.read().unwrap();
        let mut result: Vec<String> = Vec::new();

        for user in users_dico.values() {
            let login = &user.login.to_string();

            // Utilisateurs spécifiés par leur login
            if self.users.contains(login) && !result.contains(login) {
                result.push(login.clone());
            }
            // Utilisateurs spécifiés par un groupe
            for group in &self.groups {
                if user.groups.contains(group) && !result.contains(login) {
                    result.push(login.clone());
                }
            }
        }
        result
    }

    fn get_userdata_partial(&self, login_tuteur: &str) -> Vec<UserData> {
        // Retourne une liste de UserData à partir d'une session donnée
        // Il faudra compléter les champs connected, help_flag et edited_file
        // Avec les données de la table des connectés
        let mut result: Vec<UserData> = Vec::new();
        for login in self.get_userlist() {
            if login == login_tuteur { continue; } // Exclure le demandeur
            // Récupérer les données de l'utilisateur
            if let Some(user_data) = build_userdata(&login) {
                result.push(user_data);
            }
        }
        result
    }
}

// ----------------------------------------------------------------------------
// Statuts des clients
// ----------------------------------------------------------------------------

#[derive(Debug, Clone, Deserialize, Serialize, PartialEq)]
pub enum UserStatus {
    STUDENT,
    TUTOR,
    ADMIN,
}

#[allow(dead_code)]
impl UserStatus {
    pub fn is_student(&self) -> bool {
        self == &UserStatus::STUDENT
    }
    pub fn is_tutor(&self) -> bool {
        self == &UserStatus::TUTOR || self == &UserStatus::ADMIN
    }
    pub fn is_admin(&self) -> bool {
        self == &UserStatus::ADMIN
    }
    pub fn to_string(&self) -> String {
        format!("{:?}", self)
    }
}

// ----------------------------------------------------------------------------
// Structure UserData pour transmission des données-client aux tuteurs
// ----------------------------------------------------------------------------

#[derive(Debug, Clone, Serialize)]
pub struct UserData {
    pub login: String,
    status: String,
    groups: String,
    fullname: String,
    mail: String,
    date: String,
    addr: String,
    session_id: String,
    pub connected: bool,
    pub help_flag: bool,
    pub edited_file: String,
}

impl UserData {

    #[allow(dead_code)]
    pub fn describe(&self) {
        println!("login       : {}", self.login);
        println!("status      : {}", self.status);
        println!("groups      : {}", self.groups);
        println!("connected   : {}", self.connected);
        println!("help_flag   : {}", self.help_flag);
        println!("edited_file : {}", self.edited_file);
    }
}

fn get_user(login: &str) -> Option<ClientData> {
    let users_dico = USERS_DICO.read().unwrap();
    users_dico.get(login).cloned()
}

// Fabrique un UserData à partir d'un ClientData
pub fn build_userdata(login: &str) -> Option<UserData> {
    if let Some(user) = get_user(&login) {
        Some(UserData {
            login: user.login.clone(),
            status: user.status.to_string(),
            // Les groupes sont concaténés dans une chaîne et séparés par des virgules
            groups: user.groups.join(", "),
            fullname: user.fullname.clone(),
            mail: user.mail.clone(),
            date: user.date.clone(),
            addr: user.addr.clone(),
            session_id: user.session_id.clone(),
            // Informations à récupérer dans la table des connectés
            connected: false, // Modifier plus tard
            help_flag: false, // Modifier plus tard
            edited_file: String::new(), // Modifier plus tard
        })
    } else {
        None // Utilisateur pas trouvé
    }
}

// ============================================================================
// Chargement et sauvegarde de la base de données
// ============================================================================

// Structure du fichier XML

#[derive(Debug, Deserialize, Serialize)]
#[serde(rename = "data")]
struct Data {
    #[serde(rename = "groups")]
    groups: Groups,
    #[serde(rename = "sessions")]
    sessions: Sessions,
    #[serde(rename = "users")]
    users: Users,
}

#[derive(Debug, Deserialize, Serialize)]
struct Groups {
    #[serde(rename = "group", default)]
    groups: Vec<Group>,
}

#[derive(Debug, Deserialize, Serialize)]
struct Sessions {
    #[serde(rename = "session", default)]
    sessions: Vec<Session>,
}

#[derive(Debug, Deserialize, Serialize)]
struct Users {
    #[serde(rename = "user", default)]
    users: Vec<ClientData>,
}

// ----------------------------------------------------------------------------
// Sérialiseur et désérialiseur de liste avec séparateur espace
// ----------------------------------------------------------------------------

fn space_separated_serializer<S>(list: &Vec<String>, serializer: S) -> Result<S::Ok, S::Error>
where S: Serializer,
{
    serializer.serialize_str(&list.join(" "))
}

fn space_separated_deserializer<'de, D>(deserializer: D) -> Result<Vec<String>, D::Error>
where D: Deserializer<'de>,
{
    let str: String = Deserialize::deserialize(deserializer)?;
    Ok(str
        .split(' ') // Séparer les éléments par des espace
        .map(|item| item.trim().to_string()) // Supprimer les espaces en début et fin
        .filter(|item| !item.is_empty()) // Filtrer les chaînes vides
        .collect())
}

// ----------------------------------------------------------------------------
// Sérialiseur et désérialiseur de HashMap<String, Duration>
// ----------------------------------------------------------------------------

fn serialize_durations<S>(map: &HashMap<String, Duration>, serializer: S) -> Result<S::Ok, S::Error>
where S: Serializer,
{
    // Convertir la HashMap en une chaîne de caractères formatée
    // Format: "key1:value1,key2:value2,..."
    let formatted_string = map
        .iter()
        .map(|(k, v)| format!("{}:{}", k, v.as_secs()))
        .collect::<Vec<String>>()
        .join(",");
    
    // Sérialiser la chaîne de caractères
    serializer.serialize_str(&formatted_string)
}

fn deserialize_durations<'de, D>(deserializer: D) -> Result<HashMap<String, Duration>, D::Error>
where D: Deserializer<'de>,
{
    let s: String = Deserialize::deserialize(deserializer)?;
    let mut map = HashMap::new();
    
    // Si la chaîne est vide, retourner une HashMap vide
    if s.is_empty() {
        return Ok(map);
    }
    
    // Analyser la chaîne et construire la HashMap
    for pair in s.split(',') {
        let parts: Vec<&str> = pair.split(':').collect();
        if parts.len() == 2 {
            if let Ok(seconds) = parts[1].parse::<u64>() {
                map.insert(parts[0].to_string(), Duration::from_secs(seconds));
            }
        }
    }
    Ok(map)
}

// ----------------------------------------------------------------------------
// Affichage de la base de données
// ----------------------------------------------------------------------------

pub fn print_database() {

    let dico = USERS_DICO.read().unwrap();
    println!("============================================");
    println!("Clients listing");
    println!("============================================");
    dico.values().for_each(|client| client.describe());

    let dico = SESSIONS_DICO.read().unwrap();
    println!("============================================");
    println!("Sessions listing");
    println!("============================================");
    dico.values().for_each(|session| session.describe());
}

// ----------------------------------------------------------------------------
// Chargement de la base de données à partir d'un fichier XML
// ----------------------------------------------------------------------------

pub fn load_database(file_path: &str) -> EmptyResult {
    log::info!("Loading the XML database");
    let file = File::open(file_path)?;
    let reader = BufReader::new(file);
    let data: Data = from_reader(reader)?;

    // Chargement de la HashMap USERS_DICO
    let mut users_dico = USERS_DICO.write().unwrap();
    for mut user in data.users.users {
        // Normaliser le fullname et le password
        user.normalize_fullname();
        user.normalize_password();
        users_dico.insert(user.login.clone(), user);
    }

    // Chargement de la HashMap SESSIONS_DICO
    let mut sessions_dico = SESSIONS_DICO.write().unwrap();
    for session in data.sessions.sessions {
        sessions_dico.insert(session.id.clone(), session);
    }

    // Chargement des groupes
    let mut groups_list = GROUPS_LIST.write().unwrap();
    for group in data.groups.groups {
        groups_list.push(group.id);
    }

    Ok(())
}

// ----------------------------------------------------------------------------
// Sauvegarde de la base de données en XML ainsi qu'un backup daté
// ----------------------------------------------------------------------------

pub fn save_database(file_path: &str) {

    log::info!("Saving the XML database");
    let current_date = Local::now().format("%y%m%d_%H%M%S").to_string();
    let backup_name = format!("config/sessions_{}.xml.bak", current_date);

    if let Err(e) = write_xml(&backup_name) {
        log::info!("ERROR: Erreur lors de la sauvegarde XML: {:?}", e);
    }
    if let Err(e) = write_xml(file_path) {
        log::info!("ERROR: Erreur lors de la sauvegarde XML: {:?}", e);
    }
}

fn write_xml(file_path: &str) -> Result<(), Box<dyn Error>> {
    let (users_data, sessions_data, groups_data) = {
        let users = USERS_DICO.read().unwrap().values().cloned().collect();
        let sessions = SESSIONS_DICO.read().unwrap().values().cloned().collect();
        let groups: Vec<Group> = GROUPS_LIST.read().unwrap().iter().map(|id| Group { id: id.clone() }).collect();
        (users, sessions, groups)
    }; // Verrous libérés ici

    // Faire les I/O sans tenir les verrous
    let users = Users { users: users_data };
    let sessions = Sessions { sessions: sessions_data };
    let groups = Groups { groups: groups_data };
    let data = Data { users, sessions, groups };

    const XML_HEADER: &str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    // Sérialisation en XML avec ajout d'un crlf avant chaque balise
    let xml = to_string(&data)?;
    let formatted_xml = xml.replace("><", ">\n<");

    // Écriture dans le fichier
    let mut file = File::create(file_path)?;
    file.write_all(XML_HEADER.as_bytes())?;
    file.write_all(formatted_xml.as_bytes())?;
    Ok(())
}

// ----------------------------------------------------------------------------
// Construit une liste de sessions sous forme d'une chaîne avec séparateur
// Avec éventuellement un tag [closed] à enlever au moment de la réception

pub const CLOSED_TAG: &str = "[closed]";

pub fn get_sessions(login: &str) -> String {
    // Accès en lecture sur les HashMap
    let users_dico = USERS_DICO.read().unwrap();
    let sessions_dico = SESSIONS_DICO.read().unwrap();

    // Récupérer l'utilisateur correspondant au login
    match users_dico.get(login) {
        Some(client) => {
            let mut result = Vec::new();
            for session in sessions_dico.values() {
                let mut descr = session.id.clone(); // Pour pouvoir ajouter le CLOSED_TAG
                // Si la session est fermée et que l'utilisateur est un étudiant => on passe à la suivante
                if !session.openned && client.is_student() { continue; }
                // Si la session est fermée et que l'utilisateur n'est pas un étudiant => on ajoute le CLOSED_TAG
                if !session.openned && !client.is_student() { descr += CLOSED_TAG; }
                // Vérifier si l'utilisateur est listé dans les utilisateurs de la session
                if session.users.contains(&login.to_string()) && !result.contains(&session.id) {
                    result.push(descr.clone());
                }
                // Vérifier si un groupe de l'utilisateur est listé dans les groupes de la session
                for group_id in &session.groups {
                    if client.groups.contains(group_id) && !result.contains(&session.id) {
                        result.push(descr.clone());
                    }
                }
            }
            result.join(";") // Retourner une chaîne avec séparateur
        }
        None => {
            String::new() // Chaîne vide si le login est inconnu
        }
    }
}

// ----------------------------------------------------------------------------
// Procède à un transfert de session pour un utilisateur donné
// ----------------------------------------------------------------------------
// Usage: transfer_user("jdoe", "session1", "session3", "PROG_DIR")?;

// Note: procédure de transfert de session

#[allow(dead_code)]

fn transfer_user(user_login: &str, from_session: &str, to_session: &str, base_dir: &str) -> Result<(), io::Error> {
    let mut sessions_dico = SESSIONS_DICO.write().unwrap();

    // Retirer temporairement les entrées pour éviter des emprunts multiples
    let from_session_entry = sessions_dico.remove(from_session);
    let to_session_entry = sessions_dico.remove(to_session);

    if from_session_entry.is_none() || to_session_entry.is_none() {
        // Réinsérer les sessions retirées pour retrouver l'état initial
        if let Some(entry) = from_session_entry {
            sessions_dico.insert(from_session.to_string(), entry);
        }
        if let Some(entry) = to_session_entry {
            sessions_dico.insert(to_session.to_string(), entry);
        }

        return Err(io::Error::new(
            io::ErrorKind::NotFound,
            "Session source ou destination introuvable",
        ));
    }

    let mut from_session = from_session_entry.unwrap();
    let mut to_session = to_session_entry.unwrap();

    // Vérifier si l'utilisateur est dans la session source, explicitement ou via un groupe
    let user_in_from_session =
        from_session.users.iter().any(|u| u == user_login) ||
        from_session.groups.iter().any(|group| group.contains(user_login));

    if user_in_from_session {
        // Si l'utilisateur est dans la session source, le retirer de la session source
        if let Some(pos) = from_session.users.iter().position(|u| u == user_login) {
            from_session.users.remove(pos);
        }

        // Ajouter l'utilisateur à la session destination
        if !to_session.users.contains(&user_login.to_string()) {
            to_session.users.push(user_login.to_string());
        }

        // Renommer le dossier utilisateur
        let from_path = format!("{}/{}/{}", base_dir, from_session.id, user_login);
        let to_path = format!("{}/{}/{}", base_dir, to_session.id, user_login);

        if fs::metadata(&from_path).is_ok() {
            fs::rename(&from_path, &to_path)?;
        }
    } else {
        // Réinsérer les sessions dans le dictionnaire
        sessions_dico.insert(from_session.id.clone(), from_session);
        sessions_dico.insert(to_session.id.clone(), to_session);

        return Err(io::Error::new(
            io::ErrorKind::NotFound,
            "Utilisateur non trouvé dans la session source",
        ));
    }

    // Réinsérer les sessions dans le dictionnaire après modifications
    sessions_dico.insert(from_session.id.clone(), from_session);
    sessions_dico.insert(to_session.id.clone(), to_session);

    Ok(())
}

// ============================================================================
// Fonctions utilisée par le serveur d'administration web
// ============================================================================

#[derive(Debug, Clone)]
pub struct UserDataAdmin {
    pub login: String,
    pub status: String,
    pub groups: String,
    pub fullname: String,
    pub mail: String,
    pub date: String,
    pub addr: String,
    pub session_id: String,
}

#[derive(Debug, Clone)]
pub struct SessionDataAdmin {
    pub id: String,
    pub openned: bool,
    pub groups: String,
    pub users: String,
    pub total_users: usize,
}

// Fonction pour récupérer tous les utilisateurs
pub fn get_userdata_all() -> Vec<UserDataAdmin> {
    let users_dico = USERS_DICO.read().unwrap();
    let mut result: Vec<UserDataAdmin> = Vec::new();
    
    for user in users_dico.values() {
        result.push(UserDataAdmin {
            login: user.login.clone(),
            status: user.status.to_string(),
            groups: user.groups.join(", "),
            fullname: user.fullname.clone(),
            mail: user.mail.clone(),
            date: user.date.clone(),
            addr: user.addr.clone(),
            session_id: user.session_id.clone(),
        });
    }
    
    // Trier par login
    result.sort_by(|a, b| a.login.cmp(&b.login));
    result
}

// Fonction pour récupérer toutes les sessions
pub fn get_sessions_all() -> Vec<SessionDataAdmin> {
    let sessions_dico = SESSIONS_DICO.read().unwrap();
    let mut result: Vec<SessionDataAdmin> = Vec::new();
    
    for session in sessions_dico.values() {
        // Calculer le nombre total d'utilisateurs dans cette session
        let total_users = session.get_userlist().len();
        
        result.push(SessionDataAdmin {
            id: session.id.clone(),
            openned: session.openned,
            groups: session.groups.join(" "),
            users: session.users.join(" "),
            total_users,
        });
    }
    
    // Trier par ID de session
    result.sort_by(|a, b| a.id.cmp(&b.id));
    result
}

// ============================================================================
// Gestion du changement de groupe d'un étudiant
// ============================================================================

pub fn get_groups_all() -> Vec<String> {
    let groups = GROUPS_LIST.read().unwrap();
    let mut result = groups.clone();
    result.sort();
    result
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MoveSuggestion {
    pub from_session: String,
    pub to_session: Option<String>,
    pub source_exists: bool,
    pub source_file_count: usize,
    pub target_exists: bool,
    pub blocked: bool,
    pub warning: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GroupChangeSimulation {
    pub login: String,
    pub fullname: String,
    pub old_group: String,
    pub new_group: String,
    pub current_groups: Vec<String>,
    pub resulting_groups: Vec<String>,
    pub unchanged_sessions: Vec<String>,
    pub departed_sessions: Vec<String>,
    pub arrived_sessions: Vec<String>,
    pub moves: Vec<MoveSuggestion>,
    pub is_connected: bool,
    pub can_apply: bool,
    pub block_reason: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct SessionMoveInput {
    pub from: String,
    pub to: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct MoveExecutionResult {
    pub from_session: String,
    pub to_session: String,
    pub source_existed: bool,
    pub files_moved: bool,
    pub message: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct GroupChangeResult {
    pub status: String,
    pub login: String,
    pub old_group: String,
    pub new_group: String,
    pub resulting_groups: Vec<String>,
    pub moves: Vec<MoveExecutionResult>,
    pub disconnected: bool,
    pub xml_saved: bool,
    pub message: String,
}

// Compte récursivement le nombre de fichiers dans un répertoire
fn count_files_in_dir(path: &Path) -> usize {
    if !path.is_dir() {
        return 0;
    }
    let mut count = 0;
    if let Ok(entries) = fs::read_dir(path) {
        for entry in entries.flatten() {
            let p = entry.path();
            if p.is_file() {
                count += 1;
            } else if p.is_dir() {
                count += count_files_in_dir(&p);
            }
        }
    }
    count
}

// Extrait le préfixe du module d'une session (ex: "IntroProg" pour "IntroProg_TP1")
fn session_module_prefix(session_id: &str) -> &str {
    if let Some(idx) = session_id.find("_TP") {
        return &session_id[..idx];
    }
    if let Some(idx) = session_id.rfind('_') {
        return &session_id[..idx];
    }
    session_id
}

// Simule un changement de groupe pour un étudiant
pub fn simulate_group_change(
    login: &str,
    old_group: &str,
    new_group: &str,
    is_connected: bool,
) -> Result<GroupChangeSimulation, String> {
    // 1. Vérification de l'étudiant
    let (fullname, current_groups) = {
        let dico = USERS_DICO.read().unwrap();
        let client = dico.get(login).ok_or_else(|| format!("Utilisateur '{}' introuvable", login))?;
        if client.status != UserStatus::STUDENT {
            return Err(format!("L'utilisateur '{}' n'est pas un étudiant ({:?})", login, client.status));
        }
        if !client.groups.contains(&old_group.to_string()) {
            return Err(format!("L'étudiant '{}' n'appartient pas au groupe '{}'", login, old_group));
        }
        (client.fullname.clone(), client.groups.clone())
    };

    // 2. Vérification du nouveau groupe
    {
        let groups_list = GROUPS_LIST.read().unwrap();
        if !groups_list.contains(&new_group.to_string()) {
            return Err(format!("Le groupe cible '{}' n'existe pas", new_group));
        }
    }

    if old_group == new_group {
        return Err("Le nouveau groupe doit être différent de l'ancien groupe".to_string());
    }

    // 3. Calcul des nouveaux groupes
    let mut resulting_groups = current_groups.clone();
    if let Some(pos) = resulting_groups.iter().position(|g| g == old_group) {
        resulting_groups[pos] = new_group.to_string();
    }
    resulting_groups.sort();
    resulting_groups.dedup();

    // 4. Calcul des sessions impactées
    let (unchanged_sessions, departed_sessions, arrived_sessions) = {
        let sessions_dico = SESSIONS_DICO.read().unwrap();
        let mut unchanged = Vec::new();
        let mut departed = Vec::new();
        let mut arrived = Vec::new();

        let has_student = |session: &Session, groups: &[String]| -> bool {
            if session.users.contains(&login.to_string()) {
                return true;
            }
            session.groups.iter().any(|g| groups.contains(g))
        };

        for session in sessions_dico.values() {
            let in_before = has_student(session, &current_groups);
            let in_after = has_student(session, &resulting_groups);

            if in_before && in_after {
                unchanged.push(session.id.clone());
            } else if in_before && !in_after {
                departed.push(session.id.clone());
            } else if !in_before && in_after {
                arrived.push(session.id.clone());
            }
        }

        unchanged.sort();
        departed.sort();
        arrived.sort();
        (unchanged, departed, arrived)
    };

    // 5. Appariement intelligent des sessions quittées vers les sessions d'arrivée
    let mut unmatched_arrived = arrived_sessions.clone();
    let mut move_suggestions = Vec::new();
    let mut can_apply = true;
    let mut block_reason = None;

    for from_sess in &departed_sessions {
        let from_prefix = session_module_prefix(from_sess);
        let mut best_idx = None;

        // Priorité 1 : même préfixe de module (ex: IntroProg_TP1 -> IntroProg_TP2)
        for (i, to_sess) in unmatched_arrived.iter().enumerate() {
            if session_module_prefix(to_sess) == from_prefix {
                best_idx = Some(i);
                break;
            }
        }

        // Priorité 2 : s'il n'y a qu'une seule session quittée et une seule rejointe
        if best_idx.is_none() && departed_sessions.len() == 1 && unmatched_arrived.len() == 1 {
            best_idx = Some(0);
        }

        let to_sess_opt = if let Some(idx) = best_idx {
            Some(unmatched_arrived.remove(idx))
        } else {
            None
        };

        // Vérification sur le système de fichiers
        let source_path = Path::new(PROG_DIR).join(from_sess).join(login);
        let source_exists = source_path.is_dir();
        let source_file_count = if source_exists {
            count_files_in_dir(&source_path)
        } else {
            0
        };

        let mut target_exists = false;
        let mut blocked = false;
        let mut warning = None;

        if let Some(ref to_sess) = to_sess_opt {
            let target_path = Path::new(PROG_DIR).join(to_sess).join(login);
            if target_path.exists() {
                target_exists = true;
                blocked = true;
                can_apply = false;
                let msg = format!(
                    "Le dossier '{}' existe déjà dans la session cible '{}'. Opération bloquée.",
                    login, to_sess
                );
                warning = Some(msg.clone());
                block_reason = Some(msg);
            }
        } else {
            warning = Some("Aucune session de destination automatique trouvée pour ce dossier.".to_string());
        }

        move_suggestions.push(MoveSuggestion {
            from_session: from_sess.clone(),
            to_session: to_sess_opt,
            source_exists,
            source_file_count,
            target_exists,
            blocked,
            warning,
        });
    }

    Ok(GroupChangeSimulation {
        login: login.to_string(),
        fullname,
        old_group: old_group.to_string(),
        new_group: new_group.to_string(),
        current_groups,
        resulting_groups,
        unchanged_sessions,
        departed_sessions,
        arrived_sessions,
        moves: move_suggestions,
        is_connected,
        can_apply,
        block_reason,
    })
}

// Applique le changement de groupe et déplace les dossiers de session
pub fn apply_group_change(
    login: &str,
    old_group: &str,
    new_group: &str,
    moves: Vec<SessionMoveInput>,
) -> Result<GroupChangeResult, String> {
    // 1. Vérification de l'étudiant
    let resulting_groups = {
        let dico = USERS_DICO.read().unwrap();
        let client = dico.get(login).ok_or_else(|| format!("Utilisateur '{}' introuvable", login))?;
        if client.status != UserStatus::STUDENT {
            return Err(format!("L'utilisateur '{}' n'est pas un étudiant", login));
        }
        if !client.groups.contains(&old_group.to_string()) {
            return Err(format!("L'étudiant '{}' n'appartient pas au groupe '{}'", login, old_group));
        }
        let mut groups = client.groups.clone();
        if let Some(pos) = groups.iter().position(|g| g == old_group) {
            groups[pos] = new_group.to_string();
        }
        groups.sort();
        groups.dedup();
        groups
    };

    // 2. Vérification du groupe cible
    {
        let groups_list = GROUPS_LIST.read().unwrap();
        if !groups_list.contains(&new_group.to_string()) {
            return Err(format!("Le groupe cible '{}' n'existe pas", new_group));
        }
    }

    // 3. Règle absolue de sécurité (Opération bloquée si dossier cible existant)
    for m in &moves {
        let target_path = Path::new(PROG_DIR).join(&m.to).join(login);
        if target_path.exists() {
            return Err(format!(
                "Opération bloquée : le dossier de destination '{}' existe déjà sur le serveur. Aucun fichier déplacé.",
                target_path.display()
            ));
        }
    }

    // 4. Déplacement physique des répertoires
    let mut execution_results = Vec::new();

    for m in &moves {
        let from_path = Path::new(PROG_DIR).join(&m.from).join(login);
        let to_session_dir = Path::new(PROG_DIR).join(&m.to);
        let to_path = to_session_dir.join(login);

        // S'assurer que le dossier parent de la session de destination existe
        if let Err(e) = utils::ensure_dir_exists(&to_session_dir) {
            return Err(format!(
                "Impossible de créer le répertoire de session '{}' : {}",
                to_session_dir.display(),
                e
            ));
        }

        if from_path.is_dir() {
            // Déplacer le dossier
            if let Err(e) = fs::rename(&from_path, &to_path) {
                return Err(format!(
                    "Échec du déplacement de '{}' vers '{}' : {}",
                    from_path.display(),
                    to_path.display(),
                    e
                ));
            }
            execution_results.push(MoveExecutionResult {
                from_session: m.from.clone(),
                to_session: m.to.clone(),
                source_existed: true,
                files_moved: true,
                message: format!("Dossier déplacé de {} vers {}", m.from, m.to),
            });
            log::info!("Moved student folder {} from {} to {}", login, m.from, m.to);
        } else {
            // Le dossier source n'existait pas encore sur le disque : créer le dossier dans la nouvelle session
            if let Err(e) = utils::ensure_dir_exists(&to_path) {
                return Err(format!(
                    "Impossible de créer le dossier étudiant '{}' : {}",
                    to_path.display(),
                    e
                ));
            }
            execution_results.push(MoveExecutionResult {
                from_session: m.from.clone(),
                to_session: m.to.clone(),
                source_existed: false,
                files_moved: false,
                message: format!("Dossier source inexistant, nouveau dossier créé dans {}", m.to),
            });
            log::info!("Created empty student folder for {} in {}", login, m.to);
        }
    }

    // 5. Mise à jour de USERS_DICO
    {
        let mut dico = USERS_DICO.write().unwrap();
        if let Some(client) = dico.get_mut(login) {
            client.groups = resulting_groups.clone();
        }
    }

    // 6. Nettoyage des sessions_dico (retirer login de sessions.users si présent dans les sessions quittées)
    {
        let mut sess_dico = SESSIONS_DICO.write().unwrap();
        for m in &moves {
            if let Some(session) = sess_dico.get_mut(&m.from) {
                session.users.retain(|u| u != login);
            }
        }
    }

    // 7. Sauvegarde du fichier XML et du backup daté
    save_database(CONFIG_XML);
    log::info!(
        "Group change applied for student {}: {} -> {}. Database saved to {}",
        login, old_group, new_group, CONFIG_XML
    );

    Ok(GroupChangeResult {
        status: "success".to_string(),
        login: login.to_string(),
        old_group: old_group.to_string(),
        new_group: new_group.to_string(),
        resulting_groups,
        moves: execution_results,
        disconnected: false,
        xml_saved: true,
        message: format!(
            "Groupe mis à jour avec succès ({} ➔ {}) et dossiers déplacés pour l'étudiant {}",
            old_group, new_group, login
        ),
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Mutex;

    static TEST_MUTEX: Mutex<()> = Mutex::new(());

    #[test]
    fn test_session_module_prefix() {
        assert_eq!(session_module_prefix("IntroProg_TP1"), "IntroProg");
        assert_eq!(session_module_prefix("Python_TP3"), "Python");
        assert_eq!(session_module_prefix("Test_1"), "Test");
        assert_eq!(session_module_prefix("SimpleSession"), "SimpleSession");
    }

    #[test]
    fn test_group_change_validation() {
        let _lock = TEST_MUTEX.lock().unwrap();
        // Initialiser temporairement les structures en mémoire
        {
            let mut groups = GROUPS_LIST.write().unwrap();
            groups.clear();
            groups.push("Group_1".to_string());
            groups.push("Group_2".to_string());
            groups.push("Group_3".to_string());
        }
        {
            let mut users = USERS_DICO.write().unwrap();
            users.clear();
            users.insert("student1".to_string(), ClientData {
                login: "student1".to_string(),
                status: UserStatus::STUDENT,
                groups: vec!["Group_1".to_string(), "Group_2".to_string()],
                fullname: "Student One".to_string(),
                mail: "--".to_string(),
                date: "--".to_string(),
                addr: "--".to_string(),
                passwd: "pass".to_string(),
                session_id: "--".to_string(),
                work_time: HashMap::new(),
            });
            users.insert("tutor1".to_string(), ClientData {
                login: "tutor1".to_string(),
                status: UserStatus::TUTOR,
                groups: vec![],
                fullname: "Tutor One".to_string(),
                mail: "--".to_string(),
                date: "--".to_string(),
                addr: "--".to_string(),
                passwd: "pass".to_string(),
                session_id: "--".to_string(),
                work_time: HashMap::new(),
            });
        }
        {
            let mut sessions = SESSIONS_DICO.write().unwrap();
            sessions.clear();
            sessions.insert("Session_A".to_string(), Session {
                id: "Session_A".to_string(),
                openned: true,
                groups: vec!["Group_1".to_string()],
                users: vec![],
            });
            sessions.insert("Session_B".to_string(), Session {
                id: "Session_B".to_string(),
                openned: true,
                groups: vec!["Group_2".to_string()],
                users: vec![],
            });
            sessions.insert("Session_C".to_string(), Session {
                id: "Session_C".to_string(),
                openned: true,
                groups: vec!["Group_3".to_string()],
                users: vec![],
            });
        }

        // Test non-student error
        let err = simulate_group_change("tutor1", "Group_1", "Group_2", false);
        assert!(err.is_err());
        assert!(err.unwrap_err().contains("n'est pas un étudiant"));

        // Test student does not have old_group
        let err = simulate_group_change("student1", "Group_3", "Group_2", false);
        assert!(err.is_err());
        assert!(err.unwrap_err().contains("n'appartient pas au groupe"));

        // Test target group does not exist
        let err = simulate_group_change("student1", "Group_1", "Group_NonExistent", false);
        assert!(err.is_err());
        assert!(err.unwrap_err().contains("n'existe pas"));

        // Test same group
        let err = simulate_group_change("student1", "Group_1", "Group_1", false);
        assert!(err.is_err());

        // Test valid simulation: student1 in Group_1 and Group_2, replace Group_1 by Group_3
        let sim = simulate_group_change("student1", "Group_1", "Group_3", false).unwrap();
        assert_eq!(sim.login, "student1");
        assert_eq!(sim.old_group, "Group_1");
        assert_eq!(sim.new_group, "Group_3");
        assert_eq!(sim.unchanged_sessions, vec!["Session_B"]); // Session_B relies on Group_2 which is kept
        assert_eq!(sim.departed_sessions, vec!["Session_A"]);  // Session_A relies on Group_1 which is left
        assert_eq!(sim.arrived_sessions, vec!["Session_C"]);   // Session_C relies on Group_3 which is joined
        assert_eq!(sim.moves.len(), 1);
        assert_eq!(sim.moves[0].from_session, "Session_A");
        assert_eq!(sim.moves[0].to_session, Some("Session_C".to_string()));
    }

    #[test]
    fn test_blocked_when_target_folder_exists() {
        let _lock = TEST_MUTEX.lock().unwrap();
        // Préparer un dossier cible existant sur disque
        let target_dir = format!("{}/Session_C/student_blocked", PROG_DIR);
        let _ = fs::create_dir_all(&target_dir);

        {
            let mut groups = GROUPS_LIST.write().unwrap();
            groups.clear();
            groups.push("Group_1".to_string());
            groups.push("Group_3".to_string());
        }
        {
            let mut users = USERS_DICO.write().unwrap();
            users.clear();
            users.insert("student_blocked".to_string(), ClientData {
                login: "student_blocked".to_string(),
                status: UserStatus::STUDENT,
                groups: vec!["Group_1".to_string()],
                fullname: "Blocked Student".to_string(),
                mail: "--".to_string(),
                date: "--".to_string(),
                addr: "--".to_string(),
                passwd: "pass".to_string(),
                session_id: "--".to_string(),
                work_time: HashMap::new(),
            });
        }
        {
            let mut sessions = SESSIONS_DICO.write().unwrap();
            sessions.clear();
            sessions.insert("Session_A".to_string(), Session {
                id: "Session_A".to_string(),
                openned: true,
                groups: vec!["Group_1".to_string()],
                users: vec![],
            });
            sessions.insert("Session_C".to_string(), Session {
                id: "Session_C".to_string(),
                openned: true,
                groups: vec!["Group_3".to_string()],
                users: vec![],
            });
        }

        // La simulation doit détecter que le dossier cible existe déjà et bloquer
        let sim = simulate_group_change("student_blocked", "Group_1", "Group_3", false).unwrap();
        assert!(!sim.can_apply, "can_apply doit être false car le dossier cible existe");
        assert!(sim.moves[0].blocked, "Le move doit être marqué comme bloqué");
        assert!(sim.moves[0].target_exists, "target_exists doit être true");
        assert!(sim.block_reason.is_some(), "Une raison de blocage doit être présente");

        // L'application doit également être bloquée fermement
        let apply_res = apply_group_change(
            "student_blocked",
            "Group_1",
            "Group_3",
            vec![SessionMoveInput {
                from: "Session_A".to_string(),
                to: "Session_C".to_string(),
            }],
        );
        assert!(apply_res.is_err(), "L'application doit échouer");
        assert!(apply_res.unwrap_err().contains("Opération bloquée"));

        // Nettoyage du dossier temporaire
        let _ = fs::remove_dir_all(&target_dir);
        let _ = fs::remove_dir(format!("{}/Session_C", PROG_DIR));
    }

    #[test]
    fn test_apply_group_change_success() {
        let _lock = TEST_MUTEX.lock().unwrap();
        let source_dir = format!("{}/Session_Src/student_ok", PROG_DIR);
        let target_dir = format!("{}/Session_Dst/student_ok", PROG_DIR);
        let _ = fs::create_dir_all(&source_dir);
        let test_file = format!("{}/code.c", source_dir);
        let _ = fs::write(&test_file, b"int main() { return 0; }");

        {
            let mut groups = GROUPS_LIST.write().unwrap();
            groups.clear();
            groups.push("Group_Src".to_string());
            groups.push("Group_Dst".to_string());
        }
        {
            let mut users = USERS_DICO.write().unwrap();
            users.clear();
            users.insert("student_ok".to_string(), ClientData {
                login: "student_ok".to_string(),
                status: UserStatus::STUDENT,
                groups: vec!["Group_Src".to_string()],
                fullname: "Student Success".to_string(),
                mail: "--".to_string(),
                date: "--".to_string(),
                addr: "--".to_string(),
                passwd: "pass".to_string(),
                session_id: "--".to_string(),
                work_time: HashMap::new(),
            });
        }
        {
            let mut sessions = SESSIONS_DICO.write().unwrap();
            sessions.clear();
            sessions.insert("Session_Src".to_string(), Session {
                id: "Session_Src".to_string(),
                openned: true,
                groups: vec!["Group_Src".to_string()],
                users: vec![],
            });
            sessions.insert("Session_Dst".to_string(), Session {
                id: "Session_Dst".to_string(),
                openned: true,
                groups: vec!["Group_Dst".to_string()],
                users: vec![],
            });
        }

        let apply_res = apply_group_change(
            "student_ok",
            "Group_Src",
            "Group_Dst",
            vec![SessionMoveInput {
                from: "Session_Src".to_string(),
                to: "Session_Dst".to_string(),
            }],
        ).unwrap();

        assert_eq!(apply_res.status, "success");
        assert_eq!(apply_res.resulting_groups, vec!["Group_Dst"]);
        assert!(apply_res.moves[0].files_moved);

        // Vérifier que le dossier source n'existe plus et que le dossier cible existe avec le fichier
        assert!(!Path::new(&source_dir).exists(), "Le dossier source doit avoir été déplacé");
        assert!(Path::new(&target_dir).exists(), "Le dossier cible doit exister");
        let moved_file = format!("{}/code.c", target_dir);
        assert!(Path::new(&moved_file).exists(), "Le fichier étudiant doit être présent dans la cible");

        // Nettoyage
        let _ = fs::remove_dir_all(format!("{}/Session_Dst", PROG_DIR));
        let _ = fs::remove_dir_all(format!("{}/Session_Src", PROG_DIR));
    }
}

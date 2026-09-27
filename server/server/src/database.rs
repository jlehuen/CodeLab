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

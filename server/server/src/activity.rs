// ============================================================================
// Fichier : activity.rs
// Version : 03/11/26
// Auteur  : Jérôme Lehuen
// Projet  : Serveur CodeLab
// ============================================================================
// Module de traçage complet de l'activité des étudiants :
// - Calcul des diffs textuels (crate `similar`) et détection d'alertes
// - Suivi des compilations (succès / erreurs) et exécutions (runs / crashes)
// - Suivi de focus (perte/gain, temps hors de l'IDE)
// - Suivi du copier-coller (PASTE) et de la frappe réelle (TYPING)
// - Suivi des glisser-déposer de fichiers externes (FILE_DROP)
// - Suivi des opérations d'arborescence (FILE_OP : create, delete, rename, move)
// - Suivi des demandes d'aide tuteur (HELP : requested, cancelled, resolved)
// - Suivi de la consultation de fichiers (VIEW : onglets actifs)
// - Suivi des bornes de session (SESSION : connected, disconnected)
// - Journalisation append-only unifiée au format JSON Lines (.activity.jsonl)

use std::collections::HashMap;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::path::Path;
use std::sync::Mutex;
use std::time::Instant;

use chrono::Utc;
use lazy_static::lazy_static;
use serde::{Deserialize, Serialize};

use crate::constants::PROG_DIR;

// ----------------------------------------------------------------------------
// Types et structures
// ----------------------------------------------------------------------------

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(tag = "type")]
pub enum ActivityEvent {
    #[serde(rename = "EDIT")]
    Edit {
        timestamp: String,        // ISO 8601 UTC
        session: String,          // Nom de la session
        login: String,            // Identifiant de l'étudiant
        file: String,             // Chemin relatif du fichier
        total_lines: usize,       // Nombre total de lignes après modification
        lines_added: usize,       // Nombre de lignes ajoutées
        lines_removed: usize,     // Nombre de lignes supprimées
        chunk_max: usize,         // Plus grand bloc continu de lignes insérées
        dt_sec: Option<u64>,      // Secondes écoulées depuis la modification précédente
        alert: Option<String>,    // Alerte ("BURST_INSERT", "BURST_NEW_FILE", etc.)
    },
    #[serde(rename = "COMPILE")]
    Compile {
        timestamp: String,
        session: String,
        login: String,
        file: String,
        status: String,           // "OK" ou "ERROR"
        #[serde(default, skip_serializing_if = "Option::is_none")]
        error_msg: Option<String>,
    },
    #[serde(rename = "EXECUTE")]
    Execute {
        timestamp: String,
        session: String,
        login: String,
        file: String,
        status: String,           // "STARTED", "DONE", "ERROR", "HALTED"
        #[serde(default, skip_serializing_if = "Option::is_none")]
        exit_code: Option<i32>,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        error_msg: Option<String>,
    },
    #[serde(rename = "FOCUS")]
    Focus {
        timestamp: String,
        session: String,
        login: String,
        status: String,           // "LOST" ou "GAINED"
        #[serde(default, skip_serializing_if = "Option::is_none")]
        duration_sec: Option<u64>,
    },
    #[serde(rename = "PASTE")]
    Paste {
        timestamp: String,
        session: String,
        login: String,
        file: String,
        chars: usize,
        lines: usize,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        internal: Option<bool>,
    },
    #[serde(rename = "TYPING")]
    Typing {
        timestamp: String,
        session: String,
        login: String,
        file: String,
        typed_chars: usize,
        backspaces: usize,
    },
    #[serde(rename = "FILE_DROP")]
    FileDrop {
        timestamp: String,
        session: String,
        login: String,
        file: String,
        size_bytes: u64,
    },
    #[serde(rename = "FILE_OP")]
    FileOp {
        timestamp: String,
        session: String,
        login: String,
        op: String,               // "CREATE", "DELETE", "RENAME", "MOVE"
        file: String,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        dest: Option<String>,
    },
    #[serde(rename = "HELP")]
    Help {
        timestamp: String,
        session: String,
        login: String,
        status: String,           // "REQUESTED", "CANCELLED", "RESOLVED"
        #[serde(default, skip_serializing_if = "Option::is_none")]
        tutor: Option<String>,
    },
    #[serde(rename = "VIEW")]
    View {
        timestamp: String,
        session: String,
        login: String,
        file: String,
    },
    #[serde(rename = "SESSION")]
    Session {
        timestamp: String,
        session: String,
        login: String,
        status: String,           // "CONNECTED", "DISCONNECTED"
        #[serde(default, skip_serializing_if = "Option::is_none")]
        ip: Option<String>,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        duration_sec: Option<u64>,
    },
    #[serde(rename = "GENERIC")]
    Generic {
        timestamp: String,
        session: String,
        login: String,
        message: String,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LiveAlert {
    pub code: String,       // "BURST", "PASTE", "THRASH", "DROP"
    pub level: String,      // "CRIT", "WARN", "INFO"
    pub message: String,
    pub timestamp: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LiveMetrics {
    pub s_auth: f64,
    pub i_nav: f64,
    pub typed_chars: usize,
    pub backspaces: usize,
    pub pasted_chars: usize,
    pub pasted_lines: usize,
    pub internal_pasted_chars: usize,
    pub internal_pasted_lines: usize,
    pub burst_chars: usize,
    pub burst_lines: usize,
    pub dropped_files: usize,
    pub total_away_sec: u64,
    pub consecutive_errors: usize,
}

impl Default for LiveMetrics {
    fn default() -> Self {
        Self {
            s_auth: -1.0,
            i_nav: 0.0,
            typed_chars: 0,
            backspaces: 0,
            pasted_chars: 0,
            pasted_lines: 0,
            internal_pasted_chars: 0,
            internal_pasted_lines: 0,
            burst_chars: 0,
            burst_lines: 0,
            dropped_files: 0,
            total_away_sec: 0,
            consecutive_errors: 0,
        }
    }
}

struct LastEditInfo {
    instant: Instant,
}

#[derive(Default)]
struct StudentLiveStatus {
    consecutive_errors: usize,
    last_error_time: Option<Instant>,
    typed_chars: usize,
    backspaces: usize,
    pasted_chars: usize,
    pasted_lines: usize,
    internal_pasted_chars: usize,
    internal_pasted_lines: usize,
    burst_chars: usize,
    burst_lines: usize,
    dropped_files: usize,
    focus_lost_count: usize,
    total_away_sec: u64,
    session_start: Option<Instant>,
}

impl StudentLiveStatus {
    fn compute_s_auth(&self) -> f64 {
        let total = self.typed_chars + self.pasted_chars + self.burst_chars;
        if total == 0 {
            -1.0
        } else {
            let score = (self.typed_chars as f64) / (total as f64) * 100.0;
            (score.clamp(0.0, 100.0) * 10.0).round() / 10.0
        }
    }

    fn compute_i_nav(&self) -> f64 {
        let elapsed_sec = self.session_start.map(|t| t.elapsed().as_secs()).unwrap_or(0);
        if elapsed_sec == 0 {
            0.0
        } else {
            let ratio = (self.total_away_sec as f64) / (elapsed_sec as f64) * 100.0;
            (ratio.clamp(0.0, 100.0) * 10.0).round() / 10.0
        }
    }

    fn to_live_metrics(&self) -> LiveMetrics {
        LiveMetrics {
            s_auth: self.compute_s_auth(),
            i_nav: self.compute_i_nav(),
            typed_chars: self.typed_chars,
            backspaces: self.backspaces,
            pasted_chars: self.pasted_chars,
            pasted_lines: self.pasted_lines,
            internal_pasted_chars: self.internal_pasted_chars,
            internal_pasted_lines: self.internal_pasted_lines,
            burst_chars: self.burst_chars,
            burst_lines: self.burst_lines,
            dropped_files: self.dropped_files,
            total_away_sec: self.total_away_sec,
            consecutive_errors: self.consecutive_errors,
        }
    }
}

lazy_static! {
    static ref LAST_EDITS: Mutex<HashMap<String, LastEditInfo>> = Mutex::new(HashMap::new());
    static ref STUDENT_LIVE: Mutex<HashMap<String, StudentLiveStatus>> = Mutex::new(HashMap::new());
}

// ----------------------------------------------------------------------------
// Chargement et replay résilient du journal d'activité
// ----------------------------------------------------------------------------

pub fn ensure_student_live_loaded(session: &str, login: &str) {
    let key = format!("{}/{}", session, login);
    let mut live_map = STUDENT_LIVE.lock().unwrap();
    if !live_map.contains_key(&key) {
        let mut st = StudentLiveStatus::default();
        let log_file_path = Path::new(PROG_DIR).join(session).join(login).join(".activity.jsonl");
        if let Ok(content) = fs::read_to_string(&log_file_path) {
            for line in content.lines() {
                if line.trim().is_empty() {
                    continue;
                }
                if let Ok(val) = serde_json::from_str::<serde_json::Value>(line) {
                    match val.get("type").and_then(|t| t.as_str()) {
                        Some("TYPING") => {
                            st.typed_chars += val.get("typed_chars").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                            st.backspaces += val.get("backspaces").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                        }
                        Some("PASTE") => {
                            let is_internal = val.get("internal").and_then(|v| v.as_bool()).unwrap_or(false);
                            let chars = val.get("chars").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                            let lines = val.get("lines").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                            if is_internal {
                                st.internal_pasted_chars += chars;
                                st.internal_pasted_lines += lines;
                            } else {
                                st.pasted_chars += chars;
                                st.pasted_lines += lines;
                            }
                        }
                        Some("EDIT") => {
                            if let Some(al) = val.get("alert").and_then(|a| a.as_str()) {
                                if al.starts_with("BURST") {
                                    let lines = val.get("lines_added").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                                    st.burst_lines += lines;
                                    st.burst_chars += lines * 30;
                                }
                            }
                        }
                        Some("FOCUS") => {
                            if val.get("status").and_then(|s| s.as_str()) == Some("GAINED") {
                                st.total_away_sec += val.get("duration_sec").and_then(|v| v.as_u64()).unwrap_or(0);
                            } else if val.get("status").and_then(|s| s.as_str()) == Some("LOST") {
                                st.focus_lost_count += 1;
                            }
                        }
                        Some("FILE_DROP") => {
                            st.dropped_files += 1;
                        }
                        Some("COMPILE") | Some("EXECUTE") => {
                            if val.get("status").and_then(|s| s.as_str()) == Some("ERROR") {
                                st.consecutive_errors += 1;
                            } else if val.get("status").and_then(|s| s.as_str()) == Some("OK")
                                || val.get("status").and_then(|s| s.as_str()) == Some("DONE")
                            {
                                st.consecutive_errors = 0;
                            }
                        }
                        Some("SESSION") => {
                            if st.session_start.is_none() {
                                st.session_start = Some(Instant::now());
                            }
                        }
                        _ => {}
                    }
                }
            }
        }
        live_map.insert(key, st);
    }
}

pub fn get_student_live_metrics(session: &str, login: &str) -> LiveMetrics {
    ensure_student_live_loaded(session, login);
    let key = format!("{}/{}", session, login);
    let live_map = STUDENT_LIVE.lock().unwrap();
    live_map.get(&key).map(|st| st.to_live_metrics()).unwrap_or_default()
}

// ----------------------------------------------------------------------------
// Déclenchement, extinction et propagation d'alertes temps réel vers les tuteurs
// ----------------------------------------------------------------------------

pub fn trigger_live_alert(session: &str, login: &str, code: &str, msg: &str, level: &str) {
    if let Ok(handle) = tokio::runtime::Handle::try_current() {
        let sess = session.to_string();
        let log = login.to_string();
        let c = code.to_string();
        let m = msg.to_string();
        let l = level.to_string();
        handle.spawn(async move {
            crate::client::notify_tutors_user_alert(&sess, &log, &c, &m, &l).await;
        });
    }
}

pub fn clear_live_alert(session: &str, login: &str, code: &str) {
    if let Ok(handle) = tokio::runtime::Handle::try_current() {
        let sess = session.to_string();
        let log = login.to_string();
        let c = code.to_string();
        handle.spawn(async move {
            crate::client::notify_tutors_clear_alert(&sess, &log, &c).await;
        });
    }
}

// ----------------------------------------------------------------------------
// Filtrage des fichiers éligibles au suivi d'activité
// ----------------------------------------------------------------------------

pub fn should_track_file(relative_path: &str) -> bool {
    let path = Path::new(relative_path);

    if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
        if file_name.starts_with('.') {
            return false;
        }
    }

    let lower = relative_path.to_lowercase();
    if lower.ends_with(".bak")
        || lower.ends_with(".tar.zst")
        || lower.ends_with(".zst")
        || lower.ends_with(".zip")
        || lower.ends_with(".tar.gz")
        || lower.ends_with(".jar")
        || lower.ends_with(".class")
        || lower.ends_with(".pyc")
        || lower.ends_with(".o")
        || lower.ends_with(".so")
        || lower.ends_with(".dylib")
        || lower.ends_with(".exe")
        || lower.ends_with(".png")
        || lower.ends_with(".jpg")
        || lower.ends_with(".jpeg")
        || lower.ends_with(".gif")
        || lower.ends_with(".ico")
    {
        return false;
    }

    true
}

// ----------------------------------------------------------------------------
// Persistance d'un événement d'activité
// ----------------------------------------------------------------------------

pub fn append_activity_event(session: &str, login: &str, event: &ActivityEvent) {
    let student_dir = Path::new(PROG_DIR).join(session).join(login);
    if let Err(e) = fs::create_dir_all(&student_dir) {
        log::error!("Impossible de créer le dossier étudiant pour le journal d'activité: {}", e);
        return;
    }

    let log_file_path = student_dir.join(".activity.jsonl");
    if let Ok(line) = serde_json::to_string(event) {
        if let Ok(mut f) = OpenOptions::new().create(true).append(true).open(&log_file_path) {
            let _ = writeln!(f, "{}", line);
        }
    }
}

// ----------------------------------------------------------------------------
// Enregistreurs d'événements spécialisés
// ----------------------------------------------------------------------------

pub fn record_file_op(session: &str, login: &str, op: &str, file: &str, dest: Option<&str>) {
    let event = ActivityEvent::FileOp {
        timestamp: Utc::now().to_rfc3339(),
        session: session.to_string(),
        login: login.to_string(),
        op: op.to_string(),
        file: file.to_string(),
        dest: dest.map(|s| s.to_string()),
    };
    log::info!("[ACTIVITY FILE_OP] session={} login={} op={} file={} dest={:?}", session, login, op, file, dest);
    append_activity_event(session, login, &event);
}

pub fn record_help_event(session: &str, login: &str, status: &str, tutor: Option<&str>) {
    let event = ActivityEvent::Help {
        timestamp: Utc::now().to_rfc3339(),
        session: session.to_string(),
        login: login.to_string(),
        status: status.to_string(),
        tutor: tutor.map(|s| s.to_string()),
    };
    log::info!("[ACTIVITY HELP] session={} login={} status={} tutor={:?}", session, login, status, tutor);
    append_activity_event(session, login, &event);
}

pub fn record_view_event(session: &str, login: &str, file: &str) {
    let event = ActivityEvent::View {
        timestamp: Utc::now().to_rfc3339(),
        session: session.to_string(),
        login: login.to_string(),
        file: file.to_string(),
    };
    log::info!("[ACTIVITY VIEW] session={} login={} file={}", session, login, file);
    append_activity_event(session, login, &event);
}

pub fn record_session_event(session: &str, login: &str, status: &str, ip: Option<&str>, duration_sec: Option<u64>) {
    ensure_student_live_loaded(session, login);
    if status == "CONNECTED" {
        let key = format!("{}/{}", session, login);
        let mut live_map = STUDENT_LIVE.lock().unwrap();
        let st = live_map.entry(key).or_default();
        if st.session_start.is_none() {
            st.session_start = Some(Instant::now());
        }
    }

    let event = ActivityEvent::Session {
        timestamp: Utc::now().to_rfc3339(),
        session: session.to_string(),
        login: login.to_string(),
        status: status.to_string(),
        ip: ip.map(|s| s.to_string()),
        duration_sec,
    };
    log::info!("[ACTIVITY SESSION] session={} login={} status={} ip={:?} duration={:?}", session, login, status, ip, duration_sec);
    append_activity_event(session, login, &event);
}

// ----------------------------------------------------------------------------
// Calcul du diff et journalisation des modifications (EDIT)
// ----------------------------------------------------------------------------

pub fn record_file_activity(
    session: &str,
    login: &str,
    relative_file: &str,
    old_content: Option<&[u8]>,
    new_content: &[u8],
) {
    if !should_track_file(relative_file) {
        return;
    }

    let new_text = match std::str::from_utf8(new_content) {
        Ok(s) => s,
        Err(_) => return,
    };

    let old_text = match old_content {
        Some(bytes) => match std::str::from_utf8(bytes) {
            Ok(s) => s,
            Err(_) => "",
        },
        None => "",
    };

    if !old_text.is_empty() && old_text == new_text {
        return;
    }

    let is_new_file = old_content.is_none();
    let now = Instant::now();
    let tracker_key = format!("{}/{}/{}", session, login, relative_file);

    let dt_sec = {
        let mut map = LAST_EDITS.lock().unwrap();
        let prev = map.insert(tracker_key.clone(), LastEditInfo { instant: now });
        prev.map(|info| now.duration_since(info.instant).as_secs())
    };

    let diff = similar::TextDiff::from_lines(old_text, new_text);

    let mut lines_added = 0;
    let mut lines_removed = 0;
    let mut chunk_max = 0;

    for op in diff.ops() {
        match op.tag() {
            similar::DiffTag::Insert => {
                let count = op.new_range().len();
                lines_added += count;
                if count > chunk_max {
                    chunk_max = count;
                }
            }
            similar::DiffTag::Delete => {
                lines_removed += op.old_range().len();
            }
            similar::DiffTag::Replace => {
                let add_count = op.new_range().len();
                let del_count = op.old_range().len();
                lines_added += add_count;
                lines_removed += del_count;
                if add_count > chunk_max {
                    chunk_max = add_count;
                }
            }
            similar::DiffTag::Equal => {}
        }
    }

    let total_lines = new_text.lines().count();

    let mut alert = None;
    if is_new_file {
        if lines_added >= 25 {
            alert = Some("BURST_NEW_FILE".to_string());
        }
    } else if chunk_max >= 20 {
        match dt_sec {
            Some(dt) if dt <= 15 => alert = Some("BURST_INSERT".to_string()),
            None => alert = Some("BURST_INSERT".to_string()),
            _ => {}
        }
    }

    let event = ActivityEvent::Edit {
        timestamp: Utc::now().to_rfc3339(),
        session: session.to_string(),
        login: login.to_string(),
        file: relative_file.to_string(),
        total_lines,
        lines_added,
        lines_removed,
        chunk_max,
        dt_sec,
        alert: alert.clone(),
    };

    // Réinitialisation de l'acharnement si modification de code réelle
    ensure_student_live_loaded(session, login);
    let key = format!("{}/{}", session, login);

    if lines_added + lines_removed >= 3 {
        let mut had_thrash = false;
        {
            let mut live_map = STUDENT_LIVE.lock().unwrap();
            if let Some(st) = live_map.get_mut(&key) {
                if st.consecutive_errors >= 3 {
                    had_thrash = true;
                }
                st.consecutive_errors = 0;
            }
        }
        if had_thrash {
            clear_live_alert(session, login, "THRASH");
        }
    }

    if let Some(ref al) = alert {
        if al.starts_with("BURST") {
            let mut live_map = STUDENT_LIVE.lock().unwrap();
            let st = live_map.entry(key.clone()).or_default();
            st.burst_lines += lines_added;
            st.burst_chars += lines_added * 30;
        }

        log::warn!(
            "[ACTIVITY ALERT] [{}] session={} login={} file={} (+{}/-{}) chunk_max={} dt={:?}s",
            al, session, login, relative_file, lines_added, lines_removed, chunk_max, dt_sec
        );
        let msg = if al == "BURST_NEW_FILE" {
            format!("Nouveau fichier {} ({}L)", relative_file, lines_added)
        } else {
            format!("+{}L insérées d'un bloc ({})", lines_added, relative_file)
        };
        trigger_live_alert(session, login, "BURST", &msg, "CRIT");
    } else {
        log::info!(
            "[ACTIVITY EDIT] session={} login={} file={} (+{}/-{}) chunk_max={}",
            session, login, relative_file, lines_added, lines_removed, chunk_max
        );
    }

    append_activity_event(session, login, &event);
}

// ----------------------------------------------------------------------------
// Journalisation des événements reçus du client
// ----------------------------------------------------------------------------

#[derive(Deserialize)]
struct ClientLogPayload {
    #[serde(rename = "type")]
    event_type: Option<String>,
    file: Option<String>,
    status: Option<String>,
    exit_code: Option<i32>,
    error_msg: Option<String>,
    duration_sec: Option<u64>,
    chars: Option<usize>,
    lines: Option<usize>,
    typed_chars: Option<usize>,
    backspaces: Option<usize>,
    size_bytes: Option<u64>,
    internal: Option<bool>,
}

pub fn record_client_log(session: &str, login: &str, raw_log: &str) {
    ensure_student_live_loaded(session, login);
    let key = format!("{}/{}", session, login);
    let now = Utc::now().to_rfc3339();

    if let Ok(payload) = serde_json::from_str::<ClientLogPayload>(raw_log) {
        match payload.event_type.as_deref() {
            Some("COMPILE") => {
                let file = payload.file.unwrap_or_else(|| "unknown".to_string());
                let status = payload.status.unwrap_or_else(|| "UNKNOWN".to_string());
                let error_msg = payload.error_msg;

                if status == "ERROR" {
                    log::warn!(
                        "[ACTIVITY COMPILE ERROR] session={} login={} file={} err={:?}",
                        session, login, file, error_msg
                    );
                    let err_cnt;
                    {
                        let mut live_map = STUDENT_LIVE.lock().unwrap();
                        let st = live_map.entry(key.clone()).or_default();
                        st.consecutive_errors += 1;
                        st.last_error_time = Some(Instant::now());
                        err_cnt = st.consecutive_errors;
                    }
                    if err_cnt >= 3 {
                        let msg = format!("{} échecs de compilation consécutifs ({})", err_cnt, file);
                        trigger_live_alert(session, login, "THRASH", &msg, "WARN");
                    }
                } else {
                    log::info!(
                        "[ACTIVITY COMPILE OK] session={} login={} file={}",
                        session, login, file
                    );
                    let mut had_thrash = false;
                    {
                        let mut live_map = STUDENT_LIVE.lock().unwrap();
                        if let Some(st) = live_map.get_mut(&key) {
                            if st.consecutive_errors >= 3 {
                                had_thrash = true;
                            }
                            st.consecutive_errors = 0;
                        }
                    }
                    if had_thrash {
                        clear_live_alert(session, login, "THRASH");
                    }
                }

                let event = ActivityEvent::Compile {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    file,
                    status,
                    error_msg,
                };
                append_activity_event(session, login, &event);
                return;
            }
            Some("EXECUTE") => {
                let file = payload.file.unwrap_or_else(|| "unknown".to_string());
                let status = payload.status.unwrap_or_else(|| "UNKNOWN".to_string());
                let exit_code = payload.exit_code;
                let error_msg = payload.error_msg;

                if status == "ERROR" {
                    log::warn!(
                        "[ACTIVITY EXECUTE ERROR] session={} login={} file={} code={:?} err={:?}",
                        session, login, file, exit_code, error_msg
                    );
                    let err_cnt;
                    {
                        let mut live_map = STUDENT_LIVE.lock().unwrap();
                        let st = live_map.entry(key.clone()).or_default();
                        st.consecutive_errors += 1;
                        st.last_error_time = Some(Instant::now());
                        err_cnt = st.consecutive_errors;
                    }
                    if err_cnt >= 3 {
                        let msg = format!("{} plantages consécutifs ({})", err_cnt, file);
                        trigger_live_alert(session, login, "THRASH", &msg, "WARN");
                    }
                } else {
                    log::info!(
                        "[ACTIVITY EXECUTE {}] session={} login={} file={} code={:?}",
                        status, session, login, file, exit_code
                    );
                    if status == "DONE" {
                        let mut had_thrash = false;
                        {
                            let mut live_map = STUDENT_LIVE.lock().unwrap();
                            if let Some(st) = live_map.get_mut(&key) {
                                if st.consecutive_errors >= 3 {
                                    had_thrash = true;
                                }
                                st.consecutive_errors = 0;
                            }
                        }
                        if had_thrash {
                            clear_live_alert(session, login, "THRASH");
                        }
                    }
                }

                let event = ActivityEvent::Execute {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    file,
                    status,
                    exit_code,
                    error_msg,
                };
                append_activity_event(session, login, &event);
                return;
            }
            Some("FOCUS") => {
                let status = payload.status.unwrap_or_else(|| "UNKNOWN".to_string());
                let duration_sec = payload.duration_sec;

                {
                    let mut live_map = STUDENT_LIVE.lock().unwrap();
                    let st = live_map.entry(key.clone()).or_default();
                    if status == "GAINED" {
                        if let Some(dur) = duration_sec {
                            st.total_away_sec += dur;
                        }
                    } else if status == "LOST" {
                        st.focus_lost_count += 1;
                    }
                }

                if status == "GAINED" {
                    log::info!(
                        "[ACTIVITY FOCUS GAINED] session={} login={} duration={:?}s",
                        session, login, duration_sec
                    );
                } else {
                    log::info!(
                        "[ACTIVITY FOCUS LOST] session={} login={}",
                        session, login
                    );
                }

                let event = ActivityEvent::Focus {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    status,
                    duration_sec,
                };
                append_activity_event(session, login, &event);
                return;
            }
            Some("PASTE") => {
                let file = payload.file.unwrap_or_else(|| "unknown".to_string());
                let chars = payload.chars.unwrap_or(0);
                let lines = payload.lines.unwrap_or(0);
                let is_internal = payload.internal.unwrap_or(false);

                {
                    let mut live_map = STUDENT_LIVE.lock().unwrap();
                    let st = live_map.entry(key.clone()).or_default();
                    if is_internal {
                        st.internal_pasted_chars += chars;
                        st.internal_pasted_lines += lines;
                    } else {
                        st.pasted_chars += chars;
                        st.pasted_lines += lines;
                    }
                }

                if is_internal {
                    log::info!(
                        "[ACTIVITY PASTE INTERNAL] session={} login={} file={} chars={} lines={}",
                        session, login, file, chars, lines
                    );
                } else {
                    log::warn!(
                        "[ACTIVITY PASTE] session={} login={} file={} chars={} lines={}",
                        session, login, file, chars, lines
                    );

                    if lines >= 15 || chars >= 400 {
                        let msg = format!("Collage {}L ({} cars) dans {}", lines, chars, file);
                        trigger_live_alert(session, login, "PASTE", &msg, "WARN");
                    }
                }

                let event = ActivityEvent::Paste {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    file,
                    chars,
                    lines,
                    internal: if is_internal { Some(true) } else { None },
                };
                append_activity_event(session, login, &event);
                return;
            }
            Some("TYPING") => {
                let file = payload.file.unwrap_or_else(|| "unknown".to_string());
                let typed_chars = payload.typed_chars.unwrap_or(0);
                let backspaces = payload.backspaces.unwrap_or(0);

                {
                    let mut live_map = STUDENT_LIVE.lock().unwrap();
                    let st = live_map.entry(key.clone()).or_default();
                    st.typed_chars += typed_chars;
                    st.backspaces += backspaces;
                }

                let event = ActivityEvent::Typing {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    file,
                    typed_chars,
                    backspaces,
                };
                append_activity_event(session, login, &event);
                return;
            }
            Some("FILE_DROP") => {
                let file = payload.file.unwrap_or_else(|| "unknown".to_string());
                let size_bytes = payload.size_bytes.unwrap_or(0);

                {
                    let mut live_map = STUDENT_LIVE.lock().unwrap();
                    let st = live_map.entry(key.clone()).or_default();
                    st.dropped_files += 1;
                }

                log::warn!(
                    "[ACTIVITY FILE_DROP] session={} login={} file={} size={}",
                    session, login, file, size_bytes
                );

                let msg = format!("Fichier {} déposé ({} octets)", file, size_bytes);
                trigger_live_alert(session, login, "DROP", &msg, "WARN");

                let event = ActivityEvent::FileDrop {
                    timestamp: now,
                    session: session.to_string(),
                    login: login.to_string(),
                    file,
                    size_bytes,
                };
                append_activity_event(session, login, &event);
                return;
            }
            _ => {}
        }
    }

    log::info!("[ACTIVITY LOG] session={} login={}: {}", session, login, raw_log);
    let event = ActivityEvent::Generic {
        timestamp: now,
        session: session.to_string(),
        login: login.to_string(),
        message: raw_log.to_string(),
    };
    append_activity_event(session, login, &event);
}

// ----------------------------------------------------------------------------
// Tests unitaires
// ----------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_should_track_file() {
        assert!(should_track_file("main.py"));
        assert!(should_track_file("src/hello.c"));
        assert!(should_track_file("carnet.ipynb"));
        assert!(!should_track_file("hello_231228_203000_bak.c.bak"));
        assert!(!should_track_file(".hidden_file.py"));
        assert!(!should_track_file("archive.tar.zst"));
        assert!(!should_track_file("image.png"));
    }

    #[test]
    fn test_record_file_activity_diff() {
        let session = "test_sess_diff";
        let login = "student1";
        let file = "tp1.py";

        let old_content = b"def hello():\n    print('hello')\n";
        let mut new_str = String::from("def hello():\n    print('hello')\n");
        for i in 0..30 {
            new_str.push_str(&format!("    x{} = {}\n", i, i));
        }

        record_file_activity(session, login, file, Some(old_content), new_str.as_bytes());

        let student_dir = Path::new(PROG_DIR).join(session).join(login);
        let log_file = student_dir.join(".activity.jsonl");
        assert!(log_file.exists());

        let content = fs::read_to_string(&log_file).unwrap();
        assert!(content.contains("\"type\":\"EDIT\""));
        assert!(content.contains("\"file\":\"tp1.py\""));
        assert!(content.contains("\"chunk_max\":30"));
        assert!(content.contains("\"alert\":\"BURST_INSERT\""));

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_record_client_compile_and_execute_logs() {
        let session = "test_sess_logs";
        let login = "student2";

        let compile_json = r#"{"type":"COMPILE","file":"hello.c","status":"ERROR","error_msg":"hello.c:4: syntax error"}"#;
        record_client_log(session, login, compile_json);

        let exec_start_json = r#"{"type":"EXECUTE","file":"tp.py","status":"STARTED"}"#;
        record_client_log(session, login, exec_start_json);

        let exec_err_json = r#"{"type":"EXECUTE","file":"tp.py","status":"ERROR","exit_code":1,"error_msg":"ZeroDivisionError"}"#;
        record_client_log(session, login, exec_err_json);

        let student_dir = Path::new(PROG_DIR).join(session).join(login);
        let log_file = student_dir.join(".activity.jsonl");
        assert!(log_file.exists());

        let content = fs::read_to_string(&log_file).unwrap();
        assert!(content.contains("\"type\":\"COMPILE\""));
        assert!(content.contains("\"status\":\"ERROR\""));
        assert!(content.contains("syntax error"));
        assert!(content.contains("\"type\":\"EXECUTE\""));
        assert!(content.contains("ZeroDivisionError"));

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_record_all_extended_events() {
        let session = "test_sess_ext";
        let login = "student4";

        record_file_op(session, login, "CREATE", "exo2.py", None);
        record_file_op(session, login, "RENAME", "exo2.py", Some("exercice2.py"));
        record_help_event(session, login, "REQUESTED", None);
        record_help_event(session, login, "RESOLVED", Some("tutor1"));
        record_view_event(session, login, "sujet.md");
        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        record_session_event(session, login, "DISCONNECTED", None, Some(3600));

        // Client payload for file drop
        record_client_log(session, login, r#"{"type":"FILE_DROP","file":"solution.py","size_bytes":1024}"#);

        let student_dir = Path::new(PROG_DIR).join(session).join(login);
        let log_file = student_dir.join(".activity.jsonl");
        assert!(log_file.exists());

        let content = fs::read_to_string(&log_file).unwrap();
        assert!(content.contains("\"type\":\"FILE_OP\""));
        assert!(content.contains("\"op\":\"CREATE\""));
        assert!(content.contains("\"op\":\"RENAME\""));
        assert!(content.contains("\"type\":\"HELP\""));
        assert!(content.contains("\"status\":\"RESOLVED\""));
        assert!(content.contains("\"tutor\":\"tutor1\""));
        assert!(content.contains("\"type\":\"VIEW\""));
        assert!(content.contains("\"file\":\"sujet.md\""));
        assert!(content.contains("\"type\":\"SESSION\""));
        assert!(content.contains("\"status\":\"CONNECTED\""));
        assert!(content.contains("\"status\":\"DISCONNECTED\""));
        assert!(content.contains("\"type\":\"FILE_DROP\""));
        assert!(content.contains("\"size_bytes\":1024"));

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_live_metrics_computation() {
        let session = "test_sess_metrics";
        let login = "student_metrics";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        // Avant toute frappe ou collage, s_auth doit être -1.0 (en attente d'activité)
        let init_metrics = get_student_live_metrics(session, login);
        assert_eq!(init_metrics.s_auth, -1.0);

        record_client_log(session, login, r#"{"type":"TYPING","file":"tp.py","typed_chars":800,"backspaces":80}"#);
        record_client_log(session, login, r#"{"type":"PASTE","file":"tp.py","chars":200,"lines":10}"#);

        let metrics = get_student_live_metrics(session, login);
        assert_eq!(metrics.typed_chars, 800);
        assert_eq!(metrics.backspaces, 80);
        assert_eq!(metrics.pasted_chars, 200);
        // s_auth = 800 / (800 + 200) * 100 = 80.0%
        assert_eq!(metrics.s_auth, 80.0);

        // Collage interne (ex: duplication propre d'une fonction d'un fichier à l'autre) : s_auth ne doit PAS baisser !
        record_client_log(session, login, r#"{"type":"PASTE","file":"tp2.py","chars":500,"lines":25,"internal":true}"#);
        let metrics2 = get_student_live_metrics(session, login);
        assert_eq!(metrics2.pasted_chars, 200); // externes restent à 200
        assert_eq!(metrics2.internal_pasted_chars, 500); // internes passent à 500
        assert_eq!(metrics2.s_auth, 80.0); // inchangé !

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }
}

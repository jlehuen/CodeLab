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
    pub profile_trend: String,
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
            profile_trend: "OBSERVING".to_string(),
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
    recent_internal_paste: HashMap<String, (Instant, usize)>,
    recent_external_paste: HashMap<String, (Instant, usize)>,
    external_injected_chars: usize,
    burst_chars: usize,
    burst_lines: usize,
    dropped_files: usize,
    focus_lost_count: usize,
    total_away_sec: u64,
    total_session_sec: u64,
    is_connected: bool,
    session_start: Option<Instant>,
    last_activity: Option<Instant>,
    total_runs: usize,
    total_ok_runs: usize,
    total_errors: usize,
    thrash_episodes: usize,
    thrash_error_runs: usize,
    help_requests: usize,
}

const MAX_FOCUS_AWAY_SEC: u64 = 1800;
const INTERNAL_PASTE_WINDOW_SECS: u64 = 30;

/// Détermine si un fichier déposé par FILE_DROP est un fichier de code source ou carnet
/// (exclut les fichiers de données comme .csv, .txt, .md, .json fournis par l'enseignant).
pub fn is_code_file_extension(file: &str) -> bool {
    let lower = file.to_lowercase();
    matches!(
        Path::new(&lower).extension().and_then(|e| e.to_str()),
        Some(
            "c" | "h"
                | "cpp"
                | "cc"
                | "cxx"
                | "hpp"
                | "py"
                | "hs"
                | "lhs"
                | "java"
                | "ipynb"
                | "js"
                | "ts"
                | "sh"
                | "rs"
                | "ml"
                | "mli"
                | "sql"
        )
    )
}

/// Extrait uniquement le code source des cellules de type "code" d'un carnet Jupyter (.ipynb)
/// pour éviter les faux positifs BURST sur les métadonnées JSON ou les sorties (outputs).
fn extract_ipynb_code_text(raw: &str) -> Option<String> {
    let val: serde_json::Value = serde_json::from_str(raw).ok()?;
    let cells = val.get("cells")?.as_array()?;
    let mut out = String::new();
    for cell in cells {
        if cell.get("cell_type").and_then(|v| v.as_str()) != Some("code") {
            continue;
        }
        if let Some(src) = cell.get("source") {
            if let Some(arr) = src.as_array() {
                for line in arr {
                    if let Some(s) = line.as_str() {
                        out.push_str(s);
                    }
                }
                if !out.ends_with('\n') && !out.is_empty() {
                    out.push('\n');
                }
            } else if let Some(s) = src.as_str() {
                out.push_str(s);
                if !out.ends_with('\n') && !out.is_empty() {
                    out.push('\n');
                }
            }
        }
    }
    Some(out)
}

impl StudentLiveStatus {
    fn compute_s_auth(&self) -> f64 {
        // Utilise external_injected_chars (dédupliqué sur fenêtre 30s par basename entre PASTE et BURST)
        let external = self.external_injected_chars.max(self.pasted_chars.max(self.burst_chars));
        let total = self.typed_chars + external;
        if total == 0 {
            -1.0
        } else {
            let score = (self.typed_chars as f64) / (total as f64) * 100.0;
            (score.clamp(0.0, 100.0) * 10.0).round() / 10.0
        }
    }

    fn compute_i_nav(&self) -> f64 {
        let active_sec = if self.is_connected {
            self.session_start.map(|t| t.elapsed().as_secs()).unwrap_or(0)
        } else {
            0
        };
        let elapsed_sec = self.total_session_sec + active_sec;
        if elapsed_sec == 0 {
            0.0
        } else {
            let ratio = (self.total_away_sec as f64) / (elapsed_sec as f64) * 100.0;
            (ratio.clamp(0.0, 100.0) * 10.0).round() / 10.0
        }
    }

    fn compute_profile_trend(&self) -> String {
        let s_auth = self.compute_s_auth();
        let i_nav = self.compute_i_nav();
        let total_activity_chars = self.typed_chars + self.pasted_chars + self.burst_chars;

        // L'inactivité courante n'a de sens que si l'étudiant est actuellement connecté en séance
        let idle_sec = if self.is_connected {
            self.last_activity
                .or(self.session_start)
                .map(|t| t.elapsed().as_secs())
                .unwrap_or(0)
        } else {
            0
        };
        let active_sec = if self.is_connected {
            self.session_start.map(|t| t.elapsed().as_secs()).unwrap_or(0)
        } else {
            0
        };

        // Phase d'observation initiale si aucun signal fort et trop peu d'activité
        if self.dropped_files < 2
            && self.burst_lines < 20
            && self.thrash_episodes == 0
            && idle_sec < 720
            && total_activity_chars < 60
            && self.total_runs < 2
        {
            return "OBSERVING".to_string();
        }

        let auth_val = if s_auth >= 0.0 {
            (s_auth / 5.0).clamp(0.0, 20.0)
        } else {
            10.0
        };
        let focus_val = ((100.0 - i_nav) / 5.0).clamp(0.0, 20.0);
        let err_rate = if self.total_runs > 0 {
            (self.total_errors as f64) / (self.total_runs as f64)
        } else {
            0.0
        };
        // I_thrash monotone dans [0, 1] : proportion d'erreurs survenues en rafale (rang >= 3)
        let i_thrash = (self.thrash_error_runs as f64) / (self.total_runs.max(1) as f64);
        let help_pen = (self.help_requests as f64 * 2.0).min(6.0);

        // Scores d'affinité temps réel (/20) pour les 6 profils archétypaux
        // 1. BOSS : haute authenticité, exécutions réussies fluides, faible taux d'erreur
        let mut boss_score = if self.typed_chars < 60 {
            0.5
        } else if self.total_ok_runs >= 2 {
            (auth_val * 0.60) + (focus_val * 0.25) + 3.0
                - (err_rate * 16.0)
                - help_pen
                - (i_thrash * 25.0)
                - if self.burst_lines >= 20 { 8.0 } else { 0.0 }
        } else if self.total_ok_runs == 1 && s_auth >= 80.0 && total_activity_chars >= 100 {
            (auth_val * 0.50) + (focus_val * 0.20) + 1.0 - (err_rate * 10.0) - help_pen
        } else {
            0.5
        };
        if (s_auth >= 0.0 && s_auth < 50.0) || self.dropped_files >= 2 {
            boss_score = boss_score.min(9.5);
        }
        boss_score = boss_score.clamp(0.5, 20.0);

        // 2. ARTISAN : saisie clavier réelle, corrections (backspaces), itérations saines
        let bs_bonus = if self.backspaces >= 15 {
            3.0
        } else if self.backspaces >= 5 {
            1.5
        } else {
            0.0
        };
        let runs_bonus = if self.total_runs >= 3 { 2.5 } else if self.total_runs >= 1 { 1.0 } else { 0.0 };
        let mut artisan_score = if self.typed_chars >= 60 {
            (auth_val * 0.65) + bs_bonus + runs_bonus
                - if self.burst_lines >= 20 { 8.0 } else { 0.0 }
                - if idle_sec >= 720 { 6.0 } else { 0.0 }
        } else {
            0.5
        };
        if s_auth >= 0.0 && s_auth < 50.0 {
            artisan_score = artisan_score.min(9.5);
        }
        artisan_score = artisan_score.clamp(0.5, 20.0);

        // 3. CYBORG : faible authenticité, bursts d'insertion, forte navette hors IDE
        let mut cyborg_score = if s_auth >= 0.0 {
            ((20.0 - auth_val) * 0.65) + ((20.0 - focus_val) * 0.25)
                + if self.burst_lines >= 20 { 9.0 } else { 0.0 }
        } else if self.burst_lines >= 20 {
            16.0
        } else {
            0.5
        };
        if s_auth >= 75.0 && self.burst_lines < 20 {
            cyborg_score = cyborg_score.min(5.0);
        }
        cyborg_score = cyborg_score.clamp(0.5, 20.0);

        // 4. CASSE_COU : épisodes d'acharnement amortis par total_runs ou fort taux d'erreur
        let mut casse_score = (i_thrash * 45.0)
            + if self.total_runs >= 5 && err_rate >= 0.60 {
                err_rate * 16.0
            } else if self.total_errors >= 3 && err_rate >= 0.35 {
                err_rate * 14.0
            } else {
                0.5
            };
        casse_score = casse_score.clamp(0.5, 20.0);

        // 5. DECROCHEUR : inactivité prolongée EN SÉANCE ACTIVE (étudiant connecté uniquement)
        let mut decroch_score: f64 = if !self.is_connected {
            0.5
        } else if idle_sec >= 900 {
            17.0
        } else if idle_sec >= 720 {
            14.0
        } else if active_sec >= 900 && total_activity_chars < 40 && self.total_runs == 0 {
            15.0
        } else {
            0.5
        };
        decroch_score = decroch_score.clamp(0.5, 20.0);

        // 6. PIRATE : glisser-déposer de fichiers de code externes (1 drop isolé = signal modéré, >= 2 = signal fort)
        let pirate_score = if self.dropped_files >= 2 {
            (14.0 + ((self.dropped_files - 2) as f64 * 3.0)).clamp(0.5, 20.0)
        } else if self.dropped_files == 1 && (total_activity_chars >= 60 || self.total_runs >= 2) {
            10.0
        } else {
            0.5
        };

        let mut scores = [
            ("BOSS", boss_score),
            ("ARTISAN", artisan_score),
            ("CYBORG", cyborg_score),
            ("CASSE_COU", casse_score),
            ("DECROCHEUR", decroch_score),
            ("PIRATE", pirate_score),
        ];
        scores.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let (p1_id, p1_val) = scores[0];
        let (p2_id, p2_val) = scores[1];

        if p1_val < 9.0 {
            return "OBSERVING".to_string();
        }

        // Détection formelle d'un Double-Profil temps réel (mêmes règles que Python) :
        // 1. Axes non opposés à 180° (BOSS<->DECROCHEUR, ARTISAN<->CYBORG, CASSE_COU<->PIRATE)
        // 2. Les deux affinités >= 11.0 / 20
        // 3. Écart resserré <= 3.5 pts
        let are_opposite = matches!(
            (p1_id, p2_id),
            ("BOSS", "DECROCHEUR")
                | ("DECROCHEUR", "BOSS")
                | ("ARTISAN", "CYBORG")
                | ("CYBORG", "ARTISAN")
                | ("CASSE_COU", "PIRATE")
                | ("PIRATE", "CASSE_COU")
        );

        if !are_opposite && p1_val >= 11.0 && p2_val >= 11.0 && (p1_val - p2_val).abs() <= 3.5 {
            format!("{}+{}", p1_id, p2_id)
        } else {
            p1_id.to_string()
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
            profile_trend: self.compute_profile_trend(),
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
            let mut open_session_start_ts: Option<i64> = None;
            let mut last_event_ts: Option<i64> = None;
            let mut replay_ext_pastes: HashMap<String, Vec<(Option<i64>, usize)>> = HashMap::new();

            for line in content.lines() {
                if line.trim().is_empty() {
                    continue;
                }
                if let Ok(val) = serde_json::from_str::<serde_json::Value>(line) {
                    let ev_ts = val
                        .get("timestamp")
                        .and_then(|t| t.as_str())
                        .and_then(|s| chrono::DateTime::parse_from_rfc3339(s).ok())
                        .map(|dt| dt.timestamp());

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
                                st.external_injected_chars += chars;
                                let fname = val.get("file").and_then(|f| f.as_str()).unwrap_or("unknown");
                                let bname = Path::new(fname).file_name().and_then(|n| n.to_str()).unwrap_or(fname).to_string();
                                replay_ext_pastes.entry(bname).or_default().push((ev_ts, chars));
                            }
                        }
                        Some("EDIT") => {
                            let ins = val.get("lines_added").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                            let rem = val.get("lines_removed").and_then(|v| v.as_u64()).unwrap_or(0) as usize;
                            if ins + rem >= 3 {
                                st.consecutive_errors = 0;
                            }
                            if let Some(al) = val.get("alert").and_then(|a| a.as_str()) {
                                if al.starts_with("BURST") {
                                    st.burst_lines += ins;
                                    let burst_c = ins * 30;
                                    st.burst_chars += burst_c;
                                    let fname = val.get("file").and_then(|f| f.as_str()).unwrap_or("unknown");
                                    let bname = Path::new(fname).file_name().and_then(|n| n.to_str()).unwrap_or(fname).to_string();
                                    let mut matched = 0usize;
                                    if let Some(plist) = replay_ext_pastes.get_mut(&bname) {
                                        if let Some(pos) = plist.iter().rposition(|(p_ts, _)| match (ev_ts, *p_ts) {
                                            (Some(t1), Some(t0)) => t1 >= t0 && (t1 - t0) as u64 <= INTERNAL_PASTE_WINDOW_SECS,
                                            (None, None) => true,
                                            _ => false,
                                        }) {
                                            matched = plist.remove(pos).1;
                                        }
                                    }
                                    st.external_injected_chars += burst_c.saturating_sub(matched);
                                }
                            }
                        }
                        Some("FOCUS") => {
                            if val.get("status").and_then(|s| s.as_str()) == Some("GAINED") {
                                let dur = val.get("duration_sec").and_then(|v| v.as_u64()).unwrap_or(0);
                                st.total_away_sec += dur.min(MAX_FOCUS_AWAY_SEC);
                            } else if val.get("status").and_then(|s| s.as_str()) == Some("LOST") {
                                st.focus_lost_count += 1;
                            }
                        }
                        Some("FILE_DROP") => {
                            let fname = val.get("file").and_then(|f| f.as_str()).unwrap_or("");
                            if is_code_file_extension(fname) {
                                st.dropped_files += 1;
                            }
                        }
                        Some("COMPILE") | Some("EXECUTE") => {
                            let st_str = val.get("status").and_then(|s| s.as_str()).unwrap_or("");
                            if st_str == "ERROR" {
                                st.total_runs += 1;
                                st.total_errors += 1;
                                st.consecutive_errors += 1;
                                if st.consecutive_errors == 3 {
                                    st.thrash_episodes += 1;
                                }
                                if st.consecutive_errors >= 3 {
                                    st.thrash_error_runs += 1;
                                }
                            } else if st_str == "OK" || st_str == "DONE" {
                                st.total_runs += 1;
                                st.total_ok_runs += 1;
                                st.consecutive_errors = 0;
                            } else if st_str == "HALTED" {
                                st.total_runs += 1;
                                st.consecutive_errors = 0;
                            }
                        }
                        Some("HELP") => {
                            let h_st = val.get("status").and_then(|s| s.as_str()).unwrap_or("");
                            if h_st == "REQUESTED" {
                                st.help_requests += 1;
                            } else if h_st == "CANCELLED" {
                                st.help_requests = st.help_requests.saturating_sub(1);
                            }
                        }
                        Some("SESSION") => {
                            let s_status = val.get("status").and_then(|s| s.as_str()).unwrap_or("");
                            if s_status == "DISCONNECTED" {
                                let dur = val.get("duration_sec").and_then(|v| v.as_u64()).unwrap_or_else(|| {
                                    match (open_session_start_ts, ev_ts.or(last_event_ts)) {
                                        (Some(t0), Some(t1)) if t1 > t0 => (t1 - t0) as u64,
                                        _ => 0,
                                    }
                                });
                                st.total_session_sec += dur;
                                st.is_connected = false;
                                st.session_start = None;
                                open_session_start_ts = None;
                            } else if s_status == "CONNECTED" {
                                // Clôturer une séance précédente orpheline sans DISCONNECTED
                                if let (Some(t0), Some(t_last)) = (open_session_start_ts, last_event_ts) {
                                    if t_last > t0 {
                                        st.total_session_sec += (t_last - t0) as u64;
                                    }
                                }
                                open_session_start_ts = ev_ts;
                                st.is_connected = false; // Un CONNECTED historique dans le log n'implique pas qu'il est connecté maintenant
                            }
                        }
                        _ => {}
                    }

                    if let Some(ts) = ev_ts {
                        last_event_ts = Some(ts);
                    }
                }
            }

            // Séance orpheline en fin de journal (ex: crash serveur sans DISCONNECTED)
            if let (Some(t0), Some(t_last)) = (open_session_start_ts, last_event_ts) {
                if t_last > t0 {
                    st.total_session_sec += (t_last - t0) as u64;
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

pub fn push_live_metrics(session: &str, login: &str) {
    if let Ok(handle) = tokio::runtime::Handle::try_current() {
        let metrics = get_student_live_metrics(session, login);
        let sess = session.to_string();
        let log = login.to_string();
        handle.spawn(async move {
            crate::client::notify_tutors_user_metrics(
                &sess,
                &log,
                metrics.s_auth,
                metrics.i_nav,
                &metrics.profile_trend,
            )
            .await;
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
    if status == "REQUESTED" || status == "CANCELLED" {
        ensure_student_live_loaded(session, login);
        let key = format!("{}/{}", session, login);
        {
            let mut live_map = STUDENT_LIVE.lock().unwrap();
            let st = live_map.entry(key).or_default();
            if status == "REQUESTED" {
                st.help_requests += 1;
            } else {
                st.help_requests = st.help_requests.saturating_sub(1);
            }
            st.last_activity = Some(Instant::now());
        }
        push_live_metrics(session, login);
    }
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
    ensure_student_live_loaded(session, login);
    {
        let key = format!("{}/{}", session, login);
        let mut live_map = STUDENT_LIVE.lock().unwrap();
        let st = live_map.entry(key).or_default();
        st.last_activity = Some(Instant::now());
    }
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
    {
        let key = format!("{}/{}", session, login);
        let mut live_map = STUDENT_LIVE.lock().unwrap();
        let st = live_map.entry(key).or_default();
        if status == "CONNECTED" {
            if st.is_connected {
                if let Some(prev_start) = st.session_start {
                    st.total_session_sec += prev_start.elapsed().as_secs();
                }
            }
            st.is_connected = true;
            st.session_start = Some(Instant::now());
            st.last_activity = Some(Instant::now());
        } else if status == "DISCONNECTED" {
            let dur = duration_sec.unwrap_or_else(|| st.session_start.map(|t| t.elapsed().as_secs()).unwrap_or(0));
            st.total_session_sec += dur;
            st.is_connected = false;
            st.session_start = None;
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

    let raw_new_text = match std::str::from_utf8(new_content) {
        Ok(s) => s,
        Err(_) => return,
    };

    let raw_old_text = match old_content {
        Some(bytes) => match std::str::from_utf8(bytes) {
            Ok(s) => s,
            Err(_) => "",
        },
        None => "",
    };

    let is_ipynb = relative_file.to_lowercase().ends_with(".ipynb");
    let extracted_new;
    let extracted_old;
    let (old_text, new_text) = if is_ipynb {
        extracted_new = extract_ipynb_code_text(raw_new_text);
        extracted_old = if raw_old_text.is_empty() {
            Some(String::new())
        } else {
            extract_ipynb_code_text(raw_old_text)
        };
        match (&extracted_old, &extracted_new) {
            (Some(o), Some(n)) => (o.as_str(), n.as_str()),
            _ => (raw_old_text, raw_new_text),
        }
    } else {
        (raw_old_text, raw_new_text)
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

    ensure_student_live_loaded(session, login);
    let key = format!("{}/{}", session, login);

    // Soustraire les lignes récemment collées en interne (internal: true) dans la fenêtre de 30s
    let internal_credit = {
        let mut live_map = STUDENT_LIVE.lock().unwrap();
        let st = live_map.entry(key.clone()).or_default();
        st.last_activity = Some(Instant::now());
        // Purge des entrées expirées
        st.recent_internal_paste
            .retain(|_, (t, _)| t.elapsed().as_secs() <= INTERNAL_PASTE_WINDOW_SECS);
        let rel_basename = Path::new(relative_file)
            .file_name()
            .and_then(|n| n.to_str())
            .unwrap_or(relative_file);
        let matched_key = if st.recent_internal_paste.contains_key(relative_file) {
            Some(relative_file.to_string())
        } else {
            st.recent_internal_paste
                .keys()
                .find(|k| {
                    Path::new(k.as_str())
                        .file_name()
                        .and_then(|n| n.to_str())
                        == Some(rel_basename)
                })
                .cloned()
        };
        if let Some(mk) = matched_key {
            st.recent_internal_paste.remove(&mk).map(|(_, l)| l).unwrap_or(0)
        } else {
            0
        }
    };

    let effective_added = lines_added.saturating_sub(internal_credit);
    let effective_chunk = chunk_max.saturating_sub(internal_credit);

    let mut alert = None;
    if is_new_file {
        if effective_added >= 25 {
            alert = Some("BURST_NEW_FILE".to_string());
        }
    } else if effective_chunk >= 20 {
        // Ne déclencher BURST_INSERT que si la sauvegarde précédente est récente (<= 15s) :
        // dt_sec == None correspond à la première sauvegarde depuis le démarrage du serveur (évite un faux positif).
        if let Some(dt) = dt_sec {
            if dt <= 15 {
                alert = Some("BURST_INSERT".to_string());
            }
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
            {
                let mut live_map = STUDENT_LIVE.lock().unwrap();
                let st = live_map.entry(key.clone()).or_default();
                let burst_c = lines_added * 30;
                st.burst_lines += lines_added;
                st.burst_chars += burst_c;
                // Déduplication par basename sur fenêtre de 30s avec PASTE externe
                st.recent_external_paste
                    .retain(|_, (t, _)| t.elapsed().as_secs() <= INTERNAL_PASTE_WINDOW_SECS);
                let rel_bname = Path::new(relative_file)
                    .file_name()
                    .and_then(|n| n.to_str())
                    .unwrap_or(relative_file);
                let matched_paste = st
                    .recent_external_paste
                    .remove(rel_bname)
                    .map(|(_, c)| c)
                    .unwrap_or(0);
                st.external_injected_chars += burst_c.saturating_sub(matched_paste);
            }
            push_live_metrics(session, login);
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
                        st.total_runs += 1;
                        st.total_errors += 1;
                        st.consecutive_errors += 1;
                        if st.consecutive_errors == 3 {
                            st.thrash_episodes += 1;
                        }
                        if st.consecutive_errors >= 3 {
                            st.thrash_error_runs += 1;
                        }
                        st.last_error_time = Some(Instant::now());
                        st.last_activity = Some(Instant::now());
                        err_cnt = st.consecutive_errors;
                    }
                    if err_cnt >= 3 {
                        let msg = format!("{} échecs de compilation consécutifs ({})", err_cnt, file);
                        trigger_live_alert(session, login, "THRASH", &msg, "WARN");
                    }
                    push_live_metrics(session, login);
                } else {
                    log::info!(
                        "[ACTIVITY COMPILE OK] session={} login={} file={}",
                        session, login, file
                    );
                    let mut had_thrash = false;
                    {
                        let mut live_map = STUDENT_LIVE.lock().unwrap();
                        let st = live_map.entry(key.clone()).or_default();
                        st.total_runs += 1;
                        st.total_ok_runs += 1;
                        st.last_activity = Some(Instant::now());
                        if st.consecutive_errors >= 3 {
                            had_thrash = true;
                        }
                        st.consecutive_errors = 0;
                    }
                    if had_thrash {
                        clear_live_alert(session, login, "THRASH");
                    }
                    push_live_metrics(session, login);
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
                        st.total_runs += 1;
                        st.total_errors += 1;
                        st.consecutive_errors += 1;
                        if st.consecutive_errors == 3 {
                            st.thrash_episodes += 1;
                        }
                        if st.consecutive_errors >= 3 {
                            st.thrash_error_runs += 1;
                        }
                        st.last_error_time = Some(Instant::now());
                        st.last_activity = Some(Instant::now());
                        err_cnt = st.consecutive_errors;
                    }
                    if err_cnt >= 3 {
                        let msg = format!("{} plantages consécutifs ({})", err_cnt, file);
                        trigger_live_alert(session, login, "THRASH", &msg, "WARN");
                    }
                    push_live_metrics(session, login);
                } else {
                    log::info!(
                        "[ACTIVITY EXECUTE {}] session={} login={} file={} code={:?}",
                        status, session, login, file, exit_code
                    );
                    if status == "DONE" || status == "HALTED" {
                        let mut had_thrash = false;
                        {
                            let mut live_map = STUDENT_LIVE.lock().unwrap();
                            let st = live_map.entry(key.clone()).or_default();
                            st.total_runs += 1;
                            if status == "DONE" {
                                st.total_ok_runs += 1;
                            }
                            st.last_activity = Some(Instant::now());
                            if st.consecutive_errors >= 3 {
                                had_thrash = true;
                            }
                            st.consecutive_errors = 0;
                        }
                        if had_thrash {
                            clear_live_alert(session, login, "THRASH");
                        }
                        push_live_metrics(session, login);
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
                            st.total_away_sec += dur.min(MAX_FOCUS_AWAY_SEC);
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
                    push_live_metrics(session, login);
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
                        st.last_activity = Some(Instant::now());
                        let now_inst = Instant::now();
                        let entry = st.recent_internal_paste.entry(file.clone()).or_insert((now_inst, 0));
                        if entry.0.elapsed().as_secs() <= INTERNAL_PASTE_WINDOW_SECS {
                            entry.0 = now_inst;
                            entry.1 += lines;
                        } else {
                            *entry = (now_inst, lines);
                        }
                    } else {
                        st.pasted_chars += chars;
                        st.pasted_lines += lines;
                        st.external_injected_chars += chars;
                        st.last_activity = Some(Instant::now());
                        let now_inst = Instant::now();
                        let bname = Path::new(&file)
                            .file_name()
                            .and_then(|n| n.to_str())
                            .unwrap_or(&file)
                            .to_string();
                        let entry = st.recent_external_paste.entry(bname).or_insert((now_inst, 0));
                        if entry.0.elapsed().as_secs() <= INTERNAL_PASTE_WINDOW_SECS {
                            entry.0 = now_inst;
                            entry.1 += chars;
                        } else {
                            *entry = (now_inst, chars);
                        }
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
                    push_live_metrics(session, login);
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
                    st.last_activity = Some(Instant::now());
                }
                push_live_metrics(session, login);

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
                let is_code_drop = is_code_file_extension(&file);

                {
                    let mut live_map = STUDENT_LIVE.lock().unwrap();
                    let st = live_map.entry(key.clone()).or_default();
                    if is_code_drop {
                        st.dropped_files += 1;
                    }
                    st.last_activity = Some(Instant::now());
                }

                if is_code_drop {
                    log::warn!(
                        "[ACTIVITY FILE_DROP] session={} login={} file={} size={}",
                        session, login, file, size_bytes
                    );

                    let msg = format!("Fichier {} déposé ({} octets)", file, size_bytes);
                    trigger_live_alert(session, login, "DROP", &msg, "WARN");
                    push_live_metrics(session, login);
                } else {
                    log::info!(
                        "[ACTIVITY FILE_DROP DATA] session={} login={} file={} size={} (ignoré pour profil Pirate)",
                        session, login, file, size_bytes
                    );
                }

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
        // Premier enregistrement initial pour établir LAST_EDITS
        record_file_activity(session, login, file, None, old_content);
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
        assert_eq!(metrics2.profile_trend, "ARTISAN");

        // 1 seul FILE_DROP isolé reste ARTISAN (évite les faux positifs sur dépôt d'un fichier de données),
        // 2 FILE_DROP sur un étudiant Artisan -> bascule en double-profil PIRATE+ARTISAN (ou ARTISAN+PIRATE)
        record_client_log(session, login, r#"{"type":"FILE_DROP","file":"copie.py","size_bytes":2048}"#);
        assert_eq!(get_student_live_metrics(session, login).profile_trend, "ARTISAN");
        record_client_log(session, login, r#"{"type":"FILE_DROP","file":"copie2.py","size_bytes":2048}"#);
        let metrics3 = get_student_live_metrics(session, login);
        assert!(
            metrics3.profile_trend == "PIRATE+ARTISAN" || metrics3.profile_trend == "ARTISAN+PIRATE",
            "Expected PIRATE+ARTISAN double profile, got {}",
            metrics3.profile_trend
        );

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_live_profile_trends_boss_and_casse_cou() {
        let session = "test_sess_trends";
        let boss_login = "student_boss";
        let cc_login = "student_cc";

        record_session_event(session, boss_login, "CONNECTED", Some("127.0.0.1"), None);
        assert_eq!(get_student_live_metrics(session, boss_login).profile_trend, "OBSERVING");
        record_client_log(session, boss_login, r#"{"type":"TYPING","file":"a.py","typed_chars":300,"backspaces":10}"#);
        record_client_log(session, boss_login, r#"{"type":"EXECUTE","file":"a.py","status":"DONE","exit_code":0}"#);
        record_client_log(session, boss_login, r#"{"type":"EXECUTE","file":"a.py","status":"DONE","exit_code":0}"#);
        let boss_trend = get_student_live_metrics(session, boss_login).profile_trend;
        assert!(
            boss_trend == "BOSS+ARTISAN" || boss_trend == "ARTISAN+BOSS" || boss_trend == "BOSS",
            "Expected BOSS or BOSS+ARTISAN, got {}",
            boss_trend
        );

        record_session_event(session, cc_login, "CONNECTED", Some("127.0.0.1"), None);
        record_client_log(session, cc_login, r#"{"type":"TYPING","file":"b.py","typed_chars":200,"backspaces":20}"#);
        record_client_log(session, cc_login, r#"{"type":"COMPILE","file":"b.py","status":"ERROR","error_msg":"err1"}"#);
        record_client_log(session, cc_login, r#"{"type":"COMPILE","file":"b.py","status":"ERROR","error_msg":"err2"}"#);
        record_client_log(session, cc_login, r#"{"type":"COMPILE","file":"b.py","status":"ERROR","error_msg":"err3"}"#);
        let cc_trend = get_student_live_metrics(session, cc_login).profile_trend;
        assert!(
            cc_trend == "CASSE_COU+ARTISAN" || cc_trend == "ARTISAN+CASSE_COU" || cc_trend == "CASSE_COU",
            "Expected CASSE_COU or ARTISAN+CASSE_COU, got {}",
            cc_trend
        );

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_internal_paste_followed_by_save_no_burst() {
        let session = "test_sess_int_paste_save";
        let login = "student_int_paste";
        let file = "exo2.c";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        record_client_log(session, login, r#"{"type":"TYPING","file":"exo1.c","typed_chars":800,"backspaces":40}"#);
        record_client_log(session, login, r#"{"type":"PASTE","file":"exo1.c","chars":200,"lines":8}"#);
        assert_eq!(get_student_live_metrics(session, login).s_auth, 80.0);

        // Collage interne de 30 lignes puis sauvegarde immédiate du nouveau fichier
        record_client_log(session, login, r#"{"type":"PASTE","file":"exo2.c","chars":900,"lines":30,"internal":true}"#);
        let mut content = String::new();
        for i in 0..30 {
            content.push_str(&format!("int x{} = {};\n", i, i));
        }
        record_file_activity(session, login, file, None, content.as_bytes());

        let metrics = get_student_live_metrics(session, login);
        assert_eq!(metrics.burst_lines, 0, "Internal paste must not trigger BURST");
        assert_eq!(metrics.s_auth, 80.0, "s_auth must remain unchanged at 80.0%");
        assert!(!metrics.profile_trend.contains("CYBORG"), "Must not switch to CYBORG, got {}", metrics.profile_trend);

        let log_file = Path::new(PROG_DIR).join(session).join(login).join(".activity.jsonl");
        let log_txt = fs::read_to_string(&log_file).unwrap();
        assert!(!log_txt.contains("BURST_NEW_FILE"), "No BURST_NEW_FILE alert expected");
        assert!(!log_txt.contains("BURST_INSERT"), "No BURST_INSERT alert expected");

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_ipynb_output_growth_no_burst() {
        let session = "test_sess_ipynb_out";
        let login = "student_nb";
        let file = "tp1.ipynb";

        let old_nb = serde_json::json!({
            "cells": [{
                "cell_type": "code",
                "source": ["def f(x):\n", "    return x + 1\n"],
                "outputs": []
            }],
            "metadata": {},
            "nbformat": 4,
            "nbformat_minor": 5
        }).to_string();

        let big_output: Vec<String> = (0..50).map(|i| format!("ligne sortie {}\n", i)).collect();
        let new_nb = serde_json::json!({
            "cells": [{
                "cell_type": "code",
                "source": ["def f(x):\n", "    return x + 1\n"],
                "outputs": [{"output_type": "stream", "name": "stdout", "text": big_output}]
            }],
            "metadata": {},
            "nbformat": 4,
            "nbformat_minor": 5
        }).to_string();

        record_file_activity(session, login, file, None, old_nb.as_bytes());
        record_file_activity(session, login, file, Some(old_nb.as_bytes()), new_nb.as_bytes());

        let metrics = get_student_live_metrics(session, login);
        assert_eq!(metrics.burst_lines, 0, "Growing ipynb outputs must not trigger BURST");

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_replay_crash_without_disconnected_inav_bounded() {
        let session = "test_sess_replay_crash";
        let login = "student_crash";
        let student_dir = Path::new(PROG_DIR).join(session).join(login);
        fs::create_dir_all(&student_dir).unwrap();
        let log_file = student_dir.join(".activity.jsonl");

        // Séance de 1200s sans DISCONNECTED avec 300s d'absence -> i_nav = 25.0%
        let lines = [
            r#"{"type":"SESSION","timestamp":"2026-11-10T09:00:00Z","session":"test_sess_replay_crash","login":"student_crash","status":"CONNECTED"}"#,
            r#"{"type":"TYPING","timestamp":"2026-11-10T09:10:00Z","session":"test_sess_replay_crash","login":"student_crash","file":"a.py","typed_chars":200,"backspaces":10}"#,
            r#"{"type":"FOCUS","timestamp":"2026-11-10T09:20:00Z","session":"test_sess_replay_crash","login":"student_crash","status":"GAINED","duration_sec":300}"#,
        ].join("\n") + "\n";
        fs::write(&log_file, lines).unwrap();

        let metrics = get_student_live_metrics(session, login);
        assert_eq!(metrics.i_nav, 25.0, "Expected bounded i_nav == 25.0% on orphan session replay");

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_execute_halted_clears_thrash_and_counts_run() {
        let session = "test_sess_halted";
        let login = "student_halted";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        record_client_log(session, login, r#"{"type":"COMPILE","file":"loop.c","status":"ERROR","error_msg":"err1"}"#);
        record_client_log(session, login, r#"{"type":"COMPILE","file":"loop.c","status":"ERROR","error_msg":"err2"}"#);
        record_client_log(session, login, r#"{"type":"COMPILE","file":"loop.c","status":"ERROR","error_msg":"err3"}"#);
        assert_eq!(get_student_live_metrics(session, login).consecutive_errors, 3);

        record_client_log(session, login, r#"{"type":"EXECUTE","file":"loop.c","status":"HALTED"}"#);
        assert_eq!(get_student_live_metrics(session, login).consecutive_errors, 0);

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_data_file_drop_not_pirate_and_thrash_decay() {
        let session = "test_sess_data_decay";
        let login = "student_bright";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        // 2 fichiers de données (.csv) ne doivent pas déclencher PIRATE
        record_client_log(session, login, r#"{"type":"FILE_DROP","file":"data.csv","size_bytes":512}"#);
        record_client_log(session, login, r#"{"type":"FILE_DROP","file":"train.csv","size_bytes":1024}"#);
        let m_drop = get_student_live_metrics(session, login);
        assert_eq!(m_drop.dropped_files, 0);
        assert!(!m_drop.profile_trend.contains("PIRATE"), "Data CSV drops must not trigger PIRATE, got {}", m_drop.profile_trend);

        // 1 épisode thrash initial puis 15 runs DONE + forte saisie -> retour BOSS ou ARTISAN
        record_client_log(session, login, r#"{"type":"TYPING","file":"tp.c","typed_chars":2500,"backspaces":40}"#);
        for _ in 0..3 {
            record_client_log(session, login, r#"{"type":"COMPILE","file":"tp.c","status":"ERROR","error_msg":"syntax"}"#);
        }
        for _ in 0..15 {
            record_client_log(session, login, r#"{"type":"EXECUTE","file":"tp.c","status":"DONE","exit_code":0}"#);
        }
        let m_after = get_student_live_metrics(session, login);
        assert!(
            !m_after.profile_trend.contains("CASSE_COU"),
            "Thrash should decay after 15 clean runs, got {}",
            m_after.profile_trend
        );
        assert!(
            m_after.profile_trend.contains("BOSS") || m_after.profile_trend.contains("ARTISAN"),
            "Expected BOSS/ARTISAN after recovery, got {}",
            m_after.profile_trend
        );

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_ithrash_monotone_increasing_errors() {
        let mut st_short = StudentLiveStatus::default();
        st_short.typed_chars = 200;
        for _ in 0..3 {
            st_short.total_runs += 1;
            st_short.total_errors += 1;
            st_short.consecutive_errors += 1;
            if st_short.consecutive_errors == 3 {
                st_short.thrash_episodes += 1;
            }
            if st_short.consecutive_errors >= 3 {
                st_short.thrash_error_runs += 1;
            }
        }
        let mut st_long = StudentLiveStatus::default();
        st_long.typed_chars = 200;
        for _ in 0..20 {
            st_long.total_runs += 1;
            st_long.total_errors += 1;
            st_long.consecutive_errors += 1;
            if st_long.consecutive_errors == 3 {
                st_long.thrash_episodes += 1;
            }
            if st_long.consecutive_errors >= 3 {
                st_long.thrash_error_runs += 1;
            }
        }
        let i_thrash_short = st_short.thrash_error_runs as f64 / st_short.total_runs as f64;
        let i_thrash_long = st_long.thrash_error_runs as f64 / st_long.total_runs as f64;
        assert!(
            i_thrash_long > i_thrash_short,
            "I_thrash must be strictly higher for 20 consecutive errors ({:.3}) than for 3 ({:.3})",
            i_thrash_long,
            i_thrash_short
        );
        assert!(st_long.to_live_metrics().profile_trend.contains("CASSE_COU"));
    }

    #[test]
    fn test_help_cancelled_decrements_help_requests() {
        let session = "test_sess_help_cancel";
        let login = "student_help";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        record_help_event(session, login, "REQUESTED", None);
        record_help_event(session, login, "CANCELLED", None);
        let key = format!("{}/{}", session, login);
        {
            let live_map = STUDENT_LIVE.lock().unwrap();
            assert_eq!(live_map.get(&key).unwrap().help_requests, 0);
        }

        // Vérifie aussi le replay depuis .activity.jsonl
        {
            let mut live_map = STUDENT_LIVE.lock().unwrap();
            live_map.remove(&key);
        }
        ensure_student_live_loaded(session, login);
        {
            let live_map = STUDENT_LIVE.lock().unwrap();
            assert_eq!(live_map.get(&key).unwrap().help_requests, 0);
        }

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }

    #[test]
    fn test_external_paste_and_burst_subdir_dedup() {
        let session = "test_sess_ext_paste_dedup";
        let login = "student_dedup";

        record_session_event(session, login, "CONNECTED", Some("127.0.0.1"), None);
        record_client_log(session, login, r#"{"type":"TYPING","file":"exo1.c","typed_chars":900,"backspaces":10}"#);
        // PASTE envoie le basename "exo1.c", puis record_file_activity envoie "TP1/exo1.c" (30 lignes = 900 chars)
        record_client_log(session, login, r#"{"type":"PASTE","file":"exo1.c","chars":900,"lines":30}"#);
        let mut content = String::new();
        for i in 0..30 {
            content.push_str(&format!("int v{} = {};\n", i, i));
        }
        record_file_activity(session, login, "TP1/exo1.c", None, content.as_bytes());

        let metrics = get_student_live_metrics(session, login);
        assert_eq!(
            metrics.s_auth, 50.0,
            "PASTE + BURST on same basename within 30s must not double-count (expected 50.0%, got {}%)",
            metrics.s_auth
        );

        let _ = fs::remove_dir_all(Path::new(PROG_DIR).join(session));
    }
}

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
activity_analyzer.py - Analyseur de télémétrie et d'activité étudiante CodeLab.
Traite le journal append-only (.activity.jsonl) pour dériver les indicateurs formels :
- Score d'Authenticité (S_auth)
- Taux de Navette Externe (I_nav)
- Indice d'Acharnement / Errance (I_thrash)
- Time-to-Fix moyen (TTF)
- Répartition du budget temps en 4 quadrants (Edit, Read, Test, Away)
- Détection d'alertes médico-légales (BURST_INSERT, BURST_NEW_FILE, FILE_DROP)
"""

import os
import sys
import json
import math
from pathlib import Path
from datetime import datetime

def compute_sliding_windows(parsed_events: list, window_sec: int = 900) -> list:
    """
    Découpe les événements horodatés en tranches temporelles (fenêtres de window_sec, par défaut 15 min).
    Pour chaque fenêtre, calcule l'authenticité locale, les tests et le diagnostic de phase.
    """
    valid_dts = [pe[0] for pe in parsed_events if pe[0]]
    if not valid_dts:
        return []
        
    t_start = valid_dts[0]
    t_end = valid_dts[-1]
    total_sec = max(1.0, (t_end - t_start).total_seconds())
    
    # Nombre de tranches de window_sec
    num_windows = max(1, math.ceil(total_sec / float(window_sec)))
    windows = []
    
    from datetime import timedelta
    for w_idx in range(num_windows):
        w_t0 = t_start + timedelta(seconds=w_idx * window_sec)
        w_t1 = min(t_end, w_t0 + timedelta(seconds=window_sec))
        dur_sec = max(1.0, (w_t1 - w_t0).total_seconds())
        
        # Filtre des événements dans la tranche
        win_evs = [pe for pe in parsed_events if pe[0] and (w_t0 <= pe[0] <= w_t1)]
        
        keys = 0
        backspaces = 0
        paste_chars = 0
        paste_lines = 0
        burst_lines = 0
        burst_count = 0
        code_modifs = 0
        away_sec = 0.0
        compiles = 0
        execs = 0
        errors = 0
        drops = 0
        
        for dt, ev in win_evs:
            ev_type = ev.get("type", "")
            if ev_type == "TYPING":
                keys += ev.get("typed_chars", 0)
                backspaces += ev.get("backspaces", 0)
            elif ev_type == "PASTE":
                paste_chars += ev.get("chars", 0)
                paste_lines += ev.get("lines", 0)
            elif ev_type == "EDIT":
                ins = ev.get("lines_added", 0)
                rem = ev.get("lines_removed", 0)
                code_modifs += (ins + rem)
                al = ev.get("alert")
                if al in ("BURST_INSERT", "BURST_NEW_FILE"):
                    burst_lines += ins
                    burst_count += 1
            elif ev_type == "FOCUS":
                if ev.get("status") == "GAINED":
                    dur = ev.get("duration_sec")
                    if dur:
                        away_sec += float(dur)
            elif ev_type == "COMPILE":
                compiles += 1
                if ev.get("status") == "ERROR":
                    errors += 1
            elif ev_type == "EXECUTE":
                execs += 1
                if ev.get("status") == "ERROR":
                    errors += 1
            elif ev_type == "FILE_DROP":
                drops += 1
                
        denom = keys + paste_chars + (burst_lines * 30)
        s_auth_local = round((keys / float(denom)) * 100.0, 1) if denom > 0 else (0.0 if burst_lines > 0 else None)
        inav_local = min(100.0, round((away_sec / dur_sec) * 100.0, 1))
        
        # Diagnostic de la phase
        if drops > 0:
            diag = "📥 Dépôt de fichier externe"
        elif burst_count > 0 or (s_auth_local is not None and s_auth_local < 40.0):
            diag = "🚨 Décrochage IA / Injection"
        elif errors >= 3 and code_modifs < 10:
            diag = "⚠️ Acharnement sur erreurs"
        elif inav_local > 60.0:
            diag = "🌐 Consultation externe intensive"
        elif keys >= 80 and (s_auth_local is None or s_auth_local >= 80.0):
            diag = "✅ Saisie autonome active"
        elif (compiles + execs) >= 2 and errors <= 1:
            diag = "⚙️ Validation & Tests itératifs"
        elif keys < 30 and paste_chars == 0 and burst_lines == 0:
            diag = "📖 Réflexion / Lecture de l'énoncé"
        else:
            diag = "🟡 Travail mixte"
            
        windows.append({
            "index": w_idx + 1,
            "t0_str": w_t0.strftime("%H:%M"),
            "t1_str": w_t1.strftime("%H:%M"),
            "duration_min": round(dur_sec / 60.0, 1),
            "keys": keys,
            "backspaces": backspaces,
            "paste_chars": paste_chars,
            "paste_lines": paste_lines,
            "burst_lines": burst_lines,
            "burst_count": burst_count,
            "s_auth": s_auth_local,
            "away_sec": round(away_sec, 1),
            "inav": inav_local,
            "compiles": compiles,
            "execs": execs,
            "errors": errors,
            "drops": drops,
            "diagnostic": diag
        })
        
    return windows

def parse_iso_timestamp(ts_str: str) -> datetime | None:
    """Parse un timestamp ISO 8601 (avec ou sans fuseau Z/+00:00)."""
    if not ts_str:
        return None
    try:
        # Remplacer Z par +00:00 pour la compatibilité Python 3.10+
        clean_ts = ts_str.replace("Z", "+00:00")
        return datetime.fromisoformat(clean_ts)
    except Exception:
        # Repli format partiel
        for fmt in ("%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S"):
            try:
                return datetime.strptime(ts_str[:19], fmt)
            except Exception:
                pass
    return None

def analyze_student_activity(student_dir: str | Path) -> dict:
    """
    Analyse le fichier .activity.jsonl d'un dossier étudiant.
    Retourne un dictionnaire complet d'indicateurs et d'alertes pédagogiques.
    """
    st_path = Path(student_dir)
    log_file = st_path / ".activity.jsonl"
    
    # Structure de résultats par défaut (sans trace)
    res = {
        "active": False,
        "log_file": str(log_file),
        "events_count": 0,
        "first_event": None,
        "last_event": None,
        "span_duration_sec": 0.0,
        "span_duration_min": 0.0,
        "sessions_count": 0,
        
        # 1. Authenticité & Saisie
        "typing_keys": 0,
        "typing_backspaces": 0,
        "paste_chars": 0,
        "paste_lines": 0,
        "paste_count": 0,
        "burst_insert_count": 0,
        "burst_insert_lines": 0,
        "burst_new_file_count": 0,
        "burst_new_file_lines": 0,
        "s_auth": 100.0,
        "s_auth_level": "Non mesuré (pas de trace)",
        
        # 2. Navette Externe & Focus
        "focus_lost_count": 0,
        "total_away_sec": 0.0,
        "i_nav": 0.0,
        "i_nav_level": "Non mesuré (pas de trace)",
        
        # 3. Compilations & Exécutions
        "compilations": {
            "total": 0,
            "ok": 0,
            "error": 0,
            "error_rate": 0.0,
        },
        "executions": {
            "total": 0,
            "done": 0,
            "error": 0,
            "halted": 0,
            "error_rate": 0.0,
        },
        "i_thrash": 0.0,
        "thrash_episodes_count": 0,
        "ttf_avg_sec": None,
        
        # 4. Budget Temps (4 quadrants)
        "time_quadrants": {
            "q_edit_sec": 0.0,
            "q_read_sec": 0.0,
            "q_test_sec": 0.0,
            "q_away_sec": 0.0,
            "q_edit_pct": 0.0,
            "q_read_pct": 0.0,
            "q_test_pct": 0.0,
            "q_away_pct": 0.0,
        },
        
        # 5. Événements secondaires
        "file_drops": [],
        "file_ops": [],
        "help_requests": 0,
        "help_resolved": 0,
        "sliding_windows": [],
        
        # 6. Synthèses & Alertes
        "alerts": [],
        "summary_short": "Aucune trace d'activité",
        "summary_markdown": ""
    }
    
    if not log_file.is_file():
        return res
        
    events = []
    try:
        with open(log_file, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                if line:
                    try:
                        events.append(json.loads(line))
                    except Exception:
                        pass
    except Exception as e:
        res["summary_short"] = f"Erreur de lecture .activity.jsonl: {e}"
        return res

    if not events:
        return res

    res["active"] = True
    res["events_count"] = len(events)
    
    # Tri temporel des événements
    parsed_events = []
    for ev in events:
        dt = parse_iso_timestamp(ev.get("timestamp") or ev.get("ts") or "")
        parsed_events.append((dt, ev))
        
    parsed_events.sort(key=lambda x: x[0] if x[0] else datetime.min)
    
    valid_dts = [pe[0] for pe in parsed_events if pe[0]]
    if valid_dts:
        res["first_event"] = valid_dts[0].strftime("%d/%m/%Y %H:%M:%S")
        res["last_event"] = valid_dts[-1].strftime("%d/%m/%Y %H:%M:%S")
        span_s = max(0.0, (valid_dts[-1] - valid_dts[0]).total_seconds())
        res["span_duration_sec"] = span_s
        res["span_duration_min"] = round(span_s / 60.0, 1)

    # Variables de cumul
    typing_keys = 0
    typing_backspaces = 0
    paste_chars = 0
    paste_lines = 0
    paste_count = 0
    burst_insert_count = 0
    burst_insert_lines = 0
    burst_new_file_count = 0
    burst_new_file_lines = 0
    
    focus_lost_count = 0
    total_away_sec = 0.0
    last_focus_lost_dt = None
    
    compile_ok = 0
    compile_err = 0
    exec_done = 0
    exec_err = 0
    exec_halted = 0
    
    file_drops = []
    file_ops = []
    help_requests = 0
    help_resolved = 0
    sessions_count = 0
    
    # Pour le calcul de TTF (Time-To-Fix)
    # Map: file -> datetime of pending error
    pending_errors = {}
    ttf_values = []
    
    # Pour le calcul d'acharnement I_thrash
    # Séquences d'erreurs consécutives sans edit significatif
    consecutive_errors = 0
    thrash_episodes = 0
    
    # Pour les quadrants de temps
    q_edit = 0.0
    q_read = 0.0
    q_test = 0.0
    q_away = 0.0

    alerts = []

    for i, (dt, ev) in enumerate(parsed_events):
        ev_type = ev.get("type", "")
        
        # Calcul du delta temps avec l'événement suivant pour les quadrants
        next_dt = parsed_events[i + 1][0] if (i + 1 < len(parsed_events)) else None
        dt_step = 0.0
        if dt and next_dt:
            dt_step = min(300.0, max(0.0, (next_dt - dt).total_seconds())) # borné à 5 min

        # 1. EDIT
        if ev_type == "EDIT":
            alert = ev.get("alert")
            ins = ev.get("lines_added", 0)
            chunk_max = ev.get("chunk_max", 0)
            fname = ev.get("file", "unknown")
            dt_sec = ev.get("dt_sec")
            
            if alert == "BURST_INSERT":
                burst_insert_count += 1
                burst_insert_lines += ins
                t_str = f" en {dt_sec}s" if dt_sec is not None else ""
                alerts.append(f"🚨 **BURST_INSERT** : +{ins} lignes insérées d'un bloc{t_str} dans `{fname}` (chunk: {chunk_max}L)")
            elif alert == "BURST_NEW_FILE":
                burst_new_file_count += 1
                burst_new_file_lines += ins
                alerts.append(f"🚨 **BURST_NEW_FILE** : Nouveau fichier `{fname}` créé directement avec {ins} lignes")

            # Réinitialisation de la séquence d'acharnement si modification de code réelle
            if ins + ev.get("lines_removed", 0) >= 3:
                consecutive_errors = 0
                
            q_edit += min(dt_step, 60.0)
            if dt_step > 60.0:
                q_read += (dt_step - 60.0)

        # 2. COMPILE
        elif ev_type == "COMPILE":
            status = ev.get("status", "")
            fname = ev.get("file", "unknown")
            if status == "OK":
                compile_ok += 1
                consecutive_errors = 0
                if fname in pending_errors and dt:
                    ttf = (dt - pending_errors[fname]).total_seconds()
                    if ttf > 0:
                        ttf_values.append(ttf)
                    del pending_errors[fname]
            elif status == "ERROR":
                compile_err += 1
                consecutive_errors += 1
                if consecutive_errors >= 3:
                    thrash_episodes += 1
                    alerts.append(f"⚠️ **Acharnement détecté** : {consecutive_errors} erreurs répétées sans modification de code substantielle (`{fname}`)")
                if fname not in pending_errors and dt:
                    pending_errors[fname] = dt
                    
            q_test += min(dt_step, 30.0)
            if dt_step > 30.0:
                q_read += (dt_step - 30.0)

        # 3. EXECUTE
        elif ev_type == "EXECUTE":
            status = ev.get("status", "")
            fname = ev.get("file", "unknown")
            if status == "DONE":
                exec_done += 1
                consecutive_errors = 0
                if fname in pending_errors and dt:
                    ttf = (dt - pending_errors[fname]).total_seconds()
                    if ttf > 0:
                        ttf_values.append(ttf)
                    del pending_errors[fname]
            elif status == "ERROR":
                exec_err += 1
                consecutive_errors += 1
                if consecutive_errors >= 3:
                    thrash_episodes += 1
                    alerts.append(f"⚠️ **Acharnement sur exécution** : {consecutive_errors} plantages successifs sans modification de code (`{fname}`)")
                if fname not in pending_errors and dt:
                    pending_errors[fname] = dt
            elif status == "HALTED":
                exec_halted += 1
                consecutive_errors = 0
                
            q_test += min(dt_step, 45.0)
            if dt_step > 45.0:
                q_read += (dt_step - 45.0)

        # 4. FOCUS
        elif ev_type == "FOCUS":
            st = ev.get("status", "")
            if st == "LOST":
                focus_lost_count += 1
                last_focus_lost_dt = dt
            elif st == "GAINED":
                dur = ev.get("duration_sec")
                if dur is not None:
                    total_away_sec += float(dur)
                    q_away += float(dur)
                elif last_focus_lost_dt and dt:
                    away_diff = max(0.0, (dt - last_focus_lost_dt).total_seconds())
                    total_away_sec += away_diff
                    q_away += away_diff
                last_focus_lost_dt = None

        # 5. PASTE
        elif ev_type == "PASTE":
            paste_count += 1
            chars = ev.get("chars", 0)
            lines = ev.get("lines", 0)
            paste_chars += chars
            paste_lines += lines
            fname = ev.get("file", "unknown")
            if lines >= 15 or chars >= 400:
                alerts.append(f"📋 **Collage volumineux** : {lines} lignes ({chars} caractères) collées dans `{fname}`")
            q_edit += min(dt_step, 30.0)

        # 6. TYPING
        elif ev_type == "TYPING":
            typing_keys += ev.get("typed_chars", 0)
            typing_backspaces += ev.get("backspaces", 0)
            q_edit += min(dt_step, 30.0)

        # 7. FILE_DROP
        elif ev_type == "FILE_DROP":
            fname = ev.get("file", "unknown")
            size = ev.get("size_bytes", 0)
            file_drops.append({"file": fname, "size": size, "time": dt.strftime("%H:%M:%S") if dt else ""})
            alerts.append(f"📥 **FILE_DROP** : Fichier externe `{fname}` ({size} octets) déposé par glisser-déposer dans l'IDE")

        # 8. FILE_OP
        elif ev_type == "FILE_OP":
            op = ev.get("op", "")
            fname = ev.get("file", "")
            dest = ev.get("dest")
            file_ops.append({"op": op, "file": fname, "dest": dest})

        # 9. HELP
        elif ev_type == "HELP":
            st = ev.get("status", "")
            if st == "REQUESTED":
                help_requests += 1
            elif st == "RESOLVED":
                help_resolved += 1

        # 10. SESSION
        elif ev_type == "SESSION":
            st = ev.get("status", "")
            if st == "CONNECTED":
                sessions_count += 1

        # 11. VIEW
        elif ev_type == "VIEW":
            q_read += min(dt_step, 60.0)

    # Clôture du calcul de focus lost s'il est resté ouvert
    if last_focus_lost_dt and valid_dts:
        tail_away = max(0.0, (valid_dts[-1] - last_focus_lost_dt).total_seconds())
        total_away_sec += tail_away
        q_away += tail_away

    # Dérivation des indicateurs mathématiques
    
    # A. Score d'Authenticité S_auth
    total_burst_lines = burst_insert_lines + burst_new_file_lines
    denom_auth = typing_keys + paste_chars + (total_burst_lines * 30)
    if denom_auth > 0:
        s_auth = (typing_keys / float(denom_auth)) * 100.0
    else:
        # Aucun caractère saisi ni collé
        s_auth = 0.0 if total_burst_lines > 0 else 100.0
    s_auth = max(0.0, min(100.0, round(s_auth, 1)))

    if s_auth >= 85.0:
        s_auth_level = f"Authentique ({s_auth}% saisie clavier)"
    elif s_auth >= 50.0:
        s_auth_level = f"Mixte ({s_auth}% saisie, collages partiels)"
    else:
        s_auth_level = f"🚨 Suspect ({s_auth}% saisie - Injection/Collages dominants)"

    # B. Taux de Navette Externe I_nav
    total_time_ref = max(res["span_duration_sec"], total_away_sec, 1.0)
    i_nav = (total_away_sec / float(total_time_ref)) * 100.0
    i_nav = max(0.0, min(100.0, round(i_nav, 1)))

    if i_nav <= 25.0:
        i_nav_level = f"Forte concentration locale ({i_nav}% hors IDE, {focus_lost_count} sorties)"
    elif i_nav <= 55.0:
        i_nav_level = f"Consultation normale ({i_nav}% hors IDE, {focus_lost_count} sorties)"
    else:
        i_nav_level = f"⚠️ Navette externe intensive ({i_nav}% hors IDE, {focus_lost_count} sorties)"

    # C. Indice d'Acharnement I_thrash
    total_compiles = compile_ok + compile_err
    total_execs = exec_done + exec_err + exec_halted
    total_runs = total_compiles + total_execs
    i_thrash = round(thrash_episodes / float(max(1, total_runs)), 2)

    # D. Time-To-Fix moyen (TTF)
    ttf_avg = round(sum(ttf_values) / len(ttf_values), 1) if ttf_values else None

    # E. Répartition des Quadrants de Temps
    sum_q = q_edit + q_read + q_test + q_away
    if sum_q > 0:
        p_edit = round((q_edit / sum_q) * 100.0, 1)
        p_read = round((q_read / sum_q) * 100.0, 1)
        p_test = round((q_test / sum_q) * 100.0, 1)
        p_away = round((q_away / sum_q) * 100.0, 1)
    else:
        p_edit = p_read = p_test = p_away = 25.0

    # Sauvegarde dans res
    res["typing_keys"] = typing_keys
    res["typing_backspaces"] = typing_backspaces
    res["paste_chars"] = paste_chars
    res["paste_lines"] = paste_lines
    res["paste_count"] = paste_count
    res["burst_insert_count"] = burst_insert_count
    res["burst_insert_lines"] = burst_insert_lines
    res["burst_new_file_count"] = burst_new_file_count
    res["burst_new_file_lines"] = burst_new_file_lines
    res["s_auth"] = s_auth
    res["s_auth_level"] = s_auth_level
    
    res["focus_lost_count"] = focus_lost_count
    res["total_away_sec"] = round(total_away_sec, 1)
    res["i_nav"] = i_nav
    res["i_nav_level"] = i_nav_level
    
    res["compilations"]["total"] = total_compiles
    res["compilations"]["ok"] = compile_ok
    res["compilations"]["error"] = compile_err
    res["compilations"]["error_rate"] = round((compile_err / float(max(1, total_compiles))) * 100.0, 1)

    res["executions"]["total"] = total_execs
    res["executions"]["done"] = exec_done
    res["executions"]["error"] = exec_err
    res["executions"]["halted"] = exec_halted
    res["executions"]["error_rate"] = round((exec_err / float(max(1, total_execs))) * 100.0, 1)

    res["i_thrash"] = i_thrash
    res["thrash_episodes_count"] = thrash_episodes
    res["ttf_avg_sec"] = ttf_avg
    
    res["time_quadrants"] = {
        "q_edit_sec": round(q_edit, 1),
        "q_read_sec": round(q_read, 1),
        "q_test_sec": round(q_test, 1),
        "q_away_sec": round(q_away, 1),
        "q_edit_pct": p_edit,
        "q_read_pct": p_read,
        "q_test_pct": p_test,
        "q_away_pct": p_away,
    }

    res["file_drops"] = file_drops
    res["file_ops"] = file_ops
    res["help_requests"] = help_requests
    res["help_resolved"] = help_resolved
    res["sessions_count"] = max(sessions_count, 1)
    res["alerts"] = alerts

    # Calcul des fenêtres glissantes (tranches de 15 minutes)
    sliding_windows = compute_sliding_windows(parsed_events, window_sec=900)
    res["sliding_windows"] = sliding_windows

    # Construction du résumé court console
    status_icon = "🚨" if (s_auth < 50.0 or burst_insert_count > 0 or burst_new_file_count > 0) else ("🟡" if s_auth < 85.0 else "✅")
    res["summary_short"] = f"{status_icon} S_auth: {s_auth}% | Inav: {i_nav}% ({focus_lost_count} sorties) | Runs: {total_runs} (err: {compile_err+exec_err}) | TTF: {ttf_avg or 'N/A'}s | Bursts: {burst_insert_count + burst_new_file_count}"

    # Construction de la section Markdown complète
    md_lines = []
    md_lines.append(f"**Télémétrie d'activité CodeLab** ({res['events_count']} événements tracés)")
    md_lines.append("")
    md_lines.append(f"- **Score d'Authenticité ($S_{{auth}}$)** : **{s_auth}%** — {s_auth_level}")
    md_lines.append(f"  - Frappe réelle : `{typing_keys}` touches (`{typing_backspaces}` corrections backspace)")
    md_lines.append(f"  - Collages détectés : `{paste_chars}` caractères en `{paste_count}` collage(s) ({paste_lines} lignes)")
    if total_burst_lines > 0:
        md_lines.append(f"  - Injections massives (bursts) : **{total_burst_lines} lignes** ({burst_insert_count} bursts d'insertion, {burst_new_file_count} nouveaux fichiers complets)")
        
    md_lines.append(f"- **Taux de Navette Externe ($I_{{nav}}$)** : **{i_nav}%** — {i_nav_level}")
    md_lines.append(f"  - Temps passé hors IDE : `{round(total_away_sec / 60.0, 1)} min` sur une durée totale de `{res['span_duration_min']} min`")
    
    ttf_str = f"`{ttf_avg} s`" if ttf_avg is not None else "*Aucune erreur persistante*"
    md_lines.append(f"- **Cycle de Validation & Robustesse** :")
    md_lines.append(f"  - Compilations : `{total_compiles}` ({compile_ok} succès, {compile_err} échecs)")
    md_lines.append(f"  - Exécutions : `{total_execs}` ({exec_done} terminées, {exec_err} erreurs d'exécution, {exec_halted} interruptions)")
    md_lines.append(f"  - Indice d'acharnement ($I_{{thrash}}$) : **{i_thrash}** ({thrash_episodes} épisode(s) d'erreurs en boucle)")
    md_lines.append(f"  - Temps moyen de résolution ($TTF$) : {ttf_str}")

    md_lines.append(f"- **Budget Temps (4 Quadrants)** :")
    md_lines.append(f"  - ⌨️ Saisie active ($Q_{{edit}}$) : **{p_edit}%** (`{round(q_edit/60.0, 1)} min`)")
    md_lines.append(f"  - 📖 Réflexion / Lecture ($Q_{{read}}$) : **{p_read}%** (`{round(q_read/60.0, 1)} min`)")
    md_lines.append(f"  - ⚙️ Test & Exécution ($Q_{{test}}$) : **{p_test}%** (`{round(q_test/60.0, 1)} min`)")
    md_lines.append(f"  - 🌐 Hors IDE ($Q_{{away}}$) : **{p_away}%** (`{round(q_away/60.0, 1)} min`)")

    # Table des fenêtres glissantes si au moins 2 tranches
    if len(sliding_windows) >= 2:
        md_lines.append("")
        md_lines.append("- **Dynamique Temporelle & Chronologie (Tranches de 15 min)** :")
        md_lines.append("")
        md_lines.append("| Intervalle | Saisie | Collages / Bursts | $S_{auth}$ | Hors IDE | Tests (Erreurs) | Diagnostic de Phase |")
        md_lines.append("| :---: | :---: | :---: | :---: | :---: | :---: | :--- |")
        for sw in sliding_windows:
            auth_col = f"**{sw['s_auth']}%**" if sw['s_auth'] is not None else "*--*"
            burst_str = f" +{sw['burst_lines']}L ({sw['burst_count']}b)" if sw['burst_count'] > 0 else ""
            paste_col = f"`{sw['paste_lines']}L`{burst_str}" if (sw['paste_lines'] > 0 or sw['burst_count'] > 0) else "0"
            runs_cnt = sw['compiles'] + sw['execs']
            runs_col = f"{runs_cnt} ({sw['errors']} err)" if runs_cnt > 0 else "0"
            md_lines.append(f"| {sw['t0_str']} - {sw['t1_str']} | `{sw['keys']}` c. | {paste_col} | {auth_col} | `{sw['inav']}%` | {runs_col} | {sw['diagnostic']} |")

    if alerts:
        md_lines.append("")
        md_lines.append(f"> ⚠️ **Alertes de télémétrie ({len(alerts)} signalement(s))** :")
        for a in alerts[:6]:  # max 6 alertes pour ne pas saturer
            md_lines.append(f"> - {a}")
        if len(alerts) > 6:
            md_lines.append(f"> - *...et {len(alerts) - 6} autre(s) alerte(s).*")

    res["summary_markdown"] = "\n".join(md_lines)
    return res

def generate_mermaid_radars(data: dict, student_name: str = "Étudiant") -> dict:
    """
    Génère deux diagrammes radar Mermaid (syntaxe radar-beta native de CodeLab) :
    1. Radar des Indicateurs Clés (/20) : Authenticité, Concentration, Débogage, Tests, Architecture, Style
    2. Radar de Typologie Comportementale Multidimensionnelle (/20) : Autonome, Méthodique, Expérimentateur, Persévérant, Recours Externe/IA, Besoin de Guidage
    Toutes les valeurs sont harmonisées sur une échelle de 0 à 20.
    """
    scores = data.get("scores", {})
    act = data.get("activity_analysis") or {}
    ai_data = data.get("ai_analysis") or {}
    ai_prob = float(ai_data.get("probability_pct", 0.0))
    
    # -------------------------------------------------------------
    # 1. RADAR DES INDICATEURS CLÉS (/20)
    # -------------------------------------------------------------
    # Axe 1 : Authenticité de frappe (S_auth)
    if act.get("active"):
        auth_val = min(20.0, max(0.0, round(float(act.get("s_auth", 100.0)) / 5.0, 1)))
    else:
        auth_val = max(0.0, min(20.0, round(20.0 - (ai_prob / 5.0), 1)))

    # Axe 2 : Concentration locale (100 - I_nav)
    if act.get("active"):
        focus_val = min(20.0, max(0.0, round((100.0 - float(act.get("i_nav", 0.0))) / 5.0, 1)))
    else:
        focus_val = 18.0

    # Axe 3 : Efficience de débogage (absence d'acharnement)
    if act.get("active"):
        i_thrash = float(act.get("i_thrash", 0.0))
        debug_val = min(20.0, max(0.0, round(20.0 - (i_thrash * 20.0), 1)))
    else:
        debug_val = 16.0

    # Axe 4 : Validation & Tests
    test_score = float(scores.get("tests", data.get("score_transversal", 0.0)))
    test_max = 2.0 if test_score <= 2.5 else 4.0
    if test_max > 0:
        test_val = min(20.0, max(0.0, round((test_score / test_max) * 20.0, 1)))
    else:
        test_val = 14.0

    # Axe 5 : Architecture & Conception
    arch_score = float(scores.get("architecture", data.get("score_p2", scores.get("axe2_architecture", 0.0))))
    arch_max = 8.0 if arch_score <= 8.5 else (10.0 if arch_score <= 10.5 else 20.0)
    arch_val = min(20.0, max(0.0, round((arch_score / arch_max) * 20.0, 1)))

    # Axe 6 : Style & Conventions
    style_score = float(scores.get("style", scores.get("axe3_style", data.get("score_p1", 0.0))))
    style_max = 4.0 if style_score <= 4.5 else (2.0 if style_score <= 2.5 else 8.0)
    style_val = min(20.0, max(0.0, round((style_score / style_max) * 20.0, 1)))

    # Assainissement du nom de l'étudiant pour la syntaxe Mermaid
    import re
    clean_name = re.sub(r'[\"\'\[\]\{\}\(\)]', '', student_name or "Étudiant").strip()
    if not clean_name:
        clean_name = "Étudiant"

    radar_ind_code = f"""```mermaid
%%{{init: {{'radar': {{'marginLeft': 100, 'marginRight': 100, 'marginTop': 40, 'marginBottom': 40, 'axisLabelFactor': 1.12}}, 'themeVariables': {{'fontSize': '22px', 'radar': {{'axisLabelFontSize': 22}}}}}}}}%%
radar-beta
    showLegend false
    axis auth["Authenticité"], focus["Concentration"], debug["Débogage"], test["Tests & Validation"], arch["Architecture"], style["Style & PEP8"]
    curve etud{{{auth_val}, {focus_val}, {debug_val}, {test_val}, {arch_val}, {style_val}}}
    max 20
    min 0
    graticule polygon
```"""

    # -------------------------------------------------------------
    # 2. RADAR D'AFFINITÉ AUX 6 PROFILS TYPES ARCHÉTYPAUX (/20)
    # -------------------------------------------------------------
    note_brute = float(scores.get("note_technique_brute", data.get("note_technique_brute", scores.get("total", data.get("total", 10.0)))))
    help_reqs = int(act.get("help_requests", 0))
    burst_cnt = int(act.get("burst_insert_count", 0) + act.get("burst_new_file_count", 0))
    file_drops = int(act.get("file_drop_count", 0))
    total_runs = int(act.get("compilations", {}).get("total", 0) + act.get("executions", {}).get("total", 0))
    i_thrash = float(act.get("i_thrash", 0.0))
    backspaces = int(act.get("typing_backspaces", 0))
    ttf = act.get("ttf_avg_sec")
    idle_max = float(act.get("idle_max_sec", 0.0))
    q_read_pct = float(act.get("time_quadrants", {}).get("q_read_pct", 0.0))
    q_edit_pct = float(act.get("time_quadrants", {}).get("q_edit_pct", 50.0))
    is_suspicious_copy = bool(data.get("is_suspicious_copy", False))

    # 1. LE BOSS (Excellence, rapidité, maîtrise, zéro friction)
    boss_score = (note_brute * 0.60) + (auth_val * 0.40) - (help_reqs * 4.0) - (burst_cnt * 4.0) - (i_thrash * 25.0)
    if ttf is not None:
        if ttf <= 30.0 and note_brute >= 16.0 and help_reqs == 0:
            boss_score += 1.5
        elif ttf > 90.0:
            boss_score -= min(5.0, (ttf - 90.0) / 30.0)
    boss_val = min(20.0, max(0.5, round(boss_score, 1)))

    # 2. L'ARTISAN (Démarche d'apprentissage appliquée, itérative, sans acharnement)
    bonus_bs = 2.0 if backspaces >= 10 else 0.0
    artisan_score = (auth_val * 0.45) + (debug_val * 0.35) + (style_val * 0.20) + bonus_bs - (burst_cnt * 3.5)
    artisan_val = min(20.0, max(1.0, round(artisan_score, 1)))

    # 3. LE CYBORG (Assistance IA / Solution injectée)
    if act.get("active"):
        cyborg_score = ((20.0 - auth_val) * 0.50) + ((20.0 - focus_val) * 0.30) + (burst_cnt * 4.5)
    else:
        cyborg_score = (ai_prob / 100.0) * 20.0
    cyborg_val = min(20.0, max(0.5, round(cyborg_score, 1)))

    # 4. LE CASSE-COU (Acharnement stérile, runs compulsifs, tâtonnement)
    casse_score = (i_thrash * 45.0) + (4.0 if total_runs >= 10 and (ttf is not None and ttf > 180) else 0.0)
    if act.get("active") and total_runs >= 8 and q_read_pct < 15.0:
        casse_score += 4.0
    casse_val = min(20.0, max(0.5, round(casse_score, 1)))

    # 5. LE DÉCROCHEUR (Inactivité prolongée, abandon face aux blocages, peu de runs)
    idle_score = 7.0 if idle_max >= 900 else (4.0 if idle_max >= 450 else 0.0)
    stagnation = 4.0 if (q_read_pct > 40.0 and q_edit_pct < 20.0) else 0.0
    decroch_score = max(0.0, (11.0 - note_brute) * 0.8) + idle_score + stagnation + (4.0 if (act.get("active") and total_runs <= 2) else 0.0)
    decroch_val = min(20.0, max(0.5, round(decroch_score, 1)))

    # 6. LE PIRATE (Collusion directe, import USB / drag & drop, forte similarité)
    pirate_score = (file_drops * 8.0) + (16.0 if is_suspicious_copy else 0.0)
    pirate_val = min(20.0, max(0.5, round(pirate_score, 1)))

    # Identification du profil dominant et secondaire
    profiles_list = [
        {"id": "boss", "name": "Le Boss", "icon": "👑", "val": boss_val, "desc": "Excellence et aisance conceptuelle : résolution directe, haute authenticité et autonomie complète."},
        {"id": "artisan", "name": "L'Artisan", "icon": "🔨", "val": artisan_val, "desc": "Démarche d'apprentissage exemplaire : saisie personnelle, cycles itératifs et résolution méthodique."},
        {"id": "cyborg", "name": "Le Cyborg", "icon": "🤖", "val": cyborg_val, "desc": "Recours marqué aux outils externes / IA : faible authenticité de frappe et injections massives de code."},
        {"id": "casse_cou", "name": "Le Casse-cou", "icon": "⚡", "val": casse_val, "desc": "Tâtonnement aveugle et acharnement stérile : exécutions en boucle sans recul ni lecture des tracebacks."},
        {"id": "decrocheur", "name": "Le Décrocheur", "icon": "🚪", "val": decroch_val, "desc": "Difficultés méthodologiques ou abandon silencieux : périodes d'inactivité prolongées et blocages persistants."},
        {"id": "pirate", "name": "Le Pirate", "icon": "🏴‍☠️", "val": pirate_val, "desc": "Collusion ou importation directe : transferts de fichiers externes (clé USB / réseau) ou copie directe."}
    ]
    sorted_profiles = sorted(profiles_list, key=lambda x: x["val"], reverse=True)
    dominant = sorted_profiles[0]
    secondary = sorted_profiles[1] if (len(sorted_profiles) > 1 and sorted_profiles[1]["val"] >= 10.0 and sorted_profiles[1]["id"] != dominant["id"]) else None

    radar_prof_code = f"""```mermaid
%%{{init: {{'radar': {{'marginLeft': 100, 'marginRight': 100, 'marginTop': 40, 'marginBottom': 40, 'axisLabelFactor': 1.12}}, 'themeVariables': {{'fontSize': '22px', 'radar': {{'axisLabelFontSize': 22}}}}}}}}%%
radar-beta
    showLegend false
    axis boss["Le Boss"], artisan["L'Artisan"], pirate["Le Pirate"], decroch["Le Décrocheur"], cyborg["Le Cyborg"], casse["Le Casse-cou"]
    curve etud{{{boss_val}, {artisan_val}, {pirate_val}, {decroch_val}, {cyborg_val}, {casse_val}}}
    max 20
    min 0
    graticule polygon
```"""

    # Construction du tableau explicatif et diagnostique
    table_lines = [
        "#### 📋 Caractérisation de l'Étudiant selon les Profils Types Didactiques\n",
        f"> 🎯 **Profil Dominant Détecté** : **{dominant['icon']} {dominant['name']}** (Affinité : **{dominant['val']:.1f} / 20**)",
    ]
    if secondary:
        table_lines.append(f"> 💡 *Composante secondaire notable* : **{secondary['icon']} {secondary['name']}** (Affinité : **{secondary['val']:.1f} / 20**)\n")
    else:
        table_lines.append("")

    table_lines.append("| Profil Didactique Type | Démarche & Caractéristiques | Affinité / 20 | Adéquation & Diagnostic |")
    table_lines.append("| :--- | :--- | :---: | :--- |")

    for p in profiles_list:
        val = p["val"]
        pct = (val / 20.0) * 100.0
        if p["id"] == dominant["id"]:
            diag = f"🟢 **Profil dominant** ({pct:.0f}%) : correspond au comportement observé en séance."
        elif val >= 14.0:
            diag = f"🔵 **Forte composante** ({pct:.0f}%) : traits observables très marqués."
        elif val >= 9.0:
            diag = f"🟡 **Affinité modérée** ({pct:.0f}%) : traits observables occasionnels."
        elif val >= 4.0:
            diag = f"⚪ **Faible** ({pct:.0f}%) : comportement marginal ou peu significatif."
        else:
            diag = f"⚪ **Négligeable** ({pct:.0f}%) : aucun signe comportemental associé."

        table_lines.append(f"| {p['icon']} **{p['name']}** | {p['desc']} | **{val:.1f}** | {diag} |")

    table_md = "\n".join(table_lines)

    combined = f"""### 📊 Profil Visuel Multidimensionnel (Diagrammes Radar)

> 💡 **Lecture des Deux Radars Complémentaires** :
> - **Radar de gauche (Indicateurs)** : Évalue les 6 piliers de compétence technique sur 20 (code, concentration, validation et authenticité). Un polygone large et équilibré témoigne d'une maîtrise homogène.
> - **Radar de droite (Profils Types)** : Mesure l'affinité comportementale selon 3 axes opposés deux à deux :
>   - 👑 **Le Boss** $\\longleftrightarrow$ 🚪 **Le Décrocheur** : aisance et réussite autonome face au décrochage silencieux.
>   - 🔨 **L'Artisan** $\\longleftrightarrow$ 🤖 **Le Cyborg** : démarche d'apprentissage authentique face à la délégation externe / IA.
>   - ⚡ **Le Casse-cou** $\\longleftrightarrow$ 🏴‍☠️ **Le Pirate** : combat acharné avec le code face à la facilité de la copie d'autrui.

<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border:none; border-collapse:collapse; margin: 8px 0;">
<tr>
<td align="center" width="50%" style="border:none; padding:0; vertical-align:middle;">

{radar_ind_code}

</td>
<td align="center" width="50%" style="border:none; padding:0; vertical-align:middle;">

{radar_prof_code}

</td>
</tr>
</table>

{table_md}"""

    return {
        "radar_indicateurs": radar_ind_code,
        "radar_profils": radar_prof_code,
        "radars_markdown": combined,
        "tableau_explicatif": table_md,
        "profil_dominant": dominant["name"],
        "profil_dominant_score": dominant["val"],
        "valeurs_indicateurs": {
            "auth": auth_val, "focus": focus_val, "debug": debug_val,
            "test": test_val, "arch": arch_val, "style": style_val
        },
        "valeurs_profils": {
            "boss": boss_val, "artisan": artisan_val, "cyborg": cyborg_val,
            "casse_cou": casse_val, "decrocheur": decroch_val, "pirate": pirate_val,
            # Rétrocompatibilité des anciennes clés
            "auto": boss_val, "meth": artisan_val, "exp": artisan_val,
            "pers": boss_val, "ext": cyborg_val, "guide": decroch_val
        }
    }


def generate_cohort_radar(clusters_data, total_st: int = None) -> str:
    """Génère le diagramme radar Mermaid de répartition des effectifs de la cohorte selon les 6 profils types opposés deux à deux."""
    c_counts = {"boss": 0, "artisan": 0, "pirate": 0, "decroch": 0, "cyborg": 0, "casse": 0}

    if isinstance(clusters_data, list):
        for c in clusters_data:
            cid = str(c.get("id", "")).lower()
            cnt = len(c.get("members", [])) if "members" in c else int(c.get("count", 0))
            if "boss" in cid:
                c_counts["boss"] = cnt
            elif "artisan" in cid:
                c_counts["artisan"] = cnt
            elif "pirate" in cid:
                c_counts["pirate"] = cnt
            elif "decroch" in cid:
                c_counts["decroch"] = cnt
            elif "cyborg" in cid:
                c_counts["cyborg"] = cnt
            elif "casse" in cid:
                c_counts["casse"] = cnt
    elif isinstance(clusters_data, dict):
        for k, v in clusters_data.items():
            k_low = str(k).lower()
            cnt = len(v) if isinstance(v, (list, set)) else int(v)
            if "boss" in k_low:
                c_counts["boss"] = cnt
            elif "artisan" in k_low:
                c_counts["artisan"] = cnt
            elif "pirate" in k_low:
                c_counts["pirate"] = cnt
            elif "decroch" in k_low:
                c_counts["decroch"] = cnt
            elif "cyborg" in k_low:
                c_counts["cyborg"] = cnt
            elif "casse" in k_low:
                c_counts["casse"] = cnt

    vals = [c_counts["boss"], c_counts["artisan"], c_counts["pirate"], c_counts["decroch"], c_counts["cyborg"], c_counts["casse"]]
    max_cnt = max(vals) if vals else 1

    if max_cnt <= 5:
        radar_max = 5
    elif max_cnt <= 10:
        radar_max = 10
    else:
        radar_max = ((max_cnt + 4) // 5) * 5 if (max_cnt % 5 != 0) else (max_cnt + 5)

    tot = total_st if total_st is not None else sum(vals)

    radar_md = f"""### 📊 Répartition Visuelle de la Cohorte (Diagramme Radar des 6 Profils)

<table width="100%" border="0" cellspacing="0" cellpadding="0" style="border:none; border-collapse:collapse; margin: 8px 0;">
<tr>
<td align="center" style="border:none; padding:0; vertical-align:middle;">

```mermaid
%%{{init: {{'radar': {{'marginLeft': 100, 'marginRight': 100, 'marginTop': 40, 'marginBottom': 40, 'axisLabelFactor': 1.15}}, 'themeVariables': {{'fontSize': '22px', 'radar': {{'axisLabelFontSize': 22}}}}}}}}%%
radar-beta
    showLegend false
    axis boss["Le Boss"], artisan["L'Artisan"], pirate["Le Pirate"], decroch["Le Décrocheur"], cyborg["Le Cyborg"], casse["Le Casse-cou"]
    curve effectif{{{c_counts['boss']}, {c_counts['artisan']}, {c_counts['pirate']}, {c_counts['decroch']}, {c_counts['cyborg']}, {c_counts['casse']}}}
    max {radar_max}
    min 0
    graticule polygon
```

</td>
</tr>
</table>

> 💡 **Lecture du Radar de Cohorte (Effectifs par Profil Type)** :
> Ce diagramme met en évidence la répartition des effectifs d'étudiants ({tot} étudiants au total) selon les 3 axes opposés deux à deux :
> - 👑 **Le Boss** $\\longleftrightarrow$ 🚪 **Le Décrocheur** : autonomie et excellence face au décrochage silencieux.
> - 🔨 **L'Artisan** $\\longleftrightarrow$ 🤖 **Le Cyborg** : démarche d'apprentissage authentique face à la délégation externe / IA.
> - ⚡ **Le Casse-cou** $\\longleftrightarrow$ 🏴‍☠️ **Le Pirate** : combat acharné avec le code face à la collusion / copie d'autrui."""
    return radar_md

if __name__ == "__main__":
    # Test autonome sur un dossier passé en argument
    target = sys.argv[1] if len(sys.argv) > 1 else "."
    result = analyze_student_activity(target)
    print(json.dumps(result, indent=2, ensure_ascii=False, default=str))

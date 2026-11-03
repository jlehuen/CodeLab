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
        dt = parse_iso_timestamp(ev.get("timestamp", ""))
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

    if alerts:
        md_lines.append("")
        md_lines.append("> [!WARNING]")
        md_lines.append(f"> **Alertes de télémétrie ({len(alerts)} signalement(s))** :")
        for a in alerts[:6]:  # max 6 alertes pour ne pas saturer
            md_lines.append(f"> - {a}")
        if len(alerts) > 6:
            md_lines.append(f"> - *...et {len(alerts) - 6} autre(s) alerte(s).*")

    res["summary_markdown"] = "\n".join(md_lines)
    return res

if __name__ == "__main__":
    # Test autonome sur un dossier passé en argument
    target = sys.argv[1] if len(sys.argv) > 1 else "."
    result = analyze_student_activity(target)
    print(json.dumps(result, indent=2, ensure_ascii=False, default=str))

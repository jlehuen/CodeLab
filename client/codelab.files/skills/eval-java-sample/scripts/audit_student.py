#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_student.py - Moteur d'audit automatique d'un dossier Java (Exemple).
Analyse les classes .java, contrôle la syntaxe et évalue les principes objet.
Dernière modification : 10/10/2026
"""

import os
import sys
import re
import json
import tempfile
import subprocess
from pathlib import Path
from datetime import datetime

SKILL_DIR = Path(__file__).resolve().parent.parent


def load_weights():
    """Charge dynamiquement les pondérations depuis weights.json."""
    for p in (SKILL_DIR / "weights.json", Path(__file__).resolve().parent / "weights.json"):
        if p.is_file():
            try:
                return json.loads(p.read_text(encoding="utf-8"))
            except Exception:
                pass
    return {}


def strip_java_comments(content):
    """
    Supprime les commentaires Java (/* ... */ et // ...) tout en préservant les numéros
    de lignes (sauts de ligne conservés) et en neutralisant les littéraux de chaînes.
    """
    pattern = re.compile(
        r'("(?:\\.|[^"\\])*")|(\'(?:\\.|[^\'\\])*\')|(/\*[\s\S]*?\*/)|(//[^\n]*)',
        re.MULTILINE
    )
    def _repl(match):
        if match.group(1) is not None:
            return '""'
        if match.group(2) is not None:
            return "''"
        comment = match.group(3) or match.group(4) or ''
        return '\n' * comment.count('\n')
    return pattern.sub(_repl, content)


def _resolve_codelab_javac():
    """Localise l'exécutable javac embarqué par CodeLab (via JAVAC_CMD, JAVA_HOME ou infos.txt)."""
    # 1. Variable d'environnement explicite transmise par CodeLab (SessionEvaluator)
    javac_env = os.environ.get("JAVAC_CMD")
    if javac_env and os.path.isfile(javac_env) and os.access(javac_env, os.X_OK):
        return javac_env

    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        cand = os.path.join(java_home, "bin", "javac")
        if os.path.isfile(cand) and os.access(cand, os.X_OK):
            return cand

    # 2. Fichier ~/codelab.files/.hidden/infos.txt généré au démarrage de CodeLab
    infos_file = Path.home() / "codelab.files" / ".hidden" / "infos.txt"
    if infos_file.is_file():
        try:
            for line in infos_file.read_text(encoding="utf-8", errors="ignore").splitlines():
                if line.startswith("JAVA_HOME="):
                    jh = line.split("=", 1)[1].strip()
                    cand = os.path.join(jh, "bin", "javac")
                    if os.path.isfile(cand) and os.access(cand, os.X_OK):
                        return cand
        except Exception:
            pass

    # 3. Recherche dans les bundles CodeLab (Applications, Bureau, ou dépôt de dev)
    skill_root = Path(__file__).resolve().parent.parent
    search_globs = [
        Path("/Applications/CodeLab.app/Contents/Java"),
        Path.home() / "Applications" / "CodeLab.app" / "Contents" / "Java",
        Path.home() / "Desktop" / "CodeLab.app" / "Contents" / "Java",
        skill_root.parent.parent.parent / "mac-app-arm" / "Contents" / "Java",
        skill_root.parent.parent.parent / "mac-app-x64" / "Contents" / "Java",
    ]
    for java_dir in search_globs:
        if java_dir.is_dir():
            for javac_path in sorted(java_dir.glob("jdk*/Contents/Home/bin/javac"), reverse=True):
                if javac_path.is_file() and os.access(javac_path, os.X_OK):
                    return str(javac_path)

    return "javac"


def run_javac_check(java_files):
    if not java_files:
        return True, ""
    javac_bin = _resolve_codelab_javac()
    try:
        with tempfile.TemporaryDirectory() as tmpdir:
            cmd = [javac_bin, "-nowarn", "-d", tmpdir] + [str(f) for f in java_files]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            return res.returncode == 0, res.stderr
    except Exception as e:
        return False, str(e)

def format_lines(lines, start_idx, end_idx, highlight_line=None, note=None):
    """Formate une portion de lignes avec numérotation 'ligne | code' et annotation optionnelle."""
    start = max(0, start_idx)
    end = min(len(lines), end_idx)
    res = []
    for i in range(start, end):
        l_num = i + 1
        line_str = lines[i].rstrip('\r\n')
        if l_num == highlight_line and note:
            res.append(f"{l_num:3d} | {line_str}  // <-- {note}")
        else:
            res.append(f"{l_num:3d} | {line_str}")
    return '\n'.join(res)

def detect_ai_markers(java_files, student_dir=None):
    """
    Analyse statique et stylométrique multi-dimensionnelle des signatures d'assistance IA en Java :
    1. Streams API & Lambdas avancées hors L1/L2 (.stream(), ->, .filter(), .map(), .collect()).
    2. Java 14+ records, pattern matching, var (record ..., var ..., instanceof Type x).
    3. Optional & Concurrency / NIO avancés (Optional<T>, java.util.concurrent, java.nio.file).
    4. Javadoc industrielle et sur-documentation (@param, @return, @throws formels).
    5. Phrasé didactique Chatbot & Commentaires en anglais.
    6. Rupture d'indentation intra-copie ou globale (4 espaces vs tabulations natives CodeLab).
    7. Chronométrie CodeLab : classes complexes créées en moins de 60s/300s ou rafale en séance unique sans historique .bak.
    """
    markers = []
    category_weights = {}
    subtype_counts = {}
    CATEGORY_CAPS = {
        'syntaxe_avancee': 0.35,
        'syntaxe_moderne': 0.30,
        'bibliotheque_avancee': 0.30,
        'stylometrie_ia': 0.35,
        'variance_stylometrique': 0.45,
        'chronometrie_instantanee': 0.45,
        'mention_explicite': 1.0,
    }

    def add_marker(rel_path, category, subtype, description, base_weight):
        markers.append({
            'file': rel_path,
            'category': category,
            'subtype': subtype,
            'description': description
        })
        cnt = subtype_counts.get(subtype, 0)
        subtype_counts[subtype] = cnt + 1
        factor = 1.0 if cnt == 0 else (0.35 if cnt == 1 else 0.15)
        cur = category_weights.get(category, 0.0)
        cap = CATEGORY_CAPS.get(category, 0.35)
        category_weights[category] = min(cap, cur + base_weight * factor)

    created_timestamps = []
    file_indent = {}
    total_tabs = 0
    total_spaces = 0
    bak_count = 0
    if student_dir:
        try:
            bak_count = len(list(Path(student_dir).rglob("*.bak"))) + len(list(Path(student_dir).rglob("*_bak.java")))
        except Exception:
            pass

    for fpath in java_files:
        rel_path = fpath.name
        try:
            content = fpath.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue

        code_nc = strip_java_comments(content)
        lines = content.splitlines()
        loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])

        # Analyse d'indentation (Tabulations natives CodeLab vs 4 Espaces LLM Web)
        t_count = sum(1 for l in lines if l.startswith('\t'))
        s_count = sum(1 for l in lines if l.startswith('    '))
        total_tabs += t_count
        total_spaces += s_count
        file_indent[rel_path] = {'tabs': t_count, 'spaces': s_count, 'loc': loc}

        # Horodatage CodeLab
        m_dt = re.search(r'Created by .* on (\d{2}/\d{2}/\d{2}) \((\d{2}:\d{2}:\d{2})\)', content)
        if m_dt:
            try:
                dt = datetime.strptime(f"{m_dt.group(1)} {m_dt.group(2)}", "%d/%m/%y %H:%M:%S")
                created_timestamps.append((dt, rel_path, loc))
            except Exception:
                pass

        # 1. Streams API & Lambdas (hors commentaires)
        if re.search(r'\.stream\s*\(\)', code_nc) or re.search(r'->\s*\{', code_nc) or re.search(r'\.(?:filter|map|collect)\s*\(', code_nc):
            add_marker(rel_path, 'syntaxe_avancee', 'streams', "Usage de la Streams API / Lambdas hors-programme début L1/L2", 0.30)

        # 2. Java 14+ records, pattern matching, var (hors commentaires)
        if re.search(r'\brecord\s+[A-Z]\w*\s*\(', code_nc):
            add_marker(rel_path, 'syntaxe_moderne', 'record', "Enregistrement 'record' Java moderne hors-programme L1/L2", 0.25)

        if re.search(r'\bvar\s+[a-zA-Z0-9_]+\s*=', code_nc):
            add_marker(rel_path, 'syntaxe_moderne', 'var', "Inférence de type 'var' atypique pour un apprentissage du typage statique", 0.20)

        if re.search(r'\binstanceof\s+[A-Z]\w+\s+[a-z]\w*', code_nc):
            add_marker(rel_path, 'syntaxe_moderne', 'pattern_instanceof', "Pattern Matching for instanceof Java 16+ hors-programme", 0.22)

        # 3. Optional & Concurrency / NIO (hors commentaires)
        if re.search(r'\bOptional<', code_nc) or 'java.util.concurrent' in code_nc or 'java.nio.file' in code_nc:
            add_marker(rel_path, 'bibliotheque_avancee', 'optional_nio', "Bibliothèques avancées (Optional / NIO / Concurrent) atypiques pour un débutant", 0.25)

        # 4. Javadoc industrielle formelle
        if len(re.findall(r'[@\\](param|return|throws|author)', content)) >= 4:
            add_marker(rel_path, 'stylometrie_ia', 'javadoc_heavy', "Sur-documentation Javadoc formelle (@param / @return / @throws)", 0.20)

        # 5. Phrasé didactique et étapes scolaires
        didactic = re.findall(r'//\s*(?:(?:Step|[ÉE]tape)\s+\d+|Base case|Helper method|Main logic|Initialize variables)\b', content, re.I)
        if len(didactic) >= 2:
            add_marker(rel_path, 'stylometrie_ia', 'didactic_steps', f"Découpage didactique typique de LLM : '{didactic[0].strip()}'", 0.25)

        # Commentaires en anglais
        english_comments = re.findall(r'//\s*(?:Getters and Setters|Constructor|Main method|Check condition)\b', content, re.I)
        if len(english_comments) >= 2:
            add_marker(rel_path, 'stylometrie_ia', 'english_comments', f"Commentaires en anglais typiques d'une invite LLM : '{english_comments[0].strip()}'", 0.20)

        # Aveu explicite de copier-coller ou d'aide externe dans les commentaires
        if re.search(r'//\s*(?:copier\s*coller|copi[eé]\s*coll[eé]|aide\s+(?:avec|sur)\s+(?:forum|site|internet))', content, re.I):
            add_marker(rel_path, 'stylometrie_ia', 'copy_paste_admission', "Aveu explicite dans les commentaires de copier-coller ou d'aide externe (forum/site)", 0.35)

        # Mentions directes d'outils IA
        if re.search(r'(?://|/\*)\s*(?:ChatGPT|Copilot|Claude|OpenAI|DeepSeek|AI generated|Prompt)', content, re.I):
            add_marker(rel_path, 'mention_explicite', 'explicit_ai', "Mention textuelle d'outil IA dans les commentaires", 1.0)

    # Rupture d'indentation intra-copie ou globale Tab ➔ 4 Espaces
    for f_rel, ind in file_indent.items():
        other_tabs = total_tabs - ind['tabs']
        other_spaces = total_spaces - ind['spaces']
        if ind['spaces'] >= 20 and (ind['spaces'] > ind['tabs'] * 3 or ind['tabs'] <= 3):
            if other_tabs >= 25 and other_tabs > other_spaces * 1.5:
                add_marker(f_rel, 'variance_stylometrique', 'indentation_spaces_rupture',
                           f"Rupture d'indentation intra-copie : {ind['spaces']} lignes en 4 espaces contre {ind['tabs']} tabulation(s), en contraste avec le style natif CodeLab ({other_tabs} tabulations dans le reste de la copie)", 0.35)
    if total_spaces >= 35 and total_spaces > total_tabs * 2:
        add_marker("Ensemble des fichiers", 'variance_stylometrique', 'indentation_spaces_rupture',
                   f"Indentation dominante en 4 espaces ({total_spaces} lignes en 4 espaces vs {total_tabs} tabulations natives CodeLab) caractéristique d'un copier-coller externe / LLM", 0.35)

    # Chronométrie (< 300s pour classes complexes, < 60s pour fichiers standards, ou rafale sans .bak)
    dates_info = {}
    if created_timestamps:
        created_timestamps.sort(key=lambda x: x[0])
        distinct_days = len(set(x[0].date() for x in created_timestamps))
        span_days = (created_timestamps[-1][0].date() - created_timestamps[0][0].date()).days
        dates_info = {
            "distinct_days": distinct_days,
            "span_days": span_days,
            "first_date": created_timestamps[0][0].strftime("%d/%m/%Y"),
            "last_date": created_timestamps[-1][0].strftime("%d/%m/%Y")
        }
        if distinct_days == 1 and len(created_timestamps) >= 5 and bak_count == 0:
            add_marker(created_timestamps[-1][1], 'chronometrie_instantanee', 'single_session_dump',
                       f"Dépôt massif de {len(created_timestamps)} classes sur une seule séance sans aucun historique .bak", 0.40)

    for i in range(1, len(created_timestamps)):
        dt_prev, f_prev, loc_prev = created_timestamps[i - 1]
        dt_curr, f_curr, loc_curr = created_timestamps[i]
        diff_sec = (dt_curr - dt_prev).total_seconds()
        if 0 < diff_sec <= 300 and loc_curr >= 60:
            add_marker(f_curr, 'chronometrie_instantanee', 'fast_complex_creation', f"Classe complexe ({loc_curr} lignes) produite en seulement {int(diff_sec)}s (< 5 min) après {f_prev} (copier-coller externe / IA probable)", 0.35)
        elif 0 < diff_sec < 60 and loc_curr >= 25 and loc_prev >= 15:
            add_marker(f_curr, 'chronometrie_instantanee', 'fast_creation', f"Création quasi-instantanée ({int(diff_sec)}s) d'une classe de {loc_curr} lignes (copier-coller externe)", 0.30)

    total_weight = sum(category_weights.values())
    active_categories = [cat for cat, w in category_weights.items() if w >= 0.15]
    if len(active_categories) < 2 and 'mention_explicite' not in category_weights:
        total_weight = min(total_weight, 0.45)

    score_pct = min(100, int(round(total_weight * 100)))
    score = min(1.0, round(total_weight, 2))
    if score_pct >= 60:
        level = f"Élevé ({score_pct}% - Forte présomption d'assistance IA)"
    elif score_pct >= 30:
        level = f"Modéré ({score_pct}% - Idiomes atypiques détectés)"
    else:
        level = f"Faible ({score_pct}% - Code étudiant authentique)"

    descriptions = [f"{m['file']}: {m['description']}" for m in markers[:8]]
    return {
        'level': level,
        'score': score,
        'probability_pct': score_pct,
        'count': len(markers),
        'markers': markers,
        'details': "; ".join(descriptions) if descriptions else "Aucune signature d'IA détectée",
        'indentation_stats': {
            'total_tabs': total_tabs,
            'total_spaces': total_spaces,
            'files': file_indent
        },
        'dates_info': dates_info
    }

def audit_student(folder_path, sessions_xml_path=None):
    folder = Path(folder_path).resolve()
    java_files = [f for f in folder.glob("*.java") if not f.name.startswith(".") and not f.name.endswith(".bak")]
    if not java_files:
        def _is_ign(f):
            try:
                return any(p.startswith(".") or p in ("__pycache__", "evaluations", "codelab", "bin", "build") for p in f.relative_to(folder).parts[:-1]) or f.name.endswith(".bak")
            except Exception:
                return False
        java_files = [f for f in folder.rglob("*.java") if not _is_ign(f)]

    # Détection de l'identité et du travail
    student_id = folder.name
    assignment = folder.name
    if bool(re.match(r"^(tp\d*|td\d*|projet.*|partie.*|src|exercices?)$", folder.name, re.I)) and folder.parent.name:
        student_id = folder.parent.name
        assignment = f"{folder.parent.name}/{folder.name}"

    student_name = None
    if sessions_xml_path and os.path.isfile(sessions_xml_path):
        try:
            import xml.etree.ElementTree as ET
            tree = ET.parse(sessions_xml_path)
            for u in tree.findall(".//user"):
                if u.get("login") == student_id and u.get("name"):
                    student_name = u.get("name")
                    break
        except Exception:
            pass

    if not student_name:
        scan_files = java_files if java_files else list(folder.glob("**/*.java"))
        for f in scan_files:
            try:
                cnt = f.read_text(encoding="utf-8", errors="ignore")[:3000]
                m = re.search(r"Created by\s+([^0-9\r\n\t]+?)\s+on\s+[0-9]{2}/[0-9]{2}/[0-9]{2}", cnt)
                if m:
                    cand = m.group(1).strip()
                    if cand and len(cand) >= 3 and cand.lower() not in ("john doe", "none", "inconnu", "anonymous", "_author_"):
                        student_name = cand
                        break
            except Exception:
                pass

    if not student_name and folder.parent and folder.parent.name == student_id:
        for f in list(folder.parent.glob("*.java")) + list(folder.parent.glob("**/*.java")):
            try:
                cnt = f.read_text(encoding="utf-8", errors="ignore")[:3000]
                m = re.search(r"Created by\s+([^0-9\r\n\t]+?)\s+on\s+[0-9]{2}/[0-9]{2}/[0-9]{2}", cnt)
                if m:
                    cand = m.group(1).strip()
                    if cand and len(cand) >= 3 and cand.lower() not in ("john doe", "none", "inconnu", "anonymous", "_author_"):
                        student_name = cand
                        break
            except Exception:
                pass

    results = {
        "student_id": student_id,
        "student_name": student_name or student_id,
        "assignment": assignment,
        "folder": str(folder),
        "files_count": len(java_files),
        "java_files": [f.name for f in java_files],
        "classes_count": 0,
        "interfaces_count": 0,
        "methods_count": 0,
        "private_fields": 0,
        "public_fields": 0,
        "javadoc_blocks": 0,
        "has_tests": False,
        "compilation_ok": True,
        "compilation_error": "",
        "scores": {
            "compilation": 0.0,
            "architecture": 0.0,
            "style": 0.0,
            "tests": 0.0,
            "note_technique_brute": 0.0,
            "note_apres_ia": 0.0,
            "total": 0.0
        },
        "indentation_stats": {"total_tabs": 0, "total_spaces": 0, "files": {}},
        "dates_info": {"distinct_days": 0, "span_days": 0, "first_date": "N/A", "last_date": "N/A"},
        "ai_detection": {"level": "Faible (0%)", "score": 0.0, "probability_pct": 0, "count": 0, "markers": []},
        "ai_analysis": {"level": "Faible (0%)", "score": 0.0, "probability_pct": 0, "count": 0, "markers": []},
        "feedback": []
    }

    if not java_files:
        results["feedback"].append("Aucun fichier source Java (.java) trouvé dans le dossier.")
        return results

    ok, err = run_javac_check(java_files)
    results["compilation_ok"] = ok
    if not ok:
        results["compilation_error"] = err.strip()[:300]

    total_classes = 0
    total_interfaces = 0
    total_methods = 0
    total_privates = 0
    total_privates = 0
    total_publics = 0
    total_javadoc = 0
    has_test = False
    snippets = {
        "compilation_errors": [],
        "anti_patterns": [],
        "highlights": []
    }

    for j_file in java_files:
        try:
            content = j_file.read_text(encoding="utf-8", errors="replace")
        except Exception:
            continue

        code_nc = strip_java_comments(content)
        lines = content.splitlines()

        # Highlights : Javadoc formalisé (max 1)
        if len(snippets["highlights"]) < 1:
            jd_m = re.search(r'(/\*\*[\s\S]*?\*/\s*\n\s*(?:public|protected|private|static|\s)*[\w<>\[\]]+\s+[A-Za-z0-9_]+\s*\([^)]*\)\s*\{?)', content)
            if jd_m:
                start_l = content[:jd_m.start()].count('\n')
                match_lines = lines[start_l : min(len(lines), start_l + 7)]
                formatted = '\n'.join(f"{start_l + idx + 1:3d} | {l.rstrip()}" for idx, l in enumerate(match_lines))
                snippets["highlights"].append({
                    "title": "Méthode documentée avec Javadoc formalisé",
                    "file": j_file.name,
                    "line": start_l + 1,
                    "code": formatted
                })

        # Highlights : Tests unitaires (max 1)
        if ("test" in j_file.name.lower() or "assert" in code_nc.lower()) and not any("test" in h["title"].lower() for h in snippets["highlights"]):
            test_m = re.search(r'(@Test\s*\n\s*(?:public\s+)?void\s+[A-Za-z0-9_]+\s*\([^)]*\)\s*\{[\s\S]*?\})', content)
            if test_m:
                start_l = content[:test_m.start()].count('\n')
                match_lines = lines[start_l : min(len(lines), start_l + 7)]
                formatted = '\n'.join(f"{start_l + idx + 1:3d} | {l.rstrip()}" for idx, l in enumerate(match_lines))
                snippets["highlights"].append({
                    "title": "Méthode de test unitaire JUnit",
                    "file": j_file.name,
                    "line": start_l + 1,
                    "code": formatted
                })

        # Anti-pattern : Attribut public non-statique (brèche d'encapsulation objet, vérifié hors commentaires)
        if len(snippets["anti_patterns"]) < 1:
            pub_m = re.search(r'^(.*?\bpublic\s+(?!static\s+final)[\w<>\[\]]+\s+([A-Za-z0-9_]+)\s*(=|;).*)$', code_nc, re.MULTILINE)
            if pub_m:
                var_name = pub_m.group(2)
                l_num = code_nc[:pub_m.start()].count('\n') + 1
                orig_line = lines[l_num - 1].strip() if 0 <= l_num - 1 < len(lines) else pub_m.group(1).strip()
                snippets["anti_patterns"].append({
                    "title": f"Attribut public '{var_name}' (brèche d'encapsulation, préférer 'private' + getters/setters)",
                    "file": j_file.name,
                    "line": l_num,
                    "code": f"{l_num:3d} | {orig_line}  // <-- ATTENTION : brèche d'encapsulation (attribut public)"
                })

        # Classes et interfaces (hors commentaires)
        total_classes += len(re.findall(r'\bclass\s+[A-Za-z0-9_]+', code_nc))
        total_interfaces += len(re.findall(r'\binterface\s+[A-Za-z0-9_]+', code_nc))

        # Méthodes (hors commentaires, supportant l'accolade sur la ligne suivante)
        methods = re.findall(r'\b(public|protected|private|static|\s)+[\w<>\[\]]+\s+([A-Za-z0-9_]+)\s*\([^)]*\)\s*(?:throws\s+[\w,\s]+)?\{', code_nc)
        total_methods += sum(1 for _, mname in methods if mname not in ('if', 'for', 'while', 'switch', 'catch'))

        # Champs privés vs publics (hors commentaires)
        privates = re.findall(r'\bprivate\s+[\w<>\[\]]+\s+[A-Za-z0-9_]+\s*(=|;)', code_nc)
        publics = re.findall(r'\bpublic\s+(?!static\s+final)[\w<>\[\]]+\s+[A-Za-z0-9_]+\s*(=|;)', code_nc)
        total_privates += len(privates)
        total_publics += len(publics)

        # Javadoc (sur le contenu brut)
        total_javadoc += len(re.findall(r'/\*\*.*?\*/', content, re.DOTALL))

        # Tests (hors commentaires)
        test_checks = len(re.findall(r'\b(?:assert|assertEquals|assertTrue|assertFalse|@Test)\b', code_nc))
        if "test" in j_file.name.lower() or test_checks > 0:
            has_test = True

    # Erreur de compilation javac si présente
    if not results["compilation_ok"] and results["compilation_error"]:
        m_err = re.search(r'([a-zA-Z0-9_./-]+):(\d+):\s*error:\s*(.*)', results["compilation_error"])
        if m_err:
            err_file = os.path.basename(m_err.group(1))
            err_l = int(m_err.group(2))
            err_text = m_err.group(3).strip()
            for jf in java_files:
                if jf.name == err_file:
                    j_lines = jf.read_text(encoding="utf-8", errors="replace").splitlines()
                    code_snip = format_lines(j_lines, err_l - 3, err_l + 2, highlight_line=err_l, note=err_text)
                    snippets["compilation_errors"].append({
                        "file": err_file,
                        "line": err_l,
                        "error": err_text,
                        "code": code_snip
                    })
                    break

    results["snippets"] = snippets
    results["classes_count"] = total_classes
    results["interfaces_count"] = total_interfaces
    results["methods_count"] = total_methods
    results["private_fields"] = total_privates
    results["public_fields"] = total_publics
    results["javadoc_blocks"] = total_javadoc
    results["has_tests"] = has_test

    weights = load_weights()
    axes_w = weights.get("axes", {})
    comp_max = float(axes_w.get("axe1_compilation_max", 6.0))
    arch_max = float(axes_w.get("axe2_conception_max", 8.0))
    style_max = float(axes_w.get("axe3_style_max", 4.0))
    tests_max = float(axes_w.get("axe4_validation_max", 2.0))

    # 1. Compilation javac exigeante (/6)
    results["scores"]["compilation"] = comp_max if results["compilation_ok"] else round(comp_max / 6.0, 1)

    # 2. Modélisation Objet & Encapsulation exigeante (/8)
    arch_pts = 0.0
    if total_classes >= 2 and total_interfaces >= 1:
        arch_pts += 3.5
    elif total_classes >= 2 or total_interfaces >= 1:
        arch_pts += 2.5
    elif total_classes == 1:
        arch_pts += 1.5
    if total_methods >= 5:
        arch_pts += 2.5
    elif total_methods >= 3:
        arch_pts += 1.5
    elif total_methods >= 1:
        arch_pts += 0.8
    if total_privates > 0 and total_publics == 0:
        arch_pts += 2.0  # Encapsulation stricte respectée
    elif total_privates > total_publics:
        arch_pts += max(0.0, 1.2 - 0.5 * total_publics)
    if not results["compilation_ok"]:
        arch_pts *= 0.65
    results["scores"]["architecture"] = min(arch_max, round((arch_pts / 8.0) * arch_max, 1))

    # 3. Javadoc & Normes POO (/4)
    style_pts = 0.0
    expected_javadoc = max(2, total_classes + total_interfaces + (total_methods // 2))
    jd_cov = total_javadoc / float(expected_javadoc) if expected_javadoc > 0 else 0.0
    if jd_cov >= 0.75 and total_javadoc >= 2:
        style_pts += 2.0
    elif jd_cov >= 0.40 or total_javadoc >= 2:
        style_pts += 1.2
    elif total_javadoc >= 1:
        style_pts += 0.6
    if len(java_files) > 0 and total_publics == 0 and results["compilation_ok"]:
        style_pts += 1.5
    elif len(java_files) > 0 and total_publics == 0:
        style_pts += 0.8
    if total_methods >= 2 and total_privates >= 1:
        style_pts += 0.5
    results["scores"]["style"] = min(style_max, round((style_pts / 4.0) * style_max, 1))

    # 4. Tests & Validation (/2)
    total_assert_calls = sum(
        len(re.findall(r'\b(?:assert|assertEquals|assertTrue|assertFalse|@Test)\b', strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))))
        for jf in java_files
    )
    has_dedicated_test_file = any("test" in jf.name.lower() for jf in java_files)
    if total_assert_calls >= 3 or (has_dedicated_test_file and total_assert_calls >= 1):
        test_pts = 2.0
    elif total_assert_calls >= 1 or has_dedicated_test_file:
        test_pts = 1.0
    else:
        test_pts = 0.0
    results["scores"]["tests"] = min(tests_max, round((test_pts / 2.0) * tests_max, 1))

    ai_info = detect_ai_markers(java_files, student_dir=folder)
    results["ai_analysis"] = ai_info
    results["dates_info"] = ai_info.get("dates_info", {})
    ai_prob = ai_info.get("probability_pct", 0)

    note_sans_malus = round(
        results["scores"]["compilation"] + results["scores"]["architecture"] + results["scores"]["style"] + results["scores"]["tests"],
        1
    )
    results["scores"]["note_technique_brute"] = note_sans_malus

    anti_ia = weights.get("anti_triche", {}).get("assistance_ia", {})
    malus_eleve = float(anti_ia.get("malus_eleve", 2.0))
    plafond_eleve = float(anti_ia.get("plafond_note_eleve", 12.0))
    malus_modere = float(anti_ia.get("malus_modere", 1.0))
    plafond_modere = float(anti_ia.get("plafond_note_modere", 14.0))

    total_note = note_sans_malus
    if ai_prob >= 60:
        total_note = min(plafond_eleve, max(0.0, round(total_note - malus_eleve, 1)))
    elif ai_prob >= 30:
        total_note = min(plafond_modere, max(0.0, round(total_note - malus_modere, 1)))

    results["scores"]["note_apres_ia"] = total_note
    results["scores"]["total"] = total_note
    return results

def main():
    if len(sys.argv) < 2:
        print("Usage: audit_student.py <student_folder>")
        sys.exit(1)
    import json
    res = audit_student(sys.argv[1])
    print(json.dumps(res, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()

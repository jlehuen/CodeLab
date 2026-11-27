#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_student.py - Moteur d'audit automatique d'un dossier Haskell (Exemple Générique).
Analyse les fichiers .hs et carnets .ipynb Haskell via GHC (-fno-code -Wall) et inspection statique.
Dernière modification : 27/11/2026
"""

import os
import sys
import re
import json
import shutil
import tempfile
import subprocess
from pathlib import Path
from datetime import datetime

SKILL_DIR = Path(__file__).resolve().parent.parent
SKILLS_ROOT = SKILL_DIR.parent
if str(SKILLS_ROOT) not in sys.path:
    sys.path.insert(0, str(SKILLS_ROOT))

from skills_common import (
    load_weights as _load_weights_common,
    resolve_haskell_compiler,
    detect_notebook_language,
    extract_notebook_code,
    run_declarative_tests,
)


def load_weights():
    """Charge dynamiquement les pondérations depuis weights.json."""
    return _load_weights_common(SKILL_DIR)


def is_haskell_notebook(ipynb_path):
    """Vérifie si un fichier .ipynb est un carnet Haskell."""
    return detect_notebook_language(ipynb_path) == "haskell"


def extract_haskell_from_notebook(ipynb_path):
    """Extrait les cellules de code d'un carnet .ipynb Haskell en filtrant les commandes GHCi (:t, :i, ...)."""
    return extract_notebook_code(ipynb_path, "haskell")


def strip_haskell_comments(content):
    """
    Supprime les commentaires Haskell (-- ... et {- ... -}) et neutralise les littéraux
    chaînes/caractères tout en préservant les sauts de ligne.
    """
    pattern = re.compile(
        r'("(?:\\.|[^"\\\n])*")|(\'(?:\\.|[^\'\\\n])\')|(\{-[\s\S]*?-\})|(--[^\n]*)',
        re.MULTILINE
    )

    def _repl(match):
        if match.group(1) is not None:
            return '""'
        if match.group(2) is not None:
            return "' '"
        comment = match.group(3) or match.group(4) or ""
        return "\n" * comment.count("\n")

    return pattern.sub(_repl, content)


def format_lines(lines, start_idx, end_idx, highlight_line=None, note=None):
    """Formate une portion de lignes avec numérotation 'ligne | code' et annotation optionnelle."""
    start = max(0, start_idx)
    end = min(len(lines), end_idx)
    res = []
    for i in range(start, end):
        l_num = i + 1
        line_str = lines[i].rstrip("\r\n")
        if l_num == highlight_line and note:
            res.append(f"{l_num:3d} | {line_str}  -- <-- {note}")
        else:
            res.append(f"{l_num:3d} | {line_str}")
    return "\n".join(res)


def find_ghc_compiler():
    """Localise le compilateur ghc (aligné sur HASKELL_CMD / CodeLab)."""
    return resolve_haskell_compiler()


def run_ghc_check(source_entries):
    """
    Vérifie la compilation et le typage via `ghc -fno-code -Wall`.
    source_entries: liste de tuples (display_name, path_or_none, content_str).
    Retourne (errors_dict, warnings_dict).
    """
    errors = {}
    warnings = {}
    if not source_entries:
        return errors, warnings

    ghc = find_ghc_compiler()
    if not ghc:
        # Fallback statique léger : vérification d'équilibre des parenthèses/crochets
        for name, _, content in source_entries:
            code_nc = strip_haskell_comments(content)
            if code_nc.count("(") != code_nc.count(")") or code_nc.count("[") != code_nc.count("]"):
                errors[name] = "Parenthèses ou crochets déséquilibrés"
        return errors, warnings

    with tempfile.TemporaryDirectory(prefix="codelab_hs_eval_") as tmpdir:
        tmp_p = Path(tmpdir)
        for name, orig_path, content in source_entries:
            is_ipynb = name.lower().endswith(".ipynb")
            code_nc = strip_haskell_comments(content)
            has_module_hdr = bool(re.search(r"^\s*module\s+[A-Z]", code_nc, re.M))
            has_main_def = bool(re.search(r"^main\s*(?:::=|=|::)", code_nc, re.M))
            safe_stem = re.sub(r"[^A-Za-z0-9_]", "_", Path(name).stem)

            if is_ipynb:
                target_file = tmp_p / f"Nb_{safe_stem}.hs"
                decl_lines = []
                if not has_module_hdr:
                    decl_lines.append(f"module Nb_{safe_stem} where")
                for ln in content.splitlines():
                    s = ln.strip()
                    if not s or s.startswith("--"):
                        decl_lines.append(ln)
                    elif ln[0].isspace() or any(tok in s for tok in ("=", "::", "->", "|")) or re.match(r"^(module|import|data|newtype|type|class|instance|deriving|where|let|in|case|of|if|then|else|do)\b", s):
                        decl_lines.append(ln)
                    else:
                        decl_lines.append(f"-- [expr] {ln}")
                target_file.write_text("\n".join(decl_lines), encoding="utf-8")
                cmd = [ghc, "-fno-code", "-Wall", "-Wno-missing-signatures", "-Wno-type-defaults", str(target_file)]
            elif not has_module_hdr and not has_main_def:
                # Fichier .hs destiné à GHCi sans en-tête `module` ni `main` : évite le faux positif [GHC-67120]
                target_file = tmp_p / f"Mod_{safe_stem}.hs"
                target_file.write_text(f"module Mod_{safe_stem} where\n{content}", encoding="utf-8")
                cmd = [ghc, "-fno-code", "-Wall", "-Wno-type-defaults", "-i" + str(orig_path.parent), str(target_file)]
            else:
                cmd = [ghc, "-fno-code", "-Wall", "-Wno-type-defaults", "-i" + str(orig_path.parent), str(orig_path)]

            try:
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
                stderr = (res.stderr or "") + "\n" + (res.stdout or "")
                file_warns = []
                file_errs = []
                for line in stderr.splitlines():
                    ls = line.strip()
                    if "error:" in ls:
                        msg = ls.split("error:", 1)[-1].strip()
                        if msg:
                            file_errs.append(msg)
                    elif "warning:" in ls:
                        msg = ls.split("warning:", 1)[-1].strip()
                        if msg:
                            file_warns.append(msg)
                if res.returncode != 0:
                    if not file_errs:
                        tail = [l.strip() for l in stderr.splitlines() if l.strip()]
                        file_errs = tail[:3] if tail else ["Erreur de compilation / typage GHC"]
                    errors[name] = "; ".join(file_errs[:3])
                if file_warns:
                    warnings[name] = file_warns[:5]
            except subprocess.TimeoutExpired:
                errors[name] = "Timeout lors de la vérification GHC (> 15s)"
            except Exception as e:
                errors[name] = f"Erreur d'exécution GHC : {e}"

    return errors, warnings


def detect_ai_markers(source_entries, student_dir=None):
    """
    Analyse statique et stylométrique multi-dimensionnelle des signatures d'assistance IA en Haskell :
    1. Pragmas/extensions GHC avancées hors programme d'initiation ({-# LANGUAGE ... #-}).
    2. Imports avancés hors programme (Control.Monad.Trans, Control.Lens, System.IO.Unsafe, Data.Aeson, etc.).
    3. Stylométrie didactique Chatbot (commentaires formatés, étapes numérotées, anglais artificiel).
    4. Rupture d'indentation intra-copie ou globale.
    5. Chronométrie CodeLab : fichiers complexes créés en moins de 60s/300s.
    """
    markers = []
    category_weights = {}
    subtype_counts = {}
    CATEGORY_CAPS = {
        "extensions_ghc": 0.40,
        "imports_hors_programme": 0.40,
        "stylometrie_ia": 0.35,
        "variance_stylometrique": 0.40,
        "chronometrie_instantanee": 0.45,
        "mention_explicite": 1.0,
    }

    def add_marker(rel_path, category, subtype, description, base_weight):
        markers.append({
            "file": rel_path,
            "category": category,
            "subtype": subtype,
            "description": description,
        })
        cnt = subtype_counts.get(subtype, 0)
        subtype_counts[subtype] = cnt + 1
        factor = 1.0 if cnt == 0 else (0.35 if cnt == 1 else 0.15)
        cur = category_weights.get(category, 0.0)
        cap = CATEGORY_CAPS.get(category, 0.35)
        category_weights[category] = min(cap, cur + base_weight * factor)

    dates_seen = set()
    created_timestamps = []
    all_timestamps = []
    indentation_stats = {"total_tabs": 0, "total_spaces": 0, "files": {}}
    file_complexities = []

    for name, fpath, content in source_entries:
        code_nc = strip_haskell_comments(content)
        lines = content.splitlines()
        non_empty = len([l for l in code_nc.splitlines() if l.strip()])

        if fpath and fpath.is_file():
            try:
                all_timestamps.append(fpath.stat().st_mtime)
            except Exception:
                pass

        # En-tête CodeLab ("Created by ... on DD/MM/YY (HH:MM:SS)")
        m_dt = re.search(r"Created by .*? on (\d{2}/\d{2}/\d{2})(?: \((\d{2}:\d{2}:\d{2})\))?", content[:600])
        if m_dt:
            dates_seen.add(m_dt.group(1))
            try:
                if m_dt.group(2):
                    dt_c = datetime.strptime(f"{m_dt.group(1)} {m_dt.group(2)}", "%d/%m/%y %H:%M:%S")
                    created_timestamps.append((dt_c, name, non_empty))
            except Exception:
                pass

        # 1. Mention explicite
        if re.search(r"(?:généré par|generated by|chatgpt|openai|claude|gemini|copilot)", content, re.I):
            add_marker(name, "mention_explicite", "explicit_ai", "Mention explicite d'un assistant IA dans le code", 0.90)

        # 2. Pragmas/extensions GHC avancées
        pragmas = re.findall(r"\{-#\s*LANGUAGE\s+([^#]+?)#-\}", content)
        if pragmas:
            add_marker(
                name, "extensions_ghc", "language_pragma",
                f"Extension GHC avancée hors programme d'initiation ({', '.join(p.strip() for p in pragmas[:2])})",
                0.28
            )

        # 3. Imports avancés hors programme débutant
        adv_imports = re.findall(
            r"^\s*import\s+(?:qualified\s+)?(Control\.Monad\.Trans|Control\.Lens|System\.IO\.Unsafe|Data\.Aeson|Control\.Arrow|Control\.Concurrent|GHC\.Generics)",
            code_nc, re.M
        )
        if adv_imports:
            add_marker(
                name, "imports_hors_programme", "advanced_import",
                f"Import avancé hors programme ({', '.join(sorted(set(adv_imports)))})",
                0.32
            )

        # 4. Stylométrie Chatbot
        if re.search(r"--\s*(?:Étape|Step)\s+\d+\s*[:\-]", content, re.I):
            add_marker(name, "stylometrie_ia", "numbered_steps", "Commentaires numérotés typiques d'un plan généré par LLM", 0.22)

        en_ai_comments = re.findall(r"--\s*(?:Base case|Recursive case|Helper function|Define the|Example usage|Main function)\b", content)
        if len(en_ai_comments) >= 3:
            add_marker(name, "stylometrie_ia", "en_chatbot_comments", f"{len(en_ai_comments)} commentaires didactiques anglais caractéristiques d'un LLM", 0.22)

        # 5. Indentation
        f_tabs = sum(1 for l in lines if l.startswith("\t"))
        f_spaces = sum(1 for l in lines if re.match(r"^(?:  |    )\S", l))
        indentation_stats["total_tabs"] += f_tabs
        indentation_stats["total_spaces"] += f_spaces
        indentation_stats["files"][name] = {"tabs": f_tabs, "spaces": f_spaces}

        file_complexities.append((name, non_empty))

    # Télémétrie CodeLab si disponible (alignée sur C / Python / Java)
    activity_analysis = None
    if student_dir:
        try:
            skills_root = SKILL_DIR.parent
            if str(skills_root) not in sys.path:
                sys.path.insert(0, str(skills_root))
            from activity_analyzer import analyze_student_activity
            act = analyze_student_activity(student_dir)
            if act.get("active"):
                activity_analysis = act
            if act.get("has_rich_telemetry"):
                for a in act.get("alerts", []):
                    if "BURST_INSERT" in a:
                        add_marker("Télémétrie", "chronometrie_instantanee", "burst_insert", a, 0.45)
                    elif "BURST_NEW_FILE" in a:
                        add_marker("Télémétrie", "chronometrie_instantanee", "burst_new_file", a, 0.40)
                s_auth = act.get("s_auth")
                if s_auth is not None and float(s_auth) < 50.0:
                    add_marker("Télémétrie", "variance_stylometrique", "low_s_auth", f"Score d'authenticité télémétrique faible ({s_auth:.0f}%)", 0.35)
        except Exception:
            pass

    _tel_authentic = bool(
        activity_analysis
        and activity_analysis.get("has_rich_telemetry")
        and activity_analysis.get("s_auth") is not None
        and float(activity_analysis.get("s_auth")) >= 85.0
    )

    # Chronométrie inter-fichiers (alignée sur C / Python / Java — neutralisée si frappe authentique prouvée)
    if not _tel_authentic and len(created_timestamps) >= 2:
        created_timestamps.sort(key=lambda x: x[0])
        for i in range(1, len(created_timestamps)):
            dt_prev, f_prev, loc_prev = created_timestamps[i - 1]
            dt_curr, f_curr, loc_curr = created_timestamps[i]
            diff_sec = (dt_curr - dt_prev).total_seconds()
            if 0 < diff_sec <= 300 and loc_curr >= 60:
                add_marker(f_curr, "chronometrie_instantanee", "fast_complex_creation", f"Module complexe ({loc_curr} lignes) produit en seulement {int(diff_sec)}s (< 5 min) après {f_prev}", 0.35)
            elif 0 < diff_sec < 60 and loc_curr >= 25 and loc_prev >= 15:
                add_marker(f_curr, "chronometrie_instantanee", "fast_creation", f"Création quasi-instantanée ({int(diff_sec)}s) d'un module de {loc_curr} lignes après {f_prev}", 0.30)

    raw_prob = sum(category_weights.values())
    active_categories = [cat for cat, w in category_weights.items() if w >= 0.15]
    if len(active_categories) < 2 and "mention_explicite" not in category_weights:
        raw_prob = min(raw_prob, 0.45)
    raw_prob = min(1.0, raw_prob)
    prob_pct = int(round(raw_prob * 100))

    if prob_pct >= 60:
        level = f"Élevé ({prob_pct}% - Forte suspicion IA)"
        verdict = "Marqueurs forts d'assistance IA détectés"
    elif prob_pct >= 30:
        level = f"Modéré ({prob_pct}% - Idiomes avancés / à vérifier)"
        verdict = "Quelques constructions avancées ou atypiques à vérifier"
    else:
        level = f"Faible ({prob_pct}% - Code authentique)"
        verdict = "Code cohérent avec une démarche d'apprentissage authentique"

    dates_info = {
        "distinct_days": len(dates_seen) if dates_seen else (len({datetime.fromtimestamp(t).strftime("%Y-%m-%d") for t in all_timestamps}) if all_timestamps else 0),
        "span_days": 0,
        "first_date": min(dates_seen) if dates_seen else "N/A",
        "last_date": max(dates_seen) if dates_seen else "N/A",
    }
    if len(all_timestamps) >= 2:
        dates_info["span_days"] = int((max(all_timestamps) - min(all_timestamps)) / 86400)

    return {
        "level": level,
        "score": round(raw_prob, 2),
        "probability_pct": prob_pct,
        "ai_probability": float(prob_pct),
        "count": len(markers),
        "verdict": verdict,
        "details": verdict,
        "markers": markers[:15],
        "indentation_stats": indentation_stats,
        "dates_info": dates_info,
        "activity_analysis": activity_analysis,
    }


def audit_student(student_dir, sessions_xml=None):
    """Audite un dossier étudiant contenant des fichiers .hs et/ou des carnets .ipynb Haskell."""
    p = Path(student_dir).resolve()
    student_id = p.name
    assignment = p.name

    def _is_ign(f):
        try:
            rel_parts = f.relative_to(p).parts
            if any(part.startswith(".") or part in ("evaluations", "__pycache__", ".hidden", "codelab", "recovery", "dist-newstyle") for part in rel_parts):
                return True
        except Exception:
            pass
        low = f.name.lower()
        return low.startswith(".") or low.endswith(".bak") or low.endswith("_bak.hs") or "copy" in low

    hs_files = sorted([f for f in p.glob("**/*.hs") if not _is_ign(f)])
    ipynb_files = sorted([f for f in p.glob("**/*.ipynb") if not _is_ign(f) and is_haskell_notebook(f)])

    source_entries = []
    for f in hs_files:
        try:
            source_entries.append((f.name, f, f.read_text(encoding="utf-8", errors="ignore")))
        except Exception:
            pass
    for nb in ipynb_files:
        extracted = extract_haskell_from_notebook(nb)
        if extracted.strip():
            source_entries.append((nb.name, nb, extracted))

    weights = load_weights()
    axes_w = weights.get("axes", {})
    comp_max = float(axes_w.get("axe1_compilation_max", 6.0))
    arch_max = float(axes_w.get("axe2_conception_max", 8.0))
    style_max = float(axes_w.get("axe3_style_max", 4.0))
    tests_max = float(axes_w.get("axe4_validation_max", 2.0))

    results = {
        "student_id": student_id,
        "student_name": student_id,
        "assignment": assignment,
        "folder": str(p),
        "scores": {
            "compilation": 0.0,
            "architecture": 0.0,
            "style": 0.0,
            "tests": 0.0,
            "note_technique_brute": 0.0,
            "note_apres_ia": 0.0,
            "total": 0.0,
        },
        "axes_max": {
            "compilation": comp_max,
            "architecture": arch_max,
            "style": style_max,
            "tests": tests_max,
        },
        "hs_files": [name for name, _, _ in source_entries],
        "total_files": len(source_entries),
        "valid_hs_count": 0,
        "functions": [],
        "type_signatures_count": 0,
        "algebraic_types_count": 0,
        "higher_order_count": 0,
        "pattern_matching_count": 0,
        "recursion_count": 0,
        "comments_count": 0,
        "tests_count": 0,
        "compilation_errors": {},
        "syntax_errors": {},
        "ghc_warnings": {},
        "antipatterns": [],
        "snippets": {"highlights": [], "compilation_errors": [], "anti_patterns": []},
        "indentation_stats": {"total_tabs": 0, "total_spaces": 0, "files": {}},
        "dates_info": {"distinct_days": 0, "span_days": 0, "first_date": "N/A", "last_date": "N/A"},
        "ai_detection": {"level": "Faible (0%)", "score": 0.0, "probability_pct": 0, "count": 0, "markers": []},
        "ai_analysis": {"level": "Faible (0%)", "score": 0.0, "probability_pct": 0, "count": 0, "markers": []},
        "feedback": [],
    }

    if not source_entries:
        empty_ai = {
            "level": "Faible (0% - Aucun fichier Haskell)",
            "score": 0.0,
            "probability_pct": 0,
            "count": 0,
            "details": "Aucun fichier Haskell (.hs ou .ipynb Haskell) trouvé",
            "ai_probability": 0.0,
            "verdict": "Aucun fichier Haskell trouvé",
            "markers": [],
        }
        results["ai_detection"] = empty_ai
        results["ai_analysis"] = empty_ai
        results["feedback"].append("Aucun fichier source Haskell (.hs ou .ipynb Haskell) trouvé dans le dossier.")
        return results

    # 1. Compilation & typage GHC
    comp_errs, ghc_warns = run_ghc_check(source_entries)
    results["compilation_errors"] = comp_errs
    results["syntax_errors"] = comp_errs
    results["ghc_warnings"] = ghc_warns
    results["valid_hs_count"] = len(source_entries) - len(comp_errs)

    # 2. Analyse statique des idiomes fonctionnels
    all_functions = []
    signatures = set()
    algebraic_types = []
    higher_order_uses = 0
    pattern_match_funcs = set()
    recursive_funcs = set()
    guards_count = 0
    total_comments = 0
    total_tests = 0
    total_code_lines = 0
    antipatterns_found = []
    snippets = {"highlights": [], "compilation_errors": [], "anti_patterns": []}

    hs_reserved = {"module", "import", "data", "newtype", "type", "class", "instance", "where", "let", "in", "if", "then", "else", "case", "of", "do", "deriving", "infix", "infixl", "infixr"}
    sig_re = re.compile(r"^([a-z_][a-zA-Z0-9_']*)\s*::\s*(.+)$", re.M)
    def_re = re.compile(r"^([a-z_][a-zA-Z0-9_']*)\b([^=\n]*)(?:=|\|)", re.M)
    adt_re = re.compile(r"^\s*(data|newtype|type)\s+([A-Z][a-zA-Z0-9_']*)", re.M)
    hof_re = re.compile(r"\b(map|filter|foldr|foldl|foldl'|zipWith|concatMap|any|all|curry|uncurry)\b|\s+\.\s+")
    partial_re = re.compile(r"\b(head|tail|init|last|fromJust)\b")
    bool_if_re = re.compile(r"\bif\b.+\bthen\s+(?:True|False)\s+else\s+(?:True|False)\b")

    for name, _, content in source_entries:
        code_nc = strip_haskell_comments(content)
        lines = content.splitlines()
        nc_lines = [l for l in code_nc.splitlines() if l.strip()]
        total_code_lines += len(nc_lines)

        total_comments += len(re.findall(r"--.*", content))
        total_comments += len(re.findall(r"\{-[\s\S]*?-\}", content))

        for m in sig_re.finditer(code_nc):
            fn = m.group(1)
            if fn not in hs_reserved:
                signatures.add(fn)

        fn_clause_counts = {}
        fn_bodies = {}
        for m in def_re.finditer(code_nc):
            fn = m.group(1)
            if fn in hs_reserved:
                continue
            fn_clause_counts[fn] = fn_clause_counts.get(fn, 0) + 1
            if fn not in all_functions:
                all_functions.append(fn)
            line_idx = code_nc[:m.start()].count("\n")
            fn_bodies.setdefault(fn, []).append(lines[line_idx] if line_idx < len(lines) else "")

        for fn, cnt in fn_clause_counts.items():
            if cnt >= 2:
                pattern_match_funcs.add(fn)

        # Détection de `case ... of` et de gardes `|`
        case_matches = len(re.findall(r"\bcase\b[\s\S]{1,80}?\bof\b", code_nc))
        if case_matches > 0:
            pattern_match_funcs.add(f"case@{name}")
        guards_count += len(re.findall(r"^\s+\|\s+[^=]+=", code_nc, re.M))

        # Détection simple de récursivité (appel du nom de fonction après '=' sur les lignes suivantes)
        for fn in fn_clause_counts:
            if fn == "main":
                continue
            # Vérifie l'apparition du nom de fonction dans le corps (après '=' ou '|')
            for m_body in re.finditer(rf"^{re.escape(fn)}\b[^\n]*?=(.+)$", code_nc, re.M):
                rhs = m_body.group(1)
                if re.search(rf"\b{re.escape(fn)}\b", rhs):
                    recursive_funcs.add(fn)
                    break

        for m in adt_re.finditer(code_nc):
            algebraic_types.append(m.group(2))

        higher_order_uses += len(hof_re.findall(code_nc))

        # Tests : prop_*, test*, assert, quickCheck, ou main contenant print/putStrLn
        total_tests += len(re.findall(r"^(?:prop_|test)[a-zA-Z0-9_']*\b", code_nc, re.M))
        total_tests += len(re.findall(r"\b(?:quickCheck|assert|hspec|TestCase)\b", code_nc))
        if re.search(r"^main\s*(?:::=|=|::)", code_nc, re.M) and re.search(r"\b(?:print|putStrLn)\b", code_nc):
            total_tests += 1

        # Highlights (max 2) : fonction typée + filtrage par motif ou type algébrique
        if len(snippets["highlights"]) < 2:
            for m in sig_re.finditer(code_nc):
                fn = m.group(1)
                if fn != "main" and fn not in hs_reserved:
                    l_idx = code_nc[:m.start()].count("\n")
                    snippets["highlights"].append({
                        "title": f"Fonction typée `{fn}`",
                        "function": fn,
                        "file": name,
                        "line": l_idx + 1,
                        "code": format_lines(lines, l_idx, min(len(lines), l_idx + 7)),
                    })
                    break

        # Anti-patterns
        m_part = partial_re.search(code_nc)
        if m_part:
            l_num = code_nc[:m_part.start()].count("\n") + 1
            ap_msg = f"Usage de la fonction partielle `{m_part.group(1)}` dans `{name}` (préférer le filtrage par motif)"
            if ap_msg not in antipatterns_found:
                antipatterns_found.append(ap_msg)
            if len(snippets["anti_patterns"]) < 2:
                snippets["anti_patterns"].append({
                    "title": f"Fonction partielle `{m_part.group(1)}` (risque d'exception sur liste vide)",
                    "file": name,
                    "line": l_num,
                    "code": format_lines(lines, l_num - 2, l_num + 2, highlight_line=l_num, note="Préférer le pattern matching"),
                })

        m_bif = bool_if_re.search(code_nc)
        if m_bif:
            l_num = code_nc[:m_bif.start()].count("\n") + 1
            ap_msg = f"Expression booléenne redondante `if ... then True else False` dans `{name}`"
            if ap_msg not in antipatterns_found:
                antipatterns_found.append(ap_msg)
            if len(snippets["anti_patterns"]) < 2:
                snippets["anti_patterns"].append({
                    "title": "Conditionnelle booléenne redondante (`if c then True else False`)",
                    "file": name,
                    "line": l_num,
                    "code": format_lines(lines, l_num - 1, l_num + 2, highlight_line=l_num, note="Retourner directement la condition"),
                })

        if name in comp_errs:
            snippets["compilation_errors"].append({
                "file": name,
                "error": comp_errs[name],
                "code": format_lines(lines, 0, min(12, len(lines))),
            })

    results["snippets"] = snippets
    results["functions"] = all_functions
    results["type_signatures_count"] = len(signatures)
    results["algebraic_types_count"] = len(algebraic_types)
    results["higher_order_count"] = higher_order_uses
    results["pattern_matching_count"] = len(pattern_match_funcs)
    results["recursion_count"] = len(recursive_funcs)
    results["comments_count"] = total_comments
    results["tests_count"] = total_tests
    results["antipatterns"] = antipatterns_found

    # 3. Calcul des notes calibrées selon weights.json (6 + 8 + 4 + 2 = 20 pts)
    num_errors = len(comp_errs)
    ratio_valid = results["valid_hs_count"] / float(len(source_entries)) if source_entries else 0.0
    results["scores"]["compilation"] = max(0.0, round(ratio_valid * comp_max - 0.8 * num_errors, 1))

    # Axe 2 : Conception Fonctionnelle (/8)
    non_main_funcs = [f for f in set(all_functions) if f != "main"]
    arch_pts = 0.0
    if len(non_main_funcs) >= 5:
        arch_pts += 3.0
    elif len(non_main_funcs) >= 3:
        arch_pts += 2.2
    elif len(non_main_funcs) >= 1:
        arch_pts += 1.2

    # Signatures de types explicites (jusqu'à 1.5 pts)
    sig_ratio = len(signatures) / float(max(1, len(non_main_funcs)))
    if sig_ratio >= 0.75 and len(signatures) >= 2:
        arch_pts += 1.5
    elif len(signatures) >= 1:
        arch_pts += 0.8

    # Pattern matching & gardes (jusqu'à 1.5 pts)
    if len(pattern_match_funcs) >= 2 or (len(pattern_match_funcs) >= 1 and guards_count >= 2):
        arch_pts += 1.5
    elif len(pattern_match_funcs) >= 1 or guards_count >= 1:
        arch_pts += 0.8

    # Récursivité, types algébriques ou ordre supérieur (jusqu'à 2.0 pts)
    adv_fp = (1 if recursive_funcs else 0) + (1 if algebraic_types else 0) + (1 if higher_order_uses >= 1 else 0)
    if adv_fp >= 2:
        arch_pts += 2.0
    elif adv_fp == 1:
        arch_pts += 1.0

    if num_errors > 0 and source_entries:
        arch_pts *= max(0.5, ratio_valid)
    results["scores"]["architecture"] = min(arch_max, round((arch_pts / 8.0) * arch_max, 1))

    # Axe 3 : Qualité, Style & Exhaustivité (/4)
    style_pts = 0.0
    min_expected_comments = max(2, len(source_entries))
    if total_comments >= min_expected_comments:
        style_pts += 1.5
    elif total_comments >= 1:
        style_pts += 0.7

    total_warns = sum(len(v) for v in ghc_warns.values())
    if total_warns == 0 and num_errors == 0:
        style_pts += 1.5
    elif total_warns <= 2 and num_errors == 0:
        style_pts += 0.8

    if not antipatterns_found and total_code_lines > 0:
        style_pts += 1.0
    elif len(antipatterns_found) == 1:
        style_pts += 0.4
    results["scores"]["style"] = min(style_max, round((style_pts / 4.0) * style_max, 1))

    # Axe 4 : Démarche de Test (/2) — piloté par tests.json si présent, sinon heuristique statique
    decl_tests = run_declarative_tests(SKILL_DIR, p, "haskell")
    results["declarative_tests"] = decl_tests
    if decl_tests.get("active") and decl_tests.get("total", 0) > 0:
        results["scores"]["tests"] = min(tests_max, round(decl_tests["ratio"] * tests_max, 1))
    else:
        has_test_file = any("test" in n.lower() for n, _, _ in source_entries)
        if total_tests >= 3 or (has_test_file and total_tests >= 1):
            test_pts = 2.0
        elif total_tests >= 2 or has_test_file:
            test_pts = 1.2
        elif total_tests == 1:
            test_pts = 0.6
        else:
            test_pts = 0.0
        results["scores"]["tests"] = min(tests_max, round((test_pts / 2.0) * tests_max, 1))

    # 4. Détection IA, Chronométrie & Malus
    ai_info = detect_ai_markers(source_entries, student_dir=p)
    results["ai_detection"] = ai_info
    results["ai_analysis"] = ai_info
    results["dates_info"] = ai_info.get("dates_info", {})
    results["indentation_stats"] = ai_info.get("indentation_stats", {})
    results["activity_analysis"] = ai_info.get("activity_analysis") or {}
    ai_prob = ai_info.get("probability_pct", 0)

    note_sans_malus = round(
        results["scores"]["compilation"] + results["scores"]["architecture"] + results["scores"]["style"] + results["scores"]["tests"],
        1
    )
    results["scores"]["note_technique_brute"] = note_sans_malus

    # Politique zéro faux positif : aucun malus/plafond IA automatique (alerte rouge informative uniquement)
    total_note = note_sans_malus
    results["scores"]["note_apres_ia"] = total_note
    results["scores"]["total"] = total_note
    return results


analyze_student = audit_student


def main():
    if len(sys.argv) < 2:
        print("Usage: audit_student.py <student_folder>")
        sys.exit(1)
    res = audit_student(sys.argv[1])
    print(json.dumps(res, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()

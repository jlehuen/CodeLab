#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
skills_common.py - Module partagé pour les skills d'évaluation CodeLab (C, Python, Haskell, Java).

Factorise :
  - le chargement des pondérations (weights.json)
  - la détection et l'extraction des carnets Jupyter (.ipynb) par langage
  - la résolution des compilateurs/interpréteurs alignée sur la configuration CodeLab (env vars)
  - le moteur générique de détection de plagiat par n-grams Jaccard (fichiers sources + .ipynb)
  - le lanceur de banc de tests fonctionnels déclaratif optionnel (tests.json)
"""

import os
import re
import sys
import json
import shutil
import tempfile
import subprocess
from pathlib import Path
from itertools import combinations


# ---------------------------------------------------------------------------
# 1. Pondérations & Formatage
# ---------------------------------------------------------------------------

def load_weights(skill_dir):
    """Charge dynamiquement weights.json depuis la racine du skill."""
    p_dir = Path(skill_dir)
    for cand in (p_dir / "weights.json", p_dir / "scripts" / "weights.json"):
        if cand.is_file():
            try:
                return json.loads(cand.read_text(encoding="utf-8"))
            except Exception:
                pass
    return {}


def compute_similarity_malus(plagiarism_info, weights=None):
    """
    Politique zéro faux positif : aucun malus de note n'est appliqué automatiquement
    en cas de suspicion de similarité (alerte rouge informative uniquement).
    """
    w_anti = (weights or {}).get("anti_triche", {})
    max_cumule = float(w_anti.get("malus_max_cumule_global", 3.5))
    return 0.0, max_cumule


def apply_combined_penalties(note_brute, ai_prob, plagiarism_info, weights=None):
    """
    Politique zéro faux positif : aucun malus ni plafond automatique n'est appliqué
    en cas de suspicion d'assistance IA ou de similarité. La note technique brute est
    intégralement conservée ; les suspicions sont signalées par un bandeau rouge visible
    afin que l'enseignant décide après vérification humaine.
    Retourne (note_finale, 0.0).
    """
    return max(0.0, round(float(note_brute), 1)), 0.0


def compute_indicative_malus(ai_prob=0.0, plagiarism_info=None, weights=None):
    """
    Calcule le malus INDICATIF (non appliqué automatiquement) à titre purement informatif
    pour l'enseignant :
    - Similarité Forte suspicion uniquement (0.0 sur signal faible) : m2 (1.0) ou m3 (2.0).
    - Assistance IA : malus_eleve (2.0) si >= 60%, malus_modere (1.0) si >= 30%.
    - Cumul plafonné par malus_max_cumule_global (3.5 par défaut).
    Retourne un float >= 0.0 arrondi à 0.1.
    """
    w_anti = (weights or {}).get("anti_triche", {})
    anti_ia = w_anti.get("assistance_ia", {})
    w_sim = w_anti.get("similarite", {})
    malus_eleve = float(anti_ia.get("malus_eleve", 2.0))
    malus_modere = float(anti_ia.get("malus_modere", 1.0))
    m2 = float(w_sim.get("malus_2_fichiers", 1.0))
    m3 = float(w_sim.get("malus_3_fichiers_ou_plus", 2.0))
    m_max = float(w_sim.get("malus_max_total", 2.0))
    max_cumule = float(w_anti.get("malus_max_cumule_global", 3.5))

    ia_p = float(ai_prob or 0.0)
    ia_m = malus_eleve if ia_p >= 60.0 else (malus_modere if ia_p >= 30.0 else 0.0)

    sim_m = 0.0
    if plagiarism_info:
        sev = str(plagiarism_info.get("severity", "") or "").lower()
        is_weak = bool(sev) and ("signal faible" in sev or "vérifier" in sev or "verifier" in sev)
        if not is_weak:
            raw_files = plagiarism_info.get("files", [])
            uniq = {it[0] if isinstance(it, (tuple, list)) and it else str(it) for it in raw_files}
            nb_sim = len(uniq)
            max_sim = float(plagiarism_info.get("max_sim", 0.0) or 0.0)
            is_strong = (
                any(k in sev for k in ("forte", "élevée", "elevee", "critique", "haute", "high"))
                if sev
                else (nb_sim >= 2 or max_sim >= 92.0)
            )
            if is_strong:
                if nb_sim >= 3 or max_sim >= 95.0:
                    sim_m = m3
                else:
                    sim_m = m2
                sim_m = min(sim_m, m_max)

    return round(min(max_cumule, ia_m + sim_m), 1)


def format_indicative_malus_badge(malus):
    """Formate un malus indicatif en badge rouge lisible (ou '—' si nul)."""
    m = float(malus or 0.0)
    if m > 0.0:
        return f'<span style="color:#dc2626; font-weight:bold;">-{m:.1f}</span>'
    return "—"


def build_integrity_red_banner(res, plagiarism_info=None):
    """
    Génère un bandeau HTML rouge bien visible en tête d'un rapport individuel dès qu'une
    alerte de similarité (Forte suspicion / À vérifier) ou d'assistance IA (>= 30%) est détectée.
    Retourne une chaîne vide s'il n'y a aucune alerte.
    """
    items = []
    is_critical = False

    plag = plagiarism_info or (res or {}).get("plagiarism_alert") or {}
    if plag and plag.get("files"):
        partner = plag.get("partner", "un autre étudiant")
        max_sim = float(plag.get("max_sim", 0.0) or 0.0)
        sev = str(plag.get("severity", "") or "")
        sev_low = sev.lower()
        f_list = ", ".join(
            f"<code>{f[0]}</code> ({f[1]}%)" if isinstance(f, (tuple, list)) and len(f) >= 2 else f"<code>{f}</code>"
            for f in plag.get("files", [])
        )
        if not sev or any(k in sev_low for k in ("forte", "élevée", "elevee", "critique", "haute", "high")):
            is_critical = True
            items.append(
                f"<li><b>🚨 Similarité forte (copier-coller suspecté)</b> avec <code>{partner}</code> "
                f"(max <b>{max_sim:.1f}%</b>) : {f_list}</li>"
            )
        else:
            items.append(
                f"<li><b>⚠️ Similarité à vérifier (signal faible)</b> avec <code>{partner}</code> "
                f"(max <b>{max_sim:.1f}%</b>) : {f_list}</li>"
            )

    ai_data = (res or {}).get("ai_analysis") or (res or {}).get("ai_detection") or {}
    ai_prob = float(ai_data.get("probability_pct", ai_data.get("ai_probability", 0.0)) or 0.0)
    ai_level = str(ai_data.get("level", "") or "")
    if ai_prob >= 30.0 or ai_level.startswith(("Élevé", "Eleve", "Modéré", "Modere")):
        if ai_prob >= 60.0 or ai_level.startswith(("Élevé", "Eleve")):
            is_critical = True
        details = str(ai_data.get("details", "") or "").strip()
        det_html = f" — {details[:240]}" if details and "Aucune signature" not in details else ""
        items.append(
            f"<li><b>🤖 Suspicion d'assistance IA ({ai_prob:.0f}%)</b> : {ai_level}{det_html}</li>"
        )

    act = (res or {}).get("activity_analysis") or {}
    if act.get("has_rich_telemetry"):
        s_auth = act.get("s_auth")
        drops = int(act.get("code_drops_count", act.get("file_drops_count", 0)) or 0)
        if s_auth is not None and float(s_auth) < 50.0:
            is_critical = True
            items.append(
                f"<li><b>📋 Authenticité télémétrique faible (S_auth = {float(s_auth):.1f}%)</b> — forte proportion de code collé ou injecté en bloc.</li>"
            )
        if drops >= 2:
            is_critical = True
            items.append(
                f"<li><b>📥 Importations externes détectées ({drops} fichier(s) de code déposé(s) via glisser-déposer)</b>.</li>"
            )

    if not items:
        return ""

    border = "#dc2626" if is_critical else "#d97706"
    bg = "#fef2f2" if is_critical else "#fffbeb"
    fg = "#991b1b" if is_critical else "#92400e"
    title = (
        "🚨 ALERTE INTÉGRITÉ — VÉRIFICATION ENSEIGNANT REQUISE"
        if is_critical
        else "⚠️ SIGNALEMENT INTÉGRITÉ — À VÉRIFIER"
    )
    return (
        f'<div style="border-left: 5px solid {border}; background-color: {bg}; color: {fg}; '
        f'padding: 12px 16px; border-radius: 6px; margin: 12px 0;">\n'
        f'<strong>{title}</strong>\n'
        f'<ul style="margin: 6px 0 6px 18px; padding: 0;">\n'
        + "\n".join(items)
        + "\n</ul>\n"
        f'<em style="font-size: 0.92em;">Aucun malus automatique n\'a été appliqué sur la note '
        f'(la décision finale appartient à l\'enseignant après vérification humaine).</em>\n'
        f"</div>"
    )


def build_cohort_integrity_red_banner(suspicions, results):
    """
    Génère un bandeau HTML rouge bien visible en tête de la synthèse de session si des
    cas de similarité forte ou de suspicion IA (>= 30%) sont détectés dans la cohorte.
    """
    strong_pairs = [
        s for s in (suspicions or [])
        if any(k in str(s.get("severity", "")).lower() for k in ("forte", "élevée", "elevee", "critique", "haute", "high"))
    ]
    weak_pairs = [s for s in (suspicions or []) if s not in strong_pairs]

    ai_high = []
    ai_mod = []
    for r in (results or []):
        lid = r.get("student_id") or r.get("login") or ""
        ai_d = r.get("ai_analysis") or r.get("ai_detection") or {}
        p = float(ai_d.get("probability_pct", ai_d.get("ai_probability", 0.0)) or 0.0)
        lvl = str(ai_d.get("level", "") or "")
        if p >= 60.0 or lvl.startswith(("Élevé", "Eleve")):
            ai_high.append(f"<code>{lid}</code> ({p:.0f}%)")
        elif p >= 30.0 or lvl.startswith(("Modéré", "Modere")):
            ai_mod.append(f"<code>{lid}</code> ({p:.0f}%)")

    if not strong_pairs and not weak_pairs and not ai_high and not ai_mod:
        return ""

    is_critical = bool(strong_pairs or ai_high)
    border = "#dc2626" if is_critical else "#d97706"
    bg = "#fef2f2" if is_critical else "#fffbeb"
    fg = "#991b1b" if is_critical else "#92400e"
    title = (
        "🚨 ALERTES D'INTÉGRITÉ DÉTECTÉES DANS LA SESSION — VÉRIFICATION REQUISE"
        if is_critical
        else "⚠️ SIGNAUX D'INTÉGRITÉ À VÉRIFIER DANS LA SESSION"
    )
    lines = []
    if strong_pairs:
        lines.append(f"<li><b>🚨 Similarités fortes (plagiat suspecté)</b> : <b>{len(strong_pairs)}</b> paire(s) détectée(s) (voir tableau ci-dessous).</li>")
    if weak_pairs:
        lines.append(f"<li><b>⚠️ Signaux faibles de similarité (à vérifier)</b> : <b>{len(weak_pairs)}</b> paire(s).</li>")
    if ai_high:
        lines.append(f"<li><b>🤖 Forte suspicion d'assistance IA (&gt;= 60%)</b> : {', '.join(ai_high)}.</li>")
    if ai_mod:
        lines.append(f"<li><b>🤖 Suspicion IA modérée (30–59%)</b> : {', '.join(ai_mod)}.</li>")

    return (
        f'<div style="border-left: 5px solid {border}; background-color: {bg}; color: {fg}; '
        f'padding: 12px 16px; border-radius: 6px; margin: 12px 0;">\n'
        f'<strong>{title}</strong>\n'
        f'<ul style="margin: 6px 0 6px 18px; padding: 0;">\n'
        + "\n".join(lines)
        + "\n</ul>\n"
        f'<em style="font-size: 0.92em;">Politique anti-faux-positifs : aucun malus automatique n\'a été retiré des notes '
        f'(vérification manuelle requise avant toute sanction).</em>\n'
        f"</div>"
    )


def format_lines(lines, start_idx, end_idx, highlight_line=None, note=None, comment_prefix="//"):
    """Formate une portion de lignes avec numérotation 'ligne | code' et annotation optionnelle."""
    start = max(0, start_idx)
    end = min(len(lines), end_idx)
    res = []
    for i in range(start, end):
        l_num = i + 1
        line_str = lines[i].rstrip("\r\n")
        if l_num == highlight_line and note:
            res.append(f"{l_num:3d} | {line_str}  {comment_prefix} <-- {note}")
        else:
            res.append(f"{l_num:3d} | {line_str}")
    return "\n".join(res)


def render_template_str(template_str, vars_dict):
    """Remplace les balises {cle} de manière sécurisée sans être affecté par les accolades de code."""
    res = template_str
    for k, v in vars_dict.items():
        res = res.replace(f"{{{k}}}", str(v))
    return res


# ---------------------------------------------------------------------------
# 2. Résolution des Compilateurs / Interpréteurs alignée sur CodeLab
# ---------------------------------------------------------------------------

def resolve_executable(env_vars, candidates, extra_paths=()):
    """
    Résout un exécutable en priorité depuis les variables d'environnement transmises par CodeLab
    (ex: GCC_CMD, HASKELL_CMD, JAVAC_CMD, PYTHON_CMD), puis via PATH, puis chemins connus.
    """
    for var in env_vars:
        val = os.environ.get(var, "").strip()
        if val and val.lower() != "undefined":
            if os.path.isfile(val) and os.access(val, os.X_OK):
                return val
            found = shutil.which(val)
            if found:
                return found

    for cand in candidates:
        found = shutil.which(cand)
        if found:
            return found

    for path_str in extra_paths:
        expanded = os.path.expanduser(path_str)
        if os.path.isfile(expanded) and os.access(expanded, os.X_OK):
            return expanded

    return None


def resolve_c_compiler():
    return resolve_executable(
        env_vars=("CODELAB_GCC_CMD", "GCC_CMD", "CC"),
        candidates=("clang", "gcc"),
        extra_paths=("/usr/bin/clang", "/usr/bin/gcc", "/opt/homebrew/bin/gcc"),
    )


def resolve_haskell_compiler():
    return resolve_executable(
        env_vars=("CODELAB_HASKELL_CMD", "HASKELL_CMD", "GHC_CMD"),
        candidates=("ghc",),
        extra_paths=("/opt/homebrew/bin/ghc", "/usr/local/bin/ghc", "~/.ghcup/bin/ghc"),
    )


def resolve_python_interpreter():
    return resolve_executable(
        env_vars=("CODELAB_PYTHON_CMD", "PYTHON_CMD"),
        candidates=(sys.executable, "python3", "python"),
    ) or sys.executable


# ---------------------------------------------------------------------------
# 3. Support Universel des Carnets Jupyter (.ipynb)
# ---------------------------------------------------------------------------

def detect_notebook_language(ipynb_path):
    """Détecte le langage d'un carnet .ipynb : 'haskell', 'c', ou 'python' (défaut)."""
    try:
        nb = json.loads(Path(ipynb_path).read_text(encoding="utf-8", errors="ignore"))
        meta = nb.get("metadata", {})
        ks = meta.get("kernelspec", {})
        li = meta.get("language_info", {})
        klang = str(ks.get("language", "") or ks.get("name", "")).lower()
        llang = str(li.get("name", "")).lower()
        if "haskell" in klang or "haskell" in llang or "ihaskell" in klang:
            return "haskell"
        if klang in ("c", "xcpp", "c_kernel") or llang == "c":
            return "c"
        if "python" in klang or "python" in llang:
            return "python"
        langs = {
            str(c.get("metadata", {}).get("language", "")).lower()
            for c in nb.get("cells", [])
            if c.get("cell_type") == "code"
        }
        if "haskell" in langs:
            return "haskell"
        if "c" in langs:
            return "c"
    except Exception:
        pass
    return "python"


def extract_notebook_code(ipynb_path, lang="python"):
    """Extrait le code source des cellules d'un carnet .ipynb selon son langage."""
    try:
        nb = json.loads(Path(ipynb_path).read_text(encoding="utf-8", errors="ignore"))
        chunks = []
        comment = "//" if lang == "c" else ("--" if lang == "haskell" else "#")
        for idx, cell in enumerate(nb.get("cells", []), 1):
            if cell.get("cell_type") != "code":
                continue
            src = cell.get("source", "")
            if isinstance(src, list):
                src = "".join(src)
            lines = []
            for ln in src.splitlines():
                stripped = ln.lstrip()
                if lang == "python" and stripped.startswith(("%", "!")):
                    lines.append(f"# [magic] {ln}")
                elif lang == "haskell" and stripped.startswith(":"):
                    lines.append(f"-- [GHCi] {ln}")
                else:
                    lines.append(ln)
            txt = "\n".join(lines).strip()
            if txt:
                chunks.append(f"{comment} Cellule {idx}\n{txt}")
        return "\n\n".join(chunks)
    except Exception:
        return ""


# ---------------------------------------------------------------------------
# 4. Détection de Similarités / Plagiat Inter-Étudiants (Jaccard n-grams)
# ---------------------------------------------------------------------------

def get_ngrams(tokens, n=4):
    if len(tokens) < n:
        return set()
    return set(tuple(tokens[i:i + n]) for i in range(len(tokens) - n + 1))


def jaccard_similarity(set1, set2):
    if not set1 or not set2:
        return 0.0
    inter = len(set1 & set2)
    union = len(set1 | set2)
    return inter / float(union) if union > 0 else 0.0


def get_canonical_boilerplate_keys(file_pair_hits, file_submitters, file_median_tokens=None, min_cohort_size=6, max_student_ratio=0.35, min_student_cluster=5, max_boilerplate_tokens=120):
    """
    Identifie les fichiers canoniques / exercices guidés (boilerplate) où un grand nombre
    d'étudiants produisent naturellement la même solution (évite les faux positifs massifs),
    tout en empêchant le blanchiment d'une triche collective sur des fichiers longs (> 120 tokens,
    sauf si >= 50% de la cohorte entière est identique).
    """
    boilerplate = set()
    for ckey, matched_students in file_pair_hits.items():
        n_sub = int(file_submitters.get(ckey, 0))
        if n_sub < min_cohort_size:
            continue
        med_toks = (file_median_tokens or {}).get(ckey)
        ratio = max_student_ratio if (med_toks is None or med_toks <= max_boilerplate_tokens) else 0.50
        limit = max(min_student_cluster, int((n_sub * ratio) + 0.999))
        if len(matched_students) >= limit:
            boilerplate.add(ckey)
    return boilerplate


def detect_cohort_plagiarism_generic(cohort_dir, collect_sources_fn, tokenize_fn, canonical_key_fn, threshold=75.0, min_chars=80, min_tokens=55, exclude_students=None):
    """
    Moteur générique de détection de plagiat inter-étudiants par n-grams Jaccard.
    collect_sources_fn(student_dir) -> liste de tuples (display_name, rel_path, content_str).
    """
    student_files = {}
    p = Path(cohort_dir).resolve()
    if not p.is_dir():
        return []

    ignored_dirs = {"evaluations", "__pycache__", ".hidden", "codelab", "recovery", "dist-newstyle", ".venv", "env"}
    excluded = set(exclude_students or ())
    students = [
        d.name for d in p.iterdir()
        if d.is_dir() and not d.name.startswith(".") and d.name not in ignored_dirs and d.name not in excluded
    ]

    for s in students:
        s_dir = p / s
        student_files[s] = {}
        for display_name, rel_path, content in collect_sources_fn(s_dir):
            ckey = canonical_key_fn(display_name)
            if not ckey or len(content.strip()) < min_chars:
                continue
            toks = tokenize_fn(content)
            if len(toks) >= min_tokens:
                ngrams = get_ngrams(toks, n=4)
                if ngrams:
                    prev = student_files[s].get(ckey)
                    if not prev or len(toks) > prev[2]:
                        student_files[s][ckey] = (rel_path, ngrams, len(toks), display_name)

    suspicions = []
    for s1, s2 in combinations(sorted(students), 2):
        f1 = student_files.get(s1, {})
        f2 = student_files.get(s2, {})
        if not f1 or not f2:
            continue
        similar_files = []
        max_sim = 0.0
        for ckey in set(f1.keys()) & set(f2.keys()):
            sim = round(jaccard_similarity(f1[ckey][1], f2[ckey][1]) * 100.0, 1)
            if sim >= threshold:
                similar_files.append((f1[ckey][3], sim))
                if sim > max_sim:
                    max_sim = sim
        if similar_files:
            sev = "Critique (Copie quasi-identique)" if max_sim >= 92.0 else ("Élevée" if max_sim >= 82.0 else "Modérée")
            suspicions.append({
                "s1": s1,
                "s2": s2,
                "similar_files": sorted(similar_files, key=lambda x: -x[1]),
                "max_sim": max_sim,
                "severity": sev,
            })

    suspicions.sort(key=lambda x: -x["max_sim"])
    return suspicions


# ---------------------------------------------------------------------------
# 5. Banc de Tests Fonctionnels Déclaratif Optionnel (tests.json)
# ---------------------------------------------------------------------------

def _find_target_file(student_dir, requested_name, extensions):
    """Localise un fichier cible dans le dossier étudiant (nom exact ou premier fichier correspondant)."""
    s_dir = Path(student_dir)
    if requested_name:
        exact = s_dir / requested_name
        if exact.is_file():
            return exact
        for f in s_dir.rglob(requested_name):
            if f.is_file():
                return f
        return None
    for ext in extensions:
        matches = sorted(f for f in s_dir.rglob(f"*{ext}") if not f.name.startswith("."))
        if matches:
            return matches[0]
    return None


def run_declarative_tests(skill_dir, student_dir, lang):
    """
    Exécute un banc de tests déclaratif optionnel défini dans <skill_dir>/tests.json.

    Format attendu de tests.json :
    {
      "timeout": 3,
      "tests": [
        {
          "name": "Test factorielle 5",
          "file": "algo.py",           // optionnel : premier fichier du langage sinon
          "stdin": "5\\n",              // mode E/S standard
          "expected_stdout": "120",    // sortie attendue (strip)
          "expr": "fact(5)",           // mode expression (Python / Haskell)
          "expected": "120"            // résultat attendu de expr (strip)
        }
      ]
    }

    Retourne {"active": False} si aucun tests.json n'est présent.
    """
    tests_file = Path(skill_dir) / "tests.json"
    if not tests_file.is_file():
        return {"active": False, "passed": 0, "total": 0, "ratio": 0.0, "details": []}

    try:
        spec = json.loads(tests_file.read_text(encoding="utf-8"))
    except Exception as e:
        return {"active": True, "passed": 0, "total": 0, "ratio": 0.0, "details": [], "error": f"tests.json invalide : {e}"}

    cases = spec.get("tests", [])
    if not cases:
        return {"active": False, "passed": 0, "total": 0, "ratio": 0.0, "details": []}

    default_timeout = float(spec.get("timeout", 3.0))
    details = []
    passed = 0

    with tempfile.TemporaryDirectory(prefix="codelab_tests_") as tmpdir:
        tmp_p = Path(tmpdir)
        for idx, tc in enumerate(cases, 1):
            t_name = tc.get("name", f"Test {idx}")
            t_file_req = tc.get("file")
            timeout = float(tc.get("timeout", default_timeout))
            stdin_data = tc.get("stdin", "")
            expr = tc.get("expr")
            expected = tc.get("expected_stdout", tc.get("expected"))
            expected_str = str(expected).strip() if expected is not None else ""

            ok = False
            actual = ""
            err_msg = ""

            try:
                if lang == "python":
                    target = _find_target_file(student_dir, t_file_req, (".py", ".ipynb"))
                    if not target:
                        err_msg = f"Fichier introuvable ({t_file_req or '*.py'})"
                    else:
                        py_bin = resolve_python_interpreter()
                        if target.suffix.lower() == ".ipynb":
                            src_path = tmp_p / f"nb_{idx}.py"
                            src_path.write_text(extract_notebook_code(target, "python"), encoding="utf-8")
                        else:
                            src_path = target

                        if expr:
                            runner = (
                                "import importlib.util, sys\n"
                                f"spec = importlib.util.spec_from_file_location('student_mod', {str(src_path)!r})\n"
                                "mod = importlib.util.module_from_spec(spec)\n"
                                "spec.loader.exec_module(mod)\n"
                                f"print(eval({expr!r}, mod.__dict__))\n"
                            )
                            res = subprocess.run([py_bin, "-c", runner], input=stdin_data, capture_output=True, text=True, timeout=timeout)
                        else:
                            res = subprocess.run([py_bin, str(src_path)], input=stdin_data, capture_output=True, text=True, timeout=timeout)
                        actual = (res.stdout or "").strip()
                        if res.returncode == 0 and actual == expected_str:
                            ok = True
                        elif res.returncode != 0:
                            err_msg = (res.stderr or "").strip().splitlines()[-1] if res.stderr else f"Code retour {res.returncode}"

                elif lang == "c":
                    target = _find_target_file(student_dir, t_file_req, (".c", ".ipynb"))
                    cc = resolve_c_compiler()
                    if not target:
                        err_msg = f"Fichier C introuvable ({t_file_req or '*.c'})"
                    elif not cc:
                        err_msg = "Compilateur C indisponible"
                    else:
                        if target.suffix.lower() == ".ipynb":
                            src_path = tmp_p / f"nb_c_{idx}.c"
                            src_path.write_text(extract_notebook_code(target, "c"), encoding="utf-8")
                        else:
                            src_path = target
                        exe = tmp_p / f"test_c_{idx}"
                        comp = subprocess.run([cc, str(src_path), "-o", str(exe), "-lm"], capture_output=True, text=True, timeout=10)
                        if comp.returncode != 0:
                            err_msg = "Échec compilation C"
                        else:
                            res = subprocess.run([str(exe)], input=stdin_data, capture_output=True, text=True, timeout=timeout)
                            actual = (res.stdout or "").strip()
                            if res.returncode == 0 and actual == expected_str:
                                ok = True
                            elif res.returncode != 0:
                                err_msg = f"Code retour {res.returncode}"

                elif lang == "haskell":
                    target = _find_target_file(student_dir, t_file_req, (".hs", ".ipynb"))
                    ghc = resolve_haskell_compiler()
                    if not target:
                        err_msg = f"Fichier Haskell introuvable ({t_file_req or '*.hs'})"
                    elif not ghc:
                        err_msg = "Compilateur GHC indisponible"
                    else:
                        if target.suffix.lower() == ".ipynb":
                            src_path = tmp_p / f"NbTest_{idx}.hs"
                            src_path.write_text(extract_notebook_code(target, "haskell"), encoding="utf-8")
                        else:
                            src_path = target

                        if expr:
                            res = subprocess.run([ghc, "-e", expr, str(src_path)], input=stdin_data, capture_output=True, text=True, timeout=max(timeout, 8.0))
                        else:
                            runghc = shutil.which("runghc") or os.path.join(os.path.dirname(ghc), "runghc")
                            res = subprocess.run([runghc, str(src_path)], input=stdin_data, capture_output=True, text=True, timeout=max(timeout, 8.0))
                        actual = (res.stdout or "").strip()
                        if res.returncode == 0 and actual == expected_str:
                            ok = True
                        elif res.returncode != 0:
                            err_msg = (res.stderr or "").strip().splitlines()[-1] if res.stderr else f"Code retour {res.returncode}"

                elif lang == "java":
                    target = _find_target_file(student_dir, t_file_req, (".java",))
                    javac = resolve_executable(("JAVAC_CMD",), ("javac",))
                    java_bin = resolve_executable(("JAVA_CMD",), ("java",))
                    if not target:
                        err_msg = f"Fichier Java introuvable ({t_file_req or '*.java'})"
                    elif not javac or not java_bin:
                        err_msg = "JDK (javac/java) indisponible"
                    else:
                        out_dir = tmp_p / f"java_{idx}"
                        out_dir.mkdir(exist_ok=True)
                        comp = subprocess.run([javac, "-d", str(out_dir), str(target)], capture_output=True, text=True, timeout=10)
                        if comp.returncode != 0:
                            err_msg = "Échec compilation Java"
                        else:
                            main_cls = tc.get("main_class", target.stem)
                            res = subprocess.run([java_bin, "-cp", str(out_dir), main_cls], input=stdin_data, capture_output=True, text=True, timeout=timeout)
                            actual = (res.stdout or "").strip()
                            if res.returncode == 0 and actual == expected_str:
                                ok = True
                            elif res.returncode != 0:
                                err_msg = (res.stderr or "").strip().splitlines()[-1] if res.stderr else f"Code retour {res.returncode}"

            except subprocess.TimeoutExpired:
                err_msg = f"Timeout (> {timeout:.0f}s)"
            except Exception as e:
                err_msg = str(e)

            if ok:
                passed += 1
            details.append({
                "name": t_name,
                "passed": ok,
                "expected": expected_str,
                "actual": actual,
                "error": err_msg,
            })

    total = len(cases)
    return {
        "active": True,
        "passed": passed,
        "total": total,
        "ratio": round(passed / float(total), 3) if total > 0 else 0.0,
        "details": details,
    }

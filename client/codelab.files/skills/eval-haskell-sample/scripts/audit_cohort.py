#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_cohort.py - Audit d'ensemble d'une cohorte, détection de plagiat et d'assistance IA (Exemple Haskell).
"""

import os
import sys
import re
import csv
from pathlib import Path
from itertools import combinations
from audit_student import (
    audit_student,
    strip_haskell_comments,
    is_haskell_notebook,
    extract_haskell_from_notebook,
)

SKILL_DIR = Path(__file__).resolve().parent.parent
SKILLS_ROOT = SKILL_DIR.parent
if str(SKILLS_ROOT) not in sys.path:
    sys.path.insert(0, str(SKILLS_ROOT))
from skills_common import load_weights, compute_similarity_malus, apply_combined_penalties, get_canonical_boilerplate_keys


def tokenize_haskell_code(content):
    """Normalise le code Haskell : élimine commentaires et chaînes, canonise les identifiants."""
    content = strip_haskell_comments(content)
    hs_keywords = {
        "module", "import", "qualified", "as", "hiding", "data", "newtype", "type",
        "class", "instance", "deriving", "where", "let", "in", "if", "then", "else",
        "case", "of", "do", "True", "False", "Just", "Nothing", "Left", "Right",
    }
    raw_toks = re.findall(r"[a-zA-Z_][a-zA-Z0-9_']*|\d+|::|->|=>|<-|==|/=|<=|>=|\+\+|[+\-*/$:.|()\[\]{},;=]", content)
    tokens = []
    for tok in raw_toks:
        if tok in hs_keywords or not tok[0].isalpha():
            tokens.append(tok)
        else:
            tokens.append("ID")
    return tokens


def canonical_hs_file_key(fname):
    """Canonise le nom d'un fichier Haskell (.hs / .ipynb) et ignore les sauvegardes."""
    low = fname.lower()
    if low.startswith(".") or low.endswith(".bak") or low.endswith("_bak.hs") or "copy" in low:
        return None
    stem, ext = os.path.splitext(low)
    clean = re.sub(r"[_\s-]+", "_", stem).strip("_")
    return f"{clean}{ext}" if clean else low


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


def detect_cohort_plagiarism(cohort_dir, threshold=75.0, exclude_students=None, users_db=None):
    """Détecte les similarités de code anormales entre étudiants sur les sources Haskell."""
    student_files = {}
    p = Path(cohort_dir).resolve()
    excluded = set(exclude_students or ())
    students = [
        d.name for d in p.iterdir()
        if d.is_dir() and not d.name.startswith(".")
        and d.name not in ("evaluations", "__pycache__", ".hidden", "codelab", "recovery", "dist-newstyle")
        and d.name not in excluded
    ]

    for s in students:
        s_dir = p / s
        student_files[s] = {}
        candidates = list(s_dir.glob("**/*.hs")) + [nb for nb in s_dir.glob("**/*.ipynb") if is_haskell_notebook(nb)]
        for f in candidates:
            rel_parts = f.relative_to(s_dir).parts
            if any(part.startswith(".") or part in ("evaluations", "__pycache__", ".hidden", "codelab", "recovery", "dist-newstyle") for part in rel_parts):
                continue
            ckey = canonical_hs_file_key(f.name)
            if not ckey:
                continue
            rel = str(f.relative_to(s_dir))
            try:
                content = extract_haskell_from_notebook(f) if f.suffix.lower() == ".ipynb" else f.read_text(encoding="utf-8", errors="ignore")
                if len(content.strip()) < 80:
                    continue
                toks = tokenize_haskell_code(content)
                if len(toks) >= 40:
                    ngrams = get_ngrams(toks, n=4)
                    if ngrams:
                        prev = student_files[s].get(ckey)
                        if not prev or len(toks) > prev[2]:
                            student_files[s][ckey] = (rel, ngrams, len(toks), f.name)
            except Exception:
                pass

    file_submitters = {}
    file_tok_lists = {}
    for fdict in student_files.values():
        for ckey, info in fdict.items():
            file_submitters[ckey] = file_submitters.get(ckey, 0) + 1
            file_tok_lists.setdefault(ckey, []).append(info[2])
    file_median_tokens = {k: sorted(v)[len(v) // 2] for k, v in file_tok_lists.items()}

    raw_pair_matches = []
    file_pair_hits = {}
    for s1, s2 in combinations(sorted(students), 2):
        f1 = student_files.get(s1, {})
        f2 = student_files.get(s2, {})
        if not f1 or not f2:
            continue
        common = set(f1.keys()) & set(f2.keys())
        matched_entries = []
        for ckey in common:
            sim = round(jaccard_similarity(f1[ckey][1], f2[ckey][1]) * 100.0, 1)
            if sim >= threshold:
                min_toks = min(f1[ckey][2], f2[ckey][2])
                matched_entries.append((ckey, f1[ckey][3], sim, min_toks))
                file_pair_hits.setdefault(ckey, set()).update((s1, s2))
        if matched_entries:
            raw_pair_matches.append((s1, s2, len(common), matched_entries))

    boilerplate_keys = get_canonical_boilerplate_keys(file_pair_hits, file_submitters, file_median_tokens=file_median_tokens)
    suspicions = []
    for s1, s2, common_cnt, matched_entries in raw_pair_matches:
        kept = [(dname, sim, min_toks) for ckey, dname, sim, min_toks in matched_entries if ckey not in boilerplate_keys]
        if kept:
            similar_files = [(dname, sim) for dname, sim, _ in kept]
            max_sim = max(sim for _, sim, _ in kept)
            has_substantial_92 = any(sim >= 92.0 and (toks >= 120 or common_cnt <= 2) for _, sim, toks in kept)
            sev = "Forte suspicion" if (len(similar_files) >= 2 or has_substantial_92) else "À vérifier (signal faible)"
            suspicions.append({
                "s1": s1,
                "s2": s2,
                "similar_files": sorted(similar_files, key=lambda x: -x[1]),
                "max_sim": max_sim,
                "severity": sev,
            })

    suspicions.sort(key=lambda x: -x["max_sim"])
    return suspicions


def apply_similarity_penalties(results, suspicions):
    """Applique les malus de similarité (Forte suspicion uniquement) sur la note avec malus (total)."""
    weights = load_weights(SKILL_DIR)
    sim_by_student = {}
    for susp in suspicions:
        if "forte" not in str(susp.get("severity", "")).lower():
            continue
        for sid in (susp["s1"], susp["s2"]):
            info = sim_by_student.setdefault(sid, {"files": set(), "max_sim": 0.0})
            for fname, sim_pct in susp.get("similar_files", []):
                info["files"].add(fname)
                if sim_pct > info["max_sim"]:
                    info["max_sim"] = sim_pct

    for r in results:
        sc = r.get("scores", {})
        if "note_technique_brute" not in sc:
            sc["note_technique_brute"] = round(
                sc.get("compilation", 0.0) + sc.get("architecture", 0.0) + sc.get("style", 0.0) + sc.get("tests", 0.0), 1
            )
        if "note_apres_ia" not in sc:
            sc["note_apres_ia"] = sc.get("total", sc["note_technique_brute"])
        sid = r.get("student_id", "")
        if sid not in sim_by_student:
            sc["malus_similarite"] = 0.0
            sc["total"] = sc["note_apres_ia"]
            continue
        plag_info = {
            "files": list(sim_by_student[sid]["files"]),
            "max_sim": sim_by_student[sid]["max_sim"],
        }
        ai_prob = float((r.get("ai_analysis") or r.get("ai_detection") or {}).get("probability_pct", 0.0))
        tot, malus = apply_combined_penalties(sc["note_technique_brute"], ai_prob, plag_info, weights)
        sc["malus_similarite"] = malus
        sc["total"] = tot


def ensure_results_radars(results):
    """S'assure que chaque résultat dispose de ses analyses radar et détection de double-profil."""
    try:
        if str(SKILLS_ROOT) not in sys.path:
            sys.path.insert(0, str(SKILLS_ROOT))
        from activity_analyzer import generate_mermaid_radars
        for r in results:
            if "radars" not in r or not r["radars"]:
                s_name = r.get("student_name") or r.get("name") or r.get("student_id") or "Étudiant"
                r["radars"] = generate_mermaid_radars(r, s_name)
    except Exception:
        pass


def analyze_cohort(cohort_dir, exclude_students=None):
    p = Path(cohort_dir).resolve()
    excluded = set(exclude_students or ())
    results = []
    if not p.is_dir():
        return results
    for d in sorted(p.iterdir()):
        if d.is_dir() and not d.name.startswith(".") and d.name not in ("evaluations", "__pycache__", ".hidden", "codelab", "recovery", "dist-newstyle") and d.name not in excluded:
            hs_files = list(d.rglob("*.hs"))
            hs_nbs = [nb for nb in d.rglob("*.ipynb") if is_haskell_notebook(nb)]
            if hs_files or hs_nbs:
                res = audit_student(str(d))
                results.append(res)
    suspicions = detect_cohort_plagiarism(cohort_dir, exclude_students=excluded)
    apply_similarity_penalties(results, suspicions)
    return results


def export_csv(results, out_path, cohort_dir=None, exclude_students=None):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    result_ids = {r.get("student_id", "") for r in results}
    suspicions = detect_cohort_plagiarism(cohort_dir, exclude_students=exclude_students) if cohort_dir else []
    suspicions = [s for s in suspicions if s["s1"] in result_ids and s["s2"] in result_ids]
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    ensure_results_radars(results)
    sim_map = {}
    for susp in suspicions:
        sim_map.setdefault(susp["s1"], []).append(f"{susp['s2']} ({susp['max_sim']}%)")
        sim_map.setdefault(susp["s2"], []).append(f"{susp['s1']} ({susp['max_sim']}%)")

    with open(out_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=";")
        writer.writerow([
            "login", "fichiers_hs", "compilation", "architecture", "style", "tests",
            "note_sans_malus", "note_finale", "profil_didactique", "indice_ia",
            "alerte_similarite", "s_auth", "i_nav", "compilations", "executions", "alertes_burst"
        ])
        for r in results:
            sc = r.get("scores", {})
            note_sans_malus = sc.get("note_technique_brute", round(sc.get("compilation", 0) + sc.get("architecture", 0) + sc.get("style", 0) + sc.get("tests", 0), 1))
            ai_data = r.get("ai_analysis", {})
            ai_str = ai_data.get("level", "Faible (0% - Code authentique)")
            login = r.get("student_id", "")
            sim_str = " | ".join(sim_map.get(login, [])) if login in sim_map else "Aucune"
            act = r.get("activity_analysis") or {}
            has_rich = bool(act.get("has_rich_telemetry"))
            s_auth_val = act.get("s_auth")
            s_auth_col = f"{s_auth_val}%" if (has_rich and s_auth_val is not None) else ""
            i_nav_col = f"{act.get('i_nav', '')}%" if has_rich else ""
            comp_col = act.get("compilations", {}).get("total", "") if has_rich else ""
            exec_col = act.get("executions", {}).get("total", "") if has_rich else ""
            burst_col = (act.get("burst_insert_count", 0) + act.get("burst_new_file_count", 0)) if has_rich else ""
            rad = r.get("radars") or {}
            prof_col = rad.get("profil_display", rad.get("profil_dominant", ""))
            writer.writerow([
                login,
                len(r.get("hs_files", [])),
                sc.get("compilation", 0.0),
                sc.get("architecture", 0.0),
                sc.get("style", 0.0),
                sc.get("tests", 0.0),
                note_sans_malus,
                sc.get("total", 0.0),
                prof_col,
                ai_str,
                sim_str,
                s_auth_col,
                i_nav_col,
                comp_col,
                exec_col,
                burst_col,
            ])


def build_cohort_clusters(results, suspicious_logins):
    """Classe les étudiants en typologies/clusters pédagogiques exclusifs et génère le radar de cohorte."""
    ensure_results_radars(results)
    total_st = len(results)

    clusters = [
        {"id": "boss", "label": "👑 **Les Boss**", "sub": "Excellence & Autonomie", "desc": "Note >= 14. Démarche personnelle complète, code fonctionnel propre et rigoureux.", "members": []},
        {"id": "artisans", "label": "🔨 **Les Artisans**", "sub": "Démarche d'Apprentissage & Régularité", "desc": "Note entre 10 et 13.9. Socle fonctionnel acquis, travail appliqué et régulier.", "members": []},
        {"id": "casse_cou", "label": "⚡ **Les Casse-cou**", "sub": "Acharnement & Tâtonnement / Raccourcis", "desc": "Acharnement compilatoire ou code précipité sans structuration fonctionnelle.", "members": []},
        {"id": "decrocheurs", "label": "🚪 **Les Décrocheurs**", "sub": "Perte de Pied & Décrochage", "desc": "Note < 10. Travaux partiels, blocages initiaux ou abandons précoces.", "members": []},
        {"id": "pirates", "label": "🏴‍☠️ **Les Pirates**", "sub": "Collusion & Copier-Coller", "desc": "Similarité directe (>= 75%) sur du code libre ou import de fichiers externes.", "members": []},
        {"id": "cyborgs", "label": "🤖 **Les Cyborgs**", "sub": "Recours IA Extérieur & Rupture Stylométrique", "desc": "Forte présomption de code généré par IA ou ruptures stylistiques massives.", "members": []},
    ]
    c_map = {c["id"]: c for c in clusters}

    for r in results:
        lid = r.get("student_id") or r.get("login", "")
        name = r.get("student_name") or r.get("name", "")
        note = float(r.get("scores", {}).get("total", 0.0))
        ai_data = r.get("ai_analysis") or r.get("ai_detection") or {}
        ai_level = str(ai_data.get("level", ""))
        ai_prob = float(ai_data.get("probability_pct", 0.0))
        is_ai = ai_level.startswith(("Élevé", "Modéré")) or ai_prob >= 30.0
        is_susp = lid in suspicious_logins
        member_entry = f"`{lid}` ({name})" if (name and name != lid) else f"`{lid}`"

        rad = r.get("radars") or {}
        prim_id = rad.get("primary_profile_id")

        if is_susp:
            c_map["pirates"]["members"].append(member_entry)
        elif prim_id == "boss":
            c_map["boss"]["members"].append(member_entry)
        elif prim_id == "artisan":
            c_map["artisans"]["members"].append(member_entry)
        elif prim_id == "cyborg":
            c_map["cyborgs"]["members"].append(member_entry)
        elif prim_id == "casse_cou":
            c_map["casse_cou"]["members"].append(member_entry)
        elif prim_id == "decrocheur":
            c_map["decrocheurs"]["members"].append(member_entry)
        elif prim_id == "pirate":
            c_map["pirates"]["members"].append(member_entry)
        elif is_ai:
            c_map["cyborgs"]["members"].append(member_entry)
        elif note < 10.0:
            c_map["decrocheurs"]["members"].append(member_entry)
        elif note >= 14.0:
            c_map["boss"]["members"].append(member_entry)
        else:
            c_map["artisans"]["members"].append(member_entry)

    lines = [
        "## Typologies & Répartition par Clusters d'Étudiants Types\n",
        "| Cluster Typologique | Profil Pédagogique | Effectif | Proportion | Caractéristiques & Recommandations | Étudiants Associés |",
        "| :--- | :--- | :---: | :---: | :--- | :--- |",
    ]
    for c in clusters:
        cnt = len(c["members"])
        pct = (cnt / total_st * 100.0) if total_st > 0 else 0.0
        m_str = ", ".join(c["members"]) if c["members"] else "—"
        lines.append(f"| {c['label']} | {c['sub']} | {cnt} | {pct:.1f}% | {c['desc']} | {m_str} |")
    lines.append("")

    try:
        from activity_analyzer import generate_cohort_radar
        lines.append(generate_cohort_radar(clusters, total_st))
        lines.append("")
    except Exception:
        pass

    double_profils = [r for r in results if (r.get("radars") or {}).get("is_double_profil")]
    if double_profils:
        lines.append("## 🧬 Analyse des Profils Hybrides & Doubles-Profils Remarquables\n")
        lines.append("> 💡 **Interprétation des Doubles-Profils** : Un double-profil est automatiquement identifié lorsque deux dimensions issues d'axes orthogonaux présentent conjointement une affinité élevée (scores >= 11.0 / 20 et écart <= 3.5 pts).\n")
        lines.append("| Étudiant | Login | Double-Profil Détecté | Surnom Pédagogique | Diagnostic Didactique & Posture Conseillée |")
        lines.append("| :--- | :---: | :--- | :--- | :--- |")
        for dp in double_profils:
            r_rad = dp["radars"]
            nom = dp.get("student_name") or dp.get("name") or dp.get("student_id") or "Étudiant"
            lid = dp.get("student_id") or dp.get("login") or ""
            lbl = r_rad.get("double_profil_label", r_rad.get("double_profil_nom", ""))
            surnom = f"**{r_rad.get('double_profil_surnom', '')}**"
            desc = r_rad.get("double_profil_desc", "")
            conseil = r_rad.get("double_profil_conseil", "")
            lines.append(f"| **{nom}** | `{lid}` | {lbl} | {surnom} | {desc}<br>👉 *Conseil : {conseil}* |")
        lines.append("")

    return "\n".join(lines)


def resolve_cohort_name(cohort_dir=None, output_md=None, session_id=None):
    GENERIC_NAMES = {".", "dist", "distant", "local", "programs", "evaluations", "synthese", ""}
    if session_id and str(session_id).strip() not in GENERIC_NAMES:
        return str(session_id).strip()
    if output_md:
        cand = Path(output_md).stem.replace("synthese_", "").strip()
        if cand and cand not in GENERIC_NAMES:
            return cand
    if cohort_dir:
        base = os.path.basename(str(cohort_dir).rstrip("/\\")).strip()
        if base and base not in GENERIC_NAMES:
            return base
    return "Cohorte Haskell"


def export_summary_markdown(results, out_path, cohort_dir=None, use_ai=None, session_id=None, exclude_students=None, **kwargs):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    result_ids = {r.get("student_id", "") for r in results}
    cohort_name = resolve_cohort_name(cohort_dir, out_path, session_id or kwargs.get("session"))
    suspicions = detect_cohort_plagiarism(cohort_dir, exclude_students=exclude_students) if cohort_dir else []
    suspicions = [s for s in suspicions if s["s1"] in result_ids and s["s2"] in result_ids]
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    ensure_results_radars(results)
    suspicious_logins = set()
    strong_suspicious_logins = set()
    for sp in suspicions:
        suspicious_logins.add(sp["s1"])
        suspicious_logins.add(sp["s2"])
        if "forte" in str(sp.get("severity", "")).lower():
            strong_suspicious_logins.add(sp["s1"])
            strong_suspicious_logins.add(sp["s2"])

    has_ia = bool(use_ai) if use_ai is not None else any("gemini" in str(r.get("eval_engine", "")).lower() for r in results)
    ia_status = "Active (Gemini)" if has_ia else "Inactive"

    notes = [float(r.get("scores", {}).get("total", 0.0)) for r in results]
    moyenne = (sum(notes) / len(notes)) if notes else 0.0

    md = [
        f"# Synthèse de la Cohorte : {cohort_name}",
        f"**Effectif audité** : {len(results)} étudiants | **Moyenne** : **{moyenne:.2f}/20**",
        "",
        f"**Algorithme d'évaluation** : `eval-haskell-sample` | Analyse IA : {ia_status} | Barème : 6 (Compilation GHC) + 8 (Conception Fonctionnelle) + 4 (Style) + 2 (Tests) = 20 pts",
        "",
        "---",
        "",
    ]

    from skills_common import build_cohort_integrity_red_banner
    red_banner = build_cohort_integrity_red_banner(suspicions, results)
    if red_banner:
        md.append(red_banner)
        md.append("")

    if suspicions:
        md.append("## Analyse de Similarités & Copier-Collé Inter-Étudiants")
        md.append(f"> **{len(suspicions)} paire(s) d'étudiants présentent une similarité suspecte** :\n")
        md.append("| Étudiant 1 | Étudiant 2 | Fichiers Suspects | Similarité Max | Gravité |")
        md.append("| :--- | :--- | :--- | :---: | :--- |")
        for susp in suspicions:
            f_str = ", ".join(f"`{f}` ({sim}%)" for f, sim in susp["similar_files"])
            sev_txt = susp["severity"]
            sev_badge = f'<span style="color:#dc2626; font-weight:bold;">🚨 {sev_txt}</span>' if "forte" in sev_txt.lower() else f'<span style="color:#d97706; font-weight:bold;">⚠️ {sev_txt}</span>'
            md.append(f"| `{susp['s1']}` | `{susp['s2']}` | {f_str} | **{susp['max_sim']}%** | {sev_badge} |")
        md.extend(["", "---", ""])

    from skills_common import compute_indicative_malus, format_indicative_malus_badge
    weights_s = load_weights(SKILL_DIR)
    strong_plag_by_id = {}
    for susp in suspicions:
        if "forte" in str(susp.get("severity", "")).lower():
            for sid in (susp["s1"], susp["s2"]):
                info = strong_plag_by_id.setdefault(sid, {"files": [], "max_sim": 0.0, "severity": susp["severity"]})
                info["files"].extend(susp.get("similar_files", []))
                if susp["max_sim"] > info["max_sim"]:
                    info["max_sim"] = susp["max_sim"]

    md.append("| Étudiant / Dossier | Compilation (/6) | Conception (/8) | Style (/4) | Tests (/2) | Note brute (/20) | Malus | Profil Didactique | Indice IA | Alertes |")
    md.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: | :--- |")
    for r in results:
        sc = r.get("scores", {})
        note_sans_malus = sc.get("note_technique_brute", round(sc.get("compilation", 0) + sc.get("architecture", 0) + sc.get("style", 0) + sc.get("tests", 0), 1))
        login = r.get("student_id", "")
        rad = r.get("radars") or {}
        prof_col = rad.get("profil_display", rad.get("profil_dominant", "—"))
        alerts = []
        if login in strong_suspicious_logins:
            alerts.append('<span style="color:#dc2626; font-weight:bold;">🚨 Similarité forte</span>')
        elif login in suspicious_logins:
            alerts.append('<span style="color:#d97706; font-weight:bold;">⚠️ À vérifier</span>')
        ai_data = r.get("ai_analysis") or {}
        ai_prob = float(ai_data.get("probability_pct", 0.0) or 0.0)
        ai_level = ai_data.get("level", "Faible")
        if ai_level.startswith("Élevé"):
            alerts.append('<span style="color:#dc2626; font-weight:bold;">🤖 Alerte IA</span>')
        elif ai_level.startswith("Modéré"):
            alerts.append('<span style="color:#d97706; font-weight:bold;">🤖 Idiomes avancés</span>')
        alert_str = " | ".join(alerts) if alerts else "Aucune"
        malus_ind = compute_indicative_malus(ai_prob, strong_plag_by_id.get(login), weights_s)
        malus_col = format_indicative_malus_badge(malus_ind)
        md.append(
            f"| `{login}` | {sc.get('compilation', 0):.1f} | {sc.get('architecture', 0):.1f} | "
            f"{sc.get('style', 0):.1f} | {sc.get('tests', 0):.1f} | **{note_sans_malus:.1f}** | "
            f"{malus_col} | {prof_col} | {ai_level} | {alert_str} |"
        )
    md.append("")
    md.append(build_cohort_clusters(results, strong_suspicious_logins))

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md))


def audit_cohort(cohort_dir, output_csv=None, output_md=None, **kwargs):
    if output_csv is None:
        output_csv = kwargs.get("csv_output")
    if output_md is None:
        output_md = kwargs.get("md_output")
    results = analyze_cohort(cohort_dir)
    if output_csv:
        export_csv(results, output_csv, cohort_dir)
    if output_md:
        export_summary_markdown(results, output_md, cohort_dir)
    return results


def main():
    if len(sys.argv) < 2:
        print("Usage: audit_cohort.py <cohort_dir> [output.csv] [summary.md]")
        sys.exit(1)
    cohort_dir = sys.argv[1]
    out_csv = sys.argv[2] if len(sys.argv) > 2 else "evaluations/notes_cohort.csv"
    out_md = sys.argv[3] if len(sys.argv) > 3 else "evaluations/synthese_cohort.md"
    audit_cohort(cohort_dir, out_csv, out_md)


if __name__ == "__main__":
    main()

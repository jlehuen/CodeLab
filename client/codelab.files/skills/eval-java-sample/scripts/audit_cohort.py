#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_cohort.py - Audit d'ensemble d'une cohorte, détection de plagiat et d'assistance IA (Exemple Java).
"""

import os
import sys
import re
import csv
from pathlib import Path
from itertools import combinations
from audit_student import audit_student, strip_java_comments

SKILL_DIR = Path(__file__).resolve().parent.parent
SKILLS_ROOT = SKILL_DIR.parent
if str(SKILLS_ROOT) not in sys.path:
    sys.path.insert(0, str(SKILLS_ROOT))
from skills_common import load_weights, compute_similarity_malus, apply_combined_penalties, get_canonical_boilerplate_keys

def tokenize_java_code(content):
    """Normalise le code Java : élimine commentaires et chaînes, canonise les identifiants (hors API standard)."""
    content = strip_java_comments(content)
    java_keywords = {
        'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
        'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
        'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
        'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
        'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
        'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'record', 'var',
        'String', 'System', 'out', 'println', 'print', 'Scanner', 'Math', 'Object', 'List',
        'ArrayList', 'Override', 'Integer', 'Double', 'Boolean', 'true', 'false', 'null'
    }
    raw_toks = re.findall(r'[a-zA-Z_][a-zA-Z0-9_]*|\d+|==|!=|<=|>=|[+\-*/%&|^~<>=()\[\]{},;:]', content)
    tokens = []
    for tok in raw_toks:
        if tok in java_keywords or not tok[0].isalpha():
            tokens.append(tok)
        else:
            tokens.append('ID')
    return tokens

def canonical_java_file_key(fname):
    """Canonise le nom d'une classe Java (casse, underscores parasites) et ignore les sauvegardes."""
    low = fname.lower()
    if low.startswith('.') or low.endswith('.bak') or low.endswith('_bak.java') or 'copy' in low:
        return None
    stem = os.path.splitext(low)[0]
    clean = re.sub(r'[_\s-]+', '_', stem).strip('_')
    return f"{clean}.java" if clean else low

def get_ngrams(tokens, n=4):
    if len(tokens) < n:
        return set()
    return set(tuple(tokens[i:i+n]) for i in range(len(tokens) - n + 1))

def jaccard_similarity(set1, set2):
    if not set1 or not set2:
        return 0.0
    inter = len(set1 & set2)
    union = len(set1 | set2)
    return inter / float(union) if union > 0 else 0.0

def detect_cohort_plagiarism(cohort_dir, threshold=75.0, exclude_students=None, users_db=None):
    """Détecte les similarités de code anormales entre étudiants sur les classes Java."""
    student_files = {}
    p = Path(cohort_dir).resolve()
    excluded = set(exclude_students or ())
    students = [
        d.name for d in p.iterdir()
        if d.is_dir() and not d.name.startswith('.')
        and d.name not in ('evaluations', '__pycache__', '.hidden', 'bin', 'build', 'codelab', 'recovery')
        and d.name not in excluded
    ]

    for s in students:
        s_dir = p / s
        student_files[s] = {}
        for f in s_dir.glob('**/*.java'):
            rel_parts = f.relative_to(s_dir).parts
            if any(part.startswith('.') or part in ('evaluations', '__pycache__', '.hidden', 'bin', 'build', 'codelab', 'recovery') for part in rel_parts):
                continue
            ckey = canonical_java_file_key(f.name)
            if not ckey:
                continue
            rel = str(f.relative_to(s_dir))
            try:
                content = f.read_text(encoding='utf-8', errors='ignore')
                if len(content.strip()) < 80:
                    continue
                toks = tokenize_java_code(content)
                if len(toks) >= 55:
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
    for s1, s2 in combinations(students, 2):
        files1 = student_files.get(s1, {})
        files2 = student_files.get(s2, {})
        common = set(files1.keys()) & set(files2.keys())
        if not common:
            continue

        matched_entries = []
        for ckey in sorted(common):
            sim = jaccard_similarity(files1[ckey][1], files2[ckey][1])
            sim_pct = round(sim * 100, 1)
            if sim_pct >= threshold:
                display_name = files1[ckey][3] if files1[ckey][3] == files2[ckey][3] else f"{files1[ckey][3]} / {files2[ckey][3]}"
                min_toks = min(files1[ckey][2], files2[ckey][2])
                matched_entries.append((ckey, display_name, sim_pct, min_toks))
                file_pair_hits.setdefault(ckey, set()).update((s1, s2))

        if matched_entries:
            raw_pair_matches.append((s1, s2, len(common), matched_entries))

    boilerplate_keys = get_canonical_boilerplate_keys(file_pair_hits, file_submitters, file_median_tokens=file_median_tokens)
    suspicions = []
    for s1, s2, common_cnt, matched_entries in raw_pair_matches:
        kept = [(dname, sim_pct, min_toks) for ckey, dname, sim_pct, min_toks in matched_entries if ckey not in boilerplate_keys]
        if kept:
            similar_files = [(dname, sim_pct) for dname, sim_pct, _ in kept]
            max_sim = max(sim for _, sim, _ in kept)
            has_substantial_92 = any(sim >= 92.0 and (toks >= 120 or common_cnt <= 2) for _, sim, toks in kept)
            severity = "Forte suspicion" if (len(similar_files) >= 2 or has_substantial_92) else "À vérifier (signal faible)"
            suspicions.append({
                's1': s1,
                's2': s2,
                'similar_files': similar_files,
                'max_sim': max_sim,
                'severity': severity
            })

    suspicions.sort(key=lambda x: (len(x['similar_files']), x['max_sim']), reverse=True)
    return suspicions

def apply_similarity_penalties(results, suspicions):
    """Applique les malus de similarité (Forte suspicion uniquement) sur la note avec malus (total)."""
    weights = load_weights(SKILL_DIR)
    sim_by_student = {}
    for susp in suspicions:
        if "forte" not in str(susp.get('severity', '')).lower():
            continue
        for sid in (susp['s1'], susp['s2']):
            info = sim_by_student.setdefault(sid, {'files': set(), 'max_sim': 0.0})
            for fname, sim_pct in susp.get('similar_files', []):
                info['files'].add(fname)
                if sim_pct > info['max_sim']:
                    info['max_sim'] = sim_pct

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
            "files": list(sim_by_student[sid]['files']),
            "max_sim": sim_by_student[sid]['max_sim'],
        }
        ai_prob = float((r.get("ai_analysis") or r.get("ai_detection") or {}).get("probability_pct", 0.0))
        tot, malus = apply_combined_penalties(sc["note_technique_brute"], ai_prob, plag_info, weights)
        sc["malus_similarite"] = malus
        sc["total"] = tot

def analyze_cohort(session_dir, exclude_students=None):
    p = Path(session_dir).resolve()
    excluded = set(exclude_students or ())
    cohort_results = []
    for item in sorted(p.iterdir()):
        if item.is_dir() and not item.name.startswith(".") and item.name not in ("evaluations", ".hidden", "recovery", "codelab", "__pycache__", "bin", "build") and item.name not in excluded:
            j_files = [f for f in item.glob("*.java") if not f.name.endswith(".bak")]
            if not j_files:
                def _is_ign(f):
                    try:
                        return any(part.startswith(".") or part in ("__pycache__", "evaluations", "codelab", "bin", "build") for part in f.relative_to(item).parts[:-1]) or f.name.endswith(".bak")
                    except Exception:
                        return False
                j_files = [f for f in item.rglob("*.java") if not _is_ign(f)]
            if j_files:
                res = audit_student(str(item))
                cohort_results.append(res)
    suspicions = detect_cohort_plagiarism(session_dir, exclude_students=excluded)
    apply_similarity_penalties(cohort_results, suspicions)
    return cohort_results

def ensure_results_radars(results):
    """S'assure que chaque résultat dispose de ses analyses radar et détection de double-profil."""
    try:
        skills_root = Path(__file__).resolve().parent.parent.parent
        if str(skills_root) not in sys.path:
            sys.path.insert(0, str(skills_root))
        from activity_analyzer import generate_mermaid_radars
        for r in results:
            if "radars" not in r or not r["radars"]:
                s_name = r.get("student_name") or r.get("name") or r.get("student_id") or "Étudiant"
                r["radars"] = generate_mermaid_radars(r, s_name)
    except Exception:
        pass

def export_csv(results, csv_path, cohort_dir=None, exclude_students=None):
    os.makedirs(os.path.dirname(os.path.abspath(csv_path)), exist_ok=True)
    result_ids = {r.get("student_id", "") for r in results}
    suspicions = detect_cohort_plagiarism(cohort_dir, exclude_students=exclude_students) if cohort_dir else []
    suspicions = [s for s in suspicions if s['s1'] in result_ids and s['s2'] in result_ids]
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    ensure_results_radars(results)
    sim_map = {}
    for susp in suspicions:
        s_desc = f"{susp['s2']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s1'], []).append(s_desc)
        s_desc_rev = f"{susp['s1']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s2'], []).append(s_desc_rev)

    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(["login", "fichiers_java", "compilation", "architecture", "style", "tests", "note_sans_malus", "note_finale", "profil_didactique", "indice_ia", "alerte_similarite", "s_auth", "i_nav", "compilations", "executions", "alertes_burst"])
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
                len(r.get("java_files", [])),
                sc.get("compilation", 0),
                sc.get("architecture", 0),
                sc.get("style", 0),
                sc.get("tests", 0),
                note_sans_malus,
                sc.get("total", 0),
                prof_col,
                ai_str,
                sim_str,
                s_auth_col,
                i_nav_col,
                comp_col,
                exec_col,
                burst_col
            ])

def build_cohort_clusters(results, suspicious_logins):
    """Classe les étudiants en typologies/clusters pédagogiques exclusifs (mode sans IA + IA optionnelle)."""
    ensure_results_radars(results)
    total_st = len(results)
    
    # Détection si des alertes IA existent au sein de la cohorte
    has_any_ai = any(
        str((r.get('ai_analysis') or r.get('ai_detection') or {}).get('level', '')).startswith(('Élevé', 'Modéré')) or
        float((r.get('ai_analysis') or r.get('ai_detection') or {}).get('ai_probability', 0.0)) >= 30.0
        for r in results
    )

    clusters = [
        {
            'id': 'boss',
            'label': '👑 **Les Boss**',
            'sub': 'Excellence & Autonomie',
            'desc': 'Note >= 14. Démarche personnelle complète, code propre et rigoureux. À mobiliser sur des défis d\'approfondissement ou du tutorat.',
            'members': []
        },
        {
            'id': 'artisans',
            'label': '🔨 **Les Artisans**',
            'sub': 'Démarche d\'Apprentissage & Régularité',
            'desc': 'Note entre 10 et 13.9. Socle technique acquis, travail appliqué et régulier. Poursuivre la consolidation sur la modularité et les tests.',
            'members': []
        },
        {
            'id': 'casse_cou',
            'label': '⚡ **Les Casse-cou**',
            'sub': 'Acharnement & Tâtonnement / Raccourcis',
            'desc': 'Sauts de jalons, acharnement compilatoire ou code précipité sans structuration fonctionnelle. Recadrer sur la méthode.',
            'members': []
        },
        {
            'id': 'decrocheurs',
            'label': '🚪 **Les Décrocheurs**',
            'sub': 'Perte de Pied & Décrochage',
            'desc': 'Note < 10. Travaux partiels, blocages initiaux ou abandons précoces. Remédiation requise rapidement sur les fondamentaux.',
            'members': []
        },
        {
            'id': 'pirates',
            'label': '🏴‍☠️ **Les Pirates**',
            'sub': 'Collusion & Copier-Coller',
            'desc': 'Similarité directe (>= 75%) sur du code libre ou import de fichiers externes. Soutenance individuelle requise.',
            'members': []
        },
        {
            'id': 'cyborgs',
            'label': '🤖 **Les Cyborgs**',
            'sub': 'Recours IA Extérieur & Rupture Stylométrique',
            'desc': 'Forte présomption de code généré par IA ou ruptures stylistiques massives. Entretien oral conseillé.',
            'members': []
        }
    ]

    c_map = {c['id']: c for c in clusters}

    for r in results:
        lid = r.get('student_id') or r.get('login', '')
        name = r.get('student_name') or r.get('name', '')
        note = r.get('scores', {}).get('total_note', r.get('scores', {}).get('total', r.get('total', 0.0)))
        ai_data = r.get('ai_analysis') or r.get('ai_detection', {})
        ai_level = str(ai_data.get('level', ''))
        ai_prob = float(ai_data.get('probability_pct', ai_data.get('ai_probability', 0.0)))
        is_ai = ai_level.startswith(('Élevé', 'Modéré')) or ai_prob >= 30.0
        is_susp = lid in suspicious_logins

        # Détection Casse-cou / Sauts d'étapes (100% statique sans IA)
        has_skips = bool(r.get('critical_skips')) or (float(r.get('scores', {}).get('malus_saut', 0.0) or 0.0) > 0)
        if not has_skips and 'architecture' in r.get('scores', {}):
            arch = float(r['scores'].get('architecture', 0.0))
            synt = float(r['scores'].get('syntax', r['scores'].get('compilation', 0.0)))
            if arch < 3.5 and synt >= 4.5:
                has_skips = True

        member_entry = f"`{lid}` ({name})" if (name and name != lid) else f"`{lid}`"

        rad = r.get("radars") or {}
        prim_id = rad.get("primary_profile_id")

        if is_susp:
            c_map['pirates']['members'].append(member_entry)
        elif prim_id == "boss":
            c_map['boss']['members'].append(member_entry)
        elif prim_id == "artisan":
            c_map['artisans']['members'].append(member_entry)
        elif prim_id == "cyborg":
            c_map['cyborgs']['members'].append(member_entry)
        elif prim_id == "casse_cou":
            c_map['casse_cou']['members'].append(member_entry)
        elif prim_id == "decrocheur":
            c_map['decrocheurs']['members'].append(member_entry)
        elif prim_id == "pirate":
            c_map['pirates']['members'].append(member_entry)
        elif is_ai and 'cyborgs' in c_map:
            c_map['cyborgs']['members'].append(member_entry)
        elif has_skips and note >= 8.0:
            c_map['casse_cou']['members'].append(member_entry)
        elif note < 10.0:
            c_map['decrocheurs']['members'].append(member_entry)
        elif note >= 14.0:
            c_map['boss']['members'].append(member_entry)
        else:
            c_map['artisans']['members'].append(member_entry)

    lines = [
        "## Typologies & Répartition par Clusters d'Étudiants Types\n",
        "| Cluster Typologique | Profil Pédagogique | Effectif | Proportion | Caractéristiques & Recommandations | Étudiants Associés |",
        "| :--- | :--- | :---: | :---: | :--- | :--- |"
    ]
    for c in clusters:
        cnt = len(c['members'])
        pct = (cnt / total_st * 100.0) if total_st > 0 else 0.0
        m_str = ", ".join(c['members']) if c['members'] else "—"
        lines.append(f"| {c['label']} | {c['sub']} | {cnt} | {pct:.1f}% | {c['desc']} | {m_str} |")
    lines.append("")

    try:
        skills_root = Path(__file__).resolve().parent.parent.parent
        if str(skills_root) not in sys.path:
            sys.path.insert(0, str(skills_root))
        from activity_analyzer import generate_cohort_radar
        lines.append(generate_cohort_radar(clusters, total_st))
        lines.append("")
    except Exception:
        pass

    # Section dédiée aux Doubles-Profils Didactiques Cohérents observés
    double_profils = [r for r in results if (r.get("radars") or {}).get("is_double_profil")]
    if double_profils:
        lines.append("## 🧬 Analyse des Profils Hybrides & Doubles-Profils Remarquables\n")
        lines.append("> 💡 **Interprétation des Doubles-Profils** : Un double-profil est automatiquement identifié lorsque deux dimensions issues d'axes orthogonaux présentent conjointement une affinité élevée (scores >= 11.0 / 20 et écart <= 3.5 pts). Ces hybridations permettent d'affiner le diagnostic pédagogique au-delà des archétypes simples.\n")
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
            diag_str = f"{desc}<br>👉 *Conseil : {conseil}*"
            lines.append(f"| **{nom}** | `{lid}` | {lbl} | {surnom} | {diag_str} |")
        lines.append("")

    return "\n".join(lines)

def resolve_cohort_name(cohort_dir=None, output_md=None, session_id=None):
    """Détermine le nom en clair de la session/cohorte en ignorant les dossiers techniques comme 'dist'."""
    GENERIC_NAMES = {".", "dist", "distant", "local", "programs", "evaluations", "synthese", ""}
    if session_id and str(session_id).strip() not in GENERIC_NAMES:
        return str(session_id).strip()

    # 1. Nom extrait du fichier Markdown (ex: synthese_<session>.md)
    if output_md:
        cand = Path(output_md).stem.replace("synthese_", "").strip()
        if cand and cand not in GENERIC_NAMES:
            return cand

    # 2. Nom extrait de sessions.xml si présent
    for s_xml in [
        Path(cohort_dir) / "sessions.xml" if cohort_dir else None,
        Path(output_md).parent / "sessions.xml" if output_md else None
    ]:
        if s_xml and s_xml.is_file():
            try:
                import xml.etree.ElementTree as ET
                tree = ET.parse(s_xml)
                root = tree.getroot()
                if root.tag == "session" and root.get("id"):
                    sid = root.get("id").strip()
                    if sid and sid not in GENERIC_NAMES:
                        return sid
            except Exception:
                pass

    # 3. Dossier parent de output_md (ex: evaluations/<session>/synthese_...)
    if output_md:
        p_name = Path(output_md).parent.name.strip()
        if p_name and p_name not in GENERIC_NAMES:
            return p_name

    # 4. Nom du dossier cohort_dir si non générique
    if cohort_dir:
        base = os.path.basename(str(cohort_dir).rstrip("/\\")).strip()
        if base and base not in GENERIC_NAMES:
            return base

    return "Session"

def export_summary_markdown(results, out_path, cohort_dir=None, use_ai=None, session_id=None, exclude_students=None, **kwargs):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    result_ids = {r.get("student_id", "") for r in results}
    suspicions = detect_cohort_plagiarism(cohort_dir, exclude_students=exclude_students) if cohort_dir else []
    suspicions = [s for s in suspicions if s['s1'] in result_ids and s['s2'] in result_ids]
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    ensure_results_radars(results)
    suspicious_logins = set()
    strong_suspicious_logins = set()
    for susp in suspicions:
        suspicious_logins.add(susp['s1'])
        suspicious_logins.add(susp['s2'])
        if "forte" in str(susp.get('severity', '')).lower():
            strong_suspicious_logins.add(susp['s1'])
            strong_suspicious_logins.add(susp['s2'])

    cohort_name = resolve_cohort_name(cohort_dir, out_path, session_id or kwargs.get("session"))

    # Résolution des paramètres d'évaluation
    if use_ai is not None:
        has_ia = bool(use_ai)
    elif "--no-ai" in sys.argv or "--deterministic" in sys.argv:
        has_ia = False
    elif any("gemini" in str(r.get("eval_engine", "")).lower() for r in results):
        has_ia = True
    elif any("déterministe" in str(r.get("eval_engine", "")).lower() for r in results):
        has_ia = False
    else:
        # Vérification sur les fiches de rapport individuelles si présentes sur disque
        has_ia = False
        search_dirs = []
        if out_path:
            search_dirs.append(Path(out_path).parent)
        if cohort_dir:
            search_dirs.append(Path(cohort_dir) / "evaluations")
            search_dirs.append(Path(cohort_dir))
        for sdir in search_dirs:
            if sdir.is_dir():
                for rep in sdir.glob("**/*_rapport.md"):
                    try:
                        content = rep.read_text(encoding="utf-8", errors="ignore")
                        if "Moteur d'évaluation :** LLM Gemini" in content or "LLM Gemini" in content:
                            has_ia = True
                            break
                    except Exception:
                        pass
            if has_ia:
                break

    ia_status = "Active (Gemini)" if has_ia else "Inactive"

    md = []
    md.append(f"# Synthèse de la Cohorte : {cohort_name}")
    notes = [r.get("scores", {}).get("total", 0) for r in results]
    moyenne = (sum(notes) / len(notes)) if notes else 0.0
    md.append(f"**Effectif audité** : {len(results)} étudiants | **Moyenne** : **{moyenne:.2f}/20**")
    md.append("")
    md.append(f"**Algorithme d'évaluation** : `eval-java-sample` | Analyse IA : {ia_status} | Barème : 6 (Compilation) + 8 (Conception) + 4 (Javadoc) + 2 (Validation) = 20 pts\n")
    md.append("---")
    md.append("")
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
            f_str = ", ".join(f"`{f}` ({sim}%)" for f, sim in susp['similar_files'])
            sev_txt = susp['severity']
            sev_badge = f'<span style="color:#dc2626; font-weight:bold;">🚨 {sev_txt}</span>' if "forte" in sev_txt.lower() else f'<span style="color:#d97706; font-weight:bold;">⚠️ {sev_txt}</span>'
            md.append(f"| `{susp['s1']}` | `{susp['s2']}` | {f_str} | **{susp['max_sim']}%** | {sev_badge} |")
        md.append("")
        md.append("---")
        md.append("")

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

    md.append("| Étudiant / Dossier | Compilation (/6) | Conception (/8) | Javadoc (/4) | Tests (/2) | Note brute (/20) | Malus | Profil Didactique | Indice IA | Alertes |")
    md.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: | :--- |")
    for r in results:
        sc = r.get("scores", {})
        note_sans_malus = sc.get("note_technique_brute", round(sc.get("compilation", 0) + sc.get("architecture", 0) + sc.get("style", 0) + sc.get("tests", 0), 1))
        login = r.get('student_id', '')
        rad = r.get("radars") or {}
        prof_col = rad.get("profil_display", rad.get("profil_dominant", "—"))
        alerts = []
        if login in strong_suspicious_logins:
            alerts.append('<span style="color:#dc2626; font-weight:bold;">🚨 Similarité forte</span>')
        elif login in suspicious_logins:
            alerts.append('<span style="color:#d97706; font-weight:bold;">⚠️ À vérifier</span>')
        ai_data = r.get('ai_analysis', {})
        ai_prob = float(ai_data.get('probability_pct', 0.0) or 0.0)
        ai_level = ai_data.get('level', 'Faible')
        if ai_level.startswith('Élevé'):
            alerts.append('<span style="color:#dc2626; font-weight:bold;">🤖 Alerte IA</span>')
        elif ai_level.startswith('Modéré'):
            alerts.append('<span style="color:#d97706; font-weight:bold;">🤖 Idiomes IA</span>')
        alert_str = " | ".join(alerts) if alerts else "Aucune"
        malus_ind = compute_indicative_malus(ai_prob, strong_plag_by_id.get(login), weights_s)
        malus_col = format_indicative_malus_badge(malus_ind)
        md.append(f"| `{login}` | {sc.get('compilation', 0):.1f} | {sc.get('architecture', 0):.1f} | {sc.get('style', 0):.1f} | {sc.get('tests', 0):.1f} | **{note_sans_malus:.1f}** | {malus_col} | {prof_col} | {ai_level} | {alert_str} |")
    md.append("")
    md.append(build_cohort_clusters(results, strong_suspicious_logins))
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md))

def audit_cohort(cohort_dir, output_csv=None, output_md=None, **kwargs):
    if output_csv is None:
        output_csv = kwargs.get('csv_output')
    if output_md is None:
        output_md = kwargs.get('md_output')
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

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

def tokenize_java_code(content):
    """Normalise le code Java : élimine commentaires et chaînes, canonise les identifiants."""
    content = strip_java_comments(content)
    java_keywords = {
        'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
        'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
        'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
        'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
        'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
        'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'record', 'var'
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

def detect_cohort_plagiarism(cohort_dir, threshold=75.0):
    """Détecte les similarités de code anormales entre étudiants sur les classes Java."""
    student_files = {}
    p = Path(cohort_dir).resolve()
    students = [d.name for d in p.iterdir() if d.is_dir() and not d.name.startswith('.') and d.name not in ('evaluations', '__pycache__', '.hidden', 'bin', 'build', 'codelab', 'recovery')]

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
                if len(toks) >= 35:
                    ngrams = get_ngrams(toks, n=4)
                    if ngrams:
                        prev = student_files[s].get(ckey)
                        if not prev or len(toks) > prev[2]:
                            student_files[s][ckey] = (rel, ngrams, len(toks), f.name)
            except Exception:
                pass

    suspicions = []
    for s1, s2 in combinations(students, 2):
        files1 = student_files.get(s1, {})
        files2 = student_files.get(s2, {})
        common = set(files1.keys()) & set(files2.keys())
        if not common:
            continue

        similar_files = []
        total_sim = 0.0
        for ckey in sorted(common):
            sim = jaccard_similarity(files1[ckey][1], files2[ckey][1])
            sim_pct = round(sim * 100, 1)
            if sim_pct >= threshold:
                display_name = files1[ckey][3] if files1[ckey][3] == files2[ckey][3] else f"{files1[ckey][3]} / {files2[ckey][3]}"
                similar_files.append((display_name, sim_pct))
            total_sim += sim

        if similar_files:
            max_sim = max(sim for _, sim in similar_files)
            severity = "[ALERTE] Forte suspicion" if max_sim >= 85.0 or len(similar_files) >= 3 else "[ATTENTION] Suspicion modérée"
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
    """Applique les malus de similarité sur la note avec malus (total) sans toucher à note_technique_brute."""
    sim_by_student = {}
    for susp in suspicions:
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
        nb_files = len(sim_by_student[sid]['files'])
        max_sim = sim_by_student[sid]['max_sim']
        malus = 0.0
        if nb_files >= 3 or max_sim >= 95.0:
            malus = 2.0
        elif nb_files >= 2 or max_sim >= 85.0:
            malus = 1.0
        elif nb_files >= 1 and max_sim >= 75.0:
            malus = 0.5
        sc["malus_similarite"] = malus
        tot = sc["note_apres_ia"] - malus
        if "note_technique_brute" in sc:
            tot = max(tot, sc["note_technique_brute"] - 3.5)
        sc["total"] = max(0.0, round(tot, 1))

def analyze_cohort(session_dir):
    p = Path(session_dir).resolve()
    cohort_results = []
    for item in sorted(p.iterdir()):
        if item.is_dir() and item.name not in ("evaluations", ".hidden", "recovery", "codelab", "__pycache__", "bin", "build"):
            j_files = [f for f in item.glob("*.java") if not f.name.endswith(".bak")]
            if not j_files:
                def _is_ign(f):
                    try:
                        return any(p.startswith(".") or p in ("__pycache__", "evaluations", "codelab", "bin", "build") for p in f.relative_to(item).parts[:-1]) or f.name.endswith(".bak")
                    except Exception:
                        return False
                j_files = [f for f in item.rglob("*.java") if not _is_ign(f)]
            if j_files:
                res = audit_student(str(item))
                cohort_results.append(res)
    suspicions = detect_cohort_plagiarism(session_dir)
    apply_similarity_penalties(cohort_results, suspicions)
    return cohort_results

def export_csv(results, csv_path, cohort_dir=None):
    os.makedirs(os.path.dirname(os.path.abspath(csv_path)), exist_ok=True)
    suspicions = detect_cohort_plagiarism(cohort_dir) if cohort_dir else []
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    sim_map = {}
    for susp in suspicions:
        s_desc = f"{susp['s2']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s1'], []).append(s_desc)
        s_desc_rev = f"{susp['s1']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s2'], []).append(s_desc_rev)

    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(["login", "fichiers_java", "compilation", "architecture", "style", "tests", "note_sans_malus", "note_finale", "indice_ia", "alerte_similarite"])
        for r in results:
            sc = r.get("scores", {})
            note_sans_malus = sc.get("note_technique_brute", round(sc.get("compilation", 0) + sc.get("architecture", 0) + sc.get("style", 0) + sc.get("tests", 0), 1))
            ai_data = r.get("ai_analysis", {})
            ai_str = ai_data.get("level", "Faible (0% - Code authentique)")
            login = r.get("student_id", "")
            sim_str = " | ".join(sim_map.get(login, [])) if login in sim_map else "Aucune"
            writer.writerow([
                login,
                len(r.get("java_files", [])),
                sc.get("compilation", 0),
                sc.get("architecture", 0),
                sc.get("style", 0),
                sc.get("tests", 0),
                note_sans_malus,
                sc.get("total", 0),
                ai_str,
                sim_str
            ])

def build_cohort_clusters(results, suspicious_logins):
    """Classe les étudiants en typologies/clusters pédagogiques exclusifs (mode sans IA + IA optionnelle)."""
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
            'label': '**Les Boss**',
            'sub': 'Excellence & Autonomie',
            'desc': 'Note $\\ge$ 14. Démarche personnelle complète, code propre et rigoureux. À mobiliser sur des défis d\'approfondissement ou du tutorat.',
            'members': []
        },
        {
            'id': 'artisans',
            'label': '**Les Artisans**',
            'sub': 'Pratique Régulière & Socle en Consolidation',
            'desc': 'Note entre 10 et 13.9. Socle technique acquis, travail appliqué et régulier. Poursuivre la consolidation sur la modularité et les tests.',
            'members': []
        },
        {
            'id': 'casse_cou',
            'label': '**Les Casse-cou**',
            'sub': 'Raccourcis & Architecture Fragile',
            'desc': 'Sauts de jalons intermédiaires ou code précipité sans structuration fonctionnelle. Recadrer sur la rigueur de modélisation.',
            'members': []
        },
        {
            'id': 'decrocheurs',
            'label': '**Les Décrocheurs**',
            'sub': 'Difficultés Méthodologiques ou Décrochage',
            'desc': 'Note < 10. Travaux partiels, blocages initiaux ou abandons précoces. Remédiation requise rapidement sur les fondamentaux.',
            'members': []
        },
        {
            'id': 'pirates',
            'label': '**Les Pirates**',
            'sub': 'Code Partagé & Copier-Coller',
            'desc': 'Similarité directe (≥ 75%) sur du code libre (détecté 100% hors-ligne). Rappel d\'intégrité et soutenance orale individuelle requise.',
            'members': []
        }
    ]

    if has_any_ai:
        clusters.append({
            'id': 'cyborgs',
            'label': '**Les Cyborgs**',
            'sub': 'Recours IA Extérieur & Rupture Stylométrique',
            'desc': 'Forte présomption de code généré par IA ou ruptures de style. Entretien oral conseillé pour vérifier l\'autonomie conceptuelle.',
            'members': []
        })

    c_map = {c['id']: c for c in clusters}

    for r in results:
        lid = r.get('student_id') or r.get('login', '')
        name = r.get('student_name') or r.get('name', '')
        note = r.get('scores', {}).get('total_note', r.get('scores', {}).get('total', r.get('total', 0.0)))
        ai_data = r.get('ai_analysis') or r.get('ai_detection', {})
        ai_level = str(ai_data.get('level', ''))
        ai_prob = float(ai_data.get('ai_probability', 0.0))
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

        if is_susp:
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
        "## Typologies & Clusters d'Étudiants Types\n",
        "| Cluster Typologique | Profil Pédagogique | Effectif | Proportion | Caractéristiques & Recommandations | Étudiants Associés |",
        "| :--- | :--- | :---: | :---: | :--- | :--- |"
    ]
    for c in clusters:
        cnt = len(c['members'])
        pct = (cnt / total_st * 100.0) if total_st > 0 else 0.0
        m_str = ", ".join(c['members']) if c['members'] else "—"
        lines.append(f"| {c['label']} | {c['sub']} | {cnt} | {pct:.1f}% | {c['desc']} | {m_str} |")
    lines.append("")
    return "\n".join(lines)

def export_summary_markdown(results, out_path, cohort_dir=None):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    suspicions = detect_cohort_plagiarism(cohort_dir) if cohort_dir else []
    if suspicions:
        apply_similarity_penalties(results, suspicions)
    suspicious_logins = set()
    for susp in suspicions:
        suspicious_logins.add(susp['s1'])
        suspicious_logins.add(susp['s2'])

    cohort_name = ""
    if cohort_dir:
        cohort_name = os.path.basename(str(cohort_dir).rstrip("/\\"))
    if not cohort_name or cohort_name in (".", "evaluations"):
        if out_path:
            cohort_name = Path(out_path).stem.replace("synthese_", "")
    if not cohort_name:
        cohort_name = "Session"

    w_cand = Path(cohort_dir) / "weights.json" if cohort_dir else None
    if not w_cand or not w_cand.exists():
        w_cand = Path(__file__).resolve().parent.parent / "weights.json"
    w_str = str(w_cand).replace(str(Path.home()), "~") if w_cand else "weights.json"

    has_ia = any(
        str((r.get('ai_analysis') or {}).get('level', '')).startswith(('Élevé', 'Modéré'))
        for r in results
    )
    ia_status = "Active (Gemini)" if has_ia else "Inactive (Mode statique local)"

    md = []
    md.append(f"# Synthèse de la Cohorte : {cohort_name}")
    notes = [r.get("scores", {}).get("total", 0) for r in results]
    moyenne = (sum(notes) / len(notes)) if notes else 0.0
    md.append(f"**Effectif audité** : {len(results)} étudiants | **Moyenne** : **{moyenne:.2f}/20**")
    md.append("")
    md.append(f"**Algorithme d'évaluation** : Compétence : `eval-java-sample` | Analyse IA : {ia_status} | Pondérations : `{w_str}` (Barème 6+8+4+2=20 pts)\n")
    md.append("---")
    md.append("")

    if suspicions:
        md.append("## Analyse de Similarités & Copier-Collé Inter-Étudiants")
        md.append(f"> **[ATTENTION] {len(suspicions)} paire(s) d'étudiants présentent une similarité suspecte** :\n")
        md.append("| Étudiant 1 | Étudiant 2 | Fichiers Suspects | Similarité Max | Gravité |")
        md.append("| :--- | :--- | :--- | :---: | :--- |")
        for susp in suspicions:
            f_str = ", ".join(f"`{f}` ({sim}%)" for f, sim in susp['similar_files'])
            md.append(f"| `{susp['s1']}` | `{susp['s2']}` | {f_str} | **{susp['max_sim']}%** | {susp['severity']} |")
        md.append("")
        md.append("---")
        md.append("")

    md.append("| Étudiant / Dossier | Compilation (/6) | Conception (/8) | Javadoc (/4) | Tests (/2) | Note sans malus (/20) | Note avec malus (/20) | Indice IA | Alertes |")
    md.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :--- |")
    for r in results:
        sc = r.get("scores", {})
        note_sans_malus = sc.get("note_technique_brute", round(sc.get("compilation", 0) + sc.get("architecture", 0) + sc.get("style", 0) + sc.get("tests", 0), 1))
        login = r.get('student_id', '')
        alerts = []
        if login in suspicious_logins:
            alerts.append("Similarite forte")
        ai_data = r.get('ai_analysis', {})
        ai_level = ai_data.get('level', 'Faible')
        if ai_level.startswith('Élevé'):
            alerts.append("IA suspectee")
        elif ai_level.startswith('Modéré'):
            alerts.append("Idiomes IA")
        alert_str = " | ".join(alerts) if alerts else "Aucune"
        md.append(f"| `{login}` | {sc.get('compilation', 0):.1f} | {sc.get('architecture', 0):.1f} | {sc.get('style', 0):.1f} | {sc.get('tests', 0):.1f} | {note_sans_malus:.1f} | **{sc.get('total', 0):.1f}** | {ai_level} | {alert_str} |")
    md.append("")
    md.append(build_cohort_clusters(results, suspicious_logins))
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md))

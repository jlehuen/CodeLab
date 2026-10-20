#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
audit_cohort.py - Audit d'ensemble d'une cohorte, détection de plagiat et d'assistance IA (Exemple C).
Dernière modification : 14/10/2026
"""

import os
import sys
import re
import csv
from pathlib import Path
from itertools import combinations
from audit_student import audit_student, strip_c_comments

def tokenize_c_code(content):
    """Normalise le code C : élimine commentaires et chaînes, canonise les identifiants."""
    content = strip_c_comments(content)

    c_keywords = {
        'void', 'int', 'char', 'float', 'double', 'short', 'long', 'signed', 'unsigned',
        'struct', 'union', 'enum', 'typedef', 'sizeof', 'auto', 'register', 'static',
        'extern', 'const', 'volatile', 'return', 'if', 'else', 'switch', 'case', 'default',
        'for', 'do', 'while', 'break', 'continue', 'goto', 'bool', 'true', 'false'
    }

    raw_toks = re.findall(r'[a-zA-Z_][a-zA-Z0-9_]*|\d+|==|!=|<=|>=|\+\+|--|->|[+\-*/%&|^~<>=()\[\]{},;:]', content)
    tokens = []
    for tok in raw_toks:
        if tok in c_keywords or not tok[0].isalpha():
            tokens.append(tok)
        else:
            tokens.append('ID')
    return tokens

def canonical_c_file_key(fname):
    """Canonise le nom d'un fichier C (casse, underscores parasites) et ignore les sauvegardes."""
    low = fname.lower()
    if low.startswith('.') or low.endswith('.bak') or low.endswith('_bak.c') or 'copy' in low:
        return None
    stem = os.path.splitext(low)[0]
    clean = re.sub(r'[_\s-]+', '_', stem).strip('_')
    return f"{clean}.c" if clean else low

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

def detect_cohort_plagiarism(cohort_dir, threshold=80.0):
    """Détecte les similarités de code anormales entre étudiants sur les programmes C."""
    student_files = {}
    p = Path(cohort_dir).resolve()
    students = [d.name for d in p.iterdir() if d.is_dir() and not d.name.startswith('.') and d.name not in ('evaluations', '__pycache__', '.hidden', 'codelab', 'recovery')]

    for s in students:
        s_dir = p / s
        student_files[s] = {}
        for f in s_dir.glob('**/*.c'):
            rel_parts = f.relative_to(s_dir).parts
            if any(part.startswith('.') or part in ('evaluations', '__pycache__', '.hidden', 'codelab', 'recovery') for part in rel_parts):
                continue
            ckey = canonical_c_file_key(f.name)
            if not ckey:
                continue
            rel = str(f.relative_to(s_dir))
            try:
                content = f.read_text(encoding='utf-8', errors='ignore')
                if len(content.strip()) < 80:
                    continue
                toks = tokenize_c_code(content)
                if len(toks) >= 35:
                    ngrams = get_ngrams(toks, n=4)
                    if ngrams:
                        prev = student_files[s].get(ckey)
                        if not prev or len(toks) > prev[2]:
                            student_files[s][ckey] = (rel, ngrams, len(toks), f.name)
            except Exception:
                pass

    suspicions = []
    all_pairs_matrix = []

    for s1, s2 in combinations(students, 2):
        files1 = student_files.get(s1, {})
        files2 = student_files.get(s2, {})
        common = set(files1.keys()) & set(files2.keys())
        if not common:
            continue

        similar_files = []
        max_sim = 0.0
        for ckey in sorted(common):
            sim = jaccard_similarity(files1[ckey][1], files2[ckey][1])
            sim_pct = round(sim * 100, 1)
            if sim_pct > max_sim:
                max_sim = sim_pct
            if sim_pct >= threshold:
                display_name = files1[ckey][3] if files1[ckey][3] == files2[ckey][3] else f"{files1[ckey][3]} / {files2[ckey][3]}"
                similar_files.append((display_name, sim_pct))

        all_pairs_matrix.append((s1, s2, max_sim))

        if similar_files:
            severity = "🚨 Forte suspicion" if max_sim >= 90.0 else "⚠️ Suspicion modérée"
            suspicions.append({
                's1': s1,
                's2': s2,
                'similar_files': similar_files,
                'max_sim': max_sim,
                'severity': severity
            })

    suspicions.sort(key=lambda x: (len(x['similar_files']), x['max_sim']), reverse=True)
    return suspicions, all_pairs_matrix

def analyze_cohort(session_dir):
    p = Path(session_dir).resolve()
    cohort_results = []
    for item in sorted(p.iterdir()):
        if item.is_dir() and item.name not in ("evaluations", ".hidden", "recovery", "codelab", "__pycache__"):
            c_files = list(item.glob("*.c")) + list(item.glob("*.h"))
            if not c_files:
                def _is_ign(f):
                    try:
                        return any(p.startswith(".") or p in ("__pycache__", "evaluations", "codelab") for p in f.relative_to(item).parts[:-1])
                    except Exception:
                        return False
                c_files = [f for f in item.rglob("*.c") if not _is_ign(f)]
            if c_files:
                res = audit_student(str(item))
                cohort_results.append(res)
    return cohort_results

def export_csv(results, csv_path, cohort_dir=None):
    os.makedirs(os.path.dirname(os.path.abspath(csv_path)), exist_ok=True)
    suspicions, _ = detect_cohort_plagiarism(cohort_dir) if cohort_dir else ([], [])
    sim_map = {}
    for susp in suspicions:
        s_desc = f"{susp['s2']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s1'], []).append(s_desc)
        s_desc_rev = f"{susp['s1']} ({susp['max_sim']}%)"
        sim_map.setdefault(susp['s2'], []).append(s_desc_rev)

    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(["login", "fichiers_c", "compilation", "architecture", "style", "tests", "note_finale", "indice_ia", "alerte_similarite"])
        for r in results:
            sc = r.get("scores", {})
            ai_data = r.get("ai_detection", {})
            ai_str = ai_data.get("verdict", "0% IA (Code authentique)")
            login = r.get("student_id", "")
            sim_str = " | ".join(sim_map.get(login, [])) if login in sim_map else "Aucune"
            writer.writerow([
                login,
                len(r.get("c_files", [])),
                sc.get("syntax", 0),
                sc.get("architecture", 0),
                sc.get("style", 0),
                sc.get("tests", 0),
                sc.get("total", 0),
                ai_str,
                sim_str
            ])

def export_summary_markdown(results, out_path, cohort_dir=None):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    suspicions, _ = detect_cohort_plagiarism(cohort_dir) if cohort_dir else ([], [])
    suspicious_logins = set()
    for susp in suspicions:
        suspicious_logins.add(susp['s1'])
        suspicious_logins.add(susp['s2'])

    md = []
    md.append("# Synthèse de Promotion — Évaluation C / Algorithmique")
    md.append("")
    md.append(f"- **Nombre d'étudiants évalués** : {len(results)}")
    if results:
        notes = [r.get("scores", {}).get("total", 0) for r in results]
        moyenne = sum(notes) / len(notes)
        md.append(f"- **Moyenne générale** : {moyenne:.2f} / 20")
    md.append("")
    md.append("---")
    md.append("")

    if suspicions:
        md.append("## 🔍 Analyse de Similarités & Copier-Collé Inter-Étudiants")
        md.append(f"> ⚠️ **{len(suspicions)} paire(s) d'étudiants présentent une similarité suspecte** :\n")
        md.append("| Étudiant 1 | Étudiant 2 | Fichiers Suspects | Similarité Max | Gravité |")
        md.append("| :--- | :--- | :--- | :---: | :--- |")
        for susp in suspicions:
            f_str = ", ".join(f"`{f}` ({sim}%)" for f, sim in susp['similar_files'])
            md.append(f"| `{susp['s1']}` | `{susp['s2']}` | {f_str} | **{susp['max_sim']}%** | {susp['severity']} |")
        md.append("")
        md.append("---")
        md.append("")

    md.append("| Étudiant / Dossier | Compilation (/6) | Architecture (/8) | Style (/4) | Tests (/2) | Note Totale (/20) | Indice IA | Alertes |")
    md.append("| :--- | :---: | :---: | :---: | :---: | :---: | :--- | :--- |")
    for r in results:
        sc = r.get("scores", {})
        login = r.get('student_id', '')
        alerts = []
        if login in suspicious_logins:
            alerts.append("👥 Similarité forte")
        ai_data = r.get('ai_detection', {})
        ai_prob = ai_data.get('ai_probability', 0.0)
        ai_level = f"{ai_prob}% IA"
        if ai_prob >= 75.0:
            alerts.append("🤖 IA suspectée")
        elif ai_prob >= 40.0:
            alerts.append("⚠️ Marqueurs IA")
        alert_str = " | ".join(alerts) if alerts else "Aucune"
        md.append(f"| `{login}` | {sc.get('syntax', 0):.1f} | {sc.get('architecture', 0):.1f} | {sc.get('style', 0):.1f} | {sc.get('tests', 0):.1f} | **{sc.get('total', 0):.1f}** | {ai_level} | {alert_str} |")
    md.append("")
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

    # Export matrice de plagiat
    matrix_csv = Path(cohort_dir) / "evaluations" / "plagiarism_matrix.csv"
    matrix_csv.parent.mkdir(parents=True, exist_ok=True)
    _, matrix = detect_cohort_plagiarism(cohort_dir)
    if matrix:
        with open(matrix_csv, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f, delimiter=';')
            w.writerow(["etudiant_1", "etudiant_2", "similarite_max_pct"])
            for row in matrix:
                w.writerow(row)

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

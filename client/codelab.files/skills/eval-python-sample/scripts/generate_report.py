#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple Python).
"""

import os
import sys
from pathlib import Path
from datetime import datetime

def format_report_markdown(results):
    student_id = results.get("student_id", "Inconnu")
    student_name = results.get("student_name")
    assignment = results.get("assignment")
    scores = results.get("scores", {})
    total = scores.get("total", 0.0)
    py_files = results.get("py_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "10/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    md = []
    md.append(f"# Fiche d'Évaluation : Python / Algorithmique")
    md.append("")
    if student_name and student_name != student_id:
        md.append(f"- **Étudiant** : **{student_name}** (`{student_id}`)")
    else:
        md.append(f"- **Étudiant** : `{student_id}`")
    if assignment and assignment != student_id:
        md.append(f"- **Travail / Dossier** : `{assignment}`")
    md.append(f"- **Fichiers Python analysés** : {len(py_files)} fichier(s) `.py`")
    md.append(f"- **Fonctions déclarées** : {len(results.get('functions', []))} fonction(s)")
    md.append(f"- **Docstrings & Documentation** : {results.get('docstrings_count', 0)} bloc(s)")
    md.append(f"- **Tests / Assertions** : {results.get('asserts_count', 0)} assertion(s)")
    md.append("")
    ai_data = results.get("ai_analysis") or {}
    ai_level = ai_data.get("level", "Faible (0% - Code authentique)")

    md.append("### Cartouche de Traçabilité")
    md.append(f"- **Date de l'évaluation :** {eval_date}")
    md.append(f"- **Skill appliqué :** `eval-python-sample` (dernière modification : {skill_date})")
    md.append(f"- **Moteur d'évaluation :** {engine_name}")
    md.append(f"- **Indice d'assistance IA :** {ai_level}")
    md.append(f"- **Environnement :** CodeLab v1.5.0")
    md.append("")
    md.append("---")
    md.append("")
    md.append("## 1. Synthèse des Résultats")
    md.append("")
    md.append("| Axe d'évaluation | Note obtenue | Barème | Commentaires synthétiques |")
    md.append("| :--- | :---: | :---: | :--- |")
    note_sans_malus = scores.get("note_technique_brute", round(scores.get('syntax', 0) + scores.get('architecture', 0) + scores.get('style', 0) + scores.get('tests', 0), 1))
    md.append(f"| **Axe 1 : Syntaxe & Arbre AST** | {scores.get('syntax', 0):.1f} | / 6 | {results.get('valid_ast_count', 0)}/{len(py_files)} fichier(s) syntaxiquement valides. |")
    md.append(f"| **Axe 2 : Conception & Modularité** | {scores.get('architecture', 0):.1f} | / 8 | Structuration des fonctions et lisibilité de la logique. |")
    md.append(f"| **Axe 3 : Style Pythonique & PEP 8** | {scores.get('style', 0):.1f} | / 4 | Documentation, idiomes et nommage. |")
    md.append(f"| **Axe 4 : Démarche de test** | {scores.get('tests', 0):.1f} | / 2 | Présence d'assertions ou de modules de test. |")
    md.append(f"| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20** | **Somme technique brute des 4 axes** |")
    md.append(f"| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20** | **Bilan global de l'évaluation** |")
    md.append("")
    md.append("---")
    md.append("")
    md.append("## 2. Analyse Détaillée")
    md.append("")
    snippets = results.get("snippets", {})

    if results.get("syntax_errors"):
        md.append("### Erreurs de syntaxe relevées :")
        md.append("")
        for fname, err in results["syntax_errors"].items():
            md.append(f"- `{fname}` : {err}")
        md.append("")
        if snippets.get("compilation_errors"):
            md.append("#### Extraits du code concerné :")
            md.append("")
            for se in snippets["compilation_errors"]:
                md.append(f"- `{se['file']}` (ligne {se['line']}) :")
                md.append("```python")
                md.append(se["code"])
                md.append("```")
                md.append("")
    else:
        md.append("Tous les scripts Python ont été validés avec succès par l'analyseur AST.")
        md.append("")

    if snippets.get("highlights"):
        md.append("### Points Forts Observés (Extraits de code) :")
        md.append("")
        for hl in snippets["highlights"]:
            md.append(f"- **{hl['title']}** (`{hl['file']}`, ligne {hl['line']}) :")
            md.append("```python")
            md.append(hl["code"])
            md.append("```")
            md.append("")

    md.append("### 💡 Conseils & Pistes d'Amélioration :")
    md.append("")
    if results.get("docstrings_count", 0) == 0:
        md.append("- Documenter systématiquement chaque fonction avec une `docstring` au format triple guillemets (`\"\"\"...\"\"\"`).")
    if len(results.get("functions", [])) < 2:
        md.append("- Éviter d'écrire tout le script au niveau racine : encapsuler la logique dans des fonctions dédiées.")
    if results.get("asserts_count", 0) == 0:
        md.append("- Introduire des tests automatisés avec l'instruction `assert condition` ou le module `doctest`.")
    if not results.get("feedback"):
        md.append("- Bon travail d'ensemble, persévérer dans l'adoption des idiomes Python.")

    if snippets.get("anti_patterns"):
        md.append("")
        md.append("#### Extraits à retravailler méthodologiquement :")
        md.append("")
        for ap in snippets["anti_patterns"]:
            md.append(f"- **{ap['title']}** (`{ap['file']}`, ligne {ap['line']}) :")
            md.append("```python")
            md.append(ap["code"])
            md.append("```")
            md.append("")

    md.append("")
    return "\n".join(md)

def generate_individual_report(folder_path, output_path, sessions_xml=None):
    from audit_student import audit_student
    res = audit_student(folder_path, sessions_xml)
    report_text = format_report_markdown(res)
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(report_text)
    return output_path

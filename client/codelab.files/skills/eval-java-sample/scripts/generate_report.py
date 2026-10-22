#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple Java).
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
    java_files = results.get("java_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "10/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    md = []
    md.append(f"# Fiche d'Évaluation : Java / Programmation Orientée Objet")
    md.append("")
    if student_name and student_name != student_id:
        md.append(f"- **Étudiant** : **{student_name}** (`{student_id}`)")
    else:
        md.append(f"- **Étudiant** : `{student_id}`")
    if assignment and assignment != student_id:
        md.append(f"- **Travail / Dossier** : `{assignment}`")
    md.append(f"- **Fichiers sources analysés** : {len(java_files)} fichier(s) `.java`")
    md.append(f"- **Classes & Interfaces** : {results.get('classes_count', 0)} classe(s), {results.get('interfaces_count', 0)} interface(s)")
    md.append(f"- **Méthodes déclarées** : {results.get('methods_count', 0)} méthode(s)")
    md.append(f"- **Encapsulation** : {results.get('private_fields', 0)} champ(s) privé(s) / {results.get('public_fields', 0)} public(s)")
    md.append("")
    ai_data = results.get("ai_analysis") or {}
    ai_level = ai_data.get("level", "Faible (0% - Code authentique)")

    md.append("### Cartouche de Traçabilité")
    md.append(f"- **Date de l'évaluation :** {eval_date}")
    md.append(f"- **Skill appliqué :** `eval-java-sample` (dernière modification : {skill_date})")
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
    note_sans_malus = scores.get("note_technique_brute", round(scores.get('compilation', 0) + scores.get('architecture', 0) + scores.get('style', 0) + scores.get('tests', 0), 1))
    md.append(f"| **Axe 1 : Compilation Java** | {scores.get('compilation', 0):.1f} | / 6 | {'Compilation réussie' if results.get('compilation_ok') else 'Erreurs de compilation'} |")
    md.append(f"| **Axe 2 : Modélisation Objet & Encapsulation** | {scores.get('architecture', 0):.1f} | / 8 | Respect de l'encapsulation et découpage des classes. |")
    md.append(f"| **Axe 3 : Javadoc & Conventions Java** | {scores.get('style', 0):.1f} | / 4 | Clarté de la documentation et conventions de style. |")
    md.append(f"| **Axe 4 : Démarche de test** | {scores.get('tests', 0):.1f} | / 2 | Présence de classes ou méthodes de test. |")
    md.append(f"| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20** | **Somme technique brute des 4 axes** |")
    md.append(f"| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20** | **Bilan global de l'évaluation** |")
    md.append("")
    md.append("---")
    md.append("")
    md.append("## 2. Analyse Détaillée")
    md.append("")
    snippets = results.get("snippets", {})

    if not results.get("compilation_ok"):
        md.append("### Erreurs de compilation `javac` :")
        md.append("")
        md.append("```text")
        md.append(results.get("compilation_error", ""))
        md.append("```")
        md.append("")
        if snippets.get("compilation_errors"):
            md.append("#### Extraits du code concerné :")
            md.append("")
            for ce in snippets["compilation_errors"]:
                md.append(f"- `{ce['file']}` (ligne {ce['line']}) :")
                md.append("```java")
                md.append(ce["code"])
                md.append("```")
                md.append("")
    else:
        md.append("Tous les fichiers sources compilent sans erreur.")
        md.append("")

    if snippets.get("highlights"):
        md.append("### Points Forts Observés (Extraits de code) :")
        md.append("")
        for hl in snippets["highlights"]:
            md.append(f"- **{hl['title']}** (`{hl['file']}`, ligne {hl['line']}) :")
            md.append("```java")
            md.append(hl["code"])
            md.append("```")
            md.append("")

    md.append("### 💡 Conseils & Pistes d'Amélioration :")
    md.append("")
    if results.get("public_fields", 0) > 0 and results.get("private_fields", 0) == 0:
        md.append("- Privilégier des attributs `private` pour respecter l'encapsulation objet, avec des accesseurs (getters) si nécessaire.")
    if results.get("javadoc_blocks", 0) == 0:
        md.append("- Documenter les classes et méthodes principales avec des commentaires Javadoc (`/** ... */`).")
    if not results.get("has_tests"):
        md.append("- Prévoir une classe de test dédiée avec une méthode `main` ou des tests unitaires.")
    if not results.get("feedback"):
        md.append("- Conception solide, continuer à veiller à la bonne granularité des classes.")

    if snippets.get("anti_patterns"):
        md.append("")
        md.append("#### Extraits à retravailler méthodologiquement :")
        md.append("")
        for ap in snippets["anti_patterns"]:
            md.append(f"- **{ap['title']}** (`{ap['file']}`, ligne {ap['line']}) :")
            md.append("```java")
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

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple C).
Dernière modification : 14/10/2026
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
    c_files = results.get("c_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "14/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    ai_det = results.get("ai_analysis") or results.get("ai_detection") or {}
    ai_level = ai_det.get("level") or ai_det.get("verdict", "Faible (0% - Code authentique)")

    md = []
    md.append(f"# Fiche d'Évaluation : C / Algorithmique")
    md.append("")
    if student_name and student_name != student_id:
        md.append(f"- **Étudiant** : **{student_name}** (`{student_id}`)")
    else:
        md.append(f"- **Étudiant** : `{student_id}`")
    if assignment and assignment != student_id:
        md.append(f"- **Travail / Dossier** : `{assignment}`")
    md.append(f"- **Fichiers C analysés** : {len(c_files)} fichier(s) `.c`/`.h`")
    md.append(f"- **Fonctions déclarées** : {len(results.get('functions', []))} fonction(s)")
    md.append(f"- **Commentaires & Documentation** : {results.get('comments_count', 0)} bloc(s)")
    md.append(f"- **Assertions & Tests** : {results.get('asserts_count', 0)} élément(s)")
    md.append("")
    md.append("### 📋 Cartouche de Traçabilité")
    md.append(f"- **Date de l'évaluation :** {eval_date}")
    md.append(f"- **Skill appliqué :** `eval-c-sample` (dernière modification : {skill_date})")
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
    md.append(f"| **Axe 1 : Compilation & Syntaxe** | {scores.get('syntax', 0):.1f} | / 6 | {results.get('valid_c_count', 0)}/{len(c_files)} fichier(s) compilables sans erreur. |")
    md.append(f"| **Axe 2 : Architecture & Conception** | {scores.get('architecture', 0):.1f} | / 8 | Structuration modulaire en fonctions et logique de contrôle. |")
    md.append(f"| **Axe 3 : Style & Lisibilité** | {scores.get('style', 0):.1f} | / 4 | Clarté du nommage, indentation régulière et commentaires utiles. |")
    md.append(f"| **Axe 4 : Démarche de test** | {scores.get('tests', 0):.1f} | / 2 | Présence d'assertions ou de fonctions de test. |")
    md.append(f"| **Note Globale Estimée** | **{total:.1f}** | **/ 20** | **Bilan global de l'évaluation** |")
    md.append("")
    md.append("---")
    md.append("")
    md.append("## 2. Analyse Détaillée")
    md.append("")
    snippets = results.get("snippets", {})

    if results.get("syntax_errors"):
        md.append("### ⚠️ Erreurs de compilation relevées :")
        md.append("")
        for fname, err in results["syntax_errors"].items():
            md.append(f"- `{fname}` : {err}")
        md.append("")
        if snippets.get("compilation_errors"):
            md.append("#### Extraits du code concerné :")
            md.append("")
            for sc in snippets["compilation_errors"]:
                md.append(f"**Fichier `{sc['file']}`** :")
                md.append("```c")
                md.append(sc["code"])
                md.append("```")
                md.append("")
    else:
        md.append("### ✅ Compilation : Tous les fichiers C sont syntaxiquement valides.")
        md.append("")

    if snippets.get("highlights"):
        md.append("### 🌟 Fonctions et initiatives remarquées :")
        md.append("")
        for sh in snippets["highlights"]:
            md.append(f"**Fonction `{sh['function']}` dans `{sh['file']}`** :")
            md.append("```c")
            md.append(sh["code"])
            md.append("```")
            md.append("")

    if ai_det.get("markers"):
        md.append("### 🔍 Marqueurs d'authenticité / IA observés :")
        md.append("")
        for m in ai_det["markers"]:
            md.append(f"- `{m['file']}` : **{m['category']}** — {m['description']}")
        md.append("")

    md.append("---")
    md.append("")
    md.append("## 3. Conseils Pédagogiques & Remédiation")
    md.append("")
    if total >= 16:
        md.append("1. **Excellente maîtrise** : La structure algorithmique est solide et modulaire. Continuez à soigner la documentation et l'anticipation des cas limites.")
    elif total >= 12:
        md.append("1. **Bonne progression** : Le code est fonctionnel. Pour progresser, décomposez davantage les traitements volumineux en sous-fonctions spécialisées avec passage de paramètres explicite.")
    else:
        md.append("1. **Poursuivre les efforts** : Veillez à corriger les éventuelles erreurs de compilation et à structurer votre code avec des fonctions réutilisables plutôt qu'un bloc unique.")
    md.append("")
    md.append("2. **Bonnes pratiques C** : Définissez vos constantes symboliques avec `#define` en majuscules et vérifiez toujours la portée de vos variables locales.")
    md.append("")

    return "\n".join(md)

def generate_individual_report(student_folder, output_path=None, **kwargs):
    from audit_student import audit_student
    res = audit_student(student_folder)
    md_content = format_report_markdown(res)

    if output_path:
        out_p = Path(output_path)
        out_p.parent.mkdir(parents=True, exist_ok=True)
        out_p.write_text(md_content, encoding='utf-8')
        return str(out_p)
    return md_content

def main():
    if len(sys.argv) < 2:
        print("Usage: generate_report.py <student_folder> [output.md]")
        sys.exit(1)
    s_folder = sys.argv[1]
    out_file = sys.argv[2] if len(sys.argv) > 2 else None
    res = generate_individual_report(s_folder, out_file)
    if not out_file:
        print(res)

if __name__ == "__main__":
    main()

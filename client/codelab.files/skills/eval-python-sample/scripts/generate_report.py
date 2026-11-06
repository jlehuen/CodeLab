#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple Python).
Conforme au gabarit standard CodeLab avec double radar et 4 sections pédagogiques.
"""

import os
import sys
from pathlib import Path
from datetime import datetime

SKILL_ROOT = Path(__file__).resolve().parent.parent
SKILLS_DIR = SKILL_ROOT.parent
if str(SKILLS_DIR) not in sys.path:
    sys.path.insert(0, str(SKILLS_DIR))

try:
    from activity_analyzer import generate_mermaid_radars
except ImportError:
    def generate_mermaid_radars(data, name):
        return {"radars_markdown": "", "profil_dominant": "Standard"}


def render_template_str(template_str: str, vars_dict: dict) -> str:
    """Remplace les balises {cle} de manière sécurisée sans être affecté par les accolades de code."""
    res = template_str
    for k, v in vars_dict.items():
        res = res.replace(f"{{{k}}}", str(v))
    return res


def build_deterministic_sections(results: dict):
    """Construit les 4 sections pédagogiques normalisées CodeLab."""
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    py_files = results.get("py_files", [])
    snippets = results.get("snippets", {})
    syntax_errs = results.get("syntax_errors", {})
    fn_count = len(results.get("functions", []))
    doc_count = results.get("docstrings_count", 0)
    assert_count = results.get("asserts_count", 0)
    ai_prob = results.get("ai_analysis", {}).get("probability_pct", 0)

    # 1. Bilan de Progression & Analyse Pédagogique
    if total >= 16.0:
        bilan = (
            f"Excellente prestation sur l'ensemble de l'activité Python / Algorithmique ({total:.1f}/20). "
            f"L'architecture logicielle est mature et les {len(py_files)} fichier(s) `.py` analysés "
            f"sont parfaitement structurés en fonctions réutilisables. Le code témoigne d'une grande rigueur méthodologique."
        )
    elif total >= 13.0:
        bilan = (
            f"Bon travail régulier ({total:.1f}/20). Les objectifs fondamentaux d'algorithmique et de structuration "
            f"sont acquis. Le code ({fn_count} fonction(s) déclarée(s)) résout le problème posé de manière fluide et lisible."
        )
    elif total >= 10.0:
        bilan = (
            f"Socle technique minimal validé ({total:.1f}/20). Le programme fonctionne mais manque de modularité "
            f"ou conserve une démarche impérative linéaire. Des efforts restent à fournir sur l'abstraction et le style."
        )
    else:
        bilan = (
            f"Résultats fragiles ou travail inachevé ({total:.1f}/20). Des blocages conceptuels ou des difficultés "
            f"de syntaxe entravent le fonctionnement du programme. Une reprise méthodique des bases est nécessaire."
        )

    # 2. Points Forts Observés
    points_forts_list = []
    if not syntax_errs and len(py_files) > 0:
        points_forts_list.append("Conformité syntaxique irréprochable validée par l'analyseur d'arbre AST.")
    if fn_count >= 3:
        points_forts_list.append(f"Bonne modularité avec {fn_count} fonctions spécialisées bien isolées.")
    if doc_count >= 2:
        points_forts_list.append(f"Effort d'explicitation appréciable avec {doc_count} bloc(s) de documentation `docstrings`.")
    if assert_count >= 2:
        points_forts_list.append(f"Démarche de test proactive intégrant {assert_count} assertion(s) de validation.")

    if snippets.get("highlights"):
        for hl in snippets["highlights"]:
            points_forts_list.append(
                f"**{hl.get('title', 'Extrait remarquable')}** (`{hl.get('file', '')}`, ligne {hl.get('line', '')}) :\n"
                f"```python\n{hl.get('code', '').strip()}\n```"
            )

    if not points_forts_list:
        points_forts_list.append("Présence d'un code Python exécutable.")
    points_forts = "\n\n".join(f"- {p}" if not p.startswith("**") else p for p in points_forts_list)

    # 3. Axes d'Amélioration & Conseils Méthodologiques
    axes_list = []
    if syntax_errs:
        for fname, err in syntax_errs.items():
            axes_list.append(f"**Erreur de syntaxe à corriger** dans `{fname}` : `{err}`.")
        if snippets.get("compilation_errors"):
            for se in snippets["compilation_errors"]:
                axes_list.append(
                    f"Extrait concerné (`{se.get('file')}`, ligne {se.get('line')}) :\n"
                    f"```python\n{se.get('code', '').strip()}\n```"
                )

    if doc_count == 0:
        axes_list.append("Documenter systématiquement chaque fonction avec une `docstring` au format triple guillemets (`\"\"\"...\"\"\"`).")
    if fn_count < 2:
        axes_list.append("Éviter d'écrire le script au niveau racine : encapsuler la logique dans des fonctions dédiées avec passage de paramètres.")
    if assert_count == 0:
        axes_list.append("Introduire des tests automatisés avec l'instruction `assert condition` ou le module `unittest` / `pytest`.")

    if snippets.get("anti_patterns"):
        for ap in snippets["anti_patterns"]:
            axes_list.append(
                f"**{ap.get('title', 'Anti-pattern')}** (`{ap.get('file', '')}`, ligne {ap.get('line', '')}) :\n"
                f"```python\n{ap.get('code', '').strip()}\n```"
            )

    if ai_prob >= 30:
        axes_list.append(f"**Intégrité académique ({ai_prob}% suspicion IA)** : Privilégier la frappe et la conception personnelle sans recourir à des modèles génératifs externes.")

    if not axes_list:
        axes_list.append("Poursuivre sur cette dynamique en explorant les structures de données avancées et la programmation orientée objet.")
    axes_amelioration = "\n\n".join(f"- {a}" if not a.startswith("**") and not a.startswith("Extrait") else a for a in axes_list)

    # 4. Commentaire de l'Enseignant
    if total >= 16.0:
        commentaire = "Félicitations pour cet excellent travail. La rigueur apportée à la conception et à la documentation témoigne d'une grande maturité algorithmique. Continuez ainsi !"
    elif total >= 12.0:
        commentaire = "Bon investissement dans l'activité. Les compétences clés sont acquises ; veillez à appliquer les conseils de modularité et de test pour franchir un nouveau palier."
    elif total >= 9.0:
        commentaire = "Travail encourageant mais perfectible. Prenez le temps de bien analyser les erreurs du terminal et de décomposer vos fonctions pas à pas avant de coder."
    else:
        commentaire = "Ne vous découragez pas. Sollicitez les enseignants et les tuteurs dès les premiers blocages : c'est par une pratique régulière et guidée que vous progresserez rapidement."

    return bilan, points_forts, axes_amelioration, commentaire


def format_report_markdown(results: dict) -> str:
    student_id = results.get("student_id") or results.get("login", "Inconnu")
    student_name = results.get("student_name") or results.get("name", student_id)
    group = results.get("student_group", "")
    group_line = f"\n- **Groupe :** {group}" if group else ""
    assignment = results.get("assignment", "Python / Algorithmique")
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    py_files = results.get("py_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "10/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    # Table des scores
    note_sans_malus = scores.get("note_technique_brute", round(scores.get('syntax', 0) + scores.get('architecture', 0) + scores.get('style', 0) + scores.get('tests', 0), 1))
    syntax_s = scores.get('syntax', 0.0)
    arch_s = scores.get('architecture', 0.0)
    style_s = scores.get('style', 0.0)
    tests_s = scores.get('tests', 0.0)

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Critères & Observations |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Syntaxe & Arbre AST** | **{syntax_s:.1f}** | / 6.0 | {results.get('valid_ast_count', 0)}/{len(py_files)} fichier(s) syntaxiquement valides |
| **Axe 2 : Conception & Modularité** | **{arch_s:.1f}** | / 8.0 | Découpage fonctionnel, algorithmique et structuration |
| **Axe 3 : Style Pythonique & PEP 8** | **{style_s:.1f}** | / 4.0 | Documentation, lisibilité et conventions de nommage |
| **Axe 4 : Démarche de Test** | **{tests_s:.1f}** | / 2.0 | Présence d'assertions et validation des cas limites |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20.0** | **Somme technique brute des 4 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20.0** | **Bilan global de l'évaluation** |"""

    # Calcul des radars et profil dominant
    radars = generate_mermaid_radars(results, student_name or student_id)
    radars_visualisation = radars.get("radars_markdown", "")
    profil = radars.get("profil_dominant", "Standard")

    # Intégrité & stylométrie
    ai_data = results.get("ai_analysis") or {}
    ai_level = ai_data.get("level", "Faible (0% - Code authentique)")
    ai_prob = ai_data.get("probability_pct", 0)

    dinfo = results.get("dates_info", {})
    dist_days = dinfo.get("distinct_days", 0)
    chronometrie_courte = f"Régulière ({dist_days} séance(s))" if dist_days > 0 else "Standard"

    ind_stats = ai_data.get("indentation_stats", {})
    tot_tabs = ind_stats.get("total_tabs", 0)
    tot_spaces = ind_stats.get("total_spaces", 0)
    if tot_spaces > tot_tabs and tot_spaces >= 15:
        stylometrie_courte = f"⚠️ Rupture 4 espaces ({tot_spaces} espaces vs {tot_tabs} tabs)"
    else:
        stylometrie_courte = f"Conforme CodeLab ({tot_tabs} tabs)" if tot_tabs > 0 else "Standard"

    plag = results.get("plagiarism_alert", {})
    if plag and plag.get("max_sim", 0) >= 75.0:
        similarite_courte = f"⚠️ Partage avec {plag.get('partner', 'Camarade')} ({plag.get('max_sim', 0)}%)"
        similarite_block = f"> 🚨 **Suspicion de code partagé** avec **{plag.get('partner', 'Camarade')}** (similarité : **{plag.get('max_sim', 0)}%**)."
    else:
        similarite_courte = "Original (0% partage)"
        similarite_block = "✅ **Travail original authentique** : Analyse croisée de similarité négative."

    analyse_integrite = f"""### 🔬 Analyses d'Intégrité Pédagogique

#### ⏱️ Chronométrie & Stylométrie
- **Indicateur temporel :** {chronometrie_courte}
- **Indicateur stylométrique :** {stylometrie_courte}
- **Indice d'assistance IA :** {ai_level}

#### 👥 Test de Similarité Inter-Étudiants (Plagiat)
{similarite_block}"""

    # 4 sections pédagogiques
    bilan, points_forts, axes_amelioration, commentaire = build_deterministic_sections(results)

    template_vars = {
        "student_name": student_name,
        "student_id": student_id,
        "group_line": group_line,
        "assignment": assignment,
        "profil": profil,
        "eval_date": eval_date,
        "skill_name": "eval-python-sample",
        "skill_last_modified": skill_date,
        "eval_engine": engine_name,
        "chronometrie_courte": chronometrie_courte,
        "stylometrie_courte": stylometrie_courte,
        "similarite_courte": similarite_courte,
        "ai_level": ai_level,
        "scores_table": scores_table,
        "radars_visualisation": radars_visualisation,
        "analyse_integrite": analyse_integrite,
        "bilan": bilan,
        "points_forts": points_forts,
        "axes_amelioration": axes_amelioration,
        "commentaire": commentaire
    }

    tmpl_file = SKILL_ROOT / "report_template.md"
    report_text = None
    if tmpl_file.is_file():
        try:
            report_text = render_template_str(tmpl_file.read_text(encoding="utf-8"), template_vars)
        except Exception:
            report_text = None

    if not report_text:
        report_text = f"""# Fiche d'Évaluation : Python / Algorithmique

### Étudiant : **{student_name}**

- **Login / ID :** `{student_id}`
- **Module :** Python / Algorithmique{group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date}
- **Skill appliqué :** `eval-python-sample` (dernière modification : {skill_date})
- **Moteur d'évaluation :** {engine_name}
- **Chronométrie :** {chronometrie_courte}
- **Stylométrie d'indentation :** {stylometrie_courte}
- **Test de similarité (Plagiat) :** {similarite_courte}
- **Indice d'assistance IA :** {ai_level}

---

### Tableau Récapitulatif des Notes Calibrées

{scores_table}

---

{radars_visualisation}

---

{analyse_integrite}

---

### Bilan de Progression & Analyse Pédagogique

{bilan}

---

### Points Forts Observés

{points_forts}

---

### Axes d'Amélioration & Conseils Méthodologiques

{axes_amelioration}

---

### Commentaire de l'Enseignant

{commentaire}
"""

    return report_text


def generate_individual_report(folder_path, output_path, sessions_xml=None):
    from audit_student import audit_student
    res = audit_student(folder_path, sessions_xml)
    report_text = format_report_markdown(res)
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(report_text)
    return output_path


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: generate_report.py <student_folder> <output_path> [sessions_xml]")
        sys.exit(1)
    s_folder = sys.argv[1]
    out_file = sys.argv[2]
    s_xml = sys.argv[3] if len(sys.argv) > 3 else None
    generate_individual_report(s_folder, out_file, s_xml)
    print(f"[DONE] Rapport individuel généré dans : {out_file}")


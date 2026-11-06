#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple Java).
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
    """Construit les 4 sections pédagogiques normalisées CodeLab pour Java."""
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    java_files = results.get("java_files", [])
    snippets = results.get("snippets", {})
    compil_ok = results.get("compilation_ok", True)
    classes_cnt = results.get("classes_count", 0)
    methods_cnt = results.get("methods_count", 0)
    priv_fields = results.get("private_fields", 0)
    pub_fields = results.get("public_fields", 0)
    javadoc_cnt = results.get("javadoc_blocks", 0)
    ai_prob = results.get("ai_analysis", {}).get("probability_pct", 0)

    # 1. Bilan de Progression & Analyse Pédagogique
    if total >= 16.0:
        bilan = (
            f"Excellente prestation sur le projet Java / Programmation Orientée Objet ({total:.1f}/20). "
            f"L'architecture objet est mature avec {classes_cnt} classe(s) et {methods_cnt} méthode(s) bien ciblées. "
            f"La compilation `javac` est sans faille et les principes fondamentaux de conception objet sont solidement appliqués."
        )
    elif total >= 13.0:
        bilan = (
            f"Bon travail d'ensemble ({total:.1f}/20). Le socle de programmation orientée objet est acquis "
            f"({len(java_files)} fichier(s) sources `.java`). Le code est fonctionnel et la modélisation objet "
            f"répond correctement aux exigences du cahier des charges."
        )
    elif total >= 10.0:
        bilan = (
            f"Socle technique minimal validé ({total:.1f}/20). Les classes principales compilent et s'exécutent, "
            f"mais la modélisation gagnerait à renforcer l'encapsulation et le découpage modulaire pour dépasser les réflexes procéduraux."
        )
    else:
        bilan = (
            f"Travail incomplet ou difficultés d'assimilation des concepts objet ({total:.1f}/20). "
            f"{'Des erreurs de compilation `javac` bloquent l\'exécution' if not compil_ok else 'L\'architecture objet reste trop rudimentaire'}. "
            f"Une reprise méthodique de l'encapsulation et de la structure de classe s'impose."
        )

    # 2. Points Forts Observés
    points_forts_list = []
    if compil_ok and len(java_files) > 0:
        points_forts_list.append("Compilation `javac` sans erreur sur l'ensemble de l'arborescence des sources.")
    if priv_fields > 0 and pub_fields == 0:
        points_forts_list.append(f"Encapsulation exemplaire : {priv_fields} champ(s) strictement `private` protégés par des accesseurs.")
    elif priv_fields >= pub_fields and priv_fields > 0:
        points_forts_list.append(f"Bonne pratique d'encapsulation avec {priv_fields} champ(s) déclarés `private`.")
    if javadoc_cnt >= 2:
        points_forts_list.append(f"Documentation soignée au format Javadoc ({javadoc_cnt} bloc(s) de documentation).")
    if results.get("has_tests"):
        points_forts_list.append("Présence d'une démarche de test dédiée pour valider les comportements attendus.")

    if snippets.get("highlights"):
        for hl in snippets["highlights"]:
            points_forts_list.append(
                f"**{hl.get('title', 'Extrait remarquable')}** (`{hl.get('file', '')}`, ligne {hl.get('line', '')}) :\n"
                f"```java\n{hl.get('code', '').strip()}\n```"
            )

    if not points_forts_list:
        points_forts_list.append("Présence de classes Java compilables.")
    points_forts = "\n\n".join(f"- {p}" if not p.startswith("**") else p for p in points_forts_list)

    # 3. Axes d'Amélioration & Conseils Méthodologiques
    axes_list = []
    if not compil_ok:
        axes_list.append("Corriger en priorité les erreurs de compilation signalées par `javac`.")
        if snippets.get("compilation_errors"):
            for ce in snippets["compilation_errors"]:
                axes_list.append(
                    f"Extrait concerné (`{ce.get('file')}`, ligne {ce.get('line')}) :\n"
                    f"```java\n{ce.get('code', '').strip()}\n```"
                )

    if pub_fields > 0:
        axes_list.append(f"Proscrire les attributs `public` ({pub_fields} détecté(s)) : déclarer systématiquement les attributs en `private` pour garantir l'encapsulation.")
    if javadoc_cnt == 0:
        axes_list.append("Documenter les classes et méthodes publiques avec des commentaires Javadoc normalisés (`/** ... */`).")
    if not results.get("has_tests"):
        axes_list.append("Ajouter des classes de test unitaires (JUnit ou classe avec `public static void main`) pour valider chaque composant.")

    if snippets.get("anti_patterns"):
        for ap in snippets["anti_patterns"]:
            axes_list.append(
                f"**{ap.get('title', 'Anti-pattern')}** (`{ap.get('file', '')}`, ligne {ap.get('line', '')}) :\n"
                f"```java\n{ap.get('code', '').strip()}\n```"
            )

    if ai_prob >= 30:
        axes_list.append(f"**Intégrité académique ({ai_prob}% suspicion IA)** : Privilégier la conception personnelle afin de maîtriser réellement les idiomes objet.")

    if not axes_list:
        axes_list.append("Poursuivre sur cette dynamique en explorant les patrons de conception (Design Patterns) et la programmation générique.")
    axes_amelioration = "\n\n".join(f"- {a}" if not a.startswith("**") and not a.startswith("Extrait") else a for a in axes_list)

    # 4. Commentaire de l'Enseignant
    if total >= 16.0:
        commentaire = "Excellente maîtrise des paradigmes de la programmation orientée objet en Java. L'approche est rigoureuse et professionnelle, félicitations !"
    elif total >= 12.0:
        commentaire = "Bon investissement dans ce TP. Les bases objet sont bien intégrées. Continuez à soigner l'encapsulation et la modularité de vos classes."
    elif total >= 9.0:
        commentaire = "Travail en progrès. Prenez le temps de bien modéliser vos classes sur papier avant de coder pour consolider l'encapsulation."
    else:
        commentaire = "Ne baissez pas les bras. Les débuts en programmation objet demandent un temps d'adaptation. N'hésitez pas à solliciter l'aide des tuteurs pour lever vos blocages."

    return bilan, points_forts, axes_amelioration, commentaire


def format_report_markdown(results: dict) -> str:
    student_id = results.get("student_id") or results.get("login", "Inconnu")
    student_name = results.get("student_name") or results.get("name", student_id)
    group = results.get("student_group", "")
    group_line = f"\n- **Groupe :** {group}" if group else ""
    assignment = results.get("assignment", "Java / POO")
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    java_files = results.get("java_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "10/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    # Table des scores
    note_sans_malus = scores.get("note_technique_brute", round(scores.get('compilation', 0) + scores.get('architecture', 0) + scores.get('style', 0) + scores.get('tests', 0), 1))
    compil_s = scores.get('compilation', 0.0)
    arch_s = scores.get('architecture', 0.0)
    style_s = scores.get('style', 0.0)
    tests_s = scores.get('tests', 0.0)

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Critères & Observations |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Compilation Java** | **{compil_s:.1f}** | / 6.0 | {'Compilation réussie sans erreur javac' if results.get('compilation_ok') else 'Erreurs de compilation relevées'} |
| **Axe 2 : Modélisation Objet & Encapsulation** | **{arch_s:.1f}** | / 8.0 | Découpage des classes, champs privés et contrats d'interface |
| **Axe 3 : Javadoc & Conventions Java** | **{style_s:.1f}** | / 4.0 | Documentation formelle, lisibilité et conventions de casse |
| **Axe 4 : Démarche de Test** | **{tests_s:.1f}** | / 2.0 | Présence de classes de validation ou méthodes de test |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20.0** | **Somme technique brute des 4 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20.0** | **Bilan global de l'évaluation** |"""

    # Calcul des radars et profil dominant
    radars = generate_mermaid_radars(results, student_name or student_id)
    radars_visualisation = radars.get("radars_markdown", "")
    profil = radars.get("profil_dominant", "Standard")

    # Intégrité & stylométrie
    ai_data = results.get("ai_analysis") or {}
    ai_level = ai_data.get("level", "Faible (0% - Code authentique)")

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
        "skill_name": "eval-java-sample",
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
        report_text = f"""# Fiche d'Évaluation : Java / Programmation Orientée Objet

### Étudiant : **{student_name}**

- **Login / ID :** `{student_id}`
- **Module :** Java / Programmation Orientée Objet{group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date}
- **Skill appliqué :** `eval-java-sample` (dernière modification : {skill_date})
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


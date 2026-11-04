#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple C).
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
    """Construit les 4 sections pédagogiques normalisées CodeLab pour C."""
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    c_files = results.get("c_files", [])
    snippets = results.get("snippets", {})
    syntax_errs = results.get("syntax_errors", {})
    fn_count = len(results.get("functions", []))
    comments_cnt = results.get("comments_count", 0)
    assert_count = results.get("asserts_count", 0)
    ai_prob = results.get("ai_analysis", {}).get("probability_pct", 0)

    # 1. Bilan de Progression & Analyse Pédagogique
    if total >= 16.0:
        bilan = (
            f"Excellente prestation en langage C et algorithmique ({total:.1f}/20). "
            f"La maîtrise des concepts fondamentaux (pointeurs, gestion mémoire, types stricts et structuration fonctionnelle) "
            f"est de très bon niveau. Les {len(c_files)} fichier(s) `.c`/`.h` compilent proprement sous Clang/GCC sans avertissement majeur."
        )
    elif total >= 13.0:
        bilan = (
            f"Bon travail d'ensemble ({total:.1f}/20). Les structures algorithmiques de base et le découpage modulaire "
            f"({fn_count} fonction(s) déclarée(s)) sont acquis. Le programme résout le problème de manière stable et cohérente."
        )
    elif total >= 10.0:
        bilan = (
            f"Socle technique minimal acquis ({total:.1f}/20). Le programme fonctionne dans les cas nominaux, "
            f"mais conserve des fragilités sur la portée des variables, l'anticipation des cas limites ou la décomposition modulaire."
        )
    else:
        bilan = (
            f"Travail inachevé ou difficultés d'assimilation de la syntaxe C ({total:.1f}/20). "
            f"{'Des erreurs de compilation bloquantes subsistent' if syntax_errs else 'Le code reste trop partiel pour valider les objectifs'}. "
            f"Une reprise méthodique des pointeurs, des prototypes et de la structure de base est vivement conseillée."
        )

    # 2. Points Forts Observés
    points_forts_list = []
    if not syntax_errs and len(c_files) > 0:
        points_forts_list.append(f"Tous les fichiers C ({len(c_files)}) compilent avec succès.")
    if fn_count >= 3:
        points_forts_list.append(f"Bonne modularité avec {fn_count} fonctions spécialisées clairement délimitées.")
    if comments_cnt >= 2:
        points_forts_list.append(f"Clarté du code renforcée par {comments_cnt} bloc(s) de commentaires explicatifs.")
    if assert_count > 0:
        points_forts_list.append(f"Présence d'assertions de validation défensive ({assert_count} élément(s)).")

    if snippets.get("highlights"):
        for hl in snippets["highlights"]:
            points_forts_list.append(
                f"**Fonction `{hl.get('function', '')}` dans `{hl.get('file', '')}`** :\n"
                f"```c\n{hl.get('code', '').strip()}\n```"
            )

    if not points_forts_list:
        points_forts_list.append("Présence de code source C exécutable.")
    points_forts = "\n\n".join(f"- {p}" if not p.startswith("**") else p for p in points_forts_list)

    # 3. Axes d'Amélioration & Conseils Méthodologiques
    axes_list = []
    if syntax_errs:
        for fname, err in syntax_errs.items():
            axes_list.append(f"**Erreur de compilation à corriger** dans `{fname}` : `{err}`.")
        if snippets.get("compilation_errors"):
            for sc in snippets["compilation_errors"]:
                axes_list.append(
                    f"Extrait concerné (`{sc.get('file')}`) :\n"
                    f"```c\n{sc.get('code', '').strip()}\n```"
                )

    if fn_count < 2:
        axes_list.append("Éviter de tout concentrer dans la fonction `main` : créer des sous-fonctions dédiées avec passage d'arguments explicite.")
    if comments_cnt == 0:
        axes_list.append("Documenter l'en-tête de chaque fonction avec le rôle des paramètres et de la valeur de retour.")
    if assert_count == 0:
        axes_list.append("Ajouter des vérifications aux limites (défense contre pointeurs NULL, débordements de tableaux).")

    if snippets.get("anti_patterns"):
        for ap in snippets["anti_patterns"]:
            axes_list.append(
                f"**{ap.get('title', 'Anti-pattern')}** (`{ap.get('file', '')}`, ligne {ap.get('line', '')}) :\n"
                f"```c\n{ap.get('code', '').strip()}\n```"
            )

    if ai_prob >= 30:
        axes_list.append(f"**Intégrité académique ({ai_prob}% suspicion IA)** : Privilégier la programmation manuelle et personnelle pour forger des réflexes solides en C.")

    if not axes_list:
        axes_list.append("Poursuivre sur cette dynamique en approfondissant l'allocation dynamique (`malloc`/`free`) et les structures de données complexes.")
    axes_amelioration = "\n\n".join(f"- {a}" if not a.startswith("**") and not a.startswith("Extrait") else a for a in axes_list)

    # 4. Commentaire de l'Enseignant
    if total >= 16.0:
        commentaire = "Remarquable parcours en C. La rigueur apportée à la gestion mémoire et au découpage algorithmique est exemplaire. Continuez sur cette belle lancée !"
    elif total >= 12.0:
        commentaire = "Travail sérieux et appliqué. Les fondamentaux du langage C sont en place ; poursuivez la pratique pour automatiser les bonnes pratiques de programmation défensive."
    elif total >= 9.0:
        commentaire = "Des bases sont acquises mais des lacunes subsistent sur la gestion des types et des erreurs de compilation. Prenez le temps de débugger pas à pas avant de complexifier."
    else:
        commentaire = "Le langage C est exigeant et ne pardonne pas les approximations de syntaxe. Sollicitez activement les tuteurs et reprenez les exercices d'application pour débloquer la situation."

    return bilan, points_forts, axes_amelioration, commentaire


def format_report_markdown(results: dict) -> str:
    student_id = results.get("student_id") or results.get("login", "Inconnu")
    student_name = results.get("student_name") or results.get("name", student_id)
    group = results.get("student_group", "")
    group_line = f"\n- **Groupe :** {group}" if group else ""
    assignment = results.get("assignment", "C / Algorithmique")
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    c_files = results.get("c_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "14/10/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    # Table des scores
    note_sans_malus = scores.get("note_technique_brute", round(scores.get('syntax', 0) + scores.get('architecture', 0) + scores.get('style', 0) + scores.get('tests', 0), 1))
    syntax_s = scores.get('syntax', 0.0)
    arch_s = scores.get('architecture', 0.0)
    style_s = scores.get('style', 0.0)
    tests_s = scores.get('tests', 0.0)

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Critères & Observations |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Compilation & Syntaxe** | **{syntax_s:.1f}** | / 6.0 | {results.get('valid_c_count', 0)}/{len(c_files)} fichier(s) compilables sans erreur |
| **Axe 2 : Architecture & Conception** | **{arch_s:.1f}** | / 8.0 | Structuration modulaire en fonctions et logique de contrôle |
| **Axe 3 : Style & Lisibilité** | **{style_s:.1f}** | / 4.0 | Clarté du nommage, indentation régulière et commentaires |
| **Axe 4 : Démarche de Test** | **{tests_s:.1f}** | / 2.0 | Présence d'assertions ou fonctions de test |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20.0** | **Somme technique brute des 4 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20.0** | **Bilan global de l'évaluation** |"""

    # Calcul des radars et profil dominant
    radars = generate_mermaid_radars(results, student_name or student_id)
    radars_visualisation = radars.get("radars_markdown", "")
    profil = radars.get("profil_dominant", "Standard")

    # Intégrité & stylométrie
    ai_det = results.get("ai_analysis") or results.get("ai_detection") or {}
    ai_level = ai_det.get("level") or ai_det.get("verdict", "Faible (0% - Code authentique)")

    dinfo = results.get("dates_info", {})
    dist_days = dinfo.get("distinct_days", 0)
    chronometrie_courte = f"Régulière ({dist_days} séance(s))" if dist_days > 0 else "Standard"

    ind_stats = ai_det.get("indentation_stats", {})
    tot_tabs = ind_stats.get("total_tabs", 0)
    tot_spaces = ind_stats.get("total_spaces", 0)
    if tot_spaces > tot_tabs and tot_spaces >= 15:
        stylometrie_courte = f"⚠️ Rupture 4 espaces ({tot_spaces} espaces vs {tot_tabs} tabs)"
    else:
        stylometrie_courte = f"Conforme CodeLab ({tot_tabs} tabs)" if tot_tabs > 0 else "Standard"

    plag = results.get("plagiarism_alert", {})
    if plag and plag.get("max_sim", 0) >= 75.0:
        similarite_courte = f"⚠️ Partage avec {plag.get('partner', 'Camarade')} ({plag.get('max_sim', 0)}%)"
        similarite_block = f"> [!CAUTION]\n> **Suspicion de code partagé** avec **{plag.get('partner', 'Camarade')}** (similarité : **{plag.get('max_sim', 0)}%**)."
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
        "skill_name": "eval-c-sample",
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
        report_text = f"""# Fiche d'Évaluation : C / Algorithmique

### Étudiant : **{student_name}**

- **Login / ID :** `{student_id}`
- **Module :** C / Algorithmique{group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date}
- **Skill appliqué :** `eval-c-sample` (dernière modification : {skill_date})
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


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: generate_report.py <student_folder> <output_path>")
        sys.exit(1)
    s_folder = sys.argv[1]
    out_file = sys.argv[2]
    generate_individual_report(s_folder, out_file)
    print(f"[DONE] Rapport individuel généré dans : {out_file}")


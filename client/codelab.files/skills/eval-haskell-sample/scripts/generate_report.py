#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_report.py - Générateur de fiches d'évaluation individuelles en Markdown (Exemple Haskell).
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
    """Construit les 4 sections pédagogiques normalisées CodeLab pour Haskell."""
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    hs_files = results.get("hs_files", [])
    snippets = results.get("snippets", {})
    comp_errs = results.get("compilation_errors", {})
    fn_count = len(results.get("functions", []))
    sig_count = results.get("type_signatures_count", 0)
    pm_count = results.get("pattern_matching_count", 0)
    hof_count = results.get("higher_order_count", 0)
    tests_count = results.get("tests_count", 0)
    ai_prob = results.get("ai_analysis", {}).get("probability_pct", 0)

    # 1. Bilan de Progression & Analyse Pédagogique
    if total >= 16.0:
        bilan = (
            f"Excellente maîtrise de la programmation fonctionnelle en Haskell ({total:.1f}/20). "
            f"Les {len(hs_files)} fichier(s) analysé(s) compilent proprement et exploitent avec justesse "
            f"le typage explicite, le filtrage par motif et la décomposition en fonctions pures."
        )
    elif total >= 13.0:
        bilan = (
            f"Bon travail régulier en Haskell ({total:.1f}/20). Les mécanismes clés du paradigme fonctionnel "
            f"({fn_count} fonction(s) définie(s), {sig_count} signature(s) explicite(s)) sont correctement mis en œuvre."
        )
    elif total >= 10.0:
        bilan = (
            f"Socle technique minimal validé ({total:.1f}/20). Le code présente les bases attendues mais peut gagner "
            f"en idiomaticité fonctionnelle (signatures de types, filtrage par motif exhaustif, fonctions d'ordre supérieur)."
        )
    else:
        bilan = (
            f"Résultats fragiles ou travail inachevé ({total:.1f}/20). Des erreurs de typage ou de syntaxe "
            f"empêchent la validation complète. Reprenez méthodiquement les signatures et les cas de base."
        )

    # 2. Points Forts Observés
    points_forts_list = []
    if not comp_errs and len(hs_files) > 0:
        points_forts_list.append("Compilation et vérification de types GHC validées sans erreur bloquante.")
    if sig_count >= 2:
        points_forts_list.append(f"Bonne discipline de typage avec {sig_count} signature(s) explicite(s) (`::`).")
    if pm_count >= 1:
        points_forts_list.append("Utilisation pertinente du filtrage par motif (*pattern matching*) ou d'expressions `case`.")
    if hof_count >= 1:
        points_forts_list.append(f"Recours aux fonctions d'ordre supérieur (`map`, `filter`, `fold`, `.`) ({hof_count} occurrence(s)).")
    if tests_count >= 2:
        points_forts_list.append(f"Démarche de validation présente ({tests_count} point(s) de test identifié(s)).")

    if snippets.get("highlights"):
        for hl in snippets["highlights"]:
            points_forts_list.append(
                f"**{hl.get('title', 'Extrait remarquable')}** (`{hl.get('file', '')}`, ligne {hl.get('line', '')}) :\n"
                f"```haskell\n{hl.get('code', '').strip()}\n```"
            )

    if not points_forts_list:
        points_forts_list.append("Présence d'une base de code Haskell exploitable.")
    points_forts = "\n\n".join(f"- {p}" if not p.startswith("**") else p for p in points_forts_list)

    # 3. Axes d'Amélioration & Conseils Méthodologiques
    axes_list = []
    if comp_errs:
        for fname, err in comp_errs.items():
            axes_list.append(f"**Erreur de compilation/typage à corriger** dans `{fname}` : `{err}`.")
        if snippets.get("compilation_errors"):
            for ce in snippets["compilation_errors"]:
                axes_list.append(
                    f"Extrait concerné (`{ce.get('file')}`) :\n"
                    f"```haskell\n{ce.get('code', '').strip()}\n```"
                )

    if sig_count < max(1, fn_count // 2):
        axes_list.append("Déclarer systématiquement la signature de type (`f :: ...`) avant chaque définition de fonction.")
    if pm_count == 0 and fn_count >= 1:
        axes_list.append("Privilégier le filtrage par motif et les gardes (`|`) pour rendre les cas de base explicites et exhaustifs.")
    if tests_count == 0:
        axes_list.append("Ajouter des jeux d'essais (fonction `main` de vérification, fonctions `test*` ou propriétés `prop_*`).")

    if snippets.get("anti_patterns"):
        for ap in snippets["anti_patterns"]:
            axes_list.append(
                f"**{ap.get('title', 'Anti-pattern')}** (`{ap.get('file', '')}`, ligne {ap.get('line', '')}) :\n"
                f"```haskell\n{ap.get('code', '').strip()}\n```"
            )

    if ai_prob >= 30:
        axes_list.append(f"**Intégrité académique ({ai_prob}% suspicion IA)** : Privilégier une écriture personnelle et les outils vus en cours sans extensions avancées hors programme.")

    if not axes_list:
        axes_list.append("Poursuivre sur cette dynamique en explorant les types algébriques personnalisés et les tests par propriétés (QuickCheck).")
    axes_amelioration = "\n\n".join(f"- {a}" if not a.startswith("**") and not a.startswith("Extrait") else a for a in axes_list)

    # 4. Commentaire de l'Enseignant
    if total >= 16.0:
        commentaire = "Félicitations pour cet excellent travail fonctionnel. Votre maîtrise du typage et du filtrage par motif est très convaincante. Continuez ainsi !"
    elif total >= 12.0:
        commentaire = "Bon investissement dans le module Haskell. Les fondamentaux sont bien assimilés ; soignez l'exhaustivité des motifs et vos jeux de tests pour franchir un palier supplémentaire."
    elif total >= 9.0:
        commentaire = "Travail encourageant mais encore perfectible. Prenez l'habitude d'écrire la signature de chaque fonction avant son corps et de tester vos cas de base dans GHCi."
    else:
        commentaire = "Ne vous découragez pas face au système de types de Haskell. Lisez attentivement les messages de GHC et n'hésitez pas à solliciter l'équipe pédagogique en séance."

    return bilan, points_forts, axes_amelioration, commentaire


def format_report_markdown(results: dict) -> str:
    student_id = results.get("student_id") or results.get("login", "Inconnu")
    student_name = results.get("student_name") or results.get("name", student_id)
    group = results.get("student_group", "")
    group_line = f"\n- **Groupe :** {group}" if group else ""
    assignment = results.get("assignment", "Haskell / Programmation Fonctionnelle")
    scores = results.get("scores", {})
    total = float(scores.get("total", 0.0))
    hs_files = results.get("hs_files", [])

    eval_date = results.get("eval_date") or datetime.now().strftime("%d/%m/%Y à %H:%M")
    skill_date = results.get("skill_last_modified", "27/11/2026")
    engine_name = results.get("eval_engine", "Moteur Déterministe CodeLab")

    comp_s = scores.get("compilation", 0.0)
    arch_s = scores.get("architecture", 0.0)
    style_s = scores.get("style", 0.0)
    tests_s = scores.get("tests", 0.0)
    note_sans_malus = scores.get("note_technique_brute", round(comp_s + arch_s + style_s + tests_s, 1))

    scores_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Critères & Observations |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Compilation & Typage GHC** | **{comp_s:.1f}** | / 6.0 | {results.get('valid_hs_count', 0)}/{len(hs_files)} fichier(s) compilé(s) sans erreur |
| **Axe 2 : Conception Fonctionnelle** | **{arch_s:.1f}** | / 8.0 | Signatures (`::`), récursivité, filtrage par motif, ordre supérieur |
| **Axe 3 : Style & Exhaustivité** | **{style_s:.1f}** | / 4.0 | Warnings `-Wall`, commentaires et absence d'anti-patterns |
| **Axe 4 : Démarche de Test** | **{tests_s:.1f}** | / 2.0 | Fonctions de test, propriétés `prop_*` ou `main` de validation |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | **/ 20.0** | **Somme technique brute des 4 axes** |
| **TOTAL GÉNÉRAL (Avec malus)** | **{total:.1f}** | **/ 20.0** | **Bilan global de l'évaluation** |"""

    radars = generate_mermaid_radars(results, student_name or student_id)
    radars_visualisation = radars.get("radars_markdown", "")
    profil = radars.get("profil_display", radars.get("profil_dominant", "Standard"))

    ai_data = results.get("ai_analysis") or {}
    ai_level = ai_data.get("level", "Faible (0% - Code authentique)")

    dinfo = results.get("dates_info", {})
    dist_days = dinfo.get("distinct_days", 0)
    chronometrie_courte = f"Régulière ({dist_days} séance(s))" if dist_days > 0 else "Standard"

    ind_stats = ai_data.get("indentation_stats", {})
    tot_tabs = ind_stats.get("total_tabs", 0)
    tot_spaces = ind_stats.get("total_spaces", 0)
    if tot_tabs > 0 and tot_spaces > 0:
        stylometrie_courte = f"Mixte ({tot_spaces} espaces / {tot_tabs} tabs)"
    elif tot_spaces > 0:
        stylometrie_courte = f"Standard Haskell ({tot_spaces} lignes indentées en espaces)"
    else:
        stylometrie_courte = "Standard"

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

    bilan, points_forts, axes_amelioration, commentaire = build_deterministic_sections(results)

    template_vars = {
        "student_name": student_name,
        "student_id": student_id,
        "group_line": group_line,
        "assignment": assignment,
        "profil": profil,
        "eval_date": eval_date,
        "skill_name": "eval-haskell-sample",
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
        "commentaire": commentaire,
    }

    tmpl_file = SKILL_ROOT / "report_template.md"
    report_text = None
    if tmpl_file.is_file():
        try:
            report_text = render_template_str(tmpl_file.read_text(encoding="utf-8"), template_vars)
        except Exception:
            report_text = None

    if not report_text:
        report_text = f"""# Fiche d'Évaluation : Haskell / Programmation Fonctionnelle

### Étudiant : **{student_name}**

- **Login / ID :** `{student_id}`
- **Module :** Haskell / Programmation Fonctionnelle{group_line}
- **Profil :** {profil}

---

### 📋 Cartouche de Traçabilité & Synthèse d'Intégrité

- **Date de l'évaluation :** {eval_date}
- **Skill appliqué :** `eval-haskell-sample` (dernière modification : {skill_date})
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
    generate_individual_report(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    print(f"[DONE] Rapport individuel généré dans : {sys.argv[2]}")

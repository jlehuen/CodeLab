#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
recalculate_grades.py - Recalcul dynamique des évaluations CodeLab selon les pondérations du skill.
Permet d'ajuster les pondérations d'axes, les plafonds et les malus anti-triche (IA et similarités)
directement depuis un fichier weights.json, sans ré-évaluer, sans Clang et sans appel LLM.

Usage:
    python3 recalculate_grades.py <session_eval_dir> [--weights /path/to/weights.json]
"""

import os
import sys
import json
import csv
import re
import statistics
from pathlib import Path

DEFAULT_WEIGHTS = {
    "version": "1.1",
    "description": "Pondérations et seuils de notation CodeLab (10 + 8 + 2 = 20)",
    "axes": {
        "axe1_progression_max": 10.0,
        "axe2_architecture_max": 8.0,
        "axe3_style_max": 2.0
    },
    "progression": {
        "socle_max": 3.0,
        "tp5_fangio_max": 3.0,
        "tp6_knossos_max": 4.0,
        "malus_saut_etape": 2.0,
        "plafond_sans_tp6": 5.5,
        "plafond_sans_tp5_tp6": 4.0
    },
    "anti_triche": {
        "assistance_ia": {
            "malus_eleve": 2.0,
            "plafond_note_eleve": 12.0,
            "malus_modere": 1.0,
            "plafond_note_modere": 14.0
        },
        "similarite": {
            "seuil_detection_pct": 75.0,
            "malus_1_fichier": 0.5,
            "malus_2_fichiers": 1.0,
            "malus_3_fichiers_ou_plus": 2.0,
            "malus_max_total": 2.0
        },
        "malus_max_cumule_global": 3.5
    },
    "profils": {
        "note_min_excellence": 16.0,
        "interdire_excellence_si_ia": True,
        "interdire_excellence_si_similarite": True
    }
}

def load_weights(session_eval_dir, custom_weights_path=None):
    """Charge les pondérations depuis weights.json avec recherche hiérarchique."""
    candidates = []
    if custom_weights_path and os.path.isfile(custom_weights_path):
        candidates.append(Path(custom_weights_path))

    s_dir = Path(session_eval_dir).resolve()
    # 1. Dans le dossier de session d'évaluation
    candidates.append(s_dir / "weights.json")

    # 2. Dans le dossier du skill eval-introprog
    user_skill = Path.home() / "codelab.files" / "skills" / "eval-introprog" / "weights.json"
    candidates.append(user_skill)
    candidates.append(Path.home() / "codelab.files" / "skills" / "weights.json")

    # 3. Dans le dossier parent du présent script
    candidates.append(Path(__file__).resolve().parent / "weights.json")
    candidates.append(Path(__file__).resolve().parent.parent / "weights.json")

    for c in candidates:
        if c.is_file():
            try:
                with open(c, 'r', encoding='utf-8') as fp:
                    data = json.load(fp)
                    print(f"[INFO] Pondérations chargées depuis : {c}")
                    return data, str(c)
            except Exception as e:
                print(f"[WARN] Erreur lors de la lecture de {c}: {e}", file=sys.stderr)

    print("[INFO] Utilisation des pondérations par défaut intégrées.")
    return DEFAULT_WEIGHTS, "(défaut en mémoire)"

def count_similar_files(sim_alert_str):
    """Compte le nombre de fichiers uniques impliqués dans une alerte de similarité."""
    if not sim_alert_str:
        return 0
    # Recherche les motifs type 'sur TP2E2a.c, TP1E3.c' ou '.py' ou '.java'
    files = set(re.findall(r'[a-zA-Z0-9_\-\.]+\.(?:c|h|py|java)', sim_alert_str))
    return len(files)

def update_student_report_markdown(report_file, st_info, a1_max=10.0, a2_max=8.0, a3_max=2.0, a4_max=0.0):
    """Met à jour une fiche individuelle Markdown selon le format canonique 3 axes."""
    try:
        content = report_file.read_text(encoding='utf-8', errors='ignore')
        s_name = st_info.get("name") or "Étudiant"
        s_id = st_info.get("login") or ""
        s_profil = st_info.get("profil") or "Standard"

        # 1. Mise à jour ou ajout de l'en-tête étudiant
        if re.search(r'###\s*Étudiant\s*:', content, re.IGNORECASE):
            content = re.sub(
                r'(\*\*Profil\s*:\*\*\s*)([^\r\n]+)',
                rf'\g<1>{s_profil}',
                content
            )
        else:
            id_block = f"### Étudiant : **{s_name}**\n\n- **Login / ID :** `{s_id}`\n- **Module :** Introduction à la programmation (L1 Informatique)\n- **Profil :** {s_profil}\n\n---\n"
            content = re.sub(r'(#[^\n]+\n+)', rf'\g<1>{id_block}\n', content, count=1)

        # 2. Construction de la table canonique
        if a4_max <= 0:
            app_a1 = "Parcours séquentiel régulier."
            if st_info['a1'] == 0.0: app_a1 = "Aucun travail rendu sur CodeLab."
            elif st_info['a1'] >= 8.5: app_a1 = "Parcours séquentiel complet jusqu'à Knossos."
            elif st_info['a1'] >= 5.5: app_a1 = "Socle validé et projet Fangio atteint."

            app_a2 = "Modularité et qualité algorithmique."
            if st_info['a2'] == 0.0: app_a2 = "Aucun programme soumis."
            elif st_info['a2'] >= 6.0: app_a2 = "Excellente structuration modulaire et réutilisation DRY."
            elif st_info['a2'] <= 2.0: app_a2 = "Modularité amorcée mais structuration perfectible."

            app_a3 = "Style, typage strict et conventions CodeLab."
            if st_info['a3'] == 0.0: app_a3 = "Aucun code source à analyser."
            elif st_info['a3'] >= 1.6: app_a3 = "Documentation soignée et conventions respectées."

            m_old_table = re.search(r'\|[ \t]*(?:Axe[ \t]*d[’\']?[EÉeé]valuation|Axe)[ \t]*\|[\s\S]*?\|\s*\*\*(?:TOTAL GÉNÉRAL|Total Global|NOTE FINALE)\*\*[^\n]*\|', content, re.IGNORECASE)
            if m_old_table:
                old_t = m_old_table.group(0)
                m1 = re.search(r'\|\s*\*\*Axe 1[^\n|]*\|[^|]+\|[^|]+\|\s*([^|\n]+)\|', old_t)
                m2 = re.search(r'\|\s*\*\*Axe 2[^\n|]*\|[^|]+\|[^|]+\|\s*([^|\n]+)\|', old_t)
                m3 = re.search(r'\|\s*\*\*Axe 3[^\n|]*\|[^|]+\|[^|]+\|\s*([^|\n]+)\|', old_t)
                if m1 and m1.group(1).strip() and not m1.group(1).strip().startswith('/'):
                    app_a1 = m1.group(1).strip()
                if m2 and m2.group(1).strip() and not m2.group(1).strip().startswith('/'):
                    app_a2 = m2.group(1).strip()
                if m3 and m3.group(1).strip() and not m3.group(1).strip().startswith('/'):
                    app_a3 = m3.group(1).strip()

            note_sans_malus = st_info.get('raw_subtotal', round(st_info['a1'] + st_info['a2'] + st_info['a3'], 1))
            new_table = f"""| Axe d'Évaluation | Note Attribuée | Barème Officiel | Appréciation Synthétique |
| :--- | :---: | :---: | :--- |
| **Axe 1 : Progression & Couverture des TP** | **{st_info['a1']:.1f}** | / {a1_max:.1f} | {app_a1} |
| **Axe 2 : Architecture & Qualité Algorithmique** | **{st_info['a2']:.1f}** | / {a2_max:.1f} | {app_a2} |
| **Axe 3 : Style, Rigueur & Bonnes Pratiques** | **{st_info['a3']:.1f}** | / {a3_max:.1f} | {app_a3} |
| **Note sans malus (IA / Similarité)** | **{note_sans_malus:.1f}** | / 20.0 | Somme technique brute des 3 axes |
| **TOTAL GÉNÉRAL (Avec malus)** | **{st_info['new']:.1f}** | **/ 20.0** | **{s_profil}** |"""

            content = re.sub(
                r'\|[ \t]*(?:Axe[ \t]*d[’\']?[EÉeé]valuation|Axe)[ \t]*\|[\s\S]*?\|\s*\*\*(?:TOTAL GÉNÉRAL|Total Global|NOTE FINALE)[^\n]*\|',
                new_table,
                content,
                flags=re.IGNORECASE
            )
            content = re.sub(r'###\s*Tableau[^\n]*', '### Tableau Récapitulatif des Notes Calibrées', content)

        report_file.write_text(content, encoding='utf-8')
    except Exception as e:
        print(f"[WARN] Impossible de mettre à jour {report_file}: {e}", file=sys.stderr)

def recalculate_session(session_dir, weights_path=None):
    """Recalcule toutes les notes d'une session à partir de son CSV et fiches Markdown."""
    s_path = Path(session_dir).resolve()
    if not s_path.is_dir():
        print(f"[FAIL] Répertoire introuvable : {session_dir}", file=sys.stderr)
        return False

    weights, w_source = load_weights(s_path, weights_path)

    # Paramètres des axes
    axes = weights.get("axes", {})
    a1_max = float(axes.get("axe1_progression_max", 10.0))
    a2_max = float(axes.get("axe2_architecture_max", 8.0))
    a3_max = float(axes.get("axe3_style_max", 2.0))
    a4_max = float(axes.get("axe4_tests_max", 0.0))
    sum_axes = a1_max + a2_max + a3_max + a4_max
    normalisation_factor = 20.0 / sum_axes if sum_axes > 0 else 1.0

    if abs(sum_axes - 20.0) > 0.01:
        print(f"[INFO] Somme des axes = {sum_axes} pts. Facteur de normalisation automatique vers 20 pts : {normalisation_factor:.4f}")
    else:
        if a4_max > 0:
            print(f"[INFO] Barème standard sur 20 pts (Axe1: {a1_max}, Axe2: {a2_max}, Axe3: {a3_max}, Axe4: {a4_max}).")
        else:
            print(f"[INFO] Barème standard direct sur 20 pts (Axe1: {a1_max}, Axe2: {a2_max}, Axe3: {a3_max}).")

    # Anti-triche
    anti = weights.get("anti_triche", {})
    ia_cfg = anti.get("assistance_ia", {})
    ia_malus_eleve = float(ia_cfg.get("malus_eleve", 2.0))
    ia_plafond_eleve = float(ia_cfg.get("plafond_note_eleve", 12.0))
    ia_malus_modere = float(ia_cfg.get("malus_modere", 1.0))
    ia_plafond_modere = float(ia_cfg.get("plafond_note_modere", 14.0))

    sim_cfg = anti.get("similarite", {})
    sim_m1 = float(sim_cfg.get("malus_1_fichier", 0.5))
    sim_m2 = float(sim_cfg.get("malus_2_fichiers", 1.0))
    sim_m3 = float(sim_cfg.get("malus_3_fichiers_ou_plus", 2.0))
    sim_max_total = float(sim_cfg.get("malus_max_total", 2.0))
    malus_cumule_max = float(anti.get("malus_max_cumule_global", 3.5))

    prof_cfg = weights.get("profils", {})
    note_min_excell = float(prof_cfg.get("note_min_excellence", 16.0))
    interdire_excell_ia = bool(prof_cfg.get("interdire_excellence_si_ia", True))
    interdire_excell_sim = bool(prof_cfg.get("interdire_excellence_si_similarite", True))

    # Trouver le fichier notes_*.csv
    csv_candidates = list(s_path.glob("notes_*.csv"))
    if not csv_candidates:
        csv_candidates = list(s_path.glob("*.csv"))

    if not csv_candidates:
        print(f"[FAIL] Aucun fichier notes_*.csv trouvé dans {s_path}", file=sys.stderr)
        return False

    csv_file = csv_candidates[0]
    print(f"[INFO] Traitement du fichier de notes : {csv_file.name}")

    rows = []
    with open(csv_file, 'r', encoding='utf-8') as fp:
        reader = csv.reader(fp)
        headers = next(reader, None)
        if not headers:
            print("[FAIL] Fichier CSV vide", file=sys.stderr)
            return False
        for r in reader:
            if r:
                rows.append(r)

    # Mapping des indices de colonnes
    col_idx = {h.strip(): i for i, h in enumerate(headers)}

    def get_col(row, name, default=""):
        idx = col_idx.get(name)
        return row[idx] if idx is not None and idx < len(row) else default

    old_notes = []
    new_notes = []
    updated_rows = []
    students_updated = []

    for row in rows:
        login = get_col(row, 'Login')
        nom = get_col(row, 'Nom', login)
        jalons = int(float(get_col(row, 'Jalons_Presents', 0)))

        # Notes brutes enregistrées
        has_a1_10 = 'Axe1_Progression_sur_10' in col_idx
        col_a1 = 'Axe1_Progression_sur_10' if has_a1_10 else 'Axe1_Progression_sur_8'
        raw_a1_base = 10.0 if has_a1_10 else 8.0

        try:
            raw_a1 = float(get_col(row, col_a1, 0.0))
            raw_a2 = float(get_col(row, 'Axe2_Architecture_sur_8', 0.0))
            raw_a3 = float(get_col(row, 'Axe3_Style_sur_2', 0.0))
            raw_a4 = float(get_col(row, 'Axe4_Tests_sur_2', 0.0))
            old_total = float(get_col(row, 'Note_Finale_sur_20', 0.0))
        except ValueError:
            continue

        old_notes.append(old_total)

        # Si l'étudiant n'a rendu aucun travail ou est non évalué, note stricte à 0.0
        nb_fichiers = int(float(get_col(row, 'Fichiers_Actifs', 0)))
        profil_initial = get_col(row, 'Profil', '')
        if jalons == 0 and (nb_fichiers == 0 or old_total == 0.0 or old_total <= 0.2 or "Aucun travail" in profil_initial):
            calc_a1 = 0.0
            calc_a2 = 0.0
            calc_a3 = 0.0
            calc_a4 = 0.0
            raw_subtotal = 0.0
            ia_malus = 0.0
            sim_malus = 0.0
            final_total = 0.0
            new_profil = "Aucun travail rendu (Non évalué)"
            new_notes.append(0.0)
        else:
            # 1. Mise à l'échelle des axes selon weights.json
            calc_a1 = round((raw_a1 / raw_a1_base) * a1_max, 1)
            calc_a2 = round((raw_a2 / 8.0) * a2_max, 1)
            calc_a3 = round((raw_a3 / 2.0) * a3_max, 1)
            calc_a4 = round((raw_a4 / 2.0) * a4_max, 1) if a4_max > 0 else 0.0

            subtotal = calc_a1 + calc_a2 + calc_a3 + calc_a4
            if normalisation_factor != 1.0:
                subtotal = subtotal * normalisation_factor
            raw_subtotal = round(max(0.0, min(20.0, subtotal)), 1)

            # 2. Détection et application du malus Assistance IA
            ai_level = get_col(row, 'Indice_Assistance_IA', 'Faible')
            ia_malus = 0.0
            applied_ia_cap = None
            if "Élevé" in ai_level or "Eleve" in ai_level or "High" in ai_level:
                ia_malus = ia_malus_eleve
                applied_ia_cap = ia_plafond_eleve
            elif "Modéré" in ai_level or "Modere" in ai_level or "Moderate" in ai_level:
                ia_malus = ia_malus_modere
                applied_ia_cap = ia_plafond_modere

            # 3. Détection et application du malus de similarités
            sim_alert = get_col(row, 'Alerte_Similarite', '')
            nb_sim_files = count_similar_files(sim_alert)
            sim_malus = 0.0
            if nb_sim_files >= 3:
                sim_malus = sim_m3
            elif nb_sim_files == 2:
                sim_malus = sim_m2
            elif nb_sim_files == 1:
                sim_malus = sim_m1
            sim_malus = min(sim_malus, sim_max_total)

            # 4. Calcul de la note totale finale (IA plafonnée puis similarité déduite)
            note_after_ia = raw_subtotal - ia_malus
            if applied_ia_cap is not None:
                note_after_ia = min(note_after_ia, applied_ia_cap)
            adjusted_total = note_after_ia - sim_malus
            if malus_cumule_max > 0.0:
                adjusted_total = max(adjusted_total, raw_subtotal - malus_cumule_max)

            final_total = round(max(0.0, min(20.0, adjusted_total)), 1)
            new_notes.append(final_total)

            # 5. Ajustement du profil pédagogique
            curr_profil = get_col(row, 'Profil', 'Standard')
            has_ia = (ia_malus > 0)
            has_sim = (sim_malus > 0)
            new_profil = curr_profil

            if "Excellence" in curr_profil:
                if (has_ia and interdire_excell_ia) or (has_sim and interdire_excell_sim):
                    if has_ia and has_sim:
                        new_profil = "Avancé à recadrer (Assistance IA & Similarité forte)"
                    elif has_ia:
                        new_profil = "Avancé à recadrer (Assistance IA excessive)"
                    elif has_sim:
                        new_profil = "Avancé à recadrer (Similarité sur exercices)"
                elif final_total < note_min_excell:
                    new_profil = "Consolidation maîtrisée (Projets validés)"

        # Mise à jour de la ligne CSV
        new_row = list(row)
        if col_a1 in col_idx: new_row[col_idx[col_a1]] = f"{calc_a1:.1f}"
        if 'Axe2_Architecture_sur_8' in col_idx: new_row[col_idx['Axe2_Architecture_sur_8']] = f"{calc_a2:.1f}"
        if 'Axe3_Style_sur_2' in col_idx: new_row[col_idx['Axe3_Style_sur_2']] = f"{calc_a3:.1f}"
        if 'Axe4_Tests_sur_2' in col_idx: new_row[col_idx['Axe4_Tests_sur_2']] = f"{calc_a4:.1f}"
        if 'Note_Sans_Malus_sur_20' in col_idx: new_row[col_idx['Note_Sans_Malus_sur_20']] = f"{raw_subtotal:.1f}"
        if 'Note_Finale_sur_20' in col_idx: new_row[col_idx['Note_Finale_sur_20']] = f"{final_total:.1f}"
        if 'Profil' in col_idx: new_row[col_idx['Profil']] = new_profil

        updated_rows.append(new_row)
        students_updated.append({
            'login': login,
            'name': nom,
            'old': old_total,
            'new': final_total,
            'raw_subtotal': raw_subtotal,
            'a1': calc_a1,
            'a2': calc_a2,
            'a3': calc_a3,
            'a4': calc_a4,
            'ia_malus': ia_malus,
            'sim_malus': sim_malus,
            'profil': new_profil
        })

        # 6. Mise à jour de la fiche individuelle Markdown correspondante
        student_folder = s_path / login
        report_md = student_folder / f"{login}_rapport.md"
        if not report_md.exists():
            report_md_cand = s_path / f"{login}_rapport.md"
            if report_md_cand.exists():
                report_md = report_md_cand

        if report_md.exists():
            update_student_report_markdown(report_md, students_updated[-1], a1_max, a2_max, a3_max, a4_max)

    # Réécriture du CSV
    final_headers = list(headers)
    final_rows = [list(r) for r in updated_rows]
    if a4_max <= 0:
        if 'Axe1_Progression_sur_8' in final_headers:
            final_headers[final_headers.index('Axe1_Progression_sur_8')] = 'Axe1_Progression_sur_10'
        if 'Axe4_Tests_sur_2' in final_headers:
            del_idx = final_headers.index('Axe4_Tests_sur_2')
            final_headers.pop(del_idx)
            for r in final_rows:
                if del_idx < len(r):
                    r.pop(del_idx)

    with open(csv_file, 'w', newline='', encoding='utf-8') as fp:
        writer = csv.writer(fp)
        writer.writerow(final_headers)
        writer.writerows(final_rows)

    # Mise à jour du rapport de synthèse Markdown
    synthese_md = s_path / f"synthese_{s_path.name}.md"
    if not synthese_md.exists():
        synth_cands = list(s_path.glob("synthese_*.md"))
        if synth_cands:
            synthese_md = synth_cands[0]

    valid_new_notes = [n for n in new_notes if n > 0.0]
    avg_new = round(sum(new_notes) / len(new_notes), 2) if new_notes else 0.0
    sorted_new = sorted(new_notes)
    med_new = sorted_new[len(sorted_new)//2] if sorted_new else 0.0
    stdev_new = round(statistics.stdev(new_notes), 2) if len(new_notes) > 1 else 0.0

    avg_old = round(sum(old_notes) / len(old_notes), 2) if old_notes else 0.0
    med_old = sorted(old_notes)[len(old_notes)//2] if old_notes else 0.0

    if synthese_md.exists():
        try:
            s_content = synthese_md.read_text(encoding='utf-8', errors='ignore')
            # Mise à jour du titre principal
            s_content = re.sub(
                r'^#\s*Synthèse[^\r\n]*',
                f'# Synthèse de la Cohorte : {s_path.name}',
                s_content,
                count=1,
                flags=re.M
            )
            # Mise à jour du cartouche d'en-tête de synthèse
            s_content = re.sub(
                r'(\*\*Moyenne\*\*\s*:\s*\*\*)[0-9]+(?:\.[0-9]+)?(/20\*\*)',
                rf'\g<1>{avg_new:.2f}\g<2>',
                s_content
            )
            s_content = re.sub(
                r'(\*\*Médiane\*\*\s*:\s*\*\*)[0-9]+(?:\.[0-9]+)?(/20\*\*)',
                rf'\g<1>{med_new:.2f}\g<2>',
                s_content
            )
            s_content = re.sub(
                r'(\*\*Écart-type\*\*\s*:\s*\*\*)[0-9]+(?:\.[0-9]+)?(\*\*)',
                rf'\g<1>{stdev_new:.2f}\g<2>',
                s_content
            )
            
            # Mise à jour de l'en-tête du tableau de synthèse
            if a4_max <= 0:
                s_content = re.sub(
                    r'##\s*Tableau Récapitulatif Individuel[^\n]*',
                    '## Tableau Récapitulatif Individuel (Barème 10+8+2=20)',
                    s_content
                )
                s_content = re.sub(
                    r'\|\s*Login\s*\|\s*Nom[^\n]*\|',
                    '| Login | Nom de l\'Étudiant | Jalons (/31) | Axe 1 (Prog /10) | Axe 2 (Algo /8) | Axe 3 (Style /2) | Note sans malus /20 | Note avec malus /20 | Profil Identifié | Compilation | Alerte / Remarque |',
                    s_content
                )
                s_content = re.sub(
                    r'\|\s*:---\s*\|\s*:---\s*\|(?:\s*:---:\s*\|){4,7}\s*:---\s*\|\s*:---:\s*\|\s*:---\s*\|',
                    '| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: | :--- |',
                    s_content
                )

            # Mise à jour des lignes de tableau dans la synthèse
            for st in students_updated:
                pat = rf'(\|\s*`{re.escape(st["login"])}`\s*\|\s*\*\*[^*]+\*\*\s*\|\s*[0-9]+\s*\|)[^|]+(?:\|[^|]+){{2,4}}\|\s*\**[0-9]+(?:\.[0-9]+)?\**\s*(\|\s*[^|]+\|\s*[^|]+\|\s*[^|\n]*\|)'
                repl = rf'\g<1> {st["a1"]:.1f} | **{st["a2"]:.1f}** | {st["a3"]:.1f} | {st["raw_subtotal"]:.1f} | **{st["new"]:.1f}** \g<2>'
                s_content = re.sub(pat, repl, s_content)

            # Mise à jour ou ajout de la section des typologies / clusters
            clusters = [
                {
                    'id': 'mastery',
                    'label': '🌟 **Autonomie & Maîtrise Avancée**',
                    'desc': 'Progression complète, code personnel et rigoureux. Excellente autonomie conceptuelle. À mobiliser sur des défis ou du tutorat de pairs.',
                    'members': []
                },
                {
                    'id': 'solid_progress',
                    'label': '🛠️ **Pratique Régulière & Socle en Consolidation**',
                    'desc': 'Fondamentaux acquis et démarche fonctionnelle. Quelques fragilités sur la modularité ou les cas limites. Poursuivre la consolidation.',
                    'members': []
                },
                {
                    'id': 'struggling',
                    'label': '⚠️ **Difficultés Méthodologiques ou Décrochage**',
                    'desc': 'Travaux partiels, blocages initiaux ou abandons précoces. Remédiation requise sur la syntaxe de base et la structure algorithmique.',
                    'members': []
                },
                {
                    'id': 'ai_assisted',
                    'label': '🤖 **Recours Extérieur & Rupture Stylométrique**',
                    'desc': 'Empreinte IA ou idiomes atypiques non enseignés. Contrôle de l\'appropriation orale conseillé pour vérifier l\'autonomie.',
                    'members': []
                },
                {
                    'id': 'shared_code',
                    'label': '👥 **Code Partagé & Entraide Non Différenciée**',
                    'desc': 'Similarités structurelles directes sur du code libre. Rappel des consignes d\'intégrité et évaluation orale de la compréhension.',
                    'members': []
                }
            ]
            c_map = {c['id']: c for c in clusters}
            total_st = len(students_updated)

            for st in students_updated:
                lid = st['login']
                nom = st.get('name', '')
                note = st['new']
                is_susp = st.get('sim_malus', 0) > 0
                is_ia = st.get('ia_malus', 0) > 0
                member_entry = f"`{lid}` ({nom})" if (nom and nom != lid) else f"`{lid}`"

                if is_susp:
                    c_map['shared_code']['members'].append(member_entry)
                elif is_ia:
                    c_map['ai_assisted']['members'].append(member_entry)
                elif note < 10.0:
                    c_map['struggling']['members'].append(member_entry)
                elif note >= 14.0:
                    c_map['mastery']['members'].append(member_entry)
                else:
                    c_map['solid_progress']['members'].append(member_entry)

            cluster_lines = [
                "## 👥 Typologies & Clusters d'Étudiants Types\n",
                "| Cluster Pédagogique | Effectif | Proportion | Caractéristiques & Recommandations | Étudiants Associés |",
                "| :--- | :---: | :---: | :--- | :--- |"
            ]
            for c in clusters:
                cnt = len(c['members'])
                pct = (cnt / total_st * 100.0) if total_st > 0 else 0.0
                m_str = ", ".join(c['members']) if c['members'] else "—"
                cluster_lines.append(f"| {c['label']} | {cnt} | {pct:.1f}% | {c['desc']} | {m_str} |")
            cluster_lines.append("")
            cluster_section_str = "\n".join(cluster_lines)

            if "## 👥 Typologies & Clusters d'Étudiants Types" in s_content:
                s_content = re.sub(
                    r"## 👥 Typologies & Clusters d'Étudiants Types[\s\S]*?(?=\n## |\Z)",
                    cluster_section_str.rstrip(),
                    s_content
                )
            else:
                s_content = s_content.rstrip() + "\n\n" + cluster_section_str

            synthese_md.write_text(s_content, encoding='utf-8')
        except Exception as e:
            print(f"[WARN] Impossible de mettre à jour {synthese_md}: {e}", file=sys.stderr)

    # Affichage du bilan final structuré
    print("=" * 60)
    print(f"✅ RECALCUL TERMINÉ AVEC SUCCÈS POUR {len(students_updated)} ÉTUDIANTS")
    print(f"Source barème : {w_source}")
    print(f"Moyenne promo : {avg_old:.2f}/20 -> {avg_new:.2f}/20 (Médiane : {med_old:.1f} -> {med_new:.1f})")
    print(f"Écart-type    : {stdev_new:.2f} (Min : {min(new_notes):.1f}, Max : {max(new_notes):.1f})")
    print("-" * 60)
    for st in students_updated:
        diff = st['new'] - st['old']
        diff_str = f"{diff:+.1f}" if diff != 0.0 else "="
        malus_str = ""
        if st['ia_malus'] > 0: malus_str += f" [IA: -{st['ia_malus']}pt]"
        if st['sim_malus'] > 0: malus_str += f" [Sim: -{st['sim_malus']}pt]"
        print(f"  • {st['login']:10s} {st['name']:20s} : {st['old']:4.1f} -> {st['new']:4.1f}/20 ({diff_str}){malus_str}")
    print("=" * 60)
    print(f"RECALCULATE_RESULT:SUCCESS|students={len(students_updated)}|old_avg={avg_old:.2f}|new_avg={avg_new:.2f}|median={med_new:.1f}|min={min(new_notes):.1f}|max={max(new_notes):.1f}")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: recalculate_grades.py <session_eval_dir> [--weights /path/to/weights.json]")
        sys.exit(1)

    s_dir = sys.argv[1]
    w_file = None
    if "--weights" in sys.argv:
        w_idx = sys.argv.index("--weights")
        if w_idx + 1 < len(sys.argv):
            w_file = sys.argv[w_idx + 1]

    ok = recalculate_session(s_dir, w_file)
    sys.exit(0 if ok else 1)

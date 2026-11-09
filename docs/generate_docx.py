#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_docx.py - Génère une version DOCX imprimable et élégante de ACTIVITY_TRACKING.md.
"""

import os
import sys
from pathlib import Path
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

DOCS_DIR = Path(__file__).resolve().parent
MD_PATH = DOCS_DIR / "ACTIVITY_TRACKING.md"
OUT_DOCX = DOCS_DIR / "ACTIVITY_TRACKING.docx"

# Palette de couleurs élégante
COLOR_NAVY = RGBColor(27, 54, 93)      # #1B365D
COLOR_STEEL = RGBColor(14, 116, 144)   # #0E7490
COLOR_TEXT = RGBColor(30, 41, 59)      # #1E293B
COLOR_MUTED = RGBColor(100, 116, 139)  # #64748B
COLOR_RED = RGBColor(185, 28, 28)      # #B91C1C
COLOR_AMBER = RGBColor(180, 83, 9)     # #B45309
COLOR_GREEN = RGBColor(21, 128, 61)    # #15803D

HEX_NAVY = "1B365D"
HEX_STEEL = "0E7490"
HEX_LIGHT_BG = "F8FAFC"
HEX_CALLOUT_BG = "F1F5F9"
HEX_BORDER = "CBD5E1"
HEX_RED_BG = "FEE2E2"
HEX_AMBER_BG = "FEF3C7"
HEX_GREEN_BG = "DCFCE7"

def set_cell_background(cell, hex_color: str):
    """Applique une couleur d'arrière-plan à une cellule de tableau."""
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    """Définit les marges internes (padding) d'une cellule en vingtièmes de point (dxa)."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def set_cell_borders(cell, top=None, bottom=None, left=None, right=None):
    """Applique des bordures spécifiques à une cellule."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'{"<w:top w:val=\"" + top.get("val", "single") + "\" w:sz=\"" + str(top.get("sz", 4)) + "\" w:space=\"0\" w:color=\"" + top.get("color", "CBD5E1") + "\"/>" if top else "<w:top w:val=\"none\"/>"}'
        f'{"<w:bottom w:val=\"" + bottom.get("val", "single") + "\" w:sz=\"" + str(bottom.get("sz", 4)) + "\" w:space=\"0\" w:color=\"" + bottom.get("color", "CBD5E1") + "\"/>" if bottom else "<w:bottom w:val=\"none\"/>"}'
        f'{"<w:left w:val=\"" + left.get("val", "single") + "\" w:sz=\"" + str(left.get("sz", 4)) + "\" w:space=\"0\" w:color=\"" + left.get("color", "CBD5E1") + "\"/>" if left else "<w:left w:val=\"none\"/>"}'
        f'{"<w:right w:val=\"" + right.get("val", "single") + "\" w:sz=\"" + str(right.get("sz", 4)) + "\" w:space=\"0\" w:color=\"" + right.get("color", "CBD5E1") + "\"/>" if right else "<w:right w:val=\"none\"/>"}'
        f'</w:tcBorders>'
    )
    tcPr.append(tcBorders)

def make_table_responsive(table, col_widths: list, align_header=True):
    """Configure un tableau élégant : en-tête répété, lignes non sécables, bordures propres."""
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False

    # Configuration de l'en-tête répété sur chaque page
    header_tr = table.rows[0]._tr.get_or_add_trPr()
    header_tr.append(parse_xml(r'<w:tblHeader xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>'))

    for r_idx, row in enumerate(table.rows):
        trPr = row._tr.get_or_add_trPr()
        trPr.append(parse_xml(r'<w:cantSplit xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>'))

        is_header = (r_idx == 0)
        bg = HEX_NAVY if is_header else (HEX_LIGHT_BG if r_idx % 2 == 1 else "FFFFFF")

        for c_idx, cell in enumerate(row.cells):
            if c_idx < len(col_widths):
                cell.width = col_widths[c_idx]
            cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=100, bottom=100, left=140, right=140)

            # Bordure horizontale subtile
            b_bottom = {"val": "single", "sz": 6, "color": "0F172A" if is_header else HEX_BORDER}
            b_top = {"val": "single", "sz": 4, "color": HEX_BORDER} if not is_header else None
            set_cell_borders(cell, top=b_top, bottom=b_bottom)

            # Style du texte
            for p in cell.paragraphs:
                p.paragraph_format.space_before = Pt(2)
                p.paragraph_format.space_after = Pt(2)
                p.paragraph_format.line_spacing = 1.05
                if is_header:
                    for run in p.runs:
                        run.font.bold = True
                        run.font.color.rgb = RGBColor(255, 255, 255)
                        run.font.size = Pt(9.5)
                else:
                    for run in p.runs:
                        run.font.size = Pt(9.0)

def add_callout(doc, title: str, text: str, icon: str = "💡", bg_hex=HEX_CALLOUT_BG, border_hex=HEX_NAVY):
    """Insère un bloc encadré 'Callout' stylisé avec bordure gauche épaisse."""
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    cell = table.cell(0, 0)
    cell.width = Inches(6.8)

    set_cell_background(cell, bg_hex)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=150)
    set_cell_borders(cell, left={"val": "single", "sz": 24, "color": border_hex})

    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.15

    if title:
        r_t = p.add_run(f"{icon} {title}\n")
        r_t.font.bold = True
        r_t.font.color.rgb = COLOR_NAVY
        r_t.font.size = Pt(10.0)

    r_body = p.add_run(text)
    r_body.font.size = Pt(9.5)
    r_body.font.color.rgb = COLOR_TEXT

    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

def add_code_box(doc, code_str: str, caption: str = None):
    """Insère un bloc de code avec police monospace ombrée."""
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    cell = table.cell(0, 0)
    cell.width = Inches(6.8)

    set_cell_background(cell, "F8FAFC")
    set_cell_margins(cell, top=100, bottom=100, left=140, right=140)
    set_cell_borders(cell,
        top={"val": "single", "sz": 4, "color": HEX_BORDER},
        bottom={"val": "single", "sz": 4, "color": HEX_BORDER},
        left={"val": "single", "sz": 4, "color": HEX_BORDER},
        right={"val": "single", "sz": 4, "color": HEX_BORDER}
    )

    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.05

    if caption:
        rc = p.add_run(f"// {caption}\n")
        rc.font.italic = True
        rc.font.size = Pt(8.5)
        rc.font.color.rgb = COLOR_MUTED

    r_code = p.add_run(code_str.strip())
    r_code.font.name = "Consolas"
    r_code.font.size = Pt(8.5)
    r_code.font.color.rgb = RGBColor(15, 23, 42)

    p_after = doc.add_paragraph()
    p_after.paragraph_format.space_before = Pt(0)
    p_after.paragraph_format.space_after = Pt(4)

def build_docx():
    print(f"[DOCX] Création du document Word imprimable : {OUT_DOCX}")
    doc = docx.Document()

    # Configuration des marges de page A4 : 2 cm partout
    sections = doc.sections
    for sec in sections:
        sec.top_margin = Inches(0.8)
        sec.bottom_margin = Inches(0.8)
        sec.left_margin = Inches(0.8)
        sec.right_margin = Inches(0.8)
        sec.page_width = Inches(8.27)   # A4 width
        sec.page_height = Inches(11.69) # A4 height

        # En-tête et pied de page
        header = sec.header
        hp = header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hrun = hp.add_run("CodeLab • Système Télémétrique & Profils Didactiques")
        hrun.font.name = "Calibri"
        hrun.font.size = Pt(8.5)
        hrun.font.color.rgb = COLOR_MUTED

        footer = sec.footer
        fp = footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        frun1 = fp.add_run("Le Mans Université • Département Informatique  |  ")
        frun1.font.name = "Calibri"
        frun1.font.size = Pt(8.5)
        frun1.font.color.rgb = COLOR_MUTED
        
        frun2 = fp.add_run("Documentation Technique & Pédagogique")
        frun2.font.name = "Calibri"
        frun2.font.size = Pt(8.5)
        frun2.font.bold = True
        frun2.font.color.rgb = COLOR_NAVY

    # Style par défaut
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Calibri'
    style_normal.font.size = Pt(10.0)
    style_normal.font.color.rgb = COLOR_TEXT

    # -------------------------------------------------------------
    # PAGE DE TITRE / BANDEAU D'EN-TÊTE
    # -------------------------------------------------------------
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(12)
    p_title.paragraph_format.space_after = Pt(4)
    run_title = p_title.add_run("Système Télémétrique de CodeLab")
    run_title.font.name = "Calibri"
    run_title.font.size = Pt(22)
    run_title.font.bold = True
    run_title.font.color.rgb = COLOR_NAVY

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(4)
    run_sub = p_sub.add_run("Observables, Indicateurs, Alertes Temps Réel & Doubles-Profils Didactiques")
    run_sub.font.name = "Calibri"
    run_sub.font.size = Pt(13)
    run_sub.font.color.rgb = COLOR_STEEL

    # Métadonnées
    p_meta = doc.add_paragraph()
    p_meta.paragraph_format.space_before = Pt(0)
    p_meta.paragraph_format.space_after = Pt(14)
    r_meta = p_meta.add_run("Documentation Technique et Pédagogique de Référence  •  Version 2.1 (Novembre 2026)  •  CodeLab v1.5.3\nLe Mans Université — Licence Institutionnelle Éducative & Non-Commerciale (APP IDDN-FR-001-260031-000-SC-2022-000-10000)")
    r_meta.font.size = Pt(8.5)
    r_meta.font.color.rgb = COLOR_MUTED

    # Séparateur horizontal
    p_sep = doc.add_paragraph()
    p_sep.paragraph_format.space_before = Pt(0)
    p_sep.paragraph_format.space_after = Pt(12)
    p_sep_run = p_sep.add_run("―" * 68)
    p_sep_run.font.color.rgb = RGBColor(203, 213, 225)
    p_sep_run.font.size = Pt(10)

    # -------------------------------------------------------------
    # 1. INTRODUCTION & PHILOSOPHIE ARCHITECTURALE
    # -------------------------------------------------------------
    h1 = doc.add_heading("1. Introduction & Philosophie Architecturale", level=1)
    h1.paragraph_format.space_before = Pt(14)
    h1.paragraph_format.space_after = Pt(6)
    for run in h1.runs:
        run.font.color.rgb = COLOR_NAVY
        run.font.size = Pt(14)

    doc.add_paragraph(
        "Le système de traçabilité et d'analyse comportementale de CodeLab répond à un triple défi de l'enseignement contemporain de la programmation :\n"
        "1. L'intégrité académique à l'ère des LLM (ChatGPT, Copilot, Claude, etc.) : distinguer une saisie humaine itérative d'une injection massive de code généré par IA ou copié d'un pair.\n"
        "2. La remédiation pédagogique proactive : détecter en temps réel la détresse silencieuse d'un apprenant (acharnement stérile, erreurs répétées en boucle, décrochage) avant qu'il n'abandonne.\n"
        "3. L'évaluation formative et continue : valoriser la démarche de résolution de problèmes et l'effort d'apprentissage (profil Artisan) au-delà de la simple conformité du code final."
    )

    doc.add_heading("Architecture Globale : Le Double Canal de Traitement", level=2)
    doc.add_paragraph(
        "Le système sépare clairement l'architecture en deux canaux complémentaires :\n"
        "• Canal 1 (Temps Réel — Pendant la séance) : Sondes d'activité ultra-légères (≤ 1 Ko/min) transmises au serveur Rust asynchrone (Tokio), qui calcule en direct S_auth, I_nav et déclenche les alertes pour la table de supervision du tuteur (UserTable).\n"
        "• Canal 2 (Différé — Après la séance) : Journal append-only inaltérable (.activity.jsonl) analysé longitudinalement par les modules Python (activity_analyzer.py) pour projeter les diagrammes radar Mermaid et caractériser les profils types."
    )

    add_callout(
        doc,
        "Principes Fondamentaux de Conception",
        "• Performance critique : Écriture asynchrone append-only sans blocage, débit réseau < 1 Ko/min par étudiant.\n"
        "• Conformité RGPD & Privacy : Aucune interception textuelle arbitraire du presse-papier système (seuls les volumes de caractères et lignes sont comptabilisés). Le code n'est journalisé que lors d'une sauvegarde dans l'espace de travail.\n"
        "• Résilience absolue : Replay instantané de l'état des alertes à partir du fichier journal en cas de reconnexion.",
        icon="🛡️"
    )

    # -------------------------------------------------------------
    # 2. ONTOLOGIE DES 11 OBSERVABLES BRUTS
    # -------------------------------------------------------------
    h1 = doc.add_heading("2. Ontologie des 11 Observables Bruts (Collecte)", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "Les observables constituent le socle factuel et inaltérable de la traçabilité. "
        "Ils sont stockés chronologiquement dans sessions/<session>/<login>/.activity.jsonl :"
    )

    # Tableau des 11 observables
    table_obs = doc.add_table(rows=12, cols=4)
    obs_widths = [Inches(1.2), Inches(0.9), Inches(1.7), Inches(3.0)]

    headers = ["Observable", "Origine", "Point d'ancrage Code", "Description & Déclencheur"]
    for i, h in enumerate(headers):
        table_obs.cell(0, i).paragraphs[0].text = h

    obs_data = [
        ("SESSION", "Serveur", "handler.rs", "Connexion (CONNECTED) avec IP source ou déconnexion (DISCONNECTED) avec durée totale."),
        ("VIEW", "Serveur", "handler.rs", "Consultation ou ouverture d'un document (sujet Markdown, fichier source, doc)."),
        ("FILE_OP", "Serveur", "handler.rs", "Manipulation de fichier : création (CREATE), suppression (DELETE), renommage (RENAME)."),
        ("FILE_DROP", "Client", "AbstractCodeLab.java", "Glisser-déposer d'un fichier externe depuis le bureau OS ou support amovible."),
        ("TYPING", "Client", "AbstractCodeLab.java", "Frappes réelles cumulées (typed_chars) et corrections (backspaces) par fenêtre."),
        ("FOCUS", "Client", "AbstractCodeLab.java", "Changement d'application : perte (LOST) et reprise (GAINED) avec durée d'absence."),
        ("PASTE", "Client", "AbstractCodeLab.java", "Opération Ctrl+V / Cmd+V avec volume de caractères et lignes insérés."),
        ("EDIT", "Serveur", "activity.rs", "Diff textuel unifié à la sauvegarde : lignes ajoutées/supprimées, taille du plus grand bloc."),
        ("COMPILE", "Client", "AbstractCodeLab.java", "Verdict compilateur (OK, ERROR), durée de compilation et extrait de diagnostic."),
        ("EXECUTE", "Client", "AbstractCodeLab.java", "Cycle d'exécution (STARTED, DONE, ERROR, HALTED), code retour et traceback."),
        ("HELP", "Serveur", "handler.rs", "Cycle d'appel tuteur : demande (REQUESTED), annulation (CANCELLED), résolution (RESOLVED).")
    ]

    for r_idx, row in enumerate(obs_data, start=1):
        for c_idx, val in enumerate(row):
            table_obs.cell(r_idx, c_idx).paragraphs[0].text = val

    make_table_responsive(table_obs, obs_widths)

    doc.add_paragraph()
    add_code_box(
        doc,
        '{"type":"SESSION","timestamp":"2026-11-04T08:00:00Z","session":"TP1","login":"etudiant1","status":"CONNECTED","ip":"172.20.10.42"}\n'
        '{"type":"VIEW","timestamp":"2026-11-04T08:01:10Z","session":"TP1","login":"etudiant1","file":"enonce.md"}\n'
        '{"type":"TYPING","timestamp":"2026-11-04T08:05:00Z","session":"TP1","login":"etudiant1","file":"exercice1.py","typed_chars":145,"backspaces":18}\n'
        '{"type":"FOCUS","timestamp":"2026-11-04T08:12:00Z","session":"TP1","login":"etudiant1","status":"LOST"}\n'
        '{"type":"FOCUS","timestamp":"2026-11-04T08:13:45Z","session":"TP1","login":"etudiant1","status":"GAINED","duration_sec":105}\n'
        '{"type":"PASTE","timestamp":"2026-11-04T08:13:48Z","session":"TP1","login":"etudiant1","file":"exercice1.py","chars":380,"lines":18}\n'
        '{"type":"EDIT","timestamp":"2026-11-04T08:13:52Z","session":"TP1","login":"etudiant1","file":"exercice1.py","total_lines":35,"lines_added":18,"chunk_max":18,"alert":"BURST_INSERT"}\n'
        '{"type":"EXECUTE","timestamp":"2026-11-04T08:25:00Z","session":"TP1","login":"etudiant1","file":"exercice1.py","status":"ERROR","error_msg":"IndexError"}',
        caption="Échantillon du journal chronologique append-only .activity.jsonl"
    )

    # -------------------------------------------------------------
    # 3. MOTEUR DE DÉRIVATION & INDICATEURS PÉDAGOGIQUES
    # -------------------------------------------------------------
    h1 = doc.add_heading("3. Moteur de Dérivation & Indicateurs Pédagogiques", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "Le moteur d'analytics (activity_analyzer.py) agrège les observables pour produire des métriques formelles de haut niveau :"
    )

    doc.add_heading("A. Indicateurs d'Intégrité & de Genèse du Code", level=2)
    doc.add_paragraph(
        "1. Score d'Authenticité (S_auth) :\n"
        "   Mesure la proportion de matière textuelle rédigée via frappe humaine directe face aux injections massives externes :\n"
        "   S_auth = [ N_typed / (N_typed + N_pasted + N_dropped) ] × 100\n"
        "   • S_auth ≥ 70% : Authenticité élevée (saisie artisanale, appropriation réelle du problème).\n"
        "   • 40% ≤ S_auth < 70% : Modérée (quelques collages ou réutilisations légitimes d'exemples).\n"
        "   • S_auth < 40% : Suspecte / Faible (forte prédominance d'injections externes ou génération IA).\n\n"
        "2. Indice de Navette Externe (I_nav) :\n"
        "   Quantifie la part du temps passée hors de l'IDE : I_nav = ( Σ t_away / T_session ) × 100\n"
        "   Pattern typique de prompt IA détecté automatiquement : FOCUS(LOST, 20s-180s) ──▶ FOCUS(GAINED) ──▶ PASTE(chars ≥ 150).\n\n"
        "3. Taux de Friction Cognitive (T_fric) :\n"
        "   Mesure l'hésitation rédactionnelle : T_fric = ( N_backspaces / N_typed ) × 100\n"
        "   • T_fric < 3% : Dictée passive sans hésitation.\n"
        "   • 10% ≤ T_fric ≤ 25% : Rythme sain de réflexion.\n"
        "   • T_fric > 40% : Tâtonnement et grande confusion."
    )

    doc.add_heading("B. Indicateurs de Méthodologie & Démarche Expérimentale", level=2)
    doc.add_paragraph(
        "1. Indice d'Acharnement Stérile (I_thrash) :\n"
        "   Représente la part des exécutions menées sans analyse préalable : I_thrash = ( Runs en acharnement / Total des Runs ) × 100\n"
        "   Un épisode d'acharnement est constitué d'au moins 3 exécutions ERROR consécutives espacées de moins de 30 secondes ou avec moins de 5 caractères modifiés.\n\n"
        "2. Temps Moyen de Résolution d'Erreur (TTF — Time To Fix) :\n"
        "   Durée moyenne écoulée entre l'apparition de la première erreur et le retour au premier run réussi (DONE/OK).\n"
        "   • TTF moyen < 30s : Correction immédiate d'une typo.\n"
        "   • 1 min ≤ TTF ≤ 5 min : Démarche analytique saine.\n"
        "   • TTF > 15 min : Blocage conceptuel sévère nécessitant une intervention."
    )

    doc.add_heading("C. Budget Temps en 4 Quadrants & Fenêtres Glissantes (15 min)", level=2)
    doc.add_paragraph(
        "La séance est ventilée en 4 quadrants : Q1 Lecture (15%), Q2 Édition active (45%), Q3 Réflexion/Diagnostic (25%), Q4 Hors IDE (15%).\n"
        "Pour ne pas masquer un basculement tardif vers l'IA en fin de séance, la session est segmentée en tranches continues de 15 minutes qualifiant la phase : Saisie autonome, Validation/Tests, Réflexion, Acharnement, Décrochage IA ou Dépôt externe."
    )

    # -------------------------------------------------------------
    # 4. SYSTÈME D'ALERTES & SUPERVISION TUTEUR TEMPS RÉEL
    # -------------------------------------------------------------
    h1 = doc.add_heading("4. Système d'Alertes Temps Réel & Supervision Tuteur", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "Le serveur Rust asynchrone évalue chaque observable en temps réel dans activity.rs et maintient les alertes actives :"
    )

    table_alert = doc.add_table(rows=5, cols=4)
    alert_widths = [Inches(1.1), Inches(0.9), Inches(2.4), Inches(2.4)]
    alert_headers = ["Code Alerte", "Sévérité", "Condition de Déclenchement", "Règle d'Extinction (Auto-Clear)"]
    for i, h in enumerate(alert_headers): table_alert.cell(0, i).paragraphs[0].text = h

    alert_data = [
        ("BURST", "CRIT", "≥ 20 lignes en ≤ 15s (BURST_INSERT) ou nouveau fichier créé avec ≥ 25 lignes d'un coup.", "Acquittement manuel par le tuteur au poste."),
        ("PASTE", "WARN", "Collage unitaire ≥ 15 lignes ou ≥ 400 caractères.", "Acquittement manuel par le tuteur au poste."),
        ("THRASH", "WARN", "≥ 3 compilations ERROR ou exécutions ERROR consécutives sans run réussi intercalé.", "Auto-extinction automatique dès qu'un run réussit (OK/DONE) ou modification de code ≥ 3 lignes."),
        ("DROP", "WARN", "Dépôt externe de fichier dans l'arborescence via drag & drop.", "Acquittement manuel par le tuteur au poste.")
    ]

    for r_idx, row in enumerate(alert_data, start=1):
        for c_idx, val in enumerate(row):
            table_alert.cell(r_idx, c_idx).paragraphs[0].text = val

    make_table_responsive(table_alert, alert_widths)

    doc.add_paragraph()
    doc.add_paragraph(
        "Hiérarchie & Rendu dans UserTable :\n"
        "• Priorité stricte : CRIT (rouge) > WARN (ambre) > INFO (neutre).\n"
        "• Badge compact avec compteur d'alertes résiduelles (ex: 🚨 Burst (+1) ou ⚠️ Blocage (+2)).\n"
        "• Infobulle HTML enrichie au survol détaillant l'historique horodaté des alertes actives."
    )

    # -------------------------------------------------------------
    # 5. TYPOLOGIE DES PROFILS ÉTUDIANTS & DOUBLE RADAR MERMAID
    # -------------------------------------------------------------
    doc.add_page_break()
    h1 = doc.add_heading("5. Typologie des Profils Étudiants & Visualisation Radar", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "L'audit classe automatiquement les étudiants selon 6 profils didactiques types articulés autour de la matrice d'authenticité et de vélocité, et les projette sur un diagramme radar Mermaid (radar-beta) opposés deux à deux à 180° :"
    )

    add_callout(
        doc,
        "Opposition des 3 Couples Didactiques à 180° sur le Radar :\n"
        "• Axe 0 ⟷ Axe 3 : 👑 Le Boss ⟷ 🚪 Le Décrocheur (Aisance & Autonomie vs Abandon Silencieux)\n"
        "• Axe 1 ⟷ Axe 4 : 🔨 L'Artisan ⟷ 🤖 Le Cyborg (Démarche Authentique vs Délégation Externe)\n"
        "• Axe 2 ⟷ Axe 5 : ⚡ Le Casse-cou ⟷ 🏴‍☠️ Le Pirate (Combat avec le Code vs Copie / Collusion)",
        "INFO"
    )

    table_axes = doc.add_table(rows=4, cols=4)
    axes_widths = [Inches(1.5), Inches(1.2), Inches(2.3), Inches(1.8)]
    axes_headers = ["Axe d'Opposition", "Profil Type", "Comportement Observé", "Indicateurs Clés"]
    for i, h in enumerate(axes_headers): table_axes.cell(0, i).paragraphs[0].text = h

    axes_data = [
        ("Axe 1 : Aisance vs Abandon", "👑 Le Boss\n\n🚪 Le Décrocheur", "Résolution immédiate, autonomie complète, maîtrise sans friction.\n\nDifficulté non résolue, passivité, inactivité prolongée et abandon silencieux.", "S_auth > 80%, TTF < 30s, 0 aide tuteur.\n\nIdle > 15 min, I_nav > 40%, ≤ 2 runs."),
        ("Axe 2 : Démarche vs IA", "🔨 L'Artisan\n\n🤖 Le Cyborg", "Démarche d'apprentissage exemplaire, saisie personnelle, cycles courts.\n\nDélégation externe / IA, injections massives sans maturation.", "S_auth > 80%, T_fric 10-25%, I_thrash < 10%.\n\nS_auth < 30%, BURST multiples, I_nav > 40%."),
        ("Axe 3 : Copie vs Acharnement", "🏴‍☠️ Le Pirate\n\n⚡ Le Casse-cou", "Collusion en salle, import par support externe (clé USB) ou copie d'autrui.\n\nCombat acharné avec le compilateur, tâtonnement compulsif sans recul.", "FILE_DROP, similarité Winnowing ≥ 75%.\n\nI_thrash > 35%, erreurs > 70%, Δt < 15s.")
    ]

    for r_idx, row in enumerate(axes_data, start=1):
        for c_idx, val in enumerate(row):
            table_axes.cell(r_idx, c_idx).paragraphs[0].text = val

    make_table_responsive(table_axes, axes_widths)

    # -------------------------------------------------------------
    # 6. DÉTECTION & INTERPRÉTATION DES DOUBLES-PROFILS DIDACTIQUES
    # -------------------------------------------------------------
    doc.add_page_break()
    h1 = doc.add_heading("6. Détection & Interprétation des Doubles-Profils Didactiques Cohérents", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "En situation réelle d'apprentissage en laboratoire, les comportements observés ne se réduisent que rarement à un archétype unilatéral pur. "
        "Les 6 profils didactiques de CodeLab étant bâtis sur 3 axes orthogonaux indépendants, les croisements inter-axes traduisent des signatures comportementales complexes et cohérentes."
    )

    doc.add_heading("Principes Didactiques & Règles Formelles de Détection", level=2)
    doc.add_paragraph(
        "Un double-profil didactique n'est pas un artefact statistique, mais la manifestation conjointe de deux dimensions comportementales indépendantes :\n"
        "• La maîtrise conceptuelle et algorithmique (Axe Boss ⟷ Décrocheur)\n"
        "• La démarche d'assimilation et d'effort personnel (Axe Artisan ⟷ Cyborg)\n"
        "• La gestion émotionnelle et stratégique de l'erreur (Axe Casse-cou ⟷ Pirate)\n\n"
        "Le moteur d'audit (activity_analyzer.py) identifie automatiquement un Double-Profil Hybride lorsque les 3 conditions suivantes sont réunies :\n"
        "1. Orthogonalité des dimensions : Les deux profils avec les affinités les plus fortes (P1 et P2) appartiennent à des axes distincts. Les pôles opposés d'un même axe à 180° s'excluent mutuellement par construction métrique.\n"
        "2. Niveau d'affinité significatif : Les deux scores d'affinité sont élevés (S1 ≥ 11.0 / 20 et S2 ≥ 11.0 / 20, soit au moins 55% d'adéquation comportementale).\n"
        "3. Proximité des affinités : L'écart entre les deux scores est resserré (|S1 - S2| ≤ 3.5 points), garantissant que les deux dimensions sont au coude-à-coude et motrices dans la séance."
    )

    doc.add_heading("Tableau de Croisement des Profils de Base (Matrice 6 × 6)", level=2)
    doc.add_paragraph(
        "Ce tableau à double entrée croise les 6 profils didactiques élémentaires. La diagonale correspond aux profils purs exclusifs, les cases marquées [X] aux oppositions strictes à 180° sur le radar (mutuellement exclusives par orthogonalité), et les autres cases aux 12 doubles-profils hybrides remarquables :"
    )

    table_cross = doc.add_table(rows=7, cols=7)
    cross_widths = [Inches(1.1)] + [Inches(0.95)] * 6
    cross_headers = ["Profils", "👑 Boss", "🔨 Artisan", "⚡ Casse-cou", "🚪 Décroch.", "🤖 Cyborg", "🏴‍☠️ Pirate"]
    for i, h in enumerate(cross_headers): table_cross.cell(0, i).paragraphs[0].text = h

    cross_data = [
        ["👑 Boss", "Profil pur", "Architecte Méthodique", "Hacker Instinctif", "❌ Antagoniste", "Ingénieur Augmenté", "Éclaireur Partageur"],
        ["🔨 Artisan", "Architecte Méthodique", "Profil pur", "Travailleur Acharné", "Apprenti Hésitant", "❌ Antagoniste", "Artisan s/ Influence"],
        ["⚡ Casse-cou", "Hacker Instinctif", "Travailleur Acharné", "Profil pur", "Épuisé Découragé", "Apprenti Sorcier", "❌ Antagoniste"],
        ["🚪 Décroch.", "❌ Antagoniste", "Apprenti Hésitant", "Épuisé Découragé", "Profil pur", "Délégataire Passif", "Copiste de Survie"],
        ["🤖 Cyborg", "Ingénieur Augmenté", "❌ Antagoniste", "Apprenti Sorcier", "Délégataire Passif", "Profil pur", "Contrebandier Num."],
        ["🏴‍☠️ Pirate", "Éclaireur Partageur", "Artisan s/ Influence", "❌ Antagoniste", "Copiste de Survie", "Contrebandier Num.", "Profil pur"]
    ]

    for r_idx, row in enumerate(cross_data, start=1):
        for c_idx, val in enumerate(row):
            table_cross.cell(r_idx, c_idx).paragraphs[0].text = val

    make_table_responsive(table_cross, cross_widths)
    doc.add_paragraph()

    doc.add_heading("Matrice Détaillée des 12 Doubles-Profils Didactiques Hybrides", level=2)

    table_hyb = doc.add_table(rows=13, cols=4)
    hyb_widths = [Inches(1.4), Inches(1.3), Inches(2.2), Inches(1.9)]
    hyb_headers = ["Double-Profil Détecté", "Surnom Pédagogique", "Signification Didactique & Posture", "Posture & Conseil pour l'Enseignant"]
    for i, h in enumerate(hyb_headers): table_hyb.cell(0, i).paragraphs[0].text = h

    hyb_data = [
        ("👑 Boss + 🔨 Artisan", "L'Architecte Méthodique", "Excellence absolue : aisance conceptuelle immédiate alliée à une rigueur de construction pas à pas (saisie personnelle continue, tests réguliers). Ne survole pas le sujet.", "Valoriser son autonomie, lui proposer des défis d'optimisation avancés, de la généricité ou un rôle de tuteur d'îlot."),
        ("👑 Boss + ⚡ Casse-cou", "Le Hacker Instinctif", "Grande vivacité d'esprit mais développement impulsif à chaud sans plan écrit, comptant sur son intuition pour corriger en direct.", "Canaliser sa vivacité vers des pratiques d'ingénierie pérennes : poser des assertions, structurer un plan de tests."),
        ("🔨 Artisan + ⚡ Casse-cou", "Le Travailleur Acharné", "Investissement authentique remarquable et combativité totale (zéro triche), mais pénalisé par le tâtonnement aveugle face aux erreurs.", "Profil prioritaire pour le tuteur : l'aider à décortiquer les tracebacks, poser des points d'arrêt et verbaliser son algorithme."),
        ("🤖 Cyborg + ⚡ Casse-cou", "L'Apprenti Sorcier", "Injection de code généré par IA non maîtrisé, suivi de modifications compulsives au hasard pour forcer le passage des tests.", "Recadrage méthodologique immédiat : suspendre temporairement l'IA, retour aux fondamentaux et réécriture manuelle pas à pas."),
        ("🚪 Décrocheur + 🤖 Cyborg", "Le Délégataire Passif", "Inactivité ou passivité prolongée masquée par un code externe parachuté d'un coup en fin de TP pour donner le change.", "Entretien individuel bienveillant mais ferme : vérifier l'assimilation réelle par soutenance orale et remédiation pas à pas."),
        ("🚪 Décrocheur + ⚡ Casse-cou", "L'Épuisé Découragé", "Épuisement face aux échecs répétés. Combat acharné initial ayant débouché sur la frustration puis l'abandon complet en seconde partie.", "Signe de détresse didactique : intervenir plus précocement en séance dès les premiers signes d'acharnement stérile pour éviter le renoncement."),
        ("👑 Boss + 🤖 Cyborg", "L'Ingénieur Augmenté", "Aisance conceptuelle solide exploitant l'IA comme un accélérateur de productivité assumé (copilotage). Risque de sur-délégation.", "S'assurer lors d'évaluations sur table ou d'oraux que la compétence de codage en autonomie complète sans assistance reste acquise."),
        ("🏴‍☠️ Pirate + 🤖 Cyborg", "Le Contrebandier Numérique", "Cumul de ruptures d'intégrité : importations USB / réseau et génération par IA externe. Authenticité de travail quasi inexistante.", "Convocation formelle pour manquement à la charte d'intégrité académique et nouvelle évaluation en conditions surveillées."),
        ("👑 Boss + 🏴‍☠️ Pirate", "L'Éclaireur Partageur", "Aisance technique indiscutable associée à des flux de partage de fichiers ou de code vers des tiers. Maîtrise le sujet mais diffuse.", "Rappeler le cadre déontologique : faire la distinction entre tutorat bienveillant et diffusion/collusion directe en évaluation."),
        ("🚪 Décrocheur + 🏴‍☠️ Pirate", "Le Copiste de Survie", "Démuni face au sujet et inactif, l'étudiant a importé la solution d'un voisin par clé USB ou partage réseau pour éviter la feuille blanche.", "Traiter la cause didactique racine (blocage complet) tout en appliquant le règlement pédagogique sur la fraude."),
        ("🔨 Artisan + 🏴‍☠️ Pirate", "L'Artisan Sous Influence", "Travail personnel soigné sur l'essentiel, complété par un fragment emprunté pour terminer.", "Valoriser la part authentique produite tout en invalidant le fragment collusoire pour encourager la persévérance honnête."),
        ("🔨 Artisan + 🚪 Décrocheur", "L'Apprenti Hésitant", "Démarche artisanale soignée lorsqu'il est actif, mais progression ralentie par de longs moments d'hésitation où il n'ose plus avancer.", "Stimuler la confiance en soi, dédramatiser les erreurs d'exécution et fixer des micro-objectifs pour rythmer la séance.")
    ]

    for r_idx, row in enumerate(hyb_data, start=1):
        for c_idx, val in enumerate(row):
            table_hyb.cell(r_idx, c_idx).paragraphs[0].text = val

    make_table_responsive(table_hyb, hyb_widths)

    doc.add_paragraph()
    doc.add_heading("Restitution dans les Rapports Individuels et Bilans de Cohorte", level=2)
    doc.add_paragraph(
        "• Dans la fiche individuelle (*_rapport.md) : la note finale affiche l'intitulé combiné et le surnom (ex: Profil : Le Boss + L'Artisan (L'Architecte Méthodique)), accompagné d'un cartouche explicatif détaillant la posture constatée et la recommandation pour l'enseignant. Dans le tableau diagnostique, les deux profils moteurs sont marqués '🟢 Double-profil actif'.\n"
        "• Dans la synthèse de cohorte (synthese_*.md) : le tableau récapitulatif reporte le double-profil dans la colonne 'Profil Dominant', et une section dédiée '## 🧬 Analyse des Profils Hybrides & Doubles-Profils Remarquables' dresse l'inventaire complet des étudiants hybrides de la promotion."
    )

    # -------------------------------------------------------------
    # 7. GUIDE D'EXPLOITATION PÉDAGOGIQUE
    # -------------------------------------------------------------
    h1 = doc.add_heading("7. Guide d'Exploitation pour les Enseignants & Tuteurs", level=1)
    for run in h1.runs: run.font.color.rgb = COLOR_NAVY

    doc.add_paragraph(
        "Pendant la Séance (Pilotage Temps Réel) :\n"
        "1. Prioriser les passages au poste : surveiller en premier lieu les badges ⚠️ Blocage (THRASH) pour débloquer les étudiants coincés dans une boucle d'erreurs, et vérifier les badges 🚨 Burst pour s'assurer que l'étudiant est capable d'expliquer oralement les lignes insérées.\n"
        "2. Auto-Acquittement : dès que l'étudiant applique votre conseil et réussit son run suivant, l'alerte s'éteint automatiquement dans les 2 secondes sur votre écran.\n\n"
        "Après la Séance (Bilan d'Audit & Évaluation) :\n"
        "1. Analyse télémétrique : exécuter activity_analyzer.py pour générer la synthèse globale et les fiches individuelles.\n"
        "2. Exploitation des doubles-profils : cibler immédiatement les Travailleurs Acharnés (Artisan + Casse-cou) pour une séance de consolidation sur les méthodes de débogage, et convoquer en entretien les profils Apprenti Sorcier et Délégataire Passif.\n"
        "3. Valorisation à l'évaluation : accorder un bonus de démarche à un profil Artisan persévérant et conditionner la note d'un profil Cyborg à une soutenance orale d'explication du code."
    )

    # Sauvegarde du document
    doc.save(str(OUT_DOCX))
    print(f"[DONE] Document DOCX généré avec succès ({os.path.getsize(OUT_DOCX)} octets) : {OUT_DOCX}")

if __name__ == "__main__":
    build_docx()

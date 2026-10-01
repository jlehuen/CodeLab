#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CodeLab - Générateur de fragments XML utilisateurs (build-xml.py)
Multiplateforme : Windows, macOS, Linux.
Zéro dépendance externe (utilise uniquement la bibliothèque standard Python).

Usage :
  python build-xml.py [options] [fichier_source.xlsx|csv] [fichier_sortie.xml]
"""

import sys
import os
import glob
import re
import csv
import zipfile
import xml.etree.ElementTree as ET
import xml.sax.saxutils as saxutils
from collections import Counter

# Activation des couleurs ANSI sous Windows 10+ si disponible
if os.name == 'nt':
    os.system('')

# Détection du support des couleurs
USE_COLORS = sys.stdout.isatty() and os.environ.get('TERM') != 'dumb'

if USE_COLORS:
    GREEN = '\033[0;32m'
    CYAN = '\033[0;36m'
    YELLOW = '\033[1;33m'
    RED = '\033[0;31m'
    BOLD = '\033[1m'
    DIM = '\033[2m'
    NC = '\033[0m'
else:
    GREEN = CYAN = YELLOW = RED = BOLD = DIM = NC = ''


def show_help():
    print(f"""{BOLD}Usage :{NC}
  python build-xml.py [options] [fichier_source.xlsx|csv] [fichier_sortie.xml]

{BOLD}Description :{NC}
  Convertit une liste d'utilisateurs (fichier Excel .xlsx ou CSV) en fragments
  XML <user login="..." status="..." groups="..." name="..." mail="..."/>
  compatibles avec sessions.dtd pour CodeLab.

{BOLD}Options :{NC}
  -o, --output <fichier>  Spécifie le fichier de sortie XML (défaut : <nom_source>.xml)
  -f, --force             Écrase le fichier de sortie sans confirmation
  -h, --help              Affiche cette aide et quitte

{BOLD}Exemples :{NC}
  python build-xml.py users.xlsx
  python build-xml.py users.xlsx out.xml
  python build-xml.py -o sessions_L1.xml users_L1.csv
  python build-xml.py     {DIM}# Mode interactif avec menu de sélection{NC}
""")
    sys.exit(0)


def clean(val):
    if val is None:
        return ""
    return str(val).strip()


def esc(val):
    return saxutils.escape(clean(val), {'"': '&quot;'})


def col_letter_to_index(col_str):
    idx = 0
    for ch in col_str:
        if ch.isalpha():
            idx = idx * 26 + (ord(ch.upper()) - ord('A') + 1)
    return idx - 1 if idx > 0 else 0


def read_xlsx_native(file_path):
    """Lecture d'un fichier .xlsx sans openpyxl ni xlsx2csv (standard library)."""
    with zipfile.ZipFile(file_path, 'r') as z:
        # 1. Lecture de la table des chaînes partagées (sharedStrings.xml)
        sst = []
        if 'xl/sharedStrings.xml' in z.namelist():
            sst_root = ET.fromstring(z.read('xl/sharedStrings.xml'))
            ns = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
            for si in sst_root.findall('m:si', ns):
                text_parts = [t.text or '' for t in si.findall('.//m:t', ns)]
                sst.append(''.join(text_parts))

        # 2. Détermination de la première feuille de calcul
        sheet_path = 'xl/worksheets/sheet1.xml'
        if sheet_path not in z.namelist():
            sheets = [n for n in z.namelist() if n.startswith('xl/worksheets/sheet') and n.endswith('.xml')]
            if not sheets:
                raise ValueError("Aucune feuille de calcul trouvée dans le fichier Excel.")
            sheet_path = sorted(sheets)[0]

        sheet_root = ET.fromstring(z.read(sheet_path))
        ns = {'m': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}

        rows_data = []
        for row in sheet_root.findall('.//m:row', ns):
            cells = {}
            max_col = 0
            for c in row.findall('m:c', ns):
                r_ref = c.get('r', '')
                t = c.get('t', '')
                v = c.find('m:v', ns)
                val = v.text if v is not None else ''

                if t == 's' and val:
                    try:
                        val = sst[int(val)]
                    except (IndexError, ValueError):
                        val = ''
                elif t == 'inlineStr':
                    inline_t = c.find('.//m:t', ns)
                    val = inline_t.text if inline_t is not None else ''

                col_idx = col_letter_to_index(''.join(ch for ch in r_ref if ch.isalpha()))
                cells[col_idx] = val
                if col_idx > max_col:
                    max_col = col_idx

            if cells:
                row_list = [cells.get(i, '') for i in range(max_col + 1)]
                rows_data.append(row_list)

        return rows_data


def read_csv_native(file_path):
    """Lecture d'un fichier CSV avec détection de séparateur et encodage."""
    with open(file_path, 'rb') as f:
        raw_bytes = f.read()

    # Détection encodage
    for enc in ('utf-8', 'utf-8-sig', 'cp1252', 'mac_roman', 'latin-1'):
        try:
            text = raw_bytes.decode(enc)
            break
        except UnicodeDecodeError:
            continue
    else:
        text = raw_bytes.decode('latin-1', errors='replace')

    lines = [l for l in text.splitlines() if l.strip()]
    if not lines:
        return []

    sample = "\n".join(lines[:10])
    try:
        dialect = csv.Sniffer().sniff(sample, delimiters=',;\t')
    except Exception:
        dialect = 'excel'

    reader = csv.reader(lines, dialect=dialect)
    return list(reader)


def resolve_columns(headers):
    col_map = {'login': -1, 'status': -1, 'groups': -1, 'name': -1, 'mail': -1}
    for idx, col in enumerate(headers):
        clean_col = re.sub(r'[^a-z0-9_]', '', col.lower())
        if clean_col in ('login', 'identifiant', 'id', 'numetu', 'etudiant', 'matricule'):
            if col_map['login'] == -1: col_map['login'] = idx
        elif clean_col in ('status', 'statut', 'role'):
            if col_map['status'] == -1: col_map['status'] = idx
        elif clean_col in ('groups', 'groupes', 'groupe', 'group'):
            if col_map['groups'] == -1: col_map['groups'] = idx
        elif clean_col in ('name', 'nom', 'nomprenom', 'etudiant'):
            if col_map['name'] == -1: col_map['name'] = idx
        elif clean_col in ('mail', 'email', 'courriel', 'mel'):
            if col_map['mail'] == -1: col_map['mail'] = idx

    # Repli sur les positions classiques 0..4
    if col_map['login'] == -1: col_map['login'] = 0
    if col_map['status'] == -1: col_map['status'] = 1
    if col_map['groups'] == -1: col_map['groups'] = 2
    if col_map['name'] == -1: col_map['name'] = 3
    if col_map['mail'] == -1: col_map['mail'] = 4
    return col_map


def main():
    args = sys.argv[1:]
    input_file = None
    output_file = None
    force = False

    idx = 0
    while idx < len(args):
        arg = args[idx]
        if arg in ('-h', '--help'):
            show_help()
        elif arg in ('-f', '--force'):
            force = True
        elif arg in ('-o', '--output'):
            idx += 1
            if idx >= len(args):
                sys.stderr.write(f"{RED}[Erreur]{NC} L'option -o requiert un nom de fichier.\n")
                sys.exit(1)
            output_file = args[idx]
        elif arg.startswith('-'):
            sys.stderr.write(f"{RED}[Erreur]{NC} Option inconnue : {arg}\n")
            sys.exit(1)
        else:
            if input_file is None:
                input_file = arg
            elif output_file is None:
                output_file = arg
            else:
                sys.stderr.write(f"{RED}[Erreur]{NC} Argument inattendu : {arg}\n")
                sys.exit(1)
        idx += 1

    # Mode interactif si aucun fichier spécifié
    if input_file is None:
        if not sys.stdin.isatty():
            sys.stderr.write(f"{RED}[Erreur]{NC} Aucun fichier d'entrée spécifié.\n")
            sys.stderr.write("Usage : python build-xml.py <source.xlsx|csv> [sortie.xml]\n")
            sys.exit(1)

        print(f"{BOLD}{CYAN}=== CodeLab : Générateur XML utilisateurs ==={NC}\n")

        # Recherche des fichiers Excel ou CSV dans le dossier courant
        patterns = ['*.xlsx', '*.xls', '*.csv']
        candidates = []
        for pat in patterns:
            candidates.extend(glob.glob(pat))

        # Trier en éliminant les doublons (casse sous Windows)
        candidates = sorted(list(set(candidates)))

        if candidates:
            print(f"{BOLD}Fichiers trouvés dans le dossier courant :{NC}")
            for i, cand in enumerate(candidates, 1):
                print(f"  {CYAN}{i}{NC}) {cand}")
            print("")
            choice = input(f"Choisissez un numéro (1-{len(candidates)}) ou glissez un fichier ici [1] : ").strip()
            choice = choice or "1"

            if choice.isdigit() and 1 <= int(choice) <= len(candidates):
                input_file = candidates[int(choice) - 1]
            else:
                input_file = choice
        else:
            print(f"{YELLOW}Aucun fichier .xlsx ou .csv trouvé dans le dossier courant.{NC}")
            input_file = input("Glissez-déposez un fichier ici (ou tapez son chemin) : ").strip()

    # Nettoyage des guillemets éventuels (glisser-déposer Windows ou macOS)
    input_file = input_file.strip().strip('"').strip("'").strip()

    if not input_file:
        sys.stderr.write(f"{RED}[Erreur]{NC} Aucun fichier sélectionné. Abandon.\n")
        sys.exit(1)

    if not os.path.isfile(input_file):
        sys.stderr.write(f"{RED}[Erreur]{NC} Le fichier '{input_file}' n'existe pas ou est inaccessible.\n")
        sys.exit(1)

    # Détermination du fichier de sortie
    if output_file is None:
        base, _ = os.path.splitext(input_file)
        output_file = f"{base}.xml"

    # Vérification d'écrasement en mode interactif
    if os.path.exists(output_file) and not force and sys.stdin.isatty():
        resp = input(f"Le fichier cible '{output_file}' existe déjà. L'écraser ? [O/n] : ").strip()
        resp = resp or "O"
        if resp.lower() not in ('o', 'y', 'oui', 'yes'):
            print(f"{YELLOW}Opération annulée par l'utilisateur.{NC}")
            sys.exit(0)

    print(f"\n{BOLD}Traitement en cours...{NC}")
    print(f"  {DIM}Source :{NC} {input_file}")
    print(f"  {DIM}Sortie :{NC} {output_file}")

    # Lecture des données selon l'extension
    ext = os.path.splitext(input_file)[1].lower()
    try:
        if ext in ('.xlsx', '.xls'):
            all_rows = read_xlsx_native(input_file)
        else:
            all_rows = read_csv_native(input_file)
    except Exception as e:
        sys.stderr.write(f"\n{RED}[Erreur]{NC} Impossible de lire '{input_file}' : {e}\n")
        sys.exit(1)

    if not all_rows:
        sys.stderr.write(f"\n{RED}[Erreur]{NC} Le fichier source est vide ou invalide.\n")
        sys.exit(1)

    # Détection de l'en-tête
    first_row = [clean(c).lower() for c in all_rows[0]]
    header_tokens = {'login', 'identifiant', 'id', 'status', 'statut', 'groups', 'groupes', 'nom', 'name', 'mail', 'email'}
    has_header = any(c in header_tokens for c in first_row)

    if has_header:
        col_map = resolve_columns(first_row)
        data_rows = all_rows[1:]
    else:
        col_map = resolve_columns([])
        data_rows = all_rows

    xml_lines = []
    group_counts = Counter()
    status_counts = Counter()
    with_mail_count = 0
    skipped_count = 0

    for row in data_rows:
        if not row or not any(row):
            continue

        def get_col(k):
            idx_c = col_map[k]
            return clean(row[idx_c]) if idx_c < len(row) else ""

        login = get_col('login')
        status = get_col('status').upper()
        groups = get_col('groups')
        name = get_col('name')
        mail = get_col('mail')

        if not login or login.lower() in ('login', 'identifiant'):
            skipped_count += 1
            continue

        if not status or status not in ('STUDENT', 'TUTOR', 'ADMIN'):
            status = 'STUDENT'

        status_counts[status] += 1
        if groups:
            for g in groups.split():
                group_counts[g] += 1
        if mail:
            with_mail_count += 1

        attrs = [
            f'login="{esc(login)}"',
            f'status="{esc(status)}"',
            f'groups="{esc(groups)}"',
            f'name="{esc(name)}"'
        ]
        if mail:
            attrs.append(f'mail="{esc(mail)}"')

        xml_lines.append(f'\t<user {" ".join(attrs)}/>\n')

    if not xml_lines:
        sys.stderr.write(f"\n{RED}[Erreur]{NC} Aucun utilisateur valide n'a pu être extrait.\n")
        sys.exit(1)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(xml_lines)

    total = len(xml_lines)
    print(f"\n{GREEN}[✓]{NC} {BOLD}{total} utilisateur(s) exporté(s) avec succès{NC} dans '{output_file}'.")

    if group_counts:
        groups_summary = ", ".join(f"{g} ({cnt})" for g, cnt in sorted(group_counts.items()))
        print(f"    • Groupes : {groups_summary}")

    status_summary = ", ".join(f"{s} ({cnt})" for s, cnt in sorted(status_counts.items()))
    print(f"    • Statuts : {status_summary}")
    print(f"    • Mails   : {with_mail_count}/{total} renseigné(s)")

    if skipped_count > 0:
        print(f"    • Lignes ignorées : {skipped_count}")

    print(f"\n{DIM}Astuce : copiez ces balises dans la section <users> de sessions.xml.{NC}")


if __name__ == '__main__':
    main()

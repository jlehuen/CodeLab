#!/bin/bash
# ==============================================================================
# CodeLab - Générateur de fragments XML utilisateurs (build-xml.sh)
# Convertit un fichier d'étudiants (Excel .xlsx ou CSV) en balises XML <user/>
# compatibles avec sessions.dtd pour sessions.xml
# ==============================================================================

set -eo pipefail

# Détection du support des couleurs
if [ -t 1 ]; then
    GREEN=$'\033[0;32m'
    CYAN=$'\033[0;36m'
    YELLOW=$'\033[1;33m'
    RED=$'\033[0;31m'
    BOLD=$'\033[1m'
    DIM=$'\033[2m'
    NC=$'\033[0m'
else
    GREEN=''
    CYAN=''
    YELLOW=''
    RED=''
    BOLD=''
    DIM=''
    NC=''
fi

# Nettoyage automatique des fichiers temporaires à la sortie
TMP_CSV=""
cleanup() {
    if [ -n "$TMP_CSV" ] && [ -f "$TMP_CSV" ]; then
        rm -f "$TMP_CSV"
    fi
}
trap cleanup EXIT INT TERM

# ------------------------------------------------------------------------------
# Aide / Usage
# ------------------------------------------------------------------------------

show_help() {
    cat << EOF
${BOLD}Usage :${NC}
  ./build-xml.sh [options] [fichier_source.xlsx|csv] [fichier_sortie.xml]

${BOLD}Description :${NC}
  Convertit une liste d'utilisateurs (fichier Excel .xlsx ou CSV) en fragments
  XML <user login="..." status="..." groups="..." name="..." mail="..."/>
  destinés à être insérés dans ${CYAN}sessions.xml${NC}.

${BOLD}Options :${NC}
  -o, --output <fichier>  Spécifie le fichier de sortie XML (défaut : <nom_source>.xml)
  -f, --force             Écrase le fichier de sortie sans demander confirmation
  -h, --help              Affiche cette aide et quitte

${BOLD}Exemples :${NC}
  ./build-xml.sh users_L1.xlsx
  ./build-xml.sh users_L1.xlsx out.xml
  ./build-xml.sh -o sessions_L2.xml users_L2.csv
  ./build-xml.sh          ${DIM}# Lance le mode interactif avec détection des fichiers${NC}

EOF
    exit 0
}

# ------------------------------------------------------------------------------
# Recherche de l'utilitaire xlsx2csv
# ------------------------------------------------------------------------------

get_xlsx2csv_cmd() {
    if command -v xlsx2csv >/dev/null 2>&1; then
        echo "xlsx2csv"
        return 0
    fi
    for p in \
        "/Library/Frameworks/Python.framework/Versions/3.*/bin/xlsx2csv" \
        "/opt/homebrew/bin/xlsx2csv" \
        "/usr/local/bin/xlsx2csv" \
        "$HOME/.local/bin/xlsx2csv" \
        "$HOME/Library/Python/3.*/bin/xlsx2csv"
    do
        for match in $p; do
            if [ -x "$match" ]; then
                echo "$match"
                return 0
            fi
        done
    done
    if python3 -m xlsx2csv -v >/dev/null 2>&1; then
        echo "python3 -m xlsx2csv"
        return 0
    fi
    return 1
}

# ------------------------------------------------------------------------------
# Analyse des arguments
# ------------------------------------------------------------------------------

INPUT_FILE=""
OUTPUT_FILE=""
FORCE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            show_help
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -o|--output)
            if [ -z "$2" ]; then
                echo -e "${RED}[Erreur]${NC} L'option $1 requiert un chemin de fichier." >&2
                exit 1
            fi
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -*)
            echo -e "${RED}[Erreur]${NC} Option inconnue : $1" >&2
            echo "Utilisez ./build-xml.sh --help pour voir les options disponibles." >&2
            exit 1
            ;;
        *)
            if [ -z "$INPUT_FILE" ]; then
                INPUT_FILE="$1"
            elif [ -z "$OUTPUT_FILE" ]; then
                OUTPUT_FILE="$1"
            else
                echo -e "${RED}[Erreur]${NC} Argument inattendu : $1" >&2
                exit 1
            fi
            shift
            ;;
    esac
done

# ------------------------------------------------------------------------------
# Mode interactif si aucun fichier d'entrée n'est passé
# ------------------------------------------------------------------------------

if [ -z "$INPUT_FILE" ]; then
    if [ ! -t 0 ]; then
        echo -e "${RED}[Erreur]${NC} Aucun fichier d'entrée spécifié." >&2
        echo "Usage: ./build-xml.sh <fichier.xlsx|fichier.csv> [fichier_sortie.xml]" >&2
        exit 1
    fi

    echo -e "${BOLD}${CYAN}=== CodeLab : Générateur XML utilisateurs ===${NC}\n"

    # Recherche des fichiers Excel ou CSV dans le dossier courant
    shopt -s nullglob nocaseglob
    CANDIDATES=( *.xlsx *.xls *.csv )
    shopt -u nullglob nocaseglob

    if [ ${#CANDIDATES[@]} -gt 0 ]; then
        echo -e "${BOLD}Fichiers trouvés dans le dossier courant :${NC}"
        for i in "${!CANDIDATES[@]}"; do
            echo -e "  ${CYAN}$((i+1))${NC}) ${CANDIDATES[$i]}"
        done
        echo ""
        read -r -p "Choisissez un numéro (1-${#CANDIDATES[@]}) ou glissez un fichier ici [1] : " USER_CHOICE
        USER_CHOICE="${USER_CHOICE:-1}"

        # Si l'utilisateur a tapé un numéro
        if [[ "$USER_CHOICE" =~ ^[0-9]+$ ]] && [ "$USER_CHOICE" -ge 1 ] && [ "$USER_CHOICE" -le "${#CANDIDATES[@]}" ]; then
            INPUT_FILE="${CANDIDATES[$((USER_CHOICE-1))]}"
        else
            INPUT_FILE="$USER_CHOICE"
        fi
    else
        echo -e "${YELLOW}Aucun fichier .xlsx ou .csv trouvé dans le dossier courant.${NC}"
        read -r -p "Glissez-déposez un fichier ici (ou tapez son chemin) : " INPUT_FILE
    fi
fi

# Nettoyage des guillemets et espaces superflus (cas du glisser-déposer sur macOS)
INPUT_FILE=$(echo "$INPUT_FILE" | sed -e "s/^['\"]//" -e "s/['\"]$//" -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

if [ -z "$INPUT_FILE" ]; then
    echo -e "${RED}[Erreur]${NC} Aucun fichier sélectionné. Abandon." >&2
    exit 1
fi

if [ ! -f "$INPUT_FILE" ]; then
    echo -e "${RED}[Erreur]${NC} Le fichier source '$INPUT_FILE' n'existe pas ou est inaccessible." >&2
    exit 1
fi

# ------------------------------------------------------------------------------
# Détermination du fichier de sortie
# ------------------------------------------------------------------------------

if [ -z "$OUTPUT_FILE" ]; then
    # Dérive le nom depuis l'entrée (ex: users_L1.xlsx -> users_L1.xml)
    BASENAME=$(basename "$INPUT_FILE")
    FILENAME="${BASENAME%.*}"
    DIRNAME=$(dirname "$INPUT_FILE")
    OUTPUT_FILE="${DIRNAME}/${FILENAME}.xml"
fi

# Confirmation d'écrasement si interactif
if [ -f "$OUTPUT_FILE" ] && [ "$FORCE" = false ] && [ -t 0 ]; then
    read -r -p "Le fichier cible '$OUTPUT_FILE' existe déjà. L'écraser ? [O/n] : " CONFIRM
    CONFIRM="${CONFIRM:-O}"
    case "$CONFIRM" in
        [oO]|[yY]|[oO][uU][iI])
            ;;
        *)
            echo -e "${YELLOW}Opération annulée par l'utilisateur.${NC}"
            exit 0
            ;;
    esac
fi

# ------------------------------------------------------------------------------
# Préparation du CSV source
# ------------------------------------------------------------------------------

EXT="${INPUT_FILE##*.}"
EXT=$(echo "$EXT" | tr '[:upper:]' '[:lower:]')

echo -e "\n${BOLD}Traitement en cours...${NC}"
echo -e "  ${DIM}Source :${NC} $INPUT_FILE"
echo -e "  ${DIM}Sortie :${NC} $OUTPUT_FILE"

CSV_TO_PARSE=""

if [ "$EXT" = "xlsx" ] || [ "$EXT" = "xls" ]; then
    XLSX2CSV_BIN=$(get_xlsx2csv_cmd || true)
    if [ -z "$XLSX2CSV_BIN" ]; then
        echo -e "\n${RED}[Erreur]${NC} L'outil 'xlsx2csv' est introuvable." >&2
        echo "Pour convertir directement les fichiers Excel .xlsx, installez-le avec :" >&2
        echo -e "  ${CYAN}pip3 install xlsx2csv${NC}  (ou ${CYAN}brew install xlsx2csv${NC})" >&2
        echo -e "Ou exportez votre fichier au format CSV puis relancez le script.\n" >&2
        exit 1
    fi
    TMP_CSV=$(mktemp /tmp/codelab_users_XXXXXX.csv)
    $XLSX2CSV_BIN "$INPUT_FILE" "$TMP_CSV"
    CSV_TO_PARSE="$TMP_CSV"
else
    CSV_TO_PARSE="$INPUT_FILE"
fi

# ------------------------------------------------------------------------------
# Parsing Python & Génération XML
# ------------------------------------------------------------------------------

python3 - "$CSV_TO_PARSE" "$OUTPUT_FILE" << 'PY_EOF'
import sys
import csv
import re
import xml.sax.saxutils as saxutils
from collections import Counter

csv_file = sys.argv[1]
out_file = sys.argv[2]

def clean(val):
    if val is None:
        return ""
    return str(val).strip()

def esc(val):
    # Échappe les caractères réservés XML (&, <, >, ")
    return saxutils.escape(clean(val), {'"': '&quot;'})

# Lecture avec détection de séparateur et encodage UTF-8 (repli latin-1)
raw_bytes = open(csv_file, 'rb').read()
try:
    text = raw_bytes.decode('utf-8')
except UnicodeDecodeError:
    try:
        text = raw_bytes.decode('mac_roman')
    except UnicodeDecodeError:
        text = raw_bytes.decode('latin-1', errors='replace')

lines = [l for l in text.splitlines() if l.strip()]
if not lines:
    sys.stderr.write("\033[0;31m[Erreur]\033[0m Le fichier source est vide.\n")
    sys.exit(1)

# Détection du séparateur (',', ';', '\t')
sample = "\n".join(lines[:10])
try:
    dialect = csv.Sniffer().sniff(sample, delimiters=',;\t')
except Exception:
    dialect = 'excel'

reader = csv.reader(lines, dialect=dialect)
all_rows = list(reader)

if not all_rows:
    sys.stderr.write("\033[0;31m[Erreur]\033[0m Aucune ligne exploitable dans le fichier.\n")
    sys.exit(1)

# Détection de l'en-tête
first_row = [clean(c).lower() for c in all_rows[0]]

def is_header(row):
    tokens = {'login', 'identifiant', 'id', 'status', 'statut', 'groups', 'groupes', 'nom', 'name', 'mail', 'email'}
    return any(c in tokens for c in row)

has_header = is_header(first_row)

col_map = {'login': -1, 'status': -1, 'groups': -1, 'name': -1, 'mail': -1}

if has_header:
    for idx, col in enumerate(first_row):
        col_clean = re.sub(r'[^a-z0-9_]', '', col)
        if col_clean in ('login', 'identifiant', 'id', 'numetu', 'etudiant'):
            if col_map['login'] == -1: col_map['login'] = idx
        elif col_clean in ('status', 'statut', 'role'):
            if col_map['status'] == -1: col_map['status'] = idx
        elif col_clean in ('groups', 'groupes', 'groupe', 'group'):
            if col_map['groups'] == -1: col_map['groups'] = idx
        elif col_clean in ('name', 'nom', 'nomprenom', 'etudiant'):
            if col_map['name'] == -1: col_map['name'] = idx
        elif col_clean in ('mail', 'email', 'courriel', 'mel'):
            if col_map['mail'] == -1: col_map['mail'] = idx
    data_rows = all_rows[1:]
else:
    data_rows = all_rows

# Indices par défaut si non résolus
if col_map['login'] == -1: col_map['login'] = 0
if col_map['status'] == -1: col_map['status'] = 1
if col_map['groups'] == -1: col_map['groups'] = 2
if col_map['name'] == -1: col_map['name'] = 3
if col_map['mail'] == -1: col_map['mail'] = 4

xml_lines = []
group_counts = Counter()
status_counts = Counter()
with_mail_count = 0
skipped_count = 0

for row in data_rows:
    if not row or not any(row):
        continue

    get_col = lambda k: clean(row[col_map[k]]) if col_map[k] < len(row) else ""

    login = get_col('login')
    status = get_col('status').upper()
    groups = get_col('groups')
    name = get_col('name')
    mail = get_col('mail')

    # Ignorer la répétition d'en-tête éventuelle ou les lignes sans login
    if not login or login.lower() in ('login', 'identifiant'):
        skipped_count += 1
        continue

    # Valeurs par défaut conformes à sessions.dtd
    if not status or status not in ('STUDENT', 'TUTOR', 'ADMIN'):
        status = 'STUDENT'

    status_counts[status] += 1
    if groups:
        for g in groups.split():
            group_counts[g] += 1
    if mail:
        with_mail_count += 1

    # Construction de la balise XML
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
    sys.stderr.write("\033[0;31m[Erreur]\033[0m Aucun utilisateur valide n'a pu être extrait.\n")
    sys.exit(1)

with open(out_file, 'w', encoding='utf-8') as f:
    f.writelines(xml_lines)

# Rapport de synthèse
total = len(xml_lines)
print(f"\033[0;32m[✓]\033[0m \033[1m{total} utilisateur(s) exporté(s) avec succès\033[0m dans '{out_file}'.")

# Détail des groupes
if group_counts:
    groups_summary = ", ".join(f"{g} ({cnt})" for g, cnt in sorted(group_counts.items()))
    print(f"    • Groupes : {groups_summary}")

# Détail des statuts & mails
status_summary = ", ".join(f"{s} ({cnt})" for s, cnt in sorted(status_counts.items()))
print(f"    • Statuts : {status_summary}")
print(f"    • Mails   : {with_mail_count}/{total} renseigné(s)")

if skipped_count > 0:
    print(f"    • Lignes ignorées (en-tête ou login vide) : {skipped_count}")

print(f"\n\033[2mAstuce : copiez ces balises dans la section <users> de sessions.xml.\033[0m")
PY_EOF

#!/bin/bash
# ==============================================================================
# CodeLab Server - Consultation, restauration et purge de la corbeille serveur
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/server/sessions" ] || [ -d "$SCRIPT_DIR/server/trash" ]; then
    SERVER_DIR="$SCRIPT_DIR/server"
else
    SERVER_DIR="$SCRIPT_DIR"
fi

TRASH_DIR="$SERVER_DIR/trash"
SESSIONS_DIR="$SERVER_DIR/sessions"

usage() {
    echo "Usage :"
    echo "  ./restore-trash.sh                     # Mode interactif (choix session -> étudiant -> fichier)"
    echo "  ./restore-trash.sh <session> <login>   # Restaurer un élément d'un étudiant précis"
    echo "  ./restore-trash.sh --list              # Lister tout le contenu de la corbeille"
    echo "  ./restore-trash.sh --purge <jours>     # Supprimer les fichiers de plus de <jours> jours"
}

if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    usage
    exit 0
fi

if [ ! -d "$TRASH_DIR" ]; then
    echo "ℹ️  La corbeille serveur ($TRASH_DIR) n'existe pas encore (aucune suppression enregistrée)."
    exit 0
fi

# ------------------------------------------------------------------------------
# Mode --list : vue d'ensemble de la corbeille
# ------------------------------------------------------------------------------
if [ "$1" = "--list" ]; then
    echo "🗑️  Contenu de la corbeille : $TRASH_DIR"
    echo "------------------------------------------------------------------------------"
    FOUND=0
    for sess_path in "$TRASH_DIR"/*; do
        [ -d "$sess_path" ] || continue
        sess="$(basename "$sess_path")"
        for login_path in "$sess_path"/*; do
            [ -d "$login_path" ] || continue
            login="$(basename "$login_path")"
            for ts_path in $(ls -1r "$login_path" 2>/dev/null); do
                full_ts="$login_path/$ts_path"
                [ -d "$full_ts" ] || continue
                FOUND=1
                date_fmt="20${ts_path:0:2}-${ts_path:2:2}-${ts_path:4:2} ${ts_path:7:2}:${ts_path:9:2}:${ts_path:11:2}"
                items="$(cd "$full_ts" && find . -mindepth 1 -maxdepth 2 ! -name '*.bak' | sed 's|^\./||' | tr '\n' ' ')"
                echo "  [$date_fmt]  $sess / $login  ->  $items"
            done
        done
    done
    if [ "$FOUND" -eq 0 ]; then
        echo "  (Corbeille vide)"
    fi
    exit 0
fi

# ------------------------------------------------------------------------------
# Mode --purge <jours> : nettoyage des anciennes suppressions
# ------------------------------------------------------------------------------
if [ "$1" = "--purge" ]; then
    DAYS="$2"
    if ! [[ "$DAYS" =~ ^[0-9]+$ ]]; then
        echo "❌ Erreur : précisez un nombre de jours entier (ex: ./restore-trash.sh --purge 90)"
        exit 1
    fi
    COUNT="$(find "$TRASH_DIR" -type f -mtime +"$DAYS" | wc -l | tr -d ' ')"
    if [ "$COUNT" -eq 0 ]; then
        echo "✅ Aucun fichier de plus de $DAYS jours dans la corbeille."
        exit 0
    fi
    echo "⚠️  $COUNT fichier(s) de plus de $DAYS jours vont être définitivement supprimés de $TRASH_DIR."
    read -r -p "Confirmer la purge ? (o/N) : " CONFIRM
    if [[ "$CONFIRM" =~ ^[oOyY]$ ]]; then
        find "$TRASH_DIR" -type f -mtime +"$DAYS" -delete
        find "$TRASH_DIR" -type d -empty -delete
        echo "✅ Purge terminée ($COUNT fichier(s) supprimé(s))."
    else
        echo "Annulé."
    fi
    exit 0
fi

# ------------------------------------------------------------------------------
# Sélection de la session
# ------------------------------------------------------------------------------
SESSION="$1"
LOGIN="$2"

if [ -z "$SESSION" ]; then
    SESSIONS=()
    for d in "$TRASH_DIR"/*; do
        [ -d "$d" ] && SESSIONS+=("$(basename "$d")")
    done
    if [ "${#SESSIONS[@]}" -eq 0 ]; then
        echo "ℹ️  La corbeille est vide."
        exit 0
    fi
    echo "📂 Sessions présentes dans la corbeille :"
    for i in "${!SESSIONS[@]}"; do
        printf "  [%d] %s\n" $((i + 1)) "${SESSIONS[$i]}"
    done
    read -r -p "Numéro de session : " S_IDX
    if ! [[ "$S_IDX" =~ ^[0-9]+$ ]] || [ "$S_IDX" -lt 1 ] || [ "$S_IDX" -gt "${#SESSIONS[@]}" ]; then
        echo "❌ Choix invalide."
        exit 1
    fi
    SESSION="${SESSIONS[$((S_IDX - 1))]}"
fi

if [ ! -d "$TRASH_DIR/$SESSION" ]; then
    echo "❌ Aucune corbeille trouvée pour la session '$SESSION'."
    exit 1
fi

# ------------------------------------------------------------------------------
# Sélection de l'étudiant (login)
# ------------------------------------------------------------------------------
if [ -z "$LOGIN" ]; then
    LOGINS=()
    for d in "$TRASH_DIR/$SESSION"/*; do
        [ -d "$d" ] && LOGINS+=("$(basename "$d")")
    done
    if [ "${#LOGINS[@]}" -eq 0 ]; then
        echo "ℹ️  Aucun étudiant dans la corbeille pour '$SESSION'."
        exit 0
    fi
    echo "👤 Étudiants avec fichiers supprimés dans '$SESSION' :"
    for i in "${!LOGINS[@]}"; do
        CNT="$(ls -1 "$TRASH_DIR/$SESSION/${LOGINS[$i]}" 2>/dev/null | wc -l | tr -d ' ')"
        printf "  [%d] %s (%s suppression(s))\n" $((i + 1)) "${LOGINS[$i]}" "$CNT"
    done
    read -r -p "Numéro d'étudiant : " L_IDX
    if ! [[ "$L_IDX" =~ ^[0-9]+$ ]] || [ "$L_IDX" -lt 1 ] || [ "$L_IDX" -gt "${#LOGINS[@]}" ]; then
        echo "❌ Choix invalide."
        exit 1
    fi
    LOGIN="${LOGINS[$((L_IDX - 1))]}"
fi

USER_TRASH="$TRASH_DIR/$SESSION/$LOGIN"
if [ ! -d "$USER_TRASH" ]; then
    echo "❌ Aucune corbeille trouvée pour '$SESSION/$LOGIN'."
    exit 1
fi

# ------------------------------------------------------------------------------
# Sélection de l'entrée horodatée à restaurer (plus récentes en premier)
# ------------------------------------------------------------------------------
ENTRIES=()
while IFS= read -r ts; do
    [ -d "$USER_TRASH/$ts" ] && ENTRIES+=("$ts")
done < <(ls -1r "$USER_TRASH" 2>/dev/null)

if [ "${#ENTRIES[@]}" -eq 0 ]; then
    echo "ℹ️  Aucune entrée dans la corbeille pour '$SESSION/$LOGIN'."
    exit 0
fi

echo ""
echo "🗑️  Suppressions pour $SESSION / $LOGIN (plus récentes en haut) :"
for i in "${!ENTRIES[@]}"; do
    ts="${ENTRIES[$i]}"
    date_fmt="20${ts:0:2}-${ts:2:2}-${ts:4:2} ${ts:7:2}:${ts:9:2}:${ts:11:2}"
    items="$(cd "$USER_TRASH/$ts" && find . -mindepth 1 -maxdepth 2 ! -name '*.bak' | sed 's|^\./||' | tr '\n' ' ')"
    printf "  [%d] %s  ->  %s\n" $((i + 1)) "$date_fmt" "$items"
done

read -r -p "Numéro à restaurer (Entrée pour annuler) : " E_IDX
if [ -z "$E_IDX" ]; then
    echo "Annulé."
    exit 0
fi
if ! [[ "$E_IDX" =~ ^[0-9]+$ ]] || [ "$E_IDX" -lt 1 ] || [ "$E_IDX" -gt "${#ENTRIES[@]}" ]; then
    echo "❌ Choix invalide."
    exit 1
fi

CHOSEN_TS="${ENTRIES[$((E_IDX - 1))]}"
SRC_DIR="$USER_TRASH/$CHOSEN_TS"
DEST_DIR="$SESSIONS_DIR/$SESSION/$LOGIN"

mkdir -p "$DEST_DIR"

# Vérifier les conflits éventuels avant copie
CONFLICTS=()
while IFS= read -r rel; do
    rel="${rel#./}"
    if [ -e "$DEST_DIR/$rel" ]; then
        CONFLICTS+=("$rel")
    fi
done < <(cd "$SRC_DIR" && find . -mindepth 1 -type f ! -name '*.bak')

if [ "${#CONFLICTS[@]}" -gt 0 ]; then
    echo "⚠️  Attention : les fichiers suivants existent déjà dans $DEST_DIR :"
    for c in "${CONFLICTS[@]}"; do
        echo "   - $c"
    done
    read -r -p "Écraser les fichiers existants ? (o/N) : " OVERWRITE
    if ! [[ "$OVERWRITE" =~ ^[oOyY]$ ]]; then
        echo "Annulé."
        exit 0
    fi
fi

# Restauration (fichiers + backups associés)
cp -a "$SRC_DIR"/. "$DEST_DIR"/
echo "✅ Restauration réussie vers : $DEST_DIR"

read -r -p "Supprimer cette entrée de la corbeille maintenant ? (o/N) : " CLEAN_TRASH
if [[ "$CLEAN_TRASH" =~ ^[oOyY]$ ]]; then
    rm -rf "$SRC_DIR"
    find "$TRASH_DIR/$SESSION" -type d -empty -delete 2>/dev/null || true
    echo "🧹 Entrée retirée de la corbeille."
fi

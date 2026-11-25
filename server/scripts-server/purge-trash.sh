#!/bin/bash
# ==============================================================================
# CodeLab Server - Purge périodique de la corbeille serveur (défaut : 90 jours)
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/server/sessions" ] || [ -d "$SCRIPT_DIR/server/trash" ]; then
    SERVER_DIR="$SCRIPT_DIR/server"
else
    SERVER_DIR="$SCRIPT_DIR"
fi

TRASH_DIR="$SERVER_DIR/trash"
DAYS=90
FORCE=0
DRY_RUN=0

for arg in "$@"; do
    case "$arg" in
        -h|--help)
            echo "Usage :"
            echo "  ./purge-trash.sh                 # Purge les fichiers de plus de 90 jours (avec confirmation)"
            echo "  ./purge-trash.sh <jours>         # Purge les fichiers de plus de <jours> jours"
            echo "  ./purge-trash.sh --dry-run       # Simulation sans suppression"
            echo "  ./purge-trash.sh --force         # Sans confirmation (pour cron)"
            exit 0
            ;;
        -f|--force|-y|--yes)
            FORCE=1
            ;;
        -n|--dry-run)
            DRY_RUN=1
            ;;
        *)
            if [[ "$arg" =~ ^[0-9]+$ ]]; then
                DAYS="$arg"
            else
                echo "❌ Argument inconnu : $arg (utilisez --help)"
                exit 1
            fi
            ;;
    esac
done

if [ ! -d "$TRASH_DIR" ]; then
    echo "ℹ️  La corbeille ($TRASH_DIR) n'existe pas encore."
    exit 0
fi

COUNT="$(find "$TRASH_DIR" -type f -mtime +"$DAYS" | wc -l | tr -d ' ')"

if [ "$COUNT" -eq 0 ]; then
    echo "✅ Aucun fichier de plus de $DAYS jours dans $TRASH_DIR."
    exit 0
fi

if [ "$DRY_RUN" -eq 1 ]; then
    echo "🔍 Simulation (--dry-run) : $COUNT fichier(s) de plus de $DAYS jours dans $TRASH_DIR :"
    find "$TRASH_DIR" -type f -mtime +"$DAYS"
    exit 0
fi

if [ "$FORCE" -eq 0 ]; then
    echo "⚠️  $COUNT fichier(s) de plus de $DAYS jours vont être définitivement supprimés de $TRASH_DIR."
    read -r -p "Confirmer la purge ? (o/N) : " CONFIRM
    if ! [[ "$CONFIRM" =~ ^[oOyY]$ ]]; then
        echo "Annulé."
        exit 0
    fi
fi

find "$TRASH_DIR" -type f -mtime +"$DAYS" -delete
find "$TRASH_DIR" -mindepth 1 -type d -empty -delete

echo "🧹 Purge terminée : $COUNT fichier(s) de plus de $DAYS jours supprimé(s) dans $TRASH_DIR."

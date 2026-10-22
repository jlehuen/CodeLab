#!/bin/bash
# ==============================================================================
# CodeLab Server - Consultation et suivi des journaux du serveur
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Recherche locale du journal
LOCAL_LOG=""
if [ -f "$SCRIPT_DIR/server/logs/server.log" ]; then
    LOCAL_LOG="$SCRIPT_DIR/server/logs/server.log"
elif [ -f "$SCRIPT_DIR/logs/server.log" ]; then
    LOCAL_LOG="$SCRIPT_DIR/logs/server.log"
fi

# 1. Mode local (exécuté sur la machine hébergeant le serveur)
if [ -n "$LOCAL_LOG" ]; then
    LINES="50"
    FOLLOW=false

    for arg in "$@"; do
        case "$arg" in
            -f|--follow)
                FOLLOW=true
                ;;
            *)
                if [[ "$arg" =~ ^[0-9]+$ ]]; then
                    LINES="$arg"
                fi
                ;;
        esac
    done

    if [ "$FOLLOW" = true ]; then
        echo "📋 Suivi en temps réel des logs ($LOCAL_LOG)... (Ctrl+C pour quitter)"
        tail -n "$LINES" -f "$LOCAL_LOG"
    else
        echo "📋 Dernières $LINES lignes de logs ($LOCAL_LOG) :"
        echo "------------------------------------------------------------------------"
        tail -n "$LINES" "$LOCAL_LOG"
        echo "------------------------------------------------------------------------"
        echo "💡 Astuce : Utilisez '$0 -f' pour suivre les logs en temps réel."
    fi
    exit 0
fi

# 2. Mode distant (si exécuté depuis un poste distant sans logs locaux)
REMOTE_HOST="${CODELAB_REMOTE_HOST:-}"

if [ -z "$REMOTE_HOST" ] && [ -n "$1" ] && [[ "$1" == *"@"* ]]; then
    REMOTE_HOST="$1"
    shift
fi

if [ -z "$REMOTE_HOST" ]; then
    echo "📋 Consultation des logs du serveur CodeLab"
    echo ""
    echo "Usage local (sur le serveur) :"
    echo "   $0 [-f] [nombre_lignes]"
    echo ""
    echo "Usage distant (depuis un poste d'administration) :"
    echo "   CODELAB_REMOTE_HOST=user@host $0 [-f]"
    echo "   ou : $0 user@host [-f]"
    exit 1
fi

REMOTE_PATH="${CODELAB_REMOTE_PATH:-~/codelab-server/server/logs/server.log}"
echo "📡 Récupération des logs depuis $REMOTE_HOST:$REMOTE_PATH..."
mkdir -p "$SCRIPT_DIR/temp"
scp "$REMOTE_HOST:$REMOTE_PATH" "$SCRIPT_DIR/temp/server.log"

if command -v open >/dev/null 2>&1; then
    open "$SCRIPT_DIR/temp/server.log"
else
    tail -n 100 "$SCRIPT_DIR/temp/server.log"
fi

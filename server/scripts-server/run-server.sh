#!/bin/bash
# ==============================================================================
# CodeLab Server - Démarrage du serveur en arrière-plan
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"
PORT="${PORT:-9988}"

if [ ! -d "$SERVER_DIR" ]; then
    echo "❌ Erreur : Dossier '$SERVER_DIR' introuvable."
    exit 1
fi

# Détection de Cargo / Rust
if command -v cargo >/dev/null 2>&1; then
    CARGO="cargo"
elif [ -x "$HOME/.cargo/bin/cargo" ]; then
    CARGO="$HOME/.cargo/bin/cargo"
    export PATH="$HOME/.cargo/bin:$PATH"
else
    echo "❌ Erreur : Cargo/Rust n'est pas installé ou n'est pas dans le PATH."
    exit 1
fi

start_server() {
    cd "$SERVER_DIR"
    mkdir -p logs

    # Démarrage persistant en arrière-plan (immunisé contre la fermeture SSH)
    nohup "$CARGO" run "$@" > logs/stdout.log 2>&1 &
    PID=$!
    disown "$PID" 2>/dev/null || true

    echo "🚀 Serveur CodeLab démarré en arrière-plan (PID : $PID)"
    echo "   Port client  : $PORT"
    echo "   Port admin   : 9989 (défaut)"
    echo "   Journaux     : $SERVER_DIR/logs/server.log"
}

if PID=$(lsof -t -i TCP:"$PORT" 2>/dev/null); then
    echo "⚠️ Attention : Un serveur écoute déjà sur le port $PORT (PID : $PID)."
    echo "   Pour l'arrêter, utilisez : ./kill-server.sh"
else
    start_server "$@"
fi

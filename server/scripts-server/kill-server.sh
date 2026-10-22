#!/bin/bash
# ==============================================================================
# CodeLab Server - Arrêt du serveur
# ==============================================================================

PORT="${PORT:-9988}"

# Recherche du ou des processus écoutant sur le port
PIDS=$(lsof -t -i TCP:"$PORT" 2>/dev/null || true)

if [ -n "$PIDS" ]; then
    echo "🛑 Arrêt du serveur CodeLab sur le port $PORT (PID : $PIDS)..."
    kill $PIDS 2>/dev/null || kill -9 $PIDS 2>/dev/null
    sleep 1
    # Vérification et arrêt forcé si nécessaire
    REMAINING=$(lsof -t -i TCP:"$PORT" 2>/dev/null || true)
    if [ -n "$REMAINING" ]; then
        echo "⚠️ Forçage de l'arrêt (SIGKILL sur PID : $REMAINING)..."
        kill -9 $REMAINING 2>/dev/null || true
    fi
    echo "✅ Serveur arrêté avec succès."
else
    echo "ℹ️ Aucun serveur CodeLab actif sur le port $PORT."
fi

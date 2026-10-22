#!/bin/bash
# ==============================================================================
# CodeLab Server - Décompression et installation du serveur
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f "server.tgz" ]; then
    echo "📦 Décompression de l'archive server.tgz..."
    tar --warning=no-unknown-keyword -xzf server.tgz 2>/dev/null || tar -xzf server.tgz
    echo "✅ Décompression terminée."
elif [ ! -d "server" ]; then
    echo "❌ Erreur : Ni archive 'server.tgz' ni dossier 'server/' trouvé dans $SCRIPT_DIR."
    exit 1
fi

echo "🔨 Compilation du serveur..."
if [ -x "./compile-server.sh" ]; then
    ./compile-server.sh "$@"
elif [ -x "$SCRIPT_DIR/compile-server.sh" ]; then
    "$SCRIPT_DIR/compile-server.sh" "$@"
else
    echo "❌ Erreur : Script 'compile-server.sh' introuvable ou non exécutable."
    exit 1
fi

echo ""
echo "✨ Installation terminée. Pour démarrer le serveur : ./run-server.sh"

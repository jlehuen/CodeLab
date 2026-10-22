#!/bin/bash
# ==============================================================================
# CodeLab Server - Compilation du serveur Rust
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$SCRIPT_DIR/server"

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
    echo "   Pour installer Rust : curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
    exit 1
fi

echo "🔨 Compilation du serveur CodeLab (${CARGO})..."
cd "$SERVER_DIR"
"$CARGO" build "$@"

echo "✅ Compilation terminée avec succès."

#!/bin/bash
set -e

cd "$(dirname "$0")"

# Détection automatique de l'architecture locale (arm64 ou x86_64) par défaut
DEFAULT_ARCH="-arm"
if [ "$(uname -m)" = "x86_64" ]; then
    DEFAULT_ARCH="-x64"
fi

# Utiliser le paramètre fourni s'il existe, sinon l'architecture de la machine courante
ARCH="${1:-$DEFAULT_ARCH}"

./distrib-mac.sh "$ARCH" -app

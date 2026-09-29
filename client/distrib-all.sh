#!/bin/bash
set -e

cd "$(dirname "$0")"
BASE=$(pwd)
TARGET="${TARGET:-$HOME/Desktop}"

# Extraction de la version et du build
VERSION=$(pcregrep -o1 'VERSION = "(.*)"' src/codelab/AbstractCodeLab.java)
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' src/codelab/AbstractCodeLab.java)

echo "================================================================"
echo "Génération des distributions CodeLab $VERSION (build $BUILD)"
echo "================================================================"

# 1. Distribution macOS Apple Silicon (ARM64)
./distrib-mac.sh -arm

# 2. Distribution macOS Intel (x86_64)
./distrib-mac.sh -x64

# 3. Distribution Windows 64-bit
./distrib-win64-nsis.sh

# 4. Distribution Linux 64-bit
./distrib-linux.sh

# 5. Finalisation du dossier de distribution sur le Bureau
echo "----------------------------------------------------------------"
echo "Finalisation du dossier $TARGET/$VERSION..."
echo "----------------------------------------------------------------"
mkdir -p "$TARGET/$VERSION"
rm -f "$TARGET/$VERSION"/*.md5 "$TARGET/$VERSION"/*.sha* 2>/dev/null || true
cp "$BASE/launchers/index.html" "$TARGET/$VERSION/"

echo "================================================================"
echo "Succès : toutes les distributions sont prêtes dans $TARGET/$VERSION"
echo "================================================================"

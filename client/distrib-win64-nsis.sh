#!/bin/bash
set -e

cd "$(dirname "$0")"
BASE=$(pwd)
TARGET="${TARGET:-$HOME/Desktop}"

## 1. Vérification des outils nécessaires
if ! command -v pcregrep >/dev/null 2>&1; then
    echo "Erreur : 'pcregrep' est introuvable. Installez-le avec 'brew install pcre'." >&2
    exit 1
fi

if ! command -v makensis >/dev/null 2>&1; then
    echo "================================================================" >&2
    echo "Erreur : 'makensis' (NSIS) est introuvable." >&2
    echo "Pour installer le compilateur d'installateurs Windows sur macOS :" >&2
    echo "    brew install makensis" >&2
    echo "================================================================" >&2
    exit 1
fi

## 2. Extraction dynamique de la version et du build
VERSION=$(pcregrep -o1 'VERSION = "(.*)"' src/codelab/AbstractCodeLab.java)
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' src/codelab/AbstractCodeLab.java)

if [ -z "$VERSION" ] || [ -z "$BUILD" ]; then
    echo "Erreur : impossible d'extraire VERSION ou BUILD depuis src/codelab/AbstractCodeLab.java" >&2
    exit 1
fi

FILENAME="CodeLab-Win64-Payload"
PAYLOAD="$TARGET/$FILENAME"
SETUP_NAME="CodeLab-Win64-$VERSION-Setup"
OUTPUT_DIR="$TARGET/$VERSION"
OUTPUT_EXE="$OUTPUT_DIR/$SETUP_NAME.exe"

echo "================================================================"
echo "Génération de l'installateur Windows 64-bit (NSIS)"
echo "CodeLab $VERSION (build $BUILD)"
echo "Livrable cible : $OUTPUT_EXE"
echo "================================================================"

## 3. Contrôle des prérequis
for required_path in "hidden" "codelab.files" "java/windows_64/JDK-17.0.8.1+1" "natives/windows_64" "bin/windows_64" "launchers/windows/codelab.nsi" "launchers/icon/codelab.ico"; do
    if [ ! -e "$required_path" ]; then
        echo "Erreur critique : le composant '$required_path' est introuvable." >&2
        exit 1
    fi
done

## 4. Préparation du dossier payload applicatif
echo "1. Assemblage du payload applicatif..."
rm -rf "$PAYLOAD"
mkdir -p "$PAYLOAD"
cp -r hidden "$PAYLOAD/.hidden"

echo "   - Copie des configurations Windows..."
cp sys-properties/sysconfig-win.properties "$PAYLOAD/.hidden/codelab/sysconfig.properties"
cp user-properties/user-win.properties "$PAYLOAD/.hidden/codelab/user.properties.bak"

echo "   - Copie des librairies natives (JInput)..."
mkdir -p "$PAYLOAD/.hidden/codelab/natives"
cp -f natives/windows_64/* "$PAYLOAD/.hidden/codelab/natives/"

echo "   - Copie des utilitaires (AStyle, CLIPS)..."
mkdir -p "$PAYLOAD/.hidden/bin"
cp -f bin/windows_64/* "$PAYLOAD/.hidden/bin/"

echo "   - Copie du runtime JDK 17 Windows..."
cp -r java/windows_64/JDK-17.0.8.1+1 "$PAYLOAD/.hidden/"

echo "   - Copie des launchers et documentations..."
cp launchers/windows/codelab.exe "$PAYLOAD/"
cp launchers/windows/codelab.bat "$PAYLOAD/"
cp launchers/windows/kill-codelab.bat "$PAYLOAD/"
cp launchers/windows/patch-codelab.bat "$PAYLOAD/"
cp launchers/icon/codelab.ico "$PAYLOAD/"
cp launchers/README.TXT "$PAYLOAD/"

echo "   - Compression de codelab.files..."
cp -r codelab.files "$PAYLOAD/.hidden/codelab/"
(
    cd "$PAYLOAD/.hidden/codelab/"
    find codelab.files -name ".DS_Store" -delete 2>/dev/null || true
    find codelab.files -name "._*" -delete 2>/dev/null || true
    cp user.properties.bak codelab.files/userdata/user.properties
    mv codelab.files/hidden codelab.files/.hidden
    zip -qr files.zip codelab.files -x "*.DS_Store*" -x "*._*" -x "*__MACOSX*"
    rm -rf codelab.files
)

echo "   - Nettoyage des attributs et fichiers cachés macOS..."
chmod -R u+w "$PAYLOAD"
find "$PAYLOAD" -name ".DS_Store" -delete 2>/dev/null || true
find "$PAYLOAD" -name "._*" -delete 2>/dev/null || true
xattr -cr "$PAYLOAD" 2>/dev/null || true
export COPYFILE_DISABLE=1
dot_clean "$PAYLOAD" 2>/dev/null || true

## 5. Compilation de l'installateur NSIS
echo "2. Compilation de l'installateur avec makensis (compression LZMA solide)..."
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_EXE"

makensis \
    -DVERSION="$VERSION" \
    -DBUILD="$BUILD" \
    -DPAYLOAD_DIR="$PAYLOAD" \
    -DOUTPUT_EXE="$OUTPUT_EXE" \
    -DICON_FILE="$BASE/launchers/icon/codelab.ico" \
    "$BASE/launchers/windows/codelab.nsi"

## 6. Nettoyage du payload temporaire
echo "3. Nettoyage de l'espace de staging..."
rm -rf "$PAYLOAD"

echo "================================================================"
echo "Succès : installateur Windows créé avec succès !"
echo "Exécutable : $OUTPUT_EXE"
echo "================================================================"

#!/bin/bash
set -e

cd "$(dirname "$0")"
BASE=$(pwd)
TARGET="${TARGET:-$HOME/Desktop}"

## brew install pcre
if ! command -v pcregrep >/dev/null 2>&1; then
    echo "Erreur : 'pcregrep' est introuvable. Installez-le avec 'brew install pcre'." >&2
    exit 1
fi

VERSION=$(pcregrep -o1 'VERSION = "(.*)"' src/codelab/AbstractCodeLab.java)
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' src/codelab/AbstractCodeLab.java)

if [ -z "$VERSION" ] || [ -z "$BUILD" ]; then
    echo "Erreur : impossible d'extraire VERSION ou BUILD depuis src/codelab/AbstractCodeLab.java" >&2
    exit 1
fi

FILENAME="CodeLab-Win64"
DISTRIB="$TARGET/$FILENAME"
ZIPNAME="$FILENAME-$VERSION"

echo "================================================================"
echo "Génération de la distribution Windows 64-bit"
echo "CodeLab $VERSION (build $BUILD)"
echo "Dossier cible : $DISTRIB"
echo "Archive finale : $TARGET/$VERSION/$ZIPNAME.zip"
echo "================================================================"

# Vérification des prérequis essentiels
for required_path in "hidden" "codelab.files" "java/windows_64/JDK-17.0.8.1+1" "natives/windows_64" "bin/windows_64" "launchers/windows"; do
    if [ ! -e "$required_path" ]; then
        echo "Erreur critique : le composant '$required_path' est introuvable." >&2
        exit 1
    fi
done

echo "Préparation et nettoyage du dossier $FILENAME..."
rm -rf "$DISTRIB"
mkdir -p "$DISTRIB"
cp -r hidden "$DISTRIB/.hidden"

echo "Copie des fichiers de configuration..."
cp sys-properties/sysconfig-win.properties "$DISTRIB/.hidden/codelab/sysconfig.properties"
cp user-properties/user-win.properties "$DISTRIB/.hidden/codelab/user.properties.bak"

echo "Copie des librairies natives..."
mkdir -p "$DISTRIB/.hidden/codelab/natives"
cp -f natives/windows_64/* "$DISTRIB/.hidden/codelab/natives/"

echo "Copie des utilitaires..."
mkdir -p "$DISTRIB/.hidden/bin"
cp -f bin/windows_64/* "$DISTRIB/.hidden/bin/"

echo "Copie du dossier JDK-17.0.8.1+1..."
cp -r java/windows_64/JDK-17.0.8.1+1 "$DISTRIB/.hidden/"

echo "Copie des launchers et métadonnées..."
cp launchers/windows/codelab.exe "$DISTRIB/"
cp launchers/windows/codelab.bat "$DISTRIB/"
cp launchers/windows/kill-codelab.bat "$DISTRIB/"
cp launchers/windows/patch-codelab.bat "$DISTRIB/"
cp launchers/windows/install.bat "$DISTRIB/"
cp launchers/icon/codelab.ico "$DISTRIB/"
cp launchers/README.TXT "$DISTRIB/"

echo "Préparation du dossier codelab.files..."
cp -r codelab.files "$DISTRIB/.hidden/codelab/"
(
    cd "$DISTRIB/.hidden/codelab/"
    find codelab.files -name ".DS_Store" -delete 2>/dev/null || true
    find codelab.files -name "._*" -delete 2>/dev/null || true
    cp user.properties.bak codelab.files/userdata/user.properties
    mv codelab.files/hidden codelab.files/.hidden
    zip -qr files.zip codelab.files -x "*.DS_Store*" -x "*._*" -x "*__MACOSX*"
    rm -rf codelab.files
)

echo "Suppression des fichiers cachés et attributs macOS..."
chmod -R u+w "$DISTRIB"
find "$DISTRIB" -name ".DS_Store" -delete 2>/dev/null || true
find "$DISTRIB" -name "._*" -delete 2>/dev/null || true
xattr -cr "$DISTRIB" 2>/dev/null || true
export COPYFILE_DISABLE=1
dot_clean "$DISTRIB" 2>/dev/null || true

echo "Compression du dossier $FILENAME..."
(
    cd "$TARGET"
    zip -r -q "$ZIPNAME.zip" "$FILENAME" -x "*.DS_Store*" -x "*._*" -x "*__MACOSX*"
    
    echo "Calcul des sommes de contrôle..."
    md5 "$ZIPNAME.zip" > "$ZIPNAME.md5"
    shasum "$ZIPNAME.zip" > "$ZIPNAME.sha1"
    
    echo "Déplacement dans le dossier $VERSION..."
    mkdir -p "$VERSION"
    mv "$ZIPNAME.zip" "$VERSION/"
    mv "$ZIPNAME.md5" "$VERSION/"
    mv "$ZIPNAME.sha1" "$VERSION/"
    
    echo "Nettoyage du répertoire temporaire..."
    rm -rf "$FILENAME"
)

echo "================================================================"
echo "Succès : distribution Windows créée dans $TARGET/$VERSION/$ZIPNAME.zip"
echo "================================================================"

#!/bin/bash
set -e
cd "$(dirname "$0")"

BASE=../www/www/downloads

echo "----------------------------------------------------------------"
echo "Génération de versions.html à partir de versions.md"
echo "----------------------------------------------------------------"

python3 "$BASE/generate-versions-html.py"

echo "----------------------------------------------------------------"
echo "Upload des fichiers de version"
echo "----------------------------------------------------------------"

scp "$BASE/versions.md" jlehuen@transit.univ-lemans.fr:public_html/codelab/downloads/
scp "$BASE/versions.html" jlehuen@transit.univ-lemans.fr:public_html/codelab/downloads/
scp "$BASE/downloads-table.php" jlehuen@transit.univ-lemans.fr:public_html/codelab/downloads/

echo "----------------------------------------------------------------"
echo "Upload du fichier web.properties"
echo "----------------------------------------------------------------"

FILE=src/codelab/AbstractCodeLab.java

# 1. Récupération des numéros de version et de build
VERSION=$(pcregrep -o1 'VERSION = "(.*)"' $FILE)
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' $FILE)

if [ -z "$VERSION" ] || [ -z "$BUILD" ]; then
    echo "ERREUR: Impossible de récupérer la version ou le build depuis src/codelab/AbstractCodeLab.java"
    exit 1
fi

echo "VERSION = $VERSION"
echo "BUILD   = $BUILD"

# 2. Création du fichier web.properties local
echo "Création de web.properties..."
cat << EOF > web.properties
BUILD=$BUILD
VERSION=$VERSION
EOF

# 3. Copie à distance sur le serveur transit
echo "Copie sur transit.univ-lemans.fr..."
scp "web.properties" jlehuen@transit.univ-lemans.fr:public_html/codelab/data

# 4. Suppression du fichier local temporaire
echo "Nettoyage du fichier local..."
rm -f "web.properties"

## https://perso.univ-lemans.fr/~jlehuen/codelab/data/web.properties

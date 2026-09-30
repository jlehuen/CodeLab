#!/bin/bash

TARGET="${TARGET:-$HOME/Desktop}"

echo "----------------------------------------------------------------"
echo "Génération de la distribution Linux"
echo "----------------------------------------------------------------"

## brew install pcre
if [ -f "../VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < ../VERSION)
elif [ -f "./VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < ./VERSION)
else
    VERSION=$(pcregrep -o1 'VERSION = "(.*)"' src/codelab/AbstractCodeLab.java)
fi
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' src/codelab/AbstractCodeLab.java)

echo "VERSION = $VERSION"
echo "BUILD = $BUILD"

FILENAME=CodeLab-Linux
DISTRIB=$TARGET/$FILENAME
ZIPNAME=$FILENAME-$VERSION

echo "Préparation du dossier $FILENAME..."
mkdir $DISTRIB
cp -r hidden $DISTRIB/.hidden

echo "Copie des fichiers de configuration..."
cp sys-properties/sysconfig-linux.properties $DISTRIB/.hidden/codelab/sysconfig.properties
cp user-properties/user-linux.properties $DISTRIB/.hidden/codelab/user.properties.bak

echo "Copie des librarie natives..."
cp natives/linux_64/* $DISTRIB/.hidden/codelab/natives/

echo "Copie des utilitaires..."
cp bin/linux_64/* $DISTRIB/.hidden/bin/

echo "Copie du dossier JDK-17.0.8.1+1..."
cp -r java/linux_64/JDK-17.0.8.1+1 $DISTRIB/.hidden/

echo "Copie des launchers..."
cp launchers/unix/codelab $DISTRIB
cp launchers/icon/codelab.ico $DISTRIB
cp launchers/unix/first_time.sh $DISTRIB
cp launchers/unix/kill_codelab.sh $DISTRIB
cp launchers/README.TXT $DISTRIB

echo "Préparation du dossier codelab.files..."
cp -r codelab.files $DISTRIB/.hidden/codelab/
cd $DISTRIB/.hidden/codelab/
cp user.properties.bak codelab.files/userdata/user.properties
mv codelab.files/hidden codelab.files/.hidden
zip -qr files.zip codelab.files -x ".DS_Store" -x "._*"

rm -r codelab.files

echo "Attribution des permissions d'exécution..."
chmod +x $DISTRIB/codelab $DISTRIB/kill_codelab.sh $DISTRIB/first_time.sh
chmod +x $DISTRIB/.hidden/JDK-17.0.8.1+1/bin/*
chmod +x $DISTRIB/.hidden/bin/*

echo "Suppression des fichiers cachés..."
export COPYFILE_DISABLE=1 # Désactive la création des fichiers cachés
dot_clean $DISTRIB # Supprime les fichiers cachés existants

cd "$TARGET"

echo "Compression du dossier $FILENAME..."

zip -r -q $ZIPNAME.zip $FILENAME -x ".DS_Store" -x "._*"

echo "Copie dans le dossier $VERSION..."
mkdir -p $VERSION
mv $ZIPNAME.zip $VERSION

echo "Nettoyage du bureau..."
rm -rf $FILENAME

#!/bin/bash
cd "$(dirname "$0")"
HOME=$(pwd)

DESTINATION="codelab@codelab-serv.univ-lemans.fr:~/codelab-server"

echo "Nettoyage du dossier server..."
find server -type f -name '__MACOSX' -delete
find server -type f -name '.DS_Store' -delete
rm -r server/target

echo "Compression du dossier server..."
tar -czf server.tgz server

echo "Upload vers $DESTINATION..."
scp server.tgz $DESTINATION
scp scripts/*.sh $DESTINATION

echo "Nettoyage du bureau..."
rm server.tgz

echo "========================================"
echo "Tapez: install_server.sh"
echo "========================================"

echo "Ouverture d'une connexion ssh..."
ssh -t codelab@codelab-serv.univ-lemans.fr 'cd codelab-server ; bash --login'

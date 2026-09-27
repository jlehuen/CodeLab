#!/bin/bash
cd "$(dirname "$0")"
HOME=$(pwd)

DESTINATION="codelab@codelab-serv.univ-lemans.fr:~/codelab-server"

echo "Update vers $DESTINATION..."

scp server/src/*.rs $DESTINATION/server/src

#scp server/templates/*.html $DESTINATION/server/templates
#scp server/templates/*.css $DESTINATION/server/templates
#scp server/templates/*.js $DESTINATION/server/templates

#scp server/config/*.properties $DESTINATION/server/config

#scp server/Cargo.toml $DESTINATION/server
#scp scripts/*.sh $DESTINATION

echo "Ouverture d'une connexion ssh..."
ssh -t codelab@codelab-serv.univ-lemans.fr 'cd codelab-server ; bash --login'

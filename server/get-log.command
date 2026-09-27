#!/bin/bash
cd "$(dirname "$0")"

DISTANT_FILE="codelab@codelab-serv.univ-lemans.fr:~/codelab-server/server/logs/server.log"

scp $DISTANT_FILE temp
open temp/server.log

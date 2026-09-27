#!/bin/bash
cd $(dirname $0)

attendu="codelab.files"
current=$(basename "$PWD")
if [ "$current" != "$attendu" ]; then
    echo "Le nom du dossier courant n'est pas $attendu"
    exit 1
fi

cd .hidden/programs/

find . -type f -name '*.hi' -print -delete
find . -type f -name '*.bak' -print -delete
find . -type f -name '*.bak*' -print -delete
find . -type f -name '*.exe' -print -delete
find . -type f -name '*.pyc' -print -delete
find . -type f -name '*.orig' -print -delete
find . -type f -name '*.class' -print -delete
find . -type f -name '*.blocs.py' -print -delete
find . -type f -name '__temp.c' -print -delete
find . -type f -name '__MACOSX' -print -delete
find . -type f -name '.DS_Store' -print -delete
find . -type f -name '._*' -print -delete
find . -type d -name __pycache__ -print -delete
dot_clean -n .

cp -r local ~/Desktop/Programs_backup

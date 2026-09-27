#!/bin/bash
cd "$(dirname "$0")"

if xmllint --dtdvalid sessions.dtd --noout $1; then
    echo "Le fichier $1 est valide"
fi

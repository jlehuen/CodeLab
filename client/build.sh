#!/bin/bash
cd $(dirname $0)

# ----------------------------------------------------------------------------------------
# Lecture de la version depuis le fichier racine VERSION (source de vérité)
# ----------------------------------------------------------------------------------------

if [ -f "../VERSION" ]; then
    export VERSION=$(tr -d '[:space:]' < ../VERSION)
elif [ -f "./VERSION" ]; then
    export VERSION=$(tr -d '[:space:]' < ./VERSION)
fi

# ----------------------------------------------------------------------------------------
# Détection de JAVA_HOME (JDK embarqué ou JDK système 17)
# ----------------------------------------------------------------------------------------

if [ -d "./mac-app-arm/Contents/Java/jdk-17.0.20.1+1/Contents/Home" ]; then
    export JAVA_HOME=./mac-app-arm/Contents/Java/jdk-17.0.20.1+1/Contents/Home
elif [ -d "./mac-app-x64/Contents/Java/jdk-17.0.8.1+1/Contents/Home" ]; then
    export JAVA_HOME=./mac-app-x64/Contents/Java/jdk-17.0.8.1+1/Contents/Home
elif [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ]; then
    :
elif [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home 2>/dev/null)
fi

# ----------------------------------------------------------------------------------------
# Détection d'Apache Ant (binaire local ou commande système)
# ----------------------------------------------------------------------------------------

if [ -x "./java/apache-ant-1.10.15/bin/ant" ]; then
    ANT=./java/apache-ant-1.10.15/bin/ant
elif command -v ant >/dev/null 2>&1; then
    ANT=ant
else
    ANT=./java/apache-ant-1.10.15/bin/ant
fi

# ----------------------------------------------------------------------------------------
# Exécution de Ant
# ----------------------------------------------------------------------------------------

if [[ "$*" == *"--pro"* ]]
then
    $ANT codelab-pro
else
    $ANT codelab
fi

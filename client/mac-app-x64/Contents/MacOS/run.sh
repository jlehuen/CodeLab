#!/bin/sh
cd "$(dirname "$0")"

test -f ~/.bashrc && source ~/.bashrc
test -f ~/.bash_profile && source ~/.bash_profile

# To avoid the error ApplePersistenceIgnoreState
defaults write org.python.python ApplePersistenceIgnoreState NO > /dev/null 2>&1

export BASE=$(pwd)
export LAUNCHER=$0
export JAVA_HOME="$(dirname "$BASE")/Java/jdk-17.0.8.1+1/Contents/Home"

# Vérification : empêcher l'exécution directe depuis le volume DMG ou en AppTranslocation
case "$BASE" in
    /Volumes/*|*/AppTranslocation/*)
        osascript -e 'display alert "Installation de CodeLab requise" message "Veuillez glisser l’application CodeLab dans votre dossier Applications (ou sur votre Bureau) avant de la lancer." as critical buttons {"OK"} default button "OK"'
        exit 1
        ;;
esac

# Dossier de log externe au bundle (pour préserver l'intégrité de la signature de l'app)
LOG_DIR="$HOME/codelab.files/.hidden"
mkdir -p "$LOG_DIR" 2>/dev/null
RUN_LOG="$LOG_DIR/run.log"

{
    echo "$(date +%c)"
    echo "PATH=$PATH"
    echo "HOME=$HOME"
    echo "BASE=$BASE"
    echo "LAUNCHER=$LAUNCHER"
    echo "JAVA_HOME=$JAVA_HOME"
} > "$RUN_LOG" 2>/dev/null

# -----------------------------------------------------------------------------
# Gestion moderne et silencieuse du Gatekeeper (sans mot de passe ni sudo)
# -----------------------------------------------------------------------------
APP_BUNDLE="$(cd "$BASE/../.." 2>/dev/null && pwd)"

# Si le bundle ou ses composants portent l'attribut de quarantaine, on le retire récursivement
if xattr -p com.apple.quarantine "$APP_BUNDLE" >/dev/null 2>&1; then
    xattr -cr "$APP_BUNDLE" 2>/dev/null || true
fi

# Sécurité : s'assurer que les exécutables et dylibs internes sont dé-quarantainés
xattr -d com.apple.quarantine "$JAVA_HOME/bin/java" "$JAVA_HOME/bin/javac" 2>/dev/null || true
xattr -cr "$BASE/codelab/natives" 2>/dev/null || true
xattr -cr "$BASE/bin" 2>/dev/null || true

CODELAB="codelab/codelab.jar"
JAVA="$JAVA_HOME/bin/java"
DLIB="$BASE/codelab/natives"

# https://jogamp.org/bugzilla/show_bug.cgi?id=1317#c21

"$JAVA" -Xmx1024m \
    --add-exports java.base/java.lang=ALL-UNNAMED \
    --add-exports java.desktop/sun.awt=ALL-UNNAMED \
    --add-exports java.desktop/sun.java2d=ALL-UNNAMED \
    -Dfile.encoding=UTF-8 \
    -Djava.library.path="$DLIB" \
    -Xdock:name=CodeLab \
    -jar "$CODELAB" --log "$@" &

PID=$!
echo "PID=$PID" >> "$RUN_LOG" 2>/dev/null

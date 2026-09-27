#!/bin/bash
TARGET="${TARGET:-$HOME/Desktop/CodeLab.app}"

if [ ! -d "$TARGET" ]; then
    echo "Erreur : $TARGET est introuvable."
    exit 1
fi

echo "Actualisation forcée de l'icône de CodeLab..."
touch "$TARGET"
touch "$TARGET/Contents/Info.plist"
touch "$TARGET/Contents/Resources/AppIcon.icns"
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f -u "$TARGET" 2>/dev/null || true
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f "$TARGET" 2>/dev/null || true
qlmanage -r cache 2>/dev/null || true
killall Finder 2>/dev/null || true

echo "Icône réinitialisée et Finder relancé avec succès."

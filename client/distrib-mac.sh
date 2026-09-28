#!/bin/bash
set -e

TARGET="${TARGET:-$HOME/Desktop}"
BASE=$(pwd)

# =========================================================================
# Analyse des arguments : <-x64 | -arm> [-app]
# =========================================================================

ARCH=""
MODE_APP=false

for arg in "$@"; do
    case "$arg" in
        -arm|--arm)
            ARCH="arm"
            ;;
        -x64|--x64)
            ARCH="x64"
            ;;
        -app|--app)
            MODE_APP=true
            ;;
        *)
            echo "Option inconnue : $arg" >&2
            echo "Usage: $0 <-x64 | -arm> [-app]" >&2
            exit 1
            ;;
    esac
done

if [ -z "$ARCH" ]; then
    echo "Erreur : vous devez spécifier une architecture (-x64 ou -arm)." >&2
    echo "Usage: $0 <-x64 | -arm> [-app]" >&2
    echo "  -x64 : Distribution macOS Intel (mac-app-x64)" >&2
    echo "  -arm : Distribution macOS Apple Silicon (mac-app-arm)" >&2
    echo "  -app : Génération unique de CodeLab.app (sans DMG)" >&2
    exit 1
fi

## Extraction version et build
VERSION=$(pcregrep -o1 'VERSION = "(.*)"' src/codelab/AbstractCodeLab.java)
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' src/codelab/AbstractCodeLab.java)

echo "VERSION = $VERSION"
echo "BUILD = $BUILD"

if [ "$ARCH" = "arm" ]; then
    MAC_APP_DIR="mac-app-arm"
    NATIVES_DIR="osx_arm"
    NAME="CodeLab-MacOS-Silicon-$VERSION"
    DMGNAME="CodeLab-MacOS-Silicon-$VERSION.dmg"
    ARCH_LABEL="Apple Silicon (ARM64)"
else
    MAC_APP_DIR="mac-app-x64"
    NATIVES_DIR="osx_64"
    NAME="CodeLab-MacOS-Intel-$VERSION"
    DMGNAME="CodeLab-MacOS-Intel-$VERSION.dmg"
    ARCH_LABEL="Intel (x86_64)"
fi

echo "----------------------------------------------------------------"
echo "Génération de la distribution MacOS $ARCH_LABEL"
echo "Squelette : $MAC_APP_DIR"
echo "DMG cible : $DMGNAME"
echo "----------------------------------------------------------------"

# Vérification de l'existence du squelette
if [ ! -d "$MAC_APP_DIR" ]; then
    echo "Erreur : le répertoire squelette '$MAC_APP_DIR' n'existe pas." >&2
    exit 1
fi

#########################################################################
## 1. Assemblage de l'application CodeLab.app
#########################################################################

APP="$TARGET/CodeLab.app"
MACOS="$APP/Contents/MacOS"

rm -rf "$APP"
cp -r "$MAC_APP_DIR" "$APP"
cp -r hidden/codelab "$MACOS"

echo "Copie des fichiers de configuration..."
cp sys-properties/sysconfig-mac.properties "$MACOS/codelab/sysconfig.properties"
cp user-properties/user-mac.properties "$MACOS/codelab/user.properties.bak"

echo "Copie des librairies natives ($NATIVES_DIR)..."
for lib in natives/"$NATIVES_DIR"/*.jnilib natives/"$NATIVES_DIR"/*.dylib; do
    if [ -f "$lib" ]; then
        cp "$lib" "$MACOS/codelab/natives/"
    fi
done

echo "Préparation du dossier codelab.files..."
if [ ! -d "codelab.files" ]; then
    echo "Erreur critique : le dossier source 'codelab.files' est introuvable." >&2
    exit 1
fi
cp -r codelab.files "$MACOS/codelab/"
(
    cd "$MACOS/codelab/"
    cp user.properties.bak codelab.files/userdata/user.properties
    mv codelab.files/hidden codelab.files/.hidden
    zip -qr files.zip codelab.files -x ".DS_Store" -x "._*"
    rm -rf codelab.files
)

echo "Nettoyage des attributs étendus..."
chmod -R u+w "$APP"
xattr -cr "$APP"

echo "Signature ad-hoc de l'application..."
codesign --force --deep --sign - "$APP"

echo "Actualisation du cache LaunchServices et Finder..."
touch "$APP"
touch "$APP/Contents/Info.plist"
touch "$APP/Contents/Resources/AppIcon.icns"
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f -u "$APP" 2>/dev/null || true
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f "$APP" 2>/dev/null || true
qlmanage -r cache 2>/dev/null || true

if [ "$MODE_APP" = true ]; then
    echo "Mode --app demandé : génération du DMG ignorée."
    echo "Application prête : $APP"
    exit 0
fi

#########################################################################
## 2. Création de l'image disque DMG stylisée
#########################################################################

echo "Création de l'image disque DMG..."

DIR="$TARGET/disk"
TEMP="$TARGET/temp.dmg"
IMAGE="$BASE/icons/background.png"
TITLE="CodeLab IDE and Simulators"

rm -rf "$DIR" "$TEMP"
mkdir -p "$DIR/.background"

cp -R "$APP" "$DIR/"
cp "$IMAGE" "$DIR/.background/"

# Icône de volume personnalisée persistante (.VolumeIcon.icns dans le filesystem HFS+)
if [ -f "$BASE/icons/dmg-icone.icns" ]; then
    cp "$BASE/icons/dmg-icone.icns" "$DIR/.VolumeIcon.icns"
    SetFile -c icnC "$DIR/.VolumeIcon.icns" 2>/dev/null || true
    SetFile -a V "$DIR/.VolumeIcon.icns" 2>/dev/null || true
    SetFile -a C "$DIR" 2>/dev/null || true
fi

# Taille de 500m pour avoir l'espace de manipuler Finder
hdiutil create "$TEMP" -srcfolder "$DIR" -volname "${TITLE}" -fs HFS+ -fsargs "-c c=64,a=16,e=16" -format UDRW -size 500m -quiet
MOUNT_OUTPUT=$(hdiutil attach -readwrite -noverify -noautoopen "$TEMP")
DEVICE=$(echo "$MOUNT_OUTPUT" | grep -E '^/dev/' | head -n 1 | awk '{print $1}')
VOLUME=$(echo "$MOUNT_OUTPUT" | grep -E '/Volumes/' | sed -E 's/.*(\/Volumes\/.*)/\1/')

WIDTH=$(identify -format '%w' "$IMAGE" 2>/dev/null || echo 600)
HEIGHT=$(identify -format '%h' "$IMAGE" 2>/dev/null || echo 400)

osascript << EOF || true
tell application "Finder"
    tell disk "${TITLE}"
        open
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        set the bounds of container window to {500, 200, 500 + $WIDTH, 200 + $HEIGHT + 25}
        set theViewOptions to the icon view options of container window
        set arrangement of theViewOptions to not arranged
        set icon size of theViewOptions to 72
        set background picture of theViewOptions to file ".background:background.png"
        if not (exists item "Applications" of container window) then
            make new alias file at container window to POSIX file "/Applications" with properties {name:"Applications"}
        end if
        set position of item "CodeLab.app" of container window to {180, 220}
        set position of item "Applications" of container window to {500, 220}
        update without registering applications
        delay 1
        close
    end tell
end tell
EOF

# Configuration finale de l'icône de volume APRÈS AppleScript
if [ -n "$VOLUME" ] && [ -d "$VOLUME" ]; then
    if [ -f "$BASE/icons/dmg-icone.icns" ]; then
        cp "$BASE/icons/dmg-icone.icns" "$VOLUME/.VolumeIcon.icns"
    fi
    SetFile -c icnC "$VOLUME/.VolumeIcon.icns" 2>/dev/null || true
    SetFile -a V "$VOLUME/.VolumeIcon.icns" 2>/dev/null || true
    SetFile -a C "$VOLUME" 2>/dev/null || true
    chmod -Rf go-w "$VOLUME" 2>/dev/null || true
fi
sync
sync

# Détachement propre
hdiutil detach "$DEVICE" -force -quiet 2>/dev/null || ( [ -n "$VOLUME" ] && hdiutil detach "$VOLUME" -force -quiet 2>/dev/null ) || true

# Conversion en DMG compressé UDZO (zlib niveau 9)
rm -f "$BASE/CodeLab.dmg"
hdiutil convert "$TEMP" -format UDZO -imagekey zlib-level=9 -o "$BASE/CodeLab.dmg" -quiet

#########################################################################
## 3. Personnalisation de l'icône du fichier DMG (local)
#########################################################################

if [ -f "icons/dmg-icone.icns" ]; then
    sips -i icons/dmg-icone.icns >/dev/null 2>&1
    DeRez -only icns icons/dmg-icone.icns > icons/tmpicns.rsrc 2>/dev/null || true
    Rez -append icons/tmpicns.rsrc -o "$BASE/CodeLab.dmg" 2>/dev/null || true
    SetFile -a C "$BASE/CodeLab.dmg" 2>/dev/null || true
    rm -f icons/tmpicns.rsrc
elif [ -f "icons/dmg-icone.png" ]; then
    sips -i icons/dmg-icone.png >/dev/null 2>&1
    DeRez -only icns icons/dmg-icone.png > icons/tmpicns.rsrc 2>/dev/null || true
    Rez -append icons/tmpicns.rsrc -o "$BASE/CodeLab.dmg" 2>/dev/null || true
    SetFile -a C "$BASE/CodeLab.dmg" 2>/dev/null || true
    rm -f icons/tmpicns.rsrc
fi

rm -f "$TEMP"
rm -rf "$DIR"

mv "$BASE/CodeLab.dmg" "$TARGET/$DMGNAME"
cd "$TARGET"

#########################################################################
## 4. Sommes de contrôle & Export
#########################################################################

echo "Calcul des sommes de contrôle pour $DMGNAME..."
shasum "$DMGNAME" > "$NAME.sha1"

echo "Copie dans le dossier $VERSION..."
mkdir -p "$VERSION"
mv "$DMGNAME" "$VERSION/"
mv "$NAME.sha1" "$VERSION/"

echo "Distribution DMG créée avec succès : $TARGET/$VERSION/$DMGNAME"

#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
BASE="$(dirname "$DIR")"

# 1. AppIcon.icns (pour CodeLab.app)
SRC_APP="$DIR/icone.png"
DEST_APP="$BASE/mac-app/Contents/Resources/AppIcon.icns"

if [ -f "$SRC_APP" ]; then
    echo "Génération de AppIcon.icns depuis $SRC_APP..."
    ICONSET=$(mktemp -d)/AppIcon.iconset
    mkdir -p "$ICONSET"
    sips -z 16 16     "$SRC_APP" --out "$ICONSET/icon_16x16.png" >/dev/null
    sips -z 32 32     "$SRC_APP" --out "$ICONSET/icon_16x16@2x.png" >/dev/null
    sips -z 32 32     "$SRC_APP" --out "$ICONSET/icon_32x32.png" >/dev/null
    sips -z 64 64     "$SRC_APP" --out "$ICONSET/icon_32x32@2x.png" >/dev/null
    sips -z 128 128   "$SRC_APP" --out "$ICONSET/icon_128x128.png" >/dev/null
    sips -z 256 256   "$SRC_APP" --out "$ICONSET/icon_128x128@2x.png" >/dev/null
    sips -z 256 256   "$SRC_APP" --out "$ICONSET/icon_256x256.png" >/dev/null
    sips -z 512 512   "$SRC_APP" --out "$ICONSET/icon_256x256@2x.png" >/dev/null
    sips -z 512 512   "$SRC_APP" --out "$ICONSET/icon_512x512.png" >/dev/null
    sips -z 1024 1024 "$SRC_APP" --out "$ICONSET/icon_512x512@2x.png" >/dev/null
    mkdir -p "$(dirname "$DEST_APP")"
    iconutil -c icns "$ICONSET" -o "$DEST_APP"
    rm -rf "$(dirname "$ICONSET")"
    echo "-> $DEST_APP généré."
fi

# 2. dmg-icone.icns (pour le volume du DMG)
SRC_DMG="$DIR/dmg-icone.png"
DEST_DMG="$DIR/dmg-icone.icns"

if [ -f "$SRC_DMG" ]; then
    echo "Génération de dmg-icone.icns depuis $SRC_DMG..."
    ICONSET=$(mktemp -d)/DmgIcon.iconset
    mkdir -p "$ICONSET"
    sips -z 16 16     "$SRC_DMG" --out "$ICONSET/icon_16x16.png" >/dev/null
    sips -z 32 32     "$SRC_DMG" --out "$ICONSET/icon_16x16@2x.png" >/dev/null
    sips -z 32 32     "$SRC_DMG" --out "$ICONSET/icon_32x32.png" >/dev/null
    sips -z 64 64     "$SRC_DMG" --out "$ICONSET/icon_32x32@2x.png" >/dev/null
    sips -z 128 128   "$SRC_DMG" --out "$ICONSET/icon_128x128.png" >/dev/null
    sips -z 256 256   "$SRC_DMG" --out "$ICONSET/icon_128x128@2x.png" >/dev/null
    sips -z 256 256   "$SRC_DMG" --out "$ICONSET/icon_256x256.png" >/dev/null
    sips -z 512 512   "$SRC_DMG" --out "$ICONSET/icon_256x256@2x.png" >/dev/null
    sips -z 512 512   "$SRC_DMG" --out "$ICONSET/icon_512x512.png" >/dev/null
    sips -z 1024 1024 "$SRC_DMG" --out "$ICONSET/icon_512x512@2x.png" >/dev/null
    iconutil -c icns "$ICONSET" -o "$DEST_DMG"
    rm -rf "$(dirname "$ICONSET")"
    echo "-> $DEST_DMG généré."
fi

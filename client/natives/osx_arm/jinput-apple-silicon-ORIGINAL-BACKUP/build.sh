#!/usr/bin/env bash
# ==============================================================================
# Build Script for JInput on macOS Apple Silicon (ARM64 & Universal)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "================================================================"
echo " Compilation de JInput pour Apple Silicon (ARM64 & Universal)"
echo "================================================================"

# 1. Détection du JDK
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/javac" ]; then
    CANDIDATES=(
        "/Users/lehuen/dev/codelab/client/java/macos_ARM/JDK-17.0.20.1+1/Contents/Home"
        "$(/usr/libexec/java_home 2>/dev/null || true)"
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    )
    for cand in "${CANDIDATES[@]}"; do
        if [ -n "$cand" ] && [ -x "$cand/bin/javac" ]; then
            export JAVA_HOME="$cand"
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/javac" ]; then
    echo "ERREUR: Impossible de localiser un JDK valide avec javac." >&2
    exit 1
fi

echo "-> JDK détecté : $JAVA_HOME"
"$JAVA_HOME/bin/javac" -version

# 2. Préparation des répertoires de travail
BUILD_DIR="$SCRIPT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
HEADERS_DIR="$BUILD_DIR/headers"
BIN_ARM64="$SCRIPT_DIR/bin/arm64"
BIN_UNIVERSAL="$SCRIPT_DIR/bin/universal"
JAR_DIR="$SCRIPT_DIR/jar"
LIB_DIR="$SCRIPT_DIR/lib"

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$HEADERS_DIR" "$BIN_ARM64" "$BIN_UNIVERSAL" "$JAR_DIR"

# 3. Compilation Java & Génération des en-têtes JNI
echo ""
echo "-> Compilation des classes Java (cible Java 8) et génération des en-têtes JNI..."
JAVA_SOURCES=$(find "$SCRIPT_DIR/src/coreAPI/src/main/java" "$SCRIPT_DIR/src/plugins/OSX/src/main/java" -name "*.java")

"$JAVA_HOME/bin/javac" \
    --release 8 \
    -cp "$LIB_DIR/jutils-1.0.0.jar" \
    -h "$HEADERS_DIR" \
    -d "$CLASSES_DIR" \
    $JAVA_SOURCES

echo "   En-têtes JNI générés dans $HEADERS_DIR :"
ls -1 "$HEADERS_DIR"

# 4. Création du JAR JInput (OSX + CoreAPI)
echo ""
echo "-> Création de l'archive JAR (jinput-osx-2.0.11.jar)..."
JAR_FILE="$JAR_DIR/jinput-osx-2.0.11.jar"
"$JAVA_HOME/bin/jar" --create \
    --file "$JAR_FILE" \
    -C "$CLASSES_DIR" .

echo "   Archive JAR créée : $JAR_FILE ($(wc -c < "$JAR_FILE" | tr -d ' ') octets)"

# 5. Compilation C Native pour ARM64 (Apple Silicon)
echo ""
echo "-> Compilation native ARM64 (Apple Silicon)..."
C_SOURCES=(
    "$SCRIPT_DIR/src/plugins/common/src/native/util.c"
    "$SCRIPT_DIR/src/plugins/OSX/src/main/native/macosxutil.c"
    "$SCRIPT_DIR/src/plugins/OSX/src/main/native/net_java_games_input_OSXHIDDevice.c"
    "$SCRIPT_DIR/src/plugins/OSX/src/main/native/net_java_games_input_OSXHIDDeviceIterator.c"
    "$SCRIPT_DIR/src/plugins/OSX/src/main/native/net_java_games_input_OSXHIDQueue.c"
)

C_INCLUDES=(
    "-I$HEADERS_DIR"
    "-I$SCRIPT_DIR/src/plugins/OSX/src/main/native"
    "-I$SCRIPT_DIR/src/plugins/common/src/native"
    "-I$JAVA_HOME/include"
    "-I$JAVA_HOME/include/darwin"
)

FRAMEWORKS=(
    "-framework" "CoreFoundation"
    "-framework" "IOKit"
    "-framework" "CoreServices"
)

mkdir -p "$BUILD_DIR/obj_arm64"
for src in "${C_SOURCES[@]}"; do
    obj="$BUILD_DIR/obj_arm64/$(basename "$src" .c).o"
    clang -O3 -Wall -fPIC -mmacosx-version-min=11.0 -arch arm64 \
        "${C_INCLUDES[@]}" -c "$src" -o "$obj"
done

clang -dynamiclib -arch arm64 -mmacosx-version-min=11.0 \
    -o "$BIN_ARM64/libjinput-osx.dylib" \
    "$BUILD_DIR/obj_arm64/"*.o \
    "${FRAMEWORKS[@]}"

strip -S -X "$BIN_ARM64/libjinput-osx.dylib"
cp "$BIN_ARM64/libjinput-osx.dylib" "$BIN_ARM64/libjinput-osx.jnilib"

# 6. Compilation C Native pour x86_64 (Intel)
echo ""
echo "-> Compilation native x86_64 (Intel)..."
mkdir -p "$BUILD_DIR/obj_x86_64"
for src in "${C_SOURCES[@]}"; do
    obj="$BUILD_DIR/obj_x86_64/$(basename "$src" .c).o"
    clang -O3 -Wall -fPIC -mmacosx-version-min=10.9 -arch x86_64 \
        "${C_INCLUDES[@]}" -c "$src" -o "$obj"
done

clang -dynamiclib -arch x86_64 -mmacosx-version-min=10.9 \
    -o "$BUILD_DIR/libjinput-osx-x86_64.dylib" \
    "$BUILD_DIR/obj_x86_64/"*.o \
    "${FRAMEWORKS[@]}"

strip -S -X "$BUILD_DIR/libjinput-osx-x86_64.dylib"

# 7. Création de la bibliothèque Universelle (FAT Binary arm64 + x86_64)
echo ""
echo "-> Assemblage du binaire Universel (arm64 + x86_64 via lipo)..."
lipo -create -output "$BIN_UNIVERSAL/libjinput-osx.dylib" \
    "$BIN_ARM64/libjinput-osx.dylib" \
    "$BUILD_DIR/libjinput-osx-x86_64.dylib"

cp "$BIN_UNIVERSAL/libjinput-osx.dylib" "$BIN_UNIVERSAL/libjinput-osx.jnilib"

# 8. Signature ad-hoc (obligatoire pour macOS Apple Silicon)
echo ""
echo "-> Signature ad-hoc des bibliothèques dynamiques..."
codesign --force --sign - "$BIN_ARM64/libjinput-osx.dylib"
codesign --force --sign - "$BIN_ARM64/libjinput-osx.jnilib"
codesign --force --sign - "$BIN_UNIVERSAL/libjinput-osx.dylib"
codesign --force --sign - "$BIN_UNIVERSAL/libjinput-osx.jnilib"

# Déploiement direct dans le dossier parent (osx_arm)
cp "$BIN_ARM64/libjinput-osx.jnilib" "$SCRIPT_DIR/../libjinput-osx.jnilib"
cp "$BIN_ARM64/libjinput-osx.dylib"  "$SCRIPT_DIR/../libjinput-osx.dylib"

# 9. Empreintes SHA-256
echo ""
echo "-> Calcul des sommes de contrôle SHA-256..."
(
    cd "$SCRIPT_DIR"
    shasum -a 256 \
        bin/arm64/libjinput-osx.dylib \
        bin/arm64/libjinput-osx.jnilib \
        bin/universal/libjinput-osx.dylib \
        bin/universal/libjinput-osx.jnilib \
        jar/jinput-osx-2.0.11.jar > "$SCRIPT_DIR/CHECKSUMS.sha256"
)

cat "$SCRIPT_DIR/CHECKSUMS.sha256"

# 10. Rapport d'inspection
echo ""
echo "================================================================"
echo " Rapport des binaires générés"
echo "================================================================"
echo "[ARM64 Native]"
file "$BIN_ARM64/libjinput-osx.dylib"
lipo -info "$BIN_ARM64/libjinput-osx.dylib"

echo ""
echo "[Universel (ARM64 + Intel)]"
file "$BIN_UNIVERSAL/libjinput-osx.dylib"
lipo -info "$BIN_UNIVERSAL/libjinput-osx.dylib"

echo ""
echo "Compilation terminée avec succès !"

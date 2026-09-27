#!/usr/bin/env bash
# ==============================================================================
# Test Script for JInput on Apple Silicon JVM
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
    CANDIDATES=(
        "/Users/lehuen/dev/codelab/client/java/macos_ARM/JDK-17.0.20.1+1/Contents/Home"
        "$(/usr/libexec/java_home 2>/dev/null || true)"
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    )
    for cand in "${CANDIDATES[@]}"; do
        if [ -n "$cand" ] && [ -x "$cand/bin/java" ]; then
            export JAVA_HOME="$cand"
            break
        fi
    done
fi

TARGET_BIN="${1:-$SCRIPT_DIR/bin/arm64}"
echo "-> Test avec les binaires JNI situés dans : $TARGET_BIN"

# Compilation de la classe de test
"$JAVA_HOME/bin/javac" \
    --release 8 \
    -cp "$SCRIPT_DIR/jar/jinput-osx-2.0.11.jar:$SCRIPT_DIR/lib/jutils-1.0.0.jar" \
    -d "$SCRIPT_DIR/build" \
    "$SCRIPT_DIR/TestJInput.java"

# Exécution sous JVM ARM64
"$JAVA_HOME/bin/java" \
    -cp "$SCRIPT_DIR/build:$SCRIPT_DIR/jar/jinput-osx-2.0.11.jar:$SCRIPT_DIR/lib/jutils-1.0.0.jar" \
    -Djava.library.path="$TARGET_BIN" \
    TestJInput

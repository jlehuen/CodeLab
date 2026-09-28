#!/bin/bash
set -e

# ==============================================================================
# Script de Release et Publication CodeLab
# ==============================================================================
# 1. Compilation du client (./build.sh)
# 2. Génération de toutes les distributions (./distrib-all.sh)
# 3. Commit et Push des modifications de code dans Git
# 4. Création et Push de la nouvelle version (Git tag + GitHub Release si gh dispo)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Vérification du dépôt Git
if [ ! -d "$GIT_ROOT/.git" ]; then
    echo "Erreur : Dépôt Git introuvable à la racine $GIT_ROOT" >&2
    exit 1
fi

# Source de vérité : fichier racine VERSION (ou argument optionnel ex: ./release.sh 1.4.3)
NEW_VERSION="$1"
if [ -n "$NEW_VERSION" ]; then
    echo "Mise à jour du fichier VERSION : $NEW_VERSION"
    echo "$NEW_VERSION" > "$GIT_ROOT/VERSION"
fi

if [ -f "$GIT_ROOT/VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < "$GIT_ROOT/VERSION")
    export VERSION
    echo "Version cible (depuis VERSION) : $VERSION"
fi

echo "================================================================"
echo "==> [1/4] Exécution de ./build.sh..."
echo "================================================================"
cd "$SCRIPT_DIR"
./build.sh

# Extraction dynamique des numéros de version et de build effectifs
VERSION=$(pcregrep -o1 'VERSION = "(.*)"' "$SCRIPT_DIR/src/codelab/AbstractCodeLab.java")
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' "$SCRIPT_DIR/src/codelab/AbstractCodeLab.java")

if [ -z "$VERSION" ] || [ -z "$BUILD" ]; then
    echo "Erreur : Impossible d'extraire VERSION ou BUILD depuis AbstractCodeLab.java" >&2
    exit 1
fi

echo "Version validée : $VERSION (build $BUILD)"

# Mise à jour synchronisée des badges et liens dans README.md
if [ -f "$GIT_ROOT/README.md" ]; then
    echo "Mise à jour de README.md avec la version $VERSION..."
    sed -i '' -E "s/Version-[0-9]+\.[0-9]+\.[0-9]+/Version-$VERSION/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/Téléchargements \(Version [0-9]+\.[0-9]+\.[0-9]+\)/Téléchargements (Version $VERSION)/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-MacOS-Silicon-[0-9]+\.[0-9]+\.[0-9]+\.dmg/CodeLab-MacOS-Silicon-$VERSION.dmg/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-MacOS-Intel-[0-9]+\.[0-9]+\.[0-9]+\.dmg/CodeLab-MacOS-Intel-$VERSION.dmg/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-Win64-[0-9]+\.[0-9]+\.[0-9]+-Setup\.exe/CodeLab-Win64-$VERSION-Setup.exe/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-Linux-[0-9]+\.[0-9]+\.[0-9]+\.zip/CodeLab-Linux-$VERSION.zip/g" "$GIT_ROOT/README.md"
fi

echo "================================================================"
echo "==> [2/4] Exécution de ./distrib-all.sh..."
echo "================================================================"
cd "$SCRIPT_DIR"
./distrib-all.sh

echo "================================================================"
echo "==> [3/4] Push des modifications de code dans Git..."
echo "================================================================"
cd "$GIT_ROOT"

# Message de commit
COMMIT_MSG="${2:-release: CodeLab v$VERSION (build $BUILD)}"

# Indexation des fichiers de code et de documentation modifiés
git add -u
git add README.md 2>/dev/null || true
git add VERSION release.sh client/release.sh 2>/dev/null || true

# Commit si nécessaire
if ! git diff --cached --quiet; then
    echo "Création du commit : $COMMIT_MSG"
    git commit -m "$COMMIT_MSG"
else
    echo "Aucune nouvelle modification de code à committer."
fi

# Push de la branche active
CURRENT_BRANCH=$(git branch --show-current)
echo "Push de la branche '$CURRENT_BRANCH' vers origin..."
git push origin "$CURRENT_BRANCH"

echo "================================================================"
echo "==> [4/4] Push de la nouvelle version dans Git..."
echo "================================================================"
TAG_NAME="v$VERSION"

# Création ou écrasement du tag annoté local
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Le tag $TAG_NAME existe déjà localement, mise à jour..."
    git tag -d "$TAG_NAME"
fi

echo "Création du tag $TAG_NAME (build $BUILD)..."
git tag -a "$TAG_NAME" -m "CodeLab release $VERSION (build $BUILD)"

echo "Push du tag $TAG_NAME vers origin..."
git push origin "$TAG_NAME" --force

# Gestion des paquets GitHub Release
DISTRIB_DIR="$HOME/Desktop/$VERSION"
echo ""
echo "Vérification de GitHub CLI (gh)..."
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
    echo "Publication automatique de la GitHub Release $TAG_NAME avec les installeurs..."
    if gh release view "$TAG_NAME" >/dev/null 2>&1; then
        echo "La release $TAG_NAME existe déjà sur GitHub. Téléversement des fichiers..."
        gh release upload "$TAG_NAME" "$DISTRIB_DIR"/* --clobber
    else
        gh release create "$TAG_NAME" "$DISTRIB_DIR"/* \
            --title "CodeLab $VERSION" \
            --notes "Release officielle de CodeLab $VERSION (build $BUILD)."
    fi
    echo "Release GitHub publiée avec succès !"
else
    echo "----------------------------------------------------------------"
    echo "Information : 'gh' n'est pas installé ou connecté."
    echo "Le tag Git $TAG_NAME a bien été poussé sur GitHub."
    echo "Les paquets binaires prêts à être téléversés sont dans :"
    echo "  $DISTRIB_DIR"
    echo "Pour créer la release et joindre les installeurs via le navigateur :"
    echo "  https://github.com/jlehuen/CodeLab/releases/new?tag=$TAG_NAME"
    echo "Ou en ligne de commande après installation de gh :"
    echo "  brew install gh && gh auth login"
    echo "  gh release create $TAG_NAME $DISTRIB_DIR/* --title \"CodeLab $VERSION\" --notes \"Release $VERSION\""
    echo "----------------------------------------------------------------"
fi

echo ""
echo "================================================================"
echo "Release CodeLab $VERSION (build $BUILD) terminée avec succès !"
echo "================================================================"

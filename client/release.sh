#!/bin/bash
set -e

# ==============================================================================
# Script de Release et Publication CodeLab
# ==============================================================================
# 1. Compilation du client (./build.sh)
# 2. Génération de toutes les distributions autonomes (./distrib-all.sh)
# 3. Commit et Push des modifications de code et de documentation dans Git
# 4. Création du tag Git et Publication de la Release GitHub officielle (Latest)
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Vérification du dépôt Git
if [ ! -d "$GIT_ROOT/.git" ]; then
    echo "Erreur : Dépôt Git introuvable à la racine $GIT_ROOT" >&2
    exit 1
fi

# Vérification de l'outil GitHub CLI (gh) pour la publication des Releases
if ! command -v gh >/dev/null 2>&1; then
    echo "================================================================" >&2
    echo "Erreur : GitHub CLI ('gh') est requis pour publier les releases." >&2
    echo "Installez-le avec : brew install gh" >&2
    echo "Puis connectez-vous avec : gh auth login" >&2
    echo "================================================================" >&2
    exit 1
fi

# Vérification de la connexion à GitHub CLI
if ! gh auth status >/dev/null 2>&1; then
    echo "================================================================" >&2
    echo "Attention : GitHub CLI n'est pas encore connecté à votre compte." >&2
    echo "Connexion requise pour téléverser les releases..." >&2
    echo "================================================================" >&2
    gh auth login
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
else
    echo "Erreur : Fichier VERSION introuvable à la racine $GIT_ROOT" >&2
    exit 1
fi

# ==============================================================================
# 1. Compilation du client (./build.sh)
# ==============================================================================
echo ""
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

# ==============================================================================
# 2. Génération de toutes les distributions (./distrib-all.sh)
# ==============================================================================
echo ""
echo "================================================================"
echo "==> [2/4] Exécution de ./distrib-all.sh..."
echo "================================================================"
cd "$SCRIPT_DIR"
./distrib-all.sh

DISTRIB_DIR="$HOME/Desktop/$VERSION"
if [ ! -d "$DISTRIB_DIR" ]; then
    echo "Erreur : Dossier de distribution introuvable : $DISTRIB_DIR" >&2
    exit 1
fi

# ==============================================================================
# 3. Commit et Push des modifications de code dans Git
# ==============================================================================
echo ""
echo "================================================================"
echo "==> [3/4] Push des modifications de code dans Git..."
echo "================================================================"
cd "$GIT_ROOT"

COMMIT_MSG="${2:-release: CodeLab v$VERSION (build $BUILD)}"

# Indexation des fichiers suivis et des nouveaux fichiers de gestion de version
git add -u
git add README.md 2>/dev/null || true
git add VERSION release.sh client/release.sh 2>/dev/null || true

# Commit si des modifications sont présentes
if ! git diff --cached --quiet; then
    echo "Création du commit : $COMMIT_MSG"
    git commit -m "$COMMIT_MSG"
else
    echo "Aucune modification de code supplémentaire à committer."
fi

# Push de la branche active
CURRENT_BRANCH=$(git branch --show-current)
echo "Push de la branche '$CURRENT_BRANCH' vers origin..."
git push origin "$CURRENT_BRANCH"

# ==============================================================================
# 4. Création du tag Git et Publication de la Release GitHub (Latest)
# ==============================================================================
echo ""
echo "================================================================"
echo "==> [4/4] Push de la version et Publication de la Release (Latest)..."
echo "================================================================"
TAG_NAME="v$VERSION"

# Création ou écrasement du tag annoté local
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "Le tag $TAG_NAME existe déjà localement, mise à jour..."
    git tag -d "$TAG_NAME"
fi

echo "Création du tag Git $TAG_NAME..."
git tag -a "$TAG_NAME" -m "CodeLab release $VERSION (build $BUILD)"

echo "Push du tag $TAG_NAME vers GitHub origin..."
git push origin "$TAG_NAME" --force

# Extraction des notes de version depuis versions.md
NOTES=$(python3 -c "
import sys
try:
    content = open('$GIT_ROOT/www/www/downloads/versions.md', encoding='utf-8').read()
    ver = '$VERSION'
    idx = content.find('### Version ' + ver)
    if idx != -1:
        end = content.find('\n--', idx)
        if end == -1: end = content.find('\n### Version', idx + 1)
        notes = content[idx:end].strip() if end != -1 else content[idx:].strip()
        print(notes)
    else:
        print('Release officielle de CodeLab $VERSION (build $BUILD).')
except Exception:
    print('Release officielle de CodeLab $VERSION (build $BUILD).')
")

# Collecte des fichiers à téléverser
FILES_TO_UPLOAD=()
for file in "$DISTRIB_DIR"/CodeLab-*; do
    if [ -f "$file" ]; then
        FILES_TO_UPLOAD+=("$file")
    fi
done

if [ ${#FILES_TO_UPLOAD[@]} -eq 0 ]; then
    echo "Erreur : Aucun fichier d'installation trouvé dans $DISTRIB_DIR" >&2
    exit 1
fi

echo "Fichiers prêts pour la Release :"
for f in "${FILES_TO_UPLOAD[@]}"; do
    echo "  - $(basename "$f")"
done

echo ""
echo "Téléversement des paquets sur GitHub Releases (tag $TAG_NAME, Latest)..."

if gh release view "$TAG_NAME" >/dev/null 2>&1; then
    echo "Mise à jour de la release existante $TAG_NAME sur GitHub..."
    gh release edit "$TAG_NAME" --title "CodeLab $VERSION" --notes "$NOTES" --latest
    gh release upload "$TAG_NAME" "${FILES_TO_UPLOAD[@]}" --clobber
else
    echo "Création de la nouvelle release $TAG_NAME sur GitHub..."
    gh release create "$TAG_NAME" "${FILES_TO_UPLOAD[@]}" \
        --title "CodeLab $VERSION" \
        --notes "$NOTES" \
        --latest
fi

echo ""
echo "================================================================"
echo "🎉 Succès total : CodeLab $VERSION (build $BUILD) est en ligne !"
echo "Page de la release : https://github.com/jlehuen/CodeLab/releases/tag/$TAG_NAME"
echo "Lien permanent     : https://github.com/jlehuen/CodeLab/releases/latest"
echo "================================================================"

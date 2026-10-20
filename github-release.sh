#!/bin/bash
set -e

# ==============================================================================
# Script de Release et Publication CodeLab
# ==============================================================================
# 1. Compilation du client (client/build.sh)
# 2. Génération de toutes les distributions autonomes (client/distrib-all.sh)
# 3. Commit et Push des modifications de code et de documentation dans Git
# 4. Création du tag Git et Publication de la Release GitHub officielle (Latest)
# ==============================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_DIR="$ROOT_DIR/client"
GIT_ROOT="$ROOT_DIR"

# Analyse des options et arguments
USE_UNIV_PROXY=false
ARGS=()

for arg in "$@"; do
    case "$arg" in
        --univ|-univ)
            USE_UNIV_PROXY=true
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
done

# Activation du proxy Université du Mans si demandé
if [ "$USE_UNIV_PROXY" = true ]; then
    export HTTP_PROXY="http://proxy.univ-lemans.fr:3128"
    export HTTPS_PROXY="http://proxy.univ-lemans.fr:3128"
    export http_proxy="http://proxy.univ-lemans.fr:3128"
    export https_proxy="http://proxy.univ-lemans.fr:3128"
    echo "🏛️  Proxy Université du Mans activé : http://proxy.univ-lemans.fr:3128"
fi

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

# Source de vérité : fichier racine VERSION (ou argument optionnel ex: ./git-release.sh 1.4.3)
NEW_VERSION="${ARGS[0]}"
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
echo "==> [1/4] Compilation du client (client/build.sh)..."
echo "================================================================"
cd "$CLIENT_DIR"
./build.sh

# Extraction dynamique du numéro de build effectif
BUILD=$(pcregrep -o1 'BUILD = "([0-9]*)"' "$CLIENT_DIR/src/codelab/AbstractCodeLab.java")

# Relecture systématique du fichier VERSION comme source de vérité
if [ -f "$GIT_ROOT/VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < "$GIT_ROOT/VERSION")
fi
export VERSION

if [ -z "$VERSION" ] || [ -z "$BUILD" ]; then
    echo "Erreur : Impossible d'extraire VERSION ou BUILD" >&2
    exit 1
fi

echo "Version validée (depuis VERSION) : $VERSION (build $BUILD)"

# Mise à jour synchronisée des badges et liens dans README.md
if [ -f "$GIT_ROOT/README.md" ]; then
    echo "Mise à jour de README.md avec la version $VERSION..."
    sed -i '' -E "s/Version-[0-9]+\.[0-9]+\.[0-9]+/Version-$VERSION/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/Téléchargements \([Vv]ersion [^)]+\)/Téléchargements (version $VERSION)/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-MacOS-Silicon-[0-9]+\.[0-9]+\.[0-9]+\.dmg/CodeLab-MacOS-Silicon-$VERSION.dmg/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-MacOS-Intel-[0-9]+\.[0-9]+\.[0-9]+\.dmg/CodeLab-MacOS-Intel-$VERSION.dmg/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-Win64-[0-9]+\.[0-9]+\.[0-9]+-Setup\.exe/CodeLab-Win64-$VERSION-Setup.exe/g" "$GIT_ROOT/README.md"
    sed -i '' -E "s/CodeLab-Linux-[0-9]+\.[0-9]+\.[0-9]+\.zip/CodeLab-Linux-$VERSION.zip/g" "$GIT_ROOT/README.md"
fi

# ==============================================================================
# 2. Génération de toutes les distributions (client/distrib-all.sh)
# ==============================================================================
echo ""
echo "================================================================"
echo "==> [2/4] Génération des distributions (client/distrib-all.sh)..."
echo "================================================================"
cd "$CLIENT_DIR"
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

# Lecture systématique de VERSION depuis le fichier VERSION
if [ -f "$GIT_ROOT/VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < "$GIT_ROOT/VERSION")
fi
export VERSION

COMMIT_MSG="${ARGS[1]:-release: CodeLab v$VERSION (build $BUILD)}"

# Indexation des fichiers suivis, nouveaux fichiers et du fichier VERSION
git add -A
git add README.md 2>/dev/null || true
git add VERSION 2>/dev/null || true

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
if [ -f "$GIT_ROOT/VERSION" ]; then
    VERSION=$(tr -d '[:space:]' < "$GIT_ROOT/VERSION")
fi
export VERSION
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
    build = '$BUILD'
    idx = content.find('## Version ' + ver)
    if idx == -1: idx = content.find('### Version ' + ver)
    if idx != -1:
        end = content.find('\n--', idx)
        if end == -1: end = content.find('\n##', idx + 1)
        raw_notes = content[idx:end].strip() if end != -1 else content[idx:].strip()
        lines = raw_notes.split('\n')
        lines[0] = f'## Version {ver} (build {build})'
        print('\n'.join(lines))
    else:
        print(f'## Version {ver} (build {build})\n\nRelease officielle de CodeLab {ver} (build {build}).')
except Exception:
    print('## Version $VERSION (build $BUILD)\n\nRelease officielle de CodeLab $VERSION (build $BUILD).')
")

# Nettoyage préventif des fichiers d'empreintes obsolètes (.md5, .sha1, etc.)
rm -f "$DISTRIB_DIR"/*.md5 "$DISTRIB_DIR"/*.sha* 2>/dev/null || true

# Collecte des fichiers à téléverser (exclut explicitement les sommes de contrôle .md5 et .sha*)
FILES_TO_UPLOAD=()
for file in "$DISTRIB_DIR"/CodeLab-*; do
    if [ -f "$file" ]; then
        case "$file" in
            *.md5|*.sha|*.sha1|*.sha256) continue ;;
        esac
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

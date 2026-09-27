#!/bin/bash
cd "$(dirname "$0")"
BASE="$(pwd)"

echo "---------------------------------------------------------"
echo "Configuration et intégration de CodeLab sous Linux"
echo "---------------------------------------------------------"

# 1. Droits d'exécution sur les binaires et le JDK
chmod +x "$BASE/codelab" "$BASE/kill_codelab.sh" 2>/dev/null
chmod +x "$BASE"/.hidden/JDK-17.0.8.1+1/bin/* 2>/dev/null
chmod +x "$BASE"/.hidden/bin/* 2>/dev/null

# 2. Intégration dans le menu des applications (standard FreeDesktop, sans sudo)
APP_DIR="$HOME/.local/share/applications"
mkdir -p "$APP_DIR"

DESKTOP_FILE="$APP_DIR/codelab.desktop"

cat << EOF > "$DESKTOP_FILE"
[Desktop Entry]
Type=Application
Name=CodeLab
GenericName=CodeLab IDE & Simulators
Comment=Environnement pédagogique de programmation
Icon=$BASE/codelab.ico
Exec="$BASE/codelab" %F
Terminal=false
Categories=Development;IDE;Education;
StartupNotify=true
EOF

chmod +x "$DESKTOP_FILE"
echo "[OK] Lanceur ajouté au menu des applications ($DESKTOP_FILE)"

# 3. Création du raccourci sur le Bureau (si le dossier existe)
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
if [ ! -d "$DESKTOP_DIR" ] && [ -d "$HOME/Bureau" ]; then
    DESKTOP_DIR="$HOME/Bureau"
fi

if [ -d "$DESKTOP_DIR" ]; then
    cp "$DESKTOP_FILE" "$DESKTOP_DIR/codelab.desktop"
    chmod +x "$DESKTOP_DIR/codelab.desktop"
    # Autoriser le lancement sous GNOME / Ubuntu
    gio set "$DESKTOP_DIR/codelab.desktop" metadata::trusted true 2>/dev/null || true
    echo "[OK] Raccourci créé sur le Bureau ($DESKTOP_DIR/codelab.desktop)"
fi

# 4. Note pour les manettes/joysticks USB (optionnel)
if [ -d "/dev/input" ]; then
    echo "---------------------------------------------------------"
    echo "Note périphériques USB : si vous utilisez des manettes"
    echo "ou joysticks sous Linux, ajoutez votre compte au groupe input :"
    echo "  sudo usermod -aG input \$USER"
    echo "---------------------------------------------------------"
fi

echo "CodeLab est prêt ! Vous pouvez le lancer depuis le menu ou le Bureau."

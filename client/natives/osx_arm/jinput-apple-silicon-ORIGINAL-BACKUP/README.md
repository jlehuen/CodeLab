# JInput pour macOS Apple Silicon (ARM64 & Universel)

Ce dépôt fournit les binaires natifs compilés pour Apple Silicon (**ARM64** : processeurs M1, M2, M3, M4 et ultérieurs) ainsi qu'une version **Universelle** (ARM64 + Intel x86_64) de la bibliothèque **JInput** ([https://github.com/jinput/jinput](https://github.com/jinput/jinput)).

---

## 1. Contenu du paquet

```
jinput-apple-silicon/
├── bin/
│   ├── arm64/                     # Binaires natifs Apple Silicon purs
│   │   ├── libjinput-osx.dylib
│   │   └── libjinput-osx.jnilib
│   └── universal/                 # Binaires FAT universels (arm64 + x86_64)
│       ├── libjinput-osx.dylib
│       └── libjinput-osx.jnilib
├── jar/
│   └── jinput-osx-2.0.11.jar      # Classes Java (Core API + plugin OSX), bytecode Java 8
├── lib/
│   └── jutils-1.0.0.jar           # Dépendance standard jutils
├── src/                           # Code source (C & Java) autonome
├── build.sh                       # Script de recompilation intégrale automatisé
├── test.sh                        # Script de validation sous JVM ARM64
├── TestJInput.java                # Programme de test d'énumération des manettes/périphériques
└── CHECKSUMS.sha256               # Empreintes cryptographiques SHA-256
```

---

## 2. Caractéristiques techniques des binaires

- **Compilateur** : Apple Clang 21.0.0 avec IOKit et CoreFoundation.
- **Cible ARM64** : `arm64-apple-darwin` avec `-mmacosx-version-min=11.0` (optimisations `-O3`, dépouillé de symboles de debug `-S -X`).
- **Cible x86_64** : `x86_64-apple-darwin` avec `-mmacosx-version-min=10.9`.
- **Signature de code** : Signé ad-hoc (`codesign --force --sign -`) conformément aux exigences de sécurité Gatekeeper sous macOS Apple Silicon.
- **Compatibilité bytecode** : Cible Java 8 (`--release 8`), 100% interopérable avec OpenJDK / Adoptium Temurin 8, 11, 17, 21+.

---

## 3. Test et validation

Le script `test.sh` compile et exécute `TestJInput.java` sous la JVM ARM64 adoptée :

```bash
# Tester le binaire ARM64 natif
./test.sh bin/arm64

# Tester le binaire Universel
./test.sh bin/universal
```

Exemple de résultat obtenu lors de la validation :
```text
Chargement de DefaultControllerEnvironment...
Environnement instancié : net.java.games.input.DefaultControllerEnvironment
Scan des manettes et périphériques d'entrée...
Nombre de contrôleurs détectés : 4
  [1] Nom  : Apple Internal Keyboard / Trackpad (Keyboard)
  [2] Nom  : Apple Internal Keyboard / Trackpad (Mouse)
  [3] Nom  : Pebble M350s (Mouse)
  [4] Nom  : Apple Keyboard (Keyboard - USB port)
RÉSULTAT : Succès ! La bibliothèque JNI JInput s'est chargée sans aucune UnsatisfiedLinkError.
```

---

## 4. Intégration dans CodeLab

Pour intégrer le support Apple Silicon dans **CodeLab** :

### Option A (Recommandée : binaire universel transparent)
Remplacer le binaire existant dans `~/dev/codelab/client/natives/osx_64/` par la version universelle :
```bash
cp bin/universal/libjinput-osx.jnilib /Users/lehuen/dev/codelab/client/natives/osx_64/libjinput-osx.jnilib
cp bin/universal/libjinput-osx.dylib  /Users/lehuen/dev/codelab/client/natives/osx_64/libjinput-osx.dylib
```
*Avantage* : Fonctionne immédiatement aussi bien sur les Mac Intel que sur les Mac Apple Silicon sans toucher aux scripts de packaging ni à `EventReader.java`.

### Option B (Dossier dédié ARM64)
Créer `~/dev/codelab/client/natives/osx_arm64/` et y déposer les bibliothèques de `bin/arm64/`.

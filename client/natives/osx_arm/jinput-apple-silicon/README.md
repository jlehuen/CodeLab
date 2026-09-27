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
│   ├── coreAPI/                   # API JInput de base
│   └── plugins/OSX/               # Plugin natif macOS (IOKit + CoreGraphics)
├── build.sh                       # Script de recompilation intégrale automatisé
├── test.sh                        # Script de validation sous JVM ARM64
├── TestJInput.java                # Programme de test d'énumération des contrôleurs
└── CHECKSUMS.sha256               # Empreintes cryptographiques SHA-256
```

> **Sauvegarde de sécurité** : Les binaires et sources d'origine non modifiés sont archivés dans `client/natives/osx_arm/backup_original/` et `client/natives/osx_arm/jinput-apple-silicon-ORIGINAL-BACKUP/`.

---

## 2. Caractéristiques techniques des binaires

- **Compilateur** : Apple Clang avec IOKit, CoreFoundation, CoreServices et **ApplicationServices** (CoreGraphics).
- **Cible ARM64** : `arm64-apple-darwin` avec `-mmacosx-version-min=11.0` (optimisations `-O3`, dépouillé de symboles de debug `-S -X`).
- **Cible x86_64** : `x86_64-apple-darwin` avec `-mmacosx-version-min=10.9`.
- **Signature de code** : Signé ad-hoc (`codesign --force --sign -`) conformément aux exigences de sécurité Gatekeeper sous macOS Apple Silicon.
- **Compatibilité bytecode** : Cible Java 8 (`--release 8`), 100% interopérable avec OpenJDK / Adoptium Temurin 8, 11, 17, 21+.

---

## 3. Support spécifique du Trackpad Interne sur Apple Silicon

Sur Mac Intel, le trackpad interne était relié au bus USB interne (`IOUSBDevice`) et transmettait ses deltas de déplacement et clics directement dans la file IOKit `IOHIDQueueInterface`.

Sur Mac Apple Silicon (M1/M2/M3/M4) :
1. Le clavier et le trackpad interne sont reliés en direct en bus FIFO série au coprocesseur SPU d'Apple (`AppleHIDTransportHIDDevice`).
2. Le clavier transmet toujours ses touches à IOKit HID, mais Apple a réservé le flux du trackpad interne exclusivement au sous-système multitouch (`SkyLight` / `WindowServer`). La file IOKit matérielle reste donc muette (`kIOReturnUnderrun`) pour les applications utilisateur ordinaires.
3. **Résolution native JInput** :
   - Ajout de `net_java_games_input_OSXMouse.c` avec la fonction native JNI `nPollPointer`.
   - Interrogation de l'état système du pointeur (`CGEventGetLocation`, `CGGetLastMouseDelta` et `CGEventSourceButtonState` via CoreGraphics/ApplicationServices).
   - Dans `OSXMouse.java`, surcharge propre de `pollDevice()` pour le trackpad interne : conversion automatique des deltas (`x`, `y`) et états de boutons (`Left`, `Right`, `Middle`) en événements JInput standard dans `getEventQueue()`.
   - Les souris externes (USB ou Bluetooth) continuent d'utiliser prioritairement les rapports HID matériels.

---

## 4. Test et validation

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
Nombre de contrôleurs détectés : 2
  [1] Nom  : Apple Internal Keyboard / Trackpad (Keyboard)
  [2] Nom  : Apple Internal Keyboard / Trackpad (Mouse)
RÉSULTAT : Succès ! La bibliothèque JNI JInput s'est chargée sans aucune UnsatisfiedLinkError.
```

---

## 5. Intégration dans CodeLab

Pour déployer les binaires dans **CodeLab** :
- Les binaires ARM64 sont déployés dans `client/natives/osx_arm/libjinput-osx.dylib` et `.jnilib`.
- La classe Java `OSXMouse.class` est mise à jour dans `client/hidden/codelab/libraries/jinput/jinput-2.0.9.jar`.
- `CodeLab` utilise directement la bibliothèque native sans aucun contournement applicatif dans `EventReader` ou `EventViewer`.

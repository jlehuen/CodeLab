# CodeLab — Architecture & Synthèse Technique

Ce document constitue la référence technique et conceptuelle complète du projet **CodeLab** (client v2), à destination du développeur et de l'assistant IA (Antigravity).

---

## 1. Présentation Générale

- **Nom du projet** : CodeLab IDE & Simulators
- **Auteur & Concepteur** : Jérôme Lehuen (Maître de Conférences, Le Mans Université, LIUM)
- **Site officiel** : [https://codelab.univ-lemans.fr](https://codelab.univ-lemans.fr/)
- **Dépôt officiel GitHub** : [https://github.com/jlehuen/CodeLab](https://github.com/jlehuen/CodeLab)
- **Dépôt légal APP** : N° `IDDN-FR-001-260031-000-SC-2022-000-10000` (Agence pour la Protection des Programmes)
- **Statut légal & Licence** : © 2021-2026 Jérôme Lehuen, Le Mans Université. Licence Institutionnelle Éducative & Non-Commerciale (gratuite pour l'enseignement et la recherche, interdiction formelle de toute exploitation commerciale sans accord écrit préalable de l'auteur et de Le Mans Université). Manuels et documentation sous Creative Commons CC-BY-NC-ND.

### Philosophie & Objectifs Pédagogiques
CodeLab est un environnement de développement intégré (IDE) et de simulation destiné à l'apprentissage de la programmation (collège, lycée NSI, licence L1/L2 universitaire).
- **Au-delà de l'écran-clavier** : Rompre avec les exercices textuels austères en connectant le code à des simulateurs interactifs riches (modules PAC) et des périphériques variés (capteurs, manettes USB, joysticks).
- **Fondements théoriques** : Inspiré par le cognitivisme et les travaux de Jean Piaget, Seymour Papert (créateur du langage Logo et de sa tortue, pionnier de Lego Mindstorms) et Marvin Minsky.
- **Large palette de paradigmes** : Impératif (C, Go), orienté objet (Java), multiparadigme/scripting (Python), déclaratif/règles (CLIPS), fonctionnel (Haskell), visuel/artistique (Processing), visuel sans syntaxe (langage par Blocs traduisible en Python).
- **Zéro installation pour l'apprenant** : Embarque son propre JDK (Java 17 Temurin), interpréteurs, et librairies natives sans nécessiter de configuration préalable du poste client.

---

## 2. Organisation du Répertoire Projet

L'arborescence globale dans `~/dev/codelab/` comprend :
- `client_old/` : Version précédente du client (historique avec ancien serveur Java).
- `client/` : **Workspace actif courant**, version modernisée pour interagir avec le nouveau serveur en Rust.
- `server/` : Code source et scripts de déploiement du serveur (Rust / Tokio / MessagePack).
- `builds/` : Archives et binaires générés.
- `consignes_*.eml` : Notes et communications de service (déploiement du serveur Rust, configuration de rentrée).

### Structure interne de `client/`

```
client/
├── bin/                      # Binaires et utilitaires système par OS (linux_64, windows_64)
├── src/                      # Sources Java (package racine: codelab)
│   ├── codelab/              # Noyau de l'IDE, moteur d'exécution, compilation
│   │   ├── client/           # Client réseau (protocole Rust, MessagePack, sessions)
│   │   ├── common/           # Utilitaires de sérialisation
│   │   ├── console/          # Terminal intégré, gestion des flux stdout/stderr
│   │   ├── controllers/      # Gestion JInput (USB, manettes) et widgets virtuels
│   │   ├── modules/          # Modules applicatifs (editeur, graphics, robotics)
│   │   └── utils/            # Boîte à outils (audio, cryptographie, HTTP, XML, Jess)
├── data/                     # Ressources empaquetées dans le JAR (images, audio, syslang)
├── hidden/                   # Fichiers internes et codelab.jar produit
├── icons/                    # Assets graphiques (logos, icônes DMG/App et make-icns.sh)
├── mac-app/                  # Skeleton de l'application macOS (Info.plist, JDK 17, launchers)
├── java/                     # JDKs 17 (Linux, Windows) et outils de build (Apache Ant 1.10.15)
├── launchers/                # Scripts et lanceurs d'installation (Unix, Windows, icônes, readme)
├── modules/                  # Plugins applicatifs PAC distribués (.pac)
├── natives/                  # Bibliothèques natives C/C++ par OS (.dylib, .so, .dll)
├── sys-properties/           # Propriétés système immuables par OS
├── user-properties/          # Modèles de propriétés utilisateur modifiables par OS
├── codelab.files/            # Squelette initial déployé dans ~/codelab.files/
├── build.xml                 # Fichier de build Ant
├── build.sh                  # Script de compilation local (Ant codelab / codelab-pro)
├── build-test.command        # Script de build et lancement immédiat de CodeLab.app
├── distrib-*.sh              # Scripts de packaging (mac, linux, win64, all, ic2, test)
├── update-web-properties.sh  # Déploiement distant de web.properties sur transit
├── ssh-transit.command       # Raccourci de connexion SSH vers le serveur transit
└── gemini.sh                 # Lanceur d'Antigravity avec configuration du proxy universitaire
```

---

## 3. Architecture Technique

### 3.1. Client Java Desktop
- **Version Java cible** : Java 17 LTS (JDK Temurin embarqué dans chaque distribution).
- **Interface Graphique** : Java Swing modernisé avec la bibliothèque **FlatLaf** (`FlatLightLaf`, `FlatDarkLaf`), look & feel adaptatif, support Retina et anti-aliasing AWT/OpenGL (`sun.java2d.opengl=true`).
- **Composants d'interface** :
  - Barre d'outils dynamique selon le module actif (`AbstractToolbar`).
  - Onglets multi-modules (`JTabbedPane`).
  - Console intégrée avec liens cliquables vers les erreurs de code (`ConsoleLinkAction`), architecture d'arrière-plan haute cadence sans saturation de l'EDT, surligneur défensif et aimant de focus clavier automatique.
  - Éditeur bimodal : mode texte syntaxique (`CodeLabTextArea`) et mode blocs visuels (`ScratchEditor`).

### 3.2. Architecture Réseau & Serveur Rust
CodeLab intègre un mode classe virtuelle temps réel pour assister les séances de travaux pratiques (présentiel ou distanciel) :
- **Serveur distant** : Serveur asynchrone écrit en **Rust** (`codelab-serv.univ-lemans.fr:9988`), remplaçant l'ancien serveur Java.
  - Basé sur **Tokio** (gestion non-bloquante de centaines de connexions simultanées).
  - Sérialisation binaire via **MessagePack** (`org.msgpack.core`), garantissant performances et faible empreinte réseau.
  - Transfert complet d'arborescences de sessions compressées en **TAR + ZStandard** (`TarZstExtractor.java`).
  - Authentification et sécurisation : Hachage des mots de passe en **SHA-256**, séparation stricte des sessions et des rôles.
- **Rôles utilisateurs** (`Statut.java`) :
  - `STUDENT` : Travaille sur ses fichiers, peut demander de l'aide (indicateur visuel), peut voir son poste contrôlé à distance par un tuteur.
  - `TUTOR` : Accède au panneau `UserTable`, supervise en direct la liste des étudiants connectés, ouvre/ferme la session, inspecte le code des étudiants en direct, prend le contrôle de l'éditeur d'un étudiant (`SET_CONTROL_MODE`), clone localement un programme étudiant en mode bac à sable pour tester/modifier le code en toute sécurité, diffuse des messages généraux ou individuels (`CHAT_TO`, `MESSAGE_TO_ALL`), réinitialise les mots de passe.
  - `ADMIN` : Rôle super-utilisateur.
  - `STANDALONE` : Mode hors-ligne par défaut.
- **Communication client ↔ serveur** :
  - Sortant : [`ServerFacade.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/ServerFacade.java) envoie des commandes MessagePack structurées (`PING`, `CHANGE_PASSWORD`, `SET_SESSION_OPENNED`, `UPLOAD_FILE`, `ASK_CONTROL_MODE`, etc.).
  - Entrant : [`CommandHandler.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/CommandHandler.java) écoute sur le socket et utilise la réflexion Java pour router dynamiquement les messages entrants vers les méthodes correspondantes de [`CodelabClient.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/CodelabClient.java) (`INIT_SESSION`, `UPDATE_FILE`, `CLIENT_ARRIVED`, `CLIENT_EXITED`, `SET_CONTROLLED`, etc.).

### 3.3. Gestion de l'Exécution et Compilation
- **Compilateur** ([`Compilateur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Compilateur.java)) :
  - Support de C (`gcc` avec gestion complète des options et drapeaux multiples ex: `GCC_CMD=gcc -std=c99 -Wall -pedantic` découpés proprement via [`Utils.splitCommandLine`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) avec support des guillemets et des espaces dans les chemins), Java (`javac`), Go (`go build`), Haskell (`ghc`), Processing (`.pde` converti en classe Java via un pré-processeur interne).
  - Timeout configurable (`COMPILE_TIMEOUT`) avec terminaison forcée (`destroyForcibly()`) en cas de boucle infinie du préprocesseur.
  - Nettoyage sécurisé des fichiers `.class` en Java sans commande shell externe.
  - Interception des warnings et des erreurs avec formatage spécifique pour la console pédagogique.
- **Exécuteur** ([`Executeur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Executeur.java)) :
  - Exécution asynchrone sur un thread dédié distinct du thread graphique Swing (EDT).
  - **Séparation stricte des flux** : `stdout` et `stderr` sont lus en parallèle dans deux threads distincts (`fluxSortie` et `fluxErreur`), éliminant tout risque d'interblocage (deadlock) par saturation des tampons de communication de l'OS.
  - **Arrêt forcé multi-niveaux et déterministe** : À l'arrêt, `process.destroy()` et fermeture récursive des descendants (`process.descendants().forEach(ProcessHandle::destroy)`), suivie d'un `destroyForcibly()` sous 500 ms si le processus ne répond pas. Fermeture immédiate des flux de lecture via `closeReader()`. Émission systématique d'un saut de ligne `\n` sur la console avant le message `[codelab] Exécution stoppée` pour garantir un début de ligne propre.
  - **Garantie de terminaison** : Bloc `finally` systématique dans `Executeur.run()` garantissant que [`codelab.executionCompleted()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java) est toujours exécuté sans dépendre de threads minuteurs externes.
  - Support de Python, CLIPS (embarqué via binaire adapté `clips631`), programmes compilés, et scripts blocs.
- **Watchdog Graphique Swing** ([`SwingWatchdog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/SwingWatchdog.java)) :
  - Thread démon surveillant en permanence la réactivité de l'Event Dispatch Thread (EDT) Swing par un battement de cœur régulier (1s), immunisé contre les faux positifs de mise en veille de la machine (suspension OS détectée par dérive du sleep).
  - En cas de blocage réel d'interface (> 4s) : détection automatique des deadlocks JVM (`ThreadMXBean.findDeadlockedThreads()`), capture intégrale de la pile d'exécution de tous les threads JVM avec identification des verrous et de leurs propriétaires, écriture persistante immédiate dans `~/codelab.files/freeze.log`, `codelab.log` et `System.err`, et tentative d'arrêt d'urgence du programme utilisateur en cours (`CodeLab.INSTANCE.halt()`).
- **Générateur Audio & Synthétiseur** ([`MiniSynth.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/audio/MiniSynth.java)) :
  - Boucle d'attente découpée (pas de 50 ms) avec annulation réactive par drapeau `cancelTone` et interruption de thread (`activeToneThread.interrupt()`).
  - Coupure instantanée par `MiniSynth.stop()` sans blocage du thread Swing ni réinitialisation synchrone superflue du moteur audio (évitant l'épuisement des périphériques natifs CoreAudio sous macOS).

### 3.4. Modules Applicatifs & Système PAC
Les modules applicatifs enrichissent l'IDE :
- **Modules intégrés** :
  - `ModuleEditor` : Éditeur de code complet et gestionnaire de fichiers (`FileManager`).
  - `ModuleGraphics` : Canevas 2D avec pilote de tortue Logo.
  - `ModuleRobotics` : Simulation 2D de robots mobiles (plateforme différentielle, capteurs virtuels de contact, ultrasons, couleurs, boussole).
- **Plugins PAC (`.pac`)** :
  - Fichiers `.pac` déposés dans `~/codelab.files/modules/`.
  - Chargés dynamiquement par [`PluginLoader.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/PluginLoader.java).
  - Exemple fourni : `MotorModule.pac` (simulation de servomoteur Lego NXT avec compteurs angulaires).

---

## 4. Données et Environnement Runtime

Au premier lancement, CodeLab déploie dans le dossier personnel de l'utilisateur l'espace :
`~/codelab.files/`
- `.hidden/` :
  - `CDK/` : CodeLab Development Kit pour créer des plugins PAC.
  - `includes/` : Bibliothèques d'en-tête et API par langage (C, Java, Python, Go...).
  - `templates/` & `exercices/` : Modèles de démarrage et exercices fournis.
  - `programs/local/` : Fichiers utilisateur en mode autonome.
  - `programs/dist/` : Fichiers synchronisés lors d'une session connectée.
  - `codelab.log` : Journalisation d'exécution.
- `modules/` : Emplacement où l'utilisateur place les modules `.pac`.
- `userdata/` :
  - `user.properties` : Fichier de configuration utilisateur (modifié via l'IHM ou directement).
  - `media/` : Sons et images personnalisés.

### Fichiers de Configuration
1. **Propriétés Système** (`sys-properties/sysconfig-<os>.properties`) :
   - Délais de synchronisation (`UPDATE_PERIOD=3000`, `UPDATE_PERIOD_FAST=1000`).
   - Périodicité des sauvegardes automatiques (`BACKUP_PERIOD=5` min).
   - Chemins des utilitaires embarqués (`astyle` pour le formatage automatique).
2. **Propriétés Utilisateur** (`user-properties/user-<os>.properties`) :
   - Mode connecté (`CONNECTED_MODE=yes/no`).
   - Hôte et port serveur (`SERVER_HOST=codelab-serv.univ-lemans.fr`, `SERVER_PORT=9988`).
   - Configuration Proxy (`PROXY_HOST`, `PROXY_PORT`).
   - Détection des compilateurs et interpréteurs système (`GCC_CMD`, `PYTHON_CMD`, `JAVA_HOME`).
   - Paramètres de console et polices de l'éditeur.

---

## 5. Procédures de Compilation, Test et Déploiement

### 5.1. Compilation locale
Le script [`build.sh`](file:///Users/lehuen/dev/codelab/client/build.sh) configure automatiquement le `JAVA_HOME` vers le JDK 17 embarqué dans `mac-app`, définit la variable de version (`export VERSION=1.3.2`) et appelle Ant :
```bash
./build.sh          # Compile et produit hidden/codelab/codelab.jar
./build.sh --pro    # Compile avec obfuscation ProGuard
```
Lors de l'étape de préparation, la cible Ant `replace` dans [`build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml) met à jour automatiquement dans [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) :
1. Le numéro de version (`VERSION = "${env.VERSION}"`) injecté via regex à partir de la variable d'environnement.
2. Le numéro de build (`BUILD = "${build}"`) horodaté au format `yyMMddHHmm` (ex: `2609091136`).

### 5.2. Test rapide sur macOS
Le script [`build-test.command`](file:///Users/lehuen/dev/codelab/client/build-test.command) automatise tout le cycle de vérification :
```bash
./build-test.command
```
1. Supprime l'ancien `/Users/lehuen/Desktop/CodeLab.app`.
2. Compile via `./build.sh`.
3. Assemble le bundle macOS via [`distrib-test.sh`](file:///Users/lehuen/dev/codelab/client/distrib-test.sh) (détection automatique de l'architecture locale Apple Silicon ou Intel via `uname -m`, appelant `./distrib-mac.sh "$ARCH" -app`).
4. Lance la nouvelle application immédiatement.

### 5.3. Packaging & Distributions Multi-plateformes
- **macOS** ([`distrib-mac.sh`](file:///Users/lehuen/dev/codelab/client/distrib-mac.sh)) :
  - Script unifié prenant en argument obligatoire l'architecture cible : `<-x64 | -arm> [-app]`.
    - `-arm` / `--arm` : Distribution native **Apple Silicon (ARM64)** utilisant le squelette [`mac-app-arm`](file:///Users/lehuen/dev/codelab/client/mac-app-arm) (JDK 17 aarch64, binaires et bibliothèques natives `natives/osx_arm`), produisant l'image disque `CodeLab-MacOS-ARM64-<version>.dmg`.
    - `-x64` / `--x64` : Distribution **Intel (x86_64)** utilisant le squelette [`mac-app-x64`](file:///Users/lehuen/dev/codelab/client/mac-app-x64) (JDK 17 x86_64, binaires et bibliothèques natives `natives/osx_64`), produisant l'image disque `CodeLab-MacOS-<version>.dmg`.
    - `-app` / `--app` : Mode développement générant uniquement l'application `CodeLab.app` sur le Bureau sans construire l'image DMG.
  - Génère l'image disque compressée stylisée (image de fond, alias vers `/Applications`, icône de volume personnalisée `.VolumeIcon.icns` persistante même après téléchargement web, positionnement précis des icônes via AppleScript) et ses sommes de contrôle MD5 et SHA-1.
  - Nettoie les attributs étendus (`xattr -cr`), signe le bundle en ad-hoc (`codesign --force --deep --sign -`), et actualise le cache LaunchServices (`lsregister -f "$APP"`).
  - Au runtime, `run.sh` retire silencieusement la quarantaine du JDK et des natives sans sudo ni Terminal, et prévient en cas de lancement direct depuis le DMG.
- **Linux** ([`distrib-linux.sh`](file:///Users/lehuen/dev/codelab/client/distrib-linux.sh)) :
  - Assure les droits `chmod +x` sur tous les binaires (`codelab`, JDK, scripts) et exclut les fichiers cachés macOS (`._*`).
  - Intègre [`first_time.sh`](file:///Users/lehuen/dev/codelab/client/launchers/unix/first_time.sh) pour créer dynamiquement le lanceur `codelab.desktop` dans `~/.local/share/applications/` et sur le Bureau sans sudo.
- **Linux IC2** ([`distrib-ic2.sh`](file:///Users/lehuen/dev/codelab/client/distrib-ic2.sh)) :
  - Déclinaison Linux dédiée aux salles de travaux pratiques de l'Institut Claude Chappe (Le Mans Université).
  - Intègre le profil [`user-linux-ic2.properties`](file:///Users/lehuen/dev/codelab/client/user-properties/user-linux-ic2.properties) configuré d'office en mode connecté sur le serveur Rust universitaire (`codelab-serv.univ-lemans.fr:9988`) avec interpréteur Python ciblant `/opt/python_venv/bin/python`.
  - Embarque le plugin robotique additionnel [`MotorModule.pac`](file:///Users/lehuen/dev/codelab/client/modules/MotorModule.pac).
  - Génère une archive compressée **`CodeLab-ic2-<build>.tgz`** (excluant `.DS_Store`), la téléverse automatiquement via `scp` sur le serveur académique `transit.univ-lemans.fr:public_html/temp/` et envoie un email de notification contenant le lien direct de téléchargement.
- **Windows** ([`distrib-win64.sh`](file:///Users/lehuen/dev/codelab/client/distrib-win64.sh) & [`distrib-win64-nsis.sh`](file:///Users/lehuen/dev/codelab/client/distrib-win64-nsis.sh)) :
  - Distribution portable ZIP classique (`distrib-win64.sh`) avec [`install.bat`](file:///Users/lehuen/dev/codelab/client/launchers/windows/install.bat) créant le raccourci Bureau sans droits admin.
  - Installateur moderne exécutable (`distrib-win64-nsis.sh` via `makensis` et [`codelab.nsi`](file:///Users/lehuen/dev/codelab/client/launchers/windows/codelab.nsi)) : compression LZMA solide, installation par utilisateur dans `%LOCALAPPDATA%\Programs\CodeLab` sans privilèges admin, création des raccourcis Bureau/Menu Démarrer, et désinstalleur officiel enregistré dans les paramètres Windows.
  - [`codelab.c`](file:///Users/lehuen/dev/codelab/client/launchers/windows/codelab.c) utilise `USERPROFILE` et entoure tous les chemins de guillemets stricts (`START "" "%s\bin\javaw.exe" ...`) pour supporter les espaces.
- **Tous OS** ([`distrib-all.sh`](file:///Users/lehuen/dev/codelab/client/distrib-all.sh)) :
  - Enchaîne la génération complète de toutes les cibles : macOS Apple Silicon (`distrib-mac.sh -arm`), macOS Intel (`distrib-mac.sh -x64`), Windows 64-bit (`distrib-win64.sh`) et Linux 64-bit (`distrib-linux.sh`), puis regroupe l'ensemble des paquets et sommes de contrôle dans `~/Desktop/<version>/` avec [`launchers/index.html`](file:///Users/lehuen/dev/codelab/client/launchers/index.html).

### 5.4. Déploiement distant de la configuration de version (`web.properties`)
Le script [`update-web-properties.sh`](file:///Users/lehuen/dev/codelab/client/update-web-properties.sh) extrait dynamiquement `VERSION` et `BUILD` depuis `AbstractCodeLab.java`, génère un fichier `web.properties` propre et le téléverse directement par `scp` sur le serveur académique :
```bash
./update-web-properties.sh   # Déploie sur transit.univ-lemans.fr:public_html/codelab/data/
```
Ce fichier est immédiatement servi par le site officiel à l'adresse `https://codelab.univ-lemans.fr/data/web.properties`.

### 5.5. Travailler sur le réseau de l'Université du Mans
CodeLab et Antigravity nécessitent la configuration du proxy académique :
```bash
export HTTP_PROXY="http://proxy.univ-lemans.fr:3128"
export HTTPS_PROXY="http://proxy.univ-lemans.fr:3128"
```
Le script [`gemini.sh`](file:///Users/lehuen/dev/codelab/client/gemini.sh) permet de lancer la CLI `agy` directement avec ces variables d'environnement.

---

## 6. Points Clés pour les Développements Futurs

1. **Protocole Rust / MessagePack** :
   - Toute nouvelle commande client-serveur doit être déclarée dans `ServerFacade.java` (côté client émetteur) et avoir sa contrepartie dans `server/src/` (Rust).
   - Les commandes reçues du serveur doivent porter le même nom et nombre d'arguments qu'une méthode publique de `CodelabClient.java` (résolution dynamique par réflexion dans `CommandHandler.java`).
2. **Résistance complète aux chemins avec espaces (Windows)** :
   - Tous les chemins système et utilisateurs (`HOME`, `BASE`, `codelab.files`, `javaw.exe`) sont protégés par guillemets stricts dans `codelab.c` et l'installeur NSIS.
   - La méthode universelle [`Utils.splitCommandLine()`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) protège le découpage des commandes pour les compilateurs et interpréteurs (`GCC_CMD`, `PYTHON_CMD`, `JAVAC_CMD`, `CLIPS_CMD`, `GOLANG_CMD`, `HASKELL_CMD`, `MAGIC_CMD`), en préservant les guillemets et en intégrant une heuristique de détection automatique des exécutables installés dans des dossiers à espaces (ex: `C:\Program Files\...`).
3. **Gestion et mise à jour des versions** :
   - Le numéro de version est défini de manière centralisée dans [`build.sh`](file:///Users/lehuen/dev/codelab/client/build.sh) via `export VERSION=x.x.x`.
   - La cible Ant `replace` dans [`build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml) synchronise automatiquement `VERSION` (depuis `${env.VERSION}`) et `BUILD` (`yyMMddHHmm`) dans [`src/codelab/AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) lors de la compilation.
   - La cible Ant `update-web-properties` met automatiquement à jour `BUILD` et `VERSION` dans `../www/www/data/web.properties`.
   - Le script [`update-web-properties.sh`](file:///Users/lehuen/dev/codelab/client/update-web-properties.sh) téléverse ce fichier sur `transit.univ-lemans.fr` pour publication sur le site `https://codelab.univ-lemans.fr/data/web.properties`.
   - Les scripts de distribution (`distrib-*.sh`) extraient ensuite dynamiquement ce numéro de version et de build depuis `AbstractCodeLab.java` via `pcregrep` pour nommer les archives et répertoires de sortie (`~/Desktop/<version>/CodeLab-MacOS-<version>.dmg`, etc.).
   - **Mécanisme de mise à jour propre & Invalidation de cache** : [`CodeLab.checkVersion()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java) compare la version et le build locaux à ceux déclarés à distance dans `web.properties` (comparaison sémantique de version via [`Version`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Version.java), puis de build en cas d'égalité de version). Si une version plus récente existe, une boîte de dialogue affiche les versions, un lien cliquable vers `versions.html` et propose un bouton direct pour ouvrir la page officielle des téléchargements localisée (`DOWNLOADS_PAGE_URL`, ex: `downloads-fr.php`) dans le navigateur par défaut. L'ancien remplacement à chaud du JAR en local (qui risquait d'altérer la signature `codesign`, de bloquer sur les droits dans `/Applications` et de provoquer des conflits de ports) est supprimé. L'élément de menu *Aide > Vérifier les mises à jour* (`itemCheckVersion`) dans [`CodeLabMenu.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLabMenu.java) réinterroge le serveur en direct. Afin d'éviter qu'un cache intermédiaire ou mandataire (comme le reverse-proxy `http-front.univ-lemans.fr` de l'Université) ne serve une version obsolète de `web.properties`, [`Utils.readPropertyResourceBundleOverInternet`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) désactive le cache JVM (`setUseCaches(false)`), ajoute les en-têtes `Cache-Control: no-cache, no-store` / `Pragma: no-cache` et injecte un paramètre d'URL dynamique horodaté (`?t=...`).
4. **Cycle de vie de l'éditeur & Fichiers hors-filemanager** :
   - L'ouverture de fichiers hors gestionnaire (`open_file(File, boolean)`, comme `user.properties`) instancie un nouvel éditeur sans nœud associé dans le `JTree`.
   - Dans `FileManager.java`, le `TreeSelectionListener` filtre explicitement les désélections (`!e.isAddedPath()`) pour éviter qu'un `resetSelectedNode()` ne ré-ouvre l'ancien fichier sélectionné lors de l'ouverture d'un fichier externe.
   - Dans `TextEditor.java`, `setEditableConfiguration` utilise un drapeau d'initialisation (`initialized`) pour garantir l'exécution complète du premier paramétrage Swing (focus, caret, toolbar, background), évitant le court-circuit `if (value == editable) return;` sur un éditeur fraîchement créé.
   - En mode connecté (tuteur ou admin), `getEditableStrategy()` verrouille normalement l'éditeur sur les fichiers étudiants. Pour les fichiers hors gestionnaire (`open_file(File, boolean)` avec `editor.getFileDescriptor() == null`), `open_file` force explicitement `setEditableConfiguration(true)` et `getEditableStrategy()` préserve l'état éditable afin d'éviter le verrouillage en lecture seule (fond gris, composant non `enabled`).
   - De même, dans `saveCurrentFile()` et `change_file()`, la sauvegarde locale `editor.save_content()` est préservée pour ces fichiers hors gestionnaire même en statut tuteur/admin (sans déclencher d'upload serveur).
5. **Fiabilité de l'Exécution et Contrôle des Processus** :
   - Prise en charge des arguments multiples et des chemins avec espaces dans `GCC_CMD` et l'ensemble des commandes système : utilisation systématique de [`Utils.splitCommandLine()`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) gérant les guillemets et les espaces pour `ProcessBuilder`, tandis que `check_C()` teste le binaire de base (`splitCommandLine().get(0)`).
   - Séparation stricte des flux `stdout` et `stderr` en deux threads autonomes dans `Executeur.java` pour éliminer tout risque de deadlock par saturation de pipe OS.
   - Sécurisation des commandes utilitaires synchrones ([`Utils.execute()`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) et [`Utils.execute2()`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java)) avec timeout de 5 secondes (`waitFor`), fusion des flux d'erreur (`redirectErrorStream(true)`) et destruction forcée en cas de blocage d'un sous-processus externe.
   - Fiabilisation des vérifications de modules Python au démarrage ([`check_Numpy()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java#L926) et [`check_Matplotlib()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java#L942)) : gestion robuste des retours d'erreur (détection des sorties `null`, `ERROR` ou vides) pour afficher correctement les avertissements et liens d'installation.
   - Fermeture propre des flux de lecture (`closeReader()`) et terminaison déterministe via le bloc `finally` de `Executeur.run()`, sans aucun timer aveugle dans `CodeLab.halt()`.
   - Coupure audio réactive dans `MiniSynth.java` avec contrôle de `cancelTone` et arrêt sans blocage de l'IHM.
6. **Watchdog Swing (EDT Heartbeat) & Arrêt d'urgence** :
   - Thread démon `SwingWatchdog.java` surveillant la réactivité de la boucle d'événements Swing toutes les secondes (seuil de freeze à 4s).
   - En cas de gel (> 4s) : détection automatique des deadlocks JVM via `ThreadMXBean.findDeadlockedThreads()`, capture intégrale de tous les threads de la JVM avec leurs verrous dans `~/codelab.files/freeze.log` et `codelab.log`, et arrêt d'urgence du processus utilisateur pour libérer l'IHM.
   - Au lancement, la console affiche le rappel d'arrêt forcé d'urgence en ligne de commande : `Tip: Use 'pkill -9 -fi codelab' in case of panic`.
7. **Calcul Dynamique et Immédiat des Capteurs Robotiques** :
   - Dans `SensorSonic.java`, `SensorTouch.java`, `SensorColor.java` et `SensorArray.java`, la mesure est calculée à la demande dès l'appel à `getValue()` (ou `getValue(int)`).
   - Les capteurs ne dépendent plus du cycle de rendu graphique Swing (`draw()`), garantissant des valeurs exactes dès le démarrage d'un programme, même avant le premier rafraîchissement d'écran.
8. **Mode Bac à sable Tuteur (Clonage local des programmes étudiants)** :
   - Accessible aux tuteurs et administrateurs depuis le menu contextuel (clic droit) de l'arborescence des fichiers ([`FileManager.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/manager/FileManager.java)), de l'éditeur texte ([`CodeLabTextArea.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/text/CodeLabTextArea.java)) et de l'éditeur graphique de blocs ([`GlassPanePopupMenu.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/scratch/GlassPanePopupMenu.java)).
   - **Isolation stricte et locale** : Le clone généré (`<nom>_clone[N].<ext>`) réside exclusivement sur la machine locale du tuteur. Il n'est jamais téléversé au serveur ni diffusé à l'étudiant.
   - **Suppression des requêtes réseau parasites** : Dans [`CodelabClient.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/CodelabClient.java), `downloadCurrentFile()` ignore les fichiers clones pour éviter que le tuteur ne télécharge par-dessus son clone le fichier original distant. De même, la sélection d'un clone dans l'arborescence n'émet aucune requête `requestFile`.
   - **Protection contre les mises à jour distantes** : [`ModuleEditor.updateContent()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/ModuleEditor.java) vérifie que le chemin du fichier mis à jour par l'étudiant correspond strictement au fichier affiché, empêchant les frappes en direct de l'étudiant d'écraser le clone en cours d'édition chez le tuteur.
   - **Édition et sauvegarde automatique** : [`getEditableStrategy()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/ModuleEditor.java) déverrouille l'éditeur pour les tuteurs/admins dès que le fichier est un clone (`isCloneCurrentFile() == true`), quel que soit le type d'éditeur (texte ou Scratch). La sauvegarde locale automatique est active lors des changements de fichier et par minuterie régulière (`task_1` dans `CodelabClient.java`).
   - **Adaptation Java** : Lors du clonage d'un fichier source Java, la déclaration `public class <Nom>` est automatiquement réécrite en `public class <Nom>_clone` dans le code source pour respecter les contraintes de compilation du JDK.
   - **Suppression propre** : La suppression (`delete_clone`) demande confirmation, nettoie les binaires et fichiers compilés associés (`.class`, exécutable C), met à jour le `JTree` et rouvre automatiquement le fichier original de l'étudiant dans l'éditeur.
   - **Indicateur visuel de verrouillage & Interaction** :
     - Lorsque l'éditeur est verrouillé en lecture seule (`!editable`), un cadenas flottant ([`data/locked.png`](file:///Users/lehuen/dev/codelab/client/data/locked.png)) est superposé en haut à droite via un `JLayeredPane` ([`AbstractEditor.wrapWithLockLayer()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/AbstractEditor.java#L51)) dans la couche `PALETTE_LAYER`.
     - L'utilisation d'un composant autonome `JLabel` au-dessus de la zone de défilement garantit un survol interactif natif (curseur `HAND_CURSOR` et infobulle Swing) et empêche tout écrasement ou effacement du cadenas lors des rafraîchissements ou des sélections de texte.
     - Un clic sur le cadenas par un tuteur déclenche immédiatement le clonage du programme pour basculer en mode bac à sable et rendre l'éditeur immédiatement modifiable.
9. **Architecture de la Console, Concurrence et Réactivité Clavier/IHM** :
   - **Thread d'arrière-plan vs Saturation de l'EDT** : `Console.run()` opère sur son propre thread worker pour vider la file FIFO des messages (`queue`). Il est impératif de **ne pas** relayer les écritures par blocs sur l'EDT via `SwingUtilities.invokeLater` : en présence d'une boucle infinie d'affichage (ex: `while (1) puts("xxxx...");`), l'EDT Swing serait submergé par des dizaines de milliers de `Runnable` par seconde, privant l'interface graphique de tout événement utilisateur (le bouton "Arrêter" devient inopérant et l'IDE gèle). `DefaultStyledDocument` (`AbstractDocument`) intégrant nativement son propre système de verrous réentrants (`writeLock()` / `readLock()`), les insertions directes en arrière-plan sont thread-safe.
   - **Sécurisation du surligneur face aux accès concurrents (`SafeDefaultHighlighter`)** : Lors d'impressions très rapides pendant que l'EDT exécute le rendu graphique (`paint`), le `DefaultHighlighter` Swing standard peut lever une `ArrayIndexOutOfBoundsException: 0 >= 0` dans `paintLayeredHighlights()` lors du recalcul des coordonnées de vue. Dans [`Console.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/console/Console.java), le surligneur du `textPane` est encapsulé dans un `SafeDefaultHighlighter` qui intercepte et neutralise silencieusement cette anomalie fugitive sans bloquer l'affichage.
   - **Préservation de la sélection utilisateur et du curseur** : Dans `_print_` et `_backspace_`, le repositionnement automatique du curseur (`textPane.getCaret().setDot(Integer.MAX_VALUE)`) est conditionné par `if (textPane.getSelectedText() == null)`. Ainsi, si l'utilisateur surligne ou sélectionne du texte dans la console pendant qu'un flux s'affiche, sa sélection n'est pas effacée et les balises de surlignage restent intactes.
   - **Aimant de focus clavier global (`KeyEventDispatcher`)** : Lorsqu'un programme utilisateur sollicite une saisie interactive sur l'entrée standard (ex: `scanf()`, `input()`, `cin`), le focus clavier peut être resté dans l'éditeur de code. Pour éviter que l'utilisateur ne modifie son code source par inadvertance et pour ne pas perdre la première lettre tapée, un `KeyEventDispatcher` est enregistré dans le `KeyboardFocusManager` dès l'instanciation de la console. Dès qu'un programme est en cours d'exécution (`executeur != null`), si la console n'a pas le focus et qu'aucune boîte de dialogue modale n'est active, toute frappe (hors raccourcis système) est interceptée : le focus est donné à la console (`focus()`), l'événement est relayé à son écouteur clavier, et l'événement d'origine est consommé.
   - **Protection contre les déréférencements lors du tronquage** : Dans `_removeFirstLine_()`, la recherche de saut de ligne et la suppression sont bornées par `document.getLength()`, évitant toute `BadLocationException`.
   - **Veille anti-famine CPU** : Dans `waitForQueueEmpty()`, l'attente active est remplacée par un `Thread.sleep(1)` avec retour immédiat si la méthode est invoquée depuis l'EDT.
10. **Stratégie d'Organisation et Dépôt Git / GitHub** :
    - Le répertoire de travail global (`client/`, `server/`, `mac-app/`, `builds/`) pèse environ 3,5 Go en raison des JDKs 17 complets embarqués pour chaque OS (~975 Mo), des artefacts de build Ant/Rust (`server/target` ~800 Mo, `builds/` ~950 Mo) et des DMG.
    - Le code source réel, les scripts et les bibliothèques JAR ne représentent que ~75 Mo pour le client et ~1 Mo pour le serveur.
    - Pour un hébergement Git/GitHub propre et performant :
      - Utiliser un `.gitignore` strict excluant les dossiers de compilation (`target/`, `class/`, `hidden/codelab/`, `builds/`), les caches (`.DS_Store`, `._*`) et les répertoires contenant les JDKs volumineux extraits (`mac-app/Contents/Java/jdk-*`, `java/linux_64/jdk-*`, `java/windows_64/jdk-*`).
      - Les installateurs et archives complètes multiplateformes (DMG, TGZ, ZIP) doivent être publiés via les **GitHub Releases** plutôt que commités dans l'historique du dépôt Git.
11. **Résilience Réseau & Reconnexion Automatique Transparente (Mode Connecté / Wi-Fi)** (cf. [BILAN-DECONNEXIONS-RESEAU.md](file:///Users/lehuen/dev/codelab/BILAN-DECONNEXIONS-RESEAU.md)) :
    - **Options Socket TCP** : Activation systématique de `socket.setKeepAlive(true)` et `socket.setTcpNoDelay(true)` dans [`CodelabClient.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/CodelabClient.java) pour maintenir le flux ouvert au niveau de l'OS et éviter les latences de l'algorithme de Nagle.
    - **Heartbeat Applicatif Périodique** : Planification dans `startRegularTimer()` d'un `ping()` toutes les 30 secondes vers le serveur afin d'empêcher les box Wi-Fi et routeurs NAT de purger les tables de connexion lors des périodes de lecture/réflexion sans frappe au clavier.
    - **Reconnexion Automatique en Arrière-Plan** :
      - En cas de perte de flux (`SocketException` ou `EOFException` dans [`CommandHandler.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/CommandHandler.java) ou échec d'écriture dans [`AbstractServerFacade.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/AbstractServerFacade.java)), `connectionBroken()` ne rétrograde plus immédiatement en mode autonome.
      - Un thread dédié (`AutoReconnectThread`) effectue jusqu'à 5 tentatives de reconnexion espacées de 2 secondes avec les identifiants et la session courante déjà en mémoire.
      - Si le serveur répond `ERROR_CONNECTED` (connexion fantôme précédente encore enregistrée), le client envoie automatiquement `EJECTION` pour purger l'ancien socket côté serveur, puis se reconnecte avec succès sur la tentative suivante.
      - Le fichier en cours d'édition est sauvegardé localement par précaution avant la tentative et rouvert automatiquement dans l'éditeur dès que la session est réinitialisée via `INIT_SESSION`.
      - Si la reconnexion réussit, la session se poursuit sans interruption et sans boîte de dialogue bloquante. En cas d'échec définitif au bout des 5 tentatives, le client bascule proprement en mode autonome (`STANDALONE`) avec la boîte de dialogue d'information standard.
    - **Élimination de l'Effet Rebond lors d'une Déconnexion Distante** :
      - Envoi préalable par le serveur Rust du message applicatif `SERVER_SHUTDOWN` avec délai de garde de 50 ms avant coupure du socket.
      - Réception côté client déclenchant `closeConnection()` avec `intentionalDisconnect = true` et affichage du message `MepaClient_0`, neutralisant tout rebond d'auto-reconnexion.
12. **Cycle de Vie des Boîtes de Dialogue Modales & Prévention des Freezes IHM (macOS / Cocoa)** :
    - **Prévention du Deadlock Modal vs `alwaysOnTop` (macOS)** :
      - Sous macOS Cocoa, afficher une fenêtre `setAlwaysOnTop(true)` avec capture de focus ([`Numpad.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/controllers/widgets/Numpad.java)) pendant qu'une boîte de dialogue modale d'application (`NSModalSession` / `JDialog(..., true)`) est active provoque un deadlock irréversible dans le WindowServer de macOS (gel complet de l'EDT sans exception, nécessitant un `pkill`).
      - Dans [`RobotChooser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/RobotChooser.java), lors de la validation du robot, la boîte de dialogue modale est désormais fermée et détruite (`dispose()`) **avant** d'appliquer le changement de robot (`simulator.setRobot`), et l'ouverture du Numpad est relayée via `SwingUtilities.invokeLater()`.
      - De même, dans [`Simulator.setRobot()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/Simulator.java), `Numpad.INSTANCE.open()` est encapsulé dans `SwingUtilities.invokeLater()` de façon défensive pour qu'aucune invocation directe ne bloque l'EDT.
    - **Élimination des Écouteurs Parasites (`ChoosePanel`)** :
      - Dans `RobotChooser.java`, le panneau de prévisualisation `ChoosePanel` hérite désormais directement d'un simple `JPanel` au lieu de `AbstractObjectPanel`. Cela élimine l'association automatique de l'`ObjectListener`, évitant que les clics sur un capteur n'activent la logique de déplacement d'objets ou ne relaient la molette au simulateur principal en arrière-plan.
    - **Libération Propre des Ressources Natives (`dispose()`)** :
      - Remplacement du mécanisme synchrone `dispatchEvent(WINDOW_CLOSING)` par `setVisible(false); dispose();` et `setDefaultCloseOperation(DISPOSE_ON_CLOSE)` dans tous les sélecteurs modaux ([`RobotChooser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/RobotChooser.java), [`BackgroundChooser.java` robotique](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/BackgroundChooser.java), [`TurtleChooser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/graphics/TurtleChooser.java) et [`BackgroundChooser.java` graphique](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/graphics/BackgroundChooser.java)).
      - Ajout systématique d'un écouteur clavier sur la touche `Échap` (`KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)`) pour permettre à l'utilisateur d'annuler et de fermer proprement la boîte de dialogue à tout moment.
    - **Sécurisation Anti-Blocage dans les Barres d'Outils (`ToolbarSim.java` et `ToolbarGraph.java`)** :
      - Dans `ToolbarSim.java` (`action_robot()`, `action_background()`) et `ToolbarGraph.java` (`action_turtle_choose()`, `action_back()`), l'activation des boutons est encapsulée dans un bloc `try ... finally { button.setEnabled(true); }` pour garantir que les boutons de la barre d'outils ne restent jamais grisés en cas d'erreur ou d'interruption.
      - Intégration de drapeaux anti-réentrance (`isChoosingRobot`, `isChoosingBackground`, `isChoosingTurtle`) pour neutraliser les clics répétés ou doubles-clics rapides avant l'affichage modal.
    - **Synchronisation Thread-Safe de la `DataTable` de Simulation** :
      - Dans [`DataTable.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/DataTable.java), les méthodes `setSystem()` et `update()` sont déclarées `synchronized` pour éliminer les collisions d'accès concurrent entre le thread d'animation du simulateur (`simuthread`) et le thread d'interface Swing (EDT) lors d'un changement de robot à la volée.
      - Les itérations de rafraîchissement des valeurs de capteurs sont bornées par `Math.min(system.getNbProp(), model.getRowCount())`, éradiquant définitivement l'anomalie historique `ArrayIndexOutOfBoundsException: 0 >= 0`.
    - **Harmonisation et Libération Totale de l'Ensemble des Boîtes de Dialogue** :
      - Audit exhaustif de l'intégralité des classes dérivées de `JDialog` dans le client.
      - Sécurisation complète de [`FontChooser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/FontChooser.java), [`NewFileDialog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/manager/NewFileDialog.java) et [`HTMLInfoDialog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/HTMLInfoDialog.java) : configuration systématique de `setDefaultCloseOperation(DISPOSE_ON_CLOSE)`, destruction immédiate des pairs natifs (`dispose()`) lors de la fermeture, et prise en charge universelle du raccourci clavier `Échap`.
13. **Élimination des Interblocages (Deadlocks) sur l'Arrêt & Fiabilisation Audio (Version 1.3.2)** :
    - **Deadlock sur le Bouton Arrêter (EDT vs Thread de Simulation)** :
      - Lors d'un clic sur "Arrêter" ([`AbstractToolbar.action_stop()`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractToolbar.java)), l'EDT Swing appelait `module.stop()`, qui invoquait [`Simulator.stop()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/Simulator.java) et [`AbstractSimulator.stop()`](file:///Users/lehuen/dev/codelab/client/src/codelab/helper/AbstractSimulator.java).
      - Ces méthodes contenaient un `simuthread.join()` non borné exécuté directement sur l'EDT. Si `simuthread` effectuait au même moment une mise à jour d'interface Swing nécessitant le verrou AWT TreeLock (ex. `dataTable.update()`, `setValueAt()`, `adjustScrollBars()`, ou `Toolkit.getDefaultToolkit().sync()`), l'EDT était bloqué en attente de la mort du thread tandis que le thread attendait le verrou détenu par l'EDT : deadlock mutuel permanent.
      - Résolution : Remplacement par une interruption explicite `t.interrupt()` suivie d'un `join` borné à 200 ms maximum (`t.join(200)`). L'EDT ne peut plus jamais être pris en otage.
    - **Gel de la Machine Audio après un Son (`playTone`)** :
      - Dans [`CodeLab.stopaudio()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java), `MiniSynth.reset(SYNTH_OSC)` était appelé à chaque fin d'exécution ou arrêt sonore. Cela détruisait et réinstanciait le synthétiseur JSyn à la volée.
      - Sous macOS, la rotation répétée de création/destruction sans libération matérielle saturait les unités du moteur audio natif CoreAudio, provoquant un gel irréversible dans `synth.start()`.
      - Résolution : Préservation du singleton du synthétiseur dans [`MiniSynth.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/audio/MiniSynth.java). L'arrêt d'un son s'effectue simplement via `osc.noteOff()` sans réinitialisation destructive du moteur audio.
    - **Sécurisation de la Fin d'Exécution (`executionCompleted`)** :
      - Le thread d'arrière-plan de l'exécuteur ([`Executeur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Executeur.java)) appelait [`CodeLab.executionCompleted()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java), modifiant directement des composants de l'IHM hors de l'EDT.
      - Toutes les actions d'interface (`toolbar_update()`, `enableTabbedPane()`, `tabbedpane.requestFocus()`, `consolidate()`, `updateFrameTitle()`, `checkErrorExe()`) sont désormais encapsulées dans un `SwingUtilities.invokeLater()`.
    - **Watchdog Graphique avec Détection de Deadlocks et Journal Persistant (`freeze.log`)** :
      - [`SwingWatchdog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/SwingWatchdog.java) surveille l'EDT avec un heartbeat de 1s et un seuil d'alerte de 4s.
      - Immunisation contre les faux positifs de mise en veille : détection automatique de la suspension de l'OS / JVM par comparaison du temps de sommeil réel du watchdog (`actualSleep > PING_INTERVAL_MS + 2000`), réinitialisant le battement de cœur sans faux rapport ni arrêt d'urgence intempestif.
      - Dès détection d'un gel réel, le watchdog interroge le `ThreadMXBean` de la JVM pour détecter les interblocages matériels (`findDeadlockedThreads()`), réalise un dump complet de l'ensemble des threads JVM avec leurs verrous et propriétaires, et consigne le rapport complet dans `~/codelab.files/freeze.log`, `codelab.log` et `System.err`.
14. **Gestion Propre des Interruptions de Threads (`InterruptedException`) & Élimination des Faux Positifs dans les Logs** :
    - **Origine & Mécanisme** :
      - Lors de l'arrêt du simulateur robotique ([`Simulator.stop()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/Simulator.java)) ou d'un simulateur générique ([`AbstractSimulator.stop()`](file:///Users/lehuen/dev/codelab/client/src/codelab/helper/AbstractSimulator.java)), le thread de simulation `simuthread` est explicitement interrompu (`t.interrupt()`) pour le réveiller sans attendre l'expiration de son délai de rafraîchissement d'images (FPS).
      - En Java, lorsqu'un thread dort dans `Thread.sleep()`, l'interruption lève naturellement une `InterruptedException` : c'est le mécanisme standard et coopératif de la JVM pour demander la terminaison d'une tâche.
    - **Anomalie historique** :
      - Dans [`Utils.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) et [`TurtleUtils.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/graphics/TurtleUtils.java), la méthode `wait()` interceptait cette exception et l'envoyait aveuglément à `ExceptionManager.process(e)`.
      - Cela déclenchait l'impression intempestive d'une stacktrace `sleep interrupted` dans la console et la préparation d'un rapport de bogue pour une interruption tout à fait normale et souhaitée.
    - **Résolution** :
      - Dans [`Utils.wait()`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) (surcharges millisecondes et nanosecondes) et [`TurtleUtils.wait()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/graphics/TurtleUtils.java), la capture de `InterruptedException` rétablit désormais le fanion d'interruption du thread courant (`Thread.currentThread().interrupt()`) conformément aux règles de l'art en concurrence Java, sans polluer le gestionnaire d'exceptions.
      - Dans [`Simulator.run()`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/robotics/Simulator.java) et [`AbstractSimulator.run()`](file:///Users/lehuen/dev/codelab/client/src/codelab/helper/AbstractSimulator.java), la boucle principale intègre le contrôle `!Thread.currentThread().isInterrupted()`.
      - De plus, dans `Simulator.run()`, un court-circuit immédiat (`if (!isrunning || Thread.currentThread().isInterrupted()) break;`) est positionné dès la fin de `delayFPS()`, évitant tout traitement superflu (détection de collision ou rafraîchissement) lorsque le simulateur est en cours de destruction.

15. **Modernisation du Packaging Windows, Résistance aux Espaces & Fiabilisation (Version 1.4.1)** :
    - **Installateur Windows NSIS MUI2 non-administrateur** ([`launchers/windows/codelab.nsi`](file:///Users/lehuen/dev/codelab/client/launchers/windows/codelab.nsi), [`distrib-win64-nsis.sh`](file:///Users/lehuen/dev/codelab/client/distrib-win64-nsis.sh)) :
      - Packaging moderne en un exécutable d'installation unique `CodeLab-Win64-1.4.1-Setup.exe` cross-compilé sous macOS via `makensis` (0 € de coût, aucune dépendance payante, pas de certificat EV obligatoire).
      - Installation par utilisateur (`RequestExecutionLevel user`) dans `%LOCALAPPDATA%\Programs\CodeLab`, ne requérant aucun privilège administrateur Windows (UAC).
      - Compression solide LZMA, création automatique des raccourcis Bureau (`Desktop`) et Menu Démarrer (`Start Menu`), et enregistrement conforme dans « Applications installées » de Windows avec désinstalleur complet (`Uninstall.exe`).
    - **Résistance Totale aux Espaces dans les Chemins de Fichiers sous Windows** :
      - Implémentation d'une machine à états robuste dans [`Utils.splitCommandLine(String cmd)`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) : parse proprement les arguments entre guillemets doubles et simples, préserve les espaces protégés, et inclut une heuristique Windows réassemblant dynamiquement les chemins d'exécutables non quotés existant sur disque (ex: `C:\Program Files\...`).
      - Intégration dans [`Compilateur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Compilateur.java) (GCC, Go, Haskell, Processing), [`Executeur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Executeur.java) (Python, Java, CLIPS, exécutables natifs), [`TextEditor.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/text/TextEditor.java) (formateur AStyle) et [`CodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java) (détection des interpréteurs et bibliothèques).
    - **Fiabilisation du Lancement (`NumberFormatException` sur `SERVER_PORT`)** :
      - Dans [`CodeLab.main()`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java), correction de la lecture des propriétés `SERVER_HOST` et `SERVER_PORT` lorsqu'elles sont vides ou non renseignées. Les chaînes vides sont désormais acceptées sans déclencher d'exception, et `ADMIN_URL` n'est construite que si les deux paramètres sont valides.
    - **Neutralité et Sécurité des Propriétés Utilisateur par Défaut** :
      - Purge des adresses de serveurs et ports universitaires en dur dans `user-win.properties`, `user-mac.properties` et `user-linux.properties`, garantissant une neutralité complète pour les utilisateurs externes tout en réservant la configuration dédiée à `user-linux-ic2.properties`.
    - **Hygiène et Robustesse des Scripts de Distribution** :
      - Ajout de `set -e` et sous-shells stricts dans `distrib-win64.sh` et `distrib-win64-nsis.sh`.
      - Purge récursive systématique des métadonnées invisibles macOS (`.DS_Store`, AppleDouble `._*`) avant l'archivage ou la compilation NSIS.
      - Normalisation automatique des retours à la ligne en CRLF (`\r\n`) sur tous les scripts batch (`launchers/windows/*.bat`).

16. **Architecture et Résolution Native JInput sous macOS Apple Silicon (Clavier, Souris et Trackpad)** :
    - **Origine matérielle & rupture d'architecture sous Apple Silicon** :
      - Sur Mac Intel, le clavier et le trackpad interne communiquaient via le bus USB interne (`IOUSBDevice`). Le pilote USB HID diffusait des rapports de souris relative standard à la couche IOKit `IOHIDQueueInterface`, permettant à JInput de lire nativement les axes et boutons.
      - Sur Mac Apple Silicon (M1, M2, M3, M4), le clavier et le trackpad sont reliés en direct en bus série FIFO au coprocesseur de gestion des capteurs Apple (SPU) via `AppleHIDTransportHIDDevice`.
      - Apple a compartimenté le routage des événements : le clavier (`UsagePage: 1, Usage: 6`) continue de relayer les scancodes des touches à IOKit, mais le flux du trackpad interne (`UsagePage: 1, Usage: 2`) est capté exclusivement par le sous-système multitouch interne (`MultitouchSupport` / `SkyLight` / `WindowServer`). La file IOKit `IOHIDQueueInterface` reste donc silencieuse (`kIOReturnUnderrun`) pour les applications utilisateur ordinaires.
    - **Refus strict des contournements superficiels (bricolage)** :
      - Aucun écouteur d'événements Swing synthétiques (`KeyListener`, `MouseListener`) n'est injecté dans `EventViewer` ou `EventReader`. L'intégrité de la boîte noire JInput est rigoureusement préservée.
    - **Résolution propre à la racine dans `jinput-apple-silicon`** :
      - Implémentation native C dans [`net_java_games_input_OSXMouse.c`](file:///Users/lehuen/dev/codelab/client/natives/osx_arm/jinput-apple-silicon/src/plugins/OSX/src/main/native/net_java_games_input_OSXMouse.c) avec la fonction JNI `nPollPointer` exploitant l'API native `CoreGraphics` / `ApplicationServices` (`CGEventGetLocation`, `CGGetLastMouseDelta` et `CGEventSourceButtonState`).
      - Surcharge propre de `pollDevice()` dans [`OSXMouse.java`](file:///Users/lehuen/dev/codelab/client/natives/osx_arm/jinput-apple-silicon/src/plugins/OSX/src/main/java/net/java/games/input/OSXMouse.java) : alimentation transparente de la file d'événements JInput (`Event`) avec les deltas d'axes `x`, `y` et les clics `Left`, `Right`, `Middle`.
      - Recompilation native signée `libjinput-osx.dylib` / `.jnilib` et mise à jour de `jinput-2.0.9.jar`.
      - Sécurisation : Sauvegarde intégrale des binaires et sources d'origine dans [`client/natives/osx_arm/backup_original/`](file:///Users/lehuen/dev/codelab/client/natives/osx_arm/backup_original/) pour tout rollback éventuel.

17. **Assainissement du Code, Normalisation Multiplateforme & Publication sur GitHub (Septembre 2026)** :
    - **Sécurisation Applicative du Serveur Rust** :
      - Suppression du mot de passe maître en dur (`MAGIC_PASSWORD`) : migration vers la propriété dynamique `server_magic_password` dans [`server.properties`](file:///Users/lehuen/dev/codelab/server/server/config/server.properties).
      - Externalisation des identifiants d'administration web dans [`admin.rs`](file:///Users/lehuen/dev/codelab/server/server/src/admin.rs) (`admin_username`, `admin_password_hash`).
      - Démarrage résilient dans [`main.rs`](file:///Users/lehuen/dev/codelab/server/server/src/main.rs) : basculement automatique sur les gabarits d'exemples sécurisés ([`server.properties.example`](file:///Users/lehuen/dev/codelab/server/server/config/server.properties.example) et [`sessions.example.xml`](file:///Users/lehuen/dev/codelab/server/server/config/sessions.example.xml)) lorsque les configurations réelles sont absentes.
    - **Portabilité des Scripts & Build Client** :
      - Élimination des chemins personnels absolus `/Users/lehuen/` dans l'ensemble des scripts de packaging (`distrib-mac.sh`, `distrib-linux.sh`, `distrib-win64*.sh`, `distrib-ic2.sh`) avec repli propre `${TARGET:-$HOME/Desktop}`.
      - Sécurisation de la version dans [`build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml) via `<property name="env.VERSION" value="1.4.1"/>` pour prévenir toute corruption en cas d'appel manuel à `ant`.
      - Détection autonome du JDK système (`/usr/libexec/java_home`) et d'un exécutable Ant dans le `PATH` dans [`build.sh`](file:///Users/lehuen/dev/codelab/client/build.sh).
    - **Gestion des Dépendances & Bibliothèque Jess** :
      - Conservation et suivi formel de la bibliothèque [`jess-6.1.jar`](file:///Users/lehuen/dev/codelab/client/hidden/codelab/libraries/jess-6.1.jar) dans le dépôt Git (322 Ko).
    - **Fichiers de Configuration Racine & Métadonnées Git** :
      - [`.gitignore`](file:///Users/lehuen/dev/codelab/.gitignore) : exclusion stricte des répertoires de distribution volumineux (`builds/`, `www/`), des runtimes JDK (`client/java/*_64/`, `client/mac-app-*/Contents/Java/`), des données RGPD étudiantes (`server.properties`, `sessions*.xml`, `data_*/`, `*.xlsx`, `*.eml`), et des documents de travail internes (`BILAN*`, `ROADMAP*`).
      - [`.gitattributes`](file:///Users/lehuen/dev/codelab/.gitattributes) : normalisation des fins de ligne (`eol=lf` pour Unix/Java/Rust, `eol=crlf` pour batch Windows), marquage binaire des JARs et bibliothèques natives.
      - [`.editorconfig`](file:///Users/lehuen/dev/codelab/.editorconfig) : règles d'indentation harmonisées (onglets pour Java, 4 espaces pour Rust/XML/TOML/Markdown).
    - **Calibrage des Statistiques de Langages (GitHub Linguist)** :
      - Directives `linguist-vendored` appliquées sur `client/java/**` (excluant 42 Mo de manuels/Javadoc HTML d'Apache Ant) et `client/natives/**` pour restituer les proportions réelles de développement du projet (~85% Java, ~12% Rust, ~3% Shell).
    - **Propriété Intellectuelle & Licence Officielle** :
      - Licence Institutionnelle Éducative & Non-Commerciale alignée sur le dépôt légal APP n° `IDDN-FR-001-260031-000-SC-2022-000-10000` et sur [`mentions-fr.html`](file:///Users/lehuen/dev/codelab/client/data/mentions/mentions-fr.html) : gratuité intégrale pour l'enseignement et la recherche, interdiction formelle de toute exploitation commerciale sans accord écrit préalable de l'auteur et de Le Mans Université.

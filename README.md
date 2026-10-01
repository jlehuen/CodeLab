<p align="center">
  <img src="client/icons/icone.png" alt="CodeLab Logo" width="130">
</p>

<h1 align="center">CodeLab IDE & Simulators</h1>

<div align="center">

[![Java](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://adoptium.net/)
[![Rust](https://img.shields.io/badge/Rust-2021%20Edition-red.svg)](https://www.rust-lang.org/)
[![Licence](https://img.shields.io/badge/Licence-Éducative%20%26%20Non--Commerciale-blue.svg)](LICENSE)
[![Dépôt APP](https://img.shields.io/badge/Dépôt%20APP-IDDN--FR--001--260031-green.svg)](https://www.app.asso.fr/)

</div>

<p align="center">
  <strong>Environnement d'Apprentissage de la Programmation
  <br>pour l'Enseignement Secondaire et Supérieur</strong>
  <br>
  <br>Site officiel : <a href="https://codelab.univ-lemans.fr">https://codelab.univ-lemans.fr</a>
  <br><br>
  <a href="#téléchargements-version-143"><img src="client/images/download.png" alt="Téléchargements" width="160"></a>
</p>

**CodeLab** est un environnement pédagogique dédié à l'apprentissage de la programmation en collège, lycée et premières années d'études supérieures. Son originalité est qu'il propose des alternatives ludiques ou techniques aux traditionnelles interactions écran-clavier pour la conception des activités pédagogiques, au travers de l'utilisation de "modules applicatifs". Ces modules, disponibles sous la forme de plugins, peuvent être des **visualisations**, des **panneaux de contrôle**, des **simulateurs**, et autres systèmes temps-réel. Chaque module est l'association d'une **IHM** (Interface Homme-Machine) et d'une interface de programmation applicative (souvent désignée par le terme **API** pour Application Programming Interface), disponible pour chacun des langages supportés par CodeLab. Les apprenants peuvent ainsi se focaliser sur les aspects algorithmiques et sur le codage, tout en travaillant sur des applications qui possèdent des caractéristiques riches et motivantes :

<p align="center">
  <img src="client/images/vignette-robot2D.webp" alt="Robot 2D" width="22%">&nbsp;&nbsp;
  <img src="client/images/vignette-robot3D.webp" alt="Robot 3D" width="22%">&nbsp;&nbsp;
  <img src="client/images/vignette-motor.webp" alt="Moteur" width="22%">&nbsp;&nbsp;
  <img src="client/images/vignette-sorting.webp" alt="Tri" width="22%">
</p>

CodeLab supporte un large spectre de **paradigmes de programmation** (impératif, fonctionnel, objet, déclaratif) et ce afin de répondre aux recommandations du programme de la spécialité NSI (Numérique et Sciences Informatiques) de première et de terminale. En plus des langages traditionnels, CodeLab intègre un langage "par assemblage de blocs" qui permet de découvrir les structures de programmation en s'abstrayant d'une syntaxe spécifique.

Une fonctionnalité de CodeLab est de permettre la constitution de **classes virtuelles** (en présence ou à distance) grâce à une architecture client-serveur dédiée. Le modèle utilisateur / groupe / session permet la constitution de groupes de TP à géométrie variable, encadrés par un ou plusieurs tuteurs. Ces derniers peuvent **suivre en temps réel** le travail des apprenants, tester leurs programmes, communiquer avec eux par l’intermédiaire d’une messagerie instantanée intégrée, les déconnecter en fin de séance, etc.

<p align="center">
  <img src="client/images/codelab_1.png" alt="Interface CodeLab" width="80%">
</p>

## Sommaire

- [Téléchargements](#téléchargements-version-143)
- [Plugins & Modules applicatifs](#plugins--modules-applicatifs)
- [Installation des langages](#installation-des-langages)
- [Compilation du client et du serveur](#compilation-du-client-et-du-serveur)
  - [1. Compilation du client Java](#1-compilation-du-client-java)
  - [2. Compilation du serveur Rust](#2-compilation-du-serveur-rust)
  - [3. Administration du serveur](#3-administration-du-serveur)
- [Licence & Propriété Intellectuelle](#licence--propriété-intellectuelle)

## Téléchargements (version 1.4.3)

<br>

<p align="center">
  <a href="https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-MacOS-Silicon-1.4.3.dmg"><img src="client/images/download-macos.png" alt="Télécharger pour macOS" width="150"></a>
  &emsp;
  <a href="https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Win64-1.4.3-Setup.exe"><img src="client/images/download-windows.png" alt="Télécharger pour Windows" width="150"></a>
  &emsp;
  <a href="https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Linux-1.4.3.zip"><img src="client/images/download-linux.png" alt="Télécharger pour Linux" width="150"></a>
</p>

Toutes les versions disponibles sont dans la section [Releases](https://github.com/jlehuen/CodeLab/releases) de CodeLab.

<details>
<summary>
<b>macOS : procédure d'autorisation au premier lancement (Gatekeeper)</b>
</summary>
<br>

> Les systèmes macOS bloquent au premier lancement les applications qui ne disposent pas d'un certificat payant Apple Developer. Cette opération n'est requise **qu'une seule fois** :

| 1. Installer | 2. Premier blocage | 3. Réglages Système | 4. Confirmer | 5. Mot de passe |
| :---: | :---: | :---: | :---: | :---: |
| <img src="client/images/auth_codelab_mac_1.png" alt="Étape 1 - Glisser dans Applications" height="110"> | <img src="client/images/auth_codelab_mac_2.png" alt="Étape 2 - Cliquer sur Terminé" height="110"> | <img src="client/images/auth_codelab_mac_3.png" alt="Étape 3 - Ouvrir quand même" height="110"> | <img src="client/images/auth_codelab_mac_4.png" alt="Étape 4 - Confirmer" height="110"> | <img src="client/images/auth_codelab_mac_5.png" alt="Étape 5 - Mot de passe" height="110"> |
| Glisser **CodeLab.app** dans le dossier **Applications**. | Cliquer sur **Terminé** lors du refus d'ouverture. | Dans **Réglages Système** > **Confidentialité et sécurité**, cliquer sur **Ouvrir quand même**. | Relancer CodeLab et cliquer sur **Ouvrir quand même**. | Entrer votre mot de passe administrateur pour valider. |

> 💡 **Alternative en ligne de commande** : vous pouvez aussi lever le blocage dans le Terminal avec la commande `xattr -cr /Applications/CodeLab.app` ou encore utiliser l'application gratuite [Sentinel](https://github.com/alienator88/Sentinel) disponible sur GitHub.

</details>

<details>
<summary>
<b>Windows : procédure d'autorisation de l'installeur (SmartScreen)</b>
</summary>
<br>

> Microsoft Defender SmartScreen bloque par défaut le lancement des installateurs téléchargés ne disposant pas d'un certificat commercial Microsoft Authenticode. Cette validation n'est requise **qu'une seule fois à l'installation** (aucun droit administrateur n'est requis) :

| Instructions | &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;Aperçu&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; |
| :--- | :---: |
| 1. Sur l'écran *« Windows a protégé votre ordinateur »*, cliquez sur le lien **Informations complémentaires**.<br><br>2. Cliquez ensuite sur le bouton **Exécuter quand même** qui apparaît en bas.<br><br>3. L'installation s'exécute dans votre session utilisateur ; par la suite, le raccourci du menu Démarrer lancera CodeLab directement, sans aucun avertissement. | <img src="client/images/auth_codelab_win.png" alt="Autorisation Windows SmartScreen" width="350"> |

</details>

## Plugins & Modules applicatifs

CodeLab s'enrichit grâce à des **modules applicatifs** téléchargeables sous forme de plugins (`.pac`). Chaque module propose une interface graphique dédiée et une API multi-langages (C, Go, Java, Python, CLIPS, etc.) pour concevoir des activités motivantes (robotique 2D/3D, asservissement moteur, visualisation d'algorithmes de tri, etc.).

Le catalogue complet, les archives sources (`.zip`), les paquets précompilés (`.pac`) et le guide de création de nouveaux modules avec le CDK sont disponibles dans le dossier [plugins/](plugins/) :

| Module Applicatif | Auteur | Code source | Exécutable (`.pac`) |
| :--- | :---: | :---: | :---: |
| **Hello World** (initiation premier module) | CodeLab Team | [HelloModule.zip](plugins/downloads/HelloModule.zip) | [HelloModule.pac](plugins/downloads/HelloModule.pac) |
| **Tracé 2D** (dessin géométrique vectoriel) | CodeLab Team | [DrawingModule.zip](plugins/downloads/DrawingModule.zip) | [DrawingModule.pac](plugins/downloads/DrawingModule.pac) |
| **Moteur NXT** (simulateur & asservissement) | CodeLab Team | [MotorModule.zip](plugins/downloads/MotorModule.zip) | [MotorModule.pac](plugins/downloads/MotorModule.pac) |
| **Laboratoire de tris** (visualisation bubble/quick sort) | CodeLab Team | [SortingModule.zip](plugins/downloads/SortingModule.zip) | [SortingModule.pac](plugins/downloads/SortingModule.pac) |
| **Atelier graphique** (tortue vectorielle Logo) | CodeLab Team | [TurtleModule.zip](plugins/downloads/TurtleModule.zip) | [TurtleModule.pac](plugins/downloads/TurtleModule.pac) |
| **Robot 3D** (bras articulé 5 axes cinématique 3D) | CodeLab Team | [Robot3DModule.zip](plugins/downloads/Robot3DModule.zip) | [Robot3DModule.pac](plugins/downloads/Robot3DModule.pac) |

> 📖 Consultez le [manuel des plugins](plugins/README.md) pour les instructions détaillées d'installation, de compilation et de création via le CDK (*CodeLab Development Kit*).

## Installation des langages

CodeLab prend en charge un certain nombre de langages : certains sont intégrés à CodeLab (Java, Processing, CLIPS), certains sont peut-être déjà installés sur votre machine (comme C ou Python), d'autre seront à installer. Lorsque vous lancez CodeLab, la console affiche un état des langages installés ou non :

```
[codelab] Vérification du langage C...OK -> Apple clang version 21.0.0 (clang-2100.1.1.101)
[codelab] Vérification du langage Go...indisponible
[codelab] Vérification du langage Java...OK -> javac 17.0.20.1
[codelab] Vérification du langage Python...OK -> Python 3.13.15
[codelab] Vérification de la librairie Numpy...OK -> Numpy version 2.3.0
[codelab] Vérification de la librairie Matplotlib...OK -> Matplotlib version 3.10.3
[codelab] Vérification du langage Processing...OK -> Processing version 4.3
[codelab] Vérification du langage Haskell...indisponible
[codelab] Vérification du langage CLIPS...OK -> CLIPS version 6.31 - CodeLab build (04/12/23)
```

Il se peut que votre PATH ne comporte pas les bons chemins ou qu'un chemin soit absent. Vous pouvez demander à CodeLab de rechercher le chemin d'un langage via l'item "Rechercher un langage" du menu Configuration. Enfin, il se peut que vous ayez besoin d'installer un langage absent de votre machine :

<details>
<summary><b>Installer un compilateur C</b></summary>
<br>

>Coming soon !
</details>

<details>
<summary><b>Installer Python + Numpy + Matplotlib</b></summary>
<br>

>Un environnement virtuel Python (venv) est un dossier isolé où l’on peut installer des packages et exécuter du code Python sans interférer avec les autres distributions Python et projets du système. Commencez par installer une distribution récente de Python sur votre machine (si ce n'est pas déjà la cas) en allant sur le [site officiel](https://www.python.org) de Python.

>Pour créer un environnement virtuel sous Linux / MacOS :

```bash
$ cd
$ python -m venv my_python
$ source my_python/bin/activate
(my_python) $ pip install --upgrade pip
(my_python) $ pip install numpy matplotlib
```

>Pour créer un environnement virtuel sous Windows :

```bash
$ cd ~
$ python -m venv my_python
$ .\my_python\Scripts\Activate.ps1
(my_python) $ pip install --upgrade pip
(my_python) $ pip install numpy matplotlib
```

>Ensuite, configurez CodeLab via le menu Configuration > Rechercher un langage > Interpréteur Python.


>Si lors d'une exécution, vous obtenez l'erreur **FigureCancasAgg is non-interactive, and thus cannot be shown** c'est que la librairie graphique Tk est manquante. Tapez ces lignes dans un terminal et relancez CodeLab :

```bash
sudo apt update
sudo apt install python3-tk
```
>Puis forcez au besoin le backend **TkAgg** au début de votre script :

```bash
import matplotlib
matplotlib.use('TkAgg')
import matplotlib.pyplot as plt
```
</details>

## Compilation du client et du serveur

### Architecture du projet

```text
codelab/
├── client/                     # Application cliente Java
│   ├── src/                    # Sources Java (package codelab)
│   │   ├── client/             # Protocole réseau client-serveur (MessagePack)
│   │   ├── controllers/        # Gestion des périphériques (JInput, joysticks, etc.)
│   │   ├── modules/            # Modules interactifs et simulateurs PAC
│   │   └── utils/              # Cryptographie, parseurs, compression Tar/Zstd
│   ├── data/                   # Ressources graphiques, sons, syntaxes de langages
│   ├── hidden/codelab/         # Bibliothèques tierces (FlatLaf, JInput, etc.)
│   ├── natives/                # Bibliothèques natives C/C++ par OS (.dylib, .so, .dll)
│   ├── build.xml               # Fichier de build Apache Ant
│   ├── build.sh                # Script de compilation local
│   └── distrib-*.sh            # Scripts de packaging (macOS, Linux, Windows)
│
├── plugins/                    # Modules applicatifs & simulateurs téléchargeables
│   ├── downloads/              # Exécutables précompilés (.pac) et sources (.zip)
│   └── README.md               # Catalogue, installation et guide CDK
│
├── server/                     # Serveur d'infrastructure
│   ├── server/                 # Serveur asynchrone Rust (Tokio)
│   │   ├── src/                # Code source Rust (main, database, network, admin)
│   │   ├── config/             # Gabarits de configuration (server.properties, sessions.xml)
│   │   └── Cargo.toml          # Dépendances et métadonnées Cargo
│   ├── run-server.sh           # Script de démarrage du service
│   └── kill-server.sh          # Script d'arrêt propre du service
│
├── VERSION                     # Source unique du numéro de version
├── .gitignore
├── .gitattributes
├── .editorconfig
├── LICENSE
└── README.md
```

### 1. Compilation du client Java

#### Prérequis pour la compilation
- **Java Development Kit (JDK) 17 LTS** installé et configuré (`JAVA_HOME`)
- **Apache Ant 1.10+** (ou utilisation de l'exécutable Ant présent dans `client/java/`)

#### Compilation
```bash
cd client
./build.sh
```
Le binaire résultant est généré dans `client/hidden/codelab/codelab.jar`.

#### Génération des distributions

> [!NOTE]
> **Pourquoi les runtimes JDK ne sont-ils pas inclus dans le dépôt Git ?**  
> Les environnements d'exécution Java complets dépassent la limite de taille par fichier imposée par GitHub (fichiers internes `lib/modules` > 100 Mo) et alourdiraient le dépôt de plus de 1,5 Go. Ils sont donc volontairement exclus via `.gitignore`.

Pour construire les distributions autonomes prêtes à l'emploi (embarquant leur propre JVM sans dépendance pour l'utilisateur final), téléchargez au préalable les archives **JDK 17 LTS** officielles sur [Adoptium Temurin Releases](https://adoptium.net/fr/temurin/releases/?version=17) et décompressez-les dans les dossiers correspondants :

| Plateforme | Archive officielle Adoptium | Format | Dossier cible dans `client/` |
| :--- | :--- | :--- | :--- |
| <img src="client/images/macos.png" alt="macOS" height="18" valign="middle"> **Mac Silicon** | [macOS aarch64 (JDK 17)](https://adoptium.net/fr/temurin/releases/?version=17&os=mac&arch=aarch64&package=jdk) | `.tar.gz` | `mac-app-arm/Contents/Java/jdk-17.0.20.1+1/` |
| <img src="client/images/macos.png" alt="macOS" height="18" valign="middle"> **Mac Intel** | [macOS x64 (JDK 17)](https://adoptium.net/fr/temurin/releases/?version=17&os=mac&arch=x64&package=jdk) | `.tar.gz` | `mac-app-x64/Contents/Java/jdk-17.0.8.1+1/` |
| <img src="client/images/windows.png" alt="Windows" height="18" valign="middle"> **Windows** | [Windows x64 (JDK 17)](https://adoptium.net/fr/temurin/releases/?version=17&os=windows&arch=x64&package=jdk) | `.zip` | `java/windows_64/JDK-17.0.8.1+1/` |
| <img src="client/images/linux.png" alt="Linux" height="18" valign="middle"> **Linux** | [Linux x64 (JDK 17)](https://adoptium.net/fr/temurin/releases/?version=17&os=linux&arch=x64&package=jdk) | `.tar.gz` | `java/linux_64/JDK-17.0.8.1+1/` |

> [!TIP]
> Si vous téléchargez une mise à jour mineure plus récente de Temurin 17 (ex: `jdk-17.0.14+7`), renommez simplement le dossier extrait avec le nom attendu dans le tableau ci-dessus (ou créez un lien symbolique) afin que les lanceurs et scripts de packaging le détectent automatiquement.

Une fois les JDK placés, lancez le script correspondant à votre cible :
- **macOS** (DMG universel Apple Silicon & Intel) : `./distrib-mac.sh`
- **Linux** (archive autonome FreeDesktop) : `./distrib-linux.sh`
- **Windows** (Installateur NSIS) : `./distrib-win64-nsis.sh`

### 2. Compilation du serveur Rust

Le serveur CodeLab orchestre les classes virtuelles, gère les flux bidirectionnels entre apprenants et enseignants et fournit un tableau de bord web de supervision.

#### Architecture & Caractéristiques Techniques
- **Modèle Acteur Asynchrone (Tokio)** : Conçu pour absorber simultanément des centaines de sessions à très faible empreinte mémoire, avec arrêt gracieux (*graceful shutdown*).
- **Protocole Binaire Optimisé** : Échanges réseau compacts et rapides sérialisés au format **MessagePack** sur socket TCP.
- **Tableau de Bord d'Administration Web** : Interface intégrée accessible par navigateur (HTTP Basic) permettant le suivi en temps réel des apprenants connectés, l'inspection des groupes et la déconnexion à distance.
- **Persistance & Sécurité** : Définition déclarative des sessions, tuteurs et élèves en XML (`sessions.xml`), et stockage isolé des espaces de travail étudiants sur le système de fichiers.
- **Sauvegardes Automatiques** : Système de rotation de backups horodatés avec compression haute performance **TAR + Zstandard** (`.tar.zst`).

#### Prérequis pour la compilation
- Toolchain [Rust & Cargo](https://rustup.rs/) (édition 2021 stable)

#### Configuration initiale
Créez vos fichiers de configuration locaux à partir des gabarits d'exemple fournis :
```bash
cd server/server
cp config/server.properties.example config/server.properties
cp config/sessions.example.xml config/sessions.xml
```

*Remarque : Par sécurité et conformité RGPD, les fichiers réels `server.properties` et `sessions.xml` sont strictement ignorés par Git.*

#### Démarrage du Serveur

##### Mode Développement
```bash
cd server/server
cargo run
```

##### Mode Production (Binaire optimisé)
```bash
cd server/server
cargo build --release
./target/release/server
```

Le serveur écoute sur les adresses configurées dans `server.properties` :
- **Port TCP (ex: 9988)** : Connexions réseau des clients élèves et enseignants.
- **Port HTTP (ex: 9989)** : Interface web d'administration (`http://127.0.0.1:9989`). Les identifiants sont définis dans `server.properties` (identifiant `admin` et empreinte SHA-256 du mot de passe).

Des scripts de service sont également disponibles dans le dossier `server/` :
- `./run-server.sh` : Démarrage du serveur en arrière-plan.
- `./kill-server.sh` : Arrêt propre du processus serveur.

### 3. Administration du serveur

Le serveur CodeLab intègre nativement une console web d'administration et des outils automatisés pour préparer et piloter les classes virtuelles sans dépendance externe.

#### Tableau de bord d'administration Web
- **Accès** : `http://localhost:9989` *(ou adresse IP du serveur)*.
- **Authentification** : HTTP Basic (compte défini dans `server.properties` avec mot de passe haché en SHA-256).
- **Fonctionnalités clés** :
  - **Supervision en temps réel** : visualisation des étudiants et tuteurs connectés, de leur adresse IP, de leur temps de travail et de la session rejointe.
  - **Pilotage des sessions** : ouverture et fermeture des accès aux sessions de TP en un clic.
  - **Changement de groupe à chaud** : transfert immédiat d'un étudiant d'un groupe à un autre en mémoire vive sans coupure ni perte de code.
  - **Contrôle et déconnexion forcée** des sessions bloquées.

#### Préparation des utilisateurs et des sessions (`sessions.xml`)
La liste des groupes, sessions et utilisateurs autorisés est déclarée dans `sessions.xml` (validé par `sessions.dtd`). Pour convertir automatiquement vos listes d'étudiants (fichiers `.xlsx` ou `.csv`) en fragments XML prêts à être insérés :
- **macOS / Linux** : lancez `./build-xml.sh` dans `server/server/config/data/` (mode interactif).
- **Windows** : glissez-déposez simplement votre fichier `users.xlsx` sur l'icône de `build-xml.bat`.
- **Multiplateforme** : `python build-xml.py [source.xlsx|csv]`.
- **Validation DTD** : vérifiez la conformité de votre fichier via `./xml-validator.sh sessions.xml`.

#### Scripts de service
Depuis le dossier `server/` :
- `./run-server.sh` : démarrage du serveur en tâche de fond avec contrôle de port.
- `./kill-server.sh` : arrêt propre du service avec finalisation des écritures sur disque et déconnexion ordonnée des clients.

> 📖 **Documentation complète** : Pour les détails techniques avancés (architecture asynchrone Tokio, configuration SMTP des alertes, gestion des timeouts, rotation automatique des sauvegardes et conformité RGPD), consultez le [manuel complet du serveur](server/README.md).

## Licence & Propriété Intellectuelle
 
- **Auteur** : Jérôme Lehuen, Maître de Conférences à [Le Mans Université](https://www.univ-lemans.fr).
- **Conditions d'utilisation** : Ce logiciel (code source et binaires) est mis à disposition gratuitement à des fins éducatives, académiques et de recherche non commerciale. **Toute utilisation ou exploitation commerciale, revente ou sous-licence est strictement interdite** sans accord préalable écrit de l'auteur et de Le Mans Université.
- **Dépôt légal** : CodeLab est déposé auprès de l'[Agence pour la Protection des Programmes (APP)](https://www.app.asso.fr/) sous la référence IDDN-FR-001-260031-000-SC-2022-000-10000.
- **Licence** : Consultez le fichier [LICENSE](LICENSE) pour l'intégralité des termes et des mentions légales.
- **Copyright** : © 2021-2026 Jérôme Lehuen, Le Mans Université.

<p align="center">
  <img src="client/data/mentions/app.png" alt="app.png">
  <br/>IDDN-FR-001-260031-000-SC-2022-000-10000
</p>

<p align="center">
  <br>
  <img src="client/data/mentions/separator.png" alt="separator.png">
</p>

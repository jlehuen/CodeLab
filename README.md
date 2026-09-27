<p align="center">
  <img src="client/icons/icone.png" alt="CodeLab Logo" width="130">
</p>

<h1 align="center">CodeLab IDE & Simulators</h1>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/Licence-Éducative%20%26%20Non--Commerciale-blue.svg" alt="Licence: Éducative & Non-Commerciale"></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-17%20LTS-orange.svg" alt="Java: 17"></a>
  <a href="https://www.rust-lang.org/"><img src="https://img.shields.io/badge/Rust-2021%20Edition-red.svg" alt="Rust: 2021"></a>
  <a href="https://www.app.asso.fr/"><img src="https://img.shields.io/badge/Dépôt%20APP-IDDN--FR--001--260031-green.svg" alt="Dépôt APP"></a>
</p>

<p align="center">
  <strong>Environnement d'apprentissage de la programmation pour l'enseignement secondaire et supérieur</strong><br>
  Site officiel : <a href="https://codelab.univ-lemans.fr">https://codelab.univ-lemans.fr</a>
</p>

**CodeLab** est un environnement d'apprentissage de la programmation destiné aux collèges, lycées (NSI), classes préparatoires et premières années d'université (L1/L2). Conçu à [Le Mans Université](https://www.univ-lemans.fr), il associe un **client riche multiplateforme (Java 17 Swing / FlatLaf)** à un **serveur asynchrone haute performance (Rust / Tokio)** pour offrir une expérience pédagogique interactive, visuelle et supervisée en temps réel.

<p align="center">
  <img src="client/images/screen_1.png" alt="screen_1.png">
  <img src="client/images/screen_2.png" alt="screen_2.png">
</p>

## Philosophie & Approche Pédagogique

- **Sortir du strict écran-clavier** : Rompre avec les exercices textuels austères en connectant le code à des simulateurs interactifs riches (modules PAC : robotique, ascenseur, feux tricolores, etc.) et à des périphériques matériels réels ou virtuels (capteurs, joysticks, manettes USB via JInput).
- **Fondements théoriques** : Inspiré par les travaux de Jean Piaget, Marvin Minsky et le constructionnisme de Seymour Papert (créateur du langage Logo et pionnier de Lego Mindstorms).
- **Multi-paradigmes** :
  - **Impératif & Système** : C, Go
  - **Orienté Objet** : Java
  - **Scripting & Multiparadigme** : Python
  - **Déclaratif / Systèmes experts** : CLIPS
  - **Fonctionnel** : Haskell
  - **Graphique & Créatif** : Processing
  - **Visuel** : Programmation par Blocs traduisibles en Python
- **Mode classe virtuelle & supervision temps réel** : Suivi synchrone des travaux d'étudiants par les enseignants/tuteurs, téléguidage, télé-évaluation et assistance interactive.
- **Zéro configuration pour l'apprenant** : Possibilité de distribuer des paquets autonomes embarquant leur propre environnement d'exécution (JDK, interpréteurs et bibliothèques natives).

---

## Architecture du Répertoire

```text
codelab/
├── client/                     # Application cliente Java 17 Swing
│   ├── src/                    # Sources Java (package racine: codelab)
│   │   ├── client/             # Protocole réseau client-serveur (MessagePack)
│   │   ├── controllers/        # Gestion des périphériques (JInput, joysticks, etc.)
│   │   ├── modules/            # Modules interactifs et simulateurs PAC
│   │   └── utils/              # Cryptographie, parseurs, compression Tar/Zstd
│   ├── data/                   # Ressources graphiques, sons, syntaxes de langages
│   ├── hidden/codelab/         # Bibliothèques tierces (FlatLaf, JInput, etc.)
│   ├── natives/                # Bibliothèques natives C/C++ par OS (.dylib, .so, .dll)
│   ├── build.xml               # Fichier de build Apache Ant
│   ├── build.sh                # Script de compilation local
│   └── distrib-*.sh            # Scripts de packaging (macOS, Linux, Windows NSIS)
│
├── server/                     # Serveur d'infrastructure
│   ├── server/                 # Serveur asynchrone Rust (Tokio)
│   │   ├── src/                # Code source Rust (main, database, network, admin)
│   │   ├── config/             # Gabarits de configuration (server.properties, sessions.xml)
│   │   └── Cargo.toml          # Dépendances et métadonnées Cargo
│   └── scripts-server/         # Scripts de service et d'exploitation
│
├── .gitignore                  # Règles d'exclusion Git (RGPD, secrets, binaires > 100 Mo)
├── .gitattributes              # Normalisation des fins de ligne (LF/CRLF) et binaires
├── .editorconfig               # Règles d'indentation et d'encodage
├── LICENSE                     # Licence GNU General Public License v3.0
└── README.md                   # Ce document
```

---

## Démarrage Rapide & Compilation

### 1. Client Java

#### Prérequis
- Java Development Kit (JDK) 17 LTS installé et configuré (`JAVA_HOME`)
- Apache Ant 1.10+ (ou utilisation de l'exécutable Ant détecté automatiquement)

#### Compilation
```bash
cd client
./build.sh
```
Le binaire résultant est généré dans `client/hidden/codelab/codelab.jar`.

#### Génération des paquets de distribution
Des scripts automatisés permettent de construire les distributions pour chaque OS :
- **macOS** (DMG universel Apple Silicon & Intel) : `./distrib-mac.sh`
- **Linux** (archive autonome FreeDesktop) : `./distrib-linux.sh`
- **Windows** (Installateur NSIS MUI2 par utilisateur) : `./distrib-win64-nsis.sh`

---

### 2. Serveur Asynchrone Rust

Le serveur CodeLab prend en charge les connexions simultanées des clients élèves et tuteurs, la sérialisation des échanges en MessagePack et l'administration web.

#### Prérequis
- Toolchain [Rust & Cargo](https://rustup.rs/) (édition 2021 stable)

#### Configuration initiale
Créez vos fichiers de configuration locaux à partir des gabarits d'exemple fournis :
```bash
cd server/server
cp config/server.properties.example config/server.properties
cp config/sessions.example.xml config/sessions.xml
```

*Remarque : Par sécurité et conformité RGPD, les fichiers réels `server.properties` et `sessions.xml` sont strictement ignorés par Git.*

#### Lancement en développement
```bash
cargo run
```
Le serveur démarre son écoute TCP sur le port configuré (par exemple `127.0.0.1:9988`) et son interface d'administration HTTP sur `127.0.0.1:9989`.

---

## Sécurité & Protection des Données (RGPD)

- **Confidentialité** : Les données nominatives étudiantes, listes de promotions, traces d'exécution et identifiants SMTP de production sont systématiquement isolés et exclus du dépôt public.
- **Authentification** : Mots de passe chiffrés par empreinte SHA-256 (compatible avec le format standard `echo -n "votre_mdp" | sha256sum | xxd -r -p | base64`).
- **Résilience** : Le serveur intègre un mode de repli automatique sur ses gabarits d'exemple si les configurations personnalisées ne sont pas présentes au démarrage.

---

## Propriété Intellectuelle & Licence
 
- **Auteur** : Jérôme Lehuen, Maître de Conférences, Le Mans Université (LIUM).
- **Dépôt légal** : Le logiciel *CodeLab IDE & Simulators* est enregistré auprès de l'[Agence pour la Protection des Programmes (APP)](https://www.app.asso.fr/) sous le numéro :

<p align="center">
  <img src="client/data/mentions/app.png" alt="app.png">
  <br/>IDDN-FR-001-260031-000-SC-2022-000-10000
</p>

- **Licence & Conditions d'utilisation** : Ce logiciel (code source et binaires) est mis à disposition gratuitement à des fins éducatives, académiques et de recherche non commerciale. **Toute utilisation ou exploitation commerciale, revente ou sous-licence est strictement interdite** sans accord préalable écrit de l'auteur et de Le Mans Université. Consultez le fichier [LICENSE](LICENSE) pour l'intégralité des termes et des mentions légales.
- **Documentation** : La documentation, les guides et les programmes d'exemples sont mis à disposition sous licence [Creative Commons CC-BY-NC-ND](https://creativecommons.org/licenses/by-nc-nd/4.0/)
- **Copyright** : © 2021-2026 Jérôme Lehuen, Le Mans Université.


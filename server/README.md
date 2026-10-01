# Serveur CodeLab

Le **serveur CodeLab** est un service réseau haute performance écrit en **Rust**. Il assure la synchronisation en temps réel du code des étudiants, la supervision pédagogique par les tuteurs et la gestion des sessions d'enseignement pour l'environnement CodeLab.

---

## Sommaire

1. [Vue d'ensemble et Architecture](#1-vue-densemble-et-architecture)
2. [Prérequis et Installation](#2-prérequis-et-installation)
3. [Configuration du Serveur (`server.properties`)](#3-configuration-du-serveur-serverproperties)
4. [Gestion des Utilisateurs et des Sessions (`sessions.xml`)](#4-gestion-des-utilisateurs-et-des-sessions-sessionsxml)
   - [Structure de données (DTD)](#structure-de-données-dtd)
   - [Génération depuis Excel/CSV avec `build-xml`](#génération-depuis-excelcsv-avec-build-xml)
   - [Validation formelle du fichier XML](#validation-formelle-du-fichier-xml)
5. [Compilation et Démarrage](#5-compilation-et-démarrage)
   - [Compilation standard Cargo](#compilation-standard-cargo)
   - [Lancement et arrêt](#lancement-et-arrêt)
6. [Tableau de Bord d'Administration Web](#6-tableau-de-bord-dadministration-web)
7. [Sauvegardes, Logs et Sécurité RGPD](#7-sauvegardes-logs-et-sécurité-rgpd)

---

## 1. Vue d'ensemble et Architecture

Le serveur repose sur le moteur asynchrone **Tokio** (modèle multi-threadé avec vol de travail) et héberge deux services distincts :

```
                  ┌─────────────────────────────────────────┐
                  │          Serveur Rust CodeLab           │
                  ├────────────────────┬────────────────────┤
                  │  Service TCP       │  Interface Web     │
                  │  (Port 9988)       │  (Port 9989)       │
                  └─────────┬──────────┴─────────┬──────────┘
                            │                    │
                MessagePack │ TAR+Zstd           │ HTTP / HTML5
                            ▼                    ▼
                 Clients Java CodeLab       Navigateurs Enseignants
               (Étudiants & Tuteurs)         (Tableau de bord Admin)
```

- **Service applicatif TCP (port `9988`)** :
  - Protocole binaire compact via **MessagePack** (`rmp-serde`) pour les messages d'état et le contrôle distant.
  - Transfert atomique des projets sous forme d'archives compressées **TAR + ZStandard** (`tar` et `zstd`).
  - Hachage sécurisé des identifiants par **SHA-256**.
- **Service d'administration HTTP (port `9989`)** :
  - Serveur web autonome intégré (aucun serveur web externe requis).
  - Authentification HTTP Basic avec hash SHA-256.
  - Tableau de bord en temps réel (HTML5 / Flat UI) pour surveiller les connexions, fermer/ouvrir des sessions et déplacer des étudiants de groupe à la volée.

---

## 2. Prérequis et Installation

### Installation de Rust

Le serveur requiert un compilateur Rust récent (édition 2021). Si Rust n'est pas installé sur votre machine :

```bash
# Installation de Rust via rustup (macOS / Linux)
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Rechargement de l'environnement courant
source "$HOME/.cargo/env"
```

Sous **Windows**, téléchargez et exécutez l'installateur officiel [rustup-init.exe](https://rustup.rs/).

### Configuration d'un proxy réseau (le cas échéant)

Si vous opérez derrière un proxy institutionnel (par exemple en réseau universitaire) :

```bash
export HTTP_PROXY="proxy.univ-lemans.fr:3128"
export HTTPS_PROXY="proxy.univ-lemans.fr:3128"
```

---

## 3. Configuration du Serveur (`server.properties`)

Le fichier de configuration est stocké dans `server/config/server.properties`. 

Lors de la première installation, copiez le fichier d'exemple [server.properties.example](file:///Users/lehuen/dev/codelab/server/server/config/server.properties.example) :

```bash
cd server/config
cp server.properties.example server.properties
```

### Exemple de configuration commenté

```toml
[server]
name = "Serveur CodeLab Local"
# Écoute locale ("127.0.0.1:9988") ou sur toutes les interfaces ("0.0.0.0:9988")
addr = "127.0.0.1:9988"

# Mot de passe maître optionnel pour maintenance (laisser commenté si inactif)
# Génération : echo -n "mon_pass" | sha256sum | xxd -r -p | base64
# magic_password = ""

[admin]
# Port d'écoute de l'interface web d'administration
addr = "127.0.0.1:9989"
username = "admin"

# Hash SHA-256 du mot de passe admin (par défaut ci-dessous : "admin")
# Pour générer votre hash : echo -n "votre_mot_de_passe" | sha256sum
password_hash = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"

[timeout]
read = 30        # Timeout de lecture en secondes
write = 10       # Timeout d'écriture en secondes

[cleanup]
phantom_interval = 60   # Intervalle de vérification des sockets fantômes (sec)
socket_timeout = 2      # Timeout de vérification d'une socket (sec)

[report]
status    = "DISABLED"  # "ENABLED" pour activer l'envoi de rapports par email
server    = "smtp.univ-lemans.fr:587"
username  = "codelab"
password  = "secret"
from      = "codelab@univ-lemans.fr"
to        = "admin@univ-lemans.fr"
```

> [!TIP]
> Si `server.properties` est absent lors du démarrage, le serveur se replie automatiquement sur les valeurs définies dans [server.properties.example](file:///Users/lehuen/dev/codelab/server/server/config/server.properties.example).

---

## 4. Gestion des Utilisateurs et des Sessions (`sessions.xml`)

Le fichier `server/config/sessions.xml` constitue la base de référence du serveur. Il définit les **groupes de TP**, les **sessions de cours** et les **comptes utilisateurs** autorisés.

### Structure de données (DTD)

La grammaire formelle est définie dans [sessions.dtd](file:///Users/lehuen/dev/codelab/server/server/config/sessions.dtd) :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<data info="Base de test CodeLab">

  <!-- 1. Déclaration des groupes de TD/TP -->
  <groups>
    <group id="Group_1"/>
    <group id="Group_2"/>
  </groups>

  <!-- 2. Déclaration des sessions de travail -->
  <sessions>
    <!-- Une session associe des groupes autorisés et des tuteurs responsables -->
    <session id="TP_Algo_1" openned="false" groups="Group_1" users="lehuen lemeunier"/>
    <session id="TP_Algo_2" openned="false" groups="Group_2" users="lehuen lemeunier"/>
  </sessions>

  <!-- 3. Comptes utilisateurs (enseignants et étudiants) -->
  <users>
    <!-- Tuteurs / Administrateurs -->
    <user login="lehuen" passwd="secret" status="TUTOR" name="Jérôme Lehuen"/>
    <user login="lemeunier" passwd="secret" status="TUTOR" name="Thierry Lemeunier"/>

    <!-- Étudiants rattachés à des groupes -->
    <user login="test01" passwd="test01" status="STUDENT" groups="Group_1" name="James Griffin" mail="James.Griffin@univ-lemans.fr"/>
    <user login="test04" passwd="test04" status="STUDENT" groups="Group_1 Group_2" name="Katherine Scott"/>
  </users>

</data>
```

- **`status`** : `STUDENT`, `TUTOR` ou `ADMIN`.
- **`groups`** : un ou plusieurs identifiants de groupe séparés par des espaces.
- **`openned`** : booléen (`true` ou `false`) indiquant si la session accepte actuellement les connexions étudiantes.

---

### Génération depuis Excel/CSV avec `build-xml`

Pour éviter de saisir manuellement les listes d'étudiants, le dossier [server/config/data/](file:///Users/lehuen/dev/codelab/server/server/config/data/) met à disposition des outils automatisés :

- **Sous macOS / Linux** : [build-xml.sh](file:///Users/lehuen/dev/codelab/server/server/config/data/build-xml.sh)
- **Sous Windows** : [build-xml.bat](file:///Users/lehuen/dev/codelab/server/server/config/data/build-xml.bat)
- **Multiplateforme (Python 3)** : [build-xml.py](file:///Users/lehuen/dev/codelab/server/server/config/data/build-xml.py)

#### Utilisation sous macOS / Linux :

```bash
cd server/config/data

# Mode interactif (détecte automatiquement les fichiers .xlsx et .csv)
./build-xml.sh

# Ou en passant directement le fichier d'entrée
./build-xml.sh users.xlsx
```

#### Utilisation sous Windows :

- **Glisser-déposer** : Glissez votre fichier `users.xlsx` ou `users.csv` directement sur l'icône de `build-xml.bat` dans l'Explorateur de fichiers.
- **Double-clic** : Double-cliquez sur `build-xml.bat` pour ouvrir le menu interactif (la fenêtre reste ouverte à la fin pour consulter le bilan).
- **Ligne de commande** :
  ```cmd
  build-xml.bat users.xlsx
  ```

Le script produit un fichier `.xml` (ex. `users.xml`) contenant les balises `<user .../>` correctement échappées et formatées. Il ne reste plus qu'à copier ces balises dans la section `<users>` de votre `sessions.xml`.

---

### Validation formelle du fichier XML

Pour vérifier que votre fichier `sessions.xml` respecte scrupuleusement la DTD :

```bash
cd server/config
./xml-validator.sh sessions.xml
```

Si le fichier est valide, le script affiche :
```
Le fichier sessions.xml est valide
```

---

## 5. Compilation et Démarrage

### Compilation standard Cargo

Placez-vous dans le répertoire du projet Rust (`server/server/`) :

```bash
cd server/server

# Compilation en mode Debug (développement rapide)
cargo build

# Compilation optimisée en mode Release (production)
cargo build --release
```

Le binaire exécutable généré est situé dans `server/server/target/release/codelab-server`.

---

### Lancement et arrêt

Deux scripts de gestion sont fournis à la racine du dossier `server/` :

#### Lancer le serveur :
```bash
cd server
./run-server.sh
```
Ce script vérifie si le port d'écoute est déjà occupé et lance le serveur en tâche de fond.

#### Arrêter le serveur :
```bash
cd server
./kill-server.sh
```
Ce script termine proprement le processus serveur associé au port d'écoute configuré.

> [!NOTE]
> Le serveur intègre la capture des signaux POSIX (`SIGINT`, `SIGTERM`). Lorsqu'il reçoit un signal d'arrêt, il déconnecte proprement les clients actifs, vide les buffers réseau et finalise les écritures sur disque.

---

## 6. Tableau de Bord d'Administration Web

Lorsque le serveur est actif, connectez-vous avec votre navigateur web sur :

```
http://localhost:9989
```
*(ou remplacez `localhost` par l'adresse IP du serveur distant)*.

Une boîte de dialogue d'authentification HTTP Basic s'affiche. Saisissez les identifiants configurés dans la section `[admin]` de `server.properties` (par défaut : `admin` / `admin`).

### Fonctionnalités du tableau de bord

- **Supervision en temps réel** :
  - Liste de tous les étudiants et tuteurs actuellement connectés.
  - Adresse IP d'origine, durée de connexion et session rejointe.
- **Gestion dynamique des sessions** :
  - Ouvrir ou fermer l'accès à une session d'un simple clic (les étudiants ne peuvent se connecter que si la session est ouverte).
- **Changement de groupe à chaud** :
  - Possibilité de déplacer un étudiant d'un groupe à un autre en mémoire sans avoir à redémarrer le serveur ni à interrompre son travail.
- **Déconnexion forcée** :
  - Permet d'éjecter une socket ou une session en cas de blocage client.

---

## 7. Sauvegardes, Logs et Sécurité RGPD

### Sauvegardes automatiques

Le serveur crée automatiquement des instantanés horodatés des projets étudiants lors des événements clés (sauvegarde à chaque compilation réussie, archivage à la déconnexion). Ces sauvegardes sont stockées dans :
```
server/server/sessions/
server/server/backups/
```
Un mécanisme de rotation automatique purge les fichiers les plus anciens afin de préserver l'espace disque.

### Consultation des logs

Les traces d'exécution et les diagnostics détaillés sont enregistrés dans :
```
server/server/logs/server.log
```
Le niveau de verbosité et la rotation des journaux sont gérés via le moteur `log4rs`.

### Bonnes pratiques & RGPD

> [!IMPORTANT]
> Les listes contenant des noms réels, matricules ou emails institutionnels d'étudiants relèvent de la réglementation RGPD.
> - Le fichier `.gitignore` du projet est configuré pour **exclure automatiquement** les dossiers contenant des données sensibles (notamment `server/config/data_*/` et les logs).
> - Seul le dossier [server/config/data/](file:///Users/lehuen/dev/codelab/server/server/config/data/) avec des étudiants fictifs (`test01`, `test02`...) doit être conservé dans le dépôt public à titre d'exemple et de test.

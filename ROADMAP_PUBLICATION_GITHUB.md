# Feuille de route : Préparation, Assainissement et Publication de CodeLab sur GitHub

Ce document constitue le plan d'action de référence pour préparer le projet **CodeLab** (client Java Swing et serveur asynchrone Rust) à une publication publique sur GitHub. Il fusionne la gestion de l'arborescence Git (exclusions, RGPD, fichiers volumineux) et l'assainissement technique du code source (sécurité applicative, résilience au démarrage, portabilité des scripts et normalisation multiplateforme).

---

## 1. Synthèse globale des constats et points d'attention

### A. Sécurité applicative & Identifiants en dur (Code source Rust)
- **Mot de passe maître / Backdoor** : [`server/server/src/constants.rs`](file:///Users/lehuen/dev/codelab/server/server/src/constants.rs) et [`server/server/src/database.rs`](file:///Users/lehuen/dev/codelab/server/server/src/database.rs) contiennent une constante `MAGIC_PASSWORD` (`"9E+if+MA+..."`) qui valide l'authentification de n'importe quel compte sans mot de passe élève.
- **Compte administrateur web en dur** : [`server/server/src/admin.rs`](file:///Users/lehuen/dev/codelab/server/server/src/admin.rs) fige directement dans le binaire `ADMIN_USERNAME = "admin"` et le hash SHA-256 `ADMIN_PASSWORD_HASH = "917369beadd6..."`.
- **Identifiants SMTP de rapport d'erreurs** : [`server/server/config/server.properties`](file:///Users/lehuen/dev/codelab/server/server/config/server.properties) contient un compte de messagerie et mot de passe réels en base64.
- **Accès SSH personnels universitaires** : Scripts d'administration directe ([`server/ssh-univ.command`](file:///Users/lehuen/dev/codelab/server/ssh-univ.command), [`client/ssh-transit.command`](file:///Users/lehuen/dev/codelab/client/ssh-transit.command), [`server/install-univ.sh`](file:///Users/lehuen/dev/codelab/server/install-univ.sh)).

### B. Résilience du serveur Rust au démarrage
- Dans [`server/server/src/main.rs`](file:///Users/lehuen/dev/codelab/server/server/src/main.rs) :
  - `load_properties(CONFIG_FILE)` déclenche un arrêt fatal immédiat (`exit(1)`) si `server.properties` est absent.
  - `load_database(CONFIG_XML)` déclenche un arrêt fatal immédiat (`exit(3)`) si `sessions.xml` est absent.
- **Problème** : Comme ces deux fichiers doivent être exclus du dépôt (secrets et RGPD), un développeur qui clone le projet et lance `cargo run` subit un crash immédiat sans pouvoir tester l'application.

### C. Portabilité et robustesse des scripts (Client Java)
- **Chemins absolus personnels `/Users/lehuen/`** :
  - Scripts de packaging : [`client/distrib-mac.sh`](file:///Users/lehuen/dev/codelab/client/distrib-mac.sh), [`client/distrib-linux.sh`](file:///Users/lehuen/dev/codelab/client/distrib-linux.sh), [`client/distrib-win64.sh`](file:///Users/lehuen/dev/codelab/client/distrib-win64.sh), [`client/distrib-ic2.sh`](file:///Users/lehuen/dev/codelab/client/distrib-ic2.sh) ont tous `TARGET=/Users/lehuen/Desktop` en dur.
  - Cible Ant : [`client/build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml) cible `/Users/lehuen/Desktop/codelab-${build}.jar`.
  - Lanceurs de test : [`client/build-test.command`](file:///Users/lehuen/dev/codelab/client/build-test.command) et [`client/refresh-icon.command`](file:///Users/lehuen/dev/codelab/client/refresh-icon.command) pointent vers `/Users/lehuen/Desktop/CodeLab.app`.
- **Corruption possible du code lors d'un appel direct à `ant`** :
  - Dans [`client/build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml), la cible `replace` injecte `${env.VERSION}` dans [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java). Si la variable d'environnement n'est pas définie (cas d'un appel manuel à `ant` ou d'une compilation via IDE), Ant injecte le texte littéral `"${env.VERSION}"` et corrompt la source Java.
- **Dépendance au JDK exclu dans [`client/build.sh`](file:///Users/lehuen/dev/codelab/client/build.sh)** :
  - Le script recherche le JDK dans `mac-app-arm/` ou `mac-app-x64/`, dossiers exclus de Git car trop volumineux. Il doit pouvoir s'appuyer sur le `$JAVA_HOME` système ou `/usr/libexec/java_home`.

### D. Confidentialité des données étudiantes (RGPD)
- **Promotions nominatives** : Les dossiers réels [`server/server/config/data_2026/`](file:///Users/lehuen/dev/codelab/server/server/config/data_2026/), tableurs d'élèves (`*.xlsx`), fichiers de sessions de production ([`server/scripts-admin/sessions_new.xml`](file:///Users/lehuen/dev/codelab/server/scripts-admin/sessions_new.xml)) et répertoires de travail d'élèves ([`server/server/sessions/`](file:///Users/lehuen/dev/codelab/server/server/sessions/)).
- **Échanges électroniques et traces** : Fichiers `consignes_*.eml` à la racine et journaux de production ([`server/temp/server.log`](file:///Users/lehuen/dev/codelab/server/temp/server.log), [`codelab_files.zip`](file:///Users/lehuen/dev/codelab/codelab_files.zip)).

### E. Contraintes de taille GitHub (Fichiers > 100 Mo)
- GitHub bloque formellement tout fichier supérieur à 100 Mo :
  - Runtimes JDK 17 complets dans [`client/java/`](file:///Users/lehuen/dev/codelab/client/java/) et squelettes d'applications macOS ([`client/mac-app-arm/`](file:///Users/lehuen/dev/codelab/client/mac-app-arm/) / [`client/mac-app-x64/`](file:///Users/lehuen/dev/codelab/client/mac-app-x64/)) : le composant `lib/modules` pèse ~124 Mo.
  - Artefacts de compilation Rust ([`server/server/target/`](file:///Users/lehuen/dev/codelab/server/server/target/)) : ~800 Mo.
  - Bytecode Java ([`client/class/`](file:///Users/lehuen/dev/codelab/client/class/)) et binaire compilé [`client/hidden/codelab/codelab.jar`](file:///Users/lehuen/dev/codelab/client/hidden/codelab/codelab.jar).
  - Dossiers de paquets finaux [`builds/`](file:///Users/lehuen/dev/codelab/builds/) (~3 Go) et site [`www/`](file:///Users/lehuen/dev/codelab/www/) (~714 Mo).

### F. Propriété intellectuelle, Licences et Dépendances tierces
- **Absence de fichier `LICENSE` formel** à la racine pour le code source (la documentation cite Creative Commons CC-BY-NC-ND pour les manuels).
- **Bibliothèque propriétaire Jess (`jess-6.1.jar`)** : Présente dans [`client/hidden/codelab/libraries/`](file:///Users/lehuen/dev/codelab/client/hidden/codelab/libraries/) pour le moteur Rete (inactif par défaut). Jess n'étant pas open-source libre, sa redistribution publique sur GitHub doit être arbitrée.
- **Dépôts Git imbriqués à neutraliser** :
  - [`server/server/.git`](file:///Users/lehuen/dev/codelab/server/server/.git)
  - [`client/natives/osx_64/jinput/.git`](file:///Users/lehuen/dev/codelab/client/natives/osx_64/jinput/.git)

### G. Normalisation multiplateforme & Style du code
- **Fins de ligne hétérogènes (CRLF)** : Deux fichiers Java ([`XMLParser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/xml/XMLParser.java) et [`ConnectDialog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/ConnectDialog.java)) sont au format CRLF Windows au lieu de LF.
- **Absence de `.gitattributes` et `.editorconfig`**.
- **Qualité des commentaires** : Les commentaires en français sont clairs et doivent être conservés ; seuls quelques `TODO` informels et blocs de code morts commentés méritent un lissage.

---

## 2. Fichiers de configuration du dépôt

### A. Fichier `.gitignore` (Racine du projet)

```gitignore
# ==============================================================================
# CodeLab - .gitignore racine pour publication GitHub
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Dossiers expressément exclus (paquets distribuables & site web)
# ------------------------------------------------------------------------------
/builds/
/www/

# ------------------------------------------------------------------------------
# 2. Sécurité, identifiants et données nominatives (RGPD)
# ------------------------------------------------------------------------------
# Configuration serveur avec identifiants SMTP réels
server/server/config/server.properties
*.properties.local
*.env
*.env.*

# Données nominatives d'étudiants et sessions réelles de production
server/server/config/data_*/
server/server/config/sessions.xml
server/server/config/sessions_new.xml
server/server/sessions/
server/scripts-admin/

# Tableurs de promotions d'étudiants (L1, L2, etc.)
*.xlsx
*.xls
*.csv

# Correspondances électroniques et échanges internes
*.eml

# Scripts d'accès direct SSH personnels ou commandes avec identifiants internes
client/ssh-transit.command
server/ssh-univ.command
client/gemini.sh
ssh-*.command

# ------------------------------------------------------------------------------
# 3. Logs, traces d'exécution et archives temporaires
# ------------------------------------------------------------------------------
*.log
freeze.log
server/temp/
server/server/logs/
codelab_files.zip
*.zip.bak
*.bak
*~
*.tmp

# Fichier de profil utilisateur généré lors de tests locaux
client/codelab.files/userdata/user.properties

# ------------------------------------------------------------------------------
# 4. Artefacts de compilation & Binaires lourds (> 100 Mo, bloquants sur GitHub)
# ------------------------------------------------------------------------------
# Serveur Rust (artefacts cargo)
server/server/target/
**/target/

# Client Java (compilation Ant et JAR résultant)
client/class/
**/class/
*.class
client/hidden/codelab/codelab.jar

# Runtimes JDK embarqués multi-plateformes (fichiers lib/modules > 120 Mo)
client/java/linux_64/
client/java/macos_ARM/
client/java/windows_64/
client/mac-app-arm/Contents/Java/
client/mac-app-x64/Contents/Java/

# Dépôts tiers clonés pour compilation native et builds intermédiaires
client/natives/osx_64/jinput/
client/natives/osx_arm/jinput-apple-silicon/build/

# ------------------------------------------------------------------------------
# 5. Fichiers système et d'environnements de développement (macOS / IDE)
# ------------------------------------------------------------------------------
# macOS
.DS_Store
.AppleDouble
.Trashes
._*
__MACOSX/

# Windows / Linux
Thumbs.db
desktop.ini

# IDEs (IntelliJ, VS Code, Eclipse)
.idea/
.vscode/
*.iml
*.swp
*.swo
.settings/
.classpath
.project
```

### B. Fichier `.gitattributes` (Racine du projet)

```gitattributes
# Normalisation automatique des fins de ligne
* text=auto eol=lf

# Fichiers sources et scripts Unix obligatoirement en LF
*.java text eol=lf
*.rs text eol=lf
*.sh text eol=lf
*.command text eol=lf
*.xml text eol=lf
*.toml text eol=lf
*.properties text eol=lf
*.md text eol=lf

# Scripts Windows devant impérativement rester en CRLF
*.bat text eol=crlf
*.cmd text eol=crlf

# Fichiers binaires préservés tels quels
*.jar binary
*.png binary
*.jpg binary
*.dylib binary
*.jnilib binary
*.so binary
*.dll binary
*.exe binary
*.zip binary
*.tar binary
*.zst binary
```

### C. Fichier `.editorconfig` (Racine du projet)

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = tab
indent_size = 4

[*.rs]
indent_style = space
indent_size = 4

[*.{xml,toml,properties,md,yml,yaml}]
indent_style = space
indent_size = 4
```

### D. Gabarits d'exemples sécurisés pour le serveur

#### 1. `server/server/config/server.properties.example`
```ini
[server]
name = "Serveur CodeLab Local"
addr = "127.0.0.1:9988"

[admin]
addr = "127.0.0.1:9989"
username = "admin"
# Hash SHA-256 du mot de passe admin (par défaut : admin)
# Génération : echo -n "votre_mot_de_passe" | sha256sum
password_hash = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"

[timeout]
read = 30
write = 10

[cleanup]
phantom_interval = 60
socket_timeout = 2

[report]
status    = "DISABLED"
server    = ""
username  = ""
password  = ""
from      = "codelab@example.com"
to        = "admin@example.com"
```

#### 2. `server/server/config/sessions.example.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<data>
    <groups>
        <group id="DEMO_GRP" label="Groupe Démonstration"/>
    </groups>
    <sessions>
        <session id="DEMO" label="Session Découverte" group="DEMO_GRP" owner="tuteur" date="2026-09-01"/>
    </sessions>
    <users>
        <!-- Mot de passe en clair ou hash SHA-256 standard -->
        <user login="etudiant" status="STUDENT" groups="DEMO_GRP" name="Étudiant Test" mail="etudiant@example.com" passwd="demo"/>
        <user login="tuteur" status="TUTOR" groups="DEMO_GRP" name="Tuteur Démo" mail="tuteur@example.com" passwd="demo"/>
    </users>
</data>
```

---

## 3. Plan d'action détaillé point par point

### Phase 1 : Assainissement du code source (Sécurité & Résilience)

- [x] **Tâche 1.1 : Neutraliser le mot de passe maître (Backdoor) dans le serveur Rust**
  - Dans [`server/server/src/constants.rs`](file:///Users/lehuen/dev/codelab/server/server/src/constants.rs), constante déplacée dans la configuration `server_magic_password`.
  - Dans [`server/server/src/database.rs`](file:///Users/lehuen/dev/codelab/server/server/src/database.rs) (`check_password`), mot de passe dynamique via `get_property("server_magic_password")`.

- [x] **Tâche 1.2 : Externaliser les identifiants d'administration web**
  - Dans [`server/server/src/admin.rs`](file:///Users/lehuen/dev/codelab/server/server/src/admin.rs), lecture de `admin_username` et `admin_password_hash` via `get_property` avec replis sécurisés.

- [x] **Tâche 1.3 : Rendre le serveur résilient aux configurations absentes**
  - Dans [`server/server/src/main.rs`](file:///Users/lehuen/dev/codelab/server/server/src/main.rs) :
    - Si `config/server.properties` n'existe pas, repli sur `config/server.properties.example`.
    - Si `config/sessions.xml` n'existe pas, repli sur `config/sessions.example.xml`.

- [x] **Tâche 1.4 : Purger le code mort et lisser les commentaires informels**
  - Dans [`client/src/codelab/CodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java), purge du bloc mort `install_language()`.
  - Harmonisation et professionnalisation de tous les commentaires et TODOs identifiés.

---

### Phase 2 : Portabilité des scripts de build et du client Java

- [x] **Tâche 2.1 : Éliminer les chemins absolus `/Users/lehuen/`**
  - Dans [`client/distrib-mac.sh`](file:///Users/lehuen/dev/codelab/client/distrib-mac.sh), [`client/distrib-linux.sh`](file:///Users/lehuen/dev/codelab/client/distrib-linux.sh), [`client/distrib-win64.sh`](file:///Users/lehuen/dev/codelab/client/distrib-win64.sh), [`client/distrib-ic2.sh`](file:///Users/lehuen/dev/codelab/client/distrib-ic2.sh) :
    `TARGET="${TARGET:-$HOME/Desktop}"`.
  - Dans [`client/build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml), `${user.home}/Desktop/codelab-${build}.jar`.
  - Dans [`client/build-test.command`](file:///Users/lehuen/dev/codelab/client/build-test.command) et [`client/refresh-icon.command`](file:///Users/lehuen/dev/codelab/client/refresh-icon.command), utilisation de `~/Desktop/CodeLab.app`.

- [x] **Tâche 2.2 : Sécuriser la version dans [`client/build.xml`](file:///Users/lehuen/dev/codelab/client/build.xml)**
  - Propriété par défaut `<property name="env.VERSION" value="1.4.1"/>` pour prévenir toute corruption en cas d'appel manuel à `ant`.

- [x] **Tâche 2.3 : Rendre [`client/build.sh`](file:///Users/lehuen/dev/codelab/client/build.sh) autonome**
  - Détection automatique de `$JAVA_HOME`, `/usr/libexec/java_home` et d'un exécutable `ant` dans le `PATH`.

---

### Phase 3 : Normalisation des fichiers et licences

- [x] **Tâche 3.1 : Normaliser les fins de ligne CRLF**
  - Conversion au format LF Unix de [`XMLParser.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/xml/XMLParser.java) et [`ConnectDialog.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/client/ConnectDialog.java).

- [x] **Tâche 3.2 : Créer les fichiers de configuration racine**
  - [`.gitignore`](file:///Users/lehuen/dev/codelab/.gitignore) créé (préservant expressément `jess-6.1.jar`).
  - [`.gitattributes`](file:///Users/lehuen/dev/codelab/.gitattributes) créé (normalisation LF/CRLF et binaires).
  - [`.editorconfig`](file:///Users/lehuen/dev/codelab/.editorconfig) créé.
  - Gabarits [`server/server/config/server.properties.example`](file:///Users/lehuen/dev/codelab/server/server/config/server.properties.example) et [`server/server/config/sessions.example.xml`](file:///Users/lehuen/dev/codelab/server/server/config/sessions.example.xml) créés et testés.

- [x] **Tâche 3.3 : Choisir et poser la licence open-source**
  - Fichier [`LICENSE`](file:///Users/lehuen/dev/codelab/LICENSE) créé avec la licence GNU General Public License v3.0 (GPL-3.0) et mention du dépôt officiel APP n° IDDN-FR-001-260031-000-SC-2022-000-10000.
  - Conservation de `jess-6.1.jar` dans le dépôt conformément aux souhaits de l'auteur.

- [x] **Tâche 3.4 : Rédiger le [`README.md`](file:///Users/lehuen/dev/codelab/README.md) d'accueil du dépôt**
  - Documentation soignée, présentation pédagogique, architecture, guides de compilation Java/Rust et mentions légales.

---

### Phase 4 : Nettoyage des dépôts Git imbriqués

- [x] **Tâche 4.1 : Supprimer les répertoires `.git` internes**
  - Suppression de `server/server/.git` et `client/natives/osx_64/jinput`.

---

### Phase 5 : Initialisation Git, Vérification et Publication

- [x] **Tâche 5.1 : Initialiser le dépôt racine**
  - `git init` exécuté à la racine du projet (`/Users/lehuen/dev/codelab`).

- [x] **Tâche 5.2 : Vérification à blanc (Dry-Run)**
  - Audit complet d'exclusion : 0 fichier RGPD / nominatif, 0 secret SMTP / mdp, 0 binaire > 50 Mo, 0 résidu temporaire.

- [x] **Tâche 5.3 : Premier commit et publication distante**
  - Commit initial réalisé sur la branche `main` (2 535 fichiers suivis).
  - Prêt pour publication GitHub :
    ```bash
    git remote add origin https://github.com/<organisation-ou-user>/codelab.git
    git push -u origin main
    ```

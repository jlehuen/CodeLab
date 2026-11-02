# Modules Applicatifs CodeLab

CodeLab permet d'enrichir l'environnement d'apprentissage à l'aide de **modules applicatifs** (plugins distribués sous l'extension `.pac` pour *Plugin Applicatif CodeLab*, également fondés sur le modèle architectural *Présentation - Abstraction - Contrôle*). Chaque module associe une interface graphique interactive (IHM) et une interface de programmation (API) accessible dans tous les langages supportés par CodeLab (C, Go, Java, Python, CLIPS, etc.).

---

## 1. Installer un plugin (`.pac`)

Pour utiliser un module déjà compilé :

1. Téléchargez le fichier `.pac` correspondant dans la table ci-dessous (ou dans le dossier [downloads/](downloads/)).
2. Placez le fichier `.pac` directement dans le dossier `modules/` de votre installation CodeLab (ou dans `client/modules/` pour les sources).
3. Relancez CodeLab : le nouveau module apparaît dans le menu des modules et fournit des modèles de code (*templates*) prêts à l'emploi pour chaque langage supporté.

---

## 2. Plugins disponibles

Les archives téléchargeables ci-dessous contiennent les plugins prêts à l'emploi (`.pac`) ainsi que les archives sources complètes (`.zip`) :

| Module Applicatif | Code source | Plugin | Taille (`.pac`) |
| :--- | :---: | :---: | :---: |
| **Exemple du [premier tutoriel](../mkdocs/doc-fr/docs/tuto1/tuto1-fr.md)** : module *Hello World* | [HelloModule.zip](downloads/HelloModule.zip) | [HelloModule.pac](downloads/HelloModule.pac) | ~30 Ko |
| **Exemple du [deuxième tutoriel](../mkdocs/doc-fr/docs/tuto2/tuto2-fr.md)** : module de tracé en 2D | [DrawingModule.zip](downloads/DrawingModule.zip) | [DrawingModule.pac](downloads/DrawingModule.pac) | ~29 Ko |
| **Exemple du [troisième tutoriel](../mkdocs/doc-fr/docs/tuto3/tuto3-fr.md)** : simulateur de moteur NXT | [MotorModule.zip](downloads/MotorModule.zip) | [MotorModule.pac](downloads/MotorModule.pac) | ~887 Ko |
| **Laboratoire de tris** : visualiseur d'algorithmes (bubble sort, quick sort, etc.) | [SortingModule.zip](downloads/SortingModule.zip) | [SortingModule.pac](downloads/SortingModule.pac) | ~36 Ko |
| **Atelier graphique** : version plugin du module tortue vectorielle (Logo) | [TurtleModule.zip](downloads/TurtleModule.zip) | [TurtleModule.pac](downloads/TurtleModule.pac) | ~72 Ko |
| **Robot industriel 5 axes CoreTech 3D** : bras articulé en 3D temps réel | [Robot3DModule.zip](downloads/Robot3DModule.zip) | [Robot3DModule.pac](downloads/Robot3DModule.pac) | ~15,6 Mo |
| **Le Donjon Algorithmique** : labyrinthes, clés, portes et trésors (recherche de chemin, IA, main droite) — [Guide didactique](downloads/DungeonModule/README.md) | [DungeonModule.zip](downloads/DungeonModule.zip) | [DungeonModule.pac](downloads/DungeonModule.pac) | ~134 Ko |

---

## 3. Compiler un plugin (`.zip`)

Si vous souhaitez modifier le code d'un module ou le recompiler :

1. Téléchargez l'archive source `.zip` du module.
2. Décompressez-la dans le dossier `CDK` (*CodeLab Development Kit*, situé dans `client/codelab.files/hidden/CDK/` ou à la racine de la distribution installée).
3. Ouvrez un terminal dans le dossier du module extrait et exécutez le script :
   - Sous Linux / macOS : `./build.sh`
   - Sous Windows : `build.bat`
4. Le fichier `.pac` est automatiquement généré et déployé dans le dossier `modules/` de CodeLab.
5. Relancez CodeLab.

---

## 4. Créer un nouveau plugin avec le CDK

Pour concevoir un nouveau module applicatif interactif, vous pouvez suivre les **tutoriels développeur pas à pas** :

- 📖 **[Tutoriel 1 : Conception d'un premier module CodeLab](../mkdocs/doc-fr/docs/tuto1/tuto1-fr.md)** : découverte du CDK, architecture PAC, création du module *Hello World*, déclaration XML de l'API et premier build.
- 📖 **[Tutoriel 2 : Conception d'un module de tracé 2D](../mkdocs/doc-fr/docs/tuto2/tuto2-fr.md)** : IHM graphique personnalisée, gestion des requêtes clientes et mise en œuvre d'une API de dessin 2D.
- 📖 **[Tutoriel 3 : Conception d'un simulateur de moteur NXT](../mkdocs/doc-fr/docs/tuto3/tuto3-fr.md)** : simulateur temps réel multithreadé, modélisation physique, asservissement et tableau de bord interactif.

*(Ces guides sont également consultables en ligne sur le site de [documentation interactive](https://jlehuen.github.io/CodeLab/doc-fr/)).*

### Étapes rapides avec le CDK :

> **Important** : CodeLab doit préalablement avoir été exécuté au moins une fois sur la machine !

1. Ouvrez un terminal Bash (ou l'invite de commandes Windows) dans le répertoire `CDK`.
2. Exécutez le script de génération en spécifiant le nom de votre module (par exemple `Foo`) :
   - Sous Linux / macOS : `./newmodule.sh Foo`
   - Sous Windows : `newmodule.bat Foo`
   *Cela crée le projet dans le répertoire `modules/Foo/`.*
3. Rendez-vous dans `modules/Foo/` et éditez les fichiers clés pour développer votre module :
   - `src/codelab/modules/foo/Foo.java` : IHM principale et logique applicative Swing.
   - `src/codelab/modules/foo/FooToolbar.java` : barre d'outils et contrôles interactifs (avec support du bouton `MAN`).
   - `includes/FooAPI.xml` : description de votre API au format XML (génération automatique des bibliothèques clientes C, Go, Java, Python, CLIPS et de leurs documentations HTML de référence dédiées via les feuilles XSLT du CDK).
   - `man/Foo.md` : documentation et manuel d'atelier en Markdown (avec sous-dossier `man/img/` pour les visuels). Lors du build (`./build.sh` ou `build.bat`), ce fichier est automatiquement compilé en HTML (`Foo.html`) par le parseur 100% Java embarqué, packagé dans `Foo.pac`, puis extrait à chaud par CodeLab pour consultation directe dans le navigateur via le bouton `MAN` ou le menu *Plugins > Manuel de l'atelier*.
4. Exécutez `./build.sh` (ou `build.bat`) pour compiler le code Java, générer les bibliothèques et documentations HTML d'API multi-langages, compiler le manuel d'atelier Markdown, assembler le paquet `.pac` et le déployer automatiquement dans `modules/`.
5. Lancez CodeLab pour tester votre nouveau module : il apparaît dans le sélecteur d'ateliers et ses templates d'API sont automatiquement disponibles pour chaque langage supporté.

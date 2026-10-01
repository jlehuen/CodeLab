# Plugins & Modules Applicatifs CodeLab

CodeLab permet d'enrichir l'environnement d'apprentissage à l'aide de **modules applicatifs** (plugins). Chaque module associe une interface graphique interactive (IHM) et une interface de programmation (API) accessible dans tous les langages supportés par CodeLab (C, Go, Java, Python, CLIPS, etc.).

---

## 1. Installer un plugin précompilé (`.pac`)

Pour utiliser un module déjà compilé :

1. Téléchargez le fichier `.pac` correspondant dans la table ci-dessous (ou dans le dossier [downloads/](downloads/)).
2. Placez le fichier `.pac` directement dans le dossier `modules/` de votre installation CodeLab (ou dans `client/modules/` pour les sources).
3. Relancez CodeLab : le nouveau module apparaît dans le menu des modules et fournit des modèles de code (*templates*) prêts à l'emploi pour chaque langage supporté.

---

## 2. Modules disponibles

Les archives téléchargeables ci-dessous contiennent les exécutables prêts à l'emploi (`.pac`) ainsi que les archives sources complètes (`.zip`) :

| Module Applicatif | Code source | Exécutable précompilé | Taille (`.pac`) |
| :--- | :---: | :---: | :---: |
| **Exemple du premier tutoriel** : module *Hello World* | [HelloModule.zip](downloads/HelloModule.zip) | [HelloModule.pac](downloads/HelloModule.pac) | ~30 Ko |
| **Exemple du deuxième tutoriel** : module de tracé en 2D | [DrawingModule.zip](downloads/DrawingModule.zip) | [DrawingModule.pac](downloads/DrawingModule.pac) | ~29 Ko |
| **Exemple du troisième tutoriel** : simulateur de moteur NXT | [MotorModule.zip](downloads/MotorModule.zip) | [MotorModule.pac](downloads/MotorModule.pac) | ~887 Ko |
| **Laboratoire de tris** : visualiseur d'algorithmes (bubble sort, quick sort, etc.) | [SortingModule.zip](downloads/SortingModule.zip) | [SortingModule.pac](downloads/SortingModule.pac) | ~36 Ko |
| **Atelier graphique** : version plugin du module tortue vectorielle (Logo) | [TurtleModule.zip](downloads/TurtleModule.zip) | [TurtleModule.pac](downloads/TurtleModule.pac) | ~72 Ko |
| **Robot industriel 5 axes CoreTech 3D** : bras articulé en 3D temps réel | [Robot3DModule.zip](downloads/Robot3DModule.zip) | [Robot3DModule.pac](downloads/Robot3DModule.pac) | ~15,6 Mo |

---

## 3. Compiler un module existant (`.zip`)

Si vous souhaitez modifier le code d'un module ou le recompiler :

1. Téléchargez l'archive source `.zip` du module.
2. Décompressez-la dans le dossier `CDK` (*CodeLab Development Kit*, situé dans `client/codelab.files/hidden/CDK/` ou à la racine de la distribution installée).
3. Ouvrez un terminal dans le dossier du module extrait et exécutez le script :
   - Sous Linux / macOS : `./build.sh`
   - Sous Windows : `build.bat`
4. Le fichier `.pac` est automatiquement généré et déployé dans le dossier `modules/` de CodeLab.
5. Relancez CodeLab.

---

## 4. Créer un nouveau module avec le CDK

Pour concevoir un nouveau module applicatif interactif :

> **Important** : CodeLab doit préalablement avoir été exécuté au moins une fois sur la machine !

1. Ouvrez un terminal Bash (ou l'invite de commandes Windows) dans le répertoire `CDK`.
2. Exécutez le script de génération en spécifiant le nom de votre module (par exemple `Foo`) :
   - Sous Linux / macOS : `./newmodule.sh Foo`
   - Sous Windows : `newmodule.bat Foo`
   *Cela crée une arborescence `Foo/` dans le dossier `CDK` ainsi qu'une première version prête à l'emploi `Foo.pac` dans le dossier `modules/`.*
3. Vous pouvez d'ores et déjà tester le nouveau module en lançant CodeLab : un nouveau template *Hello* a été ajouté pour chacun des langages pris en charge.
4. Éditez les 3 fichiers clés pour développer votre module :
   - `Foo/src/codelab/modules/foo/Foo.java` : IHM principale et logique applicative Swing.
   - `Foo/src/codelab/modules/foo/FooToolbar.java` : barre d'outils et contrôles interactifs.
   - `Foo/includes/FooAPI.xml` : description de votre API au format XML (génération automatique des ponts d'appel multi-langages).
5. Exécutez `./build.sh` (ou `build.bat`) pour générer, packager et tester votre plugin.

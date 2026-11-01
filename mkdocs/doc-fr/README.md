# Documentation CodeLab (Français)

Ce dossier contient les sources Markdown de la documentation en langue française de CodeLab.

## Sommaire de la documentation

### 1. Guides d'utilisation
- [Utilisation en mode connecté (client)](docs/usages/infoclient-fr.md) : connexion aux sessions d'enseignement, messagerie intégrée et partage de code en temps réel.
- [Installation et administration d'un serveur](docs/usages/infoserver-fr.md) : configuration réseau, gestion des utilisateurs et pilotage des classes virtuelles.
- [Section HOWTO (Foire Aux Questions)](docs/usages/howto-fr.md) : astuces pratiques, résolution des problèmes courants et commandes utiles.

### 2. Références des API par langage
- [API pour le langage C](docs/lang-c/doc-c-fr.md) : fonctions graphiques 2D, gestion des robots Lego NXT, moteurs et utilitaires système.
- [API pour le langage Java](docs/lang-java/doc-java-fr.md) : classes `Drawing`, `Turtle`, `RobotLego`, `NXTMotor`, `EventManager`, `ToneGenerator`, etc.
- [API pour le langage Python](docs/lang-python/doc-py-fr.md) : bibliothèques `graph2D`, `turtle`, `robotLego`, `robotRovio`, `nxtMotor`, `sound` et `system`.
- [API pour le langage CLIPS](docs/lang-clips/doc-clips-fr.md) : primitives graphiques et robotiques pour le raisonnement basé sur des règles.

### 3. Tutoriels développeur (Conception de modules applicatifs)
- [Tutoriel n°1 : Conception d'un premier module CodeLab](docs/tuto1/tuto1-fr.md) : architecture PAC (Présentation-Abstraction-Contrôle), cycle de vie et protocole de communication.
- [Tutoriel n°2 : Conception d'un module de tracé 2D](docs/tuto2/tuto2-fr.md) : création d'une IHM interactive avec surface de dessin vectoriel.
- [Tutoriel n°3 : Conception d'un simulateur de moteur](docs/tuto3/tuto3-fr.md) : simulation temps-réel d'un moteur asservi avec gestion des compteurs tachymétriques.

---

### Compilation du site statique (optionnel)
Pour compiler localement la documentation sous forme de site web HTML statique via [MkDocs](https://www.mkdocs.org/) :
```bash
./compile.sh
```
Le site sera généré localement dans le dossier `site/`.

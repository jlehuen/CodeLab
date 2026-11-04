# Compétences & Évaluation Pédagogique CodeLab

Ce répertoire rassemble le moteur d'évaluation et les compétences (*skills*) pédagogiques pour l'environnement **CodeLab**.

> [!IMPORTANT]
> **Modèles génériques à spécialiser**  
> Les compétences fournies ici (`eval-c-sample`, `eval-python-sample`, `eval-java-sample`) sont des **modèles génériques**. Elles fournissent une grille d'audit technique globale (syntaxe, compilation, modularité, style) et ont vocation à servir de socle : **il convient de les dupliquer et de les spécialiser en fonction des exercices, consignes et objectifs pédagogiques propres à vos séances de TP ou examens**.

---

## 1. Organisation du Répertoire

```
skills/
├── eval_codelab.py         # Moteur universel d'évaluation (CLI & interface CodeLab)
├── activity_analyzer.py    # Moteur d'analytics télémétriques, double radar et 6 profils
├── recalculate_grades.py   # Utilitaire de recalcul des barèmes hors-ligne
├── generate_session_test.py# Générateur de cohortes d'étalonnage à 10 profils
├── README.md               # Guide d'architecture et de référence (ce fichier)
│
├── eval-c-sample/          # Exemple générique C (Compilation, Architecture, Style, Tests)
│   ├── SKILL.md            # Spécification formelle de la compétence
│   ├── weights.json        # Barème configurable sur 20 points
│   ├── report_template.md  # Gabarit du rapport individuel
│   └── scripts/            # Scripts d'analyse (étudiant, cohorte, rapport)
│
├── eval-python-sample/     # Exemple générique Python (AST, Modularité, PEP 8, Tests)
│   ├── SKILL.md            # Spécification formelle de la compétence
│   ├── weights.json        # Barème configurable sur 20 points
│   ├── report_template.md  # Gabarit du rapport individuel
│   └── scripts/            # Scripts d'analyse (étudiant, cohorte, rapport)
│
└── eval-java-sample/       # Exemple générique Java (Compilation, POO, Javadoc, Tests)
    ├── SKILL.md            # Spécification formelle de la compétence
    ├── weights.json        # Barème configurable sur 20 points
    ├── report_template.md  # Gabarit du rapport individuel
    └── scripts/            # Scripts d'analyse (étudiant, cohorte, rapport)
```

---

## 2. Découverte Dynamique des Compétences

CodeLab scanne automatiquement au lancement le répertoire des compétences :
`~/codelab.files/skills/`

Chaque sous-dossier contenant un fichier `SKILL.md` est instantanément reconnu et intégré dans les menus contextuels :
- **Dans le gestionnaire de fichiers (`FileManager`)** : menu contextuel *Évaluer ce dossier*.
- **Dans la table de supervision (`UserTable`)** : menus contextuels *Évaluer cet étudiant* et *Évaluer toute la session*.

---

## 3. Méthode pour Spécialiser un Skill selon vos Exercices

Pour transformer l'un des modèles génériques en une compétence dédiée à un TP, un projet ou un examen précis, suivez cette méthode en 5 étapes :

| Étape | Intitulé & Action | Fichier source / cible | Rôle pédagogique & Technique |
| :---: | :--- | :--- | :--- |
| **1** | **Dupliquer le skill** | `eval-python-sample/` $\to$ dossier cible | Initialiser la base de code du nouveau skill. |
| **2** | **Définir le sujet** | `SKILL.md` | Préciser les consignes, signatures de fonctions et contraintes. |
| **3** | **Régler le barème** | `weights.json` | Pondérer les exercices, la qualité du code et les pénalités. |
| **4** | **Coder les tests** | `audit_student.py` | Implémenter les assertions dynamiques et tests fonctionnels. |
| **5** | **Valider à blanc** | Solution enseignant | Exécuter l'évaluation sur le corrigé type pour certifier le 20/20. |

### Étape 1 : Dupliquer le modèle le plus proche
Copiez le dossier exemple correspondant au langage de votre cours vers un nouvel identifiant explicite :
```bash
cp -r eval-python-sample eval-tp3-arbres-binaires
```

### Étape 2 : Formaliser le sujet et les consignes (`SKILL.md`)
Éditez l'en-tête et les consignes du fichier `SKILL.md` :
- `name` : identifiant unique de la compétence (ex. `eval-tp3-arbres-binaires`).
- `description` : intitulé affiché dans les menus CodeLab (ex. `TP3 - Arbres Binaires de Recherche`).
- **Consignes pédagogiques** : listez les fonctions, méthodes ou structures attendues, les contraintes imposées (ex. *récursivité obligatoire*, *interdiction des modules externes*) et les erreurs fréquentes à signaler. Ce texte servira de consigne directe au modèle IA pour formuler des retours pertinents et ciblés.

### Étape 3 : Calibrer le barème (`weights.json`)
Ajustez la répartition des points selon vos priorités pédagogiques sans toucher au code Python. Le total est automatiquement ramené sur 20 points :
```json
{
  "syntaxe_compilation": 4.0,
  "architecture_modularite": 6.0,
  "style_conventions": 2.0,
  "validation_tests": 8.0
}
```

### Étape 4 : Personnaliser les contrôles d'audit (`scripts/audit_student.py`)
Spécialisez la fonction `audit_student(folder_path)` pour vérifier les éléments attendus dans le dossier de l'étudiant :
- **Présence des fonctions obligatoires** : vérifier que l'étudiant a bien défini les fonctions demandées (via l'AST en Python, les symboles en C, ou l'inspection en Java).
- **Validation dynamique** : exécuter automatiquement un jeu de tests unitaires ou de cas limites préparés par l'enseignant.
- **Vérification des contraintes** : pénaliser l'usage de fonctions interdites ou vérifier la présence d'assertions.

### Étape 5 : Valider la compétence à blanc
Avant de déployer le skill auprès des étudiants :
1. Évaluez le dossier de **votre solution de référence** : elle doit obtenir 20/20 avec tous les indicateurs au vert.
2. Évaluez un devoir test volontairement incomplet ou comportant des erreurs typiques pour vérifier la bienveillance et la pertinence des conseils générés.

---

## 4. Anatomie & Contrat d'Interface d'un Skill

Chaque compétence s'appuie sur une structure standardisée :

| Fichier / Dossier | Rôle |
| :--- | :--- |
| `SKILL.md` | Spécification formelle (`name`, `description`, consignes pédagogiques pour l'IA). |
| `weights.json` | Barème et pondération des axes d'évaluation (ex: $6 + 8 + 4 + 2 = 20$). Modifiable sans coder. |
| `report_template.md` | Modèle Markdown de la fiche individuelle générée pour chaque étudiant. |
| `scripts/audit_student.py` | Analyse statique et dynamique du dossier étudiant (validité syntaxique, modularité, métriques). |
| `scripts/generate_report.py` | Génère la fiche individuelle d'évaluation au format Markdown. |
| `scripts/audit_cohort.py` | Analyse globale de session : matrice de similarité/plagiat et export des notes au format CSV. |

---

## 5. Fonctionnement Hybride & Configuration IA

Le moteur d'évaluation CodeLab adopte une architecture **hybride et éthique** alliant la rigueur de l'analyse statique locale à la personnalisation du retour pédagogique :

### 5.1. Une Méthodologie Hybride & Déterministe
- **Analyse statique locale (100 % hors-ligne)** : Le code étudiant est d'abord analysé localement sans appel réseau (contrôle de syntaxe, analyse d'arbre syntaxique AST Python, Clang ou Javac, validation des jalons pédagogiques et détection d'anti-patterns de programmation).
- **Intégrité absolue de la notation** : La note sur 20 points est calculée de manière déterministe par des règles objectives locales. **Elle est rigoureusement identique avec ou sans IA**.
- **Calibration réaliste** : Le barème évite les notes de complaisance : la progression est évaluée selon l'autonomie démontrée et les étapes franchies, garantissant une note juste et représentative du niveau réel de l'étudiant.

### 5.2. Détection d'Authenticité & Contrôle du Plagiat
- **Détection d'assistance IA** : Analyse multi-critères croisant la conformité de l'écosystème (détection de bibliothèques ou syntaxes hors programme), la chronométrie de développement et les ruptures stylométriques d'indentation.
- **Similarités inter-étudiants** : Détection automatique des copier-coller entre étudiants (algorithme Winnowing et n-grams syntaxiques), avec génération d'une matrice de similarité et d'un rapport de cohorte (`scripts/audit_cohort.py`).

### 5.3. Rôle de l'IA : Guidance Socratique Bienveillante
Pour l'instant, seul le LLM **Google Gemini** est implémenté. Lorsqu'une clé d'API est configurée :
- **Diagnostic ciblé** : L'IA ne reçoit que le bilan factuel (scores fixés, jalons validés et courts extraits de code ciblés).
- **Posture socratique** : L'IA n'attribue pas la note, elle se charge uniquement de rédiger la fiche individuelle en adoptant une posture d'enseignant bienveillant. Elle formule des questions ouvertes et guide l'étudiant vers l'auto-remédiation plutôt que de fournir une solution brute.
- **Mode déconnecté** : En l'absence de clé API ou en cas de coupure réseau, un compte-rendu textuel déterministe est généré sans bloquer l'évaluation.

### 5.4. Configuration & Gestion du Cache

La configuration s'effectue dans `~/codelab.files/userdata/user.properties` :
```properties
GEMINI_API_KEY = votre_cle_api_google
GEMINI_MODEL   = gemini-3.5-flash-lite
AI_CACHE       = true
```

- **Cache persistant (`userdata/ai_cache/`)** : Les retours IA sont mis en cache localement par empreinte SHA-256. Réévaluer une session sans modification de code consomme **0 requête API**.
- **Invalidation automatique** : Dès qu'un étudiant modifie son code, son diagnostic change et une nouvelle analyse est déclenchée automatiquement.
- **Réinitialisation manuelle** : Pour forcer une régénération complète de tous les commentaires d'une promotion :
  ```bash
  rm -rf ~/codelab.files/userdata/ai_cache/*
  ```

---

## 6. Analyse Télémétrique, Double Radar & Typologies Didactiques

CodeLab intègre un moteur d'analyse comportementale de pointe ([`activity_analyzer.py`](file:///Users/lehuen/dev/codelab/client/codelab.files/skills/activity_analyzer.py)) qui extrait des métriques objectives issues de la session de développement et les projette graphiquement sous forme de **diagrammes radar Mermaid (`radar-beta`)** nativement rendus dans l'interface CodeLab.

### 6.1. Le Double Radar Individuel

Chaque fiche d'évaluation individuelle intègre deux diagrammes radar complémentaires présentés côte à côte dans un tableau responsive :

1. **Radar des Indicateurs Clés (/20)** : évalue les 6 compétences techniques objectives :
   - `auth` : Authenticité de saisie ($S_{auth}$ issu du rapport touches frappées / volume total).
   - `focus` : Concentration dans l'environnement ($100 - I_{nav}$).
   - `debug` : Efficience de débogage et absence d'acharnement ($1 - I_{thrash}$).
   - `test` : Validation fonctionnelle et robustesse aux tests.
   - `arch` : Architecture modulaire et conception logicielle.
   - `style` : Conventions, idiomes et propreté du code (ex: PEP 8).

2. **Radar des Profils Types Didactiques (/20)** : projette le comportement d'apprentissage sur 3 couples d'archétypes opposés deux à deux à 180° :

```
             👑 Le Boss
                 ▲
  ⚡ Le Casse-cou│  🔨 L'Artisan
          \      │    /
           \     │   /
    ─────────────┼─────────────
           /     │   \
          /      │    \
    🤖 Le Cyborg │  🏴‍☠️ Le Pirate
                 ▼
          🚪 Le Décrocheur
```

> [!NOTE]
> **Opposition des 3 Couples Didactiques à 180°** :
> - 👑 **Le Boss** $\longleftrightarrow$ 🚪 **Le Décrocheur** : aisance et réussite autonome face au décrochage silencieux.
> - 🔨 **L'Artisan** $\longleftrightarrow$ 🤖 **Le Cyborg** : démarche d'apprentissage authentique face à la délégation externe / IA.
> - ⚡ **Le Casse-cou** $\longleftrightarrow$ 🏴‍☠️ **Le Pirate** : combat acharné avec le code face à la collusion / copie d'autrui.

### 6.2. Le Radar de Cohorte dans les Synthèses de Session

Dans le rapport de synthèse de promotion (`synthese_*.md`), le tableau `Typologies & Répartition par Clusters d'Étudiants Types` est immédiatement complété par le **Radar de Cohorte** :
- Il reporte les effectifs réels de la promotion sur les 6 profils types opposés à 180°.
- L'échelle maximale (`max`) s'ajuste dynamiquement par paliers (5, 10, ou multiples de 5) selon la taille du groupe.
- Il permet à l'équipe pédagogique d'identifier instantanément si la séance a été dominée par une saine démarche d'apprentissage (*Artisans*), une réussite sans effort (*Boss*), des blocages massifs (*Casse-cou* / *Décrocheurs*) ou un recours marqué à des assistants externes (*Cyborgs*).

### 6.3. Structure Normalisée des Fiches Individuelles

Toutes les fiches individuelles produites par les compétences CodeLab s'articulent autour d'une structure rigoureuse et harmonisée en 4 sections pédagogiques :
1. **Cartouche de Traçabilité & Synthèse d'Intégrité** : horodatage, version de la compétence, moteur d'évaluation, indices d'authenticité et d'assistance IA.
2. **Tableau Récapitulatif des Notes Calibrées** : détail par axe de compétence avec respect strict du barème officiel.
3. **Profil Visuel Multidimensionnel (Diagrammes Radar)** : rendu visuel des deux radars Mermaid et tableau diagnostique d'affinité aux 6 profils didactiques.
4. **Analyses d'Intégrité & Dynamique Temporelle** : chronométrie fine, stylométrie, tranches de 15 minutes et alertes télémétriques.
5. **Sections Pédagogiques Structurées** :
   - `### 1. Synthèse Globale & Appréciation Pédagogique`
   - `### 2. Bilan Chiffré & Respect des Spécifications`
   - `### 3. Métriques & Qualité du Code`
   - `### 4. Pistes d'Amélioration & Conseils Pratiques`

### 6.4. Génération de Cohortes d'Étalonnage (`generate_session_test.py`)

L'outil [`generate_session_test.py`](file:///Users/lehuen/dev/codelab/client/codelab.files/skills/generate_session_test.py) permet de créer instantanément une promotion de test complète (`Session_test`) comportant 10 étudiants simulés illustrant les différents profils types :
- *Boss* autonome (`boss_expert`)
- *Artisans* studieux (`artisan_regulier`, `artisan_debutant`)
- *Cyborgs* dopés à l'IA (`cyborg_ia`, `cyborg_copier_coller`)
- *Casse-cou* persévérant mais bloqué (`casse_cou_acharne`)
- *Décrocheur* passif (`decrocheur_inactif`)
- *Pirate* par importation externe (`pirate_drop`)
- Profils mixtes et équilibrés (`etudiant_moyen`, `etudiant_hesitant`)

Cet outil sert de banc d'essai pour valider le rendu du double radar, des fenêtres glissantes et de la synthèse de promotion.

---

## 7. Consultation des Résultats

- **Stockage sécurisé** : Les fiches d'évaluation sont enregistrées dans `~/codelab.files/evaluations/` (espace enseignant strictement isolé des postes étudiants).
- **Visualiseur Markdown intégré** : CodeLab inclut un visualiseur Markdown interactif avec rendu HTML5, typographie soignée et formules mathématiques.
- **Explorateur CodeLab** : Basculez en un clic entre vos fichiers de code et vos rapports via le menu *Consulter les évaluations* / *Revenir aux programmes*.
- **Export Scolarité** : Le fichier `notes_session.csv` généré à l'issue d'une session est directement importable dans Moodle ou les logiciels de scolarité (Apogée).

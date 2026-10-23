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
├── recalculate_grades.py   # Utilitaire de recalcul des barèmes hors-ligne
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

```
┌────────────────────────┐     ┌────────────────────────┐     ┌────────────────────────┐
│  1. Dupliquer le skill │ ──> │  2. Définir le sujet   │ ──> │  3. Régler le barème   │
│     (ex. eval-python)  │     │     dans SKILL.md      │     │    dans weights.json   │
└────────────────────────┘     └────────────────────────┘     └────────────────────────┘
                                                                           │
                               ┌────────────────────────┐                  │
                               │  5. Valider à blanc    │ <────────────────┘
                               │  (solution enseignant) │     ┌────────────────────────┐
                               └────────────────────────┘ <── │  4. Coder les tests    │
                                                              │   dans audit_student   │
                                                              └────────────────────────┘
```

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

Le moteur d'évaluation CodeLab adopte une architecture **hybride et éthique** :

### 5.1. Notes 100 % Déterministes & Hors-Ligne
- **Calcul objectif local** : L'analyse syntaxique (AST Python, compilateur GCC/Clang ou Javac), la vérification des critères et le calcul de la note finale sur 20 points sont **exclusivement exécutés en local par Python**.
- **Équité absolue** : La note chiffrée est strictement déterministe et identique avec ou sans activation de l'IA.
- **Intégrité académique** : Les analyses de similarité inter-étudiants (Winnowing / n-grams) et de cohérence temporelle sont calculées localement sans aucun envoi de code à des services tiers.

### 5.2. Rôle de l'IA (Optionnelle)
L'IA intervient uniquement pour la **rédaction pédagogique** :
- À partir du diagnostic factuel chiffré, elle formule des appréciations personnalisées, encourageantes et bienveillantes.
- Elle propose des conseils de remédiation socratiques (explications méthodologiques plutôt que des solutions toutes faites).
- **Mode déconnecté** : En l'absence de clé API ou en cas de coupure réseau, un compte-rendu textuel déterministe est généré sans bloquer l'évaluation.

### 5.3. Configuration & Gestion du Cache

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

## 6. Consultation des Résultats

- **Stockage sécurisé** : Les fiches d'évaluation sont enregistrées dans `~/codelab.files/evaluations/` (espace enseignant strictement isolé des postes étudiants).
- **Visualiseur Markdown intégré** : CodeLab inclut un visualiseur Markdown interactif avec rendu HTML5, typographie soignée et formules mathématiques.
- **Explorateur CodeLab** : Basculez en un clic entre vos fichiers de code et vos rapports via le menu *Consulter les évaluations* / *Revenir aux programmes*.
- **Export Scolarité** : Le fichier `notes_session.csv` généré à l'issue d'une session est directement importable dans Moodle ou les logiciels de scolarité (Apogée).

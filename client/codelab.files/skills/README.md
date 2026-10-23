# Compétences & Évaluation Pédagogique CodeLab

Ce répertoire rassemble le moteur d'évaluation et les compétences (*skills*) pédagogiques pour l'environnement **CodeLab**.

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

> 💡 **Compétences personnalisées** : Les enseignants peuvent dupliquer un exemple (ex. [eval-python-sample](file:///Users/lehuen/dev/codelab/client/codelab.files/skills/eval-python-sample/README.md)) ou créer leurs propres compétences adaptées à leurs travaux pratiques et examens. Elles sont immédiatement opérationnelles sans modifier le code source de CodeLab.

---

## 3. Structure d'une Compétence (*Skill*)

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

## 4. Fonctionnement Hybride & Configuration IA

Le moteur d'évaluation CodeLab adopte une architecture **hybride et éthique** :

### 4.1. Notes 100 % Déterministes & Hors-Ligne
- **Calcul objectif local** : L'analyse syntaxique (AST Python, compilateur GCC/Clang ou Javac), la vérification des critères et le calcul de la note finale sur 20 points sont **exclusivement exécutés en local par Python**.
- **Équité absolue** : La note chiffrée est strictement déterministe et identique avec ou sans activation de l'IA.
- **Intégrité académique** : Les analyses de similarité inter-étudiants (Winnowing / n-grams) et de cohérence temporelle sont calculées localement sans aucun envoi de code à des services tiers.

### 4.2. Rôle de l'IA (Optionnelle)
L'IA intervient uniquement pour la **rédaction pédagogique** :
- À partir du diagnostic factuel chiffré, elle formule des appréciations personnalisées, encourageantes et bienveillantes.
- Elle propose des conseils de remédiation socratiques (explications méthodologiques plutôt que des solutions toutes faites).
- **Mode déconnecté** : En l'absence de clé API ou en cas de coupure réseau, un compte-rendu textuel déterministe est généré sans bloquer l'évaluation.

### 4.3. Configuration & Gestion du Cache

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

## 5. Consultation des Résultats

- **Stockage sécurisé** : Les fiches d'évaluation sont enregistrées dans `~/codelab.files/evaluations/` (espace enseignant strictement isolé des postes étudiants).
- **Visualiseur Markdown intégré** : CodeLab inclut un visualiseur Markdown interactif avec rendu HTML5, typographie soignée et formules mathématiques.
- **Explorateur CodeLab** : Basculez en un clic entre vos fichiers de code et vos rapports via le menu *Consulter les évaluations* / *Revenir aux programmes*.
- **Export Scolarité** : Le fichier `notes_session.csv` généré à l'issue d'une session est directement importable dans Moodle ou les logiciels de scolarité (Apogée).

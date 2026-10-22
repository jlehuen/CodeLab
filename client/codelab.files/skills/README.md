# Compétences & Moteurs d'Évaluation Pédagogique CodeLab

Ce répertoire rassemble les compétences (*skills*) et scripts d'évaluation automatisée assistée par IA pour l'environnement **CodeLab**.

---

## 1. Organisation du Répertoire

```
skills/
├── eval_codelab.py         # Moteur universel CLI & CodeLab (SessionEvaluator)
├── recalculate_grades.py   # Moteur de recalcul dynamique des notes sans réévaluation
├── README.md               # Guide d'architecture et de référence globale (ce fichier)
├── eval-c-sample/          # Exemple générique C (Compilation /6, Architecture /8, Style /4, Tests /2)
│   ├── README.md           # Documentation end-user pour projets C
│   ├── report_template.md  # Gabarit canonique des fiches d'évaluation individuelles
│   ├── SKILL.md            # Spécification formelle du skill
│   ├── weights.json        # Grille de pondération critériée
│   └── scripts/            # Scripts d'analyse (audit_student, audit_cohort, generate_report)
├── eval-python-sample/     # Exemple générique Python (Syntaxe AST /6, Conception /8, Style PEP8 /4, Tests /2)
│   ├── README.md           # Documentation end-user pour projets Python
│   ├── report_template.md  # Gabarit canonique des fiches d'évaluation individuelles
│   ├── SKILL.md            # Spécification formelle du skill
│   ├── weights.json        # Grille de pondération critériée
│   └── scripts/            # Scripts d'analyse (audit_student, audit_cohort, generate_report)
└── eval-java-sample/       # Exemple générique Java (Compilation /6, Modélisation Objet /8, Javadoc /4, Tests /2)
    ├── README.md           # Documentation end-user pour projets Java POO
    ├── report_template.md  # Gabarit canonique des fiches d'évaluation individuelles
    ├── SKILL.md            # Spécification formelle du skill
    ├── weights.json        # Grille de pondération critériée
    └── scripts/            # Scripts d'analyse (audit_student, audit_cohort, generate_report)
```

---

## 2. Découverte Dynamique des Skills au Démarrage

CodeLab scanne automatiquement au lancement le dossier des skills utilisateur situé dans :
`~/codelab.files/skills/`

Chaque sous-dossier contenant un fichier `SKILL.md` est reconnu comme une compétence d'évaluation disponible.
CodeLab extrait son identifiant (`name`), sa description et son titre pour alimenter dynamiquement les sous-menus d'évaluation :
- Dans le gestionnaire de fichiers (`FileManager`) : menu contextuel **Évaluer ce dossier**
- Dans la table de supervision des étudiants (`UserTable`) : menus contextuels **Évaluer <étudiant>** et **Évaluer toute la session**

Les enseignants peuvent ainsi ajouter leurs propres compétences privées (ex : examens, TPs spécifiques d'une filière) directement dans leur dossier personnel `~/codelab.files/skills/` sans impacter le dépôt public.

---

## 3. Contrat d'Interface Standardisé (`scripts/`)

Chaque compétence d'évaluation implémente les points d'entrée suivants dans son sous-dossier `scripts/` :

| Script / Fonction | Rôle & Signature |
| :--- | :--- |
| `audit_student.py` : `audit_student(folder_path, sessions_xml_path=None)` | Analyse statique et dynamique du dossier étudiant, étalonnage de progression, détection multi-critères d'usage d'IA (écosystème, inclusions interdites, chronométrie, stylométrie). |
| `generate_report.py` : `generate_individual_report(folder_path, output_path, ...)` | Produit une fiche d'évaluation individuelle structurée en Markdown avec Cartouche de Traçabilité, indice d'authenticité et tableau récapitulatif. |
| `audit_cohort.py` : `analyze_cohort(session_dir)` / `export_csv(...)` | Traitement par lot d'une session complète, détection de copier-collé inter-étudiants par n-grams / Winnowing (`plagiarism_matrix.csv`, `rapport_plagiat.md`), export CSV pour scolarité (Moodle / Apogée) et bilan global. |

---

## 4. Configuration, Clés d'API IA & Coût d'Analyse

Le moteur `eval_codelab.py` lit sa configuration directement dans le fichier unifié :
`~/codelab.files/userdata/user.properties` (avec rétro-compatibilité `ai.properties`).

- **`GEMINI_API_KEY`** : Clé d'API Google Gemini (obtenue gratuitement sur Google AI Studio).
- **`GEMINI_MODEL`** : Modèle recommandé par défaut : `gemini-3.5-flash-lite` (ou `gemini-3.5-flash`).
- **`AI_CACHE`** : Cache local des fiches pédagogiques pour éviter les requêtes redondantes (`userdata/ai_cache/`).
- **Fallback Déterministe** : Si aucune clé n'est renseignée ou en cas de coupure réseau, le moteur bascule automatiquement sur les générateurs déterministes locaux sans bloquer l'interface.

### 4.1. Fonctionnement Hybride & Intégrité des Notes
- **Calcul 100 % Déterministe Hors-Ligne** : L'analyse statique/AST, la vérification syntaxique Clang (`-fsyntax-only`), le contrôle des jalons et le barème sur 20 points ($8 + 8 + 2 + 2$) sont calculés localement par Python. **La note finale est strictement identique avec ou sans IA**.
- **Calibration Réaliste des Notes** : Plafonnements stricts selon l'état d'avancement pour éliminer les notes de complaisance (un élève débutant bloqué au TP4 est plafonné autour de 10-11/20, sans dévaloriser les élèves autonomes atteignant 17-19/20).
- **Détection Multi-Critères d'Usage d'IA** : Détection 100% hors-ligne sur 5 dimensions objectives (incohérence d'écosystème comme `<stdio.h>`, chronométrie anormale, stylométrie ChatGPT) avec indice d'authenticité consigné dans le rapport.
- **Détection de Copier-Collé & Plagiat Inter-Étudiants** : Matrice croisée de similarité par tokens n-grams et algorithme Winnowing (`plagiarism_matrix.csv` et alertes `rapport_plagiat.md`).
- **Rôle de l'IA** : L'appel à Gemini est purement rédactionnel. À partir du diagnostic JSON factuel pré-calculé, l'IA formule des appréciations personnalisées, des encouragements bienveillants et des pistes d'auto-remédiation socratiques (expliquant le *pourquoi* des anti-patrons plutôt que de livrer la solution brute).
- **Synthèse de Cohorte & Export CSV Apogée/Moodle** : Générés 100 % hors-ligne par `audit_cohort.py` (0 requête API, exclusion stricte des comptes tuteurs/enseignants).

### 4.2. Coût en Requêtes et Tokens d'une Évaluation de Session
Le code source brut complet n'est pas envoyé à l'IA ; seul le diagnostic JSON compact (jalons, scores fixés, 2 ou 3 extraits ciblés de quelques lignes) est transmis :

| Flux / Traitement | Volume / Ratio |
| :--- | :--- |
| **Synthèse globale & CSV de session** | **0 requête** *(100 % local et déterministe)* |
| **Comptes tuteurs / enseignants** | **0 requête** *(automatiquement exclus)* |
| **Par étudiant** | **1 requête unique** (~1 500 tokens d'entrée, ~1 000 tokens de sortie = **~2 500 tokens**) |
| **Groupe TP (35 étudiants)** | **35 requêtes** (~87 500 tokens au total) $\rightarrow$ **0,00 €** *(Free Tier)* ou **~0,01 €** |
| **Promotion complète (100 étudiants)** | **100 requêtes** (~250 000 tokens au total) $\rightarrow$ **0,00 €** *(Free Tier)* ou **~0,04 €** |

> 💡 **Optimisations économiques et écologiques intégrées** :
> 1. **Palier gratuit Google AI Studio** : Le quota gratuit (15 requêtes/min, 1 500 requêtes/jour) couvre intégralement plusieurs promotions complètes sans frais (**0,00 €**).
> 2. **Cache local persistant (`AI_CACHE`)** : Chaque fiche rédigée est indexée par empreinte SHA-256 dans `userdata/ai_cache/`. Réévaluer une session ou un étudiant sans modification de code consomme **0 requête et 0 token**.

### 4.3. Gestion et Cycle de Vie du Cache Local (`userdata/ai_cache/`)

Le dossier `~/codelab.files/userdata/ai_cache/` stocke les fiches générées sous forme d'empreintes **SHA-256** combinant le modèle et le texte exact du prompt envoyé (`SHA-256(modèle + ":::" + prompt)`).

#### Quand le cache s'invalide-t-il automatiquement ? (Zéro action requise)
- **Le code de l'élève a changé** : dès que l'étudiant modifie un fichier, le diagnostic JSON change, ce qui modifie le prompt et produit une nouvelle clé SHA-256. L'IA est alors automatiquement interrogée sans qu'il soit nécessaire de vider le cache.
- **Le modèle a changé** : passer par exemple de `gemini-3.5-flash-lite` à `gemini-3.5-flash` recalcule une clé différente et déclenche de nouvelles requêtes.

#### Quand faut-il vider manuellement le dossier `ai_cache/` ?
1. **Modification des consignes pédagogiques ou du prompt du skill** : Si vous avez modifié le texte du prompt dans `eval_codelab.py` (ou les barèmes d'un skill) mais que **le code des élèves n'a pas bougé**, le cache renverra l'ancienne rédaction. Il faut vider le cache pour que la nouvelle consigne soit appliquée à toute la promo.
2. **Besoin d'une nouvelle proposition rédactionnelle** : La température étant à 0.3, chaque tirage offre de légères variations. Vider le fichier correspondant force l'IA à reformuler son appréciation.
3. **Grand nettoyage de fin d'année / changement de promotion** : Pour repartir sur un environnement vierge entre deux années universitaires.

#### Comment le vider ou le désactiver :
- **Vider le cache en ligne de commande** :
  ```bash
  rm -rf ~/codelab.files/userdata/ai_cache/*
  ```
- **Désactiver le cache sans rien effacer** :
  Dans `~/codelab.files/userdata/user.properties` (ou `ai.properties`) :
  ```properties
  AI_CACHE = false
  ```

---

## 5. Stockage Confidentiel & Visualisation des Comptes-Rendus

- **Emplacement permanent & confidentiel** :
  Toutes les évaluations sont centralisées dans `~/codelab.files/evaluations/` (`local/` et `<sessionId>/`). Elles ne disparaissent pas à la déconnexion et sont rigoureusement exclues de toute synchronisation réseau avec les étudiants.
- **Bascule instantanée dans l'explorateur** :
  Depuis le menu contextuel de l'arborescence (`FileManager`) :
  - En mode programmes : `📊 Consulter les évaluations`
  - En mode évaluations : `📁 Revenir aux programmes`
- **Visualiseur Markdown natif** :
  Le compte-rendu généré s'affiche automatiquement dans CodeLab avec :
  - Bascule dynamique code source / rendu HTML5 (`ToolBarMarkdown`)
  - Typographie Inter et style CodeLab (`markdown.css` éditable depuis le menu Éditeur de code)
  - Formules mathématiques $\LaTeX$ vectorielles intégrées
  - Tableau récapitulatif aéré et conseils méthodologiques socratiques
  - Mode consultation sécurisé (boutons RUN et Nouveau désactivés, zéro freeze au double-clic)

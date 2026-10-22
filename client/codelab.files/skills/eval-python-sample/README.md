# Skill d'Évaluation : Python / Algorithmique (Exemple Générique)

> **Langage** : Python 3.9+  
> **Analyseur requis** : Module standard Python `ast` (zéro dépendance externe lourde)  
> **Environnement** : CodeLab v1.5.0+  
> **Type de compétence** : Modèle générique d'évaluation pour tout projet ou TP en Python  

---

## 1. Présentation & Philosophie du Skill

Le skill **`eval-python-sample`** est une compétence générique fournie avec CodeLab pour auditer et évaluer automatiquement des scripts et des algorithmes en langage Python.

Conçu pour être simple, portable et robuste, il utilise exclusivement le module standard `ast` (Abstract Syntax Tree) de Python pour inspecter la structure du code source sans nécessiter l'exécution directe des programmes, ce qui garantit une sécurité et une rapidité optimales lors de l'évaluation de cohortes entières.

### Caractéristiques principales :
- **Analyse syntaxique pure via l'AST** : Détecte les erreurs de syntaxe, les fonctions déclarées, les arguments, les boucles et les compréhensions sans exécuter de code non fiable.
- **Vérification du style Pythonique (PEP 8)** : Évalue la concision, la présence de docstrings, l'usage de compréhensions et de gestionnaires de contexte `with`.
- **Pondérations éditables sans codage** : Les barèmes et seuils sont entièrement paramétrables dans le fichier `weights.json`.
- **Intégrité académique native** : Intègre les trois piliers de vérification (Chronométrie, Stylométrie d'indentation, Détection de plagiat inter-étudiants).

---

## 2. La Grille d'Évaluation Critériée (Barème sur 20 points)

Par défaut, l'évaluation repose sur une répartition équilibrée en 4 axes :

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BARÈME TOTAL : 20 POINTS                        │
├───────────────────────────────────┬────────────────────────────────────┤
│ Axe 1 : Syntaxe & Arbre AST       │ Axe 2 : Conception & Modularité    │
│ (6,0 pts - 30 %)                  │ (8,0 pts - 40 %)                   │
├───────────────────────────────────┼────────────────────────────────────┤
│ Axe 3 : Style Pythonique & PEP 8  │ Axe 4 : Démarche de Test           │
│ (4,0 pts - 20 %)                  │ (2,0 pts - 10 %)                   │
└───────────────────────────────────┴────────────────────────────────────┘
```

| Axe d'Évaluation | Barème | Critères & Indicateurs Observés |
| :--- | :---: | :--- |
| **Axe 1 : Syntaxe & Arbre AST** | **6,0 pts** | • Validité syntaxique complète de tous les fichiers `.py`.<br>• Capacité du parseur AST à construire l'arbre de syntaxe abstraite.<br>• Diagnostic précis des erreurs de syntaxe (`SyntaxError`, `IndentationError`). |
| **Axe 2 : Conception & Modularité** | **8,0 pts** | • Décomposition modulaire en fonctions (`def`).<br>• Refus du script séquentiel "spaghetti" sans encapsulation fonctionnelle.<br>• Nombre de fonctions écrites et cohérence du passage de paramètres.<br>• Valeurs de retour explicites (`return`) au lieu de simples effets de bord `print`. |
| **Axe 3 : Style Pythonique & PEP 8** | **4,0 pts** | • Documentation formalisée des fonctions et modules via des docstrings (`""" ... """`).<br>• Utilisation d'idiomes Python modernes (compréhensions de listes/dicos, `with open(...)`).<br>• Respect des conventions de nommage PEP 8 (`snake_case` pour fonctions et variables).<br>• Indentation régulière et absence d'anti-patterns impératifs. |
| **Axe 4 : Démarche de Test** | **2,0 pts** | • Présence d'assertions de validation (`assert ...`).<br>• Fonctions de test dédiées (ex: `test_algo()`, `test_solution()`).<br>• Scripts de test séparés (`test_*.py`) ou doctests. |

> [!TIP]
> **Personnalisation du barème (`weights.json`)** :  
> Vous pouvez modifier les poids des 4 axes (ex: passer à 8+8+2+2 ou 10+8+2) directement dans `weights.json` à la racine du skill ou dans le sous-dossier d'évaluation de votre session.

---

## 3. Les Trois Piliers d'Intégrité Académique

Ce skill intègre un moteur de contrôle garantissant l'authenticité des travaux étudiants :

### 1. ⏱️ Analyse Chronométrique
- Mesure l'étalement des séances de développement sur le serveur CodeLab.
- Détecte l'apparition instantanée de codes volumineux en moins de 60 secondes (indicateur de copier-coller ou de génération IA non assimilée).

### 2. 📐 Stylométrie d'Indentation & Détection IA
- Vérifie l'homogénéité du style d'indentation : respect des tabulations de l'éditeur CodeLab vs insertions sauvages de blocs indentés en 4 espaces provenant d'Internet.
- Détecte les marqueurs typiques des LLM (ChatGPT/Claude) : commentaires didactiques formatés, annotations de types complexes (`typing`), imports interdits (`sys`, `os`, `subprocess`).

### 3. 👥 Détection de Copier-Collé Inter-Étudiants (Plagiat)
- Analyse croisée de l'ensemble de la promotion par n-grams syntaxiques (algorithme de Jaccard normalisé).
- Détecte les similitudes structurelles même après renommage des variables ou réorganisation de surface.
- Identifie le binôme suspect et liste les fichiers présentant un taux de similarité élevé ($\ge 75\%$).

---

## 4. Format du Rapport et Gabarit Markdown

L'évaluation génère un compte-rendu individuel Markdown conforme au gabarit [report_template.md](file:///Users/lehuen/codelab.files/skills/eval-python-sample/report_template.md) :
1. **En-tête & Cartouche de Traçabilité** : Date, version du skill, moteur utilisé, synthèse des 3 piliers d'intégrité.
2. **Tableau Récapitulatif des Notes Calibrées** : Restitution stricte des notes obtenues par axe avec barème officiel.
3. **Analyses d'Intégrité Pédagogique** : Détail de la chronométrie, stylométrie et contrôle de similarités.
4. **Sections Pédagogiques** :
   - `### Bilan de Progression & Analyse Pédagogique`
   - `### Points Forts Observés` (avec extraits de code Python dans des blocs ```python ... ```)
   - `### Axes d'Amélioration & Conseils Méthodologiques`
   - `### Commentaire de l'Enseignant`

---

## 5. Guide d'Utilisation en Ligne de Commande (CLI)

### Utilisation avec le Moteur Unifié (`eval_codelab.py`)
```bash
# Évaluer une copie Python individuelle
python3 ~/codelab.files/skills/eval_codelab.py \
  --folder <chemin_dossier_etudiant> \
  --skill eval-python-sample \
  --out evaluations/<login>_rapport.md

# Évaluer une promotion complète
python3 ~/codelab.files/skills/eval_codelab.py \
  --session Session_Python_L1 \
  --dir <repertoire_session> \
  --skill eval-python-sample
```

### Utilisation des Scripts Autonomes du Skill
```bash
# 1. Audit technique et sortie JSON
python3 ~/codelab.files/skills/eval-python-sample/scripts/audit_student.py <chemin_dossier_etudiant>

# 2. Génération directe de la fiche individuelle
python3 ~/codelab.files/skills/eval-python-sample/scripts/generate_report.py <chemin_dossier_etudiant> <fichier_sortie.md>

# 3. Audit global d'une cohorte avec matrice de similarité
python3 ~/codelab.files/skills/eval-python-sample/scripts/audit_cohort.py <chemin_dossier_cohorte> \
  --csv evaluations/notes.csv \
  --md evaluations/synthese.md
```

---

## 6. Arborescence du Skill

```
eval-python-sample/
├── SKILL.md                          # Définition du skill pour Antigravity
├── README.md                         # Documentation end-user (ce fichier)
├── report_template.md                # Gabarit Markdown pour les rapports individuels
├── weights.json                      # Paramétrage des pondérations et plafonds
├── references/
│   └── grille_evaluation.md          # Guide méthodologique de la grille critériée
└── scripts/
    ├── audit_student.py              # Analyse statique AST et extraction de métriques
    ├── generate_report.py            # Générateur de fiches Markdown individuelles
    └── audit_cohort.py               # Analyseur de promotion et détection de plagiat
```

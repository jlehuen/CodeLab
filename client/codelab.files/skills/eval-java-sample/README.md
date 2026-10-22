# Skill d'Évaluation : Java / POO (Exemple Générique)

> **Langage** : Java (JDK 8 à 21+)  
> **Compilateur requis** : `javac` standard  
> **Environnement** : CodeLab v1.5.0+  
> **Type de compétence** : Modèle générique d'évaluation pour tout projet ou TP en Java / Programmation Orientée Objet  

---

## 1. Présentation & Philosophie du Skill

Le skill **`eval-java-sample`** est une compétence générique fournie avec CodeLab pour auditer et évaluer automatiquement des projets d'apprentissage en langage Java.

Conçu pour les modules d'initiation et de consolidation de la **Programmation Orientée Objet (POO)**, il inspecte la structure du code source, vérifie la compilation sans nécessiter de configuration lourde (Maven/Gradle) et analyse l'application des principes fondamentaux de conception objet : encapsulation, modularité, documentation Javadoc et tests.

### Caractéristiques principales :
- **Contrôle de compilation Java natif** : Vérification de la validité syntaxique de l'ensemble des fichiers `.java` via `javac`.
- **Audit de modélisation objet** : Analyse du ratio de classes, interfaces, méthodes d'instance et respect strict de l'encapsulation (`private`).
- **Inspection Javadoc formalisée** : Vérifie la documentation des classes et méthodes (`/** ... */`).
- **Pondérations éditables sans codage** : Les barèmes et seuils sont entièrement paramétrables dans le fichier `weights.json`.
- **Intégrité académique native** : Intègre les trois piliers de vérification (Chronométrie, Stylométrie d'indentation, Détection de plagiat inter-étudiants).

---

## 2. La Grille d'Évaluation Critériée (Barème sur 20 points)

Par défaut, l'évaluation repose sur une répartition équilibrée en 4 axes :

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BARÈME TOTAL : 20 POINTS                        │
├───────────────────────────────────┬────────────────────────────────────┤
│ Axe 1 : Compilation Java          │ Axe 2 : Modélisation Objet &       │
│ (6,0 pts - 30 %)                  │ Encapsulation (8,0 pts - 40 %)     │
├───────────────────────────────────┼────────────────────────────────────┤
│ Axe 3 : Javadoc & Conventions     │ Axe 4 : Démarche de Test           │
│ (4,0 pts - 20 %)                  │ (2,0 pts - 10 %)                   │
└───────────────────────────────────┴────────────────────────────────────┘
```

| Axe d'Évaluation | Barème | Critères & Indicateurs Observés |
| :--- | :---: | :--- |
| **Axe 1 : Compilation Java** | **6,0 pts** | • Compilation complète et sans erreur fatale des fichiers `.java` via `javac`.<br>• Respect des règles de nommage des fichiers correspondant aux classes publiques.<br>• Diagnostic précis des erreurs de compilation. |
| **Axe 2 : Modélisation Objet & Encapsulation** | **8,0 pts** | • Décomposition en classes autonomes et interfaces métier.<br>• **Encapsulation stricte** : tous les attributs doivent être `private` (zéro attribut public).<br>• Présence de constructeurs explicites et de méthodes d'instance adaptées.<br>• Séparation claire des responsabilités entre les entités. |
| **Axe 3 : Javadoc & Conventions Java** | **4,0 pts** | • Présence de commentaires Javadoc formels (`/** ... */`) sur classes et méthodes.<br>• Respect des conventions de nommage Java (`PascalCase` pour les classes, `camelCase` pour méthodes et attributs).<br>• Indentation régulière et propreté du code source. |
| **Axe 4 : Démarche de Test** | **2,0 pts** | • Présence de classes de test dédiées (ex: `TestCompte.java`, `TestBanque.java`).<br>• Méthodes de test ou assertions de validation (`assert` ou tests unitaires). |

> [!TIP]
> **Personnalisation du barème (`weights.json`)** :  
> Vous pouvez modifier les poids des 4 axes (ex: passer à 8+8+2+2 ou 10+8+2) directement dans `weights.json` à la racine du skill ou dans le sous-dossier d'évaluation de votre session.

---

## 3. Les Trois Piliers d'Intégrité Académique

Ce skill intègre un moteur de contrôle garantissant l'authenticité des travaux étudiants :

### 1. ⏱️ Analyse Chronométrique
- Mesure l'étalement des séances de développement sur le serveur CodeLab.
- Détecte l'apparition instantanée de classes complètes en moins de 60 secondes (indicateur de copier-coller ou de génération IA non assimilée).

### 2. 📐 Stylométrie d'Indentation & Détection IA
- Vérifie l'homogénéité du style d'indentation : respect des tabulations de l'éditeur CodeLab vs insertions sauvages de blocs indentés en 4 espaces provenant d'Internet.
- Détecte les marqueurs typiques des LLM (ChatGPT/Claude) : constructions Java 8+ hors programme d'initiation (`Streams`, lambdas complexes, inférence `var`), introspection `java.lang.reflect`, imports réseau non justifiés.

### 3. 👥 Détection de Copier-Collé Inter-Étudiants (Plagiat)
- Analyse croisée de l'ensemble de la promotion par n-grams syntaxiques (algorithme de Jaccard normalisé).
- Détecte les similitudes structurelles même après renommage des classes et attributs.
- Identifie le binôme suspect et liste les fichiers présentant un taux de similarité élevé ($\ge 75\%$).

---

## 4. Format du Rapport et Gabarit Markdown

L'évaluation génère un compte-rendu individuel Markdown conforme au gabarit [report_template.md](file:///Users/lehuen/codelab.files/skills/eval-java-sample/report_template.md) :
1. **En-tête & Cartouche de Traçabilité** : Date, version du skill, moteur utilisé, synthèse des 3 piliers d'intégrité.
2. **Tableau Récapitulatif des Notes Calibrées** : Restitution stricte des notes obtenues par axe avec barème officiel.
3. **Analyses d'Intégrité Pédagogique** : Détail de la chronométrie, stylométrie et contrôle de similarités.
4. **Sections Pédagogiques** :
   - `### Bilan de Progression & Analyse Pédagogique`
   - `### Points Forts Observés` (avec extraits de code Java dans des blocs ```java ... ```)
   - `### Axes d'Amélioration & Conseils Méthodologiques`
   - `### Commentaire de l'Enseignant`

---

## 5. Guide d'Utilisation en Ligne de Commande (CLI)

### Utilisation avec le Moteur Unifié (`eval_codelab.py`)
```bash
# Évaluer une copie Java individuelle
python3 ~/codelab.files/skills/eval_codelab.py \
  --folder <chemin_dossier_etudiant> \
  --skill eval-java-sample \
  --out evaluations/<login>_rapport.md

# Évaluer une promotion complète
python3 ~/codelab.files/skills/eval_codelab.py \
  --session Session_Java_L2 \
  --dir <repertoire_session> \
  --skill eval-java-sample
```

### Utilisation des Scripts Autonomes du Skill
```bash
# 1. Audit technique et sortie JSON
python3 ~/codelab.files/skills/eval-java-sample/scripts/audit_student.py <chemin_dossier_etudiant>

# 2. Génération directe de la fiche individuelle
python3 ~/codelab.files/skills/eval-java-sample/scripts/generate_report.py <chemin_dossier_etudiant> <fichier_sortie.md>

# 3. Audit global d'une cohorte avec matrice de similarité
python3 ~/codelab.files/skills/eval-java-sample/scripts/audit_cohort.py <chemin_dossier_cohorte> \
  --csv evaluations/notes.csv \
  --md evaluations/synthese.md
```

---

## 6. Arborescence du Skill

```
eval-java-sample/
├── SKILL.md                          # Définition du skill pour Antigravity
├── README.md                         # Documentation end-user (ce fichier)
├── report_template.md                # Gabarit Markdown pour les rapports individuels
├── weights.json                      # Paramétrage des pondérations et plafonds
├── references/
│   └── grille_evaluation.md          # Guide méthodologique de la grille critériée
└── scripts/
    ├── audit_student.py              # Analyse statique javac et extraction de métriques
    ├── generate_report.py            # Générateur de fiches Markdown individuelles
    └── audit_cohort.py               # Analyseur de promotion et détection de plagiat
```

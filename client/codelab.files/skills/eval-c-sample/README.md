# Skill d'Évaluation : C / Algorithmique (Exemple Générique)

> **Langage** : C (normes C99 / C11 / GNU C)  
> **Compilateur requis** : Clang ou GCC (mode `-fsyntax-only`)  
> **Environnement** : CodeLab v1.5.0+  
> **Type de compétence** : Modèle générique d'évaluation pour tout projet ou TP en C  

---

## 1. Présentation & Philosophie du Skill

Le skill **`eval-c-sample`** est une compétence générique fournie avec CodeLab pour évaluer automatiquement des programmes et algorithmes écrits en langage C. Conçu pour être réutilisable dans divers contextes pédagogiques (TP d'initiation, projets de première année, travaux dirigés sur machine), il analyse la qualité logicielle et la rigueur algorithmique sans dépendre d'un sujet imposé particulier.

### Caractéristiques principales :
- **Contrôle de compilation non-intrusif** : Analyse syntaxique rigoureuse via `clang -fsyntax-only` (ou `gcc -fsyntax-only`), sans exécution de binaire potentiellement risqué.
- **Tolérance universitaire standard** : L'écriture `void main()` est expressément tolérée sans malus (`-Wno-main-return-type -Wno-main`).
- **Pondérations éditables sans codage** : Les barèmes et seuils sont entièrement paramétrables dans le fichier `weights.json`.
- **Intégrité académique native** : Intègre les trois piliers de vérification (Chronométrie, Stylométrie d'indentation, Détection de plagiat inter-étudiants).

---

## 2. La Grille d'Évaluation Critériée (Barème sur 20 points)

Par défaut, l'évaluation repose sur une répartition équilibrée en 4 axes :

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BARÈME TOTAL : 20 POINTS                        │
├───────────────────────────────────┬────────────────────────────────────┤
│ Axe 1 : Compilation & Syntaxe     │ Axe 2 : Architecture & Conception  │
│ (6,0 pts - 30 %)                  │ (8,0 pts - 40 %)                   │
├───────────────────────────────────┼────────────────────────────────────┤
│ Axe 3 : Style & Lisibilité        │ Axe 4 : Démarche de Test           │
│ (4,0 pts - 20 %)                  │ (2,0 pts - 10 %)                   │
└───────────────────────────────────┴────────────────────────────────────┘
```

| Axe d'Évaluation | Barème | Critères & Indicateurs Observés |
| :--- | :---: | :--- |
| **Axe 1 : Compilation & Syntaxe** | **6,0 pts** | • Ratio de fichiers `.c` et `.h` compilables sans erreur.<br>• Détection et diagnostic précis des erreurs de syntaxe Clang.<br>• Pénalité progressive par fichier non compilable. |
| **Axe 2 : Architecture & Conception** | **8,0 pts** | • Décomposition modulaire en fonctions spécialisées.<br>• Refus de la fonction `main()` monolithique de 200 lignes.<br>• Passage explicite de paramètres et valeurs de retour pertinentes.<br>• Présence de prototypes et modularité inter-fichiers. |
| **Axe 3 : Style & Lisibilité** | **4,0 pts** | • Clarté et cohérence du nommage des identifiants (`camelCase` ou `snake_case`).<br>• Définition des constantes symboliques en `MAJUSCULES` (`#define` ou `const`).<br>• Indentation régulière et proportion de commentaires explicatifs utiles.<br>• Absence de constructions archaïques (ex: saut inconditionnel `goto`). |
| **Axe 4 : Démarche de Test** | **2,0 pts** | • Présence d'assertions de validation (`assert(...)` via `<assert.h>`).<br>• Fonctions de test dédiées (ex: `test_calcul()`, `test_moteur()`).<br>• Présence de fichiers de test unitaires séparés (`test_*.c`). |

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
- Détecte les marqueurs typiques des LLM (ChatGPT/Claude) : commentaires ultra-détaillés en français formaté ou en anglais artificiel, bibliothèques non enseignées, constructions anachroniques pour un débutant.

### 3. 👥 Détection de Copier-Collé Inter-Étudiants (Plagiat)
- Analyse croisée de l'ensemble de la promotion par n-grams syntaxiques (algorithme de Jaccard normalisé).
- Détecte les similitudes structurelles même après renommage des variables ou réorganisation de surface.
- Identifie le binôme suspect et liste les fichiers présentant un taux de similarité élevé ($\ge 75\%$).

---

## 4. Format du Rapport et Gabarit Markdown

L'évaluation génère un compte-rendu individuel Markdown conforme au gabarit [report_template.md](file:///Users/lehuen/codelab.files/skills/eval-c-sample/report_template.md) :
1. **En-tête & Cartouche de Traçabilité** : Date, version du skill, moteur utilisé, synthèse des 3 piliers d'intégrité.
2. **Tableau Récapitulatif des Notes Calibrées** : Restitution stricte des notes obtenues par axe avec barème officiel.
3. **Analyses d'Intégrité Pédagogique** : Détail de la chronométrie, stylométrie et contrôle de similarités.
4. **Sections Pédagogiques** :
   - `### Bilan de Progression & Analyse Pédagogique`
   - `### Points Forts Observés` (avec extraits de code C dans des blocs ```c ... ```)
   - `### Axes d'Amélioration & Conseils Méthodologiques`
   - `### Commentaire de l'Enseignant`

---

## 5. Guide d'Utilisation en Ligne de Commande (CLI)

### Utilisation avec le Moteur Unifié (`eval_codelab.py`)
```bash
# Évaluer une copie C individuelle
python3 ~/codelab.files/skills/eval_codelab.py \
  --folder <chemin_dossier_etudiant> \
  --skill eval-c-sample \
  --out evaluations/<login>_rapport.md

# Évaluer une promotion complète
python3 ~/codelab.files/skills/eval_codelab.py \
  --session Session_C_L1 \
  --dir <repertoire_session> \
  --skill eval-c-sample
```

### Utilisation des Scripts Autonomes du Skill
```bash
# 1. Audit technique et sortie JSON
python3 ~/codelab.files/skills/eval-c-sample/scripts/audit_student.py <chemin_dossier_etudiant>

# 2. Génération directe de la fiche individuelle
python3 ~/codelab.files/skills/eval-c-sample/scripts/generate_report.py <chemin_dossier_etudiant> <fichier_sortie.md>

# 3. Audit global d'une cohorte avec matrice de similarité
python3 ~/codelab.files/skills/eval-c-sample/scripts/audit_cohort.py <chemin_dossier_cohorte> \
  --csv evaluations/notes.csv \
  --md evaluations/synthese.md
```

---

## 6. Arborescence du Skill

```
eval-c-sample/
├── SKILL.md                          # Définition du skill pour Antigravity
├── README.md                         # Documentation end-user (ce fichier)
├── report_template.md                # Gabarit Markdown pour les rapports individuels
├── weights.json                      # Paramétrage des pondérations et plafonds
├── references/
│   └── grille_evaluation.md          # Guide méthodologique de la grille critériée
└── scripts/
    ├── audit_student.py              # Analyse statique Clang et extraction de métriques
    ├── generate_report.py            # Générateur de fiches Markdown individuelles
    └── audit_cohort.py               # Analyseur de promotion et détection de plagiat
```

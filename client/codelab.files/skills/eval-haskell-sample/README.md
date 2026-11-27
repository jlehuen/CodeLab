# Skill d'Évaluation : Haskell / Programmation Fonctionnelle (Exemple Générique)

> **Langage** : Haskell 2010 / GHC 9+ (fichiers `.hs` et carnets `.ipynb` Haskell)  
> **Compilateur requis** : `ghc` (avec repli sur une analyse syntaxique statique si `ghc` est absent)  
> **Environnement** : CodeLab v1.5.5+  
> **Type de compétence** : Modèle générique d'évaluation pour tout projet ou TP en Haskell  

---

## 1. Présentation & Philosophie du Skill

Le skill **`eval-haskell-sample`** est une compétence générique fournie avec CodeLab pour auditer et évaluer automatiquement des programmes et carnets interactifs écrits en langage Haskell.

Conçu pour accompagner l'apprentissage de la programmation fonctionnelle (typage statique fort, pureté, récursivité, filtrage par motif, fonctions d'ordre supérieur), il combine la vérification de types et les avertissements de `ghc -fno-code -Wall` avec une analyse statique dédiée des idiomes fonctionnels, sans nécessiter l'exécution de code non fiable.

### Caractéristiques principales :
- **Vérification de types & compilation sans exécutable** : Utilise `ghc -fno-code -Wall` pour isoler les erreurs de syntaxe/typage et les avertissements d'exhaustivité (`-Wincomplete-patterns`, `-Wunused-binds`).
- **Support natif des carnets `.ipynb` Haskell** : Extrait et analyse automatiquement les cellules de code des notebooks Haskell de CodeLab en plus des fichiers `.hs`.
- **Audit de conception fonctionnelle** : Détecte les signatures explicites (`::`), les définitions récursives, le filtrage par motif, les gardes (`|`), les types algébriques (`data`/`newtype`/`type`), les fonctions d'ordre supérieur et les anti-patterns courants (`head`/`tail` partiels, `if c then True else False`).
- **Intégrité académique native** : Intègre les trois piliers de vérification (Chronométrie, Stylométrie d'indentation, Détection de plagiat inter-étudiants et marqueurs IA fonctionnels).

---

## 2. La Grille d'Évaluation Critériée (Barème sur 20 points)

Par défaut, l'évaluation repose sur une répartition équilibrée en 4 axes :

```
┌────────────────────────────────────────────────────────────────────────┐
│                        BARÈME TOTAL : 20 POINTS                        │
├───────────────────────────────────┬────────────────────────────────────┤
│ Axe 1 : Compilation & Typage GHC  │ Axe 2 : Conception Fonctionnelle   │
│ (6,0 pts - 30 %)                  │ (8,0 pts - 40 %)                   │
├───────────────────────────────────┼────────────────────────────────────┤
│ Axe 3 : Style & Exhaustivité      │ Axe 4 : Démarche de Test           │
│ (4,0 pts - 20 %)                  │ (2,0 pts - 10 %)                   │
└───────────────────────────────────┴────────────────────────────────────┘
```

| Axe d'Évaluation | Barème | Critères & Indicateurs Observés |
| :--- | :---: | :--- |
| **Axe 1 : Compilation & Typage GHC** | **6,0 pts** | • Compilation et vérification de types sans erreur (`ghc -fno-code`) de tous les fichiers `.hs` et carnets `.ipynb` Haskell.<br>• Repli sur une vérification syntaxique statique si `ghc` n'est pas installé. |
| **Axe 2 : Conception Fonctionnelle** | **8,0 pts** | • Décomposition en fonctions pures avec signatures de types explicites (`::`).<br>• Utilisation du filtrage par motif (*pattern matching*), des gardes (`|`) et de la récursivité.<br>• Emploi de fonctions d'ordre supérieur (`map`, `filter`, `foldr`, `foldl`, composition `.`) et/ou de types algébriques (`data`, `newtype`, `type`). |
| **Axe 3 : Style & Exhaustivité** | **4,0 pts** | • Absence de warnings `-Wall` (motifs incomplets, variables inutilisées).<br>• Commentaires utiles (`--`, `{- -}`) ou documentation Haddock (`-- |`).<br>• Absence d'anti-patterns débutants (`head`/`tail` partiels non protégés, `if cond then True else False`). |
| **Axe 4 : Démarche de Test** | **2,0 pts** | • Présence de fonctions de test (`test*`), de propriétés (`prop_*` / QuickCheck), d'assertions ou d'un `main` vérifiant les cas limites. |

> [!TIP]
> **Personnalisation du barème (`weights.json`)** :  
> Vous pouvez modifier les poids des 4 axes directement dans `weights.json` à la racine du skill ou dans le sous-dossier d'évaluation de votre session.

---

## 3. Les Trois Piliers d'Intégrité Académique

Ce skill intègre un moteur de contrôle garantissant l'authenticité des travaux étudiants :

### 1. ⏱️ Analyse Chronométrique
- Mesure l'étalement des séances de développement sur le serveur CodeLab.
- Détecte l'apparition instantanée de codes volumineux en moins de 60 secondes.

### 2. 📐 Stylométrie d'Indentation & Détection IA
- Vérifie la régularité de l'indentation et la cohérence stylistique entre fichiers.
- Détecte les marqueurs typiques des LLM en Haskell : pragmas d'extensions avancées (`{-# LANGUAGE ... #-}`), bibliothèques hors programme (`Control.Monad.Trans`, `Control.Lens`, `System.IO.Unsafe`), commentaires didactiques formatés de chatbot.

### 3. 👥 Détection de Copier-Collé Inter-Étudiants (Plagiat)
- Analyse croisée de l'ensemble de la promotion par n-grams syntaxiques (algorithme de Jaccard normalisé).
- Détecte les similitudes structurelles même après renommage des identifiants (>= 75 %).

---

## 4. Format du Rapport et Gabarit Markdown

L'évaluation génère un compte-rendu individuel Markdown conforme au gabarit [report_template.md](report_template.md) :
1. **En-tête & Cartouche de Traçabilité** : Date, version du skill, moteur utilisé, synthèse des 3 piliers d'intégrité et indice IA.
2. **Tableau Récapitulatif des Notes Calibrées** : Restitution stricte des notes obtenues par axe avec barème officiel.
3. **Profil Visuel Multidimensionnel (Diagrammes Radar)** : Double diagramme radar Mermaid (`radar-beta`) affichant côte à côte les 6 indicateurs techniques et l'affinité aux 6 profils didactiques.
4. **Analyses d'Intégrité & Dynamique Temporelle** : Télémétrie d'activité, stylométrie et contrôle de similarités.
5. **Sections Pédagogiques Structurées** :
   - *Bilan de Progression & Analyse Pédagogique*
   - *Points Forts Observés* (avec extraits de code Haskell annotés)
   - *Axes d'Amélioration & Conseils Méthodologiques*
   - *Commentaire de l'Enseignant*

---

## 5. Guide d'Utilisation en Ligne de Commande (CLI)

### Utilisation avec le Moteur Unifié (`eval_codelab.py`)
```bash
# Évaluer une copie Haskell individuelle
python3 ~/codelab.files/skills/eval_codelab.py \
  --folder <chemin_dossier_etudiant> \
  --skill eval-haskell-sample \
  --out evaluations/<login>_rapport.md

# Évaluer une promotion complète
python3 ~/codelab.files/skills/eval_codelab.py \
  --session Session_Haskell_L2 \
  --dir <repertoire_session> \
  --skill eval-haskell-sample
```

### Utilisation des Scripts Autonomes du Skill
```bash
# 1. Audit technique et sortie JSON
python3 ~/codelab.files/skills/eval-haskell-sample/scripts/audit_student.py <chemin_dossier_etudiant>

# 2. Génération directe de la fiche individuelle
python3 ~/codelab.files/skills/eval-haskell-sample/scripts/generate_report.py <chemin_dossier_etudiant> <fichier_sortie.md>

# 3. Audit global d'une cohorte avec matrice de similarité
python3 ~/codelab.files/skills/eval-haskell-sample/scripts/audit_cohort.py <chemin_dossier_cohorte> \
  evaluations/notes.csv \
  evaluations/synthese.md
```

---

## 6. Arborescence du Skill

```
eval-haskell-sample/
├── SKILL.md                          # Définition du skill pour CodeLab / IA
├── README.md                         # Documentation end-user (ce fichier)
├── report_template.md                # Gabarit Markdown pour les rapports individuels
├── weights.json                      # Paramétrage des pondérations et plafonds
├── references/
│   └── grille_evaluation.md          # Guide méthodologique de la grille critériée
└── scripts/
    ├── audit_student.py              # Analyse GHC / statique et extraction de métriques
    ├── generate_report.py            # Générateur de fiches Markdown individuelles
    └── audit_cohort.py               # Analyseur de promotion et détection de plagiat
```

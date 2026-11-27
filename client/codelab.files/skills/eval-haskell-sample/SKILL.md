---
name: eval-haskell-sample
description: >-
  Skill d'exemple pour l'évaluation de programmes et carnets en langage Haskell.
  Vérifie la compilation et le typage GHC, la conception fonctionnelle (signatures, récursivité, filtrage par motif, ordre supérieur), le style, les tests et l'authenticité du code.
version: 1.2.0
last_modified: 2026-11-27
---

# Skill d'Évaluation : Haskell / Programmation Fonctionnelle (Exemple)

**Version :** 1.2.0 | **Dernière modification :** 27 novembre 2026

Ce skill est un modèle d'exemple générique fourni avec CodeLab pour illustrer l'évaluation automatique de code fonctionnel en langage Haskell (fichiers `.hs` et carnets interactifs `.ipynb` Haskell).
Il s'appuie sur le compilateur GHC (`ghc -fno-code -Wall`, avec repli syntaxique si GHC est absent) et vérifie la qualité fonctionnelle sans dépendre d'un cursus ou d'un jeu de TP particulier.

## Structure du barème d'exemple (sur 20 points)
1. **Compilation & Typage GHC (6 pts)** : Analyse syntaxique et vérification de types sans erreur des fichiers `.hs` et cellules `.ipynb` Haskell (`ghc -fno-code`).
2. **Conception Fonctionnelle (8 pts)** : Signatures de types explicites (`::`), décomposition en fonctions pures, récursivité, filtrage par motif (*pattern matching*), gardes (`|`), types algébriques (`data`/`newtype`/`type`) et fonctions d'ordre supérieur (`map`, `filter`, `foldr`/`foldl`, composition `.`).
3. **Qualité, Style & Exhaustivité (4 pts)** : Absence de warnings `-Wall` (notamment `-Wincomplete-patterns` et `-Wunused-binds`), commentaires `--` / `{- -}` / Haddock, conventions de nommage (`camelCase` / `PascalCase`) et absence d'anti-patterns (`head`/`tail` partiels, `if c then True else False`).
4. **Démarche de Test & Validation (2 pts)** : Présence de fonctions de test (`test*`, `prop_*` QuickCheck), d'expressions de vérification ou d'un `main` de test.

> [!TIP]
> **Pondérations éditables (`weights.json`)** : Les poids des 4 axes, les plafonds et les malus anti-triche sont configurables directement dans `weights.json` à la racine du skill ou dans le dossier d'évaluation de la session. Utilisez l'option *« Éditer les pondérations »* du menu contextuel CodeLab pour ajuster les barèmes sans modifier le code Python.

## Détection d'IA & Plagiat
- **Détection IA (0 à 100%)** : Détection de pragmas/extensions GHC avancées anachroniques (`{-# LANGUAGE ... #-}`), d'imports hors programme d'initiation (`Control.Monad.Trans`, `Control.Lens`, `System.IO.Unsafe`, `Data.Aeson`), de stylométrie de prompt ChatGPT (commentaires didactiques formatés, étapes numérotées, anglais artificiel) et de chronométrie CodeLab anormale (< 60s pour un fichier complet).
- **Détection de Copier-Collé Inter-Étudiants** : Matrice croisée de similarité par n-grams syntaxiques Jaccard (`plagiarism_matrix.csv`) et rapport de cohorte (`rapport_plagiat.md`).

---

## Modèles d'IA Générative Supportés (Google Gemini)

L'évaluation pédagogique hybride s'appuie sur la famille de modèles Google Gemini :

| Modèle | Précision d'analyse | Détection subtile | Temps par étudiant | Promo (30 étudiants) | Recommandation |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Gemini 3.5 Flash-Lite** *(défaut)* | Bonne (guidée) | Standard | ~8 – 10 s | ~4 – 5 min | **Idéal pour les évaluations de masse** régulières. Les verrous et plafonds sont garantis par le moteur d'audit. |
| **Gemini 3.5 Flash** *(standard)* | Très bonne | Élevée | ~15 – 20 s | ~8 – 10 min | **Le compromis optimal** pour une analyse critique plus incisive et une relecture fine du code. |
| **Gemini 3.5 Pro** | Excellente | Maximale | ~35 – 50 s | ~20 – 25 min | À réserver pour réévaluer un cas litigieux ou pour des audits individuels très approfondis. |

### Configuration du modèle
Le modèle actif peut être configuré :
- Via `user.properties` : `GEMINI_MODEL=gemini-3.5-flash`
- Ou en variable d'environnement : `export GEMINI_MODEL="gemini-3.5-flash"`

---

## Instructions pour la rédaction du compte-rendu (LLM / Fallback)
- Adopter une approche pédagogique, rigoureuse et constructive sur les principes de la programmation fonctionnelle.
- Mettre en valeur l'élégance du filtrage par motif, des signatures de types explicites, de la récursivité structurelle et des fonctions d'ordre supérieur.
- Signaler avec bienveillance les fonctions partielles dangereuses (`head`, `tail`, `fromJust`) et les motifs non exhaustifs.
- Insérer obligatoirement de véritables lignes physiques vides (`\n\n`) entre chaque bloc Markdown.

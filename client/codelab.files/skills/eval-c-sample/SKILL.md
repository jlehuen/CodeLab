---
name: eval-c-sample
description: >-
  Skill d'exemple pour l'évaluation de programmes et algorithmes en langage C.
  Vérifie la syntaxe et la compilation C, la modularité et les fonctions, le style de codage, la détection d'anti-patrons et l'authenticité du code.
version: 1.1.0
last_modified: 2026-10-14
---

# Skill d'Évaluation : C / Algorithmique (Exemple)

**Version :** 1.1.0 | **Dernière modification :** 14 octobre 2026

Ce skill est un modèle d'exemple générique fourni avec CodeLab pour illustrer l'évaluation automatique de code en langage C.
Il s'appuie sur une analyse syntaxique Clang/GCC et vérifie la qualité du code sans dépendre d'un cursus ou d'un jeu de TP particulier.

## Structure du barème d'exemple (sur 20 points)
1. **Compilation & Syntaxe (6 pts)** : Parsing et compilation sans erreur des fichiers `.c` et `.h` (via Clang ou GCC, avec tolérance `-Wno-main-return-type` pour `void main()`).
2. **Architecture & Fonctions (8 pts)** : Présence de fonctions déclarées, décomposition modulaire, prototypes, passage de paramètres, absence de fonction `main` monolithique.
3. **Qualité de code, Style & Lisibilité (4 pts)** : Respect des conventions de nommage, constantes symboliques `#define` en MAJUSCULES, indentation régulière, commentaires utiles et documentation des fonctions.
4. **Démarche de test & Validation (2 pts)** : Présence d'assertions (`assert`), de fonctions de test ou de jeux d'essais.

> [!TIP]
> **Pondérations éditables (`weights.json`)** : Les poids des 4 axes, les plafonds et les malus anti-triche sont configurables directement dans `weights.json` à la racine du skill ou dans le dossier d'évaluation de la session. Utilisez l'option *« Éditer les pondérations »* du menu contextuel CodeLab pour ajuster les barèmes sans modifier le code Python.

## Détection d'IA & Plagiat
- **Détection IA (0 à 100%)** : Détection des constructions anachroniques pour un débutant (pointeurs complexes, macros ésotériques, structures imbriquées), stylométrie de prompt ChatGPT (commentaires didactiques formatés, étapes numérotées, anglais artificiel) et chronométrie CodeLab anormale (< 60s pour un fichier complet).
- **Détection de Copier-Collé Inter-Étudiants** : Matrice croisée de similarité par algorithme Winnowing n-grams (`plagiarism_matrix.csv`) et rapport de cohorte (`rapport_plagiat.md`).

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
- Adopter une approche pédagogique et constructive.
- Mettre en valeur la décomposition modulaire en fonctions et la lisibilité du code C.
- Rappeler les bonnes pratiques de programmation défensive et de typage strict.
- Insérer obligatoirement de véritables lignes physiques vides (`\n\n`) entre chaque bloc Markdown.


---
name: eval-python-sample
description: >-
  Skill d'exemple pour l'évaluation de scripts et algorithmes en langage Python.
  Analyse l'AST, vérifie le respect des conventions PEP 8, détecte les anti-patterns et exécute les tests unitaires.
version: 1.1.0
last_modified: 2026-10-10
---

# Skill d'Évaluation : Python / Algorithmique (Exemple)

**Version :** 1.1.0 | **Dernière modification :** 10 octobre 2026

Ce skill est un modèle d'exemple fourni avec CodeLab pour illustrer l'évaluation automatique de code en langage Python.
Il s'appuie sur l'analyseur syntaxique AST standard de Python (`ast`) et vérifie la qualité du code sans dépendance externe lourde.

## Structure du barème d'exemple (sur 20 points)
1. **Syntaxe & Validité AST (6 pts)** : Parsing sans erreur de syntaxe de tous les fichiers `.py`.
2. **Conception & Fonctions (8 pts)** : Présence de fonctions définies avec `def`, signatures adaptées, modularité.
3. **Qualité & Style Pythonique PEP 8 (4 pts)** : Respect des conventions de nommage, docstrings, absence d'anti-patterns impératifs.
4. **Tests & Assertions (2 pts)** : Présence de doctests ou de blocs `assert`.

> [!TIP]
> **Pondérations éditables (`weights.json`)** : Les poids des 4 axes, les plafonds et les malus anti-triche sont configurables directement dans `weights.json` à la racine du skill ou dans le dossier d'évaluation de la session. Utilisez l'option *« Éditer les pondérations »* du menu contextuel CodeLab pour ajuster les barèmes sans modifier le code Python.

## Détection d'IA & Plagiat
- **Détection IA (0 à 100%)** : Détection des imports interdits (`sys`, `os`), de constructions suspectes pour un débutant (décorateurs, annotations de types), de stylométrie de prompt ChatGPT et de chronométrie anormale.
- **Détection de Copier-Collé Inter-Étudiants** : Matrice croisée de similarité Jaccard (`plagiarism_matrix.csv`) et rapport de cohorte (`rapport_plagiat.md`).

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
- Mettre en valeur l'élégance du style pythonique (compréhensions de listes, gestionnaires de contexte `with`).
- Rappeler l'importance des docstrings et du typage optionnel (`type hints`).
- Insérer obligatoirement de véritables lignes physiques vides (`\n\n`) entre chaque bloc Markdown.


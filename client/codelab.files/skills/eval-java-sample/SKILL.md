---
name: eval-java-sample
description: >-
  Skill d'exemple pour l'évaluation de programmes orientés objet en langage Java.
  Vérifie la compilation javac, l'encapsulation, la modélisation objet et la documentation Javadoc.
version: 1.1.0
last_modified: 2026-10-10
---

# Skill d'Évaluation : Java / POO (Exemple)

**Version :** 1.1.0 | **Dernière modification :** 10 octobre 2026

Ce skill est un modèle d'exemple fourni avec CodeLab pour illustrer l'évaluation automatique de code orienté objet en langage Java.
Il analyse la structure des classes, l'encapsulation (attributs privés, accesseurs/mutateurs) et la présence de documentation Javadoc.

## Structure du barème d'exemple (sur 20 points)
1. **Compilation Java (6 pts)** : Syntaxe valide et compilation sans erreur fatale (via `javac` ou analyseur syntaxique).
2. **Modélisation Objet & Encapsulation (8 pts)** : Conception des classes, respect de l'encapsulation (`private`), méthodes d'instance.
3. **Clarté & Documentation Javadoc (4 pts)** : Présence de commentaires Javadoc `/** ... */`, indentation et conventions Java (`camelCase`).
4. **Validation & Tests (2 pts)** : Présence de méthodes de test `test...()` ou d'assertions.

> [!TIP]
> **Pondérations éditables (`weights.json`)** : Les poids des 4 axes, les plafonds et les malus anti-triche sont configurables directement dans `weights.json` à la racine du skill ou dans le dossier d'évaluation de la session. Utilisez l'option *« Éditer les pondérations »* du menu contextuel CodeLab pour ajuster les barèmes sans modifier le code Python.

## Détection d'IA & Plagiat
- **Détection IA (0 à 100%)** : Détection des constructions Java 8+ hors programme d'initiation (`Streams`, expressions lambdas, inférence de type `var`), imports réseau ou réflexion (`java.lang.reflect`), stylométrie de prompt ChatGPT et chronométrie anormale.
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
- Adopter un ton rigoureux et formateur sur les principes de la programmation orientée objet.
- Valoriser l'encapsulation et la séparation des responsabilités.
- Structurer le compte-rendu avec un tableau récapitulatif des critères.
- Insérer obligatoirement de véritables lignes physiques vides (`\n\n`) entre chaque bloc Markdown.


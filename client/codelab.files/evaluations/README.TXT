#####################################################################
## This file is part of the software CodeLab IDE and Simulators    ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################

[FR]
Ce dossier est utilisé par CodeLab pour stocker et archiver :
- Les synthèses et bilans d'évaluation de session (synthese_*.md)
- Les comptes-rendus d'audit individuel des étudiants (*_rapport.md)
- Les fichiers récapitulatifs des notes au format tableur (notes_*.csv)

Ce dossier est principalement exploité par les enseignants et tuteurs.
Vous pouvez consulter ces rapports directement dans CodeLab via l'explorateur
de fichiers (icône « Consulter les évaluations »).

=====================================================================
GUIDE D'INTERPRÉTATION DES RAPPORTS & PROFILS DIDACTIQUES CODELAB
=====================================================================

1. LES 6 PROFILS ARCHÉTYPAUX (3 AXES OPPOSÉS À 180°)
-----------------------------------------------------
CodeLab projette le comportement télémétrique de l'étudiant sur 3 axes
de tension didactique indépendants :

* Axe 1 : Aisance conceptuelle vs Renoncement
  - 👑 LE BOSS : Résolution directe, haute authenticité, grande autonomie.
  - 🚪 LE DÉCROCHEUR : Inactivité prolongée, blocages non résolus, abandon.

* Axe 2 : Démarche d'apprentissage vs Délégation externe
  - 🔨 L'ARTISAN : Saisie personnelle, progression par cycles itératifs et méthode.
  - 🤖 LE CYBORG : Recours marqué aux outils externes / IA, code injecté brutalement.

* Axe 3 : Stratégie face aux erreurs
  - ⚡ LE CASSE-COU : Tâtonnement compulsif, runs en boucle sans lecture des diagnostics.
  - 🏴‍☠️ LE PIRATE : Collusion, transfert de fichiers externes (clé USB) ou copie directe.

2. DÉTECTION DES DOUBLES-PROFILS DIDACTIQUES COHÉRENTS
------------------------------------------------------
En situation réelle d'apprentissage, un étudiant est rarement un archétype
unilatéral. Deux dimensions orthogonales peuvent coexister avec force.

Un DOUBLE-PROFIL COHÉRENT est automatiquement détecté lorsque :
1. Les deux profils appartiennent à des axes orthogonaux (non antagonistes).
2. Les deux scores d'affinité sont élevés (tous deux >= 11.0 / 20).
3. L'écart d'affinité est resserré (|Score_1 - Score_2| <= 3.5 points).

3. CATALOGUE DES 12 HYBRIDATIONS DIDACTIQUES REMARQUABLES
---------------------------------------------------------
- 👑 Boss + 🔨 Artisan -> « L'Architecte Méthodique » (ou L'Orfèvre)
  Signification : Excellence conceptuelle et rigueur d'apprentissage exemplaire.
  Conseil : Valoriser l'autonomie, proposer des défis d'optimisation ou du tutorat.

- 👑 Boss + ⚡ Casse-cou -> « Le Hacker Instinctif »
  Signification : Grande vivacité intellectuelle, mais démarche impulsive à chaud.
  Conseil : Canaliser vers les tests unitaires et la conception préalable.

- 🔨 Artisan + ⚡ Casse-cou -> « Le Travailleur Acharné »
  Signification : Investissement total et authentique, mais acharnement face aux bugs.
  Conseil : Prioritaire : apprendre à lire les tracebacks et poser des hypothèses.

- 🤖 Cyborg + ⚡ Casse-cou -> « L'Apprenti Sorcier »
  Signification : Injection de code IA non compris + modifications compulsives au hasard.
  Conseil : Suspendre l'IA, retour aux bases et écriture pas à pas avec verbalisation.

- 🚪 Décrocheur + 🤖 Cyborg -> « Le Délégataire Passif »
  Signification : Inactivité prolongée masquée en fin de séance par un code IA parachuté.
  Conseil : Soutenance orale pour vérifier l'assimilation et remédiation guidée.

- 🚪 Décrocheur + ⚡ Casse-cou -> « L'Épuisé Découragé »
  Signification : Combat compulsif initial soldé par la frustration et l'abandon.
  Conseil : Déceler la détresse plus tôt en séance avant la phase de renoncement.

- 👑 Boss + 🤖 Cyborg -> « L'Ingénieur Augmenté »
  Signification : Maîtrise conceptuelle mise au service d'un copilotage IA productif.
  Conseil : S'assurer que la compétence de codage sans filet reste acquise.

- 🏴‍☠️ Pirate + 🤖 Cyborg -> « Le Contrebandier Numérique »
  Signification : Cumul de ruptures d'intégrité (clé USB + IA générative).
  Conseil : Convocation formelle et nouvelle évaluation en environnement surveillé.

- 👑 Boss + 🏴‍☠️ Pirate -> « L'Éclaireur Partageur »
  Signification : Maîtrise technique associée à des transferts de code non autorisés.
  Conseil : Rappeler la frontière déontologique entre entraide et collusion.

- 🚪 Décrocheur + 🏴‍☠️ Pirate -> « Le Copiste de Survie »
  Signification : Blocage complet résolu par l'emprunt d'un fichier externe voisin.
  Conseil : Traiter le blocage didactique racine tout en appliquant le règlement.

- 🔨 Artisan + 🏴‍☠️ Pirate -> « L'Artisan Sous Influence »
  Signification : Travail personnel appliqué complété par une brique collusoire.
  Conseil : Valoriser l'effort propre tout en invalidant le fragment emprunté.

- 🔨 Artisan + 🚪 Décrocheur -> « L'Apprenti Hésitant »
  Signification : Progression soignée mais lenteurs excessives et doute persistant.
  Conseil : Stimuler la confiance en soi et rythmer le travail par micro-objectifs.

---------------------------------------------------------------------

[EN]
This folder is used by CodeLab to store and archive:
- Session evaluation summaries (synthese_*.md)
- Individual student audit reports (*_rapport.md)
- Grade spreadsheets (notes_*.csv)

This folder is mainly used by teachers and tutors.
You can view these reports directly in CodeLab through the file explorer
("View evaluations" icon).

See the French section above for the comprehensive guide to interpreting
archetypes and coherent dual-profiles ("The Architect", "Hard Worker", etc.).

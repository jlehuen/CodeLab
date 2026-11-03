# Traçabilité Fine du Processus de Programmation en Environnement Pédagogique : Détection Comportementale de l'IA, Modélisation de l'Engagement et Diagnostic Didactique

**Jérôme Lehuen**  
*Le Mans Université — Département Informatique / Laboratoire d'Informatique de l'Université du Mans (LIUM)*  
*Novembre 2026*

---

### Résumé

L'avènement des modèles de langage à large échelle (LLM) et des assistants génératifs de code a profondément bouleversé l'enseignement de l'informatique, rendant l'évaluation sommative basée sur le seul produit final (*le code source statique*) vulnérable et insuffisante pour apprécier les compétences réelles des apprenants. Cet article présente une approche et une architecture dédiées au traçage continu et fin du **processus de programmation** au sein de l'environnement éducatif intégré CodeLab. En articulant une sonde événementielle légère au niveau de l'éditeur client (Java Swing) et un moteur d'audit différentiel temps réel asynchrone sur le serveur (Rust/Tokio), notre système capture une ontologie de 11 primitives d'activité inaltérables (diffs unifiés, dynamiques de frappe, rétentions de focus, collages, cycles de compilation et d'exécution, requêtes d'aide). À partir de cette trace chronologique, nous définissons un corpus d'indicateurs quantitatifs permettant de discriminer l'effort de conception humain de l'externalisation vers une IA (*Score d'Authenticité*, *Indice de Navette Externe*), d'objectiver la démarche de résolution (*Indice d'Acharnement*, *Time-To-Fix*) et de catégoriser automatiquement les apprenants au sein d'une typologie pédagogique explicable (*Artisans, Cyborgs, Casse-cou, Décrocheurs*).

**Mots-clés :** *Learning Analytics, CS Education, Traçage de processus, Intégrité académique, Détection d'IA générative, Environnements de développement pédagogiques (IDE), ProgSnap2.*

---

## 1. Introduction & Problématique

L'apprentissage de la programmation informatique repose sur l'acquisition progressive de compétences méthodologiques : formalisation d'algorithmes, itération par essais/erreurs, interprétation des diagnostics d'erreurs et remédiation cognitive. Historiquement, l'évaluation de ces compétences reposait majoritairement sur l'analyse statique ou dynamique du livrable final (batteries de tests unitaires, respect des consignes, conformité stylistique).

L'accessibilité massive d'outils d'IA générative (ChatGPT, Claude, GitHub Copilot) et de canaux de communication synchrones a rendu cette approche produit caduque. Deux copies syntaxiquement identiques peuvent aujourd'hui recouvrir deux réalités didactiques opposées :
1. Une démarche rigoureuse de conception humaine échelonnée sur deux heures de travaux pratiques ;
2. Une soumission opaque obtenue en quelques secondes par copier-coller d'une invite (*prompt*) externe.

Les méthodes traditionnelles de détection de plagiat (MOSS, JPlag) ou d'analyse stylométrique statique atteignent leurs limites face à du code reformaté ou généré de novo par des modèles stochastiques. Dès lors, **la seule source de vérité réside dans la dynamique temporelle de production du code** (*Process Data Analytics*).

Cet article présente la conception, l'implémentation et la formalisation analytique d'un cadre de traçabilité complet déployé au sein de la plateforme CodeLab.

---

## 2. Travaux Connexes & Cadre Théorique

L'exploitation des traces fines en didactique de l'informatique s'inscrit dans la lignée de projets pionniers tels que *Blackbox* pour l'environnement BlueJ (Brown et al., 2014) et les initiatives de standardisation de données d'apprentissage comme le format *ProgSnap2* (Price et al., 2020). 

Ces travaux ont établi que la dynamique de saisie clavier (*Keystroke Logging*) et les intervalles temporels séparant les compilations permettent de mesurer l'engagement (*Blikstein, 2011*) et de diagnostiquer des profils en difficulté (*Ahadi et al., 2015*). Toutefois, la majorité de ces systèmes souffrent de deux écueils :
* Soit une lourdeur télémétrique disproportionnée (enregistrement de chaque caractère à la milliseconde provoquant une saturation de bande passante) ;
* Soit une absence de contextualisation des événements système (méconnaissance des pertes de focus applicatif ou des interactions de glisser-déposer).

Notre contribution comble ce fossé par une collecte asynchrone hybride, associant un échantillonnage par seuils d'inactivité (*debounced autosave*), une interception explicite des primitives d'OS (presse-papier, gestionnaire de fenêtres) et un calcul différentiel textuel côté serveur.

---

## 3. Architecture Système & Ontologie Événementielle

### 3.1. Philosophie de découplage : Collecte vs Dérivation

Le système repose sur une séparation hermétique entre la **collecte en continu** (qui doit demeurer imperceptible pour l'utilisateur) et le **calcul analytique** (opéré a posteriori lors des évaluations sommatives ou en tâche de fond pour la supervision tuteur) :

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT CODELAB (Java Swing)                              │
│   • Écouteur de saisie et de suppressions (TYPING)                               │
│   • Détecteur de perte/gain de focus (FOCUS)                                     │
│   • Intercepteur de presse-papier (PASTE)                                        │
│   • Moniteur de cycle de vie des processus (COMPILE, EXECUTE)                    │
│   • Récepteur de glisser-déposer de fichiers (FILE_DROP)                         │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Flux réseau asynchrone TCP / MsgPack
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         SERVEUR CODELAB (Rust / Tokio)                           │
│   • Calcul différentiel textuel immédiat (similar) sur chaque upload (EDIT)      │
│   • Gestionnaire de session et traçabilité réseau (SESSION, FILE_OP)             │
│   • Centralisation des flux d'aide pédagogique (HELP)                            │
│   • Journalisation append-only immuable : .activity.jsonl                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2. L'Ontologie des 11 Événements Élémentaires

Chaque session d'apprentissage génère une séquence linéaire d'événements discrets $E = \{e_1, e_2, \dots, e_n\}$, où chaque événement est un tuple JSON horodaté selon la norme ISO 8601 UTC :

1. **`SESSION`** : Marqueurs d'ouverture et clôture de session ($\text{status} \in \{\text{CONNECTED}, \text{DISCONNECTED}\}$, adresse IP, durée totale en secondes).
2. **`VIEW`** : Fichier rendu actif dans l'interface de travail (permet de quantifier la consultation de l'énoncé Markdown ou des fichiers de spécification).
3. **`FILE_OP`** : Mutations structurelles de l'espace de travail ($\text{op} \in \{\text{CREATE}, \text{DELETE}, \text{RENAME}, \text{MOVE}\}$).
4. **`FILE_DROP`** : Importation de fichiers externes par glisser-déposer (volume en octets, nom de la ressource).
5. **`TYPING`** : Métriques cumulées d'édition manuelle (nombre de frappes alphanumériques utiles `typed_chars`, nombre de retours arrière / suppressions `backspaces`).
6. **`FOCUS`** : État d'attention sur l'IDE ($\text{status} \in \{\text{LOST}, \text{GAINED}\}$, durée d'absence `duration_sec`).
7. **`PASTE`** : Événement d'insertion depuis le presse-papier (`chars`, `lines`).
8. **`EDIT`** : Snapshot différentiel textuel (`lines_added`, `lines_removed`, `chunk_max`, intervalle $\Delta t$, drapeaux d'alerte volumétrique `BURST_INSERT`).
9. **`COMPILE`** : Tentative de compilation ($\text{status} \in \{\text{OK}, \text{ERROR}\}$, extrait normalisé du diagnostic compilateur).
10. **`EXECUTE`** : Exécution de script ou cellule de carnet Jupyter ($\text{status} \in \{\text{STARTED}, \text{DONE}, \text{ERROR}, \text{HALTED}\}$, code d'erreur système, trace d'exception).
11. **`HELP`** : Interaction didactique avec l'équipe pédagogique ($\text{status} \in \{\text{REQUESTED}, \text{CANCELLED}, \text{RESOLVED}\}$, identifiant du tuteur intervenant).

### 3.3. Déontologie et Protection de la Vie Privée (*Privacy by Design*)

Conformément aux préconisations du RGPD relatives aux environnements d'apprentissage, le système proscrit l'aspiration intégrale du contenu textuel du presse-papier système : seules les métriques quantitatives (longueur, sauts de ligne) sont consignées. Le contenu n'est tracé que lorsqu'il est effectivement incorporé dans le fichier source géré par l'université.

---

## 4. Modélisation et Métriques Dérivées

À partir du journal d'événements, le moteur d'évaluation dérive un ensemble de métriques comportementales et cognitives.

### 4.1. Modélisation de l'Intégrité & Détection d'Externalisation (IA)

#### Score d'Authenticité du Code ($S_{\text{auth}}$)
Ce score quantifie la part intrinsèque du code rédigée manuellement par l'apprenant par rapport aux apports massifs externes :

$$S_{\text{auth}} = \frac{N_{\text{typed}}}{N_{\text{typed}} + N_{\text{pasted}} + N_{\text{dropped}}} \times 100$$

* Un apprenant authentique présente un score $S_{\text{auth}} > 75\%$.
* Un score $S_{\text{auth}} < 25\%$ constitue une présomption forte d'externalisation quasi intégrale du livrable.

#### Indice de Navette Externe ($I_{\text{nav}}$)
Les environnements d'IA générative fonctionnant par allers-retours asynchrones entre l'invite et la réponse, nous détectons le motif comportemental invariant :

$$P_{\text{IA}} = \left( \text{FOCUS}_{\text{LOST}} \,[\Delta t \in [30\text{s}, 180\text{s}]] \longrightarrow \text{FOCUS}_{\text{GAINED}} \longrightarrow \text{PASTE} \lor \text{EDIT}_{\text{BURST}} \right)$$

L'indice $I_{\text{nav}}$ correspond au nombre d'occurrences du motif $P_{\text{IA}}$ sur la durée de la séance. Une valeur $I_{\text{nav}} \ge 3$ caractérise un mode de travail sous-traité par requêtes itératives.

#### Ratio d'Hésitation Cognitive ($R_{\text{hesit}}$)
La programmation humaine naturelle implique une constante révision locale :

$$R_{\text{hesit}} = \frac{N_{\text{backspaces}}}{N_{\text{typed}}} \times 100$$

* **Comportement nominal** : $10\% \le R_{\text{hesit}} \le 25\%$.
* **Anomalie de recopie passive** : $R_{\text{hesit}} < 3\%$ (l'apprenant recopie à l'aveugle un code affiché sur un écran tiers sans phase de réflexion).

---

### 4.2. Diagnostic Méthodologique & Didactique

#### Granularité du Cycle de Développement ($G_{\text{dev}}$)
Mesure la moyenne des lignes modifiées $\Delta L$ entre deux rétroactions de l'interpréteur ou du compilateur :

$$G_{\text{dev}} = \frac{1}{|K|} \sum_{k \in K} \Delta L_k \quad \text{où } K = \{ \text{événements } \text{COMPILE} \lor \text{EXECUTE} \}$$

* **Démarche incrémentale (TDD)** : $G_{\text{dev}} \le 15$ lignes. L'apprenant valide unitairement chaque sous-fonction.
* **Démarche en tunnel** : $G_{\text{dev}} \ge 60$ lignes. L'apprenant rédige l'intégralité du problème sans confrontation au système, provoquant des impasses diagnostiques.

#### Indice d'Acharnement Stérile (*Thrashing Index*, $I_{\text{thrash}}$)
L'acharnement stérile survient lorsqu'un apprenant réitère des exécutions défaillantes sans modifier significativement son code :

$$E_k = \text{EXECUTE}(\text{status} = \text{ERROR}) \land E_{k+1} = \text{EXECUTE}(\text{status} = \text{ERROR}) \quad \text{avec } \Delta t < 15\text{s} \lor \Delta\text{code} \le 5\text{ chars}$$

L'indice correspond à la cardinalité maximale de cette chaîne. Une valeur élevée caractérise l'étudiant « espérant une résolution magique » sans analyse du message d'erreur.

#### Persévérance et Temps de Résolution (*Time-To-Fix*, $TTF$)
Durée séparant la première occurrence d'une erreur d'exécution ou de syntaxe de sa correction effective :

$$TTF = t(\text{EXECUTE}_{\text{DONE}}) - t(\text{EXECUTE}_{\text{ERROR}}^{\text{first}})$$

---

### 4.3. Répartition du Budget-Temps en 4 Quadrants

La durée totale de présence $T_{\text{tot}}$ est décomposée de manière exhaustive en quatre composantes disjointes :

$$T_{\text{tot}} = T_{\text{lecture}} + T_{\text{code\_actif}} + T_{\text{reflexion}} + T_{\text{hors\_IDE}}$$

1. $T_{\text{lecture}}$ : Cumul des temps passés sur des fichiers d'énoncé (`VIEW(sujet.md)`).
2. $T_{\text{code\_actif}}$ : Temps passé sous focus de l'éditeur avec activité de frappe continue.
3. $T_{\text{reflexion}}$ : Temps passé sous focus mais sans événement de saisie pendant un intervalle $t \in [30\text{s}, 3\text{min}]$.
4. $T_{\text{hors\_IDE}}$ : Somme des durées $\text{FOCUS}_{\text{LOST}}$.

---

### 4.4. Analyse Relationnelle & Réseau de Transmission Inter-Apprenants

En observant l'ensemble des journaux d'une même cohorte, le système met en relation les horodatages des événements de complétion et d'insertion :

$$\exists (A, B) \quad \text{tels que } t_A(\text{EXECUTE}_{\text{DONE}}) < t_B(\text{PASTE}) < t_A(\text{EXECUTE}_{\text{DONE}}) + 90\text{s}$$

Si le bloc collé par $B$ partage une structure syntaxique similaire au code validé par $A$, une arête dirigée $A \to B$ est instanciée dans le graphe de propagation de code en séance.

---

## 5. Typologie et Clustering des Apprenants

Sur la base de ces dimensions, nous formalisons une classification pédagogique en 6 clusters comportementaux :

| Profil | Score $S_{\text{auth}}$ | Indice $I_{\text{nav}}$ | Granularité $G_{\text{dev}}$ | Caractéristiques observées |
| :--- | :--- | :--- | :--- | :--- |
| **L'Artisan** | $> 80\%$ | Faible ($< 2$) | Petite ($\le 15$ l.) | Programmation méthodique, hésitations cognitives normales, tests réguliers. |
| **Le Boss** | $> 85\%$ | Nul ($0$) | Moyenne ($20-30$ l.) | Maîtrise conceptuelle immédiate, résolution quasi instantanée des erreurs, autonomie totale. |
| **Le Cyborg** | $< 25\%$ | Élevé ($\ge 4$) | Élevée (bursts $\ge 40$ l.) | Externalisation systématique vers une IA, présence hors IDE majeure, absence d'hésitation locale. |
| **Le Casse-cou** | Variable | Faible | Très faible | Taux d'acharnement stérile élevé ($I_{\text{thrash}} \ge 4$), absence de lecture des diagnostics d'erreurs. |
| **Le Décrocheur** | Indéterminé | Nul | Nulle | Inactivité $> 40\%$, abandon précoce après 1 ou 2 échecs sans sollicitation du corps enseignant. |
| **Le Pirate** | $< 15\%$ | Nul | Nulle | Importation brute de fichiers externes (`FILE_DROP`), synchronisme temporel avéré avec un émetteur. |

---

## 6. Applications Pédagogiques & Retours d'Expérience

Le dispositif soutient deux cas d'usage complémentaires :

### 6.1. Supervision et Régulation en Temps Réel (Évaluation Formative)
Durant les séances de travaux pratiques, le tableau de bord de supervision des tuteurs agrège les flux d'activité :
* Détection précoce des blocages : alerte visuelle lorsqu'un étudiant franchit un seuil de $I_{\text{thrash}} \ge 3$, permettant à l'enseignant d'intervenir avant le désengagement.
* Gestion de file d'attente d'aide : optimisation des tournées de tuteurs basée sur l'ancienneté des requêtes `HELP`.

### 6.2. Évaluation Sommative & Justification des Notations
Dans le cadre d'épreuves certificatives sur machine (ex: modules universitaires évalués par le skill `eval_introPython`) :
* **Preuve opposable** : En cas de recours, la trace inaltérable fournit la démonstration objective de l'origine du code (séquence de perte de focus suivie d'un collage massif de 80 lignes).
* **Valorisation de la démarche** : La note finale peut intégrer une pondération récompensant la persévérance méthodologique de l'*Artisan* face aux résultats artificiels d'un profil *Cyborg*.

---

## 7. Conclusion & Perspectives

Le traçage fin de processus proposé par CodeLab déplace le paradigme de l'évaluation en programmation : il substitue à l'analyse incertaine d'un artefact statique une **analyse dynamique et scientifique de l'activité cognitive réelle**. En combinant 11 indicateurs élémentaires légers, l'approche garantit la souveraineté pédagogique des enseignants face aux défis posés par l'IA générative, tout en offrant un outil puissant de remédiation bienveillante pour les apprenants en difficulté.

Les travaux futurs porteront sur l'intégration de modèles prédictifs capables d'anticiper le décrochage dès les 20 premières minutes d'une épreuve, ainsi que sur la corrélation entre les indices de frappe et les résultats d'apprentissage à long terme.

---

## Références Bibliographiques

* Ahadi, A., Lister, R., Haapala, H., & Vihavainen, A. (2015). *Exploring machine learning methods to automatically identify students in need of assistance*. In Proceedings of the eleventh annual International Conference on International Computing Education Research (ICER '15), pp. 121–130.
* Becker, B. A., Denny, P., Finnie-Ansley, J., Luxton-Reilly, A., Prather, J., & Santos, E. A. (2023). *Programming Is Hard-Or at Least It Used to Be: Educational Opportunities and Challenges of AI Code Generation*. In Proceedings of the 54th ACM Technical Symposium on Computer Science Education (SIGCSE 2023), pp. 500–506.
* Blikstein, P. (2011). *Using learning analytics to assess students' developing competence in open-ended programming tasks*. In Proceedings of the 1st International Conference on Learning Analytics and Knowledge (LAK '11), pp. 102–116.
* Brown, N. C., Altadmri, A., Sentance, S., & Kölling, M. (2014). *Blackbox, Java, and the open research challenge*. In Proceedings of the 45th ACM Technical Symposium on Computer Science Education (SIGCSE '14), pp. 495–496.
* Price, T. W., Hovemeyer, D., Kelly, K., & Becker, B. A. (2020). *ProgSnap2: A Standardized Format for Programming Process Data*. In Proceedings of the 2020 ACM Conference on Innovation and Technology in Computer Science Education (ITiCSE '20), pp. 544–545.

# Traçabilité de l'Activité & Analyse Comportementale des Étudiants dans CodeLab

Ce document décrit en détail le système de traçabilité d'activité intégré à CodeLab (client Java Swing et serveur Rust), les 11 types d'événements capturés, les indicateurs calculés lors des audits et les conclusions pédagogiques et d'intégrité qui en découlent.

---

## 1. Vue d'ensemble & Philosophie Architecturale

Le dispositif repose sur une séparation stricte entre **Collecte en temps réel** et **Analyse en post-traitement** :

1. **Pendant la session (Collecte légère & inaltérable)** :
   * Le client et le serveur enregistrent les faits bruts horodatés dans un journal append-only unifié : `sessions/<session>/<login>/.activity.jsonl`.
   * Chaque ligne est un objet JSON autonome.
   * L'impact sur les performances, le processeur et la bande passante est quasi nul ($\le 1$ Ko par minute d'activité).
   * **Respect de la vie privée (RGPD)** : Le presse-papier externe de l'OS n'est jamais copié intégralement (aucun risque de fuite de mot de passe ou message privé) ; seuls les volumes (caractères, lignes) et les horodatages sont conservés.

2. **À l'évaluation (Dérivation des indicateurs & profilage)** :
   * Les scripts d'audit (ex: `eval_introPython` ou outils de supervision) rejouent la chronologie pour calculer des ratios, des temps moyens et détecter les anomalies de comportement.

---

## 2. Nomenclature des 11 Événements Capturés

| Événement | Source | Description & Déclencheur |
| :--- | :--- | :--- |
| `SESSION` | Serveur Rust | Connexion (`CONNECTED`) avec IP ou déconnexion (`DISCONNECTED`) avec durée totale. |
| `VIEW` | Serveur Rust | Fichier ouvert ou consulté dans l'éditeur (sujets, code source, documentation). |
| `FILE_OP` | Serveur Rust | Création (`CREATE`), suppression (`DELETE`), renommage (`RENAME`) ou déplacement (`MOVE`). |
| `FILE_DROP` | Client Java | Glisser-déposer d'un fichier externe depuis le système hôte (Bureau, clé USB, Téléchargements). |
| `TYPING` | Client Java | Cumul des frappes clavier réelles (`typed_chars`) et touches de suppression (`backspaces`). |
| `FOCUS` | Client Java | Perte (`LOST`) et reprise (`GAINED`) du focus applicatif avec durée hors de l'IDE en secondes. |
| `PASTE` | Client Java | Opération explicite de collage avec nombre de caractères et nombre de lignes insérés. |
| `EDIT` | Serveur Rust | Diff unifié à chaque sauvegarde : lignes ajoutées/supprimées, taille du plus grand bloc (`chunk_max`), alertes heuristiques. |
| `COMPILE` | Client Java | Tentative de compilation avec statut (`OK` / `ERROR`) et extrait du diagnostic compilateur. |
| `EXECUTE` | Client Java | Cycle d'exécution (`STARTED`, `DONE`, `ERROR`, `HALTED`), code de sortie et extrait de traceback / crash. |
| `HELP` | Serveur Rust | Demande d'aide tuteur (`REQUESTED`), annulation (`CANCELLED`) ou intervention (`RESOLVED` avec login du tuteur). |

---

### Exemples de structures JSON dans `.activity.jsonl`

```json
{"type":"SESSION","timestamp":"2026-11-03T14:00:00Z","session":"TP1","login":"etudiant1","status":"CONNECTED","ip":"172.20.10.4"}
{"type":"VIEW","timestamp":"2026-11-03T14:00:05Z","session":"TP1","login":"etudiant1","file":"sujet.md"}
{"type":"FILE_OP","timestamp":"2026-11-03T14:02:10Z","session":"TP1","login":"etudiant1","op":"CREATE","file":"tp1.py"}
{"type":"TYPING","timestamp":"2026-11-03T14:05:00Z","session":"TP1","login":"etudiant1","file":"tp1.py","typed_chars":120,"backspaces":15}
{"type":"FOCUS","timestamp":"2026-11-03T14:10:12Z","session":"TP1","login":"etudiant1","status":"LOST"}
{"type":"FOCUS","timestamp":"2026-11-03T14:11:30Z","session":"TP1","login":"etudiant1","status":"GAINED","duration_sec":78}
{"type":"PASTE","timestamp":"2026-11-03T14:11:35Z","session":"TP1","login":"etudiant1","file":"tp1.py","chars":450,"lines":22}
{"type":"EDIT","timestamp":"2026-11-03T14:11:41Z","session":"TP1","login":"etudiant1","file":"tp1.py","total_lines":45,"lines_added":22,"lines_removed":0,"chunk_max":22,"dt_sec":6,"alert":"BURST_INSERT"}
{"type":"FILE_DROP","timestamp":"2026-11-03T14:15:20Z","session":"TP1","login":"etudiant1","file":"data.csv","size_bytes":10240}
{"type":"COMPILE","timestamp":"2026-11-03T14:16:00Z","session":"TP1","login":"etudiant1","file":"main.c","status":"ERROR","error_msg":"syntax error"}
{"type":"HELP","timestamp":"2026-11-03T14:16:05Z","session":"TP1","login":"etudiant1","status":"REQUESTED"}
{"type":"HELP","timestamp":"2026-11-03T14:20:00Z","session":"TP1","login":"etudiant1","status":"RESOLVED","tutor":"hamdi"}
{"type":"EXECUTE","timestamp":"2026-11-03T14:21:00Z","session":"TP1","login":"etudiant1","file":"tp1.py","status":"STARTED"}
{"type":"EXECUTE","timestamp":"2026-11-03T14:21:02Z","session":"TP1","login":"etudiant1","file":"tp1.py","status":"ERROR","exit_code":1,"error_msg":"ZeroDivisionError: division by zero"}
{"type":"EXECUTE","timestamp":"2026-11-03T14:22:15Z","session":"TP1","login":"etudiant1","file":"tp1.py","status":"DONE","exit_code":0}
{"type":"SESSION","timestamp":"2026-11-03T16:00:00Z","session":"TP1","login":"etudiant1","status":"DISCONNECTED","duration_sec":7200}
```

---

## 3. Indicateurs Dérivés & Formules de Calcul

En croisant les horodatages et les données des événements, l'audit calcule les indicateurs suivants :

### A. Indicateurs d'Intégrité & Détection d'IA

| Indicateur | Formule / Règle de calcul | Seuil d'alerte | Interprétation |
| :--- | :--- | :--- | :--- |
| **Score d'Authenticité du Code** | $\frac{\text{typed\_chars}}{\text{typed\_chars} + \text{pasted\_chars} + \text{dropped\_chars}} \times 100$ | $< 25\%$ | Révèle si le code provient d'une frappe humaine active ou d'un import extérieur massif. |
| **Indice de Navette Externe** *(Pattern ChatGPT / Discord)* | Compte des séquences :<br>$\text{FOCUS(LOST, } 30\text{s-180s}) \to \text{FOCUS(GAINED)} \to \text{PASTE}$ | $\ge 3$ occurrences | L'étudiant va chercher chaque solution sur un navigateur ou une IA pour la recoller aussitôt. |
| **Taux de Friction Cognitive** | $\frac{\text{backspaces}}{\text{typed\_chars}} \times 100$ | $< 3\%$ ou $> 40\%$ | $\mathbf{< 3\%}$ : Recopie passive sans hésitation (dictée visuelle).<br>$\mathbf{> 40\%}$ : Hésitation extrême, tâtonnement aveugle. |
| **Fréquence des Bursts Massifs** | Compte des alertes `BURST_INSERT` ($\ge 20$ lignes en $\le 15$s) et `BURST_NEW_FILE` | $\ge 2$ occurrences | Code complet généré d'un bloc. |
| **Apports Externes Directs** | Compte des événements `FILE_DROP` | $\ge 1$ occurrence | Fichiers complets importés depuis clé USB ou téléchargement direct. |

---

### B. Indicateurs de Méthodologie & Démarche de Programmation

| Indicateur | Formule / Règle de calcul | Profil visé | Signification pédagogique |
| :--- | :--- | :--- | :--- |
| **Granularité de Développement** *(Cycle Edit-Test)* | Temps moyen et lignes modifiées entre deux `COMPILE` ou `EXECUTE` | $\mathbf{\le 15}$ lignes / test : Incrémental<br>$\mathbf{\ge 60}$ lignes / test : Tunnel | L'étudiant valide-t-il pas à pas (démarche agile/TDD) ou écrit-il tout d'un coup avant de tester ? |
| **Indice d'Acharnement Stérile** *(Thrashing Index)* | Séquence d'au moins 3 runs `ERROR` consécutifs avec $\Delta t < 20$s ou $\Delta\text{code} \le 5$ caractères | $\ge 3$ épisodes | L'étudiant ne lit pas la console et clique en boucle sur Exécuter en espérant un miracle. |
| **Temps de Résolution d'Erreur** *(Time-To-Fix)* | $\Delta t$ moyen entre la première `ERROR` sur un fichier et le premier `DONE` / `OK` | $\mathbf{< 30}$s : Inattention<br>$\mathbf{> 10}$min : Blocage conceptuel | Mesure la capacité de l'étudiant à interpréter les diagnostics du compilateur ou de l'interpréteur. |

---

### C. Budget Temps de la Séance *(Time Budget en 4 Quadrants)*

Calculé en ventilant les 100% de la durée de connexion (`SESSION`) :

```
┌────────────────────────────────────────────────────────────────────────┐
│  1. Temps de consultation / lecture (VIEW sur énoncé/headers) :  15 %  │
│  2. Temps de codage actif dans l'IDE (Focus + Frappe continue):  45 %  │
│  3. Temps de pause cognitive / réflexion (Focus sans frappe)  :  25 %  │
│  4. Temps passé hors de l'IDE (Cumul FOCUS_LOST)              :  15 %  │
└────────────────────────────────────────────────────────────────────────┘
```

* **Indice de décrochage** : Somme des périodes sans aucune interaction $> 10$ minutes.

---

### D. Dynamique d'Encadrement & Aide Tuteur

* **Temps de Réponse Tuteur** : $\text{timestamp}(\text{HELP RESOLVED}) - \text{timestamp}(\text{HELP REQUESTED})$. Permet de mesurer le temps d'attente subi par l'étudiant.
* **Efficacité de l'Intervention** : Pourcentage de fois où le premier run suivant `RESOLVED` aboutit à `DONE`.
* **Taux d'Abandon de Demande** : Fréquence du statut `CANCELLED` (l'étudiant a trouvé seul ou a abandonné par lassitude).

---

### E. Synchronicité Inter-Étudiants *(Réseau de Transmission en Séance)*

Si l'étudiant **A** réussit son exécution (`EXECUTE DONE`) à l'instant $T_A$, et que l'étudiant **B** (même session, IP proche) effectue un `PASTE` ou un `FILE_DROP` de structure identique à $T_B = T_A + \delta t$ (avec $\delta t \le 90$s) :
* L'algorithme identifie la **source** (étudiant émetteur) et la **cible** (étudiant récepteur) du transfert de code en séance.

---

## 4. Typologie des Profils Étudiants (Clusters Pédagogiques)

Le croisement automatique de ces indicateurs alimente les 6 clusters d'évaluation utilisés dans les bilans de promotion CodeLab :

```
                                 AUTHENTICITÉ
                                      ▲
                                      │
                 [ L'Artisan ]        │         [ Le Boss ]
         • Authentique > 80%          │   • Authentique > 80%
         • Cycles courts et réguliers │   • Résolution rapide (TTF faible)
         • Lecture attentive du sujet │   • Zéro acharnement
                                      │   • Autonomie totale
        ──────────────────────────────┼──────────────────────────────► TEMPS DE
                 [ Le Décrocheur ]    │         [ Le Cyborg ]         RÉSOLUTION
         • Hors IDE > 40%             │   • Authentique < 25%
         • Périodes IDLE > 15 min     │   • Navettes externes fréquentes
         • Abandon après erreurs      │   • Bursts massifs de lignes
                                      │
                                      ▼
```

### Description détaillée des profils :

1. **L'Artisan** :
   * Score d'authenticité $> 80\%$, ratio de backspaces $10-25\%$.
   * Cycles de développement courts et itératifs.
   * Temps de lecture du sujet régulier. Comportement idéal d'apprentissage.

2. **Le Boss** :
   * Progression linéaire très rapide, temps de résolution d'erreur quasi nul.
   * Zéro acharnement, zéro demande d'aide, code rédigé directement avec maîtrise.

3. **Le Cyborg** :
   * Score d'authenticité $< 25\%$, bursts massifs ($\ge 30$ lignes en quelques secondes).
   * Indice de navette externe élevé (allers-retours constants avec le navigateur).
   * Temps de codage actif très faible rapporté au volume de code final.

4. **Le Casse-cou** :
   * Indice d'acharnement stérile élevé ($\ge 5$ runs consécutifs en erreur en moins d'une minute).
   * L'étudiant tente des modifications au hasard sans lire les messages d'erreur.

5. **Le Décrocheur** :
   * Périodes d'inactivité prolongée $> 15$ minutes, temps passé hors de l'IDE $> 40\%$.
   * Très faible nombre total d'exécutions ; abandon rapide dès la première difficulté sans solliciter le tuteur.

6. **Le Pirate** :
   * Événements `FILE_DROP` détectés (importation de scripts complets).
   * Corrélations de synchronicité directes avec un camarade émetteur dans la salle.

---

## 5. Conclusions & Exploitation Pratique

### Pour la Remédiation en Temps Réel (Pendant le TP)
* L'interface tuteur peut signaler visuellement les étudiants dont l'**Indice d'Acharnement** dépasse un seuil ou dont le **Temps d'attente d'aide** excède 5 minutes, permettant une intervention ciblée sur les étudiants en détresse avant qu'ils ne décrochent.

### Pour l'Évaluation Sommative & la Notation (Après le TP)
* **Preuve matérielle opposable** : En cas de suspicion de triche ou de contestation, la trace fournit l'historique inaltérable seconde par seconde (focus perdu, texte collé, diff généré).
* **Valorisation de l'effort** : Un étudiant ayant une note finale moyenne mais présentant un profil *Artisan* (travail acharné, persévérance, authenticité à 90%) peut être valorisé par rapport à un étudiant ayant un résultat fonctionnel obtenu en 3 copier-coller externes (*Cyborg*).

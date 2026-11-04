# Système Télémétrique de CodeLab : Observables, Indicateurs et Alertes Temps Réel
> **Documentation Technique et Pédagogique de Référence**  
> *Version du document : 2.0 (Novembre 2026) — CodeLab v1.5.2*

---

## 1. Introduction & Philosophie Architecturale

Le système de traçabilité et d'analyse comportementale de CodeLab répond à un triple défi de l'enseignement contemporain de la programmation :
1. **L'intégrité académique à l'ère des LLM (ChatGPT, Copilot, etc.)** : distinguer une saisie humaine itérative d'une injection massive de code généré par IA ou copié d'un pair.
2. **La remédiation pédagogique proactive** : détecter en temps réel la détresse silencieuse d'un apprenant (acharnement stérile, erreurs répétées en boucle, décrochage) avant qu'il n'abandonne.
3. **L'évaluation formative et continue** : valoriser la démarche de résolution de problèmes et l'effort d'apprentissage (*profil Artisan*) au-delà de la simple conformité du code final.

### Architecture Globale du Flux de Données

Le système sépare clairement l'architecture en deux canaux complémentaires :
1. **Le Canal Temps Réel** (séance de TP) : boucle réactive ultra-légère entre l'étudiant, le serveur Rust et le tuteur.
2. **Le Canal Différé** (après la séance) : analyse longitudinale approfondie à partir du journal inaltérable.

#### Vue Synthétique : Le Pipeline en 3 Temps

```mermaid
flowchart LR
    A["1. COLLECTE<br/>Client Étudiant<br/><i>Frappes, Collages, Runs</i>"] 
    -->|"Événements bruts<br/>(&le; 1 Ko/min)"| B["2. TRAITEMENT & CALCULS<br/>Serveur Rust (Natif)<br/><i>Diffs, S_auth direct, Alertes</i>"]
    
    B -->|"Diffusion MsgPack<br/>Alertes &amp; Métriques"| C["3. SUPERVISION<br/>Poste Tuteur (UserTable)<br/><i>Badges prioritaires &amp; Infobulle</i>"]
    
    B -.->|"Journal immuable<br/>.activity.jsonl"| D["4. AUDIT DIFFÉRÉ<br/>Skills d'Évaluation<br/><i>Radars &amp; Fenêtres 15 min</i>"]
```

#### Vue Fonctionnelle : Les Deux Canaux d'Exploitation

```mermaid
flowchart TD
    E["<b>1. ÉTUDIANT (Client Java Swing)</b><br/>──────────────────────────────────────────────────────────<br/>Édition de code en direct, frappes clavier, sorties de focus, runs &amp; tests"]
    -->|"Sondes d'activité légères (≤ 1 Ko/min)"| S["<b>2. SERVEUR RUST ASYNCHRONE (Tokio)</b><br/>──────────────────────────────────────────────────────────<br/>• Traçabilité temps réel, calcul incrémental de S_auth et I_nav en direct<br/>• Détection continue des alertes : BURST, THRASH, PASTE, DROP<br/>• Automate d'auto-extinction contextuel d'alerte (Auto-Clear)"]

    S -->|"Diffusion instantanée d'état (MsgPack)"| T["<b>3. POSTE TUTEUR (Tableau de Bord UserTable)</b><br/>──────────────────────────────────────────────────────────<br/>• Badges compacts hiérarchisés : 🚨 Burst (+1), ⚠️ Blocage<br/>• Suivi temps réel du score d'authenticité et temps hors IDE<br/>• Infobulle HTML multi-lignes détaillée avec historique au survol"]
    T -.->|"Acquittement manuel clic droit (RESET_USER_ALERT)"| S

    S ==>|"Journalisation append-only continue"| J[("<b>Journal d'Activité Immuable (.activity.jsonl)</b><br/>──────────────────────────────────────────────────────────<br/>Fichiers d'événements append-only sécurisés persistés sur disque")]

    J --> A["<b>4. MOTEUR D'AUDIT &amp; ANALYTICS (Skills Python / CDK)</b><br/>──────────────────────────────────────────────────────────<br/>• Segmentation en fenêtres glissantes d'activité (15 minutes)<br/>• Classification automatisée selon les 6 profils didactiques types"]
    A --> R["<b>5. BILAN PÉDAGOGIQUE &amp; RESTITUTION</b><br/>──────────────────────────────────────────────────────────<br/>• Double diagramme radar Mermaid étalonné sur 20 points<br/>• Valorisation formative de la démarche d'apprentissage et de l'effort"]
```

### Principes Fondamentaux de Conception

1. **Faible Empreinte & Performance Critique** :
   - Écriture asynchrone non-bloquante au format *append-only*.
   - Débit réseau inférieur à **1 Ko par minute d'activité** par étudiant.
   - Les calculs d'analyse lourds ne bloquent jamais la boucle d'événements du serveur.
2. **Privacy by Design & Conformité RGPD** :
   - **Aucune interception textuelle arbitraire** : le contenu textuel du presse-papier système de l'OS n'est jamais aspiré ni stocké sur le serveur (aucun risque de fuite de mot de passe ou message privé). Seuls les volumes (nombre de caractères, nombre de lignes insérés) et les horodatages sont conservés.
   - Le code source n'est versionné que lorsqu'il est explicitement écrit dans l'espace de travail du projet.
3. **Résilience et Tolérance aux Pannes** :
   - En cas de redémarrage ou de déconnexion du tuteur, un snapshot complet des alertes actives est immédiatement resynchronisé à la connexion.

---

## 2. Ontologie des 11 Observables Bruts (Collecte)

Les observables constituent le socle factuel et inaltérable de la traçabilité. Ils sont stockés chronologiquement dans `sessions/<session>/<login>/.activity.jsonl`.

### Matrice des Primitives Télémétriques

| Type d'Événement | Origine | Point d'ancrage Code Source | Description & Déclencheur |
| :--- | :---: | :--- | :--- |
| `SESSION` | Serveur | [`handler.rs`](file:///Users/lehuen/dev/codelab/server/server/src/handler.rs) | Connexion (`CONNECTED`) avec IP source ou déconnexion (`DISCONNECTED`) avec durée totale. |
| `VIEW` | Serveur | [`handler.rs`](file:///Users/lehuen/dev/codelab/server/server/src/handler.rs) | Consultation ou ouverture d'un document (sujet Markdown, fichier source, documentation). |
| `FILE_OP` | Serveur | [`handler.rs`](file:///Users/lehuen/dev/codelab/server/server/src/handler.rs) | Manipulation de structure : création (`CREATE`), suppression (`DELETE`), renommage (`RENAME`). |
| `FILE_DROP` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Glisser-déposer d'un fichier externe depuis le bureau de l'OS ou un support amovible. |
| `TYPING` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Frappes réelles cumulées (`typed_chars`) et corrections (`backspaces`) dédupliquées par fenêtre d'édition. |
| `FOCUS` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Alt-Tab ou changement d'application : perte (`LOST`) et reprise (`GAINED`) avec durée d'absence. |
| `PASTE` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Opération Ctrl+V / Cmd+V avec volume de caractères et de lignes insérés. |
| `EDIT` | Serveur | [`activity.rs`](file:///Users/lehuen/dev/codelab/server/server/src/activity.rs) | Diff textuel unifié à la sauvegarde : lignes ajoutées/supprimées, taille du plus grand bloc (`chunk_max`). |
| `COMPILE` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Verdict compilateur (`OK`, `ERROR`), durée de compilation et extrait de diagnostic. |
| `EXECUTE` | Client | [`AbstractCodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/AbstractCodeLab.java) | Cycle d'exécution (`STARTED`, `DONE`, `ERROR`, `HALTED`), code retour et diagnostic d'erreur/traceback. |
| `HELP` | Serveur | [`handler.rs`](file:///Users/lehuen/dev/codelab/server/server/src/handler.rs) | Cycle d'appel tuteur : demande (`REQUESTED`), annulation (`CANCELLED`), résolution (`RESOLVED`). |

### Schémas JSON Détaillés des Primitives

```json
{"type":"SESSION","timestamp":"2026-11-04T08:00:00Z","session":"TP1","login":"etudiant1","status":"CONNECTED","ip":"172.20.10.42"}
{"type":"VIEW","timestamp":"2026-11-04T08:01:10Z","session":"TP1","login":"etudiant1","file":"enonce.md"}
{"type":"FILE_OP","timestamp":"2026-11-04T08:02:15Z","session":"TP1","login":"etudiant1","op":"CREATE","file":"exercice1.py"}
{"type":"TYPING","timestamp":"2026-11-04T08:05:00Z","session":"TP1","login":"etudiant1","file":"exercice1.py","typed_chars":145,"backspaces":18}
{"type":"FOCUS","timestamp":"2026-11-04T08:12:00Z","session":"TP1","login":"etudiant1","status":"LOST"}
{"type":"FOCUS","timestamp":"2026-11-04T08:13:45Z","session":"TP1","login":"etudiant1","status":"GAINED","duration_sec":105}
{"type":"PASTE","timestamp":"2026-11-04T08:13:48Z","session":"TP1","login":"etudiant1","file":"exercice1.py","chars":380,"lines":18}
{"type":"EDIT","timestamp":"2026-11-04T08:13:52Z","session":"TP1","login":"etudiant1","file":"exercice1.py","total_lines":35,"lines_added":18,"lines_removed":0,"chunk_max":18,"dt_sec":4,"alert":"BURST_INSERT"}
{"type":"FILE_DROP","timestamp":"2026-11-04T08:20:00Z","session":"TP1","login":"etudiant1","file":"helper.py","size_bytes":4096}
{"type":"COMPILE","timestamp":"2026-11-04T08:22:10Z","session":"TP1","login":"etudiant1","file":"main.c","status":"ERROR","error_msg":"undefined reference to `init_buffer'"}
{"type":"EXECUTE","timestamp":"2026-11-04T08:25:00Z","session":"TP1","login":"etudiant1","file":"exercice1.py","status":"ERROR","exit_code":1,"error_msg":"IndexError: list index out of range"}
{"type":"HELP","timestamp":"2026-11-04T08:26:00Z","session":"TP1","login":"etudiant1","status":"REQUESTED"}
{"type":"HELP","timestamp":"2026-11-04T08:29:30Z","session":"TP1","login":"etudiant1","status":"RESOLVED","tutor":"hamdi"}
{"type":"EXECUTE","timestamp":"2026-11-04T08:31:00Z","session":"TP1","login":"etudiant1","file":"exercice1.py","status":"DONE","exit_code":0}
{"type":"SESSION","timestamp":"2026-11-04T10:00:00Z","session":"TP1","login":"etudiant1","status":"DISCONNECTED","duration_sec":7200}
```

---

## 3. Moteur de Dérivation & Indicateurs Pédagogiques

Le moteur d'analytics ([`activity_analyzer.py`](file:///Users/lehuen/dev/codelab/client/codelab.files/skills/activity_analyzer.py)) agrège les observables pour produire des métriques formelles de haut niveau.

### A. Indicateurs d'Intégrité & de Genèse du Code

#### 1. Score d'Authenticité (S_auth)
Mesure la proportion de matière textuelle rédigée via frappe humaine directe face aux injections massives externes :

$$S_{\mathrm{auth}} = \frac{N_{\mathrm{typed}}}{N_{\mathrm{typed}} + N_{\mathrm{pasted}} + N_{\mathrm{dropped}}} \times 100$$

* **Interprétation** :
  * **S_auth ≥ 70%** : **Élevée** (saisie artisanale, appropriation du problème).
  * **40% ≤ S_auth < 70%** : **Modérée** (quelques collages ou réutilisations d'exemples).
  * **S_auth < 40%** : **Suspecte / Faible** (forte prédominance d'injections externes ou génération IA).

#### 2. Indice de Navette Externe (I_nav)
Quantifie la tendance de l'étudiant à travailler hors de l'environnement de développement :

$$I_{\mathrm{nav}} = \frac{\sum t_{\mathrm{away}}}{T_{\mathrm{session}}} \times 100$$

* **Pattern "Prompt IA / Copier-Coller"** :
  `FOCUS(LOST, Δt ∈ [20s, 180s])` ──▶ `FOCUS(GAINED)` ──▶ `PASTE(chars ≥ 150)`
  
  La détection de ce motif **≥ 3 fois** caractérise un cycle de requête de solution externe systématique.

#### 3. Taux de Friction Cognitive (T_fric)
Mesure l'hésitation rédactionnelle et les reprises de code :

$$T_{\mathrm{fric}} = \frac{N_{\mathrm{backspaces}}}{N_{\mathrm{typed}}} \times 100$$

* **T_fric < 3%** : Dictée passive, recopie mécanique sans aucune hésitation.
* **10% ≤ T_fric ≤ 25%** : Rythme normal de réflexion et de réajustement syntaxique.
* **T_fric > 40%** : Tâtonnement aveugle, grande confusion algorithmique.

---

### B. Indicateurs de Méthodologie & Démarche Expérimentale

#### 1. Indice d'Acharnement Stérile (I_thrash)
Représente la part des exécutions ou compilations menées sans réflexion préalable :

$$I_{\mathrm{thrash}} = \frac{\sum \text{Runs en acharnement}}{\text{Total des Runs}} \times 100$$

* **Définition d'un épisode d'acharnement** : séquence d'au moins 3 exécutions consécutives soldées par `ERROR` avec Δt ≤ 30 secondes ou modification textuelle ≤ 5 caractères entre chaque tentative.

#### 2. Temps Moyen de Résolution d'Erreur (TTF — *Time To Fix*)
Durée moyenne écoulée entre l'apparition de la première erreur d'exécution sur un fichier et le retour au premier run `DONE` / `OK` :

$$\overline{\mathrm{TTF}} = \frac{1}{K} \sum_{k=1}^K (t_{\mathrm{resolved}, k} - t_{\mathrm{error}, k})$$

* **TTF moyen < 30 s** : Erreur d'inattention ou typo rapidement corrigée.
* **TTF moyen ∈ [1 min, 5 min]** : Démarche analytique saine de débogage.
* **TTF moyen > 15 min** : Blocage conceptuel sévère nécessitant une intervention.

---

### C. Budget Temps en 4 Quadrants

La durée totale de session est ventilée en 4 dimensions exclusives :

| Quadrant | Intitulé & Événements associés | Part Typique |
| :--- | :--- | :---: |
| **Q1** | **Temps de Lecture / Spécification** (consultation de l'énoncé ou des headers via `VIEW`) | 15% |
| **Q2** | **Temps d'Édition Active** (focus dans l'IDE avec frappe continue `TYPING`) | 45% |
| **Q3** | **Temps de Réflexion / Diagnostic** (focus dans l'IDE sans frappe, pause cognitive) | 25% |
| **Q4** | **Temps Hors IDE** (cumul des périodes d'absence `FOCUS_LOST`) | 15% |

$$\sum_{i=1}^4 Q_i = 100\%$$

---

### D. Dynamique Temporelle par Fenêtres Glissantes (*Sliding Windows*)

Une moyenne globale sur 2 heures peut masquer un basculement tardif vers une IA générative en fin de séance. Le moteur segmente donc la session en tranches continues de Δt = 15 minutes :

$$\mathcal{E}_k = \{ e \in \mathcal{E} \mid t_k \le t(e) < t_k + \Delta t \}$$

Pour chaque tranche temporelle k, le moteur calcule localement :
* **S_auth(k)** : score d'authenticité de la tranche.
* **I_nav(k)** : ratio de temps hors IDE sur la tranche.
* **Typologie de Phase** : qualification algorithmique de l'activité.

#### Matrice de Qualification de Phase

| Phase Qualifiée | Critères de Décision |
| :--- | :--- |
| `✅ Saisie autonome active` | S_auth ≥ 70% et volume de frappe > 80 car/min |
| `⚙️ Validation & Tests` | Runs fréquents (> 2), taux de succès ≥ 60%, S_auth ≥ 60% |
| `📖 Réflexion / Lecture` | Faible frappe (< 30 car), temps de lecture/consultation prédominant |
| `⚠️ Acharnement sur erreurs` | ≥ 3 exécutions consécutives soldées par un échec |
| `🚨 Décrochage IA / Injection` | S_auth < 40% avec présence de `BURST` ou I_nav > 50% |
| `📥 Dépôt externe` | Au moins un événement `FILE_DROP` sur la tranche |

---

## 4. Système d'Indicateurs en Direct, Alertes & Supervision Tuteur

Le serveur Rust évalue en temps réel chaque observable reçu au sein de son module [`activity.rs`](file:///Users/lehuen/dev/codelab/server/server/src/activity.rs). Il maintient pour chaque étudiant un état d'indicateurs dynamiques (`StudentLiveStatus` / `LiveMetrics`) et une collection d'alertes en mémoire (`HashMap<String, LiveAlert>`).

### A. Indicateurs Calculés en Direct par le Serveur Rust

Sans dépendre d'aucun interpréteur externe, le serveur calcule de manière incrémentale :
* **Score d'Authenticité en direct (S_auth)** : rapport continu entre les caractères issus de la frappe clavier réelle et le volume total intégrant collages et bursts.
* **Taux de Navette Hors IDE (I_nav)** : ratio de temps passé hors focus applicatif par rapport à la durée totale écoulée.
* **Compteur d'Acharnement** : suivi des échecs consécutifs sans succès intermédiaire.
* **Replay Résilient** : en cas de reconnexion d'un étudiant ou de redémarrage du serveur, l'état `StudentLiveStatus` est automatiquement reconstruit en quelques millisecondes à partir du journal local `.activity.jsonl`.

Ces indicateurs sont transmis aux postes superviseurs dans la structure `UserData` (`s_auth`, `i_nav`) et consultables en temps réel dans l'infobulle de chaque étudiant.

### B. Matrice des Alertes Détectées en Flux Continu

| Code Alerte | Niveau de Sévérité | Condition de Déclenchement | Règle d'Extinction (*Auto-Clear*) |
| :---: | :---: | :--- | :--- |
| `BURST` | `CRIT` | ≥ 20 lignes insérées en ≤ 15s (`BURST_INSERT`) ou nouveau fichier créé avec ≥ 25 lignes d'un coup (`BURST_NEW_FILE`). | Acquittement manuel par le tuteur au poste. |
| `PASTE` | `WARN` | Collage unitaire ≥ 15 lignes ou ≥ 400 caractères. | Acquittement manuel par le tuteur au poste. |
| `THRASH` | `WARN` | ≥ 3 compilations `ERROR` ou exécutions `ERROR` consécutives sans run réussi intercalé. | **Auto-extinction automatique** dès :<br>1. Compilation réussie (`status: OK`)<br>2. Exécution réussie (`status: DONE`)<br>3. Modification de code ≥ 3 lignes. |
| `DROP` | `WARN` | Dépôt externe de fichier dans l'arborescence du projet via drag & drop. | Acquittement manuel par le tuteur au poste. |

### Gestion Multi-Alertes & Hiérarchie de Sévérité

Pour éviter qu'une alerte secondaire ne masque une alerte critique (ex: un collage mineur `PASTE` recouvrant une injection massive `BURST`), l'état étudiant consolide l'ensemble des alertes actives :

| Échelon | Rôle & Structure | Comportement & Rendu |
| :--- | :--- | :--- |
| **Stockage Mémoire** | `HashMap<String, LiveAlert>` | Cumul non-destructif de toutes les alertes simultanées par étudiant. |
| **Priorité de Sévérité** | `CRIT (1) > WARN (2) > INFO (3)` | Sélection du statut le plus urgent pour déterminer la couleur de la cellule. |
| **Présentation IHM** | Badge compact `UserTable` | Affichage du picto prioritaire et du compteur résiduel : `🚨 Burst (+1)`. |

1. **Hiérarchie Stricte** : `CRIT > WARN > INFO`.
2. **Badge Compact** : affiche le pictogramme et l'intitulé de l'alerte la plus prioritaire, augmenté d'un compteur d'alertes résiduelles s'il y en a plusieurs (ex: `🚨 Burst (+1)` ou `⚠️ Blocage (+2)`).
3. **Coloration Dynamique de Cellule** :
   - Fond **rouge clair** / texte **rouge vif** si au moins une alerte `CRIT` est active.
   - Fond **ambre clair** / texte **orange foncé** si uniquement des alertes `WARN` sont actives.

### Spécification du Protocole Réseau (MsgPack)

La communication bidirectionnelle entre le serveur Rust et les clients de supervision tuteurs s'effectue par des messages légers encodés en MessagePack :

#### 1. Diffusion d'une Alerte : `SET_USER_ALERT`
Diffusé en broadcast asynchrone Tokio à tous les tuteurs connectés de la session :

```json
{
  "command": "SET_USER_ALERT",
  "login": "etudiant1",
  "alerts_json": "{\"BURST\":{\"kind\":\"BURST\",\"level\":\"CRIT\",\"message\":\"+35L insérées d'un bloc (tp1.py)\",\"timestamp\":\"14:22:15\"},\"THRASH\":{\"kind\":\"THRASH\",\"level\":\"WARN\",\"message\":\"3 échecs consécutifs d'exécution\",\"timestamp\":\"14:25:30\"}}"
}
```

#### 2. Réinitialisation / Acquittement : `RESET_USER_ALERT`
* **Envoyé par le tuteur** lors d'un clic droit pour acquitter :
  ```json
  { "command": "RESET_USER_ALERT", "login": "etudiant1", "kind": "ALL" }
  ```
* **Diffusé par le serveur** aux autres tuteurs pour synchroniser la suppression d'une alerte éteinte :
  ```json
  { "command": "RESET_USER_ALERT", "login": "etudiant1", "kind": "THRASH" }
  ```

---

## 5. Interface Tuteur & Expérience Utilisateur (`UserTable`)

La supervision en classe s'opère via la table des étudiants ([`UserTable.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/usertable/UserTable.java)).

| Étudiant | Groupe | Connecté | Activité | Alerte (5e colonne) |
| :--- | :---: | :---: | :---: | :--- |
| **lmartin** | G1 | En ligne | Il y a 10s | `🚨 Burst (+1)` *(fond rouge)* |
| **tdubois** | G1 | En ligne | Il y a 4s | `⚠️ Blocage` *(fond ambre)* |
| **lbernard** | G2 | En ligne | Il y a 1m | `📋 Paste` *(fond ambre)* |
| **cpetit** | G2 | En ligne | Il y a 15s | — *(neutre)* |

### Infobulle HTML Enrichie

Au survol de la cellule Alerte d'un étudiant, une infobulle HTML dynamique affiche l'inventaire chronologique et détaillé de toutes les anomalies actives :

```html
<html>
  <body style='font-family:sans-serif; margin:2px;'>
    <b>Alertes actives (2) :</b><br/>
    • <font color='#c0392b'><b>🚨 BURST (14:22:15)</b></font> : +35L insérées d'un bloc (tp1.py)<br/>
    • <font color='#d35400'><b>⚠️ THRASH (14:25:30)</b></font> : 3 échecs consécutifs d'exécution
  </body>
</html>
```

### Interaction Tuteur (Acquittement Contextuel)

* **Menu Clic Droit** : L'enseignant clique sur l'étudiant dans la liste et sélectionne :
  > **« Acquitter les alertes de [Nom] (2) »**
* **Comportement Réseau** : L'IHM envoie immédiatement `RESET_USER_ALERT` au serveur, qui purge le statut mémoire et propage l'extinction à l'ensemble des tuteurs de la salle, signalant que l'étudiant a été pris en charge.

---

## 6. Typologie des Profils Étudiants (Clusters Pédagogiques)

L'audit de post-traitement classe automatiquement les étudiants selon 6 profils didactiques types articulés autour de la matrice d'authenticité et de vélocité de résolution :

| Authenticité (S_auth) / Résolution | Résolution Rapide (TTF faible / Autonomie) | Résolution Lente ou Hésitante |
| :--- | :--- | :--- |
| **Authenticité Élevée (S_auth > 80%)** | **Le Boss**<br>• Résolution immédiate (TTF quasi nul)<br>• Zéro acharnement<br>• Autonomie complète | **L'Artisan**<br>• Cycles courts et itératifs<br>• Lecture attentive du sujet<br>• Démarche d'apprentissage idéale |
| **Authenticité Faible (S_auth < 30%)** | **Le Cyborg**<br>• Navettes externes fréquentes (IA)<br>• Bursts massifs de lignes<br>• Faible volume de frappe manuelle | **Le Décrocheur**<br>• Temps hors IDE > 40%<br>• Périodes Idle > 15 min<br>• Abandon face aux blocages |

### 1. L'Artisan (Comportement d'Apprentissage Exemplaire)
* **Indicateurs** : S_auth > 80%, T_fric ∈ [10%, 25%], I_thrash < 10%.
* **Démarche** : Cycles de développement courts et incrémentaux, lecture approfondie du sujet avant de coder, résolution méthodique des erreurs étape par étape.

### 2. Le Boss (Expertise et Maîtrise Directe)
* **Indicateurs** : S_auth > 80%, TTF < 30 s, zéro demande d'aide tuteur, cadence de frappe élevée.
* **Démarche** : L'étudiant maîtrise parfaitement les concepts et déroule la solution sans friction notable.

### 3. Le Cyborg (Assistance IA / Solution Importée)
* **Indicateurs** : S_auth < 30%, alertes `BURST` multiples, I_nav > 40%, T_fric < 5%.
* **Démarche** : Absence de genèse itérative du code. L'étudiant formule des requêtes sur un modèle externe et injecte des blocs complets de 30 à 60 lignes sans les retoucher.

### 4. Le Casse-Cou (Acharnement Stérile / Tâtonnement Aveugle)
* **Indicateurs** : I_thrash > 35%, ratio d'erreurs d'exécution > 70%, Δt entre runs < 15 s.
* **Démarche** : L'étudiant ne consulte pas la sortie console ni le traceback. Il modifie un caractère au hasard et relance en boucle en espérant un résultat positif.

### 5. Le Décrocheur (Perte de Pied et Abandon Silencieux)
* **Indicateurs** : Temps hors IDE > 50%, périodes d'inactivité > 15 min, moins de 3 runs sur l'ensemble de la séance.
* **Démarche** : Face à une difficulté initiale non résolue, l'étudiant renonce à chercher et attend la fin de séance sans solliciter l'encadrement.

### 6. Le Pirate (Collusion en Salle / Import Direct)
* **Indicateurs** : Présence d'événements `FILE_DROP`, corrélation de synchronicité (≤ 90 s) avec la réussite d'un poste voisin.
* **Démarche** : Récupération directe d'un fichier source par clé USB, partage local ou messagerie instantanée.

---

## 7. Guide d'Exploitation pour les Enseignants & Tuteurs

### Pendant la Séance (Pilotage Temps Réel)
1. **Prioriser les passages au poste** :
   - Surveiller en premier lieu les badges **`⚠️ Blocage` (`THRASH`)** : ils signalent des étudiants qui travaillent activement mais sont coincés dans une impasse conceptuelle.
   - Vérifier les badges **`🚨 Burst`** : un rapide coup d'œil permet de s'assurer que l'étudiant est capable d'expliquer oralement les 30 lignes qu'il vient d'insérer.
2. **Utiliser l'Auto-Acquittement** :
   - Il n'est pas nécessaire d'acquitter manuellement les alertes de blocage : si l'étudiant comprend votre conseil et réussit son run suivant, l'alerte s'éteint automatiquement dans les 2 secondes sur votre écran.

### Après la Séance (Bilan d'Audit & Évaluation)
1. **Lancer l'Analyseur** :
   ```bash
   python3 client/codelab.files/skills/activity_analyzer.py sessions/2026-TP1/
   ```
2. **Consulter les Fenêtres Glissantes** :
   - Le tableau chronologique des tranches de 15 minutes permet d'identifier à la minute près l'instant de bascule vers une IA générative (chute brutale de S_auth).
3. **Valorisation à l'Évaluation** :
   - Deux étudiants obtenant la note de 14/20 aux tests unitaires automatiques peuvent être différenciés : accorder un bonus de démarche à un profil *Artisan* persévérant, et conditionner la note d'un profil *Cyborg* à une soutenance orale d'explication du code.


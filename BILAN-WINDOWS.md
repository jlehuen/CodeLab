# Bilan Technique : Packaging Windows Moderne, Robustesse Chemins & Virtualisation UTM

> **Date** : 24 septembre 2026  
> **Projet** : CodeLab (Client v2 Java & Environnement de Simulation)  
> **Auteurs** : Jérôme Lehuen & Antigravity  
> **Version cible** : CodeLab 1.4.1 (build 2609241824)  

---

## 1. Contexte & Objectifs

Historiquement, la distribution de CodeLab sous Windows reposait sur une archive `.zip` brute contenant l'arborescence complète (JDK 17 embarqué, binaires, bibliothèques natives, lanceurs `.bat` et `codelab.exe`). Cette approche présentait plusieurs limites majeures pour les élèves et les enseignants :
- **Absence d'assistant d'installation** : L'utilisateur devait extraire manuellement le ZIP sans garantie d'emplacement approprié, puis créer manuellement ses raccourcis.
- **Chemins avec espaces sous Windows** : L'installation dans des dossiers comme `C:\Program Files\...` ou sous des profils utilisateurs contenant des espaces (ex: `C:\Users\Jean Dupont\...`) provoquait des ruptures lors de l'appel aux compilateurs et outils externes (GCC, Python, Java, AStyle).
- **Crash au démarrage sur configuration par défaut** : Si les propriétés réseau `SERVER_PORT` étaient laissées vides dans `user.properties`, l'application levait une `NumberFormatException` fatale au premier lancement.
- **Pollution de métadonnées macOS** : Les archives générées depuis macOS intégraient des fichiers cachés (`.DS_Store`, AppleDouble `._*`) et des scripts batch en fins de ligne Unix (LF au lieu de CRLF).

### Objectifs atteints en Version 1.4.1 :
1. **Packaging moderne NSIS (0 € de coût)** : Génération d'un installeur unique `CodeLab-Win64-1.4.1-Setup.exe` par utilisateur (`RequestExecutionLevel user`), ne requérant aucun privilège administrateur, créant les raccourcis Bureau et Menu Démarrer, et s'enregistrant proprement dans les paramètres de désinstallation de Windows.
2. **Résistance absolue aux espaces dans les chemins** : Tokenisation robuste de toutes les lignes de commande via `Utils.splitCommandLine` avec prise en charge des guillemets et réassemblage heuristique des chemins Windows non quotés.
3. **Démarrage fiabilisé & propriétés neutres** : Prise en charge sécurisée des champs serveur vides, sans injection de données universitaires dans les fichiers par défaut.
4. **Protocole de test et virtualisation documenté** : Procédure reproductible pour tester les exécutables Windows sur Mac Apple Silicon via UTM (contournement OOBE, pilotes SPICE, correctif WebDAV 50 Mo).

---

## 2. Choix d'Architecture : NSIS vs MSIX

| Critère | Format MSIX / AppX | Installeur NSIS (MUI2) retenu |
| :--- | :--- | :--- |
| **Coût financier** | Payant (exige un certificat de signature de code EV à 200–500 €/an) | **0 €** (aucun coût, 100% open-source) |
| **Exécution sans signature** | Bloquée par Windows SmartScreen et la stratégie d'entreprise | Exécution autorisée avec simple avertissement SmartScreen standard |
| **Droits d'administration** | Sandbox UWP / Windows Store | **Non requis** (`RequestExecutionLevel user` dans `%LOCALAPPDATA%\Programs`) |
| **Cross-compilation macOS** | Complexe, nécessite des outils tiers propriétaires | **Immédiate** via `brew install makensis` |
| **Intégration système** | Registre applicatif UWP | Raccourcis Bureau, Menu Démarrer, désinstalleur `Uninstall.exe` enregistré |

---

## 3. Mise en Œuvre du Packaging NSIS

### 3.1. Script NSIS (`launchers/windows/codelab.nsi`)
Le script s'appuie sur l'interface moderne **MUI2** (Modern User Interface 2) :
- **Portée utilisateur** : Défini avec `RequestExecutionLevel user`. Les fichiers sont copiés dans `$LOCALAPPDATA\Programs\CodeLab`, éliminant toute invite UAC (User Account Control) bloquante en salle de TP ou sur PC familial.
- **Compression solide LZMA** : Réduction maximale du volume de téléchargement tout en embarquant le JDK 17 complet.
- **Raccourcis & Icônes** :
  ```nsis
  CreateShortcut "$DESKTOP\CodeLab.lnk" "$INSTDIR\codelab.exe" "" "$INSTDIR\codelab.ico" 0 SW_SHOWNORMAL "" "CodeLab IDE & Simulators"
  CreateDirectory "$SMPROGRAMS\CodeLab"
  CreateShortcut "$SMPROGRAMS\CodeLab\CodeLab.lnk" "$INSTDIR\codelab.exe" "" "$INSTDIR\codelab.ico" 0 SW_SHOWNORMAL "" "CodeLab IDE & Simulators"
  CreateShortcut "$SMPROGRAMS\CodeLab\Uninstall CodeLab.lnk" "$INSTDIR\Uninstall.exe"
  ```
- **Désinstallation conforme** : Enregistrement dans la clé de registre `HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\CodeLab` permettant une désinstallation en un clic depuis « Paramètres Windows > Applications installées ».

### 3.2. Automatisation macOS (`distrib-win64-nsis.sh`)
Le script de génération :
1. Purge récursivement les fichiers parasites macOS :
   ```bash
   find "$WORK_DIR" -name ".DS_Store" -delete
   find "$WORK_DIR" -name "._*" -delete
   ```
2. Convertit tous les fichiers de lancement batch en terminaisons CRLF (`\r\n`).
3. Compile l'installeur via `makensis -DVERSION="$VERSION" ...`.
4. Calcule et produit automatiquement les condensats d'intégrité **MD5** et **SHA-1**.

---

## 4. Résistance Totale aux Espaces dans les Chemins de Fichiers

### 4.1. Le Problème
Historiquement, les commandes vers les outils externes étaient découpées via des expressions régulières simplistes :
```java
// Fragile : casse dès qu'un chemin contient un espace (ex: "C:\Program Files\...")
String[] parts = cmd.trim().split("\\s+");
```
Si l'utilisateur installait CodeLab ou un compilateur sous un chemin avec espaces, `ProcessBuilder` recevait des arguments fragmentés (`"C:\\Program"`, `"Files\\..."`), entraînant l'échec de compilation ou d'exécution (`file not found`).

### 4.2. La Solution : `Utils.splitCommandLine`
Une machine à états de tokenisation dédiée a été intégrée dans [`Utils.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/utils/Utils.java) :
- Respecte les guillemets doubles (`"..."`) et simples (`'...'`).
- Préserve les espaces protégés à l'intérieur des chaînes de caractères.
- **Heuristique de recollement pour Windows** : Si le premier argument non quoté ne correspond pas à un exécutable existant sur le disque, l'algorithme teste itérativement la concaténation avec les tokens suivants jusqu'à trouver un fichier existant (ex: `C:\Program` + `Files\...` ➔ `C:\Program Files\...`).

```java
public static List<String> splitCommandLine(String cmd) {
    if (cmd == null || cmd.isBlank()) return Collections.emptyList();
    List<String> tokens = new ArrayList<>();
    // Tokenisation avec support des guillemets
    ...
    // Heuristique de résolution des chemins Windows non quotés
    if (IS_WINDOWS && !tokens.isEmpty() && !new File(tokens.get(0)).exists()) {
        StringBuilder candidate = new StringBuilder(tokens.get(0));
        int mergeCount = 0;
        for (int i = 1; i < tokens.size(); i++) {
            candidate.append(" ").append(tokens.get(i));
            if (new File(candidate.toString()).exists()) {
                mergeCount = i;
                break;
            }
        }
        ...
    }
    return tokens;
}
```

### 4.3. Déploiement dans le Codebase
La méthode est systématiquement employée dans :
- [`Compilateur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Compilateur.java) (GCC, Java, Go, Haskell, Processing).
- [`Executeur.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/Executeur.java) (Python, Java, CLIPS, programmes exécutables).
- [`TextEditor.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/modules/editeur/text/TextEditor.java) (formateur de code AStyle).
- [`CodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java) (détection des environnements et bibliothèques).

---

## 5. Résolution du Crash de Démarrage (`NumberFormatException`)

Dans [`CodeLab.java`](file:///Users/lehuen/dev/codelab/client/src/codelab/CodeLab.java#L1283), le port serveur était parsé sans test préalable :
```java
// Bogue historique : exception levée si SERVER_PORT=""
SERVER_PORT = Integer.parseInt(userProperties.getProperty("SERVER_PORT"));
```
### Correctif
- Affectation sécurisée : si `SERVER_PORT` est vide ou absent, la valeur reste `""`.
- La construction de l'URL administrative `ADMIN_URL` est conditionnée par `!SERVER_HOST.isBlank() && !SERVER_PORT.isBlank()`.
- Rétablissement de la neutralité stricte des fichiers de configuration par défaut (`user-win.properties`, `user-mac.properties`, `user-linux.properties`), sans données universitaires codées en dur.

---

## 6. Protocole de Test sur Mac Apple Silicon (UTM / QEMU)

Pour tester de manière autonome l'installeur Windows sans poste physique dédié sous la main :

1. **Obtention de l'ISO Windows 11 ARM64** :
   - Utiliser **CrystalFetch** (application macOS open-source disponible sur le Mac App Store ou GitHub) pour télécharger l'ISO officielle Microsoft pour architecture ARM64.
2. **Création de la VM dans UTM** :
   - Mode : **Virtualiser** (pas d'émulation, performance native ARM64).
   - Système : Windows (Windows 11).
   - Cocher l'installation automatique des pilotes **SPICE Guest Tools**.
3. **Contournement du compte Microsoft obligatoire (OOBE)** :
   - Sur l'écran de sélection du pays lors de l'installation :
     1. Appuyer sur `Maj + F10` (ou `Fn + Maj + F10`) pour ouvrir l'invite de commande.
     2. Taper : `OOBE\BYPASSNRO` et valider par Entrée.
     3. La machine redémarre. Choisir ensuite « Je n'ai pas Internet » pour créer un compte local hors-ligne.
4. **Pilotes SPICE Guest Tools** :
   - Monter l'ISO `spice-guest-tools-xxx.iso` dans le lecteur virtuel UTM et exécuter l'installateur dans Windows pour activer le plein écran dynamique, le presse-papier partagé et le dossier partagé WebDAV.
5. **Résolution de l'Erreur WebDAV `0x800700DF` (Limite de 50 Mo)** :
   - Par défaut, le service Windows `WebClient` refuse le transfert de fichiers de plus de 50 Mo via le dossier partagé UTM.
   - **Correctif permanent** : Ouvrir PowerShell en Administrateur dans la VM et exécuter :
     ```powershell
     Set-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Services\WebClient\Parameters' -Name 'FileSizeLimitInBytes' -Value 4294967295 ; Restart-Service WebClient
     ```
   - Permet de transférer instantanément l'installateur CodeLab de 211 Mo depuis le Mac vers la VM Windows.

---

## 7. Synthèse des Bénéfices

- **Expérience Utilisateur** : Installation en 2 clics sans droit admin, icônes automatiques, désinstallation propre.
- **Fiabilité** : Prise en charge sans faille des dossiers avec espaces et accents, démarrage robuste en mode hors-ligne.
- **Maintenabilité** : Chaîne de packaging reproductible à 0 € intégrée aux scripts de release de CodeLab.

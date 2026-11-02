<style>
	html {
		scroll-behavior: smooth;
	}
	img[src$="center"] {
		display: block;
		margin: 0 auto;
	}
	img[src$="noborder"] {
		border-style: none;
		background-color: none;
	}
	img[src$="center+margin"] {
		display: block;
		margin: 0 auto;
		margin-top: 25px;
		margin-bottom: 25px;
	}
	img[src$="center+noborder"] {
		display: block;
		margin: 0 auto;
		border-style: none;
		background-color: none;
	}
	pre code {
		background: transparent;
	}
	:not(pre) > code {
		background: yellow;
	}
</style>

# ⚡ Guide de Démarrage Rapide

Bienvenue dans le guide express de **CodeLab**. Ce guide est conçu pour vous permettre de lancer l'environnement, d'écrire et d'exécuter votre premier programme en **moins de 2 minutes**, sans prérequis complexe.

---

## 1. Télécharger et lancer CodeLab

CodeLab est fourni sous forme d'application autonome intégrant son propre environnement d'exécution (aucune installation de Java ou de compilateur n'est requise pour débuter).

Téléchargez la version correspondant à votre système depuis la page des [Releases officielles](https://github.com/jlehuen/CodeLab/releases/latest) :

| Système | Téléchargement direct | Premier lancement |
| :--- | :--- | :--- |
| **Windows** | [**`CodeLab-Win64-Setup.exe`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Win64-1.5.2-Setup.exe) | Lancez l'installeur (si l'écran bleu *SmartScreen* apparaît, cliquez sur *Informations complémentaires* puis *Exécuter quand même*). Aucun droit administrateur requis. |
| **macOS** | [**`CodeLab-MacOS-Silicon.dmg`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-MacOS-Silicon-1.5.2.dmg) (Apple Silicon)<br>[**`CodeLab-MacOS-Intel.dmg`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-MacOS-Intel-1.5.2.dmg) (Intel) | Ouvrez le `.dmg`, glissez `CodeLab.app` dans *Applications*. À la première ouverture, faites un **clic droit > Ouvrir** pour autoriser le lancement. |
| **Linux** | [**`CodeLab-Linux.zip`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Linux-1.5.2.zip) | Décompressez l'archive, entrez dans le dossier et double-cliquez sur `codelab.sh` (ou `./codelab.sh` dans un terminal). |

---

## 2. L'interface en un coup d'œil

À l'ouverture de CodeLab, l'environnement se présente sous une interface claire et épurée :

![](../img/codelab_1.png?style=center+margin)

1. **Onglets de modules (en haut)** : basculez entre l'éditeur de code principal et les simulateurs interactifs intégrés (Robotique Lego 2D, Robotique 3D, Moteur NXT, Tracé 2D, etc.).
2. **Barre d'outils (en haut)** : contient les boutons essentiels, dont le bouton vert **▶️ Exécuter** (raccourci `F5`) et le bouton rouge **⏹️ Arrêter**.
3. **Zone d'édition (au centre)** : votre espace pour écrire le code source avec coloration syntaxique, numérotation des lignes et auto-complétion.
4. **Console d'exécution (en bas)** : affiche instantanément les sorties texte de votre programme (`print`, `printf`, erreurs, saisies utilisateur).

---

## 3. Votre premier programme en 30 secondes

Choisissez votre langage préféré et lancez votre premier test :

### Option A : En Python
1. Cliquez sur le menu **Fichier > Nouveau** et sélectionnez **Python** (ou ouvrez un fichier `.py`).
2. Saisissez le code suivant :
```python
print("Bienvenue dans CodeLab !")
for i in range(1, 6):
    print(f"Étape {i} : tout fonctionne parfaitement.")
```
3. Cliquez sur le bouton vert **▶️ Exécuter** (ou appuyez sur **`F5`**).
4. Observez le résultat s'afficher immédiatement dans la console inférieure !

### Option B : En Langage C
1. Cliquez sur **Fichier > Nouveau > Fichier C**.
2. Saisissez ce programme minimal :
```c
#include <stdio.h>

int main() {
    printf("Bienvenue dans CodeLab !\n");
    for (int i = 1; i <= 5; i++) {
        printf("Étape %d : tout fonctionne parfaitement.\n", i);
    }
    return 0;
}
```
3. Cliquez sur **▶️ Exécuter** (`F5`) : CodeLab compile et exécute automatiquement votre code.

### Option C : Par assemblage de blocs
1. Sélectionnez le mode **Blocs** dans le menu ou la barre latérale.
2. Glissez un bloc d'affichage depuis la boîte à outils.
3. Cliquez sur **▶️ Exécuter** : idéal pour s'initier aux algorithmes sans contrainte de syntaxe.

---

## 4. Tester un simulateur interactif en 1 clic

L'atout majeur de CodeLab réside dans ses simulateurs intégrés. Pour en tester un sans écrire une seule ligne de code :

1. Cliquez sur le menu **Fichier > Modèles** (ou *Templates*).
2. Choisissez un exemple clé en main, par exemple dans la section **Robotique mobile** ou **Moteur NXT**.
3. Le code correspondant s'ouvre dans l'éditeur et l'onglet du simulateur s'active à l'écran.
4. Cliquez sur **▶️ Exécuter** (`F5`) : regardez le robot mobile ou l'axe du moteur s'animer en temps réel selon les commandes de votre programme !

---

## 5. Raccourcis clavier essentiels

| Raccourci | Action |
| :--- | :--- |
| **`F5`** | **Exécuter** le programme actif |
| **`Maj + F5`** (ou `Shift + F5`) | **Arrêter** l'exécution en cours |
| **`Ctrl + N`** (ou `Cmd + N`) | Créer un **nouveau fichier** |
| **`Ctrl + S`** (ou `Cmd + S`) | **Enregistrer** le fichier actif |
| **`Ctrl + O`** (ou `Cmd + O`) | **Ouvrir** un fichier existant |

---

## Pour aller plus loin

- 📖 **Références des API** : consultez les guides dédiés pour le [Langage C](../lang-c/doc-c-fr.md), [Java](../lang-java/doc-java-fr.md), [Python](../lang-python/doc-py-fr.md) et [CLIPS](../lang-clips/doc-clips-fr.md).
- 🛠️ **Créer vos propres plugins** : suivez nos tutoriels développeur pour [créer un module applicatif](../tuto1/tuto1-fr.md), [concevoir un panneau de tracé 2D](../tuto2/tuto2-fr.md) ou [réaliser un simulateur temps réel](../tuto3/tuto3-fr.md).

<br/><center>
![](../img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2026 Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

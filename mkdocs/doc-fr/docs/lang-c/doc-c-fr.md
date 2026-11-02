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
		margin-top: 40px;
		margin-bottom: 40px;
	}
	img[src$="center+noborder"] {
		display: block;
		margin: 0 auto;
		border-style: none;
		background-color: none;
	}
	pre { 
		margin-top: 20px;
		margin-bottom: 20px;
		padding: 20px;
		margin: 10px;
	}
	pre code {
		background: transparent;
	}
	:not(pre) > code {
		background: yellow;
	}
	code[class=yellow] {
		background: yellow;
	}
</style>

![](../img/logo-codelab.png?style=noborder)

# CodeLab IDE & Simulators
# API CodeLab - Langage C

>Ce document n'est pas une référence du langage C. Il présente les fonctions présentes dans les bibliothèques C associées aux modules applicatifs intégrés à CodeLab. Pour les utiliser, il est nécessaire de connaître les bases de la programmation C.

## Notion de bibliothèque

Le langage C intègre de façon native les concepts de base de la programmation impérative (types primitifs, gestion des variables et de la mémoire, structures de programmation, entrées et sorties de bas niveau). Pour aller plus loin, il faut préciser en début de programme avec la directive `#include` les bibliothèques (ou librairies) qui contiennent les fonctions utilisées qui ne font pas partie du langage C de base. Ce n'est pas une instruction du langage C, mais une **directive** prise en compte au moment de la compilation.

Les fonctions décrites dans ce document sont rangées dans les bibliothèques suivantes :

- `#include "codelab/utils.c"` charge des fonctions utilitaires (entrées-sorties essentiellement)
- `#include "codelab/moduleGraphics/graph2D.c"` charge les fonctions du module graphique 2D
- `#include "codelab/moduleRobotics/robotLego.c"` charge les fonctions du module robotique mobile
- `#include "codelab/motormodule/NXTMotor.c"` charge les fonctions du module simulateur de moteur NXT
- `#include "codelab/tasks.c"` fournit les macros de gestion multi-tâches (threads POSIX)
- `#include "codelab/moduleRobotics3D/robotCoretech.c"` charge les fonctions du module robotique 3D (plugin Robot3D)

<br/>
## Notion de prototype

Pour documenter une fonction, on utilise son **prototype** qui précise :

- Le **nom** de la fonction ;
- Le **type** de la valeur de retour ou `void` si pas de valeur de retour ;
- Le **nombre** et le **type** des paramètres (les noms des paramètres sont donnés à titre indicatif).

<font color=red>_Il faut bien distinguer **prototype** d'une fonction et **appel** de cette même fonction :_</font>

Par exemple `void drawCircle(int x, int y, int r)` est le prototype de la fonction `drawCircle`. Pour appeler cette fonction dans un programme, il ne faut écrire ni le `void` en début de ligne, ni les `int` devant les paramètres, et il faut ajouter un point-virgule après la parenthèse fermante :

```c
#include "codelab/moduleGraphics/graph2D.c"

// Dessine un cercle de centre (100, 100) et de rayon 50

void main() {
	clearScreen();
	drawCircle(100, 100, 50);
}
```

<br/>![](../img/sep.png?style=center+noborder)

## Bibliothèque utilitaire

Cette bibliothèque contient essentiellement des fonctions d'entrée et de sorties (texte, audio, etc.) et d'accès aux périphériques. Le type `string` (chaîne de caractères) n'existe pas en tant que tel en langage C ; il a été implémenté dans cette bibliothèque par un `#define string char*` dans un souci de simplification d'écriture. <font color=red>Attention : la manipulation des chaînes de caractères en langage C repose sur des mécanismes de gestion de la mémoire (allocation dynamique). Consultez une documentation adaptée si besoin.</font> Les retours à la ligne doivent être indiqués à l'aide de la notation `\n` à l'intérieur des chaînes de caractères.

Par ailleurs, il est toujours possible d'utiliser les fonctions d'entrées-sorties de la librairie standard `stdio.h` en ajoutant la directive `#include <stdio.h>` en début de programme. Sachez également que `codelab/utils.c` inclue la librairie standard.

Fonctions d'affichage vers la console :

- [`void printStr(string chaine)`](#printStr)
- [`void printStrLn(string chaine)`](#printStr)
- [`void printInt(int entier)`](#printInt)
- [`void printIntLn(int entier)`](#printInt)
- [`void printLong(long entier)`](#printLong)
- [`void printLongLn(long entier)`](#printLong)
- [`void printFloat(float flottant)`](#printFloat)
- [`void printFloatLn(float flottant)`](#printFloat)
- [`void printFormat(string format, ...)`](#printFormat)
- [`void newline()`](#newline)

Fonctions d'entrée au clavier :

- [`string inputStr(string prompt)`](#inputStr)
- [`int inputInt(string prompt)`](#inputInt)
- [`float inputFloat(string prompt)`](#inputFloat)
- [`void waitKeyPressed()`](#waitKeyPressed)

Fonctions d'accès aux périphériques :

- [`int getNumpadValue()`](#getNumpadValue)
- [`int getJoystickValueX()`](#getJoystickValueX)
- [`int getJoystickValueY()`](#getJoystickValueX)
- [`int hasNextEvent()`](#hasNextEvent)
- [`string getNextEvent()`](#getNextEvent)
- [`void resetEventQueue()`](#resetEventQueue)

Fonctions de sortie audio :

- [`void playTone(int freq, int duree)`](#playTone)
- [`void playToneOn(int freq)`](#playToneOn)
- [`void playToneOff()`](#playToneOff)
- [`void playAudioFile(string fichier)`](#playAudioFile)
- [`void loopAudioFile(string fichier)`](#playAudioFile)
- [`void stopAudioPlayer()`](#stopAudioPlayer)

Fonctions additionnelles :

- [`int randomInt(int max)`](#randomInt)
- [`void initRandom()`](#randomInt)
- [`void waitFor(int duree)`](#waitFor)
- [`long systemTime()`](#systemTime)

<hr/><a name=printStr></a><br/>
`void printStr(string chaine)`<br/>
`void printStrLn(string chaine)`

**Description :** Affiche une chaine de caractères dans la console (suffixe `Ln` = retour à la ligne)

- Paramètre chaine : le texte à afficher (variable ou constante littérale)
- Valeur de retour : aucune

<hr/><a name=printInt></a><br/>
`void printInt(int entier)`<br/>
`void printIntLn(int entier)`

**Description :** Affiche un nombre entier dans la console (suffixe `Ln` = retour à la ligne)

- Paramètre entier : la valeur à afficher (variable ou constante littérale)
- Valeur de retour : aucune

<hr/><a name=printLong></a><br/>
`void printLong(long entier)`<br/>
`void printLongLn(long entier)`

**Description :** Affiche un nombre entier long dans la console (suffixe `Ln` = retour à la ligne)

- Paramètre entier : la valeur à afficher (variable ou constante littérale)
- Valeur de retour : aucune

<hr/><a name=printFloat></a><br/>
`void printFloat(float flottant)`<br/>
`void printFloatLn(float flottant)`

**Description :** Affiche un nombre à virgule flottante dans la console (suffixe `Ln` = retour à la ligne)

- Paramètre flottant : la valeur à afficher (variable ou constante littérale)
- Valeur de retour : aucune

<hr/><a name=printFormat></a><br/>
`void printFormat(string format, ...)`

**Description :** Permet d'afficher des valeurs de façon formatée

- Paramètre format : une chaîne de caractères comportant des "places"
- Valeur de retour : aucune

**Explication :** Cette fonction d'affichage peut prendre un nombre variable de paramètres. Le premier paramètre (obligatoire) est une chaîne de caractères comportant des "places" notées `%` où seront insérées les valeurs de différentes expressions, les paramètres suivants sont les expressions. <font color=red>Il doit y avoir autant de "places" que d'expressions.</font> Le principe est le suivant : chaque `%` est suivi d'une lettre qui précise le type de l'expression à afficher :

- `%d` pour un `int`
- `%l` pour un `long`
- `%f` pour un `float`
- `%s` pour un `string`

**Exemple d'utilisation :**

```c
#include "codelab/utils.c"

void main() {
	string nom = "Valentin";
	int age = 14;
	printFormat("Bonjour %s tu as %d ans\n", nom, age);
}
```

<hr/><a name=newline></a><br/>
`void newline()`

**Description :** Provoque un retour à la ligne dans la console

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=inputStr></a><br/>
`string inputStr(string prompt)`

**Description :** Affiche un prompt dans la console et saisit une chaîne de caractères

- Paramètre prompt : un texte à afficher dans la console en guise de prompt
- Valeur de retour : la chaîne saisie au clavier

<hr/><a name=inputInt></a><br/>
`int inputInt(string prompt)`

**Description :** Affiche un prompt dans la console et saisit un nombre entier

- Paramètre prompt : un texte à afficher dans la console en guise de prompt
- Valeur de retour : le nombre entier saisi au clavier

<hr/><a name=inputFloat></a><br/>
`float inputFloat(string prompt)`

**Description :** Affiche un prompt dans la console et saisit un nombre flottant

- Paramètre prompt : un texte à afficher dans la console en guise de prompt
- Valeur de retour : le nombre flottant saisi au clavier

<hr/><a name=waitKeyPressed></a><br/>
`void waitKeyPressed()`

**Description :** Attend une action au clavier (fonction bloquante)

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=getNumpadValue></a><br/>
`int getNumpadValue()`

**Description :** Retourne la valeur du périphérique virtuel "clavier numérique"

- Paramètre : aucun
- Valeur de retour : un entier dans l'intervalle [ -1, 11 ]

**Explication :** Le clavier numérique (numpad) est un périphérique non-bloquant, c'est-à-dire qu'il retourne toujours une valeur, même lorsqu'aucune touche n'est sélectionnée. Dans ce cas, il retourne la valeur -1. Les valeurs 10 et 11 correspondent respectivement aux touches [＊] et [ # ].

<hr/><a name=getJoystickValueX></a><br/>
`int getJoystickValueX()`<br/>
`int getJoystickValueY()`

**Description :** Retourne les valeur en X et en Y du périphérique virtuel "joystick"

- Paramètre : aucun
- Valeur de retour : un entier dans l'intervalle [ -100, 100 ]

<hr/><a name=hasNextEvent></a><br/>
`int hasNextEvent()`

**Description :** Informe s'il y a des évènements en attente d'être traités

- Paramètre : aucun
- Valeur de retour : 0 si la file d'attente est vide, 1 sinon

<hr/><a name=getNextEvent></a><br/>
`string getNextEvent()`

**Description :** Récupère un évènement dans la file d'attente

- Paramètre : aucun
- Valeur de retour : l'évènement codé dans une chaîne `"device=AAA:name=BBB:value=CCC"`

**Exemple d'utilisation :** Une méthode pour décoder l'évènement est d'utiliser une expression régulière ou la fonction `sscanf` :

```c
#include "codelab/utils.c"
#define REGEXP "device=%[^:]:name=%[^:]:value=%s"
	...
	while (hasNextEvent()) {
		string event = getNextEvent();
		if (sscanf(event, REGEXP, device, name, value) == 3) {
			...
		}
	}
```

<hr/><a name=resetEventQueue></a><br/>
`void resetEventQueue()`

**Description :** Réinitialise (vide) la file d'attente d'évènements

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=playTone></a><br/>
`void playTone(int frequence, int duree)`

**Description :** Génère un son d'une fréquence et d'une durée données (fonction bloquante)

- Paramètre frequence : la fréquence exprimée en Hertz
- Paramètre duree : la durée exprimée en millisecondes
- Valeur de retour : aucune

<hr/><a name=playToneOn></a><br/>
`void playToneOn(int frequence)`

**Description :** Génère un son d'une fréquence donnée (fonction non bloquante)

- Paramètre frequence : la fréquence exprimée en Hertz
- Valeur de retour : aucune

<hr/><a name=playToneOff></a><br/>
`void playToneOff()`

**Description :** Stoppe le générateur de fréquence initialisé avec `playToneOn`

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=playAudioFile></a><br/>
`void playAudioFile(string fichier)`<br/>
`void loopAudioFile(string fichier)`

**Description :** Joue un fichier audio WAV au format PCM 44100 Hz, 8 ou 16 bits

- Paramètre fichier : le nom du fichier, placé dans le répertoire `audiofiles`
- Valeur de retour : aucune

<hr/><a name=stopAudioPlayer></a><br/>
`void stopAudioPlayer()`

**Description :** Stoppe le player initialisé avec `playAudioFile` ou `loopAudioFile`

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=randomInt></a><br/>
`int randomInt(int max)`<br/>
`void initRandom()`

**Description :** Retourne un nombre entier "pseudo-aléatoire" (cf. note)

- Paramètre max : la borne supérieure (exclue) de l'intervalle
- Valeur de retour : le nombre entier aléatoire dans [ 0, max [

**Attention :** Pour obtenir des suites aléatoires différentes à chaque exécution du programme, placer l'instruction `initRandom()` au début du programme.

<hr/><a name=waitFor></a><br/>
`void waitFor(int duree)`

**Description :** Provoque une temporisation pendant une durée donnée (fonction bloquante)

- Paramètre duree : la durée exprimée en millisecondes
- Valeur de retour : aucune

<hr/><a name=systemTime></a><br/>
`long systemTime()`

**Description :** Retourne un "temps système" dans un entier long

- Paramètre : aucun
- Valeur de retour : le temps système exprimé en millisecondes

**Explication :** Tout ordinateur peut accéder à un "temps système" qui permet de calculer des intervalles de temps. Attention : la valeur retournée par la fonction systemTime peut être très grande et nécessite l'usage du type entier long.

<br/>![](../img/sep.png?style=center+noborder)

## Module Graphique 2D

Cette bibliothèque rassemble les fonctions de l'atelier graphique. Elle comporte deux familles de fonctions : les fonctions de tracé des **primitives géométriques** (point, ligne et cercle) ainsi que les fonctions de pilotage de la **tortue graphique** (avancer, tourner à gauche, tourner à droite, etc.). Pour plus d'informations sur la tortue graphique, consultez la page Wikipedia consacrée au [langage Logo](https://fr.wikipedia.org/wiki/Logo_(langage)) et à sa pédagogie.

Les fonctions de la bibliothèque du module graphique sont les suivantes :

- [`void clearScreen()`](#clearScreen)
- [`void setColor(int couleur)`](#setColor)
- [`void setWidth(int epaisseur)`](#setWidth)
- [`void drawPoint(int x, int y)`](#drawPoint)
- [`void drawLine(int x1, int y1, int x2, int y2)`](#drawLine)
- [`void drawCircle(int x, int y, int rayon)`](#drawCircle)
- [`void turtleShow()`](#turtleShow)
- [`void turtleHide()`](#turtleShow)
- [`void turtleReset()`](#turtleReset)
- [`void turtleGoto(int x, int y, int cap)`](#turtleGoto)
- [`void turtleForward(int distance)`](#turtleForward)
- [`void turtleTurnLeft(int angle)`](#turtleTurnLeft)
- [`void turtleTurnRight(int angle)`](#turtleTurnRight)
- [`void turtlePenUp()`](#turtlePenUp)
- [`void turtlePenDown()`](#turtlePenDown)
- [`int getColor()`](#getColor)
- [`int getBrightness()`](#getBrightness)

<hr/><a name=clearScreen></a><br/>
`void clearScreen()`

**Description :** Efface la zone de dessin

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=setColor></a><br/>
`void setColor(int couleur)`

**Description :** Fixe la couleur utilisée pour les tracés à venir

- Paramètre : un code de couleur autorisé (cf. ci-dessous)
- Valeur de retour : aucune

**Codes de couleur :**

- Pour spécifier la couleur blanche utilisez la constante `COLOR_WHITE`
- Pour spécifier la couleur noire utilisez la constante `COLOR_BLACK`
- Pour spécifier la couleur rouge utilisez la constante `COLOR_RED`
- Pour spécifier la couleur verte utilisez la constante `COLOR_GREEN`
- Pour spécifier la couleur bleue utilisez la constante `COLOR_BLUE`
- Pour spécifier la couleur cyan utilisez la constante `COLOR_CYAN`
- Pour spécifier la couleur jaune utilisez la constante `COLOR_YELLOW`
- Pour spécifier la couleur magenta utilisez la constante `COLOR_MAGENTA`

<hr/><a name=setWidth></a><br/>
`void setWidth(int epaisseur)`

**Description :** Fixe l'épaisseur du trait pour les tracés à venir

- Paramètre epaisseur : l'épaisseur du trait en pixels (>= 1)
- Valeur de retour : aucune

<hr/><a name=drawPoint></a><br/>
`void drawPoint(int x, int y)`

**Description :** Allume un pixel en position (x, y)

- Paramètre x : la coordonnée en abscisse du pixel
- Paramètre y : la coordonnée en ordonnée du pixel
- Valeur de retour : aucune

<hr/><a name=drawLine></a><br/>
`void drawLine(int x1, int y1, int x2, int y2)`

**Description :** Trace une ligne entre le point (x1, y1) et le point (x2, y2)

- Paramètre x1 : la coordonnée en abscisse du premier point
- Paramètre y1 : la coordonnée en ordonnée du premier point
- Paramètre x2 : la coordonnée en abscisse du deuxième point
- Paramètre y2 : la coordonnée en ordonnée du deuxième point
- Valeur de retour : aucune

<hr/><a name=drawCircle></a><br/>
`void drawCircle(int x, int y, int rayon)`

**Description :** Trace un cercle de centre (x, y) et de rayon rayon

- Paramètre x : la coordonnée en abscisse du centre du cercle
- Paramètre y : la coordonnée en ordonnée du centre du cercle
- Paramètre rayon : le rayon du cercle
- Valeur de retour : aucune

<hr/><a name= turtleShow></a><br/>
`void turtleShow()`<br/>
`void turtleHide()`

**Description :** Montre ou cache la tortue graphique

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=turtleReset></a><br/>
`void turtleReset()`

**Description :** Réinitialise la tortue graphique (centrée)

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=turtleGoto></a><br/>
`void turtleGoto(int x, int y, int cap)`

**Description :** Place la tortue en position (x, y) avec l'orientation indiquée sans tracer de ligne

- Paramètre x : la coordonnée en abscisse du point d'arrivée
- Paramètre y : la coordonnée en ordonnée du point d'arrivée
- Paramètre cap : l'orientation (cap) en degrés (0 = vers le haut, 90 = vers la droite, etc.)
- Valeur de retour : aucune

<hr/><a name=turtleForward></a><br/>
`void turtleForward(int distance)`

**Description :** Déplace la tortue en ligne droite sur une distance donnée

- Paramètre distance : la distance en "équivalent pixels"
- Valeur de retour : aucune

<hr/><a name=turtleTurnLeft></a><br/>
`void turtleTurnLeft(int angle)`

**Description :** Pivote la tortue dans le sens anti-horaire selon un angle donné

- Paramètre angle : l'angle exprimé en degrés
- Valeur de retour : aucune

<hr/><a name=turtleTurnRight></a><br/>
`void turtleTurnRight(int angle)`

**Description :** Pivote la tortue dans le sens horaire selon un angle donné

- Paramètre angle : l'angle exprimé en degrés
- Valeur de retour : aucune

<hr/><a name=turtlePenUp></a><a name=turtlePenDown></a><br/>
`void turtlePenUp()`<br/>
`void turtlePenDown()`

**Description :** Lève ou abaisse le stylet de la tortue graphique

- Paramètre : aucun
- Valeur de retour : aucune

**Explication :** Lorsque le stylet est levé (`turtlePenUp`), les déplacements suivants ne tracent aucun trait. Lorsqu'il est abaissé (`turtlePenDown`), chaque déplacement trace une ligne avec la couleur et l'épaisseur courantes.

<hr/><a name=getColor></a><br/>
`int getColor()`

**Description :** Retourne le code de couleur sous la position courante de la tortue

- Paramètre : aucun
- Valeur de retour : un code de couleur (`COLOR_WHITE`, `COLOR_BLACK`, etc.)

<hr/><a name=getBrightness></a><br/>
`int getBrightness()`

**Description :** Retourne la luminosité du pixel situé sous la tortue

- Paramètre : aucun
- Valeur de retour : un entier représentant l'intensité lumineuse

<br/>![](../img/sep.png?style=center+noborder)

## Module Robotique mobile

Cette bibliothèque rassemble les fonctions permettant de piloter le robot NXT de l'atelier robotique. Toutes ces fonctions possèdent un paramètre `port` de type `int` qui représente, soit un port de sortie (pour accéder aux moteurs), soit un port d'entrée (pour accéder aux capteurs). Il est également possible dans certains cas de spécifier deux ports de sortie simultanément.

Le robot NXT dispose de trois ports de sortie physiques (les moteurs gauche et droit sont conventionnellement connectés sur **<font color=red>port B</font>** et **<font color=red>port C</font>**). Les constantes suivantes permettent de désigner les ports moteur :

- `OUT_A`, `OUT_B`, `OUT_C` pour spécifier un moteur individuel
- `OUT_AB`, `OUT_AC`, `OUT_BC`, `OUT_ABC` pour combiner plusieurs moteurs simultanément

Le robot NXT dispose également de quatre ports d'entrée pour les capteurs (`IN_1`, `IN_2`, `IN_3`, `IN_4`). Le simulateur propose des configurations pré-établies (outil « clé » dans la barre d'outils) à choisir en fonction de l'exercice à réaliser. Par exemple, la configuration `COLOR/SONIC/SONIC/NONE` signifie qu'un capteur de couleur est relié au port n°1 et que deux capteurs de distance sont reliés respectivement aux ports n°2 et n°3, le port n°4 étant inutilisé.

Les constantes de couleur pour les capteurs sont identiques à celles du module graphique (`COLOR_WHITE` à `COLOR_MAGENTA`, et `COLOR_ERROR`).

Les fonctions de la bibliothèque du module robotique sont les suivantes :

- [`void motorOn(int port, int puissance)`](#motorOn)
- [`void motorOff(int port)`](#motorOff)
- [`void resetMotorRotationCount(int port)`](#resetMotorRotationCount)
- [`int motorRotationCount(int port)`](#motorRotationCount)
- [`int getSensorValue(int port)`](#getSensorValue)
- [`void getSensorArray(int port, int array[])`](#getSensorArray)
- [`void setBackground(string nom)`](#setBackground)
- [`void setRobotConfiguration(string config)`](#setRobotConfiguration)
- [`void simu_start()`](#simu_start)
- [`void simu_stop()`](#simu_start)

<hr/><a name=motorOn></a><br/>
`void motorOn(int port, int puissance)`

**Description :** Actionne un ou plusieurs moteurs

- Paramètre port : le(s) port(s) moteur (utilisez une constante symbolique)
- Paramètre puissance : la puissance du/des moteur(s) dans l'intervalle [ -100, 100 ]
- Valeur de retour : aucune

**Attention :** Une valeur en dehors de l'intervalle [ -100, 100 ] provoque un arrêt d'urgence

<hr/><a name=motorOff></a><br/>
`void motorOff(int port)`

**Description :** Stoppe un ou plusieurs moteurs

- Paramètre port : le(s) port(s) moteur (utilisez une constante symbolique)
- Valeur de retour : aucune

<hr/><a name=motorRotationCount></a><br/>
`int motorRotationCount(int port)`

**Description :** Récupère la valeur du compteur de rotation d'un moteur

- Paramètre port : le port du moteur (utilisez une constante symbolique)
- Valeur de retour : le nombre de degrés effectué depuis la dernière remise à 0

**Explication :** En interne, chaque moteur est associé à un dispositif optique qui compte le nombre de degrés que le moteur effectue. Lorsqu'on utilise cette fonction, le moteur est alors considéré comme un capteur, puisqu'il retourne une valeur.

**Attention :** Cette fonction n'est applicable qu'à un seul moteur à la fois

<hr/><a name=resetMotorRotationCount></a><br/>
`void resetMotorRotationCount(int port)`

**Description :** Stoppe un moteur et réinitialise son compteur de rotation (remise à 0)

- Paramètre port : le port du moteur (utilisez une constante symbolique)
- Valeur de retour : aucune

**Attention :** Cette fonction n'est applicable qu'à un seul moteur à la fois

<hr/><a name=getSensorValue></a><br/>
`int getSensorValue(int port)`

**Description :** Récupère la valeur d'un capteur (à l'exception du capteur de ligne)

- Paramètre port : le port d'entrée utilisé (utilisez une constante symbolique)
- Valeur de retour : la valeur lue (ou -1 en cas d'erreur de lecture) :
	- Si le capteur est le clavier numérique, on obtient une valeur dans [ -1, 11 ]
	- Si le capteur est la boussole, on obtient une valeur en degrés dans [ 0, 360 [
	- Si le capteur est un capteur de contact, on obtient une valeur 0 ou 1
	- Si le capteur est un capteur de distance, on obtient une distance en cm dans [ 0, 100 ]
	- Si le capteur est un capteur de couleur, on obtient une couleur codée comme suit :

		- La couleur blanche est codée par la constante `COLOR_WHITE`
		- La couleur noire est codée par la constante `COLOR_BLACK `
		- La couleur rouge est codée par la constante `COLOR_RED`
		- La couleur verte est codée par la constante `COLOR_GREEN`
		- La couleur bleue est codée par la constante `COLOR_BLUE`
		- La couleur cyan est codée par la constante `COLOR_CYAN`
		- La couleur jaune est codée par la constante `COLOR_YELLOW`
		- Le code `COLOR_ERROR` signifie que la couleur n'a pas pu être déterminée

<font color=red>**Attention :** La couleur cyan possède une propriété particulière : celle de se comporter comme un obstacle avec les capteurs de contact et de distance, ainsi qu'avec le robot lui-même (collisions). Elle est notamment utilisée sur certains fonds.</font>

**Note :** Le clavier numérique (numpad) est un périphérique non-bloquant, c'est-à-dire qu'il retourne toujours une valeur, même lorsqu'aucune touche n'est sélectionnée. Dans ce cas, il retourne la valeur `-1`. Les valeurs `10` et `11` correspondent respectivement aux touches [＊] et [ # ].

<hr/><a name= getSensorArray></a><br/>
`void getSensorArray(int port, int array[])`

**Description :** Récupère dans un tableau les 8 valeurs du capteur de ligne

- Paramètre port : le port d'entrée utilisé (utilisez une constante symbolique)
- Paramètre array : un tableau de 8 entiers (modifié par la fonction)
- Valeur de retour : aucune

**Exemple d'utilisation :**

```c
#include "codelab/moduleRobotics/robotLego.c"

void main() {
	int array[8];
	...
	getSensorArray(IN_1, array);
	...
}
```

<hr/><a name=setBackground></a><br/>
`void setBackground(string nom)`

**Description :** Définit l'image d'arrière-plan ou le circuit chargé dans le simulateur robotique

- Paramètre nom : le nom de l'image (ex. `"circuit1.png"`, `"arene.png"`)
- Valeur de retour : aucune

<hr/><a name=setRobotConfiguration></a><br/>
`void setRobotConfiguration(string config)`

**Description :** Configure la disposition des capteurs du robot mobile

- Paramètre config : la chaîne descriptive des 4 ports (ex. `"COLOR/SONIC/SONIC/NONE"`)
- Valeur de retour : aucune

<hr/><a name=simu_start></a><a name=simu_stop></a><br/>
`void simu_start()`<br/>
`void simu_stop()`

**Description :** Démarre ou interrompt l'animation temporelle du simulateur

- Paramètre : aucun
- Valeur de retour : aucune

<br/>![](../img/sep.png?style=center+noborder)

## Module Robotique 3D

Cette bibliothèque rassemble les fonctions permettant de piloter le robot CORETECH de l'atelier robotique 3D. Ce bras manipulateur comporte 5 moteurs pas-à-pas, les quels actionnent les 4 axes principaux du robot, et l'orientation de la pince. Les moteurs sont identifiés par les constantes prédéfinies entières suivantes :

![](../img/coretech.png?style=center+noborder)

Les fonctions pour accéder aux moteurs sont les suivantes :

- [`int motorOn(int motor, float speed)`](#motorOn3D)
- [`int motorStep(int motor, int steps)`](#motorStep)
- [`int motorOff(int motor)`](#motorOff3D)
- [`int grab()`](#grab)
- [`int drop()`](#grab)

Les fonctions pour accéder aux capteurs sont les suivantes :

- [`float getRotation(int motor)`](#getRotation)
- [`char* getSensorValue(int sensor)`](#getSensorValue3D)

Les fonctions de dessin dans l'espace sont les suivantes :

- [`int plot()`](#plot)
- [`int drawOn()`](#drawOn)
- [`int drawOff()`](#drawOff)
- [`int clearAll()`](#clearAll)
- [`int setColor(int r, int g, int b)`](#setColor3D)
- [`char* putBall(float x, float y, float z, int r, int g, int b, char* descr)`](#putBall)

Les fonctions d'accès aux positions sont les suivantes :

- [`char* getObjectPosition(char* uid)`](#getObjectPosition)
- [`char* getPencilPosition()`](#getPencilPosition)
- [`float getX()`](#getX)
- [`float getY()`](#getX)
- [`float getZ()`](#getX)

<hr/><a name=motorOn3D></a><br/>
`int motorOn(int motor, float speed)`

**Description :** Actionne un moteur avec une vitesse donnée

- Paramètre motor : l'identificateur du moteur (constante symbolique)
- Paramètre speed : la vitesse du moteur dans l'intervalle [-2.0, 2.0]
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

**Attention :** Une valeur en dehors de l'intervalle [-2.0, 2.0] provoque un arrêt d'urgence.

<hr/><a name=motorStep></a><br/>
`int motorStep(int motor, int steps)`

**Description :** Actionne un moteur d'un nombre de pas donné

- Paramètre motor : l'identificateur du moteur (constante symbolique)
- Paramètre steps : le nombre de pas dans l'intervalle [-10, 10]
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

**Attention :** Une valeur en dehors de l'intervalle [-10, 10] provoque un arrêt d'urgence.

<hr/><a name=motorOff3D></a><br/>
`int motorOff(int motor)`

**Description :** Stoppe un moteur

- Paramètre motor : l'identificateur du moteur (constante symbolique)
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=grab></a><br/>
`int grab()`<br/>
`int drop()`

**Description :** Actionne la pince pour saisir ou relâcher un objet

- Paramètre : aucun
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=getRotation></a><br/>
`float getRotation(int motor)`

**Description :** Retourne la position de l'élément actionné par un moteur

- Paramètre motor : l'identificateur du moteur (constante symbolique)
- Valeur de retour : un angle en degrés

**Attention :** Les plages angulaires des éléments sont les suivantes :

- Axe 0 : base (shoulder) : [0, 360] en continu
- Axe 1 : bras supérieur (upper arm) : [-110, 110]
- Axe 2 : bras inférieur (lower arm) : [-130, 130]
- Axe 3 : main (hand) : [-120, 120]
- Axe 4 : pince (claw) : [0, 360] en continu

<hr/><a name=getSensorValue3D></a><br/>
`char* getSensorValue(int sensor)`

**Description :** Retourne la valeur d'un capteur

- Paramètre sensor : l'identificateur du capteur (constante symbolique)
	- Capteur de contact au bout de la pince : `CONTACT_SENSOR`
	- Capteur de couleur au bout de la pince : `COLOR_SENSOR`
- Valeur de retour : la donnée <font color=red>codée dans une chaîne de caractères</font> ou "ERROR"

**Attention :** le codage de la valeur de retour dépend du type du capteur :

- Dans le cas du capteur de contact : `"1"` si contact ou `""` (chaîne vide)
- Dans le cas du capteur de couleur : `"R:G:B"` (code RGB) ou `"NONE"`

<hr/><a name=plot></a><br/>
`int plot()`

**Description :** Place un point aux coordonnées données par l'extrémité de la pince, et de couleur donnée par [`setColor`](#setColor3D)

- Paramètre : aucun
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=drawOn></a><br/>
`int drawOn()`

**Description :** Initialise un tracé de couleur donnée par [`setColor`](#setColor3D)

- Paramètre : aucun
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=drawOff></a><br/>
`int drawOff()`

**Description :** Met fin au tracé initialisé avec [`drawOn`](#drawOn)

- Paramètre : aucun
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=clearAll></a><br/>
`int clearAll()`

**Description :** Efface les tracés et supprime les objets de la scène

- Paramètre : aucun
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=setColor3D></a><br/>
`int setColor(int r, int g, int b)`

**Description :** Définit la couleur des tracés à venir réalisés avec [`plot`](#plot) et [`drawOn`](#drawOn)

- Paramètres r, g, b : les composantes RGB de la couleur dans [0, 255]
- Valeur de retour : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=putBall></a><br/>
`char* putBall(float x, float y, float z, int r, int g, int b, const char* descr)`

**Description :** Crée et positionne une balle colorée dans la scène du robot

- Paramètres x, y, z : les coordonnées où placer la balle dans la scène
- Paramètres r, g, b : les composantes RGB de la couleur dans [0, 255]
- Paramètre descr : une description littérale de la balle (ex : "la balle rouge")
- Valeur de retour : l'identifiant unique (uid) de la balle

<hr/><a name= getObjectPosition></a><br/>
`char* getObjectPosition(const char* uid)`

**Description :** Retourne la position d'un objet à partir de son identifiant (uid)

- Paramètre uid : un identifiant (donné par la fonction `putBall` par exemple)
- Valeur de retour : des coordonnées codées dans une chaine du type `"x:y:z"` ou `"ERROR"`

**Exemple d'utilisation :**

```c
#include "codelab/moduleRobotics3D/robotCoretech.c"
#include "codelab/utils.c"

void main() {
	char* uid;
	char* position;
	float x, y, z;
	
	uid = strdup(putBall(-0.2, 0.05, 0.8, 255, 0, 0, "the red ball"));
	printStrLn(uid);
	
	position = strdup(getObjectPosition(uid));
	printStrLn(position);
	
	sscanf(position, "%f:%f:%f", &x, &y, &z);
	printFormat("x = %f\n", x);
	printFormat("y = %f\n", y);
	printFormat("z = %f\n", z);
}
```

<font color=red>**Attention :** La manipulation des chaînes de caractères en langage C repose sur des mécanismes de gestion de la mémoire (allocation dynamique). L'allocation pour les chaînes `uid` et `position` a été réalisée ici par la fonction `strdup` qui utilise `malloc`. Consultez une documentation adaptée si besoin.</font>

Résultat de l'exécution du programme dans la console :

```
the red ball-4dc99a31
-0.200000:0.050000:0.800000
x = -0.200000
y = 0.050000
z = 0.800000
```

<hr/><a name= getPencilPosition></a><br/>
`char* getPencilPosition()`

**Description :** Retourne la position du crayon (extrémité de la pince)

- Paramètre : aucun
- Valeur de retour : des coordonnées codées dans une chaine du type `"x:y:z"`

<hr/><a name=getX></a><br/>
`float getX()`<br/>
`float getY()`<br/>
`float getZ()`

**Description :** Retourne une coordonnée du crayon (extrémité de la pince)

- Paramètre : aucun
- Valeur de retour : la coordonnée x / y / z du crayon

<br/><center>
![](../img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2026 Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>
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
	code {
		background: yellow;
	}
	code[class=yellow] {
		background: yellow;
	}
</style>

![](../img/logo-codelab.png?style=noborder)

# CodeLab IDE & Simulators
# Référence Langage Python
<br/>

## Notion de bibliothèque

Le langage Python intègre de façon native les concepts de base de la programmation impérative (types natifs, structures de programmation et de données, entrées et sorties). Pour aller plus loin, il faut préciser en début de programme avec la notation `from ... import *` les bibliothèques (ou modules Python) qui contiennent d'autres instructions qui ne font pas partie du langage Python de base. Par exemple, la notation `from A.B import *` importe en totalité la bibliothèque B du paquet A. Un paquet est juste un groupe de bibliothèques.

Les fonctions décrites dans ce document sont rangées dans 4 bibliothèques :

- `from codelab.utils import *` charge des fonctions utilitaires (entrées-sorties, etc.)
- `from codelab.moduleGraphics.graph2D import *` charge les fonctions du module graphique 2D
- `from codelab.moduleRobotics.robotLego import *` charge les fonctions du module robotique mobile (robot Lego NXT)
- `from codelab.moduleRobotics.robotRovio import *` charge les fonctions du module robotique mobile (robot Rovio)
- `from codelab.motormodule.NXTMotor import *` charge les fonctions du module simulateur de moteur NXT
- `from codelab.moduleRobotics3D.robotCoretech import *` charge les fonctions du module robotique 3D

<br/>![](../img/sep.png?style=center+noborder)

## Bibliothèque utilitaire

Cette bibliothèque contient quelques fonctions d'entrée et de sorties dédiées à l'environnement CodeLab (sortie audio, accès aux périphériques), plus quelques autres fonctions utilitaires.

Fonctions d'accès aux périphériques :

- [`getNumpadValue()`](#getNumpadValue)
- [`getJoystickValueX()`](#getJoystickValueX)
- [`getJoystickValueY()`](#getJoystickValueX)
- [`hasNextEvent()`](#hasNextEvent)
- [`getNextEvent()`](#getNextEvent)
- [`resetEventQueue()`](#resetEventQueue)

Fonctions de sortie audio :

- [`playTone(freq, duree)`](#playTone)
- [`playChord2(f1, f2, duree)`](#playChord2)
- [`playToneOn(freq)`](#playToneOn)
- [`playToneOff()`](#playToneOff)
- [`playAudioFile(fichier)`](#playAudioFile)
- [`loopAudioFile(fichier)`](#playAudioFile)
- [`stopAudioPlayer()`](#stopAudioPlayer)

Fonctions additionnelles :

- [`waitFor(duree)`](#waitFor)
- [`systemTime()`](#systemTime)

<hr/><a name=getNumpadValue></a><br/>
`getNumpadValue()`

**Description :** Retourne la valeur du périphérique virtuel "clavier numérique"

- Paramètre : aucun
- Valeur de retour : un entier dans l'intervalle [ -1, 11 ]

**Explication :** Le clavier numérique (numpad) est un périphérique non-bloquant, c'est-à-dire qu'il retourne toujours une valeur, même lorsqu'aucune touche n'est sélectionnée. Dans ce cas, il retourne la valeur -1. Les valeurs 10 et 11 correspondent respectivement aux touches [＊] et [ # ].

<hr/><a name=getJoystickValueX></a><br/>
`getJoystickValueX()`<br/>
`getJoystickValueY()`

**Description :** Retourne les valeur en X et en Y du périphérique virtuel "joystick"

- Paramètre : aucun
- Valeur de retour : un entier dans l'intervalle [ -100, 100 ]

<hr/><a name=hasNextEvent></a><br/>
`hasNextEvent()`

**Description :** Informe s'il y a des évènements en attente d'être traités

- Paramètre : aucun
- Valeur de retour : `True` si la file d'attente est vide, `False` sinon

<hr/><a name=getNextEvent></a><br/>
`getNextEvent()`

**Description :** Récupère un évènement dans la file d'attente

- Paramètre : aucun
- Valeur de retour : l'évènement codé dans une chaîne `"device=AAA:name=BBB:value=CCC"`

**Exemple d'utilisation :** Une méthode pour décoder l'évènement est d'utiliser une expression régulière :

```python
from codelab.utils import *
import re

REGEXP = r'device=([^:]*):name=([^:]*):value=(.*)'
...
while hasNextEvent():
	event = getNextEvent()
	if match := re.match(REGEXP, event):
		print(match.group(1), match.group(2), match.group(3))
		...
```

<hr/><a name=resetEventQueue></a><br/>
`resetEventQueue()`

**Description :** Réinitialise (vide) la file d'attente d'évènements

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=playTone></a><br/>
`playTone(frequence, duree)`

**Description :** Génère un son d'une fréquence et d'une durée données (fonction bloquante)

- Paramètre frequence (entier) : la fréquence exprimée en Hertz
- Paramètre duree (entier) : la durée exprimée en millisecondes
- Valeur de retour : aucune

<hr/><a name=playChord2></a><br/>
`playChord2(f1, f2, duree)`

**Description :** Génère un accord de deux fréquences simultanées pendant une durée donnée

- Paramètre f1 (entier) : première fréquence en Hertz
- Paramètre f2 (entier) : seconde fréquence en Hertz
- Paramètre duree (entier) : la durée exprimée en millisecondes
- Valeur de retour : aucune

<hr/><a name=playToneOn></a><br/>
`playToneOn(frequence)`

**Description :** Génère un son d'une fréquence donnée (fonction non bloquante)

- Paramètre frequence (entier) : la fréquence exprimée en Hertz
- Valeur de retour : aucune

<hr/><a name=playToneOff></a><br/>
`playToneOff()`

**Description :** Stoppe le générateur de fréquence initialisé avec `playToneOn`

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=playAudioFile></a><br/>
`playAudioFile(fichier)`<br/>
`loopAudioFile(fichier)`

**Description :** Joue un fichier audio WAV au format PCM 44100 Hz, 8 ou 16 bits

- Paramètre fichier (chaîne) : le nom du fichier, placé dans le répertoire `audiofiles`
- Valeur de retour : aucune

<hr/><a name=stopAudioPlayer></a><br/>
`stopAudioPlayer()`

**Description :** Stoppe le player initialisé avec `playAudioFile` ou `loopAudioFile`

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=waitFor></a><br/>
`waitFor(duree)`

**Description :** Provoque une temporisation pendant une durée donnée (fonction bloquante)

- Paramètre duree (entier) : la durée exprimée en millisecondes
- Valeur de retour : aucune

<hr/><a name=systemTime></a><br/>
`systemTime()`

**Description :** Retourne un "temps système" dans un entier

- Paramètre : aucun
- Valeur de retour : le temps système exprimé en millisecondes

**Explication :** Tout ordinateur peut accéder à un "temps système" qui permet de calculer des intervalles de temps.

<br/>![](../img/sep.png?style=center+noborder)

## Module Graphique 2D

Cette bibliothèque rassemble les fonctions de l'atelier graphique. Elle comporte deux familles de fonctions : les fonctions de tracé des **primitives géométriques** (point, ligne et cercle) ainsi que les fonctions de pilotage de la **tortue graphique** (avancer, tourner à gauche, tourner à droite, etc.). Pour plus d'informations sur la tortue graphique, consultez la page Wikipedia consacrée au [langage Logo](https://fr.wikipedia.org/wiki/Logo_(langage)) et à sa pédagogie.

Les fonctions de la bibliothèque du module graphique sont les suivantes :

- [`clearScreen()`](#clearScreen)
- [`setColor(couleur)`](#setColor)
- [`setWidth(epaisseur)`](#setWidth)
- [`drawPoint(x, y)`](#drawPoint)
- [`drawLine(x1, y1, x2, y2)`](#drawLine)
- [`drawCircle(x, y, rayon)`](#drawCircle)
- [`turtleShow()`](#turtleShow)
- [`turtleHide()`](#turtleShow)
- [`turtleReset()`](#turtleReset)
- [`turtleGoto(x, y, cap)`](#turtleGoto)
- [`turtleForward(distance)`](#turtleForward)
- [`turtleTurnLeft(angle)`](#turtleTurnLeft)
- [`turtleTurnRight(angle)`](#turtleTurnRight)
- [`turtlePenUp()`](#turtlePenUp)
- [`turtlePenDown()`](#turtlePenDown)
- [`getColor()`](#getColor)
- [`getBrightness()`](#getBrightness)

<hr/><a name=clearScreen></a><br/>
`clearScreen()`

**Description :** Efface la zone de dessin

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=setColor></a><br/>
`setColor(couleur)`

**Description :** Fixe la couleur utilisée pour les tracés à venir

- Paramètre couleur (entier) : un code de couleur autorisé (cf. ci-dessous)
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
`setWidth(epaisseur)`

**Description :** Fixe l'épaisseur du trait pour les tracés à venir

- Paramètre epaisseur (entier) : l'épaisseur du trait en pixels (>= 1)
- Valeur de retour : aucune

<hr/><a name=drawPoint></a><br/>
`drawPoint(x, y)`

**Description :** Allume un pixel en position (x, y)

- Paramètre x (entier) : la coordonnée en abscisse du pixel
- Paramètre y (entier) : la coordonnée en ordonnée du pixel
- Valeur de retour : aucune

<hr/><a name=drawLine></a><br/>
`drawLine(x1, y1, x2, y2)`

**Description :** Trace une ligne entre le point (x1, y1) et le point (x2, y2)

- Paramètre x1 (entier) : la coordonnée en abscisse du premier point
- Paramètre y1 (entier) : la coordonnée en ordonnée du premier point
- Paramètre x2 (entier) : la coordonnée en abscisse du deuxième point
- Paramètre y2 (entier) : la coordonnée en ordonnée du deuxième point
- Valeur de retour : aucune

<hr/><a name=drawCircle></a><br/>
`drawCircle(x, y, rayon)`

**Description :** Trace un cercle de centre (x, y) et de rayon rayon

- Paramètre x (entier) : la coordonnée en abscisse du centre du cercle
- Paramètre y (entier) : la coordonnée en ordonnée du centre du cercle
- Paramètre rayon (entier) : le rayon du cercle
- Valeur de retour : aucune

<hr/><a name= turtleShow></a><br/>
`turtleShow()`<br/>
`turtleHide()`

**Description :** Montre ou cache la tortue graphique

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=turtleReset></a><br/>
`turtleReset()`

**Description :** Réinitialise la tortue graphique (centrée)

- Paramètre : aucun
- Valeur de retour : aucune

<hr/><a name=turtleGoto></a><br/>
`turtleGoto(x, y, cap)`

**Description :** Place la tortue en position (x, y) avec l'orientation indiquée sans tracer de ligne

- Paramètre x (entier) : la coordonnée en abscisse du point d'arrivée
- Paramètre y (entier) : la coordonnée en ordonnée du point d'arrivée
- Paramètre cap (entier) : l'orientation (cap) en degrés (0 = vers le haut, 90 = vers la droite, etc.)
- Valeur de retour : aucune

<hr/><a name=turtleForward></a><br/>
`turtleForward(distance)`

**Description :** Déplace la tortue en ligne droite sur une distance donnée

- Paramètre distance (entier) : la distance en "équivalent pixels"
- Valeur de retour : aucune

<hr/><a name=turtleTurnLeft></a><br/>
`turtleTurnLeft(angle)`

**Description :** Pivote la tortue dans le sens anti-horaire selon un angle donné

- Paramètre angle (entier) : l'angle exprimé en degrés
- Valeur de retour : aucune

<hr/><a name=turtleTurnRight></a><br/>
`turtleTurnRight(angle)`

**Description :** Pivote la tortue dans le sens horaire selon un angle donné

- Paramètre angle (entier) : l'angle exprimé en degrés
- Valeur de retour : aucune

<hr/><a name=turtlePenUp></a><a name=turtlePenDown></a><br/>
`turtlePenUp()`<br/>
`turtlePenDown()`

**Description :** Lève ou abaisse le crayon (stylet) de la tortue graphique

- Paramètre : aucun
- Valeur de retour : aucune

**Explication :** Lorsque le crayon est levé (`turtlePenUp()`), les déplacements suivants ne tracent aucun trait. Lorsqu'il est abaissé (`turtlePenDown()`), chaque déplacement trace une ligne de la couleur et de l'épaisseur courantes.

<hr/><a name=getColor></a><br/>
`getColor()`

**Description :** Retourne le code de couleur sous la position courante de la tortue

- Paramètre : aucun
- Valeur de retour : un entier représentant le code de couleur (`COLOR_WHITE`, `COLOR_BLACK`, etc.)

<hr/><a name=getBrightness></a><br/>
`getBrightness()`

**Description :** Retourne la luminosité du pixel situé sous la position courante de la tortue

- Paramètre : aucun
- Valeur de retour : un entier représentant l'intensité lumineuse

<br/>![](../img/sep.png?style=center+noborder)

## Module Robotique mobile

Cette bibliothèque rassemble les fonctions permettant de piloter le robot NXT de l'atelier robotique. Toutes ces fonctions possèdent un paramètre `port` de type entier qui représente, soit un port de sortie (pour accéder aux moteurs), soit un port d'entrée (pour accéder aux capteurs). Il est également possible dans certains cas de spécifier deux ports de sortie simultanément.

Le robot NXT dispose de trois ports de sortie physiques (les moteurs gauche et droit sont conventionnellement connectés sur **<font color=red>port B</font>** et **<font color=red>port C</font>**). Les constantes suivantes permettent de désigner les ports moteur :

- `OUT_A`, `OUT_B`, `OUT_C` pour spécifier un moteur individuel
- `OUT_AB`, `OUT_AC`, `OUT_BC`, `OUT_ABC` pour combiner plusieurs moteurs simultanément

Le robot NXT dispose également de quatre ports d'entrée pour les capteurs (`IN_1`, `IN_2`, `IN_3`, `IN_4`). Le simulateur propose des configurations pré-établies (outil « clé » dans la barre d'outils) à choisir en fonction de l'exercice à réaliser. Par exemple, la configuration `COLOR/SONIC/SONIC/NONE` signifie qu'un capteur de couleur est relié au port n°1 et que deux capteurs de distance sont reliés respectivement aux ports n°2 et n°3, le port n°4 étant inutilisé.

Les constantes de couleur pour les capteurs sont identiques à celles du module graphique (`COLOR_WHITE` à `COLOR_MAGENTA`, et `COLOR_ERROR`).

Les fonctions de la bibliothèque du module robotique (Lego NXT) sont les suivantes :

- [`motorOn(port, puissance)`](#motorOn)
- [`motorOff(port)`](#motorOff)
- [`resetMotorRotationCount(port)`](#resetMotorRotationCount)
- [`motorRotationCount(port)`](#motorRotationCount)
- [`getSensorValue(port)`](#getSensorValue)
- [`getSensorArray(port)`](#getSensorArray)
- [`setBackground(nom)`](#setBackground)
- [`setRobotConfiguration(config)`](#setRobotConfiguration)

<hr/><a name=motorOn></a><br/>
`motorOn(port, puissance)`

**Description :** Actionne un ou plusieurs moteurs

- Paramètre port (entier) : le(s) port(s) moteur (utilisez une constante symbolique)
- Paramètre puissance (entier) : la puissance du/des moteur(s) dans l'intervalle [ -100, 100 ]
- Valeur de retour : aucune

**Attention :** Une valeur en dehors de l'intervalle [ -100, 100 ] provoque un arrêt d'urgence

<hr/><a name=motorOff></a><br/>
`motorOff(port)`

**Description :** Stoppe un ou plusieurs moteurs

- Paramètre port (entier) : le(s) port(s) moteur (utilisez une constante symbolique)
- Valeur de retour : aucune

<hr/><a name=motorRotationCount></a><br/>
`motorRotationCount(port)`

**Description :** Récupère la valeur du compteur de rotation d'un moteur

- Paramètre port (entier) : le port du moteur (utilisez une constante symbolique)
- Valeur de retour : le nombre de degrés effectué depuis la dernière remise à 0

**Explication :** En interne, chaque moteur est associé à un dispositif optique qui compte le nombre de degrés que le moteur effectue. Lorsqu'on utilise cette fonction, le moteur est alors considéré comme un capteur, puisqu'il retourne une valeur.

**Attention :** Cette fonction n'est applicable qu'à un seul moteur à la fois

<hr/><a name=resetMotorRotationCount></a><br/>
`resetMotorRotationCount(port)`

**Description :** Stoppe un moteur et réinitialise son compteur de rotation (remise à 0)

- Paramètre port (entier) : le port du moteur (utilisez une constante symbolique)
- Valeur de retour : aucune

**Attention :** Cette fonction n'est applicable qu'à un seul moteur à la fois

<hr/><a name=getSensorValue></a><br/>
`getSensorValue(port)`

**Description :** Récupère la valeur d'un capteur (à l'exception du capteur de ligne)

- Paramètre port (entier) : le port d'entrée utilisé (utilisez une constante symbolique)
- Valeur de retour (entier) : la valeur lue (ou -1 en cas d'erreur de lecture) :
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
`getSensorArray(port)`

**Description :** Récupère dans une liste les 8 valeurs du capteur de ligne

- Paramètre port (entier) : le port d'entrée utilisé (utilisez une constante symbolique)
- Valeur de retour (liste d'entiers) : les 8 valeurs dans [0, 100] issues du capteur de ligne

**Exemple d'utilisation :**

```python
from codelab.moduleRobotics.robotLego import *
from codelab.utils import *

setRobotConfiguration('NXT06')

while True:
	print(getSensorArray(IN_1))
	waitFor(100)
```

<hr/><a name=setBackground></a><br/>
`setBackground(nom)`

**Description :** Définit l'image d'arrière-plan ou le circuit chargé dans le simulateur robotique

- Paramètre nom (chaîne) : le nom de l'image (ex. `"circuit1.png"`, `"arene.png"`)
- Valeur de retour : aucune

<hr/><a name=setRobotConfiguration></a><br/>
`setRobotConfiguration(config)`

**Description :** Configure la disposition des capteurs du robot mobile

- Paramètre config (chaîne) : la chaîne descriptive des 4 ports (ex. `"COLOR/SONIC/SONIC/NONE"`)
- Valeur de retour : aucune

<br/>![](../img/sep.png?style=center+noborder)

### Module Robotique mobile - Robot Rovio

CodeLab intègre également le support du robot Rovio via la bibliothèque dédiée `codelab.moduleRobotics.robotRovio` :

```python
from codelab.moduleRobotics.robotRovio import *
```

Constantes des moteurs du robot Rovio :
- `MOTOR_L = 0` : moteur gauche
- `MOTOR_R = 1` : moteur droit
- `MOTOR_C = 2` : moteur central
- `MOTOR_LR = 3` : moteurs gauche et droit
- `MOTOR_ALL = 4` : l'ensemble des moteurs

Fonctions disponibles :
- `motorOn(port, puissance)` : active le ou les moteurs Rovio
- `motorOff(port)` : arrête le ou les moteurs Rovio
- `setBackground(nom)` : configure l'arrière-plan du simulateur
- `setRobotConfiguration(config)` : configure le simulateur Rovio

<br/>![](../img/sep.png?style=center+noborder)

## Module Robotique 3D

Cette bibliothèque rassemble les fonctions permettant de piloter le robot CORETECH de l'atelier robotique 3D. Ce bras manipulateur comporte 5 moteurs pas-à-pas, les quels actionnent les 4 axes principaux du robot, et l'orientation de la pince. Les moteurs sont identifiés par les constantes prédéfinies entières suivantes :

![](../img/coretech.png?style=center+noborder)

Les fonctions pour accéder aux moteurs sont les suivantes :

- [`motorOn(motor, speed)`](#motorOn3D)
- [`motorStep(motor, steps)`](#motorStep)
- [`motorOff(motor)`](#motorOff3D)
- [`grab()`](#grab)
- [`drop()`](#grab)

Les fonctions pour accéder aux capteurs sont les suivantes :

- [`getRotation(motor)`](#getRotation)
- [`getSensorValue(sensor)`](#getSensorValue3D)

Les fonctions de dessin dans l'espace sont les suivantes :

- [`plot()`](#plot)
- [`drawOn()`](#drawOn)
- [`drawOff()`](#drawOff)
- [`clearAll()`](#clearAll)
- [`setColor(r, g, b)`](#setColor3D)
- [`putBall(x, y, z, r, g, b, descr)`](#putBall)

Les fonctions d'accès aux positions sont les suivantes :

- [`getObjectPosition(uid)`](#getObjectPosition)
- [`getPencilPosition()`](#getPencilPosition)
- [`getX()`](#getX)
- [`getY()`](#getX)
- [`getZ()`](#getX)

<hr/><a name=motorOn3D></a><br/>
`motorOn(motor, speed)`

**Description :** Actionne un moteur avec une vitesse donnée

- Paramètre motor (entier) : l'identificateur du moteur (constante symbolique)
- Paramètre speed (flottant) : la vitesse du moteur dans l'intervalle [-2.0, 2.0]
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

**Attention :** Une valeur en dehors de l'intervalle [-2.0, 2.0] provoque un arrêt d'urgence.

<hr/><a name=motorStep></a><br/>
`motorStep(motor, steps)`

**Description :** Actionne un moteur d'un nombre de pas donné

- Paramètre motor (entier) : l'identificateur du moteur (constante symbolique)
- Paramètre steps (entier) : le nombre de pas dans l'intervalle [-10, 10]
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

**Attention :** Une valeur en dehors de l'intervalle [-10, 10] provoque un arrêt d'urgence.

<hr/><a name=motorOff3D></a><br/>
`motorOff(motor)`

**Description :** Stoppe un moteur

- Paramètre motor (entier) : l'identificateur du moteur (constante symbolique)
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=grab></a><br/>
`grab()`<br/>
`drop()`

**Description :** Actionne la pince pour saisir ou relâcher un objet

- Paramètre : aucun
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=getRotation></a><br/>
`getRotation(motor)`

**Description :** Retourne la position de l'élément actionné par un moteur

- Paramètre motor (entier) : l'identificateur du moteur (constante symbolique)
- Valeur de retour (flottant) : un angle en degrés

**Attention :** Les plages angulaires des éléments sont les suivantes :

- Axe 0 : base (shoulder) : [0, 360] en continu
- Axe 1 : bras supérieur (upper arm) : [-110, 110]
- Axe 2 : bras inférieur (lower arm) : [-130, 130]
- Axe 3 : main (hand) : [-120, 120]
- Axe 4 : pince (claw) : [0, 360] en continu

<hr/><a name=getSensorValue3D></a><br/>
`getSensorValue(sensor)`

**Description :** Retourne la valeur d'un capteur

- Paramètre sensor (entier) : l'identificateur du capteur (constante symbolique)
	- Capteur de contact au bout de la pince : `CONTACT_SENSOR`
	- Capteur de couleur au bout de la pince : `COLOR_SENSOR`
- Valeur de retour (chaîne) : la donnée <font color=red>codée dans une chaîne de caractères</font> ou "ERROR"

**Attention :** le codage de la valeur de retour dépend du type du capteur :

- Dans le cas du capteur de contact : `"1"` si contact ou `""` (chaîne vide)
- Dans le cas du capteur de couleur : `"R:G:B"` (code RGB) ou `"NONE"`

<hr/><a name=plot></a><br/>
`plot()`

**Description :** Place un point aux coordonnées données par l'extrémité de la pince, et de couleur donnée par [`setColor`](#setColor3D)

- Paramètre : aucun
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=drawOn></a><br/>
`drawOn()`

**Description :** Initialise un tracé de couleur donnée par [`setColor`](#setColor3D)

- Paramètre : aucun
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=drawOff></a><br/>
`drawOff()`

**Description :** Met fin au tracé initialisé avec [`drawOn`](#drawOn)

- Paramètre : aucun
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=clearAll></a><br/>
`clearAll()`

**Description :** Efface les tracés et supprime les objets de la scène

- Paramètre : aucun
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=setColor3D></a><br/>
`setColor(r, g, b)`

**Description :** Définit la couleur des tracés à venir réalisés avec [`plot`](#plot) et [`drawOn`](#drawOn)

- Paramètres r, g, b (entiers) : les composantes RGB de la couleur dans [0, 255]
- Valeur de retour (entier) : 1 si la commande a été exécutée, -1 sinon

<hr/><a name=putBall></a><br/>
`putBall(x, y, z, r, g, b, descr)`

**Description :** Crée et positionne une balle colorée dans la scène du robot

- Paramètres x, y, z (flottants) : les coordonnées où placer la balle dans la scène
- Paramètres r, g, b (entiers) : les composantes RGB de la couleur dans [0, 255]
- Paramètre descr (chaîne) : une description littérale de la balle (ex : "la balle rouge")
- Valeur de retour (chaîne) : l'identifiant unique (uid) de la balle

<hr/><a name= getObjectPosition></a><br/>
`getObjectPosition(uid)`

**Description :** Retourne la position d'un objet à partir de son identifiant (uid)

- Paramètre uid (chaîne) : un identifiant (donné par la fonction `putBall` par exemple)
- Valeur de retour (chaîne) : des coordonnées codées dans une chaine du type `"x:y:z"` ou `"ERROR"`

**Exemple d'utilisation :**

```python
from codelab.moduleRobotics3D.robotCoretech import *

def getPosition(uid):
	position = getObjectPosition(uid)
	array = list(map(float, position.split(':')))
	return array[0], array[1], array[2]

uid = putBall(-0.2, 0.05, 0.8, 255, 0, 0, 'the red ball')
x, y, z = getPosition(uid)

print('The position of %s is (%f, %f, %f)' % (uid, x, y, z))
```

Résultat de l'exécution du programme dans la console :

```
The position of the red ball-221a8efc is (-0.200000, 0.050000, 0.800000)
```

<hr/><a name= getPencilPosition></a><br/>
`getPencilPosition()`

**Description :** Retourne la position du crayon (extrémité de la pince)

- Paramètre : aucun
- Valeur de retour (chaîne) : des coordonnées codées dans une chaine du type `"x:y:z"`

<hr/><a name=getX></a><br/>
`getX()`<br/>
`getY()`<br/>
`getZ()`

**Description :** Retourne une coordonnée du crayon (extrémité de la pince)

- Paramètre : aucun
- Valeur de retour (flottants) : la coordonnée x / y / z du crayon

<center>
![](../img/fin.png?style=center+noborder)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2022 Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
Version 04-05-2022


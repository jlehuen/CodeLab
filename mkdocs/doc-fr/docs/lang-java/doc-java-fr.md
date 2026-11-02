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
# API CodeLab - Langage Java

> Ce document présente les classes et méthodes Java associées aux modules applicatifs intégrés à CodeLab (`includes_init/java`). Pour les utiliser, il est nécessaire de connaître les bases de la programmation orientée objet en Java.

## Notion de bibliothèque et imports

Dans un programme Java développé sous CodeLab, les fonctionnalités des modules s'utilisent en important les classes appropriées via l'instruction `import` en début de fichier source.

Les classes fournies par l'environnement CodeLab sont réparties dans les packages suivants :

- [`import codelab.Utils;`](#classe-utils) : méthodes statiques utilitaires (temporisation, temps système, saisie)
- [`import codelab.Numpad;`](#classe-numpad) : accès au périphérique virtuel "clavier numérique"
- [`import codelab.Joystick;`](#classe-joystick) : accès au périphérique virtuel "manette de jeu"
- [`import codelab.EventManager;`](#classe-eventmanager) : gestionnaire de file d'évènements
- [`import codelab.ToneGenerator;`](#classe-tonegenerator) : générateur de son et lecteur audio WAV
- [`import codelab.moduleGraphics.Drawing;`](#classe-drawing) : primitives de dessin 2D
- [`import codelab.moduleGraphics.Turtle;`](#classe-turtle) : tortue graphique de style Logo
- [`import codelab.moduleRobotics.RobotLego;`](#classe-robotlego) : pilotage du robot mobile Lego NXT
- [`import codelab.motormodule.NXTMotor;`](#classe-nxtmotor) : pilotage du moteur NXT (simulateur)

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-utils></a>
## Classe Utils

La classe `codelab.Utils` propose des méthodes statiques utilitaires accessibles sans instanciation :

```java
import codelab.Utils;
```

Méthodes disponibles :

- `public static void waitFor(int ms)` : suspend l'exécution du programme pendant une durée spécifiée en millisecondes.
- `public static long systemTime()` : retourne l'heure courante du système en millisecondes.
- `public static String readLine()` : lit une ligne saisie au clavier dans la console.

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-numpad></a>
## Classe Numpad

La classe `codelab.Numpad` permet de lire l'état du pavé numérique virtuel intégré dans CodeLab :

```java
import codelab.Numpad;
```

- `public int getValue()` : retourne la valeur courante de la touche actionnée. Le périphérique est non-bloquant : s'il n'y a aucune saisie active, il retourne `-1`. Les touches spéciales `＊` et `#` retournent respectivement `10` et `11`.

**Exemple d'utilisation :**

```java
Numpad numpad = new Numpad();
int touche = numpad.getValue();
if (touche != -1) {
    System.out.println("Touche pressée : " + touche);
}
```

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-joystick></a>
## Classe Joystick

La classe `codelab.Joystick` permet d'interagir avec la manette de jeu virtuelle intégrée :

```java
import codelab.Joystick;
```

- `public int getValueX()` : retourne la déviation horizontale de la manette dans l'intervalle `[-100, 100]`.
- `public int getValueY()` : retourne la déviation verticale de la manette dans l'intervalle `[-100, 100]`.

**Exemple d'utilisation :**

```java
Joystick joy = new Joystick();
int x = joy.getValueX();
int y = joy.getValueY();
```

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-eventmanager></a>
## Classe EventManager

La classe `codelab.EventManager` gère la file d'attente d'évènements asynchrones générés par l'interface et les capteurs :

```java
import codelab.EventManager;
```

Méthodes :

- `public boolean hasNextEvent()` : indique si un ou plusieurs évènements sont en attente de traitement.
- `public String getNextEvent()` : extrait et retourne le prochain évènement sous forme de chaîne formatée `"device=AAA:name=BBB:value=CCC"`.
- `public int resetEventQueue()` : vide la file d'attente des évènements.

**Exemple d'utilisation :**

```java
EventManager em = new EventManager();
em.resetEventQueue();
while (true) {
    while (em.hasNextEvent()) {
        String event = em.getNextEvent();
        System.out.println("Évènement reçu : " + event);
    }
    Utils.waitFor(20);
}
```

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-tonegenerator></a>
## Classe ToneGenerator

La classe `codelab.ToneGenerator` permet l'émission de signaux sonores et la lecture de fichiers audio :

```java
import codelab.ToneGenerator;
```

Méthodes :

- `public int play(int freq, int time)` : joue un signal sinusoïdal de fréquence `freq` (en Hertz) pendant une durée `time` (en millisecondes). Fonction bloquante.
- `public int playToneOn(int freq)` : démarre la génération continue d'une fréquence `freq` (en Hertz). Fonction non-bloquante.
- `public int playToneOff()` : interrompt la génération démarrée par `playToneOn`.
- `public int playAudioFile(String filename)` : joue une fois le fichier audio spécifié (format WAV placé dans le dossier `audiofiles`).
- `public int loopAudioFile(String filename)` : joue le fichier audio spécifié en boucle continue.
- `public int stopAudioPlayer()` : interrompt la lecture audio.

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-drawing></a>
## Classe Drawing (Atelier Graphique 2D)

La classe `codelab.moduleGraphics.Drawing` propose les primitives de dessin géométrique dans le canevas graphique :

```java
import codelab.moduleGraphics.Drawing;
```

Constantes de couleurs :
- `Drawing.COLOR_WHITE` (0)
- `Drawing.COLOR_BLACK` (1)
- `Drawing.COLOR_RED` (2)
- `Drawing.COLOR_GREEN` (3)
- `Drawing.COLOR_BLUE` (4)
- `Drawing.COLOR_CYAN` (5)
- `Drawing.COLOR_YELLOW` (6)
- `Drawing.COLOR_MAGENTA` (7)
- `Drawing.COLOR_ERROR` (8)

Méthodes de dessin :

- `public int clearScreen()` : efface la zone de dessin.
- `public int setColor(int color)` : sélectionne la couleur courante à l'aide d'une constante de couleur.
- `public int setWidth(int width)` : fixe l'épaisseur du trait en pixels (>= 1).
- `public int drawPoint(int x, int y)` : allume le pixel aux coordonnées `(x, y)`.
- `public int drawLine(int x1, int y1, int x2, int y2)` : trace un segment rectiligne entre `(x1, y1)` et `(x2, y2)`.
- `public int drawCircle(int x, int y, int r)` : trace un cercle de centre `(x, y)` et de rayon `r`.

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-turtle></a>
## Classe Turtle (Tortue Logo)

La classe `codelab.moduleGraphics.Turtle` permet de piloter la tortue graphique pour tracer des figures géométriques et interagir avec le canevas :

```java
import codelab.moduleGraphics.Turtle;
```

Constantes de couleurs :
- Identiques à la classe `Drawing` (`Turtle.COLOR_WHITE` à `Turtle.COLOR_MAGENTA`, et `Turtle.COLOR_ERROR`).

Méthodes de commande :

- `public int clearScreen()` : réinitialise le canevas et efface les tracés.
- `public int setColor(int color)` : fixe la couleur du tracé de la tortue.
- `public int setWidth(int width)` : fixe l'épaisseur du trait tracé par la tortue.
- `public int forward(int distance)` : fait avancer la tortue en ligne droite sur la distance donnée.
- `public int turnRight(int angle)` : fait pivoter la tortue vers la droite (sens horaire) selon l'angle en degrés.
- `public int turnLeft(int angle)` : fait pivoter la tortue vers la gauche (sens anti-horaire) selon l'angle en degrés.
- `public int goto_(int x, int y, int theta)` : déplace directement la tortue aux coordonnées `(x, y)` avec l'orientation `theta` (en degrés) sans tracer de trait.
- `public int show()` : rend la tortue visible à l'écran.
- `public int hide()` : masque la tortue.
- `public int reset()` : replace la tortue au centre de la zone de dessin dans son état initial.
- `public int penUp()` : lève le stylet (les déplacements suivants ne tracent pas de ligne).
- `public int penDown()` : abaisse le stylet (les déplacements suivants tracent un trait).
- `public int getColor()` : retourne le code de couleur du pixel sous la position courante de la tortue.
- `public int getBrightness()` : retourne l'intensité lumineuse du pixel sous la position de la tortue.

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-robotlego></a>
## Classe RobotLego (Robotique mobile)

La classe `codelab.moduleRobotics.RobotLego` permet de commander le robot mobile Lego NXT :

```java
import codelab.moduleRobotics.RobotLego;
```

Constantes des ports moteurs (sorties) :
- `RobotLego.OUT_A`, `RobotLego.OUT_B`, `RobotLego.OUT_C` : moteurs individuels
- `RobotLego.OUT_AB`, `RobotLego.OUT_AC`, `RobotLego.OUT_BC`, `RobotLego.OUT_ABC` : combinaisons de moteurs

Constantes des ports capteurs (entrées) :
- `RobotLego.IN_1`, `RobotLego.IN_2`, `RobotLego.IN_3`, `RobotLego.IN_4`

Constantes de couleurs des capteurs :
- `RobotLego.COLOR_WHITE` à `RobotLego.COLOR_MAGENTA`, et `RobotLego.COLOR_ERROR`.

Méthodes de contrôle des moteurs et capteurs :

- `public int motorOn(int port, int power)` : actionne le ou les moteurs du port spécifié à une puissance donnée dans `[-100, 100]`.
- `public int motorOff(int port)` : coupe l'alimentation du ou des moteurs spécifiés.
- `public int motorRotationCount(int port)` : retourne l'angle de rotation cumulé du moteur (en degrés).
- `public int resetMotorRotationCount(int port)` : réinitialise à 0 le compteur de rotation du moteur.
- `public int getSensorValue(int port)` : retourne la valeur mesurée par le capteur branché sur le port d'entrée indiqué.
- `public int[] getSensorArray(int port)` : lit les 8 valeurs retournées par le capteur suiveur de ligne sous forme d'un tableau `int[8]`.
- `public int setBackground(String name)` : charge un circuit ou une image de fond dans le simulateur.
- `public int setRobotConfiguration(String name)` : définit la configuration matérielle des capteurs du robot (ex. `"COLOR/SONIC/SONIC/NONE"`).

<br/>![](../img/sep.png?style=center+noborder)

<a name=classe-nxtmotor></a>
## Classe NXTMotor (Simulateur Moteur)

La classe `codelab.motormodule.NXTMotor` permet de commander le simulateur de servomoteur NXT développé dans le cadre du tutoriel 3 :

```java
import codelab.motormodule.NXTMotor;
```

Constantes des ports :
- `OUT_A`, `OUT_B`, `OUT_C`

Méthodes :

- `public int motorOn(int port, int power)` : active la rotation du moteur sur le port choisi avec la puissance spécifiée.
- `public int motorOff(int port)` : stoppe le moteur spécifié.
- `public int motorRotationCount(int port)` : lit le nombre de degrés effectués par le moteur depuis la dernière remise à zéro.
- `public int resetMotorRotationCount(int port)` : réinitialise le compteur de degrés du moteur.

<br/><center>
![](../img/fin.png?style=center+noborder)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

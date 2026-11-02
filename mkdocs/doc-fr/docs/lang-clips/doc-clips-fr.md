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
# API CodeLab - Langage CLIPS

> Ce document présente les fonctions et variables globales fournies pour le langage CLIPS (`includes_init/clips`) dans l'environnement CodeLab. Pour les utiliser, il est nécessaire de connaître les bases de la programmation par règles sous CLIPS.

## Chargement des bibliothèques

Sous CodeLab, les bibliothèques CLIPS peuvent être chargées en début de script à l'aide de la directive `#requires` ou via la fonction `batch*` :

- `#requires codelab.utils` : charge les utilitaires d'entrées-sorties, sons et périphériques
- `#requires codelab.moduleGraphics.graph2D` : charge les fonctions du module graphique 2D
- `#requires codelab.moduleRobotics.robotLego` : charge les fonctions du robot mobile Lego NXT
- `#requires codelab.motormodule.NXTMotor` : charge les fonctions du simulateur moteur NXT

<br/>![](../img/sep.png?style=center+noborder)

<a name=utils></a>
## Bibliothèque utilitaire (`codelab/utils.clp`)

### Périphériques et évènements

- `(getNumpadValue)` : retourne la valeur du clavier numérique virtuel (`-1` si aucune touche n'est active, `0` à `9`, `10` pour `＊`, `11` pour `#`).
- `(getJoystickValueX)` : retourne l'axe X de la manette de jeu dans `[-100, 100]`.
- `(getJoystickValueY)` : retourne l'axe Y de la manette de jeu dans `[-100, 100]`.
- `(hasNextEvent)` : retourne `1` s'il y a des évènements dans la file, `0` sinon.
- `(getNextEvent)` : dépile le prochain évènement (`"device=...:name=...:value=..."`).
- `(resetEventQueue)` : vide la file d'attente.

### Génération sonore et audio

- `(playTone ?freq ?time)` : émet un son de fréquence `?freq` (Hz) pendant `?time` (ms).
- `(playToneOn ?freq)` : démarre un son continu à la fréquence `?freq`.
- `(playToneOff)` : coupe le son continu.
- `(playAudioFile ?filename)` : lit un fichier WAV du dossier `audiofiles`.
- `(loopAudioFile ?filename)` : lit un fichier WAV en boucle.
- `(stopAudioPlayer)` : arrête la lecture audio.

### Temporisation et temps système

- `(waitFor ?ms)` : suspend l'exécution pendant `?ms` millisecondes.
- `(systemTime)` : retourne l'horodatage système en millisecondes.

<br/>![](../img/sep.png?style=center+noborder)

<a name=graph2d></a>
## Module Graphique 2D (`codelab/moduleGraphics/graph2D.clp`)

### Constantes de couleur
- `?*COLOR_WHITE*` (0)
- `?*COLOR_BLACK*` (1)
- `?*COLOR_RED*` (2)
- `?*COLOR_GREEN*` (3)
- `?*COLOR_BLUE*` (4)
- `?*COLOR_CYAN*` (5)
- `?*COLOR_YELLOW*` (6)
- `?*COLOR_MAGENTA*` (7)
- `?*COLOR_ERROR*` (8)

### Primitives géométriques
- `(clearScreen)` : efface le canevas graphique.
- `(setColor ?color)` : sélectionne la couleur active du tracé.
- `(setWidth ?width)` : fixe l'épaisseur du trait (>= 1).
- `(drawPoint ?x ?y)` : allume le pixel aux coordonnées `(?x, ?y)`.
- `(drawLine ?x1 ?y1 ?x2 ?y2)` : trace une ligne entre `(?x1, ?y1)` et `(?x2, ?y2)`.
- `(drawCircle ?x ?y ?r)` : trace un cercle de centre `(?x, ?y)` et de rayon `?r`.

### Tortue Logo
- `(turtleShow)` : affiche la tortue graphique.
- `(turtleHide)` : masque la tortue.
- `(turtleReset)` : recentre la tortue à l'état initial.
- `(turtleForward ?distance)` : avance en ligne droite de `?distance` pixels.
- `(turtleTurnRight ?angle)` : pivote vers la droite selon `?angle` degrés.
- `(turtleTurnLeft ?angle)` : pivote vers la gauche selon `?angle` degrés.
- `(turtleGoto ?x ?y ?theta)` : déplace la tortue en `(?x, ?y)` avec le cap `?theta` sans tracer.
- `(turtlePenUp)` : relève le stylet.
- `(turtlePenDown)` : abaisse le stylet.
- `(getColor)` : lit le code couleur sous la tortue.
- `(getBrightness)` : lit la luminosité sous la tortue.

<br/>![](../img/sep.png?style=center+noborder)

<a name=robotlego></a>
## Module Robotique mobile (`codelab/moduleRobotics/robotLego.clp`)

### Constantes de ports et couleurs
- Ports moteurs : `?*OUT_A*`, `?*OUT_B*`, `?*OUT_C*`, `?*OUT_AB*`, `?*OUT_AC*`, `?*OUT_BC*`, `?*OUT_ABC*`
- Ports capteurs : `?*IN_1*`, `?*IN_2*`, `?*IN_3*`, `?*IN_4*`
- Couleurs capteur : `?*COLOR_WHITE*` à `?*COLOR_MAGENTA*`, et `?*COLOR_ERROR*`

### Fonctions
- `(motorOn ?port ?power)` : active le ou les moteurs avec une puissance dans `[-100, 100]`.
- `(motorOff ?port)` : éteint le ou les moteurs spécifiés.
- `(motorRotationCount ?port)` : retourne la rotation en degrés du moteur.
- `(resetMotorRotationCount ?port)` : remet à zéro le compteur de rotation.
- `(getSensorValue ?port)` : lit la valeur du capteur sur le port indiqué.
- `(getSensorArray ?port)` : retourne les 8 valeurs du capteur suiveur de ligne.
- `(setBackground ?name)` : charge une arène ou un circuit dans le simulateur.
- `(setRobotConfiguration ?config)` : définit la configuration des capteurs (ex. `"COLOR/SONIC/SONIC/NONE"`).

<br/><center>
![](../img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

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

![](./img/logo-codelab.png?style=noborder)

# CodeLab IDE & Simulators
# Conception d'un module simulateur
</center>

En suivant ce tutoriel, vous allez réaliser le plugin "MotorModule" qui visualise un moteur Lego NXT et permet de simuler son fonctionnement. Le rendu ci-dessous est le résultat de l'exécution du programme C suivant terminé, c'est-à-dire après une rotation de 45 degrés :

```c
#include "codelab/motormodule/NXTMotor.c"

void main() {
	resetMotorRotationCount(OUT_A);
	motorOn(OUT_A, 25);
	while (motorRotationCount(OUT_A) < 45);
	motorOff(OUT_A);
}
```

<br/>
![](./img/motor-module.png?style=center+noborder)

La différence entre ce module et ceux des précédents tutoriels réside dans leurs fonctionnements : les deux premiers correspondent à des visualisations, celui-ci à une simulation.

### Prérequis pour réaliser ce tutoriel :

- Avoir suivi les deux premiers tutoriels
- Maîtriser les concepts objet et la POO en Java
- Savoir exploiter les API Java awt et Swing
- Comprendre la syntaxe des fichiers XML

<br/>![](./img/sep.png?style=center+noborder)

## 1. Création du plugin avec le CDK

Comme pour les tutoriels précédents, ouvrez un terminal dans le dossier `CDK` et lancez le script de génération :
- Sous Linux / macOS : `./newmodule.sh MotorModule`
- Sous Windows : `newmodule.bat MotorModule`

Le script prépare automatiquement l'arborescence du projet dans `modules/MotorModule/`, avec le package `codelab.modules.motormodule`, la classe principale `MotorModule.java`, la barre d'outils (`MotorModuleToolbar.java` que vous renommez en `MotorToolbar.java`) et le manuel Markdown.

Pour ce module simulateur, effectuez les ajustements suivants :
- Téléchargez dans le dossier `data` les ressources [axis.png](./data/axis.png) et [motor.png](./data/motor.png) ;
- Renommez le fichier `includes/MotorModuleAPI.xml` en `includes/NXTMotor.xml` ;
- Ajoutez la nouvelle classe `MotorSimulator.java` dans `src/codelab/modules/motormodule/`.

Vous disposez de l'arborescence suivante :

<pre>
MotorModule/
├── class/
├── data/
│   ├── icon_clear.png
│   ├── icon_hello.png
│   ├── axis.png                              &larr; Ressource graphique
│   └── motor.png                             &larr; Ressource graphique
├── includes/
│   ├── api.dtd
│   └── NXTMotor.xml                          &larr; Fichier XML d'API renommé
├── lib/
│   └── codelab.jar
├── man/
│   └── MotorModule.md
├── src/
│   └── codelab/
│       └── modules/
│           └── motormodule/
│               ├── MotorModule.java
│               ├── MotorToolbar.java
│               └── MotorSimulator.java       &larr; Classe simulateur
├── templates/
├── build.bat
└── build.sh
</pre>

La compilation via `./build.sh` (ou `build.bat`) produira le paquet **`MotorModule.pac`** automatiquement déployé dans le dossier `modules/` de CodeLab.

<br/>![](./img/sep.png?style=center+noborder)

## 2. Classes principales du module

Le modèle objet du `MotorModule` est similaire à celui du `DrawingModule` à ceci près que nous allons remplacer le `DrawingPanel` par un `MotorSimulator` hérité de la classe `AbstractSimulator`. Les trois classes que vous allez définir sont en rouge dans le diagramme suivant :

![](./img/motor-uml.png?style=center+noborder)

Commençez par définir la classe `MotorModule` comme ci-dessous. Les instructions que le gestionnaire d'instructions (handleRequest) devra prendre en compte sont les suivantes :

- `motorOn(port, puissance)` pour faire tourner le moteur
- `motorOff(port)` pour arrêter le moteur
- `motorRotationCount(port)` pour récupérer la valeur du compteur de rotation
- `resetMotorRotationCount(port)` pour réinitialiser le compteur de rotation

Remarquez que toutes ces instructions correspondent à des méthodes publiques de la classe `MotorSimulator` (qui n'est pas encore définie) que vous pouvez voir dans le diagramme de classes. Remarquez également que les méthodes `start()`, `stop()` et `reset()` ont été complétées pour piloter le simulateur.

Ci-dessous le code de la classe `MotorModule` :

```java
package codelab.modules.motormodule;

import codelab.CodeLab;
import codelab.AbstractModule;

import java.util.Map;

public class MotorModule extends AbstractModule {

	private MotorSimulator simulator;

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public MotorModule(CodeLab codelab) {
		super(codelab);
		setTitle("NXT motor");
		setToolbar(new MotorToolbar(this));
		setComponent(simulator = new MotorSimulator(this));
	}

	///////////////////////////////////////////////////
	// AbstractModule methods to implement (or not)
	///////////////////////////////////////////////////

	public void init() {}
	public void exit() {}

	public void start() {
		simulator.start();
	}

	public void stop() {
		simulator.stop();
	}

	public void reset() {
		simulator.reset();
	}

	///////////////////////////////////////////////////
	// Instruction handler
	///////////////////////////////////////////////////

	public String handleRequest(Map<String, String> params) {
		String cmd = params.get("cmd");
		int port, power;
		switch (cmd) {

			case "motorOn":
				port = Integer.valueOf(params.get("port"));
				power = Integer.valueOf(params.get("power"));
				return String.valueOf(simulator.motorOn(port, power));

			case "motorOff":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(simulator.motorOff(port));

			case "motorRotationCount":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(simulator.motorRotationCount(port));

			case "resetMotorRotationCount":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(simulator.resetMotorRotationCount(port));

			default:
				return unknownCommandError(cmd);
		}
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 3. Définition de la classe MotorSimulator

Nous allons maintenant écrire le simulateur `MotorSimulator` qui hérite de la classe abstraite `AbstractSimulator` fournie dans le kit de développement. Cette dernière est une classe "runnable" qui actualise le simulateur de façon asynchrone, en faisant appel à la méthode `update()` que vous allez définir dans `MotorSimulator`.

Comme les précédents tutoriels, celui-ci n'a pas vocation à expliciter les API Java awt et Swing (classes graphiques, placement des composants, etc.), ces classes étant largement abordées sur le web.

La classe `MotorSimulator` comporte les méthodes suivantes :

- Les méthodes correspondant aux instructions de l'API du module (motorOn, motorOff, etc.) ;
- Les méthodes invoquées par les boutons de la barre d'outils (non encore définie) ;
- La méthode `update()` d'actualisation des paramètres du simulateur ;
- La méthode `paintComponent(Graphics graphics)` qui réalise la visualisation ;

Voilà ci-dessous une première version du simulateur :

```java
package codelab.modules.motormodule;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import codelab.helper.AbstractSimulator;
import codelab.helper.PluginUtils;

public class MotorSimulator extends AbstractSimulator {

	private MotorModule module;

	private BufferedImage image1; // Image of the motor block
	private BufferedImage image2; // Image of the rotating part

	private double power = 0; // Power given to he motor
	private double alpha = 0; // Actual angle of the axis
	private double counter = 0; // Resetable counter

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public MotorSimulator(MotorModule module) {
		this.module = module;
		image1 = PluginUtils.loadBufferedImage(module, "motor.png");
		image2 = PluginUtils.loadBufferedImage(module, "axis.png");
		startSimulationThread(10); // Update every 10 ms
	}

	///////////////////////////////////////////////////
	// Instruction methods
	///////////////////////////////////////////////////

	public int motorOn(int port, int power) {
		this.power = ((double) power) / 1000;
		return 1;
	}

	public int motorOff(int port) {
		power = 0;
		return 1;
	}

	public int motorRotationCount(int port) {
		return (int) Math.toDegrees(counter);
	}

	public int resetMotorRotationCount(int port) {
		counter = 0;
		return 1;
	}

	///////////////////////////////////////////////////
	// Toobar methods
	///////////////////////////////////////////////////

	public void stop() {
		power = 0;
		super.stop();  // Stop simulation
		repaint();     // Repaint the motor
	}

	public void reset() {
		power = 0;
		alpha = 0;
		counter = 0;
		super.stop();  // Stop simulation
		repaint();     // Repaint the motor
	}

	///////////////////////////////////////////////////
	// Updating method
	///////////////////////////////////////////////////

	protected void update() {
		alpha += power;
		counter += power;
		repaint(); // Repaint the motor
	}

	///////////////////////////////////////////////////
	// Drawing method
	///////////////////////////////////////////////////

	private final int WIDTH = 2000;       // Drawing zone
	private final int HEIGHT = 1500;      // Drawing zone
	private final int X_AXIS = 540;       // Position of image2
	private final int Y_AXIS = 118;       // Position of image2
	private final int X_AXIS_LOCAL = 53;  // Position of the axis in image2
	private final int Y_AXIS_LOCAL = 50;  // Position of the axis in image2
	
	private final Color bgcolor = new Color(231, 231, 231);

	private final int X0 = X_AXIS + X_AXIS_LOCAL;
	private final int Y0 = Y_AXIS + Y_AXIS_LOCAL;

	public void paintComponent(Graphics graphics) {
	
		// Transformation of the motor's axis
		AffineTransform affine = new AffineTransform();
		affine.setToTranslation(X_AXIS, Y_AXIS);
		affine.rotate(alpha, X_AXIS_LOCAL, Y_AXIS_LOCAL);

		super.paintComponent(graphics);
		Graphics2D g2d = getGraphics2D(graphics);
		
		// Clear the background
		g2d.setBackground(bgcolor);
		g2d.clearRect(0, 0, WIDTH, HEIGHT);
		
		// Draw the motor
		g2d.drawImage(image1, null, null);
		g2d.drawImage(image2, affine, null);
		
		// Draw the graduation
		g2d.setColor(Color.red);
		for (int deg = 0; deg < 360; deg += 10) {
			double a =  Math.toRadians(deg);
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			g2d.drawLine(
				X0 + (int)(120*cos), Y0 + (int)(120*sin),
				X0 + (int)(130*cos), Y0 + (int)(130*sin));
		}
		g2d.dispose();
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 4. Définition de l'API du module

La cohérence de notre module repose sur une bonne correspondance entre :

- Le gestionnaire d'instructions de la classe `MotorModule`
- Les méthodes publiques de la classe `MotorSimulator`
- Les définitions du fichier XML décrivant l'API du module

Ce dernier permet notamment de générer les fichiers à inclure pour chaque langage cible. Rappellons que le nom de ce fichier est important puisqu'il détermine les noms des fichiers à inclure. Écrivez le document XML suivant dans le fichier `NXTMotor.xml` et placez-le dans le répertoire `includes` :

```xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='motormodule' name='NXTMotor'>

	<description>API du simulateur moteur Lego NXT</description>

	<constant name='OUT_A' type='int' value='0'/>
	<constant name='OUT_B' type='int' value='1'/>
	<constant name='OUT_C' type='int' value='2'/>

	<instruction name='motorOn' type='int'>
		<arg name='port' type='int'/>
		<arg name='power' type='int'/>
	</instruction>

	<instruction name='motorOff' type='int'>
		<arg name='port' type='int'/>
	</instruction>

	<instruction name='motorRotationCount' type='int'>
		<arg name='port' type='int'/>
	</instruction>

	<instruction name='resetMotorRotationCount' type='int'>
		<arg name='port' type='int'/>
	</instruction>

</api>
```

Exemple de programme C :

```c
#include "codelab/motormodule/NXTMotor.c"

void main() {
	resetMotorRotationCount(OUT_A);
	motorOn(OUT_A, 25);
	while (motorRotationCount(OUT_A) < 45);
	motorOff(OUT_A);
}
```

Exemple de programme Java :

```java
import codelab.motormodule.NXTMotor;

class MotorTest extends NXTMotor {

	private void run() {
		resetMotorRotationCount(OUT_A);
		motorOn(OUT_A, 25);
		while (motorRotationCount(OUT_A) < 45);
		motorOff(OUT_A);
	}
	
	public static void main(String args[]) {
		new MotorTest().run();
	}
}
```

Exemple de programme Python :

```python
from codelab.motormodule.NXTMotor import *

resetMotorRotationCount(OUT_A)
motorOn(OUT_A, 25)
while motorRotationCount(OUT_A) < 45: pass
motorOff(OUT_A)
```

Exemple de programme CLIPS :

```lisp
#requires codelab.motormodule.NXTMotor

(defrule only-rule
	=>
	(resetMotorRotationCount ?*OUT_A*)
	(motorOn ?*OUT_A* 25)
	(while (< (motorRotationCount ?*OUT_A*) 45) do)
	(motorOff ?*OUT_A*))
```

<br/>![](./img/sep.png?style=center+noborder)

## 5. Définition de la barre d'outils

L'écriture de la barre d'outils est on ne peut plus simple, puisqu'il suffit juste de demander l'ajout d'un bouton dédié `RESET` avec un `addButton("RESET")` dans le constructeur :

```java
package codelab.modules.motormodule;

import codelab.AbstractToolbar;

public class MotorToolbar extends AbstractToolbar {

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public MotorToolbar(MotorModule module) {
		super(module);
		addSeparator();
		addButton("RESET");
		addButton("MAN");
		endToolbar();
	}

	///////////////////////////////////////////////////
	// Action handler
	///////////////////////////////////////////////////

	public void action(String ident) {
		switch (ident) {
			// No specific button
			default: super.action(ident);
		}
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 6. Génération du module

Le module est maintenant prêt à être généré. Avant cela, vous pouvez éventuellement définir des templates, des exercices, voire internationnaliser votre module, comme cela est expliqué dans le premier tutoriel. Pour générer le module, exécutez le script de build, ce qui aura pour effet de placer un fichier `MotorModule.pac` dans le dossier `modules/` de CodeLab :

![](./img/motor-term.png?style=center+noborder)

## 7. Ajout d'un tableau de bord

Pour terminer ce tutoriel, vous allez ajouter un tableau de bord pour afficher certaines données (puissance et compteur de rotation) en provenance du simulateur. Cette fonctionnalité, prévue dans CodeLab, est facilement réalisable. Il suffit d'impémenter l'interface `DataTableInterface` qui comporte notamment trois méthodes principales :

- Une méthode `getNbProp()` qui retourne le nombre de propriétés à afficher ;
- Une méthode `getPropKey(int)` qui retourne le nom de la ième propriété ;
- Une méthode `getPropVal(int)` qui retourne la valeur de la ième propriété ;

Vous êtes libres de choisir la façon dont ces méthodes accèdent aux données, mais le plus simple est d'utiliser deux tableaux, comme dans l'exemple ci-dessous. L'actualisation des tableaux est réalisée dans la méthode `updateValues()` qui est automatiquement invoquée par le simulateur.

Tout d'abord, complétez la classe `MotorSimulator` de la façon suivante :

```java
import codelab.DataTableInterface;
...

public class MotorSimulator extends AbstractSimulator implements DataTableInterface {
	...

	///////////////////////////////////////////////////
	// DataTableInterface Implementation
	///////////////////////////////////////////////////

	private final int NBDATA = 2;
	private String[] propKey = new String[NBDATA];
	private String[] propVal = new String[NBDATA];

	public int getNbProp() {
		return NBDATA;
	}

	public String getPropKey(int i) {
		return propKey[i];
	}

	public String getPropVal(int i) {
		return propVal[i];
	}

	public void updateValues() {
		propKey[0] = "Power";
		propVal[0] = String.valueOf(power * 1000);
		propKey[1] = "Counter";
		propVal[1] = String.format("%d°", motorRotationCount(0));
	}
}
```

Ensuite, précisez dans le constructeur de `MotorModule` qu'un tableau de bord est disponible :

```java
public MotorModule(CodeLab codelab) {
	super(codelab);
	setTitle("NXT motor");
	setToolbar(new MotorToolbar(this));
	setComponent(simulator = new MotorSimulator(this));
	setSystem(simulator);
}
```

Enfin, ajoutez le bouton dédié `DATA` dans la barre d'outils :

```java
public MotorToolbar(MotorModule module) {
	super(module);
	addSeparator();
	addButton("RESET");
	addButton("DATA");
	addButton("MAN");
	endToolbar();
}
```

Re-générez et testez votre module : vous devrier avoir un nouveau bouton qui ouvre un panel à droite, qui affiche les données numériques "Power" et "Counter" en provenance du simulateur.

<br/><center>
![](./img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2026 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center+noborder)
</center>

<link href='../css/style-md.css' rel='stylesheet'>

<header id='header'>
	<div class='left'>
		<a href='../index.html#documentation'>Codelab Home page</a><br/>
		<a href='#top'>Retourner en haut de la page</a><br/>
		<a href='#toc'>Aller à la table des matières</a><br/>
	</div>
	<div class='center'>
		<h1>Tutoriel CodeLab n°3</h1>
	</div>
	<div class='right'>
		<button type='button' onclick="setlang('fr'); location.href='tuto1-fr.html'">FR</button>
		<button type='button' onclick="setlang('en'); location.href='tuto1-en.html'">EN</button>
	</div>
</header>

<script type='text/javascript'>
	function setlang(lang) {
		const languages = ['fr','en'];
		if (!languages.includes(lang)) lang = 'en';
		sessionStorage.setItem('LANG', lang);
	}
</script>

<a name=0></a>
<br/>
<br/>
<center>
# Conception d'un module simulateur
</center>

En suivant ce tutoriel, vous allez réaliser le plugin "MotorModule" qui visualise un moteur Lego NXT et permet de simuler son fonctionnement. Le rendu ci-dessous est le résultat de l'exécution du programme C suivant terminé, c'est-à-dire après une rotation de 45 degrés :

~~~c
#include "codelab/motormodule/NXTMotor.c"

void main() {
	resetMotorRotationCount(OUT_A);
	motorOn(OUT_A, 25);
	while (motorRotationCount(OUT_A) < 45);
	motorOff(OUT_A);
}
~~~

![](./img/motor-module.png?style=center+margin)

The key difference between this module and those of the previous tutorials lies in how they operate: the first two are visualizations, while this one is a real-time simulation.

### Prerequisites for this tutorial:

- Having completed the first two tutorials
- Mastering object-oriented programming (OOP) in Java
- Knowing how to use Java AWT and Swing APIs
- Understanding XML syntax

<a name=toc></a>
### Table of Contents:

1. [Creating the plugin with the CDK](#1)
2. [Main classes of the module](#2)
3. [Definition of the MotorSimulator class](#3)
4. [Definition of the module API](#4)
5. [Definition of the toolbar](#5)
6. [Module generation](#6)
7. [Adding a dashboard](#7)


<a name=1></a>
## 1. Creating the plugin with the CDK

As in the previous tutorials, open a terminal in the `CDK` directory and execute the generation script:
- Under Linux / macOS: `./newmodule.sh MotorModule`
- Under Windows: `newmodule.bat MotorModule`

The script automatically sets up the `MotorModule/` directory tree with the `codelab.modules.motormodule` package, the `MotorModule.java` and `MotorToolbar.java` classes, and the Markdown manual.

For this simulator module, make the following adjustments:
- Download the graphic resources [axis.png](./data/axis.png) and [motor.png](./data/motor.png) into the `data` directory;
- Rename the API file `includes/MotorModuleAPI.xml` to `includes/NXTMotor.xml`;
- Add the new simulator class `MotorSimulator.java` into `src/codelab/modules/motormodule/`.

You now have the following directory tree:

<pre>
MotorModule/
├── class/
├── data/
│   ├── icon_clear.png
│   ├── icon_hello.png
│   ├── axis.png                              &larr; Graphic resource
│   └── motor.png                             &larr; Graphic resource
├── includes/
│   ├── api.dtd
│   └── NXTMotor.xml                          &larr; Renamed API XML file
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
│               └── MotorSimulator.java       &larr; Simulator class
├── templates/
├── build.bat
└── build.sh
</pre>

Compiling with `./build.sh` (or `build.bat`) will produce the **`MotorModule.pac`** package, automatically deployed into CodeLab's `modules/` directory.

<a name=2></a>
## 2. Classes principales du module

Le modèle objet du `MotorModule` est similaire à celui du `DrawingModule` à ceci près que nous allons remplacer le `DrawingPanel` par un `MotorSimulator` hérité de la classe `AbstractSimulator`. Les trois classes que vous allez définir sont en rouge dans le diagramme suivant :

![](./img/motor-uml.png?style=center+margin)

Commençez par définir la classe `MotorModule` comme ci-dessous. Les instructions que le gestionnaire d'instructions (handleRequest) devra prendre en compte sont les suivantes :

- `motorOn(puissance)` pour faire tourner le moteur
- `motorOff()` pour arrêter le moteur
- `getMotorRotationCount()` pour récupérer la valeur du compteur de rotation
- `resetMotorRotationCount()` pour réinitialiser le compteur de rotation

Remarquez que toutes ces instructions correspondent à des méthodes publiques de la classe `MotorSimulator` (qui n'est pas encore définie) que vous pouvez voir dans le diagramme de classes. Remarquez également que les méthodes `start()`, `stop()` et `reset()` ont été complétées pour piloter le simulateur.

Ci-dessous le code de la classe `MotorModule` :

~~~java
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
~~~

<a name=3></a>
## 3. Définition de la classe MotorSimulator

Nous allons maintenant écrire le simulateur `MotorSimulator` qui hérite de la classe abstraite `AbstractSimulator` fournie dans le kit de développement. Cette dernière est une classe "runnable" qui actualise le simulateur de façon asynchrone, en faisant appel à la méthode `update()` que vous allez définir dans `MotorSimulator`.

Comme les précédents tutoriels, celui-ci n'a pas vocation à expliciter les API Java awt et Swing (classes graphiques, placement des composants, etc.), ces classes étant largement abordées sur le web.

La classe `MotorSimulator` comporte les méthodes suivantes :

- Les méthodes correspondant aux instructions de l'API du module (motorOn, motorOff, etc.) ;
- Les méthodes invoquées par les boutons de la barre d'outils (non encore définie) ;
- La méthode `update()` d'actualisation des paramètres du simulateur ;
- La méthode `paintComponent(Graphics graphics)` qui réalise la visualisation ;

Voilà ci-dessous une première version du simulateur :

~~~java
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
~~~

<a name=4></a>
## 4. Définition de l'API du module

La cohérence de notre module repose sur une bonne correspondance entre :

- Le gestionnaire d'instructions de la classe `MotorModule`
- Les méthodes publiques de la classe `MotorSimulator`
- Les définitions du fichier XML décrivant l'API du module

Ce dernier permet notamment de générer les fichiers à inclure pour chaque langage cible. Rappellons que le nom de ce fichier est important puisqu'il détermine les noms des fichiers à inclure. Écrivez le document XML suivant dans le fichier `NXTMotor.xml` et placez-le dans le répertoire `includes` :

~~~xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='motormodule' name='NXTMotor'>

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
~~~

<a name=5></a>
## 5. Définition de la barre d'outils

L'écriture de la barre d'outils est on ne peut plus simple, puisqu'il suffit juste de demander l'ajout d'un bouton dédié `RESET` avec un `addButton("RESET")` dans le constructeur :

~~~java
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
~~~

<a name=6></a>
## 6. Module generation

The module is now ready to be generated. Before that, you can optionally define templates, exercises, or even internationalize your module, as explained in the first tutorial. To generate the module, run the build script, which will place a `MotorModule.pac` file in the `modules` directory of CodeLab:

![](./img/motor-term.png?style=center+margin)

<a name=7></a>
## 7. Adding a dashboard

To complete this tutorial, you will add a dashboard to display certain data (power and rotation count) coming from the simulator. This feature, provided by CodeLab, is easy to implement. Simply implement the `DataTableInterface` interface, which includes three main methods:

- A `getNbProp()` method that returns the number of properties to display;
- A `getPropKey(int)` method that returns the name of the i-th property;
- A `getPropVal(int)` method that returns the value of the i-th property;

You are free to choose how these methods access the data, but the simplest approach is to use two arrays, as in the example below. Updating the arrays is performed in the `updateValues()` method, which is automatically invoked by the simulator.

First, complete the `MotorSimulator` class as follows:

~~~java
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
		propVal[1] = String.format("%d°", getMotorRotationCount());
	}
}
~~~

Ensuite, précisez dans le constructeur de `MotorModule` qu'un tableau de bord est disponible :

~~~java
public MotorModule(CodeLab codelab) {
	super(codelab);
	setTitle("NXT motor");
	setToolbar(new MotorToolbar(this));
	setComponent(simulator = new MotorSimulator(this));
	setSystem(simulator);
}
~~~

Enfin, ajoutez le bouton dédié `DATA` dans la barre d'outils :

~~~java
public MotorToolbar(MotorModule module) {
	super(module);
	addSeparator();
	addButton("RESET");
	addButton("DATA");
	endToolbar();
}
~~~

Re-générez et testez votre module : vous devrier avoir un nouveau bouton qui ouvre un panel à droite, qui affiche les données numériques "Power" et "Counter" en provenance du simulateur.

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2021 Jérôme Lehuen, Le Mans Université, France
![](./img/licence.png?style=center)
Version 21-06-2021

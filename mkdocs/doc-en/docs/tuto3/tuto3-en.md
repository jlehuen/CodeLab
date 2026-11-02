<link href='../css/style-md.css' rel='stylesheet'>

<header id='header'>
	<div class='left'>
		<a href='../index.html#documentation'>Codelab Home page</a><br/>
		<a href='#top'>Return to the top of this page</a><br/>
		<a href='#toc'>Go to table of contents</a><br/>
	</div>
	<div class='center'>
		<h1>CodeLab Tutorial #3</h1>
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
# Designing a Simulator Module
</center>

By following this tutorial, you will develop the "MotorModule" plugin, which visualizes a Lego NXT motor and simulates its operation. The rendering below shows the result of running the following C program upon completion, after a 45-degree rotation:

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

The script automatically prepares the project tree in `modules/MotorModule/`, with the `codelab.modules.motormodule` package, the main class `MotorModule.java`, the toolbar (`MotorModuleToolbar.java` which you rename to `MotorToolbar.java`), and the Markdown manual.

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
## 2. Main classes of the module

The object model of `MotorModule` is similar to that of `DrawingModule`, except that we replace `DrawingPanel` with `MotorSimulator`, which inherits from the `AbstractSimulator` class. The three classes you will implement are highlighted in red in the following class diagram:

![](./img/motor-uml.png?style=center+margin)

Start by defining the `MotorModule` class as shown below. The instructions that the instruction handler (`handleRequest`) must process are:

- `motorOn(port, power)` to spin the motor
- `motorOff(port)` to stop the motor
- `motorRotationCount(port)` to retrieve the rotation counter value
- `resetMotorRotationCount(port)` to reset the rotation counter

Note that all of these instructions correspond to public methods of `MotorSimulator` (not yet defined) seen in the class diagram. Also note that `start()`, `stop()` and `reset()` have been completed to control the simulator.

Below is the code for the `MotorModule` class:

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
## 3. Defining the MotorSimulator class

We will now write the simulator `MotorSimulator`, which inherits from the abstract class `AbstractSimulator` provided in the development kit. The latter is a "runnable" class that updates the simulator asynchronously by invoking the `update()` method that you will define in `MotorSimulator`.

Like the previous tutorials, this guide is not intended to explain Java AWT and Swing APIs (graphics classes, component layout, etc.), as these are widely covered on the web.

The `MotorSimulator` class contains the following methods:

- Methods corresponding to module API instructions (`motorOn`, `motorOff`, etc.);
- Methods invoked by toolbar buttons (not yet defined);
- The `update()` method for refreshing simulator parameters;
- The `paintComponent(Graphics graphics)` method that renders the visualization;

Below is a first version of the simulator:

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
## 4. Defining the module API

The coherence of our module relies on a tight match between:

- The instruction handler of the `MotorModule` class
- The public methods of the `MotorSimulator` class
- The definitions in the XML file describing the module's API

The latter enables the generation of include files for each target language. Remember that this file name is important because it dictates the names of the include files. Write the following XML document into `NXTMotor.xml` and place it in the `includes` directory:

~~~xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='motormodule' name='NXTMotor'>

	<description>Lego NXT motor simulator API</description>

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

Example C program:

~~~c
#include "codelab/motormodule/NXTMotor.c"

void main() {
	resetMotorRotationCount(OUT_A);
	motorOn(OUT_A, 25);
	while (motorRotationCount(OUT_A) < 45);
	motorOff(OUT_A);
}
~~~

Example Java program:

~~~java
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
~~~

Example Python program:

~~~python
from codelab.motormodule.NXTMotor import *

resetMotorRotationCount(OUT_A)
motorOn(OUT_A, 25)
while motorRotationCount(OUT_A) < 45: pass
motorOff(OUT_A)
~~~

Example CLIPS program:

~~~lisp
#requires codelab.motormodule.NXTMotor

(defrule only-rule
	=>
	(resetMotorRotationCount ?*OUT_A*)
	(motorOn ?*OUT_A* 25)
	(while (< (motorRotationCount ?*OUT_A*) 45) do)
	(motorOff ?*OUT_A*))
~~~

<br/>![](./img/sep.png?style=center+noborder)

<a name=5></a>
## 5. Defining the toolbar

Writing the toolbar could not be simpler, since you just need to request adding a dedicated `RESET` button with `addButton("RESET")` in the constructor:

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
~~~

<a name=6></a>
## 6. Module generation

The module is now ready to be generated. Before that, you can optionally define templates, exercises, or even internationalize your module, as explained in the first tutorial. To generate the module, run the build script, which will place a `MotorModule.pac` file in the `modules/` directory of CodeLab:

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
		propVal[1] = String.format("%d°", motorRotationCount(0));
	}
}
~~~

Next, specify in the `MotorModule` constructor that a dashboard is available:

~~~java
public MotorModule(CodeLab codelab) {
	super(codelab);
	setTitle("NXT motor");
	setToolbar(new MotorToolbar(this));
	setComponent(simulator = new MotorSimulator(this));
	setSystem(simulator);
}
~~~

Finally, add the dedicated `DATA` button to the toolbar:

~~~java
public MotorToolbar(MotorModule module) {
	super(module);
	addSeparator();
	addButton("RESET");
	addButton("DATA");
	addButton("MAN");
	endToolbar();
}
~~~

Re-generate and test your module: you should see a new button that opens a panel on the right, displaying numerical data "Power" and "Counter" from the simulator.

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2021 Jérôme Lehuen, Le Mans Université, France
![](./img/licence.png?style=center)
Version 21-06-2021

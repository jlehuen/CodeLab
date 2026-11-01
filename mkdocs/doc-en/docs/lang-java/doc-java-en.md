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
# CodeLab API - Java Language

> This document presents Java classes and methods provided with CodeLab modules (`includes_init/java`). Basic knowledge of Java object-oriented programming is assumed.

## Packages and Imports

In a Java program written in CodeLab, module functionalities are accessed by importing the appropriate classes:

- [`import codelab.Utils;`](#utils-class) : utility static methods (waiting, system time, line reading)
- [`import codelab.Numpad;`](#numpad-class) : virtual numeric keypad device
- [`import codelab.Joystick;`](#joystick-class) : virtual joystick device
- [`import codelab.EventManager;`](#eventmanager-class) : asynchronous event queue manager
- [`import codelab.ToneGenerator;`](#tonegenerator-class) : audio frequency generator and WAV sound player
- [`import codelab.moduleGraphics.Drawing;`](#drawing-class) : 2D geometric drawing primitives
- [`import codelab.moduleGraphics.Turtle;`](#turtle-class) : Logo-style graphic turtle
- [`import codelab.moduleRobotics.RobotLego;`](#robotlego-class) : Lego NXT mobile robot control
- [`import codelab.motormodule.NXTMotor;`](#nxtmotor-class) : NXT motor simulator

<br/>![](../img/sep.png?style=center+noborder)

<a name=utils-class></a>
## Utils Class

The `codelab.Utils` class provides static helper methods:

```java
import codelab.Utils;
```

Methods:

- `public static void waitFor(int ms)` : pauses execution for the given duration in milliseconds.
- `public static long systemTime()` : returns current system timestamp in milliseconds.
- `public static String readLine()` : reads a string line entered in the console terminal.

<br/>![](../img/sep.png?style=center+noborder)

<a name=numpad-class></a>
## Numpad Class

The `codelab.Numpad` class allows reading the virtual numeric keypad:

```java
import codelab.Numpad;
```

- `public int getValue()` : returns the currently pressed key value. Non-blocking: returns `-1` if no key is active. Special keys `＊` and `#` return `10` and `11` respectively.

<br/>![](../img/sep.png?style=center+noborder)

<a name=joystick-class></a>
## Joystick Class

The `codelab.Joystick` class interacts with the virtual joystick widget:

```java
import codelab.Joystick;
```

- `public int getValueX()` : returns horizontal axis position in `[-100, 100]`.
- `public int getValueY()` : returns vertical axis position in `[-100, 100]`.

<br/>![](../img/sep.png?style=center+noborder)

<a name=eventmanager-class></a>
## EventManager Class

The `codelab.EventManager` class processes asynchronous incoming events:

```java
import codelab.EventManager;
```

Methods:

- `public boolean hasNextEvent()` : returns `true` if events are pending in the queue.
- `public String getNextEvent()` : dequeues and returns next event formatted as `"device=AAA:name=BBB:value=CCC"`.
- `public int resetEventQueue()` : clears the event queue.

<br/>![](../img/sep.png?style=center+noborder)

<a name=tonegenerator-class></a>
## ToneGenerator Class

The `codelab.ToneGenerator` class generates tones and plays audio clips:

```java
import codelab.ToneGenerator;
```

Methods:

- `public int play(int freq, int time)` : plays a sine wave tone of frequency `freq` (Hz) for `time` milliseconds (blocking).
- `public int playToneOn(int freq)` : starts continuous tone generation at frequency `freq` (non-blocking).
- `public int playToneOff()` : stops continuous tone playback.
- `public int playAudioFile(String filename)` : plays a WAV audio file once from the `audiofiles` directory.
- `public int loopAudioFile(String filename)` : plays a WAV audio file in continuous loop.
- `public int stopAudioPlayer()` : stops audio playback.

<br/>![](../img/sep.png?style=center+noborder)

<a name=drawing-class></a>
## Drawing Class (2D Graphics)

The `codelab.moduleGraphics.Drawing` class provides geometric plotting primitives:

```java
import codelab.moduleGraphics.Drawing;
```

Color constants:
- `Drawing.COLOR_WHITE` (0)
- `Drawing.COLOR_BLACK` (1)
- `Drawing.COLOR_RED` (2)
- `Drawing.COLOR_GREEN` (3)
- `Drawing.COLOR_BLUE` (4)
- `Drawing.COLOR_CYAN` (5)
- `Drawing.COLOR_YELLOW` (6)
- `Drawing.COLOR_MAGENTA` (7)
- `Drawing.COLOR_ERROR` (8)

Methods:

- `public int clearScreen()` : clears the canvas.
- `public int setColor(int color)` : selects drawing color from constants.
- `public int setWidth(int width)` : sets line stroke width in pixels (>= 1).
- `public int drawPoint(int x, int y)` : draws a pixel at `(x, y)`.
- `public int drawLine(int x1, int y1, int x2, int y2)` : draws a line segment from `(x1, y1)` to `(x2, y2)`.
- `public int drawCircle(int x, int y, int r)` : draws a circle at center `(x, y)` with radius `r`.

<br/>![](../img/sep.png?style=center+noborder)

<a name=turtle-class></a>
## Turtle Class (Logo Turtle)

The `codelab.moduleGraphics.Turtle` class drives the graphic turtle:

```java
import codelab.moduleGraphics.Turtle;
```

Methods:

- `public int clearScreen()` : resets the canvas.
- `public int setColor(int color)` : selects turtle stroke color.
- `public int setWidth(int width)` : sets turtle line stroke width.
- `public int forward(int distance)` : moves turtle forward by `distance` pixels.
- `public int turnRight(int angle)` : turns turtle clockwise by `angle` degrees.
- `public int turnLeft(int angle)` : turns turtle counter-clockwise by `angle` degrees.
- `public int goto_(int x, int y, int theta)` : moves turtle to `(x, y)` with orientation `theta` without drawing.
- `public int show()` : displays turtle icon.
- `public int hide()` : hides turtle icon.
- `public int reset()` : resets turtle to canvas center.
- `public int penUp()` : lifts pen (movements do not draw).
- `public int penDown()` : lowers pen (movements draw a line).
- `public int getColor()` : returns color code of pixel directly under the turtle.
- `public int getBrightness()` : returns brightness intensity of pixel under the turtle.

<br/>![](../img/sep.png?style=center+noborder)

<a name=robotlego-class></a>
## RobotLego Class (Mobile Robotics)

The `codelab.moduleRobotics.RobotLego` class controls the Lego NXT mobile robot:

```java
import codelab.moduleRobotics.RobotLego;
```

Motor ports (outputs):
- `RobotLego.OUT_A`, `RobotLego.OUT_B`, `RobotLego.OUT_C` (single motors)
- `RobotLego.OUT_AB`, `RobotLego.OUT_AC`, `RobotLego.OUT_BC`, `RobotLego.OUT_ABC` (motor combinations)

Sensor ports (inputs):
- `RobotLego.IN_1`, `RobotLego.IN_2`, `RobotLego.IN_3`, `RobotLego.IN_4`

Methods:

- `public int motorOn(int port, int power)` : activates motor(s) at given power in `[-100, 100]`.
- `public int motorOff(int port)` : stops motor(s).
- `public int motorRotationCount(int port)` : returns motor rotation count in degrees.
- `public int resetMotorRotationCount(int port)` : resets rotation counter to 0.
- `public int getSensorValue(int port)` : reads input sensor value.
- `public int[] getSensorArray(int port)` : reads 8 values from line tracking sensor into an `int[8]` array.
- `public int setBackground(String name)` : sets simulator background image or racetrack.
- `public int setRobotConfiguration(String name)` : configures the 4 robot sensors (e.g. `"COLOR/SONIC/SONIC/NONE"`).

<br/>![](../img/sep.png?style=center+noborder)

<a name=nxtmotor-class></a>
## NXTMotor Class (Motor Simulator)

The `codelab.motormodule.NXTMotor` class drives the NXT motor simulator:

```java
import codelab.motormodule.NXTMotor;
```

Port constants:
- `OUT_A`, `OUT_B`, `OUT_C`

Methods:

- `public int motorOn(int port, int power)` : drives the motor on `port` at given `power`.
- `public int motorOff(int port)` : stops motor.
- `public int motorRotationCount(int port)` : returns rotation angle in degrees.
- `public int resetMotorRotationCount(int port)` : resets angle counter to zero.

<br/><center>
![](../img/fin.png?style=center+noborder)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

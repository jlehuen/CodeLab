<link href='../css/style-md.css' rel='stylesheet'>

<header id='header'>
	<div class='left'>
		<a href='../index.html#documentation'>Codelab Home page</a><br/>
		<a href='#top'>Top of this page</a><br/>
	</div>
	<div class='center'>
		<h1>CodeLab : C language</h1>
	</div>
	<div class='right'>
		<button type='button' onclick="setlang('fr'); location.href='doc-c-fr.html'">FR</button>
		<button type='button' onclick="setlang('en'); location.href='doc-c-en.html'">EN</button>
	</div>
</header>

<script type='text/javascript'>
	function setlang(lang) {
		const languages = ['fr','en'];
		if (!languages.includes(lang)) lang = 'en';
		sessionStorage.setItem('LANG', lang);
	}
</script>

<style>
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

<a name=0></a><br/>
<br/>
<br/>
<center>
# CodeLab IDE & Simulators<br>C language Reference 
</center>

## Table of contents

- [Notion of library](#lib)
- [Notion of prototype](#proto)
- [Input/output functions](#inout)
- [API of the Graphics module](#graph)
- [API of the Robotics module](#robot)
- [API of the 3D Robotics module](#robot3D)

## Introduction

<a name=lib></a><br/>
### Notion of function library:

The C language natively integrates the basic concepts of imperative programming (primitive types, variable and memory management, programming structures, low level inputs and outputs). To go further, it is necessary to specify at the beginning of the program with the `#include` directive the libraries which contain the functions used which are not part of the basic C language. This is not a C language instruction, but a **directive** taken into account at compile time.

The functions described in the following are stored in the following libraries:

- `#include "codelab/utils.c"` loads the [input-output function library](#inout)
- `#include "codelab/moduleGraphics/graph2D.c"` loads the [graphics module library](#graph)
- `#include "codelab/moduleRobotics/robotLego.c"` loads the [robotic module library](#robot)
- `#include "codelab/motormodule/NXTMotor.c"` loads the NXT motor simulator module library
- `#include "codelab/tasks.c"` provides POSIX multithreading macros
- `#include "codelab/moduleRobotics3D/robotCoretech.c"` loads the [3D robotics module library](#robot3D)

<a name=proto></a><br/>
### Notion of prototype of a function:

To document a function, we use its **prototype** which specifies:

- The **name** of the function;
- The **type** of the return value or `void` if no return value;
- The **number** and **type** of the parameters (the parameter names are given as an indication).

<font color=red>_It is important to distinguish between **prototype** of a function and **call** of this same function:_</font>

For example `void drawCircle(int x, int y, int r)` is the prototype of the `drawCircle` function. To call this function in a program, you must write neither the `void` at the beginning of the line, nor the `int` before the parameters, and you must add a semicolon after the closing parenthesis:

```c
#include "codelab/moduleGraphics/graph2D.c"

// Draw a circle with center (100, 100) and radius 50

void main() {
	clearScreen();
	drawCircle(100, 100, 50);
}
```

<br/><br/>![](../img/sep.png?style=center)<br/><br/>

<a name=inout></a><br/>
## Input/output functions:

This large library contains mainly input and output functions (text, audio, etc.) and access to peripherals. The `string` type does not exist as such in the C language; it has been implemented in this library by a `#define string char*` in the interest of writing simplicity. <font color=red>Warning: string manipulation in C language relies on memory management mechanisms (dynamic allocation). Consult suitable documentation if necessary.</font> Line breaks must be indicated using the `\n` notation inside strings.

Display functions to the terminal:

- [`void printStr(string text)`](#printStr)
- [`void printStrLn(string text)`](#printStr)
- [`void printInt(int number)`](#printInt)
- [`void printIntLn(int number)`](#printInt)
- [`void printLong(long number)`](#printLong)
- [`void printLongLn(long number)`](#printLong)
- [`void printFloat(float number)`](#printFloat)
- [`void printFloatLn(float number)`](#printFloat)
- [`void printFormat(string format, ...)`](#printFormat)
- [`void newline()`](#newline)

Keyboard input functions:

- [`string inputStr(string prompt)`](#inputStr)
- [`int inputInt(string prompt)`](#inputInt)
- [`float inputFloat(string prompt)`](#inputFloat)
- [`void waitKeyPressed()`](#waitKeyPressed)

Device access functions:

- [`int getNumpadValue()`](#getNumpadValue)
- [`int getJoystickValueX()`](#getJoystickValueX)
- [`int getJoystickValueY()`](#getJoystickValueX)
- [`int hasNextEvent()`](#hasNextEvent)
- [`string getNextEvent()`](#getNextEvent)
- [`void resetEventQueue()`](#resetEventQueue)

Audio output functions:

- [`void playTone(int freq, int time)`](#playTone)
- [`void playToneOn(int freq)`](#playToneOn)
- [`void playToneOff()`](#playToneOff)
- [`void playAudioFile(string filename)`](#playAudioFile)
- [`void loopAudioFile(string filename)`](#playAudioFile)
- [`void stopAudioPlayer()`](#stopAudioPlayer)

Fonctions additionnelles non-ES :

- [`int randomInt(int max)`](#randomInt)
- [`void initRandom()`](#randomInt)
- [`void waitFor(int time)`](#waitFor)
- [`long systemTime()`](#systemTime)

--
<a name=printStr></a><br/>
`void printStr(string text)`<br/>
`void printStrLn(string text)`

**Description:** Display a message in the terminal (suffix `Ln` = line feed)

- Parameter text: the text to display (variable or literal constant)
- Return value: none

--
<a name=printInt></a><br/>
`void printInt(int number)`<br/>
`void printIntLn(int number)`

**Description:** Displays an integer number in the terminal (suffix `Ln` = line feed)

- Parameter number: the value to display (variable or literal constant)
- Return value: none

--
<a name=printLong></a><br/>
`void printLong(long number)`<br/>
`void printLongLn(long number)`

**Description:** Displays a long integer in the terminal (suffix `Ln` = line feed)

- Parameter number: the value to display (variable or literal constant)
- Return value: none

--
<a name=printFloat></a><br/>
`void printFloat(float number)`<br/>
`void printFloatLn(float number)`

**Description:** Displays a floating point number in the terminal (suffixe `Ln` = retour à la ligne)

- Parameter number: the value to display (variable or literal constant)
- Return value: none

--
<a name=printFormat></a><br/>
`void printFormat(string format, ...)`

**Description:** Displays values in a formatted way

- Parameter format: a string containing "places"
- Return value: none

**Explanation:** This function can take a variable number of parameters. The first parameter (format) is a string that contains "places" noted `%` where the values of different expressions will be inserted, the following parameters are expressions. <font color=red>There must be as many "places" as expressions.</font> The principle is as follows: each `%` is followed by a letter that specifies the type of expression to be displayed:

- `%d` for `int`
- `%l` for `long`
- `%f` for `float`
- `%s` for `string`

**Example of use:**

```c
#include "codelab/utils.c"

void main() {
	string name = "Valentin";
	int age = 14;
	printFormat("Hello %s you are %d years old\n", name, age);
}
```

--
<a name=newline></a><br/>
`void newline()`

**Description:** Causes a line break in the terminal

- Parameter: none
- Return value: none

--
<a name=inputStr></a><br/>
`string inputStr(string prompt)`

**Description:** Displays a prompt in the terminal and enter a string

- Parameter prompt: a text to display in the terminal as a prompt
- Return value: the string entered on the keyboard

--
<a name=inputInt></a><br/>
`int inputInt(string prompt)`

**Description:** Displays a prompt in the terminal and enter an integer

- Parameter prompt: a text to display in the terminal as a prompt
- Return value: the integer entered on the keyboard

--
<a name=inputFloat></a><br/>
`float inputFloat(string prompt)`

**Description:** Displays a prompt in the terminal and enter a floating number

- Parameter prompt: a text to display in the terminal as a prompt
- Return value: the floating number entered on the keyboard

--
<a name=waitKeyPressed></a><br/>
`void waitKeyPressed()`

**Description:** Waits for a keyboard action (blocking function)

- Parameter: none
- Return value: none

--
<a name=getNumpadValue></a><br/>
`int getNumpadValue()`

**Description:** Returns the value of the virtual device "numpad"

- Parameter: none
- Return value: an integer in the interval [ -1, 11 ]

**Explication:** The numeric keyboard (numpad) is a non-blocking device, i.e. it always returns a value, even when no key is actionned. In this case, it returns the value -1. The values 10 and 11 correspond to the keys [＊] and [ # ] respectively.

--
<a name=getJoystickValueX></a><br/>
`int getJoystickValueX()`<br/>
`int getJoystickValueY()`

**Description:** Returns the X and Y values of the virtual device "joystick"

- Parameter: none
- Return value: an integer in the interval [ -100, 100 ]

--
<a name=hasNextEvent></a><br/>
`int hasNextEvent()`

**Description:** Informs if there are any events waiting to be processed

- Parameter: none
- Return value: 0 if the queue is empty, 1 if not

--
<a name=getNextEvent></a><br/>
`string getNextEvent()`

**Description:** Retrieves an event from the queue

- Parameter: none
- Return value: the event coded in a string `"device=AAA:name=BBB:value=CCC"`

**Example of use:** A method to decode the event is to use a regular expression or the `sscanf` function:

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

--
<a name=resetEventQueue></a><br/>
`void resetEventQueue()`

**Description:** Reset (empty) the event queue

- Parameter: none
- Return value: none

--
<a name=playTone></a><br/>
`void playTone(int frequency, int time)`

**Description:** Generates a sound with a given frequency and duration (blocking function)

- Parameter frequency: the frequency in Hertz
- Parameter time: the duration in milliseconds
- Return value: none

--
<a name=playToneOn></a><br/>
`void playToneOn(int frequence)`

**Description:** Generates a sound with a given frequency (non-blocking function)

- Parameter frequency: the frequency in Hertz
- Return value: none

--
<a name=playToneOff></a><br/>
`void playToneOff()`

**Description:** Stops the generator initialized with `playToneOn`

- Parameter: none
- Return value: none

--
<a name=playAudioFile></a><br/>
`void playAudioFile(string filename)`<br/>
`void loopAudioFile(string filename)`

**Description:** Plays a WAV, PCM 44100 Hz, 8 or 16 bit audio file

- Parameter filename: the name of the file, placed in the directory `audiofiles`
- Return value: none

--
<a name=stopAudioPlayer></a><br/>
`void stopAudioPlayer()`

**Description:** Stops the player initialized with `playAudioFile` or `loopAudioFile`

- Parameter: none
- Return value: none

--
<a name=randomInt></a><br/>
`int randomInt(int max)`<br/>
`void initRandom()`

**Description:** Returns a "pseudo-random" integer (see note)

- Parameter max: the upper bound (excluded) of the interval
- Return value: the random integer in [ 0, max [

**Warning:** To obtain different random sequences at each execution of the program, place the instruction `initRandom()` at the beginning of the program.

--
<a name=waitFor></a><br/>
`void waitFor(int time)`

**Description:** Causes a delay for a given duration (blocking function)

- Parameter time: the duration in milliseconds
- Return value: none

--
<a name=systemTime></a><br/>
`long systemTime()`

**Description:** Returns a "system time" in a long integer

- Parameter: none
- Return value: the system time in milliseconds

**Explanation:** Any computer can access a "system time" which allows to calculate time intervals. Warning: the value returned by the systemTime function can be very large and requires the use of the long integer type.

<br/><br/>![](../img/sep.png?style=center)<br/><br/>

<a name=graph></a><br/>
## API of the Graphics module

This library gathers the functions of the graphics lab. It includes two families of functions: functions for drawing **geometric primitives** (point, line and circle) and functions for controlling the **graphic turtle** (move forward, turn left, turn right, etc.). For more information about the graphic turtle, see the Wikipedia page dedicated to the [Logo language](https://en.wikipedia.org/wiki/Logo_(programming_language)) and its pedagogy.

The functions of the Graphics lab are as follows:

- [`void clearScreen()`](#clearScreen)
- [`void setColor(int color)`](#setColor)
- [`void setWidth(int width)`](#setWidth)
- [`void drawPoint(int x, int y)`](#drawPoint)
- [`void drawLine(int x1, int y1, int x2, int y2)`](#drawLine)
- [`void drawCircle(int x, int y, int radius)`](#drawCircle)
- [`void turtleShow()`](#turtleShow)
- [`void turtleHide()`](#turtleShow)
- [`void turtleReset()`](#turtleReset)
- [`void turtleGoto(int x, int y, int heading)`](#turtleGoto)
- [`void turtleForward(int distance)`](#turtleForward)
- [`void turtleTurnLeft(int angle)`](#turtleTurnLeft)
- [`void turtleTurnRight(int angle)`](#turtleTurnRight)
- [`void turtlePenUp()`](#turtlePenUp)
- [`void turtlePenDown()`](#turtlePenDown)
- [`int getColor()`](#getColor)
- [`int getBrightness()`](#getBrightness)

--
<a name=clearScreen></a><br/>
`void clearScreen()`

**Description:** Erase the drawing area

- Parameter: none
- Return value: none

--
<a name=setColor></a><br/>
`void setColor(int couleur)`

**Description:** Sets the color used for future drawings

- Parameter: an authorized color code (see below)
- Return value: none

**Color codes:**

- To specify the white color use the constant `COLOR_WHITE`
- To specify the black color use the `COLOR_BLACK` constant
- To specify the red color use the `COLOR_RED` constant
- To specify the green color use the `COLOR_GREEN` constant
- To specify the blue color use the `COLOR_BLUE` constant
- To specify the cyan color use the `COLOR_CYAN` constant
- To specify the yellow color use the `COLOR_YELLOW` constant
- To specify the magenta color use the `COLOR_MAGENTA` constant

--
<a name=setWidth></a><br/>
`void setWidth(int width)`

**Description:** Sets stroke line width for future drawings

- Parameter width: the line width in pixels (>= 1)
- Return value: none

--
<a name=drawPoint></a><br/>
`void drawPoint(int x, int y)`

**Description:** Put a pixel at position (x, y)

- Parameter x: the x-coordinate of the pixel
- Parameter y: the y-coordinate of the pixel
- Return value: none

--
<a name=drawLine></a><br/>
`void drawLine(int x1, int y1, int x2, int y2)`

**Description:** Draw a line between the point (x1, y1) and the point (x2, y2)

- Parameter x1: the abscissa coordinate of the first point
- Parameter y1: the ordinate coordinate of the first point
- Parameter x2: the x-coordinate of the second point
- Parameter y2: the y-coordinate of the second point
- Return value: none

--
<a name=drawCircle></a><br/>
`void drawCircle(int x, int y, int radius)`

**Description:** Draw a circle with center (x, y) and radius

- Parameter x: the x-coordinate of the center of the circle
- Parameter y: the y-coordinate of the center of the circle
- Parameter radius: the radius of the circle
- Return value: none

--
<a name= turtleShow></a><br/>
`void turtleShow()`<br/>
`void turtleHide()`

**Description:** Show or hide the graphic turtle

- Parameter: none
- Return value: none

--
<a name=turtleReset></a><br/>
`void turtleReset()`

**Description:** Resets the graphic turtle (centered)

- Parameter: none
- Return value: none

--
<a name=turtleGoto></a><br/>
`void turtleGoto(int x, int y, int heading)`

**Description:** Place the turtle at (x, y) with the given heading without drawing a line

- Parameter x: the x-coordinate of the endpoint
- Parameter y: the y-coordinate of the endpoint
- Parameter heading: the orientation heading in degrees (0 = up, 90 = right, etc.)
- Return value: none

--
<a name=turtleForward></a><br/>
`void turtleForward(int distance)`

**Description:** Move the turtle in a straight line over a given distance

- Parameter distance: the distance in "pixel equivalents"
- Return value: none

--
<a name=turtleTurnLeft></a><br/>
`void turtleTurnLeft(int angle)`

**Description:** Rotates the turtle counter-clockwise by a given angle

- Parameter angle: the angle expressed in degrees
- Return value: none

--
<a name=turtleTurnRight></a><br/>
`void turtleTurnRight(int angle)`

**Description:** Rotates the turtle clockwise through a given angle

- Parameter angle: the angle expressed in degrees
- Return value: none

--
<a name=turtlePenUp></a><a name=turtlePenDown></a><br/>
`void turtlePenUp()`<br/>
`void turtlePenDown()`

**Description:** Lifts or lowers the drawing pen of the turtle

- Parameter: none
- Return value: none

**Explanation:** When the pen is lifted (`turtlePenUp`), turtle movements do not leave a trail. When lowered (`turtlePenDown`), movements draw a line with current color and width.

--
<a name=getColor></a><br/>
`int getColor()`

**Description:** Returns the color code of the pixel under the turtle

- Parameter: none
- Return value: a color code (`COLOR_WHITE`, `COLOR_BLACK`, etc.)

--
<a name=getBrightness></a><br/>
`int getBrightness()`

**Description:** Returns the brightness intensity of the pixel under the turtle

- Parameter: none
- Return value: an integer representing light brightness

<br/><br/>![](../img/sep.png?style=center)<br/><br/>

<a name=robot></a><br/>
## API of the Robotics module

This library gathers the functions allowing to drive the NXT robot of the Robotics lab. All these functions have a `port` integer parameter which represents either an output port (to access the motors) or an input port (to access the sensors). It is also possible in some cases to specify two output ports simultaneously.

The NXT robot has three physical output ports (the left and right motors are usually plugged on **<font color=red>port B</font>** and **<font color=red>port C</font>**). The following constants define motor ports:

- `OUT_A`, `OUT_B`, `OUT_C` to control individual motors
- `OUT_AB`, `OUT_AC`, `OUT_BC`, `OUT_ABC` to control multiple motors simultaneously

The NXT robot also has four input ports (`IN_1`, `IN_2`, `IN_3`, `IN_4`). The simulator offers predefined configurations (tool "key" in the toolbar) to be chosen according to the exercise to be performed. For example, the configuration `COLOR/SONIC/SONIC/NONE` means that a color sensor is connected to port #1 and that two distance sensors are connected respectively to ports #2 and #3, port #4 being unused.

Color constants for sensors are identical to the graphics module (`COLOR_WHITE` to `COLOR_MAGENTA`, and `COLOR_ERROR`).

The functions of the Robotics lab are as follows:

- [`void motorOn(int port, int power)`](#motorOn)
- [`void motorOff(int port)`](#motorOff)
- [`void resetMotorRotationCount(int port)`](#resetMotorRotationCount)
- [`int motorRotationCount(int port)`](#motorRotationCount)
- [`int getSensorValue(int port)`](#getSensorValue)
- [`void getSensorArray(int port, int array[])`](#getSensorArray)
- [`void setBackground(string name)`](#setBackground)
- [`void setRobotConfiguration(string config)`](#setRobotConfiguration)
- [`void simu_start()`](#simu_start)
- [`void simu_stop()`](#simu_start)

--
<a name=motorOn></a><br/>
`void motorOn(int port, int power)`

**Description:** Starts one or more motors

- Parameter port: the motor port(s) (use a symbolic constant)
- Parameter power: the power of the motor(s) in the range [ -100, 100 ]
- Return value: none

** Warning:** A value outside the range [ -100, 100 ] causes an emergency stop

--
<a name=motorOff></a><br/>
`void motorOff(int port)`

**Description:** Stops one or more motors

- Parameter port: the motor port(s) (use a symbolic constant)
- Return value: none

--
<a name=motorRotationCount></a><br/>
`int motorRotationCount(int port)`

**Description:** Retrieves the value of the rotation counter of a motor

- Parameter port: the motor port (use a symbolic constant)
- Return value: the number of degrees since the last reset

**Explication:** Internally, each motor is combined with an optical device that counts the number of degrees the motor makes. When using this function, the motor is then considered as a sensor, since it returns a value.

** Warning:** This function is only applicable to one motor at a time

--
<a name=resetMotorRotationCount></a><br/>
`void resetMotorRotationCount(int port)`

**Description:** Stops a motor and resets its rotation counter

- Parameter port: the motor port (use a symbolic constant)
- Return value: none

**Warning:** This function is only applicable to one motor at a time

--
<a name=getSensorValue></a><br/>
`int getSensorValue(int port)`

**Description:** Retrieves the value of a sensor (except for the line sensor)

- Parameter port: the input port used (use a symbolic constant)
- Return value: the value read (or -1 in case of reading error):
	- If the sensor is the numpad, you get a value in [ -1, 11 ]
	- If the sensor is the compass, you get a value in degrees in [ 0, 360 ]
	- If the sensor is a contact one, you get a value of 0 or 1
	- If the sensor is a distance one, you get a distance in cm in [ 0, 100 ]
	- If the sensor is a color one, you get a color coded as follows:
		- The white color is coded by the constant `COLOR_WHITE`
		- The black color is coded by the constant `COLOR_BLACK`
		- The red color is coded by the constant `COLOR_RED`
		- The green color is coded by the constant `COLOR_GREEN`
		- The blue color is coded by the constant `COLOR_BLUE`
		- The color cyan is coded by the constant `COLOR_CYAN`
		- The yellow color is coded by the constant `COLOR_YELLOW`
		- The code `COLOR_ERROR` means that the color could not be determined

<font color=red>**Warning:** The cyan color has a particular property: it behaves like an obstacle with the contact and distance sensors, as well as with the robot itself (collisions). It is particularly used on some backgrounds.</font>

**Warning:** The numpad is a non-blocking device, i.e. it always returns a value, even when no key is selected. In this case, it returns the value `-1`. The values `10` and `11` correspond to the [＊] and [ # ] keys respectively.

--
<a name= getSensorArray></a><br/>
`void getSensorArray(int port, int array[])`

**Description:** Retrieves in a table the 8 values of the line sensor

- Parameter port: the input port used (use a symbolic constant)
- Parameter array: an array of 8 integers (modified by the function)
- Return value: none

**Example of use:**

```c
#include "codelab/moduleRobotics/robotLego.c"

void main() {
	int array[8];
	...
	getSensorArray(IN_1, array);
	...
}
```

--
<a name=setBackground></a><br/>
`void setBackground(string name)`

**Description:** Sets the background image or racetrack in the robotics simulator

- Parameter name: the image name (e.g. `"circuit1.png"`, `"arene.png"`)
- Return value: none

--
<a name=setRobotConfiguration></a><br/>
`void setRobotConfiguration(string config)`

**Description:** Sets sensor configuration for the mobile robot

- Parameter config: descriptive string of the 4 ports (e.g. `"COLOR/SONIC/SONIC/NONE"`)
- Return value: none

--
<a name=simu_start></a><a name=simu_stop></a><br/>
`void simu_start()`<br/>
`void simu_stop()`

**Description:** Starts or stops time animation in the simulator

- Parameter: none
- Return value: none

<br/><br/>![](../img/sep.png?style=center)<br/><br/>

<a name=robot3D></a><br/>
## API of the 3D Robotics module

This library gathers the functions allowing to drive the CORETECH robot of the 3D Robotics lab. This "manipulator arm" has 5 stepper motors, which drive the 4 main axes of the robot, and the orientation of the clamp. The motors are identified by the following integer predefined constants:

![](../img/coretech.png?style=center)

The functions to control the motors are the following:

- [`int motorOn(int motor, float speed)`](#motorOn3D)
- [`int motorStep(int motor, int steps)`](#motorStep)
- [`int motorOff(int motor)`](#motorOff3D)
- [`int grab()`](#grab)
- [`int drop()`](#grab)

The functions to access the sensors are the following:

- [`float getRotation(int motor)`](#getRotation)
- [`char* getSensorValue(int sensor)`](#getSensorValue3D)

The drawing functions in the 3D space are the following:

- [`int plot()`](#plot)
- [`int drawOn()`](#drawOn)
- [`int drawOff()`](#drawOff)
- [`int clearAll()`](#clearAll)
- [`int setColor(int r, int g, int b)`](#setColor3D)
- [`char* putBall(float x, float y, float z, int r, int g, int b, const char* descr)`](#putBall)

The following functions calculate the position of the clamp:

- [`float getX()`](#getX)
- [`float getY()`](#getX)
- [`float getZ()`](#getX)

--
<a name=motorOn3D></a><br/>
`int motorOn(int motor, float speed)`

**Description:** Starts a motor with a given speed

- Parameter motor: the motor identifier (use a symbolic constant)
- Parameter speed: the motor speed in the interval [ -2.0, 2.0 ]
- Return value: 1 if the command has been executed, -1 otherwise

**Attention:** A value outside the range [ -2.0, 2.0 ] causes an emergency stop.

--
<a name=motorStep></a><br/>
`int motorStep(int motor, int steps)`

**Description:** Operates a motor for a given number of steps

- Parameter motor: the motor identifier (use a symbolic constant)
- Parameter steps: the number of steps in the interval [ -10, 10 ]
- Return value: 1 if the command has been executed, -1 otherwise

**Attention:** A value outside the range [ -10, 10 ] causes an emergency stop.

--
<a name=motorOff3D></a><br/>
`int motorOff(int motor)`

**Description:** Stops a motor

- Parameter motor: the motor identifier (use a symbolic constant)
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=grab></a><br/>
`int grab()`<br/>
`int drop()`

**Description:** Operates the clamp to pick up or release an object

- Parameter: none
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=getRotation></a><br/>
`float getRotation(int motor)`

**Description:** Returns the position of the element actuated by a motor

- Parameter motor: the motor identifier (use a symbolic constant)
- Return value: un angle en degrés

**Warning:** The angular ranges of the elements are as follows:

- Axis 0 (base): [ 0, 360 ] continuously
- Axis 1 (upper arm): [ -110, 110 ]
- Axis 2 (lower arm): [ -130, 130 ]
- Axis 3 (hand): [ -120, 120 ]
- Axis 4 (clamp): [ 0, 360 ] continuously

--
<a name=getSensorValue3D></a><br/>
`char* getSensorValue(int sensor)`

**Description:** Returns the value of a sensor

- Parameter sensor: the sensor identifier (use a symbolic constant)
	- Contact sensor at the end of the clamp: `CONTACT_SENSOR`
	- Color sensor at the end of the clamp: `COLOR_SENSOR`
- Return value: the data <font color=red>coded in a string</font> or "ERROR"

**Warning:** The coding of the return value depends on the type of sensor:

- In the case of the contact sensor: "1" if contact or "" (empty string)
- In the case of the color sensor: "R:G:B" (RGB code) or "NONE

--
<a name=plot></a><br/>
`int plot()`

**Description:** Draw a colored point at the end of the clamp, the color is given by [`setColor`](#setColor3D)

- Parameter: none
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=drawOn></a><br/>
`int drawOn()`

**Description:** Initialize a colored drawing, the color is given by [`setColor`]

- Parameter: none
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=drawOff></a><br/>
`int drawOff()`

**Description:** Ends the drawing initialized by [`drawOn`](#drawOn)

- Parameter: none
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=clearAll></a><br/>
`int clearAll()`

**Description:** Deletes drawings and objects from the scene

- Parameter: none
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=setColor3D></a><br/>
`int setColor(int r, int g, int b)`

**Description:** Defines the color of future drawings with [`plot`](#plot) and [`drawOn`](#drawOn)

- Parameters r, g, b: the RGB components of the color in [ 0, 255 ]
- Return value: 1 if the command has been executed, -1 otherwise

--
<a name=putBall></a><br/>
`char* putBall(float x, float y, float z, int r, int g, int b, const char* descr)`

**Description:** Create and position a colored ball in the scene

- Parameters x, y, z : the coordinates where to place the ball
- Parameters r, g, b : the RGB components of the color in [ 0, 255 ]
- Parameter descr : a literal description of the ball (e.g. "the red ball")
- Return value: the unique identifier (uid) of the ball

--
<a name=getX></a><br/>
`float getX()`<br/>
`float getY()`<br/>
`float getZ()`

**Description:** Returns the position of the end of the clamp

- Parameter: none
- Return values: the coordinates (x, y, z) of the end of the clamp

<br/><center>
![](../img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>


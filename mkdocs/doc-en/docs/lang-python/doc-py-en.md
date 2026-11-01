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
# CodeLab API - Python Language

> This document presents Python modules and functions included in CodeLab (`includes_init/python`).

## Libraries and Imports

CodeLab features the following Python modules:

- `from codelab.utils import *` : general input/output, sound, timer and peripheral functions
- `from codelab.moduleGraphics.graph2D import *` : 2D geometric plotting and Logo turtle functions
- `from codelab.moduleRobotics.robotLego import *` : Lego NXT mobile robot simulation
- `from codelab.moduleRobotics.robotRovio import *` : Rovio mobile robot simulation
- `from codelab.motormodule.NXTMotor import *` : NXT motor simulator module

<br/>![](../img/sep.png?style=center+noborder)

<a name=utils></a>
## Utilities Module (`codelab.utils`)

### Peripheral access:
- `getNumpadValue()` : returns pressed key (`-1` if none, `0`-`9`, `10` for `＊`, `11` for `#`).
- `getJoystickValueX()` : returns joystick horizontal axis in `[-100, 100]`.
- `getJoystickValueY()` : returns joystick vertical axis in `[-100, 100]`.
- `hasNextEvent()` : returns `True` if queued events exist, `False` otherwise.
- `getNextEvent()` : returns the next event string (`"device=...:name=...:value=..."`).
- `resetEventQueue()` : clears event queue.

### Sound and Audio:
- `playTone(freq, duration)` : plays a tone at frequency `freq` (Hz) for `duration` (ms).
- `playChord2(f1, f2, duration)` : plays a 2-frequency chord simultaneously.
- `playToneOn(freq)` : starts continuous tone generation.
- `playToneOff()` : stops continuous tone generation.
- `playAudioFile(filename)` : plays a WAV file from the `audiofiles` directory.
- `loopAudioFile(filename)` : plays a WAV file in loop.
- `stopAudioPlayer()` : stops audio player.

### Timing:
- `waitFor(ms)` : pauses execution for `ms` milliseconds.
- `systemTime()` : returns system timestamp in milliseconds.

<br/>![](../img/sep.png?style=center+noborder)

<a name=graph2d></a>
## 2D Graphics Module (`codelab.moduleGraphics.graph2D`)

Color constants:
- `COLOR_WHITE` (0), `COLOR_BLACK` (1), `COLOR_RED` (2), `COLOR_GREEN` (3), `COLOR_BLUE` (4), `COLOR_CYAN` (5), `COLOR_YELLOW` (6), `COLOR_MAGENTA` (7), `COLOR_ERROR` (8).

Functions:
- `clearScreen()` : clears graphics screen.
- `setColor(color)` : sets stroke color.
- `setWidth(width)` : sets stroke line width in pixels.
- `drawPoint(x, y)` : draws a pixel at `(x, y)`.
- `drawLine(x1, y1, x2, y2)` : draws line segment from `(x1, y1)` to `(x2, y2)`.
- `drawCircle(x, y, r)` : draws circle of radius `r` centered at `(x, y)`.

Logo Turtle:
- `turtleShow()` / `turtleHide()` : shows or hides graphic turtle.
- `turtleReset()` : centers turtle at origin.
- `turtleGoto(x, y, heading)` : moves turtle to `(x, y)` with given orientation `heading` (degrees) without drawing.
- `turtleForward(distance)` : moves turtle forward in current heading.
- `turtleTurnRight(angle)` : turns turtle clockwise by `angle` degrees.
- `turtleTurnLeft(angle)` : turns turtle counter-clockwise by `angle` degrees.
- `turtlePenUp()` : lifts pen (movements do not draw).
- `turtlePenDown()` : lowers pen (movements draw a line).
- `getColor()` : reads color code under the turtle.
- `getBrightness()` : reads light brightness intensity under the turtle.

<br/>![](../img/sep.png?style=center+noborder)

<a name=robotlego></a>
## Mobile Robotics Module (`codelab.moduleRobotics.robotLego`)

Motor ports:
- `OUT_A`, `OUT_B`, `OUT_C`, `OUT_AB`, `OUT_AC`, `OUT_BC`, `OUT_ABC`

Sensor ports:
- `IN_1`, `IN_2`, `IN_3`, `IN_4`

Functions:
- `motorOn(port, power)` : activates motor(s) with power `[-100, 100]`.
- `motorOff(port)` : stops motor(s).
- `motorRotationCount(port)` : reads accumulated rotation angle in degrees.
- `resetMotorRotationCount(port)` : resets angle counter to zero.
- `getSensorValue(port)` : reads input sensor value.
- `getSensorArray(port)` : returns 8 values `[0, 100]` from line tracking sensor.
- `setBackground(name)` : loads racetrack or background image.
- `setRobotConfiguration(config)` : sets sensor configuration (e.g. `"COLOR/SONIC/SONIC/NONE"`).

<br/>![](../img/sep.png?style=center+noborder)

<a name=robotrovio></a>
## Rovio Robotics Module (`codelab.moduleRobotics.robotRovio`)

Motor constants:
- `MOTOR_L = 0`, `MOTOR_R = 1`, `MOTOR_C = 2`, `MOTOR_LR = 3`, `MOTOR_ALL = 4`

Functions:
- `motorOn(port, power)`
- `motorOff(port)`
- `setBackground(name)`
- `setRobotConfiguration(config)`

<br/><center>
![](../img/fin.png?style=center+noborder)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

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
# CodeLab API - CLIPS Language

> This document presents CLIPS language libraries and functions included in CodeLab (`includes_init/clips`).

## Loading Libraries

In CodeLab, CLIPS libraries can be imported at script start using `#requires` or the `batch*` command:

- `#requires codelab.utils` : utility I/O, audio, timing and peripheral functions
- `#requires codelab.moduleGraphics.graph2D` : 2D geometric and Logo turtle functions
- `#requires codelab.moduleRobotics.robotLego` : Lego NXT mobile robot simulation
- `#requires codelab.motormodule.NXTMotor` : NXT motor simulator module

<br/>![](../img/sep.png?style=center+noborder)

<a name=utils></a>
## Utilities Library (`codelab/utils.clp`)

### Peripherals and Events
- `(getNumpadValue)` : reads virtual numeric keypad (`-1` if idle, `0`-`9`, `10` for `＊`, `11` for `#`).
- `(getJoystickValueX)` : reads joystick X axis in `[-100, 100]`.
- `(getJoystickValueY)` : reads joystick Y axis in `[-100, 100]`.
- `(hasNextEvent)` : returns `1` if events are queued, `0` otherwise.
- `(getNextEvent)` : pops next event string.
- `(resetEventQueue)` : resets event queue.

### Sound and Audio
- `(playTone ?freq ?time)` : plays tone at frequency `?freq` (Hz) for `?time` (ms).
- `(playToneOn ?freq)` : starts continuous tone generation.
- `(playToneOff)` : stops tone generation.
- `(playAudioFile ?filename)` : plays a WAV audio file once.
- `(loopAudioFile ?filename)` : plays a WAV audio file continuously.
- `(stopAudioPlayer)` : stops audio player.

### Timing
- `(waitFor ?ms)` : pauses execution for `?ms` milliseconds.
- `(systemTime)` : returns current system timestamp in milliseconds.

<br/>![](../img/sep.png?style=center+noborder)

<a name=graph2d></a>
## 2D Graphics Module (`codelab/moduleGraphics/graph2D.clp`)

Color constants:
- `?*COLOR_WHITE*` (0), `?*COLOR_BLACK*` (1), `?*COLOR_RED*` (2), `?*COLOR_GREEN*` (3), `?*COLOR_BLUE*` (4), `?*COLOR_CYAN*` (5), `?*COLOR_YELLOW*` (6), `?*COLOR_MAGENTA*` (7), `?*COLOR_ERROR*` (8).

Geometric functions:
- `(clearScreen)` : clears canvas.
- `(setColor ?color)` : selects stroke color.
- `(setWidth ?width)` : sets stroke width.
- `(drawPoint ?x ?y)` : draws pixel at `(?x, ?y)`.
- `(drawLine ?x1 ?y1 ?x2 ?y2)` : draws line from `(?x1, ?y1)` to `(?x2, ?y2)`.
- `(drawCircle ?x ?y ?r)` : draws circle with radius `?r` at `(?x, ?y)`.

Logo Turtle functions:
- `(turtleShow)` / `(turtleHide)` : displays or hides turtle.
- `(turtleReset)` : centers turtle at origin.
- `(turtleForward ?distance)` : moves forward by `?distance` pixels.
- `(turtleTurnRight ?angle)` : turns right by `?angle` degrees.
- `(turtleTurnLeft ?angle)` : turns left by `?angle` degrees.
- `(turtleGoto ?x ?y ?theta)` : moves turtle to `(?x, ?y)` with heading `?theta`.
- `(turtlePenUp)` : lifts pen.
- `(turtlePenDown)` : lowers pen.
- `(getColor)` : reads color code under the turtle.
- `(getBrightness)` : reads light brightness under the turtle.

<br/>![](../img/sep.png?style=center+noborder)

<a name=robotlego></a>
## Mobile Robotics Module (`codelab/moduleRobotics/robotLego.clp`)

Ports and Colors:
- Motor ports : `?*OUT_A*`, `?*OUT_B*`, `?*OUT_C*`, `?*OUT_AB*`, `?*OUT_AC*`, `?*OUT_BC*`, `?*OUT_ABC*`
- Sensor ports : `?*IN_1*`, `?*IN_2*`, `?*IN_3*`, `?*IN_4*`
- Color codes : `?*COLOR_WHITE*` to `?*COLOR_MAGENTA*`, `?*COLOR_ERROR*`

Functions:
- `(motorOn ?port ?power)` : drives motor(s) with power `[-100, 100]`.
- `(motorOff ?port)` : stops motor(s).
- `(motorRotationCount ?port)` : returns motor rotation count in degrees.
- `(resetMotorRotationCount ?port)` : resets angle counter.
- `(getSensorValue ?port)` : reads sensor value.
- `(getSensorArray ?port)` : returns 8 line tracking values.
- `(setBackground ?name)` : sets background image or circuit.
- `(setRobotConfiguration ?config)` : sets sensor configuration (e.g. `"COLOR/SONIC/SONIC/NONE"`).

<br/><center>
![](../img/fin.png?style=center+noborder)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

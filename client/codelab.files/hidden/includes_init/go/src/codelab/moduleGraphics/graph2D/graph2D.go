// #####################################################################
// ## This file is part of the software CodeLab IDE & Simulators      ##
// ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
// #####################################################################

// DO NOT DELETE OR MODIFY THIS FILE !!

package Turtle

import "codelab"
import "fmt"

const CLEARSCREEN_CMD = "module=MODULEGRAPHICS&cmd=clearScreen"
const SETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=setColor&color=%d"
const SETWIDTH_CMD = "module=MODULEGRAPHICS&cmd=setWidth&width=%d"
const DRAWLINE_CMD = "module=MODULEGRAPHICS&cmd=drawLine&x1=%d&y1=%d&x2=%d&y2=%d"
const DRAWPOINT_CMD = "module=MODULEGRAPHICS&cmd=drawPoint&x=%d&y=%d"
const DRAWCIRCLE_CMD = "module=MODULEGRAPHICS&cmd=drawCircle&x=%d&y=%d&r=%d"

const TURTLEFORWARD_CMD = "module=MODULEGRAPHICS&cmd=turtleForward&distance=%d"
const TURTLETURNRIGHT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnRight&angle=%d"
const TURTLETURNLEFT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnLeft&angle=%d"
const TURTLEGOTO_CMD = "module=MODULEGRAPHICS&cmd=turtleGoto&x=%d&y=%d&theta=%d"
const TURTLERESET_CMD = "module=MODULEGRAPHICS&cmd=turtleReset"

const TURTLESHOW_CMD = "module=MODULEGRAPHICS&cmd=turtleShow"
const TURTLEHIDE_CMD = "module=MODULEGRAPHICS&cmd=turtleHide"
const TURTLERESET_CMD = "module=MODULEGRAPHICS&cmd=turtleReset"
const TURTLEPENUP_CMD = "module=MODULEGRAPHICS&cmd=turtlePenUp"
const TURTLEPENDOWN_CMD = "module=MODULEGRAPHICS&cmd=turtlePenDown"

const GETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=getColor"
const GETBRIGHTNESS_CMD = "module=MODULEGRAPHICS&cmd=getBrightness"

const (
	COLOR_WHITE = iota
	COLOR_BLACK
	COLOR_RED
	COLOR_GREEN
	COLOR_BLUE
	COLOR_CYAN
	COLOR_YELLOW
	COLOR_MAGENTA
	COLOR_ERROR
)

func ClearScreen() int {
	buffer := fmt.Sprintf(CLEARSCREEN_CMD)
	return codelab.RequestToi(buffer)
}

func SetColor(color int) int {
	buffer := fmt.Sprintf(SETCOLOR_CMD, color)
	return codelab.RequestToi(buffer)
}

func setWidth(width int) int {
	buffer := fmt.Sprintf(SETWIDTH_CMD, width)
	return codelab.RequestToi(buffer)
}

func DrawPoint(x int, y int) int {
	buffer := fmt.Sprintf(DRAWPOINT_CMD, x, y)
	return codelab.RequestToi(buffer)
}

func DrawLine(x1 int, y1 int, x2 int, y2 int) int {
	buffer := fmt.Sprintf(DRAWLINE_CMD, x1, y1, x2, y2)
	return codelab.RequestToi(buffer)
}

func DrawCircle(x int, y int, r int) int {
	buffer := fmt.Sprintf(DRAWCIRCLE_CMD, x, y, r)
	return codelab.RequestToi(buffer)
}

func TurtleForward(distance int) int {
	buffer := fmt.Sprintf(TURTLEFORWARD_CMD, distance)
	return codelab.RequestToi(buffer)
}

func TurtleTurnRight(angle int) int {
	buffer := fmt.Sprintf(TURTLETURNRIGHT_CMD, angle)
	return codelab.RequestToi(buffer)
}

func TurtleTurnLeft(angle int) int {
	buffer := fmt.Sprintf(TURTLETURNLEFT_CMD, angle)
	return codelab.RequestToi(buffer)
}

func TurtleGoto(x int, y int, theta int) int {
	buffer := fmt.Sprintf(TURTLEGOTO_CMD, x, y, theta)
	return codelab.RequestToi(buffer)
}

func TurtleShow() int {
	buffer := fmt.Sprintf(TURTLESHOW_CMD)
	return codelab.RequestToi(buffer)
}

func TurtleHide() int {
	buffer := fmt.Sprintf(TURTLEHIDE_CMD)
	return codelab.RequestToi(buffer)
}

func TurtleReset() int {
	buffer := fmt.Sprintf(TURTLERESET_CMD)
	return codelab.RequestToi(buffer)
}

func TurtlePenUp() int {
	buffer := fmt.Sprintf(TURTLEPENUP_CMD)
	return codelab.RequestToi(buffer)
}

func TurtlePenDown() int {
	buffer := fmt.Sprintf(TURTLEPENDOWN_CMD)
	return codelab.RequestToi(buffer)
}

func GetColor() int {
	buffer := fmt.Sprintf(GETCOLOR_CMD)
	return codelab.RequestToi(buffer)
}

func GetBrightness() int {
	buffer := fmt.Sprintf(GETBRIGHTNESS_CMD)
	return codelab.RequestToi(buffer)
}

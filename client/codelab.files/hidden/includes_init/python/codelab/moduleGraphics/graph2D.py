#####################################################################
## This file is part of the software CodeLab IDE & Simulators      ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################

## DO NOT DELETE OR MODIFY THIS FILE !!

from __init import *

CLEARSCREEN_CMD = 'module=MODULEGRAPHICS&cmd=clearScreen'
SETCOLOR_CMD = 'module=MODULEGRAPHICS&cmd=setColor&color=%d'
SETWIDTH_CMD = 'module=MODULEGRAPHICS&cmd=setWidth&width=%d'
DRAWLINE_CMD = 'module=MODULEGRAPHICS&cmd=drawLine&x1=%d&y1=%d&x2=%d&y2=%d'
DRAWPOINT_CMD = 'module=MODULEGRAPHICS&cmd=drawPoint&x=%d&y=%d'
DRAWCIRCLE_CMD = 'module=MODULEGRAPHICS&cmd=drawCircle&x=%d&y=%d&r=%d'

TURTLEFORWARD_CMD = 'module=MODULEGRAPHICS&cmd=turtleForward&distance=%d'
TURTLETURNRIGHT_CMD = 'module=MODULEGRAPHICS&cmd=turtleTurnRight&angle=%d'
TURTLETURNLEFT_CMD = 'module=MODULEGRAPHICS&cmd=turtleTurnLeft&angle=%d'
TURTLEGOTO_CMD = 'module=MODULEGRAPHICS&cmd=turtleGoto&x=%d&y=%d&theta=%d'
TURTLESHOW_CMD = 'module=MODULEGRAPHICS&cmd=turtleShow'
TURTLEHIDE_CMD = 'module=MODULEGRAPHICS&cmd=turtleHide'
TURTLERESET_CMD = 'module=MODULEGRAPHICS&cmd=turtleReset'
TURTLEPENUP_CMD = 'module=MODULEGRAPHICS&cmd=turtlePenUp'
TURTLEPENDOWN_CMD = 'module=MODULEGRAPHICS&cmd=turtlePenDown'

GETCOLOR_CMD = 'module=MODULEGRAPHICS&cmd=getColor'
GETBRIGHTNESS_CMD = 'module=MODULEGRAPHICS&cmd=getBrightness'

COLOR_WHITE = 0
COLOR_BLACK = 1
COLOR_RED = 2
COLOR_GREEN = 3
COLOR_BLUE = 4
COLOR_CYAN = 5
COLOR_YELLOW = 6
COLOR_MAGENTA = 7
COLOR_ERROR = 8

def clearScreen():
	return int(request(CLEARSCREEN_CMD))

def setColor(color):
	buffer = SETCOLOR_CMD % color
	return int(request(buffer))

def setWidth(width):
	buffer = SETWIDTH_CMD % width
	return int(request(buffer))

def drawPoint(x, y):
	buffer = DRAWPOINT_CMD % (x, y)
	return int(request(buffer))

def drawLine(x1, y1, x2, y2):
	buffer = DRAWLINE_CMD % (x1, y1, x2, y2)
	return int(request(buffer))

def drawCircle(x, y, r):
	buffer = DRAWCIRCLE_CMD % (x, y, r)
	return int(request(buffer))

def turtleForward(distance):
	buffer = TURTLEFORWARD_CMD % distance
	return int(request(buffer))

def turtleTurnRight(angle):
	buffer = TURTLETURNRIGHT_CMD % angle
	return int(request(buffer))

def turtleTurnLeft(angle):
	buffer = TURTLETURNLEFT_CMD % angle
	return int(request(buffer))

def turtleGoto(x, y, theta):
	buffer = TURTLEGOTO_CMD % (x, y, theta)
	return int(request(buffer))

def turtleShow():
	return int(request(TURTLESHOW_CMD))
	
def turtleHide():
	return int(request(TURTLEHIDE_CMD))
	
def turtleReset():
	return int(request(TURTLERESET_CMD))

def turtlePenUp():
	return int(request(TURTLEPENUP_CMD))
	
def turtlePenDown():
	return int(request(TURTLEPENDOWN_CMD))
	
def getColor():
	return int(request(GETCOLOR_CMD))

def getBrightness():
	return int(request(GETBRIGHTNESS_CMD))

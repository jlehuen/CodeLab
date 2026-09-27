#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################

## DO NOT DELETE OR MODIFY THIS FILE !!

from __init import *

SETBACKGROUND_CMD = 'module=MODULEROBOTICS&cmd=setBackground&name=%s'
SETROBOTCONFIG_CMD = 'module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s'

MOTORON_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=motorOn&port=%d&power=%d'
MOTOROFF_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=motorOff&port=%d'
GETARRAY_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=getSensorArray&port=%d&index=%d'
GETSENSOR_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=getSensorValue&port=%d'
GETROTATION_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=getMotorRotationCount&port=%d'
RESETROTATION_CMD = 'module=MODULEROBOTICS&robot=lego&cmd=resetMotorRotationCount&port=%d'

OUT_A = 0
OUT_B = 1
OUT_C = 2
OUT_AB = 3
OUT_AC = 4
OUT_BC = 5
OUT_ABC = 6

IN_1 = 0
IN_2 = 1
IN_3 = 2
IN_4 = 3

COLOR_WHITE = 0
COLOR_BLACK = 1
COLOR_RED = 2
COLOR_GREEN = 3
COLOR_BLUE = 4
COLOR_CYAN = 5
COLOR_YELLOW = 6
COLOR_MAGENTA = 7
COLOR_ERROR = 8

def motorOn(port, power):
	buffer = MOTORON_CMD % (port, power)
	return int(request(buffer))

def motorOff(port):
	buffer = MOTOROFF_CMD % port
	return int(request(buffer))

def getSensorValue(port):
	buffer = GETSENSOR_CMD % port
	return int(request(buffer))

def getSensorArray(port):
	liste = []
	for i in range(0, 8):
		buffer = GETARRAY_CMD % (port, i)
		liste.append(int(request(buffer)))
	return liste

def motorRotationCount(port):
	buffer = GETROTATION_CMD % port
	return int(request(buffer))

def resetMotorRotationCount(port):
	buffer = RESETROTATION_CMD % port
	return int(request(buffer))

def setBackground(name):
	buffer = SETBACKGROUND_CMD % name
	return int(request(buffer))

def setRobotConfiguration(name):
	buffer = SETROBOTCONFIG_CMD % name
	return int(request(buffer))

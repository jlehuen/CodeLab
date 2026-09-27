#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2022 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################

## DO NOT DELETE OR MODIFY THIS FILE !!

from __init import *

SETBACKGROUND_CMD = 'module=MODULEROBOTICS&cmd=setBackground&name=%s'
SETROBOTCONFIG_CMD = 'module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s'

MOTORON_CMD = 'module=MODULEROBOTICS&robot=rovio&cmd=motorOn&port=%d&power=%d'
MOTOROFF_CMD = 'module=MODULEROBOTICS&robot=rovio&cmd=motorOff&port=%d'

MOTOR_L = 0
MOTOR_R = 1
MOTOR_C = 2
MOTOR_LR = 3
MOTOR_ALL = 4

def motorOn(port, power):
	buffer = MOTORON_CMD % (port, power)
	return int(request(buffer))

def motorOff(port):
	buffer = MOTOROFF_CMD % port
	return int(request(buffer))

def setBackground(name):
	buffer = SETBACKGROUND_CMD % name
	return int(request(buffer))

def setRobotConfiguration(name):
	buffer = SETROBOTCONFIG_CMD % name
	return int(request(buffer))

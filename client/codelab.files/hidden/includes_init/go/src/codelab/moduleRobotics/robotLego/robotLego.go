// #####################################################################
// ##  This file is part of the software CodeLab IDE and Simulators   ##
// ##  Copyright © Jérôme Lehuen 2022 - Jerome.Lehuen@univ-lemans.fr  ##
// #####################################################################

// DO NOT DELETE OR MODIFY THIS FILE !!

package robotLego

import "codelab"
import "fmt"

const MOTORON_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOn&port=%d&power=%d"
const MOTOROFF_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOff&port=%d"
const GETARRAY_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorArray&port=%d&index=%d"
const GETSENSOR_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorValue&port=%d"
const GETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getMotorRotationCount&port=%d"
const RESETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=resetMotorRotationCount&port=%d"

const (
	IN_1 = iota
	IN_2
	IN_3
	IN_4
)

const (
	OUT_A = iota
	OUT_B
	OUT_C
	OUT_AB
	OUT_AC
	OUT_BC
	OUT_ABC
)

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

func MotorOn(port int, power int) int {
	return codelab.RequestToi(fmt.Sprintf(MOTORON_CMD, port, power))
}

func MotorOff(port int) int {
	return codelab.RequestToi(fmt.Sprintf(MOTOROFF_CMD, port))
}

func GetSensorValue(port int) int {
	return codelab.RequestToi(fmt.Sprintf(GETSENSOR_CMD, port))
}

func GetSensorArray(port int) [8]int {
	var array [8]int
	for i := 0; i < 8; i++ {
		array[i] = codelab.RequestToi(fmt.Sprintf(GETARRAY_CMD, port, i))
	}
	return array;
}

func MotorRotationCount(port int) int {
	return codelab.RequestToi(fmt.Sprintf(GETROTATION_CMD, port))
}

func ResetMotorRotationCount(port int) int {
	return codelab.RequestToi(fmt.Sprintf(RESETROTATION_CMD, port))
}

// --------------------------------------------------------------
// Contrôle du simulateur

const SETBACKGROUND_CMD = "module=MODULEROBOTICS&cmd=setBackground&name=%s"
const SETROBOTCONFIG_CMD = "module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s"

func SetBackground(name string) int {
	return codelab.RequestToi(fmt.Sprintf(SETBACKGROUND_CMD, name))
}

func SetRobotConfiguration(name string) int {
	return codelab.RequestToi(fmt.Sprintf(SETROBOTCONFIG_CMD, name))
}

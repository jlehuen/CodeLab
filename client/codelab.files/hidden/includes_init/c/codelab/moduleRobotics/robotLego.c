/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#ifndef LEGO_H
#define LEGO_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

const char* MOTORON_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOn&port=%d&power=%d";
const char* MOTOROFF_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOff&port=%d";
const char* GETARRAY_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorArray&port=%d&index=%d";
const char* GETSENSOR_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorValue&port=%d";
const char* GETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getMotorRotationCount&port=%d";
const char* RESETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=resetMotorRotationCount&port=%d";

enum inPort {
	IN_1,
	IN_2,
	IN_3,
	IN_4
};

enum outPort {
	OUT_A,
	OUT_B,
	OUT_C,
	OUT_AB,
	OUT_AC,
	OUT_BC,
	OUT_ABC
};

enum colorCode {
	COLOR_WHITE,
	COLOR_BLACK,
	COLOR_RED,
	COLOR_GREEN,
	COLOR_BLUE,
	COLOR_CYAN,
	COLOR_YELLOW,
	COLOR_MAGENTA,
	COLOR_ERROR
};

int motorOn(int port, int power) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), MOTORON_CMD, port, power);
	return atoi(request(buffer));
}

int motorOff(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), MOTOROFF_CMD, port);
	return atoi(request(buffer));
}

int getSensorValue(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), GETSENSOR_CMD, port);
	return atoi(request(buffer));
}

int getSensorArray(int port, int array[]) {
	char buffer[1024];
	for (int i = 0; i < 8; i++) {
		snprintf(buffer, sizeof(buffer), GETARRAY_CMD, port, i);
		array[i] = atoi(request(buffer));
	}
	return 0;
}

int motorRotationCount(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), GETROTATION_CMD, port);
	return atoi(request(buffer));
}

int resetMotorRotationCount(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), RESETROTATION_CMD, port);
	return atoi(request(buffer));
}

// --------------------------------------------------------------
// Contrôle du simulateur

const char* SETBACKGROUND_CMD = "module=MODULEROBOTICS&cmd=setBackground&name=%s";
const char* SETROBOTCONFIG_CMD = "module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s";

const char* SIMUSTART_CMD = "module=MODULEROBOTICS&cmd=simu_start";
const char* SIMUSTOP_CMD = "module=MODULEROBOTICS&cmd=simu_stop";

int setBackground(const char* name) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), SETBACKGROUND_CMD, name);
	return atoi(request(buffer));
}

int setRobotConfiguration(const char* name) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), SETROBOTCONFIG_CMD, name);
	return atoi(request(buffer));
}

int simu_start() {
	return atoi(request(SIMUSTART_CMD));
}

int simu_stop() {
	return atoi(request(SIMUSTOP_CMD));
}

#endif

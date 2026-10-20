/*
#####################################################################
## This file is part of the software CodeLab IDE & Simulators      ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

const int OUT_A = 0;
const int OUT_B = 1;
const int OUT_C = 2;

const char* MOTORON_CMD = "module=MOTORMODULE&cmd=motorOn&port=%d&power=%d";
const char* MOTOROFF_CMD = "module=MOTORMODULE&cmd=motorOff&port=%d";
const char* MOTORROTATIONCOUNT_CMD = "module=MOTORMODULE&cmd=motorRotationCount&port=%d";
const char* RESETMOTORROTATIONCOUNT_CMD = "module=MOTORMODULE&cmd=resetMotorRotationCount&port=%d";

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

int motorRotationCount(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), MOTORROTATIONCOUNT_CMD, port);
	return atoi(request(buffer));
}

int resetMotorRotationCount(int port) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), RESETMOTORROTATIONCOUNT_CMD, port);
	return atoi(request(buffer));
}



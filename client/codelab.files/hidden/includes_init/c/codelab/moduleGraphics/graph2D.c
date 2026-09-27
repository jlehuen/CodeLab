/*
#####################################################################
## This file is part of the software CodeLab IDE & Simulators      ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#ifndef GRAPH2D_H
#define GRAPH2D_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

const char* CLEARSCREEN_CMD = "module=MODULEGRAPHICS&cmd=clearScreen";
const char* SETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=setColor&color=%d";
const char* SETWIDTH_CMD = "module=MODULEGRAPHICS&cmd=setWidth&width=%d";
const char* DRAWLINE_CMD = "module=MODULEGRAPHICS&cmd=drawLine&x1=%d&y1=%d&x2=%d&y2=%d";
const char* DRAWPOINT_CMD = "module=MODULEGRAPHICS&cmd=drawPoint&x=%d&y=%d";
const char* DRAWCIRCLE_CMD = "module=MODULEGRAPHICS&cmd=drawCircle&x=%d&y=%d&r=%d";

const char* TURTLEFORWARD_CMD = "module=MODULEGRAPHICS&cmd=turtleForward&distance=%d";
const char* TURTLETURNRIGHT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnRight&angle=%d";
const char* TURTLETURNLEFT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnLeft&angle=%d";
const char* TURTLEGOTO_CMD = "module=MODULEGRAPHICS&cmd=turtleGoto&x=%d&y=%d&theta=%d";
const char* TURTLESHOW_CMD = "module=MODULEGRAPHICS&cmd=turtleShow";
const char* TURTLEHIDE_CMD = "module=MODULEGRAPHICS&cmd=turtleHide";
const char* TURTLERESET_CMD = "module=MODULEGRAPHICS&cmd=turtleReset";
const char* TURTLEPENUP_CMD = "module=MODULEGRAPHICS&cmd=turtlePenUp";
const char* TURTLEPENDOWN_CMD = "module=MODULEGRAPHICS&cmd=turtlePenDown";

const char* GETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=getColor";
const char* GETBRIGHTNESS_CMD = "module=MODULEGRAPHICS&cmd=getBrightness";

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

int clearScreen() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), CLEARSCREEN_CMD);
	return atoi(request(buffer));
}

int setColor(int color) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), SETCOLOR_CMD, color);
	return atoi(request(buffer));
}

int setWidth(int width) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), SETWIDTH_CMD, width);
	return atoi(request(buffer));
}

int drawPoint(int x, int y) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), DRAWPOINT_CMD, x, y);
	return atoi(request(buffer));
}

int drawLine(int x1, int y1, int x2, int y2) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), DRAWLINE_CMD, x1, y1, x2, y2);
	return atoi(request(buffer));
}

int drawCircle(int x, int y, int r) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), DRAWCIRCLE_CMD, x, y, r);
	return atoi(request(buffer));
}

int turtleForward(int distance) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLEFORWARD_CMD, distance);
	return atoi(request(buffer));
}

int turtleTurnRight(int angle) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLETURNRIGHT_CMD, angle);
	return atoi(request(buffer));
}

int turtleTurnLeft(int angle) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLETURNLEFT_CMD, angle);
	return atoi(request(buffer));
}

int turtleGoto(int x, int y, int theta) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLEGOTO_CMD, x, y, theta);
	return atoi(request(buffer));
}

int turtleHide() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLEHIDE_CMD);
	return atoi(request(buffer));
}

int turtleShow() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLESHOW_CMD);
	return atoi(request(buffer));
}

int turtleReset() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLERESET_CMD);
	return atoi(request(buffer));
}

int turtlePenUp() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLEPENUP_CMD);
	return atoi(request(buffer));
}

int turtlePenDown() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), TURTLEPENDOWN_CMD);
	return atoi(request(buffer));
}

int getColor() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), GETCOLOR_CMD);
	return atoi(request(buffer));
}

int getBrightness() {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), GETBRIGHTNESS_CMD);
	return atoi(request(buffer));
}

#endif

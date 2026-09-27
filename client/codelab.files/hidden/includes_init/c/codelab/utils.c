/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#ifndef INOUT_H
#define INOUT_H

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <unistd.h>
#include <time.h>
#include <math.h>

#ifdef _WIN32
#include <windows.h>
#include <sys/timeb.h>
#include <limits.h>
#endif

#define string char*

// --------------------------------------------------------------
// Pour désactiver le buffering de stdout

void buffering_off() {
	setvbuf(stdout, NULL, _IONBF, 0);
}

// --------------------------------------------------------------
// Fonctions d'affichage

void printFormat(const char* format, ...) {
	va_list ptr;
	va_start(ptr, format);
	vprintf(format, ptr);
	va_end(ptr);
	fflush(stdout);
}

void printStr(char* chaine) {
	printf("%s", chaine);
	fflush(stdout);
}

void printStrLn(char* chaine) {
	printf("%s\n", chaine);
	fflush(stdout);
}

void printInt(int entier) {
	printf("%d", entier);
	fflush(stdout);
}

void printIntLn(int entier) {
	printf("%d\n", entier);
	fflush(stdout);
}

void printLong(long entier) {
	printf("%ld", entier);
	fflush(stdout);
}

void printLongLn(long entier) {
	printf("%ld\n", entier);
	fflush(stdout);
}

void printFloat(float flottant) {
	printf("%f", flottant);
	fflush(stdout);
}

void printFloatLn(float flottant) {
	printf("%f\n", flottant);
	fflush(stdout);
}

void newline() {
	printStrLn("");
}

// --------------------------------------------------------------
// Fonctions de saisie

char* inputStr(char* output) {
	printf("%s", output);
	fflush(stdout);
	char chaine[256];
	// Cette solution conserve le \n à la fin
	// fgets(chaine, sizeof(chaine), stdin);
	scanf("%255[^\n]", &chaine);
	return strdup(chaine);
}

int inputInt(char* output) {
	printf("%s", output);
	fflush(stdout);
	int entier;
	scanf("%d", &entier);
	return entier;
}

float inputFloat(char* output) {
	printf("%s", output);
	fflush(stdout);
	float ieee;
	scanf("%f", &ieee);
	return ieee;
}

void waitKeyPressed() {
	puts("\nPress any key\n");
	getchar();
}

// --------------------------------------------------------------
// Numpad et Joystick

const char* GETNUMPAD_CMD = "module=INOUT&cmd=getNumpadValue";
const char* GETJOYSTICKX_CMD = "module=INOUT&cmd=getJoystickValueX";
const char* GETJOYSTICKY_CMD = "module=INOUT&cmd=getJoystickValueY";

int getNumpadValue() {
	return atoi(request(GETNUMPAD_CMD));
}

int getJoystickValueX() {
	return atoi(request(GETJOYSTICKX_CMD));
}

int getJoystickValueY() {
	return atoi(request(GETJOYSTICKY_CMD));
}

// --------------------------------------------------------------
// Gestionnaire d'évènements

const char* HASNEXTEVENT_CMD = "module=INOUT&cmd=hasNextEvent";
const char* GETNEXTEVENT_CMD = "module=INOUT&cmd=getNextEvent";
const char* RESETQUEUE_CMD = "module=INOUT&cmd=resetEventQueue";

int hasNextEvent() {
	return atoi(request(HASNEXTEVENT_CMD));
}

char* getNextEvent() {
	return request(GETNEXTEVENT_CMD);
}

int resetEventQueue() {
	return atoi(request(RESETQUEUE_CMD));
}


// --------------------------------------------------------------
// Bibliothèque sonore

const char* PLAYTONE_CMD = "module=AUDIO&cmd=playTone&freq=%d&time=%d&ampl=%d";
const char* PLAYTONEON_CMD = "module=AUDIO&cmd=playToneOn&freq=%d&ampl=%d";
const char* PLAYTONEOFF_CMD = "module=AUDIO&cmd=playToneOff";
const char* PLAYAUDIOFILE_CMD = "module=AUDIO&cmd=playAudioFile&filename=%s";
const char* LOOPAUDIOFILE_CMD = "module=AUDIO&cmd=loopAudioFile&filename=%s";
const char* STOPAUDIOPLAYER_CMD = "module=AUDIO&cmd=stopAudioPlayer";

const int ampl = 25; // Amplitude dans [0, 100]

int playTone(int freq, int time) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), PLAYTONE_CMD, freq, time, ampl);
	return atoi(request(buffer));
}

int playToneOn(int freq) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), PLAYTONEON_CMD, freq, ampl);
	return atoi(request(buffer));
}

int playToneOff() {
	return atoi(request(PLAYTONEOFF_CMD));
}

int playAudioFile(char* filename) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), PLAYAUDIOFILE_CMD, filename);
	return atoi(request(buffer));
}

int loopAudioFile(char* filename) {
	char buffer[1024];
	snprintf(buffer, sizeof(buffer), LOOPAUDIOFILE_CMD, filename);
	return atoi(request(buffer));
}

int stopAudioPlayer() {
	return atoi(request(STOPAUDIOPLAYER_CMD));
}

// --------------------------------------------------------------
// Temporisation en millisecondes

#ifdef _WIN32
void waitFor(int ms) {
	Sleep(ms);
}
#else
void waitFor(int ms) {
	usleep(ms * 1000);
}
#endif

// --------------------------------------------------------------
// Temps système Epoch

#ifdef _WIN32
long systemTime() {
	struct timeb tmb;
	ftime(&tmb);
	// Le modulo est pour éviter le dépassement de capacité
	// lors de la conversion des uint64_t vers les long
	long MAX = LONG_MAX / 1000;
	return tmb.time % MAX * 1000 + tmb.millitm;
}
#else
// #define CLOCK_REALTIME 0
long systemTime() {
	struct timespec ts;
	clock_gettime(CLOCK_REALTIME, &ts);
	return ts.tv_sec * 1000 + lround(ts.tv_nsec / 1.0e6);
}
#endif

// https://stackoverflow.com/questions/5167269/clock-gettime-alternative-in-mac-os-x

// --------------------------------------------------------------
// Nombre entier aléatoire

void initRandom() {
	srand(time(NULL));
}

int randomInt(int max) {
	return rand() % max;
}

#endif

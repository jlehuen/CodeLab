/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

#include <winsock2.h>
#include <windows.h>
#include <stdio.h>

#define BUFSIZE 1024
#define DELAY 5 // 5ms pour laisser le temps au serveur de ré-écouter

#pragma comment(lib, "ws2_32.lib") // Winsock library

int _sock_open();
int _sock_send(int sockfd, char* request, char* result);
int _error_exit(const char* msg, int errcode);

// Récupération du port du serveur local

char* INTERNAL_PORT = NULL;

char* get_port() {
	if (INTERNAL_PORT == NULL)
		INTERNAL_PORT = getenv("INTERNAL_PORT");
	return INTERNAL_PORT;
}

// The static array avoid memory management
// The array fill BUFSIZE bytes for the entire application
// https://stackoverflow.com/questions/11656532/returning-an-array-using-c

char* request(const char* msg) {
	char buffer[BUFSIZE];
	char result[BUFSIZE];
	snprintf(buffer, sizeof(buffer), "%s", msg);
	int sockfd = _sock_open();
	_sock_send(sockfd, buffer, result);
	closesocket(sockfd);
	WSACleanup();
	Sleep(DELAY);
	return strdup(result);
}

int _sock_open() {
	WSADATA wsa;
	SOCKET sockfd;

	if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0)
		_error_exit("Error %d in _sock_open", WSAGetLastError());

	if ((sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == INVALID_SOCKET)
		_error_exit("Error %d in _sock_open", WSAGetLastError());

	return sockfd;
}

int _sock_send(int sockfd, char* request, char* result) {
	struct sockaddr_in server;
	server.sin_addr.s_addr = inet_addr("127.0.0.1");
	server.sin_family = AF_INET;
	server.sin_port = htons(atoi(get_port()));

	if (connect(sockfd, (struct sockaddr*) &server, sizeof(server)) < 0)
		_error_exit("Error %d in _sock_send", WSAGetLastError());

	if (send(sockfd, request, strlen(request), 0) < 0)
		_error_exit("Error %d in _sock_send", WSAGetLastError());

	char buffer[BUFSIZE];
	memset(buffer, 0, BUFSIZE);
	int bytesRead = recv(sockfd, buffer, BUFSIZE, 0);
	//buffer[bytesRead] = 0x0;
	strcpy(result, buffer);
	return 1;
}

int _error_exit(const char* msg, int errcode) {
	fprintf(stderr, msg, errcode);
	exit(EXIT_FAILURE);
}

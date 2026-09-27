/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

// https://www.geeksforgeeks.org/udp-server-client-implementation-c/

#include <sys/socket.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#define BUFSIZE 1024
#define DELAY 5000 // 5ms pour laisser le temps au serveur de ré-écouter

int _sock_open(const char* port);
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
	int sockfd = _sock_open(get_port());
	_sock_send(sockfd, buffer, result);
	//printf("[%s]\n", result);
	//fflush(stdout);
	close(sockfd);
	usleep(DELAY);
	return strdup(result);
}

int _sock_open(const char* port) {
	struct addrinfo hints, *data;
	int sockfd, code;
	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_DGRAM;
	getaddrinfo("localhost", port, &hints, &data);
	sockfd = socket(data->ai_family, data->ai_socktype, data->ai_protocol);
	if ((code = connect(sockfd, data->ai_addr, data->ai_addrlen)) < 0)
		_error_exit("Error %d in _sock_open", code);
	return sockfd;
}

int _sock_send(int sockfd, char* request, char* result) {
	send(sockfd, request, strlen(request), 0);
	char buffer[BUFSIZE];
	memset(buffer, 0, BUFSIZE);
	int bytesRead = recv(sockfd, buffer, BUFSIZE, 0);
	//buffer[bytesRead] = 0;
	strcpy(result, buffer);
	return 1;
}

int _error_exit(const char* msg, int errcode) {
	fprintf(stderr, msg, errcode);
	exit(EXIT_FAILURE);
}

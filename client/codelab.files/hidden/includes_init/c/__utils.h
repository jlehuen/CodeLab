/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

void strreplace(char* src, char* str, char* rep) {
	char* p = strstr(src, str);
	do {
		if (p) {
			char buf[1024];
			memset(buf, '\0', sizeof buf);
			if (src == p) {
				strcpy(buf, rep);
				strcat(buf, p+strlen(str));
			} else {
				strncpy(buf, src, strlen(src)-strlen(p));
				strcat(buf, rep);
				strcat(buf, p+strlen(str));
			}
			memset(src, '\0', strlen(src));
			strcpy(src, buf);
		}

	} while (p && (p = strstr(src, str)));
}
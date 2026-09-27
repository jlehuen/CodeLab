#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

// https://stackoverflow.com/questions/230062/whats-the-best-way-to-check-if-a-file-exists-in-c
// https://jogamp.org/bugzilla/show_bug.cgi?id=1317#c21

void writeLine(char* filename, char* line) {
	FILE* fp = fopen(filename, "a");
	fprintf(fp, "%s\n", line);
	fclose(fp);
}

int main(int argc, const char* argv[]) {

	int BUFSIZE = 1024;
	char COMMAND[BUFSIZE];
	char ARGV[BUFSIZE];
	ARGV[0] = '\0';

	// Vérification de la ligne de commande
	for (int i = 1; i < argc; i++) {
		if (!strcmp(argv[i], "--dir")) {
			// Modification du dossier HOME
			setenv("HOME", argv[i+1], 1);
		}
		if (!strcmp(argv[i], "--kill")) {
			// Suppression des autres instances
			system("pgrep -fl codelab | cut -d " " -f1 | xargs kill");
		}
		// Reconstruction de la ligne des paramètres
		sprintf(ARGV + strlen(ARGV), " %s", argv[i]);
	}

	// Déterminer le dossier courant
	char path_save[BUFSIZE];
	char BASE[BUFSIZE];
	char *p;
	if(!(p = strrchr(argv[0], '/')))
		getcwd(BASE, sizeof(BASE));
	else {
		*p = '\0';
		getcwd(path_save, sizeof(path_save));
		chdir(argv[0]);
		getcwd(BASE, sizeof(BASE));
		chdir(path_save);
	}
	setenv("BASE", BASE, 1);
	
	// Transmettre le nom du launcher
	char LAUNCHER[BUFSIZE];
	strcpy(LAUNCHER, BASE);
	strcat(LAUNCHER, "/codelab");
	setenv("LAUNCHER", LAUNCHER, 1);

	// Le chemin vers le JDK
	char JAVA_HOME[BUFSIZE];
	strcpy(JAVA_HOME, BASE);
	strcat(JAVA_HOME, "/.hidden/JDK-17.0.8.1+1");
	setenv("JAVA_HOME", JAVA_HOME, 1);

	// Le chemin vers le jar de CodeLab
	char CODELAB[BUFSIZE];
	strcpy(CODELAB, BASE);
	strcat(CODELAB, "/.hidden/codelab/codelab.jar");

	// Vérification d'une nouvelle version
	char NEWCODELAB[BUFSIZE];
	strcpy(NEWCODELAB, BASE);
	strcat(NEWCODELAB, "/.hidden/codelab/codelab-new.jar");
	if (access(NEWCODELAB, F_OK) != -1) {
		remove(CODELAB);
		rename(NEWCODELAB, CODELAB);
    }

	// Exécuter CodeLab
	const char DLIBPATH[] = ".hidden/codelab/natives";
	const char BUFFER[] = "cd %s ; %s/bin/java -Xmx1024m --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Djava.library.path=\"%s\" -Dfile.encoding=UTF-8 -jar %s --log %s &";
	snprintf(COMMAND, sizeof(COMMAND), BUFFER, BASE, JAVA_HOME, DLIBPATH, CODELAB, ARGV);
	//writeLine("launcher.log", COMMAND);
	//puts(COMMAND);
	system(COMMAND);
    return 0;
}

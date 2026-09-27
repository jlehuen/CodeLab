#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

int file_exists(const char* filename) {
	FILE* file = fopen(filename, "r");
	if (file == NULL) return 0;
	fclose(file);
	return 1;
}

int main(int argc, const char* argv[]) {

	int BUFSIZE = 4096;
	// Pour récupérer la ligne de commande
	char ARGV[BUFSIZE];
	ARGV[0] = '\0';

	// Déterminer le dossier courant
	char path_save[BUFSIZE];
	char BASE[BUFSIZE];
	char *p;
	if(!(p = strrchr(argv[0], '\\')))
		getcwd(BASE, sizeof(BASE));
	else {
		*p = '\0';
		getcwd(path_save, sizeof(path_save));
		chdir(argv[0]);
		getcwd(BASE, sizeof(BASE));
		chdir(path_save);
	}

	// Déterminer le dossier utilisateur de manière robuste
	char HOME[BUFSIZE];
	const char* userProfile = getenv("USERPROFILE");
	if (userProfile && strlen(userProfile) > 0) {
		snprintf(HOME, BUFSIZE, "%s", userProfile);
	} else {
		snprintf(HOME, BUFSIZE, "%s%s", getenv("HOMEDRIVE"), getenv("HOMEPATH"));
	}

	// Vérification de la ligne de commande
	for (int i = 1; i < argc; i++) {
		if (!strcmp(argv[i], "--dir") && (i + 1 < argc)) {
			// Modification du dossier HOME
			snprintf(HOME, BUFSIZE, "%s", argv[i+1]);
		}
		// Reconstruction de la ligne des paramètres
		snprintf(ARGV + strlen(ARGV), BUFSIZE - strlen(ARGV), " %s", argv[i]);
	}
	
	// Reconstruire le nom du launcher
	char LAUNCHER[BUFSIZE];
	snprintf(LAUNCHER, BUFSIZE, "%s\\codelab.exe", BASE);

	// Chemin vers le JDK
	char JAVA_HOME[BUFSIZE];
	snprintf(JAVA_HOME, BUFSIZE, "%s\\.hidden\\JDK-17.0.8.1+1", BASE);
	
	// Variables d'environnement
	_putenv_s("BASE", BASE);
	_putenv_s("HOME", HOME);
	_putenv_s("LAUNCHER", LAUNCHER);
	_putenv_s("JAVA_HOME", JAVA_HOME);

	// Chemin vers le jar de CodeLab
	char CODELAB[BUFSIZE];
	snprintf(CODELAB, BUFSIZE, "%s\\.hidden\\codelab\\codelab.jar", BASE);

	// Vérification d'une nouvelle version
	char NEWCODELAB[BUFSIZE];
	snprintf(NEWCODELAB, BUFSIZE, "%s\\.hidden\\codelab\\codelab-new.jar", BASE);
	if (file_exists(NEWCODELAB)) {
		// Attendre que CodeLab se termine
		while(remove(CODELAB) != 0) sleep(1);
		rename(NEWCODELAB, CODELAB);
	}

	// Reconstruire la ligne de commande avec protection des espaces (guillemets stricts)
	const char DLIBPATH[] = ".hidden\\codelab\\natives";
	const char BUFFER[] = "ATTRIB +H +S \"%s\\codelab.files\\.hidden\" & ATTRIB +H +S \"%s\\.hidden\" & START \"\" \"%s\\bin\\javaw.exe\" -Xmx1024m -Djava.library.path=\"%s\" -Dfile.encoding=UTF-8 --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -jar \"%s\" --log %s";
	char COMMAND[BUFSIZE];
	snprintf(COMMAND, sizeof(COMMAND), BUFFER, HOME, BASE, JAVA_HOME, DLIBPATH, CODELAB, ARGV);

	// Exécuter la ligne de commande
	system(COMMAND);
	return 0;
}

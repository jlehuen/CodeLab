package codelab;

import java.util.Arrays;

import codelab.utils.Utils;

/**
*	Classe du gestionnaire d'exceptions
*	@author Jérôme Lehuen
*	@version 18/06/25
*/

public class ExceptionManager {

	public static void process(Exception exception) {

		// Vers le fichier de log local
		if (exception != null) {
			CodeLab.logger("Exception stacktrace:\n");
			exception.printStackTrace();
		}

		// Terminé si pas de CodeLab
		CodeLab codelab = CodeLab.INSTANCE;
		if (codelab == null) return;

		// Terminé si pas de rapport
		if (!CodeLab.REPORT_EXCEPTIONS) return;

		// Génération du rapport à envoyer
		StringBuilder sbuilder = new StringBuilder();
		sbuilder.append(String.format("----------------------------------------------------------------------------------------\n"));
		sbuilder.append(String.format("%s --- CODELAB EXCEPTION REPORT\n", Utils.getDate()));
		sbuilder.append(String.format("----------------------------------------------------------------------------------------\n\n"));
		sbuilder.append(String.format("\tCodeLab %s build %s running on %s\n", CodeLab.VERSION, CodeLab.BUILD, CodeLab.SYSTEM));
		sbuilder.append(String.format("\tCodeLab folder : %s\n\n", CodeLab.BASE));
		sbuilder.append(String.format("\tHost IP address : %s\n", CodeLab.IP_ADDR));
		sbuilder.append(String.format("\tHost name : %s\n", CodeLab.HOST_NAME));
		sbuilder.append(String.format("\tHome directory : %s\n", CodeLab.HOME));
		sbuilder.append(String.format("\tUser directory : %s\n", CodeLab.CODELAB_FILES));
		sbuilder.append(String.format("\tJava Home : %s\n", CodeLab.JAVA_HOME));
		sbuilder.append(String.format("\tLauncher : %s\n", CodeLab.LAUNCHER));
		sbuilder.append(String.format("\tArguments : %s\n", Arrays.toString(CodeLab.ARGV)));
		sbuilder.append(String.format("\tConnected mode : %s\n", Boolean.toString(CodeLab.CONNECTED_MODE)));
		sbuilder.append(String.format("\tServer host : %s\n", CodeLab.PROP("SERVER_HOST")));
		sbuilder.append(String.format("\tServer port : %d\n", PropertyBase.getIntegerProperty("SERVER_PORT")));
		sbuilder.append("\n");
		sbuilder.append(String.format("----------------------------------------------------------------------------------------\n"));
		sbuilder.append(String.format("Exception details:\n"));
		sbuilder.append(String.format("----------------------------------------------------------------------------------------\n"));
		
		if (exception != null) {
			sbuilder.append(String.format("Exception : %s\n", exception.getClass().getName()));
			sbuilder.append(String.format("Message : %s\n", exception.getMessage()));
			sbuilder.append(String.format("Stacktrace :\n"));
			for (StackTraceElement element : exception.getStackTrace()) {
				sbuilder.append(String.format("\t%s\n", element.toString()));
			}
		} else {
			sbuilder.append("No exception provided.\n");
		}

		sbuilder.append(String.format("----------------------------------------------------------------------------------------\n"));
		sbuilder.append(String.format("End of report.\n"));
		
		// Envoi du rapport
		codelab.getClient().exceptionReport(sbuilder.toString());
	}
}

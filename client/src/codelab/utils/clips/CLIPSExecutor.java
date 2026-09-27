package codelab.utils.clips;

import codelab.ExceptionManager;
import jess.JessException;
import jess.Rete;

/**
*	Classe de l'exécuteur de programmes CLIPS
*	@author Jérôme Lehuen
*	@version 28/11/20
*/

public class CLIPSExecutor {

	private Rete moteur = new Rete();

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CLIPSExecutor(String base, String filename) {
		try {
			moteur.store("BASE", base);
			batch(base + "/library/includes/clips/init.clp");
			batch(filename);
			moteur.reset();
			moteur.run();
		}
		catch (JessException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void batch(String path) {
		try {
			path = path.replaceAll("\\\\", "/");
			moteur.executeCommand("(batch \"" + path + "\")");
		}
		catch (JessException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthode statique main
	///////////////////////////////////////////////////

	public static void main(String[] argv) {
		new CLIPSExecutor(argv[0], argv[1]);
	}
}

package codelab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codelab.modules.editeur.ModuleEditor;
import jess.JessException;
import jess.RU;
import jess.Rete;
import jess.Value;
import jess.ValueVector;

/**
*	Classe du moteur d'inférences
*	@author Jérôme Lehuen
*	@version 23/01/24
*/

public class CodeLabRete extends Rete {

	private CodeLab codelab;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CodeLabRete(CodeLab codelab) {
		this.codelab = codelab;
		System.out.println("   Initializing inference engine...");
		// Définition des variables
		store("BASE", CodeLab.BASE);
		store("LANG", CodeLab.LANG);
		store("CODELAB", codelab);
		// Chargement des règles
		batch("common.clp");
		batch("errors_c.clp");
		batch("errors_go.clp");
		batch("errors_java.clp");
		batch("errors_clips.clp");
		batch("errors_blocs.clp");
		batch("errors_python.clp");
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void batch(String filename) {
		System.out.format("      Loading %s... ", filename);
		String path = CodeLab.RESSOURCES + "/codelab/rules/" + filename;
		try {
			path = path.replaceAll("\\\\", "/");
			executeCommand("(batch \"" + path + "\")");
			reset();
			System.out.println("DONE");
		}
		catch (JessException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void assertFact(String fact) {
		try {
			executeCommand("(assert " + fact + ")");
		}
		catch (JessException e) {
			ExceptionManager.process(e);
		}
	}

	public void reset() {
		try {
			super.reset();
		}
		catch (JessException e) {
			ExceptionManager.process(e);
		}
	}

	public int run() {
		try {
			if (CodeLab.PRINT_FACTS) {
				CodeLab.logger("Inference motor fact base:");
				executeCommand("(facts)");
			}
			return super.run();
		}
		catch (JessException e) {
			ExceptionManager.process(e);
			return -1;
		}
	}

	public void checkErrors(File file, String relation) {
		if (!CodeLab.RETE_FLAG) return;
		if (file == null) return;
		if (file.length() == 0) return;

		// Ajouter un fait précisant le langage
		ModuleEditor editor = codelab.getEditor();
		String language = editor.getCurrentLanguage().toString();
		assertFact(String.format("(lang %s)", language));

		String line, fact;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				line = line.replace("\"", ""); // Remove quotes
				fact = String.format("(%s \"%s\")", relation, line);
				assertFact(fact);
			}
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
		run(); // C'est parti !
	}

	///////////////////////////////////////////////////
	// Méthodes statiques invoquées dans common.clp
	// Attention: ne doivent pas être obfusquées !!
	///////////////////////////////////////////////////

	public static boolean matches(String str, String regex) {
		return str.matches(regex);
	}

	public static Value groups(String str, String regex) {
		try {
			ValueVector vector = new ValueVector();
			Matcher matcher = Pattern.compile(regex).matcher(str);
			if (matcher.find()) {
				for (int i = 1; i <= matcher.groupCount(); i++) {
					String group = matcher.group(i);
					vector.add(new Value(group, RU.STRING));
				}
			}
			return new Value(vector, RU.LIST);
		}
		catch (JessException e) {
			ExceptionManager.process(e);
			return null;
		}
	}
}

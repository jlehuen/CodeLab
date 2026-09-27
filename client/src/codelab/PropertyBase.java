package codelab;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyResourceBundle;

import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe de la base de propriétés
*	@author Jérôme Lehuen
*	@version 19/01/24
*/

public class PropertyBase {

	public static PropertyResourceBundle WEBPROPERTIES = null;
	public static PropertyResourceBundle PROPERTIES = null;
	public static PropertyResourceBundle USERCONF = null;

	///////////////////////////////////////////////////
	// Pour charger la base
	///////////////////////////////////////////////////

	public static boolean load() {

		File userprop = new File(CodeLab.USER_PROP_FILE);
		if (!userprop.exists()) {
			// Le fichier user.properties est manquant => on le remplace
			MyFileUtils.copy(CodeLab.USER_PROP_BAK, CodeLab.USER_PROP_FILE);
		}
		PROPERTIES = Utils.readPropertyResourceBundle(CodeLab.PROPERTY_FILE);
		USERCONF = Utils.readPropertyResourceBundle(CodeLab.USER_PROP_FILE);
		return true;
	}

	///////////////////////////////////////////////////
	// Pour afficher la base
	///////////////////////////////////////////////////

	public static void print() {
		if (USERCONF != null) {
			for (Enumeration<String> e = USERCONF.getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				System.out.format("%s=%s\n", key, USERCONF.getString(key));
			}
		}
		if (PROPERTIES != null) {
			for (Enumeration<String> e = PROPERTIES.getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				System.out.format("%s=%s\n", key, PROPERTIES.getString(key));
			}
		}
		if (WEBPROPERTIES != null) {
			for (Enumeration<String> e = WEBPROPERTIES.getKeys(); e.hasMoreElements();) {
				String key = e.nextElement();
				System.out.format("%s=%s\n", key, WEBPROPERTIES.getString(key));
			}
		}
	}

	///////////////////////////////////////////////////
	// Pour récupérer une propriété
	///////////////////////////////////////////////////

	public static String getProperty(String key) {
		if (USERCONF != null && USERCONF.containsKey(key)) return USERCONF.getString(key);
		if (PROPERTIES != null && PROPERTIES.containsKey(key)) return PROPERTIES.getString(key);
		if (WEBPROPERTIES != null && WEBPROPERTIES.containsKey(key)) return WEBPROPERTIES.getString(key);
		return CodeLab.UNDEFINED;
	}

	public static int getIntegerProperty(String key) {
		try {
			return Integer.parseInt(getProperty(key));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	public static boolean getBooleanProperty(String key) {
		switch (getProperty(key)) {
			case "0":
			case "no":
			case "No":
			case "NO":
			case "false":
			case "False":
			case "FALSE": return false;
			case "1":
			case "yes":
			case "Yes":
			case "YES":
			case "true":
			case "True":
			case "TRUE": return true;
			default: return false;
		}
	}

	///////////////////////////////////////////////////
	// Pour charger des propriétés à distance
	///////////////////////////////////////////////////

	public static boolean WEBPROPERTIES_ERR = false;

	public static boolean download(String url, int timeout) {
		WEBPROPERTIES = Utils.readPropertyResourceBundleOverInternet(url, timeout);
		WEBPROPERTIES_ERR = WEBPROPERTIES == null;
		return true;
	}

	///////////////////////////////////////////////////
	// Gestion des propriétés utilisateur
	///////////////////////////////////////////////////

	public static String initUserProperty(String name, String default_value) {
		String value = getProperty(name);
		if (value.equals(CodeLab.UNDEFINED)) {
			addUserProperty(name, default_value);
			return default_value;
		}
		else return value;
	}

	public static void setUserProperty(String name, String value) {
		String rex = String.format("%s=.*", name);
		String str = String.format("%s=%s", name, value);
		CodeLab.logger("Writing " + CodeLab.USER_PROP_FILE);
		MyFileUtils.findAndReplace(CodeLab.USER_PROP_FILE, rex, str);
		USERCONF = Utils.readPropertyResourceBundle(CodeLab.USER_PROP_FILE); // Rechargement
	}

	public static void addUserProperty(String name, String value) {
		String filename = CodeLab.USER_PROP_FILE;
		String newline = String.format("\n%s=%s", name, value);
		try (BufferedWriter output = new BufferedWriter(new FileWriter(filename, true))) {
			output.append(newline);
			USERCONF = Utils.readPropertyResourceBundle(CodeLab.USER_PROP_FILE); // Rechargement
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static ArrayList<String> collectUserPropertiesWithPrefix(String prefix) {
		// Collecte dans USERCONF les keys qui commencent par un préfixe donné
		ArrayList<String> keys = new ArrayList<String>();
		for (Enumeration<String> e = USERCONF.getKeys(); e.hasMoreElements();) {
			String key = e.nextElement();
			if (key.startsWith(prefix)) keys.add(key);
		}
		return keys;
	}

	public static void removeProperty(String key) {
		// Pour supprimer une propriété dans USER_PROP_FILE
		try {
			List<String> allLines = Files.readAllLines(Paths.get(CodeLab.USER_PROP_FILE));
			StringWriter sw = new StringWriter();
			for (String line : allLines) {
				//System.out.println(line);
				if (line.startsWith(key)) continue;
				sw.write(line + "\n");
			}
			// Enregistrer le fichier modifié
			FileWriter fw = new FileWriter(CodeLab.USER_PROP_FILE);
			fw.write(sw.toString());
			fw.close();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}
}

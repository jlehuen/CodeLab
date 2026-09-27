package codelab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe du chargeur de plugins
*	@author Jérôme Lehuen
*	@version 18/01/24
*/

public class PluginLoader {

	private CodeLab codelab;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public PluginLoader(CodeLab codelab) {
		this.codelab = codelab;
		boolean errors = false;
		File basedir = new File(CodeLab.MODULES_FOLDER);
		File[] files = basedir.listFiles();
		if (files.length == 0) System.out.println("   Scanning modules directory... NO PAC");
		else {
			System.out.println("   Scanning modules directory...");
			Arrays.sort(files);
			for (File file : files) {
				if (file.isFile() && MyFileUtils.getExtension(file).equals(".pac")) {
					String name = MyFileUtils.noExtension(file.getName()).toUpperCase();
					System.out.format("      Loading module %s... ", name);
					AbstractModule module = load(file);
					if (module == null) {
						System.out.println("ERROR");
						errors = true;
					} else {
						codelab.addModuleDisp(name, module.getTitle());
						if (codelab.isActivableModule(name)) {
							codelab.addModule(module);
							copyRessources(file);
							System.out.println("DONE");
						}
						else System.out.println("DISABLED");
					}
				}
			}
			if (errors) {
				String msg = CodeLab.LABEL("PAC_ERROR");
				Utils.showMessageDialog("ERROR", msg);
			}
		}
	}

	///////////////////////////////////////////////////
	// Chargement et instanciation d'un module
	///////////////////////////////////////////////////

	private AbstractModule load(File file) {
		try {
			URL url = file.toURI().toURL();
			// Instancier un classloader ad-hoc
			URLClassLoader loader = new URLClassLoader(new URL[] { url });
			// Obtenir la classe principale du module
			String mainClassName = getMainClass(file);
			// System.out.println(mainClassName);
			Class<?> mainClass = Class.forName(mainClassName, true, loader);
			// Instancier le plugin en invoquant son constructeur
			Class<?>[] parameterType = new Class<?>[1];
			parameterType[0] = CodeLab.class; // Unique paramètre du constructeur
			Constructor<?> constructor = mainClass.getDeclaredConstructor(parameterType);
			AbstractModule module = (AbstractModule) constructor.newInstance(codelab);
			return module;
		}
		catch (java.lang.UnsupportedClassVersionError e) {
			System.out.println("ERROR: " + e.getMessage());
			return null;
		}
		catch (Exception e) {
			// ClassNotFoundException | NoSuchMethodException | SecurityException |
			// MalformedURLException | InstantiationException | IllegalAccessException |
			// IllegalArgumentException | InvocationTargetException | IOException
			System.out.println("ERROR: " + e.getMessage());
			ExceptionManager.process(e);
			return null;
		}
	}

	///////////////////////////////////////////////////
	// Identification de la classe principale
	///////////////////////////////////////////////////

	private String getMainClass(File file) throws Exception {
		try (JarFile jar = new JarFile(file.getAbsolutePath())) {
			String fileName = MyFileUtils.noExtension(file.getName());
			Enumeration<JarEntry> content = jar.entries();
			// Rechercher une classe ayant le même nom que le plugin
			while (content.hasMoreElements()) {
				String str = content.nextElement().toString();
				if (MyFileUtils.getExtension(str).equals(".class")) {
					str = MyFileUtils.noExtension(str);
					String className = MyFileUtils.getFilename(str);
					if (className.equals(fileName)) {
						// Retourner le nom complet du package
						return str.replaceAll("/", ".");
					}
				}
			}
			throw new Exception("The module does not contain the main class " + fileName);
		}
		catch (IOException e) { throw e; }
	}

	///////////////////////////////////////////////////
	// Extraction et recopie des ressources
	///////////////////////////////////////////////////

	// Il s'agit de proposer les ressources (includes, templates et exercices)
	// en fonction des modules chargés dans CodeLab. On commence par effacer les
	// anciennes ressources puis on recopie les ressources pour chaque module.

	private void copyRessources(File file) {
		String moduleName = MyFileUtils.noExtension(file.getName()).toLowerCase();

		String includes_c = String.format("%s/c/codelab/%s", CodeLab.INCLUDES_FOLDER, moduleName);
		String includes_go = String.format("%s/go/src/codelab/%s", CodeLab.INCLUDES_FOLDER, moduleName);
		String includes_java = String.format("%s/java/codelab/%s", CodeLab.INCLUDES_FOLDER, moduleName);
		String includes_clips = String.format("%s/clips/codelab/%s", CodeLab.INCLUDES_FOLDER, moduleName);
		String includes_python = String.format("%s/python/codelab/%s", CodeLab.INCLUDES_FOLDER, moduleName);

		String templates_folder = String.format("%s/%s", CodeLab.TEMPLATES_FOLDER, moduleName);
		String exercices_folder = String.format("%s/%s", CodeLab.EXERCICES_FOLDER, moduleName);

		// Effacer les anciens répertoires
		MyFileUtils.delete(new File(includes_c));
		MyFileUtils.delete(new File(includes_go));
		MyFileUtils.delete(new File(includes_java));
		MyFileUtils.delete(new File(includes_clips));
		MyFileUtils.delete(new File(includes_python));
		MyFileUtils.delete(new File(templates_folder));
		MyFileUtils.delete(new File(exercices_folder));

		Pattern pattern = Pattern.compile("includes/([^.]+).go");

		try (JarFile jar = new JarFile(file.getAbsolutePath())) {
			for (JarEntry entry : Collections.list(jar.entries())) {
				if (entry.isDirectory()) continue;
				String path = entry.toString();

				// Recopier les librairies
				if (path.matches("includes/([^.]+).c")) extract(jar, entry, includes_c);
				else if (path.matches("includes/([^.]+).java")) extract(jar, entry, includes_java);
				else if (path.matches("includes/([^.]+).clp")) extract(jar, entry, includes_clips);
				else if (path.matches("includes/([^.]+).py")) extract(jar, entry, includes_python);
				else if (path.matches("includes/([^.]+).go")) {
					// Le path pour Go comporte une étape supplémentaire
					Matcher m = pattern.matcher(path);
					m.find(); // Nécessaire pour utiliser group()
					extract(jar, entry, includes_go + "/" + m.group(1));
				}
				// Recopier les templates et les exercices
				else if (path.matches("templates/.*")) extract(jar, entry, templates_folder);
				else if (path.matches("exercices/.*")) extract(jar, entry, exercices_folder);
			}
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	private void extract(JarFile jar, JarEntry entry, String destination) throws IOException {
		new File(destination).mkdirs(); // Créer le répertoire
		String filename = MyFileUtils.getFilename(entry.toString());
		File file = new File(destination, filename);
		// Vérifier si un fichier ayant le même nom n'existe pas déjà
		// if (Utils.fileExists(file)) return;
		try (InputStream in = jar.getInputStream(entry)) {
			try (FileOutputStream out = new FileOutputStream(file)) {
				while (in.available() > 0) out.write(in.read());
			}
		}
	}
}

package codelab;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codelab.utils.Utils;
import codelab.console.Console;
import codelab.modules.editeur.manager.TypeLang;
import codelab.utils.MyFileUtils;

/**
*	Classe du compilateur C, Go, Java et Haskell
*	Pré-processeur Processing
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public class Compilateur {

	private String filename;
	private AbstractModule module;
	private ProcessBuilder pb = null;

	private Process process;
	private InputStream stdout = null;
	private InputStream stderr = null;

	private AfficheurFlux fluxSortie;
	private AfficheurFlux fluxErreur;

	private boolean halted = false; // Flag arrêt forcé
	protected boolean javamode = false;
	protected boolean cmode = false;
	private boolean warnings = false;

	private boolean isProcessing;
	private File processingFile;

	public static boolean stopOnWarning = true;

	public static File ERR_FILE; // Initialisée dans compile()

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Compilateur(AbstractModule module, File file) {

		this.module = module;
		this.filename = file.getName();
		String directory = file.getAbsoluteFile().getParent();
		String ext = MyFileUtils.getExtension(filename);
		TypeLang lang = TypeLang.identify(ext);
		String target;

		String CPATH = CodeLab.INCLUDES_FOLDER + "/c";
		String GOPATH = CodeLab.INCLUDES_FOLDER + "/go";
		String JPATH_MAC = ".:" + CodeLab.INCLUDES_FOLDER + "/java";
		String JPATH_WIN = ".;" + CodeLab.INCLUDES_FOLDER + "/java";

		// Quelques affichages
		module.codelab.getClient().log2server("Compiling " + filename);
		module.codelab.consoleLog(String.format(CodeLab.LABEL("Compile_program"), filename));

		isProcessing = lang == TypeLang.PROCESS;

		if (isProcessing) {
			// Ajouter les librairies Processing
			JPATH_MAC += ":" + CodeLab.PROCESSING + "/*";
			JPATH_WIN += ";" + CodeLab.PROCESSING + "/*";
			// Générer le fichier Java
			filename = prepareProcessingProgram(file);
			processingFile = new File(filename);

			// Pour visualiser le code Processing adapté pour Java
			/*
			try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
				String line;
				while ((line = br.readLine()) != null) System.out.println(line);
			}
			catch(IOException e) {
			}
			*/
		}

		switch (lang) {

			// https://clang.llvm.org/docs/UsersManual.html#command-line-options
			// https://gcc.gnu.org/onlinedocs/gcc-4.4.7/gcc/Warning-Options.html
			// https://www.unix.com/man-page/osx/1/gcc/
			// http://tigcc.ticalc.org/doc/comopts.html

			// -----------------------------------------------------
			// Langage C
			// -----------------------------------------------------

			case C:

				if (	CodeLab.GCC_CMD == null ||
						CodeLab.GCC_CMD.equals("undefined") ||
						CodeLab.GCC_CMD.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noClangError"));
					break;
				}
				cmode = true;
				target = String.format("%s_c.exe", MyFileUtils.noExtension(filename));
				MyFileUtils.delete(String.format("%s/%s", directory, target));

				// Pour ajouter "setvbuf(stdout, NULL, _IONBF, 0)" en début de programme
				String filename2 = insertBufferingOff(file);

				// -Wno-main-return-type pour autoriser le void main()
				// -Wreturn-type pour contrôler les types de return
				// -Wformat=0 pour évider warning: format not a string literal
				// -Wfloat-equal pour prévenir des tests d'agalité avec des floats

				List<String> cmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.GCC_CMD));

				switch (CodeLab.SYSTEM) {
					case "Mac":
						Collections.addAll(cmd,
							"-Wno-main-return-type",
							"-Wreturn-type",
							"-Wformat=0",
							"-Wfloat-equal",
							"-I", CPATH, "-include", "__init.h", "-o", target, filename2);
						break;
					case "Linux":
						Collections.addAll(cmd,
							"-Wreturn-type",
							"-Wformat=0",
							"-Wfloat-equal",
							"-I", CPATH, "-include", "__init.h", "-o", target, filename2, "-lm");
						break;
					case "Windows":
						Collections.addAll(cmd,
							"-Wno-deprecated-declarations",
							"-I", CPATH, "-include", "__init.h", "-o", target, filename2, "-lws2_32");
						break;
				}
				pb = new ProcessBuilder(cmd);
				break;

			// -----------------------------------------------------
			// Langage Go
			// -----------------------------------------------------

			case GO:

				if (	CodeLab.GOLANG_CMD == null ||
						CodeLab.GOLANG_CMD.equals("undefined") ||
						CodeLab.GOLANG_CMD.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noGolangError"));
					break;
				}
				target = String.format("%s_go.exe", MyFileUtils.noExtension(filename));
				MyFileUtils.delete(String.format("%s/%s", directory, target));

				switch (CodeLab.SYSTEM) {
					case "Mac":
					case "Linux":
						List<String> goCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.GOLANG_CMD));
						goCmd.add("build");
						goCmd.add("-o");
						goCmd.add(target);
						goCmd.add(filename);
						pb = new ProcessBuilder(goCmd);
						break;
					case "Windows":
						// Compilation Go sous Windows à activer si l'environnement est configuré
						break;
				}
				break;

			// -----------------------------------------------------
			// Langage Haskell
			// -----------------------------------------------------

			case HASKELL:

				if (	CodeLab.HASKELL_CMD == null ||
						CodeLab.HASKELL_CMD.equals("undefined") ||
						CodeLab.HASKELL_CMD.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noHaskellError"));
					break;
				}
				target = String.format("%s_hs.exe", MyFileUtils.noExtension(filename));
				MyFileUtils.delete(String.format("%s/%s", directory, target));

				switch (CodeLab.SYSTEM) {
					case "Mac":
					case "Linux":
						List<String> hsCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.HASKELL_CMD));
						hsCmd.add("-o");
						hsCmd.add(target);
						hsCmd.add(filename);
						pb = new ProcessBuilder(hsCmd);
						break;
					case "Windows":
						// Compilation Haskell sous Windows à activer si l'environnement est configuré
						break;
				}
				break;

			// -----------------------------------------------------
			// Langages Java et Processing
			// -----------------------------------------------------

			case JAVA:
			case PROCESS:

				if (	CodeLab.JAVA_HOME == null ||
						CodeLab.JAVA_HOME.equals("undefined") ||
						CodeLab.JAVA_HOME.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noJavaError"));
					break;
				}
				javamode = true;

				deleteClassFiles(directory);

				List<String> javacCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.JAVAC_CMD));
				javacCmd.add("-classpath");
				javacCmd.add(CodeLab.IS_WINDOWS ? JPATH_WIN : JPATH_MAC);
				javacCmd.add(filename);
				pb = new ProcessBuilder(javacCmd);
				break;

			default: // Non compilable
		}

		if (pb != null) {
			// Tout s'est bien passé
			pb.directory(new File(directory));
			Map<String,String> env = pb.environment();
			if (CodeLab.GOLANG_CMD != null) env.put("GOPATH", GOPATH); // Go include directory
			if (CodeLab.JAVA_HOME != null) env.put("JAVA_HOME", CodeLab.JAVA_HOME); // Java home
			if (CodeLab.GCC_CMD != null) env.put("GCC_CMD", CodeLab.GCC_CMD); // GCC command
		}
	}

	///////////////////////////////////////////////////
	// Compilation
	///////////////////////////////////////////////////

	public boolean compile() {
		if (pb == null) return false;
		try {
			//Affichage dans la console de la ligne de commande pour les tuteurs
			CodeLab.directPrintln(String.join(" ", pb.command()), Color.ORANGE);

			// On crée un nouveau fichier ERR_FILE à chaque compilation
			// Nom de fichier temporaire unique pour éviter tout conflit d'accès sous Windows
			ERR_FILE = File.createTempFile("err_", ".log", new File(CodeLab.TEMP_FOLDER));

			process = pb.start();
			stdout = process.getInputStream();
			stderr = process.getErrorStream();
			fluxSortie = new AfficheurFlux(this, stdout, Console.COLOR_ERROR);
			fluxErreur = new AfficheurFlux(this, stderr, Console.COLOR_ERROR);
			fluxSortie.start();
			fluxErreur.start();

			int timeout = CodeLab.COMPILE_TIMEOUT > 0 ? CodeLab.COMPILE_TIMEOUT : 5;
			boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
			if (!finished) {
				halt();
				module.codelab.consoleLogErr(String.format(
					"Compilation timeout (%d seconds) - compilation stopped", timeout));
				return false;
			}

			int errcode = process.exitValue();
			fluxSortie.join(500); // Attendre max 500ms que la sortie se termine sans tronquer les erreurs
			fluxErreur.join(500); // Attendre max 500ms que les erreurs se terminent

			try { if (stdout != null) stdout.close(); } catch (IOException ignored) {}
			try { if (stderr != null) stderr.close(); } catch (IOException ignored) {}

			if (isProcessing) MyFileUtils.delete(processingFile); // Supression du fichier Java temporaire

			if (warnings) {
				module.codelab.consoleLog(CodeLab.LABEL("Compilation_warning_1"));
				if (stopOnWarning) {
					// Option désactivable dans le menu du bouton Exécuter
					module.codelab.consoleLog(CodeLab.LABEL("Compilation_warning_2"));
					return false;
				}
			}
			if (errcode > 0) {
				module.codelab.getClient().log2server("ERROR");
				module.codelab.consoleLog(CodeLab.LABEL("Compilation_errors"));
				return false;
			}
			else {
				module.codelab.getClient().log2server("OK");
				module.codelab.consoleLog(CodeLab.LABEL("Compilation_ok"));
				return true;
			}
		}
		catch (Exception e) {
			halt();
			ExceptionManager.process(e);
			return false;
		}
		finally {
			if (process != null && process.isAlive()) {
				halt();
			}
		}
	}

	public void halt() {
		halted = true;
		if (process != null && process.isAlive()) {
			process.destroy();
			try {
				if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
					process.destroyForcibly();
				}
			}
			catch (InterruptedException e) {
				process.destroyForcibly();
			}
		}
	}

	private void deleteClassFiles(String dirPath) {
		if (dirPath == null) return;
		try {
			File dir = new File(dirPath);
			if (dir.exists() && dir.isDirectory()) {
				File[] classFiles = dir.listFiles((d, name) -> name.endsWith(".class"));
				if (classFiles != null) {
					for (File f : classFiles) {
						f.delete();
					}
				}
			}
		}
		catch (Exception e) {
			// Ignorer les erreurs de nettoyage des .class
		}
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private synchronized void printToConsole(String str, Color color) {
		// Cette méthode doit-elle être synchronized ??
		module.printToConsole(str, color, false);
	}

	// -------------------------------------------------------------------------------
	// Pour ajouter "setvbuf(stdout, NULL, _IONBF, 0)" en début des programmes C
	// -------------------------------------------------------------------------------

	// https://www.regexplanet.com/advanced/java/index.html

	private static String REGEXP1 = ".*main\\s*\\(.*\\)\\s*\\{";
	private static String SETVBUF1 = "\tsetvbuf(stdout, NULL, _IONBF, 0);";
	private static Pattern PATTERN1 = Pattern.compile(REGEXP1);
	private static Charset charset = StandardCharsets.UTF_8;

	private String insertBufferingOff(File file) {
		File dir = MyFileUtils.getDirectory(file);
		String filename_out = dir + "/__temp.c";
		Path path_out = Paths.get(filename_out);
		try {
			String content = new String(Files.readAllBytes(file.toPath()), charset);
			Matcher matcher = PATTERN1.matcher(content);
			if (matcher.find()) {
				String newstr = matcher.group(0) + SETVBUF1; // Sur la même ligne que main() pour préserver la numérotation des lignes
				content = content.replaceFirst(REGEXP1, newstr);
			}
			Files.write(path_out, content.getBytes(charset));
			// Cette méthode garantit que le fichier est fermé lorsque tous les octets ont été écrits
			// (ou lorsqu'une erreur d'E/S ou une autre exception d'exécution est levée).
			return filename_out;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}

	// -------------------------------------------------------------------------------
	// Pré-processeur pour la compilation Java des programmes Processing
	// -------------------------------------------------------------------------------

	// https://happycoding.io/tutorials/java/processing-in-java

	private static String PROCESSING1_a = "import java.util.*;\n";
	private static String PROCESSING1_b = "import javax.swing.*;\n";
	private static String PROCESSING1_c = "import processing.awt.*;\n";
	private static String PROCESSING1_d = "import processing.core.*;\n";
	private static String PROCESSING1_e = "import processing.data.*;\n";
	private static String PROCESSING1_f = "import processing.event.*;\n";
	private static String PROCESSING1_g = "import processing.opengl.*;\n";

	// Pour une ancienne version de Processing
	// private static String PROCESSING2 = "public class %s extends PApplet {\n";
	// private static String PROCESSING3 = "public static void main(String[] args) { PApplet.main(\"%s\"); }\n\n";

	private static String PROCESSING2 = "public class %s extends PApplet {\n";
	private static String PROCESSING3 = "public static void main(String[] args) { String[] processingArgs = {\"%s\"}; %s mySketch = new %s(); PApplet.runSketch(processingArgs, mySketch); }\n\n";

	private int offset = 10; // Offset à ajouter en cas de message d'erreur

	private String prepareProcessingProgram(File file) {
		File dir = MyFileUtils.getDirectory(file);
		String name = MyFileUtils.noExtension(file.getName());
		String filename_out = String.format("%s/%s.java", dir, name);
		try {
			FileWriter fw = new FileWriter(filename_out, true);

			// Écrire les premières lignes
			fw.write(PROCESSING1_a);
			fw.write(PROCESSING1_b);
			fw.write(PROCESSING1_c);
			fw.write(PROCESSING1_d);
			fw.write(PROCESSING1_e);
			fw.write(PROCESSING1_f);
			fw.write(PROCESSING1_g);
			fw.write(String.format(PROCESSING2, name));
			fw.write(String.format(PROCESSING3, name, name, name));

			// Adapter la suite du programme
			String content = new String(Files.readAllBytes(file.toPath()), charset);
			content = content.replaceAll("void", "public void"); // Remplacer les void par des public void
			content = content.replaceAll("(\\d+\\.\\d+)", "$1f"); // Ajouter des f après les nombres à virgule

			content = content.replaceAll("=\\s*int\\s*\\(", "= (int)("); // Adapter les casts vers int
			content = content.replaceAll("=\\s*byte\\s*\\(", "= (byte)("); // Adapter les casts vers byte
			content = content.replaceAll("=\\s*float\\s*\\(", "= (float)("); // Adapter les casts vers float
			content = content.replaceAll("=\\s*str\\s*\\(", "= String.valueOf("); // Adapter les casts vers String

			content += "}"; // Fermer la définition de la classe

			String settings = "public void settings() {\n";
			String regex;
			Pattern pattern;
			Matcher matcher;
			offset++;

			// Extraction de size()
			regex = ".*(size\\(.*\\);).*";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(content);
			if (matcher.find()) {
				settings += matcher.group() + "\n";
				content = content.replaceAll(regex, "");
				offset++;
			}

			// Extraction de fullScreen()
			regex = ".*(fullScreen\\(.*\\);).*";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(content);
			if (matcher.find()) {
				settings += matcher.group() + "\n";
				content = content.replaceAll(regex, "");
				offset++;
			}

			// Extraction de smooth()
			regex = ".*(smooth\\(.*\\);).*";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(content);
			if (matcher.find()) {
				settings += matcher.group() + "\n";
				content = content.replaceAll(regex, "");
				offset++;
			}

			// Extraction de noSmooth()
			regex = ".*(noSmooth\\(.*\\);).*";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(content);
			if (matcher.find()) {
				settings += matcher.group() + "\n";
				content = content.replaceAll(regex, "");
				offset++;
			}

			// Fin du settings
			settings += "}\n\n";
			offset += 2;

//System.out.println(settings);
//System.out.println(content);

			fw.write(settings);
			fw.write(content);
			fw.close();
			return filename_out;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}

	///////////////////////////////////////////////////
	// Thread de l'afficheur de la console + log
	///////////////////////////////////////////////////

	private final String WARNING = "warning";
	private final String REGEX = "java:(\\d+): error:";
	private Pattern pattern = Pattern.compile(REGEX);

	private class AfficheurFlux extends Thread {

		private InputStream inputStream;
		private Compilateur compilateur;
		private Color color;

		AfficheurFlux(Compilateur compilateur, InputStream inputStream, Color color) {
			this.inputStream = inputStream;
			this.compilateur = compilateur;
			this.color = color;
		}

		private BufferedReader getBufferedReader(InputStream is) {
			return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		}

		private void writeline(String str) {
			// Ajout synchronisé dans le fichier temporaire de log pour éviter les collisions d'accès concurrent sous Windows
			synchronized (Compilateur.class) {
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(ERR_FILE, true))) {
					writer.append(str + "\n");
				}
				catch (IOException e) {
					System.out.println("ERROR (AfficheurFlux): " + e.getMessage());
				}
			}
		}

		public void run() {
			BufferedReader reader = getBufferedReader(inputStream);
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					if (cmode && line.contains(WARNING)) {
						warnings = true;
					}
					if (isProcessing) {
						Matcher matcher = pattern.matcher(line);
						if (matcher.find()) {
							// Modifier le numéro de ligne pour Processing (nb de lignes insérées)
							int nbline = Integer.parseInt(matcher.group(1)) - offset;
							line = line.replaceAll(REGEX, String.format("pde:%d: error:", nbline));
						}
					}
					compilateur.printToConsole(String.format(" %s\n", line), color);
					writeline(line); // Ajout dans le fichier temporaire de log
				}
			}
			catch (IOException e) {
				if (!halted) {
					ExceptionManager.process(e);
				}
			}
		}
	}
}

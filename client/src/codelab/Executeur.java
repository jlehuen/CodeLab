package codelab;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import codelab.console.Console;
import codelab.modules.editeur.manager.TypeLang;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe de l'exécuteur de programmes
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public class Executeur implements Runnable {

	private CodeLab codelab;
	private String base, filename, name, ext;
	private TypeLang lang;

	private Process process;
	private ProcessBuilder pb = null;

	private OutputStream stdin = null;
	private BufferedWriter stdinWriter = null;
	private ExecutorService stdinExecutor = null;
	private InputStream stdout = null;
	private InputStream stderr = null;

	private boolean halted = false; // Flag arrêt forcé
	private boolean errorFlag = false;
	private boolean isBlocs = false;
	private boolean isHTML = false;

	private AfficheurFlux fluxSortie = null;
	private AfficheurFlux fluxErreur = null;

	public boolean isReady = false;

	public static File ERR_FILE; // Initialisée dans AfficheurFlux()

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Executeur(String filename, String base, CodeLab codelab) {

		// En principe cela ne doit pas arriver !!
		if (filename == null || base == null) return;

		this.codelab = codelab;
		this.filename = filename;
		this.base = base;
		String target;

		ext = MyFileUtils.getExtension(filename);
		name = MyFileUtils.noExtension(filename);
		lang = TypeLang.identify(ext);

		isHTML = lang == TypeLang.HTML;
		isBlocs = lang == TypeLang.BLOCS;
		boolean isProcessing = lang == TypeLang.PROCESS;

		String PYPATH = CodeLab.INCLUDES_FOLDER + "/python";
		String LIPATH = CodeLab.INCLUDES_FOLDER + "/lisp";
		String LUPATH = CodeLab.INCLUDES_FOLDER + "/lua/?.lua";
		String CLIPSLIB = CodeLab.INCLUDES_FOLDER + "/clips";
		String JPATH = null;

		// Complément du path Java
		if (CodeLab.SYSTEM.equals("Windows")) {
			JPATH = String.format(".;%s/java", CodeLab.INCLUDES_FOLDER);
			if (isProcessing) JPATH += String.format(";%s/*", CodeLab.PROCESSING);
		} else {
			JPATH = String.format(".:%s/java", CodeLab.INCLUDES_FOLDER);
			if (isProcessing) JPATH += String.format(":%s/*", CodeLab.PROCESSING);
		}

		// Emplacement des librairies natives
		String DLIBPATH = CodeLab.PROCESSING;
		switch (CodeLab.SYSTEM) {
			case "Mac": DLIBPATH += "/macos-x86_64"; break;
			case "Linux": DLIBPATH += "/linux-amd64"; break;
			case "Windows": DLIBPATH += "/windows-amd64"; break;
		}

		switch (lang) {

			///////////////////////////////////////////////////
			// Exécution d'un programme C / Go / Haskell
			///////////////////////////////////////////////////

			case C:
				target = String.format("%s_c.exe", name);
				pb = new ProcessBuilder(String.format("%s/%s", base, target));
				break;
			case GO:
				target = String.format("%s_go.exe",name);
				pb = new ProcessBuilder(String.format("%s/%s", base, target));
				break;
			case HASKELL:
				target = String.format("%s_hs.exe", name);
				pb = new ProcessBuilder(String.format("%s/%s", base, target));
				break;

			///////////////////////////////////////////////////
			// Exécution d'un programme Java / Processing
			///////////////////////////////////////////////////

			case JAVA:
			case PROCESS:
				target = Utils.capitalize(name);
				List<String> javaCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.JAVA_CMD));
				javaCmd.add("-Djava.library.path=" + DLIBPATH);
				javaCmd.add("-classpath");
				javaCmd.add(JPATH);
				javaCmd.add(target);
				pb = new ProcessBuilder(javaCmd);
				break;

			///////////////////////////////////////////////////
			// Exécution d'un programme CLIPS
			///////////////////////////////////////////////////

			case CLIPS:
				if (	CodeLab.CLIPS_CMD == null ||
						CodeLab.CLIPS_CMD.equals("undefined") ||
						CodeLab.CLIPS_CMD.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noClipsError"));
					break;
				}
				List<String> clipsCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.CLIPS_CMD));
				clipsCmd.add("-f2");
				clipsCmd.add(CLIPSLIB + "/__init.clp");
				clipsCmd.add("-l");
				clipsCmd.add(filename);
				clipsCmd.add("-f2");
				clipsCmd.add(CLIPSLIB + "/__run.clp");
				pb = new ProcessBuilder(clipsCmd);
				break;

			///////////////////////////////////////////////////
			// Exécution d'un programme Python / Scratch
			///////////////////////////////////////////////////

			case PYTHON:
			case BLOCS:
				if (	CodeLab.PYTHON_CMD == null ||
						CodeLab.PYTHON_CMD.equals("undefined") ||
						CodeLab.PYTHON_CMD.equals("DISABLED")) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("noPythonError"));
					break;
				}
				if (isBlocs) filename += ".py";
				// -u force stdout to be unbuffered
				List<String> pyCmd = new ArrayList<>(Utils.splitCommandLine(CodeLab.PYTHON_CMD));
				pyCmd.add("-u");
				pyCmd.add(filename);
				pb = new ProcessBuilder(pyCmd);
				break;

			///////////////////////////////////////////////////
			// Visualisation d'un code HTML
			///////////////////////////////////////////////////

			case HTML:
				isReady = true;
				return;

			default: // Non exécutable
		}

		///////////////////////////////////////////////////
		// Variables d'environnement
		///////////////////////////////////////////////////

		if (pb != null) {
			// Tout s'est bien passé
			pb.directory(new File(base)); // Le répertoire courant
			Map<String,String> env = pb.environment();
			env.put("FILENAME", filename);
			env.put("DIRECTORY", base);
			env.put("PYTHONPATH", PYPATH);
			env.put("PYTHONUNBUFFERED", "true"); // Force stdin stdout and stderr to be unbuffered (option -u)
			env.put("NEWLISPDIR", LIPATH);
			env.put("NEWLISPLIB_INIT", LIPATH);
			env.put("LUA_PATH", LUPATH);
			env.put("CLIPSLIB", CLIPSLIB);
			env.put("CODELAB_FILES", CodeLab.CODELAB_FILES); // Pour les ressources-utilisateur
			env.put("INTERNAL_PORT", CodeLab.LOCAL_PORT); // Port du serveur UDP local
			isReady = true;
		}
	}

	///////////////////////////////////////////////////
	// Pour lancer une exécution
	///////////////////////////////////////////////////

	public void run() {

		if (isHTML) {
			try {
				String url = String.format("file://%s/%s", base, filename);
				if (CodeLab.IS_WINDOWS) {
					url = url.replace("\\", "/");
				}
				codelab.consoleLog("Openning external browser with " + url);
				Utils.openBrowser(url);
			}
			catch (Exception e) {
				ExceptionManager.process(e);
			}
			finally {
				codelab.executionCompleted(); // Rien à faire de plus
			}
			return;
		}

		printInfo();
		codelab.getClient().log2server("Executing " + filename);
		
		try {
			// On crée un nouveau fichier ERR_FILE à chaque exécution
			// Nom de fichier temporaire unique pour éviter tout conflit d'accès sous Windows
			try {
				ERR_FILE = File.createTempFile("err_", ".log", new File(CodeLab.TEMP_FOLDER));
			}
			catch (IOException e) {
				System.out.println(e.getMessage());
			}

			process = pb.start();
			stdin = process.getOutputStream();
			stdinWriter = new BufferedWriter(new OutputStreamWriter(stdin, StandardCharsets.UTF_8));
			stdinExecutor = Executors.newSingleThreadExecutor();
			stdout = process.getInputStream();
			stderr = process.getErrorStream();

			// Deux threads distincts pour éviter le blocage si l'un des pipes OS sature
			fluxSortie = new AfficheurFlux(this, stdout, Console.COLOR_PRINT, false);
			fluxErreur = new AfficheurFlux(this, stderr, Console.COLOR_ERROR, true);
			fluxSortie.start();
			fluxErreur.start();

			int errcode = -1;
			try {
				errcode = process.waitFor();
			}
			catch (InterruptedException e) {
				halt();
			}
			long joinTimeout = halted ? 200 : 3000;
			try { if (fluxSortie != null) fluxSortie.join(joinTimeout); } catch (InterruptedException ignored) {}
			try { if (fluxErreur != null) fluxErreur.join(joinTimeout); } catch (InterruptedException ignored) {}
			try { if (stdout != null) stdout.close(); } catch (IOException ignored) {}
			try { if (stderr != null) stderr.close(); } catch (IOException ignored) {}

			// Traitement des interruptions en C
			// https://leo.leung.xyz/wiki/Exit_Codes
			String str = "";
			if (lang == TypeLang.C) switch (errcode) {
				case 137: str = "Program killed"; break;
				case 134: str = "Program aborted"; break;
				case 130: str = "Program interrupted"; break;
				case 136: str = "RUNTIME ERROR: Division by zero"; break;
				case 139: str = "RUNTIME ERROR: Segmentation fault"; break;
			}

			if (halted) {
				// Programme stoppé
				codelab.getClient().log2server("HALTED");
				codelab.printToConsole("\n", Console.COLOR_LOG, false);
				codelab.consoleLog(CodeLab.LABEL("Execution_halted"));
			} else if (!str.isEmpty()) {
				// Runtime error (langage C)
				codelab.getClient().log2server("ERROR");
				printToConsole(String.format("[codelab] %s (code %d)", str, errcode), Console.COLOR_ERROR, false);
				codelab.consoleLog(CodeLab.LABEL("Execution_error"));
			} else if (errorFlag) {
				// Runtime error (autres langages)
				codelab.getClient().log2server("ERROR");
				codelab.consoleLog(CodeLab.LABEL("Execution_error"));
			} else {
				// Exécution complète
				codelab.getClient().log2server("DONE");
				codelab.consoleLog(CodeLab.LABEL("Execution_completed"));
			}
		}
		catch (Exception e) {
			ExceptionManager.process(e);
			codelab.consoleLogErr("[codelab] Execution error: " + e.getMessage());
		}
		finally {
			closeStdin();
			if (process != null && process.isAlive()) {
				halt();
			}
			codelab.executionCompleted(); // Exécution terminée
		}
	}

	///////////////////////////////////////////////////
	// Pour stopper une exécution
	///////////////////////////////////////////////////

	public void halt() {
		halted = true;
		closeStdin();
		if (process != null) {
			try {
				process.descendants().forEach(ProcessHandle::destroy);
			}
			catch (Exception ignored) {}
			process.destroy();
			// Destruction forcée en tâche de fond si le processus ne répond pas
			new Thread(() -> {
				try {
					if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
						process.descendants().forEach(ProcessHandle::destroyForcibly);
						process.destroyForcibly();
					}
				}
				catch (Exception ignored) {}
			}).start();
		}
		if (fluxSortie != null) fluxSortie.closeReader();
		if (fluxErreur != null) fluxErreur.closeReader();
	}

	private void closeStdin() {
		if (stdinExecutor != null) {
			stdinExecutor.shutdownNow();
			stdinExecutor = null;
		}
		if (stdinWriter != null) {
			try { stdinWriter.close(); } catch (IOException ignored) {}
			stdinWriter = null;
		}
		if (stdin != null) {
			try { stdin.close(); } catch (IOException ignored) {}
			stdin = null;
		}
	}

	///////////////////////////////////////////////////
	// Pour récupérer le flux stdin de la console
	///////////////////////////////////////////////////

	public void write(String line) {
		// Invoqué dans la méthode keyPressed de la classe Console (EDT)
		// Traité en tâche de fond pour ne jamais bloquer l'EDT si le pipe stdin OS est saturé
		if (stdinWriter == null || halted) return;
		final BufferedWriter writer = stdinWriter;
		if (stdinExecutor != null && !stdinExecutor.isShutdown()) {
			stdinExecutor.submit(() -> {
				try {
					writer.write(line);
					writer.flush();
				}
				catch (IOException e) {
					if (!halted) ExceptionManager.process(e);
				}
			});
		}
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private BufferedReader __getBufferedReader__(InputStream is) {
		return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
	}

	private void printToConsole(String str, Color color, boolean caret) {
		codelab.printToConsole(str+'\n', color, caret);
	}

	private void printChunkToConsole(String str, Color color, boolean caret) {
		codelab.printToConsole(str, color, caret);
	}

	private void printInfo() {
		//printToConsole(String.format("%s %s", Utils.repeat(70, "-"), Utils.getDate()), Console.GRAY, false);
		codelab.consoleLog(String.format(CodeLab.LABEL("Execution_started"), filename));
	}

	///////////////////////////////////////////////////
	// Afficheur de flux d'exécution
	///////////////////////////////////////////////////

	private class AfficheurFlux extends Thread {

		private Executeur executeur;
		private InputStream stream;
		private Color color;
		private boolean isError;

		///////////////////////////////////////////////////
		// Constructeur
		///////////////////////////////////////////////////

		AfficheurFlux(Executeur executeur, InputStream stream, Color color, boolean isError) {
			this.executeur = executeur;
			this.stream = stream;
			this.color = color;
			this.isError = isError;
		}

		void closeReader() {
			try {
				if (stream != null) stream.close();
			}
			catch (IOException ignored) {}
			interrupt();
		}

		///////////////////////////////////////////////////
		// Méthode Run
		///////////////////////////////////////////////////

		public void run() {
			BufferedReader reader = __getBufferedReader__(stream);
			BufferedWriter errWriter = null;
			if (isError && ERR_FILE != null) {
				try {
					errWriter = new BufferedWriter(new FileWriter(ERR_FILE, true));
				}
				catch (IOException e) {
					System.out.println("ERROR (AfficheurFlux): " + e.getMessage());
				}
			}

			char[] buf = new char[1024];
			int n = -1;
			try {
				// Lecture par bloc sur le flux (non-bloquant au-delà du 1er caractère disponible)
				// Le !halted et !isInterrupted permettent de quitter la boucle en cas d'arrêt de l'exécution
				while (!halted && !isInterrupted() && (n = reader.read(buf, 0, buf.length)) != -1) {
					String str = new String(buf, 0, n);
					if (!isError || !isBlocs) {
						executeur.printChunkToConsole(str, color, false); // Vers la console
					}
					if (isError) {
						errorFlag = true; // Flag erreur pour le message de clôture
						if (errWriter != null) {
							errWriter.write(buf, 0, n);
							errWriter.flush();
						}
					}
				}
			}
			catch (IOException e) {
				// On va avoir un Stream closed sur le reader.read()
				// C'est normal car on était en attente bloquée
			}
			finally {
				if (errWriter != null) {
					try {
						errWriter.close();
					}
					catch (IOException e) {
						// Ignorer
					}
				}
			}
		}
	}
}

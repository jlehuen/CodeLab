package codelab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import codelab.client.CodelabClient;
import codelab.client.ConnectDialog;
import codelab.client.Statut;
import codelab.console.Console;
import codelab.controllers.devices.EventReader;
import codelab.controllers.devices.EventViewer;
import codelab.controllers.widgets.Joystick;
import codelab.controllers.widgets.Numpad;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.manager.TypeLang;
import codelab.modules.graphics.ModuleGraphics;
import codelab.modules.robotics.ModuleRobotics;
import codelab.utils.Encryption;
import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.JEditorPaneWithLink;
import codelab.utils.SplashWindow;
import codelab.utils.Utils;
import codelab.utils.WaitingDialog;
import codelab.utils.SwingWatchdog;
import codelab.utils.Version;
import codelab.utils.audio.MiniPlayer_v1;
import codelab.utils.audio.MiniPlayer_v2;
import codelab.utils.audio.MiniSynth;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
*	Classe principale de CodeLab
*	@author Jérôme Lehuen
*	@version 15/09/26
*/

public class CodeLab extends AbstractCodeLab {

	public static JFrame FRAME = null;
	public static CodeLab INSTANCE = null;

	private ModuleEditor editor;
	private CodeLabRete moteur;
	private ServeurUDP serveur;
	private CodelabClient client;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CodeLab() {
		super(); // AbstractCodeLab

		// Instanciation de la console
		console = new Console(this);

		// Instanciation du client
		client = new CodelabClient(this);

		// Instanciation du module éditeur
		addModule(editor = new ModuleEditor(this));

		// Instanciation des modules intégrés
		modules_disp.put("MODULEGRAPHICS", LABEL("Tab_graphic"));
		modules_disp.put("MODULEROBOTICS", LABEL("Tab_simulator"));
		boolean graphics_flag = isActivableModule("MODULEGRAPHICS");
		boolean robotics_flag = isActivableModule("MODULEROBOTICS");
		if (graphics_flag) addModule(new ModuleGraphics(this));
		if (robotics_flag) addModule(new ModuleRobotics(this));

		// Chargement des plugins
		new PluginLoader(this);
		updateModuleProperties();

		// Instanciation du moteur Jess
		moteur = new CodeLabRete(this);

		// Placement des composants
		currentModule = module_list.get(0); // Module éditeur
		getContentPane().add(toolbar = currentModule.getToolbar(), BorderLayout.NORTH);
		getContentPane().add(tabbedpane, BorderLayout.CENTER);
		setWindowSize();
		pack();

		// Initialisations
		if (CONSOLE_FLAG) showConsole();
		for (AbstractModule module : module_list) {
			module.getToolbar().update(); // Déchenche l'intanciation du EventViewer
			module.getToolbar().updateRunTipText("");
			module.init();
		}

		// Listener de changement de module
		tabbedpane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				JTabbedPane pane = (JTabbedPane) event.getSource();
				module_index = pane.getSelectedIndex();
				currentModule = module_list.get(module_index);
				currentModule.updateConsole(); // Actualiser l'état de la console
				changeToolBar(currentModule.getToolbar()); // Changer et actualiser la barre
				CodeLab.logger("Switch to module " + currentModule.getName());
			}
		});

		// Listener de redimmentionnement
		getRootPane().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				WaitingDialog.updateLocation();
				for (AbstractModule module : module_list)
					module.updateDividersLocation();
			}
		});

		// Listener de déplacement
		addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				WaitingDialog.updateLocation();
			}
		});

		// Listener de fermeture de CodeLab
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				things_to_do_before_exiting();
				CodeLab.exit();
			}
		});

		setTitle(TITLE);
		setWindowLocation();
		//setRunKeyStroke(); // Pour intercepter les Cmd-R (compile & run)
		setIconImage(ResourceUtils.loadImageIcon("appicon.png").getImage());
		setIconImage(this, "icone_big.png");
		setResizable(true);
		setVisible(true);
	}

	///////////////////////////////////////////////////
	// Getters et setters (et autres choses)
	///////////////////////////////////////////////////

	public ModuleEditor getEditor() {
		return editor;
	}

	public CodelabClient getClient() {
		return client;
	}

	public CodeLabRete getRete() {
		return moteur;
	}

	public void showEditor() {
		showModule(editor);
	}

	public void editPropFile() {
		editPropFile(true);
	}

	public void editPropFile(boolean save_flag) {
		File file = new File(USER_PROP_FILE);
		if (!file.exists()) {
			consoleLog("WARNING: Can't find " + USER_PROP_FILE);
			return;
		}
		editor.open_file(file, save_flag);
		showEditor();
		// Prévenir le serveur
		client.setEditedFile(AbstractEditor.EMPTY);
	}

	///////////////////////////////////////////////////
	// Terminateurs
	///////////////////////////////////////////////////

	public void things_to_do_before_exiting() {
		CodeLab.logger("Things to do before exiting CodeLab...");
		if (isRunning()) halt(); // Stopper exécution en cours
		editor.saveCurrentFile(); // Sauvegarde en local et à distance
		editor.backupCurrentFile(); // Backup daté du fichier courant
		//executionMonitor.shutdown(); // Arrêter le moniteur d'exécution
		updateSizeProperties(); // Actualisation de la taille de la fenêtre
		saveHiddenProperties(); // Sauvegarde des hidden properties
		Utils.wait(500); // Attendre 500 ms
	}

	public static void relaunch() {
		try {
			new ProcessBuilder(LAUNCHER, "--relauch").start();
			CodeLab.exit();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static void exit() {
		if (IS_WINDOWS) {
			// Ya que ça qui dégage javaw.exe sous Windows !!
			Runtime.getRuntime().halt(0);
		}
		else System.exit(0);
	}

	///////////////////////////////////////////////////
	// Serveur interne
	///////////////////////////////////////////////////

	private boolean startLocalServer() {
		int port = Integer.valueOf(LOCAL_PORT);
		if (!Utils.isPortAvailable(port)) {
			System.err.format("\n----------------------------------------------");
			System.err.format("\nERROR: Internal port %d unavailable", port);
			System.err.format("\n----------------------------------------------\n");
			System.err.format("\nTry pkill -9 -fi codelab");
			System.err.format("\nAnd relaunch CodeLab...\n");
			return false;
		}
		serveur = new ServeurUDP(this, port);
		serveur.start();
		return true;
	}

	private void checkLocalServer() {
		if (serveur == null) startLocalServer();
		if (!serveur.isRunning()) serveur.start();
	}

	///////////////////////////////////////////////////
	// Compilation et exécution
	///////////////////////////////////////////////////

	private final boolean RESET_AGENDA = true; // Reset du moteur avant chaque exécution

	private Executeur executeur = null;
	private Thread thread = null;

	public boolean isRunning() {
		return (executeur != null);
	}

	public Executeur getExecuteur() {
		return executeur;
	}

	public boolean execute() {
		if (isRunning()) return false; // En principe cela ne doit pas arriver !!

		checkLocalServer(); // Lancer le serveur local (si besoin)
		clearConsole(); // Effacer la console (si autoclear)

		//System.out.println("Fichier => " + editor.getCurrentFilename());
		//System.out.println("Flag modifié => " + editor.isModified());
		//System.out.println("Flag comp_OK => " + editor.compilation_OK());

		// Sauvagarder (si nécessaire)
		if (editor.isModified()) {
			editor.saveCurrentFile();
			Utils.wait(100);
		}
		
		// Compiler (si nécessaire)
		if (editor.isCompilable() && !editor.compilation_OK()) {
			CodeLab.logger("Compile program " + editor.getCurrentFilename());
			boolean ok = editor.compileFile();
			if (!ok) {
				console.flush();
				checkErrorComp();
				console.flush();
				return false;
			}
		}

		// Instancier un exécuteur
		String base = editor.getCurrentDirectory();
		String filename = editor.getCurrentFilename();
		executeur = new Executeur(filename, base, this);
		if (!executeur.isReady) {
			// Pas censé arriver
			consoleLogErr("[codelab] Can't execute program");
			return false;
		}
		CodeLab.logger("Execute program " + filename);

		// Configurer la console
		console.setExecuteur(executeur);
		console.reset();
		console.focus();

		// Empêcher les changements d'onglet
		enableTabbedPane(false);

		// Exécuter l'exécuteur dans un thread
		thread = new Thread(executeur);
		thread.start();

		// Actualiser l'interface
		Utils.wait(100);
		toolbar_update();
		editor.updateFrameTitle();
		return true;
	}

	public void halt() {
		// Invoqué également par le bouton RESET et le menu CodeLab
		if (isRunning()) {
			MiniSynth.stop();
			MiniPlayer_v2.stopAll();
			if (executeur != null) {
				console.reset();
				executeur.halt();
			}
		}
	}

	public void executionCompleted() {
		// Invoqué en fin de run() de Executeur
		// executionMonitor.stopMonitoring(); // Arrêter la surveillance du thread
		executeur = null;
		thread = null;
		stopwidgets();
		stopmodules();
		stopaudio(); // Arrêt immédiat des sons (synthétiseur et fichiers audio)

		SwingUtilities.invokeLater(() -> {
			toolbar_update();
			enableTabbedPane(true); // Autoriser les changements d'onglet
			console.setExecuteur(null);
			console.flush(); // Flush tout l'affichage avant de lancer le diagnostic d'erreurs
			console.focus();
			tabbedpane.requestFocus();
			editor.getManager().consolidate(); // Détection de nouveaux fichiers
			editor.updateFrameTitle();
			checkErrorExe();
			console.flush(); // Flush les explications du moteur d'inférence
		});
	}

	private void checkErrorComp() {
		if (RESET_AGENDA) moteur.reset();
		moteur.checkErrors(Compilateur.ERR_FILE, "error_comp");
	}

	private void checkErrorExe() {
		if (RESET_AGENDA) moteur.reset();
		boolean isBlocs = editor.getCurrentLanguage().equals(TypeLang.BLOCS);
		if (isBlocs) editor.checkBlocsErrors();
		else moteur.checkErrors(Executeur.ERR_FILE, "error_exe");
	}

	private void stopaudio() {
		MiniSynth.stop();
		MiniPlayer_v2.stopAll();
	}

	private void stopmodules() {
		for (AbstractModule module : module_list)
			stopModule(module);
	}

	private void stopwidgets() {
		Numpad.INSTANCE.reset();
		Joystick.INSTANCE.reset();
		if (!EventReader.natives_OK) return;
		EventViewer.INSTANCE.reset();
	}

	///////////////////////////////////////////////////
	// Gestionnaire de requêtes primaire
	///////////////////////////////////////////////////

	public synchronized String handleRequest(String query) {
		//System.out.format("[%s]\n", query);
		Map<String, String> params = Utils.getQueryParameters(query);
		String name = params.get("module");

		for (AbstractModule module : module_list) {
			if (name.equals(module.getName())) {
				if (currentModule != module) showModule(module); // Montrer le module (si besoin)
				if (!module.isRunning()) startModule(module); // Activer le module (si besoin)
				return module.handleRequest(params);
			}
		}
		switch (name) {
			case "INOUT":
			case "AUDIO":
				return this.handleRequest(params);
			default:
				return unknownModuleError(name);

		}
	}

	///////////////////////////////////////////////////
	// Gestionnaire de requêtes secondaire
	///////////////////////////////////////////////////

	private String handleRequest(Map<String, String> params) {
		String cmd = params.get("cmd");
		String filename;
		File file;
		int freq, time, ampl;

		switch (cmd) {
			case "getNumpadValue":
				Numpad.INSTANCE.open();
				return String.valueOf(Numpad.INSTANCE.getValue());
			case "getJoystickValueX":
				Joystick.INSTANCE.open();
				return String.valueOf(Joystick.INSTANCE.getValueX());
			case "getJoystickValueY":
				Joystick.INSTANCE.open();
				return String.valueOf(Joystick.INSTANCE.getValueY());
			case "hasNextEvent":
				EventViewer.INSTANCE.open();
				return String.valueOf(EventViewer.INSTANCE.hasNextEvent());
			case "getNextEvent":
				EventViewer.INSTANCE.open();
				return EventViewer.INSTANCE.getNextEvent();
			case "resetEventQueue":
				return String.valueOf(EventViewer.INSTANCE.reset());
			case "playTone":
				freq = Integer.valueOf(params.get("freq"));
				time = Integer.valueOf(params.get("time"));
				ampl = Integer.valueOf(params.get("ampl"));
				return String.valueOf(MiniSynth.playTone(freq, time, ampl));
			case "playToneOn":
				freq = Integer.valueOf(params.get("freq"));
				ampl = Integer.valueOf(params.get("ampl"));
				return String.valueOf(MiniSynth.playToneOn(freq, ampl));
			case "playToneOff":
				return String.valueOf(MiniSynth.playToneOff());
			case "playAudioFile":
				filename = params.get("filename");
				file = new File(USER_MEDIA_FOLDER + File.separator + filename);
				return String.valueOf(MiniPlayer_v2.play(file));
			case "loopAudioFile":
				filename = params.get("filename");
				file = new File(USER_MEDIA_FOLDER + File.separator + filename);
				return String.valueOf(MiniPlayer_v2.loop(file));
			case "stopAudioPlayer":
				return String.valueOf(MiniPlayer_v2.stopAll());
			default:
				return unknownCommandError(cmd);
		}
	}

	private String unknownModuleError(String name) {
		String msg = String.format("ERROR handleRequest: unknown module [%s]\n", name);
		printlnToConsole(msg, Color.RED);
		halt(); // On arrête l'exécution
		return "error";
	}

	public String unknownCommandError(String cmd) {
		String msg = String.format("ERROR handleRequest: unknown command [%s]\n", cmd);
		printlnToConsole(msg, Color.RED);
		halt(); // On arrête l'exécution
		return "error";
	}

	///////////////////////////////////////////////////
	// Contrôle du client
	///////////////////////////////////////////////////

	private String login;
	private String password;
	public boolean helpFlag = false;

	public String getLogin() {
		return login;
	}

	public boolean isConnected() {
		return client.isConnected();
	}

	public boolean isTutor() {
		return client.isTutor();
	}

	public boolean isAdmin() {
		return client.isAdmin();
	}

	public boolean isStudent() {
		return client.isStudent();
	}

	public Statut getStatut() {
		if (client.isConnected())
			return client.getStatut();
		else return Statut.STANDALONE;
	}

	public String getProgramDir() {
		if (client.isConnected())
			return PROG_DIST_FOLDER;
		else return PROG_LOCAL_FOLDER;
	}

	public boolean connection() {
		final String masterkey = BUILD;
		boolean input_required = true;

		if (AUTO_LOGIN && !PASSWD.equals(UNDEFINED)) {
			try {
				// Décryptage des identifiants
				consoleLog("Auto-login to the file server...");
				login = Encryption.decrypt(LOGIN, masterkey);
				password = Encryption.decrypt(PASSWD, masterkey);
				input_required = false;
			}
			catch (Exception e) {
				consoleLog("An error occurred while retrieving connection data (perhaps due to a recent CodeLab update)");
			}
		}

		if (input_required) {
			// Saisie des identifiants
			ConnectDialog cd = new ConnectDialog();
			if (cd.cancelled()) return false;
			login = cd.getLogin();
			password = cd.getPassword();

			if (AUTO_LOGIN) {
				try {
					// Cryptage des identifiants
					LOGIN = Encryption.encrypt(login, masterkey);
					PASSWD = Encryption.encrypt(password, masterkey);
				}
				catch (Exception e) {
					consoleLog("An error occurred while encrypt connection data");
					ExceptionManager.process(e); // Pas censé arriver !
				}
			}
		}

		// Tentative de connexion au serveur
		return client.connection(login, password);
	}

	public String getSession() {
		return isConnected() ? client.getSession() : "--";
	}

	public String getUsername() {
		// Le nom complet de connexion ou le nom du fichier de pptés
		return isConnected() ? client.getUsername() : USER;
	}

	public void serverActivity() {
		// LED clignotante
		toolbar.serverActivity();
		toolbar.serverActivityLED();
	}

	public void toggleHelpFlag() {
		helpFlag = !helpFlag;
		client.setHelpFlag(helpFlag);	
	}
	
	///////////////////////////////////////////////////
	// Vérification de la version de CodeLab
	///////////////////////////////////////////////////

	public static int checkVersion() {
		return checkVersion(true);
	}

	public static int checkVersion(boolean forceRefresh) {
		System.out.print("   Checking new version... ");

		// Rafraîchir les propriétés distantes si demandé ou si nécessaire
		if (forceRefresh || PropertyBase.WEBPROPERTIES_ERR || PropertyBase.WEBPROPERTIES == null) {
			PropertyBase.download(CodeLab.PROP("URL_PROP"), TIME_OUT);
		}

		if (PropertyBase.WEBPROPERTIES_ERR || PropertyBase.WEBPROPERTIES == null) {
			System.out.println("NETWORK ERROR");
			return 2;
		}

		String last_version = CodeLab.PROP("VERSION");
		String last_build = CodeLab.PROP("BUILD");

		boolean has_version = (last_version != null && !last_version.equals(UNDEFINED) && !last_version.trim().isEmpty());
		boolean has_build = (last_build != null && !last_build.equals(UNDEFINED) && !last_build.trim().isEmpty());

		if (!has_version && !has_build) {
			System.out.println("UNDEFINED");
			return 3;
		}

		boolean new_version_available = false;

		if (has_version) {
			try {
				Version currentV = new Version(VERSION);
				Version lastV = new Version(last_version.trim());
				int cmp = lastV.compareTo(currentV);
				if (cmp > 0) {
					new_version_available = true;
				}
				else if (cmp == 0 && has_build) {
					try {
						long this_build_value = Long.parseLong(BUILD);
						long last_build_value = Long.parseLong(last_build.trim());
						if (last_build_value > this_build_value) {
							new_version_available = true;
						}
					}
					catch (NumberFormatException ignored) {
					}
				}
			}
			catch (IllegalArgumentException e) {
				if (has_build) {
					try {
						long this_build_value = Long.parseLong(BUILD);
						long last_build_value = Long.parseLong(last_build.trim());
						if (last_build_value > this_build_value) {
							new_version_available = true;
						}
					}
					catch (NumberFormatException nfe) {
						System.out.println("PARSE ERROR");
						return 3;
					}
				}
				else {
					System.out.println("PARSE ERROR");
					return 3;
				}
			}
		}
		else if (has_build) {
			try {
				long this_build_value = Long.parseLong(BUILD);
				long last_build_value = Long.parseLong(last_build.trim());
				if (last_build_value > this_build_value) {
					new_version_available = true;
				}
			}
			catch (NumberFormatException e) {
				System.out.println("PARSE ERROR");
				return 3;
			}
		}

		if (FORCE_DOWNLOAD || new_version_available) {
			System.out.println();
			System.out.format("      Current: %s (%s)\n", VERSION, BUILD);
			System.out.format("      Available: %s (%s)\n", last_version != null ? last_version : "-", last_build != null ? last_build : "-");

			// Fenêtre de dialogue proposant de se rendre sur la page de téléchargement
			String msg = String.format(
				CodeLab.LABEL("NEW_VERSION"),
				VERSION, BUILD,
				last_version != null ? last_version : "-",
				last_build != null ? last_build : "-",
				String.format("%s/versions.html", DOWNLOADS_URL));

			Object[] options = { CodeLab.LABEL("BTN_LATER"), CodeLab.LABEL("BTN_DOWNLOAD") };
			ImageIcon icon = ResourceUtils.loadImageIcon("icons/icon_codelab.png");

			int choice = JOptionPane.showOptionDialog(
				CodeLab.FRAME,
				new JEditorPaneWithLink(msg),
				CodeLab.LABEL("VERSION_TITLE"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				icon,
				options,
				options[1]);

			// Si l'utilisateur choisit "Télécharger" (index 1)
			if (choice == 1) {
				Utils.openBrowser(String.format(DOWNLOADS_PAGE_URL, LANG));
			}
			return 0;
		}
		else {
			System.out.println("NONE"); // Pas de nouvelle version
			return 1;
		}
	}

	///////////////////////////////////////////////////
	// Vérification des langages de programmation
	///////////////////////////////////////////////////

	private static final String DISABLED = "DISABLED";

	public static String GCC_CMD;
	public static String HASKELL_CMD;
	public static String GOLANG_CMD;
	public static String PYTHON_CMD;
	public static String CLIPS_CMD;

	public static String JAVA_HOME_USER;
	public static String JAVA_CMD;
	public static String JAVAC_CMD;

	public static void checkProgramingLanguages() {
		// Propriétés dans user.properties
		GCC_CMD = CodeLab.PROP("GCC_CMD");
		HASKELL_CMD = CodeLab.PROP("HASKELL_CMD");
		GOLANG_CMD = CodeLab.PROP("GOLANG_CMD");
		PYTHON_CMD = CodeLab.PROP("PYTHON_CMD");
		JAVA_HOME_USER = CodeLab.PROP("JAVA_HOME");
		// Propriétés dans sysconfig.properties
		CLIPS_CMD = String.format("%s/%s", BASE, CodeLab.PROP("CLIPS_CMD"));

		if (!JAVA_HOME_USER.isBlank() && !JAVA_HOME_USER.equals("undefined") && JAVA_HOME_USER != null) {
			// On remplace le JAVA_HOME par le JAVA_HOME_USER spécifié dans user.properties
			JAVA_HOME = JAVA_HOME_USER;
		}
		JAVA_CMD = JAVA_HOME + "/bin/java";
		JAVAC_CMD = JAVA_HOME + "/bin/javac";
		System.out.println("   Programming language commands (configured in user.properties):");
		System.out.println("      GCC_CMD=" + GCC_CMD);
		System.out.println("      GOLANG_CMD=" + GOLANG_CMD);
		System.out.println("      PYTHON_CMD=" + PYTHON_CMD);
		System.out.println("      HASKELL_CMD=" + HASKELL_CMD);
		System.out.println("      CLIPS_CMD=" + CLIPS_CMD);
		System.out.println("      JAVA_HOME=" + JAVA_HOME);

		WaitingDialog.open("Checking programming languages...");
		System.out.println("   Checking languages...");
		check_C();
		check_GO();
		check_Java();
		check_Python();
		check_Processing();
		check_Haskell();
		check_CLIPS();
		Utils.wait(1000);
		WaitingDialog.close();

		console.focus(); // Flusher la console
	}

	// ------------------------------------------------
	// Vérification du langage C

	private static void check_C() {
		System.out.print("      Language C... ");
		directPrint("[codelab] " + LABEL("CHECK_CC"), Console.COLOR_LOG);
		if (GCC_CMD.equals(DISABLED)) {
			System.out.println(DISABLED);
			directPrintln(DISABLED, Console.COLOR_LOG);
			return;
		}
		String cmd = GCC_CMD + " --version";
		String res = Utils.execute(cmd);
		if (res == null || res.contains("error")) {
			System.out.println("Can't find gcc");
			String msg = String.format(LABEL("ERROR_CC"), CodeLab.PROP("URL_INSTALL_C"));
			directPrintln(msg, Console.COLOR_LOG);
			GCC_CMD = null; // Pour le compilateur
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	// ------------------------------------------------
	// Vérification du langage Go

	private static void check_GO() {
		System.out.print("      Language Go... ");
		directPrint("[codelab] " + LABEL("CHECK_GO"), Console.COLOR_LOG);
		if (GOLANG_CMD.equals(DISABLED)) {
			System.out.println(DISABLED);
			directPrintln(DISABLED, Console.COLOR_LOG);
			return;
		}
		String cmd = GOLANG_CMD + " version";
		String res = Utils.execute(cmd);
		if (res == null) {
			System.out.println("Can't find go");
			String msg = String.format(LABEL("ERROR_GO"), CodeLab.PROP("URL_INSTALL_GO"));
			directPrintln(msg, Console.COLOR_LOG);
			GOLANG_CMD = null; // Pour le compilateur
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	// ------------------------------------------------
	// Vérification du langage Haskell

	private static void check_Haskell() {
		System.out.print("      Language Haskell... ");
		directPrint("[codelab] " + LABEL("CHECK_HASKELL"), Console.COLOR_LOG);
		if (HASKELL_CMD.equals(DISABLED)) {
			System.out.println(DISABLED);
			directPrintln(DISABLED, Console.COLOR_LOG);
			return;
		}
		String cmd = HASKELL_CMD + " --version";
		String res = Utils.execute(cmd);
		if (res == null || res.contains("error")) {
			System.out.println("Can't find Haskell");
			String msg = String.format(LABEL("ERROR_HASKELL"), CodeLab.PROP("URL_INSTALL_HASKELL"));
			directPrintln(msg, Console.COLOR_LOG);
			HASKELL_CMD = null; // Pour le compilateur
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	// ------------------------------------------------
	// Vérification du langage Java

	private static void check_Java() {
		System.out.print("      Language Java... ");
		directPrint("[codelab] " + LABEL("CHECK_JAVA"), Console.COLOR_LOG);
		String cmd = JAVAC_CMD + " --version";
		String res = Utils.execute(cmd);
		if (res == null) {
			System.out.println("Can't find javac");
			directPrintln(LABEL("ERROR_JAVA"), Console.COLOR_LOG);
			JAVA_HOME = null; // Pour le compilateur
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	// ------------------------------------------------
	// Vérification du langage Processing

	private static final String processing_vers = "Processing version 4.3";

	private static void check_Processing() {
		System.out.println("      Language Processing... OK -> " + processing_vers);
		String msg = String.format("[codelab] %sOK -> %s\n", LABEL("CHECK_PROCESSING"), processing_vers);
		directPrint(msg, Console.COLOR_LOG);
	}

	// ------------------------------------------------
	// Vérification du langage CLIPS

	private static void check_CLIPS() {
		System.out.print("      Language CLIPS... ");
		directPrint("[codelab] " + LABEL("CHECK_CLIPS"), Console.COLOR_LOG);
		String cmd = String.format("%s -f2 %s/clips/__version.clp", CLIPS_CMD, INCLUDES_FOLDER);
		String res = Utils.execute(cmd);
		if (res == null) {
			System.out.println("Can't find CLIPS");
			directPrintln(LABEL("ERROR_CLIPS"), Console.COLOR_LOG);
			CLIPS_CMD = null; // Pour l'exécuteur
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	// ------------------------------------------------
	// Vérification du langage Python

	private static void check_Python() {
		System.out.print("      Language Python... ");
		directPrint("[codelab] " + LABEL("CHECK_PYTHON"), Console.COLOR_LOG);
		if (PYTHON_CMD.equals(DISABLED)) {
			System.out.println(DISABLED);
			directPrintln(DISABLED, Console.COLOR_LOG);
			return;
		}
		String cmd = PYTHON_CMD + " --version";
		String res = Utils.execute(cmd);
		if (res == null) {
			System.out.println("Can't find Python");
			String msg = String.format(LABEL("ERROR_PYTHON1"), CodeLab.PROP("URL_INSTALL_PYTHON"));
			directPrintln(msg, Console.COLOR_LOG);
			PYTHON_CMD = null; // Pour l'exécuteur
		} else {
			if (noPython3()) res = "WARNING -> " + res.split("\n")[0];
			else res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
			// Vérifier les librairie
			check_Numpy();
			check_Matplotlib();
		}
	}

	private static boolean noPython3() {
		String cmd = String.format("%s -u \"%s/python/__version.py\"", PYTHON_CMD, INCLUDES_FOLDER);
		return Utils.execute2(cmd) == 1;
	}

	// ------------------------------------------------
	// Vérification des librairies Python

	private static void check_Numpy() {
		System.out.print("      Library Numpy... ");
		directPrint("[codelab] " + LABEL("CHECK_NUMPY"), Console.COLOR_LOG);
		String cmd = String.format("%s -u \"%s/python/__numpy.py\"", PYTHON_CMD, INCLUDES_FOLDER);
		String res = Utils.execute(cmd);
		if (res == null || res.trim().isEmpty() || res.trim().equals("ERROR") || res.contains("ERROR")) {
			System.out.println("Numpy package not installed");
			String msg = String.format(LABEL("ERROR_NUMPY"), CodeLab.PROP("URL_INSTALL_NUMPY"));
			directPrintln(msg, Console.COLOR_LOG);
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	private static void check_Matplotlib() {
		System.out.print("      Library Matplotlib... ");
		directPrint("[codelab] " + LABEL("CHECK_MATPLOTLIB"), Console.COLOR_LOG);
		String cmd = String.format("%s -u \"%s/python/__matplotlib.py\"", PYTHON_CMD, INCLUDES_FOLDER);
		String res = Utils.execute(cmd);
		if (res == null || res.trim().isEmpty() || res.trim().equals("ERROR") || res.contains("ERROR")) {
			System.out.println("Matplotlib package not installed");
			String msg = String.format(LABEL("ERROR_MATPLOTLIB"), CodeLab.PROP("URL_INSTALL_MATPLOTLIB"));
			directPrintln(msg, Console.COLOR_LOG);
		} else {
			res = "OK -> " + res.split("\n")[0];
			System.out.println(res);
			directPrintln(res, Console.COLOR_LOG);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes statiques publiques
	///////////////////////////////////////////////////

	public static String getUserDataFolder() {
		// Pas dans AbstractCodeLab car défini dans CodeLab pour les modules
		return USER_DATA_FOLDER;
	}

	public static void playAudio(String filename) {
		if (SOUND_EFFECTS)
			new MiniPlayer_v1(filename);
	}

	public static void logger(String msg) {
		System.out.format("[%s] %s\n", Utils.getDate(), msg);
	}

	public static void saveCurrentFile() {
		if (CodeLab.INSTANCE == null) return;
		if (CodeLab.INSTANCE.getEditor() == null) return;
		CodeLab.INSTANCE.getEditor().saveCurrentFile();
	}

	public static void saveAllFiles() {
		if (CodeLab.INSTANCE == null) return;
		if (CodeLab.INSTANCE.getEditor() == null) return;
		CodeLab.INSTANCE.getEditor().saveAllFiles();
	}

	///////////////////////////////////////////////////
	// Méthodes statiques privées
	///////////////////////////////////////////////////
	 
	private static void verifyCodelabRunning() {
		// Le processsus courant et son PID
		ProcessHandle handle = ProcessHandle.current();
		long currentPID = handle.pid();
		// Itérer sur tous les processus
		Stream<ProcessHandle> liveProcesses = ProcessHandle.allProcesses();
		liveProcesses.filter(ProcessHandle::isAlive).forEach(process -> {
			System.out.format("Process pid=%d info=%s\n", process.pid(), process.info());
			if (process.info().toString().contains("codelab") && process.pid() != currentPID) {
				// Une autre instance de CodeLab est en cours d'exécution
				String msg = String.format(LABEL("CODELAB_ERR6"), process.pid());
				int res = JOptionPane.showConfirmDialog(null, msg, "CODELAB WARNING",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					CodeLab.ICON);
				if (res == JOptionPane.YES_OPTION) {
					Optional<ProcessHandle> optionalProcessHandle = ProcessHandle.of(process.pid());
					optionalProcessHandle.ifPresent(processHandle -> processHandle.destroy());
				} else {
					// On autorise une seconde instance de CodeLab pour les tests en mode connecté
					// On défini un autre emplacement pour les les fichiers distants
					// Attention: doit impérativement se terminer par /dist
					PROG_DIST_FOLDER = HOME + "/codelab_files/programs/dist";
					File file = new File(PROG_DIST_FOLDER);
					file.mkdirs();
				}
			}
		});
	}

	private static void setLanguage() {
		System.out.print("   Setting application language... ");
		String lang = CodeLab.PROP("LANG");
		if (lang.equals("SYSTEM")) {
			System.out.println(LANG); // Défini dans AbstractCodeLab
			return;
		}
		if (implemented(lang)) {
			LANG = lang;
			System.out.println(LANG);
		} else {
			LANG = "en";
			System.out.format("en (%s not implemented)\n", lang);
		}
	}

	private static void setIconImage(JFrame frame, String imageName) {
		Image image = ResourceUtils.loadImageIcon(imageName).getImage();
		frame.setIconImage(image); // For Windows and Linux
		if (IS_OSX) {
			try {
				// For MacOSX since JDK 9x
				Taskbar taskbar = Taskbar.getTaskbar();
				taskbar.setIconImage(image);
			}
			catch (UnsupportedOperationException e) {}
			catch (SecurityException e) {}
		}
	}

	private static String FLATLIGHTLAF = "flatlight";
	private static void setLookAndFeel(String name) {
		try {
			switch (name) {
				case "metal": UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); break;
				case "motif": UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); break;
				case "system": UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); break;
				case "flatlight": UIManager.setLookAndFeel(new FlatLightLaf()); break;
				case "flatdark": UIManager.setLookAndFeel(new FlatDarkLaf()); break;
				default: UIManager.setLookAndFeel(new FlatLightLaf()); break;
			}
		}
		catch (Exception e) {
			ExceptionManager.process(e);
			CodeLab.exit();
		}
	}

	private static void setLogFile(String filename) {
		try {
			File file = new File(filename);
			PrintStream out = new PrintStream(file);
			System.setOut(new PrintStream(out));
			System.setErr(new PrintStream(out));
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	private static void restoreUserDirectory() {
		// Choses à sauvegarder
		String BACKUP1 = HOME + "/BACKUP1";
		String BACKUP2 = HOME + "/BACKUP2";
		String BACKUP3 = HOME + "/BACKUP3";
		String BACKUP4 = HOME + "/BACKUP4";
		MyFileUtils.copyFolder(PROG_LOCAL_FOLDER, BACKUP1);
		MyFileUtils.copyFolder(MODULES_FOLDER, BACKUP2);
		MyFileUtils.copyFolder(USER_GRAPH_FOLDER, BACKUP3);
		MyFileUtils.copyFolder(USER_ROBOT_FOLDER, BACKUP4);

		// Supprimer l'actuel dossier codelab.files
		MyFileUtils.delete(CODELAB_FILES);
		// Restaurer le nouveau dossier codelab.files
		String temp = CODELAB_FILES + ".zip";
		MyFileUtils.copy(CODELAB_FILES_BAK, temp);
		MyFileUtils.unzip(temp);
		MyFileUtils.delete(temp);

		// Choses à restaurer
		MyFileUtils.copyFolder(BACKUP1, PROG_LOCAL_FOLDER);
		MyFileUtils.copyFolder(BACKUP2, MODULES_FOLDER);
		MyFileUtils.copyFolder(BACKUP3, USER_GRAPH_FOLDER);
		MyFileUtils.copyFolder(BACKUP4, USER_ROBOT_FOLDER);
		MyFileUtils.delete(BACKUP1);
		MyFileUtils.delete(BACKUP2);
		MyFileUtils.delete(BACKUP3);
		MyFileUtils.delete(BACKUP4);
	}

	///////////////////////////////////////////////////
	// Méthode statique main
	///////////////////////////////////////////////////

	public static void main(String[] argv) {

		// ------------------------------------------------
		// Paramètres de la ligne de commande

		ARGV = argv;
		for (String arg: argv) {
			if (arg.equals("--log")) REDIRECT = true; // Rediriger la sortie standard vers un fichier de log
			if (arg.equals("--auto")) AUTOMATOR = true; // Exécuter l'automate de saisie dans l'éditeur de textes
			if (arg.equals("--test")) TEST_REPORTING = true; // Ajouter un item Test reporting au menu CodeLab
			if (arg.equals("--load")) FORCE_DOWNLOAD = true; // Forcer une mise-à-jour depuis le site de CodeLab
			if (arg.equals("--restore")) FORCE_RESTORE = true; // Forcer la réinitialisation de codelab.files
			if (arg.equals("--relauch")) VERIFY_RUNNING = false; // Inhiber la vérification d'autres instances
		}

		// ------------------------------------------------
		// Vérifier si les chemins comportent des espaces

		File codelab_home = new File(CodeLab.BASE);
		if (codelab_home.toString().contains(" ")) {
			JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR1"), "CODELAB ERROR", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			CodeLab.exit();
		}

		// ------------------------------------------------
		// Vérifier si une instance de CodeLab est en cours d'exécution

		if (VERIFY_RUNNING) verifyCodelabRunning();

		// ------------------------------------------------
		// Supprimer si demandé le dossier codelab.files

		if (FORCE_RESTORE) MyFileUtils.delete(CODELAB_FILES);

		// ------------------------------------------------
		// (Ré-)installer si besoin le dossier codelab.files

		boolean first_run = false;
		File file = new File(CODELAB_FILES);
		if (!file.exists()) {
			JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR2"), "CODELAB INFORMATION", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			// Créer un dossier codelab.files
			String temp = CODELAB_FILES + ".zip";
			MyFileUtils.copy(CODELAB_FILES_BAK, temp);
			MyFileUtils.unzip(temp);
			MyFileUtils.delete(temp);
			first_run = true; // Pour inhiber la vérification de la version de codelab.files
		}

		// ------------------------------------------------
		// Absence du dossier codelab.files/.hidden

		File file1 = new File(CODELAB_FILES_HIDDEN);
		if (!file1.exists()) {
			JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR3"), "CODELAB ERROR", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			CodeLab.exit();
		}

		// ------------------------------------------------
		// Réinitialiser quelques fichiers

		MyFileUtils.touch(new File(LOGFILE));
		MyFileUtils.touch(new File(INFOFILE));

		// ------------------------------------------------
		// Redirection de la sortie standard

		if (REDIRECT) setLogFile(LOGFILE);

		// ------------------------------------------------
		// Stocker quelques infos (dont le JAVA_HOME transmis par le launcher)

		MyFileUtils.writeln(new File(INFOFILE),
			String.format("JAVA_HOME=%s\nVERSION=%s\nBUILD=%s",
			JAVA_HOME, VERSION, BUILD));

		// ------------------------------------------------
		// Quelques affichages dans le log

		logger("Starting CodeLab...");
		System.out.format("   Release: %s build %s\n", VERSION, BUILD);
		System.out.format("   Java: %s %s\n", System.getProperty("java.vm.vendor"), System.getProperty("java.version"));
		System.out.format("   System: %s architecture %s (%s cores)\n", System.getProperty("os.name"), ARCHITECTURE, Runtime.getRuntime().availableProcessors());
		System.out.format("   Screen size: %s\n", Utils.getScreenSize());
		System.out.format("   Max memory: %d bytes\n", MAX_MEMORY == Long.MAX_VALUE ? "no limit" : MAX_MEMORY);
		System.out.format("   Free memory: %d bytes\n", Runtime.getRuntime().freeMemory());
		System.out.format("   Available to Java: %d bytes\n", Runtime.getRuntime().totalMemory());
		System.out.format("   Current IP address: %s\n", IP_ADDR);
		System.out.format("   Host name: %s\n", HOST_NAME);
		System.out.format("   Home directory: %s\n", HOME);
		System.out.format("   User directory: %s\n", CODELAB_FILES);
		System.out.format("   CodeLab directory: %s\n", BASE);
		System.out.format("   Program directory: %s\n", PROG_LOCAL_FOLDER);
		System.out.format("   JAVA_HOME=%s\n", JAVA_HOME);
		System.out.format("   LAUNCHER=%s\n", LAUNCHER);
		System.out.format("   PATH=%s\n", PATH);
		System.out.format("   ARGV=%s\n", Arrays.toString(argv));

		// ------------------------------------------------
		// Initialiser le répertoire temp

		Utils.initFolder(new File(TEMP_FOLDER));

		// ------------------------------------------------
		// Réinitialiser le répertoire includes

		System.out.println("   Restoring the hidden includes folder... DONE");
		MyFileUtils.delete(new File(INCLUDES_FOLDER));
		MyFileUtils.copyFolder(INCLUDES_FOLDER + "_init", INCLUDES_FOLDER);

		// ------------------------------------------------
		// Réinitialiser le répertoire templates

		System.out.println("   Restoring the hidden templates folder... DONE");
		MyFileUtils.delete(new File(TEMPLATES_FOLDER));
		MyFileUtils.copyFolder(TEMPLATES_FOLDER + "_init", TEMPLATES_FOLDER);

		// ------------------------------------------------
		// Chargement des propriétés

		System.out.println("   Loading properties...");
		PropertyBase.load();
		loadHiddenProperties(); // Les pptés cachées dans .hidden

		// Configuration du proxy
		PROXY_HOST = CodeLab.PROP("PROXY_HOST");
		PROXY_PORT = CodeLab.PROP("PROXY_PORT");
		PROXY_CONFIGURED = (PROXY_HOST.equals("NO") || PROXY_HOST.equals("NO_PROXY")) ? false : true;
		if (PROXY_CONFIGURED) {
			System.setProperty("http.proxyHost", PROXY_HOST);
			System.setProperty("http.proxyPort", PROXY_PORT);
		}

		// Chargement des propriétés distantes
		String timeout = CodeLab.PROP("TIME_OUT");
		TIME_OUT = timeout.equals(UNDEFINED) ? 5000 : Integer.valueOf(timeout);
		PropertyBase.download(CodeLab.PROP("URL_PROP"), TIME_OUT);
		//PropertyBase.print();

		// ------------------------------------------------
		// Initialisation des variables-propriétés

		USER = CodeLab.PROP("USER"); if (USER.equals(UNDEFINED) || USER.isBlank()) USER = "John Doe";
		SYNTH_OSC = CodeLab.PROP("SYNTH_OSC"); if (SYNTH_OSC.equals(UNDEFINED) || SYNTH_OSC.isBlank()) SYNTH_OSC = "TRIANGLE";
		FONT_NAME = CodeLab.PROP("FONT_NAME"); if (FONT_NAME.equals(UNDEFINED) || FONT_NAME.isBlank()) FONT_NAME = "Monospaced";
		FONT_SIZE = PropertyBase.getIntegerProperty("FONT_SIZE"); if (FONT_SIZE == -1) FONT_SIZE = 14;
		BUFFER_SIZE = PropertyBase.getIntegerProperty("BUFFER_SIZE"); if (BUFFER_SIZE == -1) BUFFER_SIZE = 200;
		CONSOLE_SIZE = PropertyBase.getIntegerProperty("CONSOLE_SIZE"); if (CONSOLE_SIZE == -1) CONSOLE_SIZE = 250;
		MAX_FILE_SIZE = PropertyBase.getIntegerProperty("MAX_FILE_SIZE"); if (MAX_FILE_SIZE == -1) MAX_FILE_SIZE = 500;
		COMPILE_TIMEOUT = PropertyBase.getIntegerProperty("COMPILE_TIMEOUT"); if (COMPILE_TIMEOUT == -1) COMPILE_TIMEOUT = 5;
		PROGRAM_TIMEOUT = PropertyBase.getIntegerProperty("PROGRAM_TIMEOUT"); if (PROGRAM_TIMEOUT == -1) PROGRAM_TIMEOUT = 60;
		SERVER_HOST = CodeLab.PROP("SERVER_HOST"); if (SERVER_HOST.equals(UNDEFINED)) SERVER_HOST = "";
		SERVER_PORT = CodeLab.PROP("SERVER_PORT"); if (SERVER_PORT.equals(UNDEFINED)) SERVER_PORT = "";
		LOCAL_PORT = CodeLab.PROP("LOCAL_PORT"); if (LOCAL_PORT.equals(UNDEFINED) || LOCAL_PORT.isBlank()) LOCAL_PORT = "8500";
		if (!SERVER_HOST.isBlank() && !SERVER_PORT.isBlank()) {
			try {
				int sPort = Integer.parseInt(SERVER_PORT);
				ADMIN_URL = String.format("http://%s:%d", SERVER_HOST, sPort + 1);
			} catch (NumberFormatException e) {
				ADMIN_URL = "";
			}
		} else {
			ADMIN_URL = "";
		}
		CONNECTED_MODE = PropertyBase.getBooleanProperty("CONNECTED_MODE");
		REPORT_EXCEPTIONS = PropertyBase.getBooleanProperty("REPORT_EXCEPTIONS");
		RETE_FLAG = PropertyBase.getBooleanProperty("RETE_FLAG");
		PRINT_FACTS = PropertyBase.getBooleanProperty("PRINT_FACTS");

		
		// ------------------------------------------------
		// Vérification de la version de CodeLab

		if (PropertyBase.getBooleanProperty("CHECK_VERSION")) checkVersion(false);

		// ------------------------------------------------
		// Vérification du dossier codelab.files

		int previous_major = Utils.getVersionPart(DATA_VERSION, 1);
		boolean codelab_files_obsolete = (DATA_VERSION.equals(UNDEFINED) || previous_major < 1); // Condition d'obsolescence

		if (codelab_files_obsolete && !first_run) {
			JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR4"), "CODELAB INFORMATION", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			restoreUserDirectory(); // Patcher le dossier codelab.files
			saveHiddenProperties(); // Sauvegarder la ppté DATA_VERSION
			relaunch(); // Relancer CodeLab
		}

		// ------------------------------------------------
		// Vérification de la connectivité

		System.out.format("   Connected mode: %s (configured in user.properties)\n", CONNECTED_MODE ? "ENABLED" : "DISABLED");

		String server_msg_log = "   Connected mode disabled (edit properties to use connected mode)";
		String server_msg_term = LABEL("SERVER_DISABLED");

		if (CONNECTED_MODE) {
			// Vérification de la connectivité
			String host = CodeLab.PROP("SERVER_HOST");
			int port = PropertyBase.getIntegerProperty("SERVER_PORT");
			WRK_SERVER_OK = Utils.checkServer(host, port, TIME_OUT);

			if (host.isBlank() || host.equals("undefined")) {
				server_msg_log = "   No file server configured in user.properties";
				server_msg_term = LABEL("SERVER_NOSERVER");
			} else if (WRK_SERVER_OK) {
				server_msg_log = String.format("   File server (%s:%d)... OK", host, port);
				server_msg_term = String.format(LABEL("SERVER_OK"), host, port);
			} else {
				server_msg_log = String.format("   File server (%s:%d)... ERROR check your Internet connectivity", host, port);
				server_msg_term = String.format(LABEL("SERVER_ERR"), host, port);
			}
			System.out.println(server_msg_log);
		}

		// ------------------------------------------------
		// LookAndFeel et langue de CodeLab

		setLookAndFeel(FLATLIGHTLAF);
		setLanguage();

		// ------------------------------------------------
		// Instanciation de la fenêtre principale

		new SplashWindow(SPLASH_IMAGE, SPLASH_INFO);

		try {
			FRAME = new CodeLab();
			INSTANCE = (CodeLab) FRAME;
		}
		catch (Exception e) {
			// Une erreur s'est produite dans le constructeur !!
			SplashWindow.close();
			ExceptionManager.process(e);
			JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR5"), "CODELAB FATAL ERROR", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			CodeLab.exit();
		}
		SplashWindow.close();

		// ------------------------------------------------
		// Vérification des langages de programmation

		checkProgramingLanguages();

		// ------------------------------------------------
		// Shutdown hook

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				CodeLab.logger("Execute shutdownHook...");
				if (CodeLab.INSTANCE == null) return;
				CodeLab.INSTANCE.things_to_do_before_exiting();
				//captureThreadDump("Shutdown Hook");
			}
		});

		// ------------------------------------------------
		// Dernières petites choses

		MiniSynth.reset(SYNTH_OSC); // Pour ne pas avoir de latence à la première note

		directPrintln("[codelab] " + server_msg_term, Console.COLOR_LOG); // État du serveur de fichiers

		new SwingWatchdog().start(); // Le watchdog de détection des blocages Swing tourne en arrière plan
		directPrintln("[codelab] The Swing Watchdog is launched as a background process", Console.COLOR_LOG);
		directPrintln("[codelab] Tip: Use 'pkill -9 -fi codelab' in case of panic", Console.COLOR_LOG);

		String msg = String.format("CodeLab %s build %s (%s) is ready!", VERSION, BUILD, ARCHITECTURE);
		directPrintln("[codelab] " + msg, Console.COLOR_LOG);
		
		Utils.wait(1000);
		WaitingDialog.close();
		logger(msg);
	}
}

package codelab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
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
import codelab.finders.CFinder;
import codelab.finders.GoFinder;
import codelab.finders.HaskellFinder;
import codelab.finders.PythonFinder;
import codelab.modules.editeur.manager.FileManager;
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

import org.apache.commons.io.FileUtils;

import net.lingala.zip4j.ZipFile;

/**
*	Classe principale de CodeLab
*	@author Jérôme Lehuen
*	@version 12/10/26
*/

public class CodeLab extends AbstractCodeLab {

	public static JFrame FRAME = null;
	public static CodeLab INSTANCE = null;
	private static final List<File> PENDING_OPEN_FILES = Collections.synchronizedList(new ArrayList<>());

	static {
		setupDesktopHandlers();
	}

	private static void setupDesktopHandlers() {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
					desktop.setOpenFileHandler(e -> {
						List<File> files = e.getFiles();
						logger("Desktop openFiles received: " + files);
						if (files != null) {
							for (File f : files) {
								if (INSTANCE != null) {
									SwingUtilities.invokeLater(() -> INSTANCE.handleDroppedFile(f));
								} else {
									PENDING_OPEN_FILES.add(f);
								}
							}
						}
					});
					logger("Desktop OpenFileHandler registered");
				} else {
					logger("Desktop APP_OPEN_FILE not supported");
				}

				if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
					desktop.setOpenURIHandler(e -> {
						URI uri = e.getURI();
						logger("Desktop openURI received: " + uri);
						if (uri != null) {
							try {
								File f = new File(uri);
								if (INSTANCE != null) {
									SwingUtilities.invokeLater(() -> INSTANCE.handleDroppedFile(f));
								} else {
									PENDING_OPEN_FILES.add(f);
								}
							} catch (Exception ex) {
								logger("Desktop openURI conversion error: " + ex.getMessage());
							}
						}
					});
					logger("Desktop OpenURIHandler registered");
				}

				if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
					desktop.setQuitHandler((e, response) -> {
						logger("Desktop quit request received");
						SwingUtilities.invokeLater(() -> {
							try {
								if (INSTANCE != null) {
									INSTANCE.things_to_do_before_exiting();
								}
								response.performQuit();
							} catch (Throwable t) {
								logger("Desktop QuitHandler error: " + t.getMessage());
								response.cancelQuit();
							}
						});
					});
					logger("Desktop QuitHandler registered");
				}
			} else {
				logger("Desktop is not supported on this platform");
			}
		} catch (Throwable t) {
			logger("Error initializing Desktop handlers: " + t.getMessage());
		}
	}

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

		// Listener de redimensionnement
		getRootPane().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				for (AbstractModule module : module_list)
					module.updateDividersLocation();
			}
		});

		// Enregistrement continu de la position et de la taille de la fenêtre
		addComponentListener(new ComponentAdapter() {
			public void componentMoved(ComponentEvent e) {
				updateSizeProperties();
			}
			public void componentResized(ComponentEvent e) {
				updateSizeProperties();
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
	// Gestion du glisser-déposer sur l'icône de l'application
	///////////////////////////////////////////////////

	public void handleDroppedFile(File file) {
		logger("handleDroppedFile: " + (file != null ? file.getAbsolutePath() : "null"));
		if (file == null || !file.exists()) {
			logger("handleDroppedFile: file does not exist or is null");
			return;
		}
		showEditor();
		toFront();
		requestFocus();
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
				Desktop.getDesktop().requestForeground(true);
			}
		} catch (Exception ignored) {}

		FileManager manager = editor != null ? editor.getManager() : null;
		if (manager == null) {
			logger("handleDroppedFile: manager is null");
			return;
		}

		if (manager.getMode() != FileManager.MODE_PROGRAMS) {
			manager.restoreProgramsBase();
		}

		// Si le fichier se trouve déjà dans l'arborescence des programmes, on le sélectionne
		if (manager.selectNodeFromFile(file)) {
			logger("handleDroppedFile: file already in tree, selected");
			return;
		}

		// Sinon, importation nominale du fichier déposé
		logger("handleDroppedFile: importing dropped file via manager.new_droppedFile");
		manager.new_droppedFile(file, null);
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

	public static File getExecutableStandardCssFile() {
		// 1. Dossier de l'exécutable (BASE)
		if (BASE != null && !BASE.trim().isEmpty()) {
			File f = new File(BASE, "report.css");
			if (f.exists() && f.length() > 0) return f;
			File fCodelab = new File(BASE, "codelab/report.css");
			if (fCodelab.exists() && fCodelab.length() > 0) return fCodelab;
		}
		// 2. Dossier RESSOURCES
		if (RESSOURCES != null && !RESSOURCES.trim().isEmpty()) {
			File fRsc = new File(RESSOURCES, "report.css");
			if (fRsc.exists() && fRsc.length() > 0) return fRsc;
			File fRscCodelab = new File(RESSOURCES, "codelab/report.css");
			if (fRscCodelab.exists() && fRscCodelab.length() > 0) return fRscCodelab;
		}
		// 3. Mode dev
		File devArm = new File("client/mac-app-arm/Contents/MacOS/report.css");
		if (devArm.exists() && devArm.length() > 0) return devArm;
		File devData = new File("client/data/report.css");
		if (devData.exists() && devData.length() > 0) return devData;
		return null;
	}

	public static File getIpynbCssFile() {
		initStylesFolder();

		// 1. Style sélectionné pour les fichiers ipynb
		String styleName = IPYNB_STYLE != null ? IPYNB_STYLE : "markdown.css";
		File f = getReportStyleFile(styleName);
		if (f != null && f.exists() && f.length() > 0) return f;

		// 2. Fichier markdown.css dans STYLES_FOLDER (~/codelab.files/.hidden/styles/markdown.css)
		File userFile = new File(MARKDOWN_CSS_FILE);
		if (userFile.exists() && userFile.length() > 0) {
			return userFile;
		}

		// 3. CSS standard dans le dossier de l'exécutable si le dossier styles est vide
		File stdCss = getExecutableStandardCssFile();
		if (stdCss != null && stdCss.exists() && stdCss.length() > 0) {
			return stdCss;
		}

		return userFile;
	}

	public static File getMarkdownCssFile() {
		if (INSTANCE != null && INSTANCE.getEditor() != null
				&& INSTANCE.getEditor().getEditor() instanceof codelab.modules.editeur.markdown.MarkdownViewer) {
			return getReportCssFile();
		}
		return getIpynbCssFile();
	}

	public void editMarkdownCssFile() {
		File userFile = getMarkdownCssFile();
		if (userFile != null && userFile.exists()) {
			editor.open_file(userFile, true);
			showEditor();
			client.setEditedFile(AbstractEditor.EMPTY);
		} else {
			consoleLog("WARNING: Can't find or create " + (userFile != null ? userFile.getAbsolutePath() : "markdown.css"));
		}
	}

	public static final String[] BUILTIN_REPORT_STYLES = {
		"Basic", "Clearness", "Clearness_Dark", "GitHub", "GitHub2", "GitHub_2020",
		"GitHub_Tomorrow", "Github2_Dark", "Gmail", "Google_Docs", "Markdown",
		"Solarized_Dark", "Solarized_Light"
	};

	public static void initStylesFolder() {
		File target = new File(STYLES_FOLDER);
		if (!target.exists()) target.mkdirs();
	}

	public static java.util.List<String> getAvailableReportStyles() {
		initStylesFolder();
		java.util.Set<String> styles = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		// 1. Scan des styles dans le dossier STYLES_FOLDER (~/codelab.files/.hidden/styles)
		File target = new File(STYLES_FOLDER);
		if (target.exists() && target.isDirectory()) {
			File[] files = target.listFiles((dir, name) -> name.endsWith(".css"));
			if (files != null) {
				for (File f : files) {
					String n = f.getName();
					styles.add(n.substring(0, n.length() - 4));
				}
			}
		}

		// 2. Mode dev : client/codelab.files/hidden/styles
		File devDir = new File("client/codelab.files/hidden/styles");
		if (devDir.exists() && devDir.isDirectory()) {
			File[] files = devDir.listFiles((dir, name) -> name.endsWith(".css"));
			if (files != null) {
				for (File f : files) {
					String n = f.getName();
					styles.add(n.substring(0, n.length() - 4));
				}
			}
		}

		// 3. Si le dossier est vide (aucun fichier CSS), repli sur le CSS standard du dossier de l'exécutable
		if (styles.isEmpty()) {
			File standardCss = getExecutableStandardCssFile();
			if (standardCss != null && standardCss.exists() && standardCss.length() > 0) {
				String n = standardCss.getName();
				styles.add(n.endsWith(".css") ? n.substring(0, n.length() - 4) : "report");
			} else {
				styles.add("report");
			}
		}

		return new java.util.ArrayList<>(styles);
	}

	public static File getReportStyleFile(String styleName) {
		if (styleName == null || styleName.trim().isEmpty()) {
			styleName = REPORT_STYLE != null ? REPORT_STYLE : "report.css";
		}
		final String targetCssName = styleName.endsWith(".css") ? styleName : styleName + ".css";

		// 1. Chercher dans STYLES_FOLDER (~/codelab.files/.hidden/styles)
		File f = new File(STYLES_FOLDER, targetCssName);
		if (f.exists() && f.length() > 0) return f;

		File stylesDir = new File(STYLES_FOLDER);
		if (stylesDir.exists() && stylesDir.isDirectory()) {
			File[] files = stylesDir.listFiles((dir, name) -> name.equalsIgnoreCase(targetCssName));
			if (files != null && files.length > 0 && files[0].length() > 0) return files[0];
		}

		// 2. Mode dev : client/codelab.files/hidden/styles
		File dev = new File("client/codelab.files/hidden/styles", targetCssName);
		if (dev.exists() && dev.length() > 0) return dev;
		File devDir = new File("client/codelab.files/hidden/styles");
		if (devDir.exists() && devDir.isDirectory()) {
			File[] files = devDir.listFiles((dir, name) -> name.equalsIgnoreCase(targetCssName));
			if (files != null && files.length > 0 && files[0].length() > 0) return files[0];
		}

		// 3. CSS standard dans le dossier de l'exécutable si dossier vide ou demandé
		File standardCss = getExecutableStandardCssFile();
		if (standardCss != null && standardCss.exists() && standardCss.length() > 0) {
			if (styleName.equalsIgnoreCase("report") || styleName.equalsIgnoreCase("standard")
				|| (stylesDir.exists() && (stylesDir.list() == null || stylesDir.list().length == 0))) {
				return standardCss;
			}
		}

		// 4. Repli sur le CSS standard de l'exécutable s'il existe
		if (standardCss != null && standardCss.exists() && standardCss.length() > 0) {
			return standardCss;
		}

		return null;
	}

	public static File getReportCssFile() {
		initStylesFolder();
		File f = getReportStyleFile(REPORT_STYLE);
		if (f != null && f.exists() && f.length() > 0) return f;

		// CSS standard dans le dossier de l'exécutable
		File std = getExecutableStandardCssFile();
		if (std != null && std.exists() && std.length() > 0) return std;

		return null;
	}

	public void setReportStyle(String styleName) {
		if (styleName == null || styleName.trim().isEmpty()) return;
		REPORT_STYLE = styleName;
		saveHiddenProperties();

		// Nettoyer les caches CSS pour actualisation instantanée des Notebooks et MarkdownViewer
		codelab.modules.editeur.notebook.NotebookCellPane.clearCssCache();

		// Si l'éditeur actif est un MarkdownViewer, réactualiser l'affichage
		if (editor != null && editor.getEditor() instanceof codelab.modules.editeur.markdown.MarkdownViewer) {
			((codelab.modules.editeur.markdown.MarkdownViewer) editor.getEditor()).renderMarkdown();
		}
	}

	public void setIpynbStyle(String styleName) {
		if (styleName == null || styleName.trim().isEmpty()) return;
		IPYNB_STYLE = styleName;
		saveHiddenProperties();

		// Nettoyer les caches CSS pour actualisation instantanée des Notebooks
		codelab.modules.editeur.notebook.NotebookCellPane.clearCssCache();

		// Si l'éditeur actif est un NotebookEditor, réactualiser l'affichage
		if (editor != null && editor.getEditor() instanceof codelab.modules.editeur.notebook.NotebookEditor) {
			((codelab.modules.editeur.notebook.NotebookEditor) editor.getEditor()).refreshMarkdownViews();
		}
	}

	public void searchGccCompiler() {
		new CFinder().searchAndConfigure(this);
	}

	public void searchGoCompiler() {
		new GoFinder().searchAndConfigure(this);
	}

	public void searchHaskellCompiler() {
		new HaskellFinder().searchAndConfigure(this);
	}

	public void searchPythonInterpreter() {
		new PythonFinder().searchAndConfigure(this);
	}

	///////////////////////////////////////////////////
	// Gestion dynamique du proxy
	///////////////////////////////////////////////////

	public static boolean isProxyEnabled() {
		return PROXY_CONFIGURED;
	}

	public static void initProxySettings() {
		PROXY_HOST = CodeLab.PROP("PROXY_HOST");
		PROXY_PORT = CodeLab.PROP("PROXY_PORT");
		if (PROXY_PORT == null || PROXY_PORT.isBlank() || PROXY_PORT.equals(UNDEFINED)) {
			PROXY_PORT = "3128";
		}

		// CodeLab démarre systématiquement sans proxy
		PROXY_CONFIGURED = false;
		System.clearProperty("http.proxyHost");
		System.clearProperty("http.proxyPort");
		System.clearProperty("https.proxyHost");
		System.clearProperty("https.proxyPort");
	}

	public static void reloadProxySettings() {
		String host = CodeLab.PROP("PROXY_HOST");
		String port = CodeLab.PROP("PROXY_PORT");
		String proxyEnabledProp = CodeLab.PROP("PROXY_ENABLED");

		boolean shouldEnable = false;
		if (proxyEnabledProp != null && !proxyEnabledProp.equals(UNDEFINED) && !proxyEnabledProp.isBlank()) {
			shouldEnable = PropertyBase.getBooleanProperty("PROXY_ENABLED");
		}

		if (shouldEnable && host != null && !host.isBlank() 
			&& !host.equalsIgnoreCase("NO") 
			&& !host.equalsIgnoreCase("NO_PROXY") 
			&& !host.equalsIgnoreCase("NONE") 
			&& !host.equals(UNDEFINED)) {
			PROXY_HOST = host;
			PROXY_PORT = (port != null && !port.isBlank() && !port.equals(UNDEFINED)) ? port : "3128";
			PROXY_CONFIGURED = true;
			System.setProperty("http.proxyHost", PROXY_HOST);
			System.setProperty("http.proxyPort", PROXY_PORT);
			System.setProperty("https.proxyHost", PROXY_HOST);
			System.setProperty("https.proxyPort", PROXY_PORT);
		} else {
			PROXY_CONFIGURED = false;
			System.clearProperty("http.proxyHost");
			System.clearProperty("http.proxyPort");
			System.clearProperty("https.proxyHost");
			System.clearProperty("https.proxyPort");
		}

		if (CodeLabMenu.itemEnableProxy != null && CodeLabMenu.itemEnableProxy.isSelected() != PROXY_CONFIGURED) {
			CodeLabMenu.itemEnableProxy.setSelected(PROXY_CONFIGURED);
		}
	}

	public static void setProxyEnabled(boolean enable) {
		setProxyEnabled(enable, false, true);
	}

	public static void setProxyEnabled(boolean enable, boolean saveProperty, boolean notify) {
		if (enable) {
			PropertyBase.load();
			String host = CodeLab.PROP("PROXY_HOST");
			String port = CodeLab.PROP("PROXY_PORT");

			// Si PROXY_HOST n'est pas encore défini ou est NO_PROXY, demander à l'utilisateur
			if (host == null || host.isBlank() || host.equalsIgnoreCase("NO") 
				|| host.equalsIgnoreCase("NO_PROXY") || host.equalsIgnoreCase("NONE") 
				|| host.equals(UNDEFINED)) {
				String input = (String) JOptionPane.showInputDialog(FRAME,
					CodeLab.LABEL("proxyHostPrompt"),
					CodeLab.LABEL("itemEnableProxy"),
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					"proxy.univ-lemans.fr");
				if (input != null && !input.isBlank()) {
					host = input.trim();
					PropertyBase.setUserProperty("PROXY_HOST", host);
					if (port == null || port.isBlank() || port.equals(UNDEFINED)) {
						port = "3128";
						PropertyBase.setUserProperty("PROXY_PORT", port);
					}
				} else {
					// Annulé par l'utilisateur
					PROXY_CONFIGURED = false;
					if (CodeLabMenu.itemEnableProxy != null) {
						CodeLabMenu.itemEnableProxy.setSelected(false);
					}
					return;
				}
			}

			PROXY_HOST = host;
			PROXY_PORT = (port != null && !port.isBlank() && !port.equals(UNDEFINED)) ? port : "3128";
			PROXY_CONFIGURED = true;

			System.setProperty("http.proxyHost", PROXY_HOST);
			System.setProperty("http.proxyPort", PROXY_PORT);
			System.setProperty("https.proxyHost", PROXY_HOST);
			System.setProperty("https.proxyPort", PROXY_PORT);

			if (saveProperty) {
				PropertyBase.setUserProperty("PROXY_ENABLED", "yes");
			}
			if (notify && INSTANCE != null) {
				INSTANCE.printlnToConsole(String.format(CodeLab.LABEL("proxyActivated"), PROXY_HOST, PROXY_PORT));
			}
		} else {
			PROXY_CONFIGURED = false;
			System.clearProperty("http.proxyHost");
			System.clearProperty("http.proxyPort");
			System.clearProperty("https.proxyHost");
			System.clearProperty("https.proxyPort");

			if (saveProperty) {
				PropertyBase.setUserProperty("PROXY_ENABLED", "no");
			}
			if (notify && INSTANCE != null) {
				INSTANCE.printlnToConsole(CodeLab.LABEL("proxyDeactivated"));
			}
		}

		if (CodeLabMenu.itemEnableProxy != null && CodeLabMenu.itemEnableProxy.isSelected() != PROXY_CONFIGURED) {
			CodeLabMenu.itemEnableProxy.setSelected(PROXY_CONFIGURED);
		}
	}

	///////////////////////////////////////////////////
	// Terminateurs
	///////////////////////////////////////////////////

	private static final AtomicBoolean HAS_EXITED = new AtomicBoolean(false);

	public void things_to_do_before_exiting() {
		if (!HAS_EXITED.compareAndSet(false, true)) {
			return; // Déjà exécuté
		}
		CodeLab.logger("Things to do before exiting CodeLab...");
		if (isRunning()) halt(); // Stopper exécution en cours
		if (editor != null) {
			editor.saveAllFiles(); // Sauvegarde de tous les onglets ouverts
			editor.saveCurrentFile(); // Sauvegarde en local et à distance
			if (!isTutor() && !isAdmin()) {
				editor.backupCurrentFile(); // Backup daté du fichier courant
			}
		}
		//executionMonitor.shutdown(); // Arrêter le moniteur d'exécution
		updateSizeProperties(); // Actualisation de la taille de la fenêtre
		saveHiddenProperties(); // Sauvegarde des hidden properties
		Utils.wait(500); // Attendre 500 ms
	}

	public static void relaunch() {
		try {
			if (LAUNCHER != null && !LAUNCHER.isBlank()) {
				new ProcessBuilder(LAUNCHER, "--relauch").start();
				CodeLab.exit();
			} else {
				logger("No LAUNCHER defined, cannot relaunch automatically.");
			}
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
		return execute(null);
	}

	public boolean execute(String customFilename) {
		if (isRunning()) return false; // En principe cela ne doit pas arriver !!
		if (editor != null && (editor.getExplorerMode() == FileManager.MODE_EVALUATIONS || !editor.isExecutable())) return false;

		checkLocalServer(); // Lancer le serveur local (si besoin)
		clearConsole(); // Effacer la console (si autoclear)

		// Sauvagarder (si nécessaire)
		if (editor.isModified()) {
			editor.saveCurrentFile();
			Utils.wait(100);
		}
		
		// Compiler (si nécessaire)
		if (customFilename == null && editor.isCompilable() && !editor.compilation_OK()) {
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
		String filename = (customFilename != null) ? customFilename : editor.getCurrentFilename();
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

	private final Object requestLock = new Object();

	public String handleRequest(String query) {
		synchronized (requestLock) {
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
		boolean input_required = true;

		if (AUTO_LOGIN && !PASSWD.equals(UNDEFINED)) {
			consoleLog("Auto-login to the file server...");
			try {
				// Décryptage des identifiants
				login = Encryption.decrypt(LOGIN);
				password = Encryption.decrypt(PASSWD);
				input_required = false;
			}
			catch (Exception e) {
				consoleLog("An error occurred while retrieving connection data");
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
					LOGIN = Encryption.encrypt(login);
					PASSWD = Encryption.encrypt(password);
				}
				catch (Exception e) {
					consoleLog("An error occurred while encrypting connection data");
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

	public String getClientIP() {
		return (client != null) ? client.getClientIP() : IP_ADDR;
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
				DOWNLOADS_URL);

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
				Utils.openBrowser(DOWNLOADS_PAGE_URL);
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

		if (JAVA_HOME_USER != null && !JAVA_HOME_USER.isBlank() && !JAVA_HOME_USER.equals("undefined")) {
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

	public static void check_C() {
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

	public static void check_GO() {
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

	public static void check_Haskell() {
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

	public static void check_Python() {
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
		System.out.flush();
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
			PrintStream out = new PrintStream(new java.io.FileOutputStream(file, true), true, "UTF-8");
			System.setOut(out);
			System.setErr(out);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	private static void restoreUserDirectory() {
		File bakFile = getCodelabFilesArchive();
		if (bakFile == null || !bakFile.exists() || !bakFile.canRead()) {
			logger("ERROR: Reference archive not found or unreadable. Restore aborted.");
			return;
		}

		File userDir = new File(CODELAB_FILES);
		String timestamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());

		// 1. Snapshot / Archive ZIP de sécurité de codelab.files AVANT suppression
		if (userDir.exists()) {
			File zipBackup = new File(HOME, String.format("codelab.files.backup_%s.zip", timestamp));
			try {
				logger("Creating safety zip backup of " + CODELAB_FILES + " to: " + zipBackup.getAbsolutePath());
				boolean zipped = MyFileUtils.zipFolder(zipBackup.getAbsolutePath(), userDir);
				if (!zipped) {
					logger("ERROR: Failed to create safety zip backup of " + CODELAB_FILES + ". Restore aborted.");
					return;
				}
				logger("Safety zip backup created successfully: " + zipBackup.getAbsolutePath());
			} catch (Exception e) {
				logger("ERROR creating safety zip backup of " + CODELAB_FILES + ": " + e.getMessage());
				ExceptionManager.process(e);
				return;
			}
		}

		File backupDir = null;
		File tempZip = null;
		try {
			// Création d'un dossier temporaire sécurisé pour la sauvegarde des données utilisateur
			backupDir = Files.createTempDirectory("codelab_backup_").toFile();

			// 2. Sauvegarde des données utilisateur existantes
			File userPrograms = new File(CODELAB_FILES_HIDDEN, "programs");
			File userModules = new File(MODULES_FOLDER);
			File userSkills = new File(SKILLS_FOLDER);
			File userData = new File(USER_DATA_FOLDER);
			File userRecovery = new File(RECOVERY_FOLDER);
			File userStyles = new File(STYLES_FOLDER);
			File userEvaluations = new File(EVALUATIONS_FOLDER);
			File userReportCss = new File(REPORT_CSS_FILE);

			if (userPrograms.exists()) FileUtils.copyDirectory(userPrograms, new File(backupDir, "programs"));
			if (userModules.exists()) FileUtils.copyDirectory(userModules, new File(backupDir, "modules"));
			if (userSkills.exists()) FileUtils.copyDirectory(userSkills, new File(backupDir, "skills"));
			if (userData.exists()) FileUtils.copyDirectory(userData, new File(backupDir, "userdata"));
			if (userRecovery.exists()) FileUtils.copyDirectory(userRecovery, new File(backupDir, "recovery"));
			if (userStyles.exists()) FileUtils.copyDirectory(userStyles, new File(backupDir, "styles"));
			if (userEvaluations.exists()) FileUtils.copyDirectory(userEvaluations, new File(backupDir, "evaluations"));
			if (userReportCss.exists()) FileUtils.copyFile(userReportCss, new File(backupDir, "report.css"));

			// 3. Suppression de l'actuel dossier codelab.files
			MyFileUtils.delete(CODELAB_FILES);

			// 4. Extraction du nouveau dossier codelab.files depuis l'archive de référence
			tempZip = new File(HOME, String.format("codelab.files.extract_%s.tmp.zip", timestamp));
			MyFileUtils.copy(bakFile.getAbsolutePath(), tempZip.getAbsolutePath());
			MyFileUtils.unzip(tempZip.getAbsolutePath());
			MyFileUtils.delete(tempZip);
			tempZip = null;

			// Renommer 'hidden' en '.hidden' si nécessaire (selon packaging archive)
			File hiddenVisible = new File(CODELAB_FILES, "hidden");
			File hiddenDot = new File(CODELAB_FILES_HIDDEN);
			if (hiddenVisible.exists() && !hiddenDot.exists()) {
				hiddenVisible.renameTo(hiddenDot);
			}

			// 5. Restauration / fusion des données utilisateur
			File bkpPrograms = new File(backupDir, "programs");
			if (bkpPrograms.exists()) FileUtils.copyDirectory(bkpPrograms, new File(CODELAB_FILES_HIDDEN, "programs"));
			File bkpModules = new File(backupDir, "modules");
			if (bkpModules.exists()) FileUtils.copyDirectory(bkpModules, new File(MODULES_FOLDER));
			File bkpSkills = new File(backupDir, "skills");
			if (bkpSkills.exists()) FileUtils.copyDirectory(bkpSkills, new File(SKILLS_FOLDER));
			File bkpUserData = new File(backupDir, "userdata");
			if (bkpUserData.exists()) FileUtils.copyDirectory(bkpUserData, new File(USER_DATA_FOLDER));
			File bkpRecovery = new File(backupDir, "recovery");
			if (bkpRecovery.exists()) FileUtils.copyDirectory(bkpRecovery, new File(RECOVERY_FOLDER));
			File bkpStyles = new File(backupDir, "styles");
			if (bkpStyles.exists()) FileUtils.copyDirectory(bkpStyles, new File(STYLES_FOLDER));
			File bkpEvaluations = new File(backupDir, "evaluations");
			if (bkpEvaluations.exists()) FileUtils.copyDirectory(bkpEvaluations, new File(EVALUATIONS_FOLDER));
			File bkpReportCss = new File(backupDir, "report.css");
			if (bkpReportCss.exists()) FileUtils.copyFile(bkpReportCss, new File(REPORT_CSS_FILE));
			logger("User directory restored successfully.");
		}
		catch (Exception e) {
			logger("Error during restoreUserDirectory: " + e.getMessage());
			ExceptionManager.process(e);
		}
		finally {
			// 6. Nettoyage garanti
			if (tempZip != null && tempZip.exists()) {
				MyFileUtils.delete(tempZip);
			}
			if (backupDir != null && backupDir.exists()) {
				MyFileUtils.delete(backupDir);
			}
		}
	}

	public void resetCodelabFiles() {
		// 1. Demande de confirmation à l'utilisateur
		String message = LABEL("confirmRestoreCodelabFiles");
		if (!Utils.confirmDialog(message)) return;

		File archive = getCodelabFilesArchive();
		if (archive == null || !archive.exists() || !archive.canRead()) {
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("CODELAB_ERR4"));
			return;
		}

		File userDir = new File(CODELAB_FILES);
		String timestamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());

		// 2. Snapshot de sécurité avant réinitialisation
		File snapshotDir = null;
		if (userDir.exists()) {
			try {
				snapshotDir = new File(HOME, String.format("codelab.files.snapshot_%s", timestamp));
				boolean renamed = userDir.renameTo(snapshotDir);
				if (!renamed) {
					FileUtils.copyDirectory(userDir, snapshotDir);
					FileUtils.deleteDirectory(userDir);
				}
				logger("Safety snapshot created at: " + snapshotDir.getAbsolutePath());
			} catch (Exception e) {
				logger("ERROR creating safety snapshot: " + e.getMessage());
				Utils.showWarningDialog(LABEL("WARNING"), "Erreur lors de la création du snapshot de sécurité : " + e.getMessage());
				return;
			}
		}

		// 3. Extraction de la nouvelle arborescence codelab.files depuis l'archive de référence
		try {
			File tempZip = new File(HOME, String.format("codelab.files.extract_%s.tmp.zip", timestamp));
			MyFileUtils.copy(archive.getAbsolutePath(), tempZip.getAbsolutePath());
			MyFileUtils.unzip(tempZip.getAbsolutePath());
			MyFileUtils.delete(tempZip);

			// Renommer 'hidden' en '.hidden' si nécessaire (selon packaging archive)
			File hiddenVisible = new File(CODELAB_FILES, "hidden");
			File hiddenDot = new File(CODELAB_FILES_HIDDEN);
			if (hiddenVisible.exists() && !hiddenDot.exists()) {
				hiddenVisible.renameTo(hiddenDot);
			}

			// 4. Copier également le snapshot dans le nouveau RECOVERY_FOLDER pour l'historique dans l'interface
			if (snapshotDir != null && snapshotDir.exists()) {
				try {
					File recoveryBase = new File(RECOVERY_FOLDER);
					if (!recoveryBase.exists()) recoveryBase.mkdirs();
					File recSnapshot = new File(recoveryBase, String.format("snapshot_codelab_files_%s", timestamp));
					File oldProgs = new File(snapshotDir, ".hidden/programs/local");
					if (!oldProgs.exists()) oldProgs = new File(snapshotDir, "hidden/programs/local");
					if (oldProgs.exists()) {
						FileUtils.copyDirectory(oldProgs, recSnapshot);
					} else {
						FileUtils.copyDirectory(snapshotDir, recSnapshot);
					}
				} catch (Exception ignored) {}
			}

			// 5. Message de succès
			String snapPath = (snapshotDir != null) ? snapshotDir.getAbsolutePath() : "aucun";
			JOptionPane.showMessageDialog(FRAME,
				String.format(LABEL("resetCodelabFilesSuccess"), snapPath),
				LABEL("itemRestoreCodelabFiles"),
				JOptionPane.INFORMATION_MESSAGE,
				ICON);

			// 6. Relance ou rafraîchissement
			things_to_do_before_exiting();
			if (LAUNCHER != null && !LAUNCHER.isBlank()) {
				relaunch();
			} else {
				if (getEditor() != null) {
					getEditor().reset();
					if (getEditor().getManager() != null) {
						getEditor().getManager().setBase(getProgramDir());
						getEditor().getManager().populate();
					}
				}
			}
		} catch (Exception e) {
			logger("ERROR resetting codelab.files: " + e.getMessage());
			ExceptionManager.process(e);
			Utils.showWarningDialog(LABEL("WARNING"), "Erreur lors de la réinitialisation : " + e.getMessage());
		}
	}

	private static File getCodelabFilesArchive() {
		File bak = new File(CODELAB_FILES_BAK);
		if (bak.exists() && bak.canRead()) return bak;
		File rootZip = new File(BASE, "codelab_files.zip");
		if (rootZip.exists() && rootZip.canRead()) return rootZip;
		File parentZip = new File(new File(BASE).getParent(), "codelab_files.zip");
		if (parentZip.exists() && parentZip.canRead()) return parentZip;
		return null;
	}

	// -----------------------------------------------------------
	// Méthodes de gestion des sauvegardes (Menu Sauvegardes)
	// -----------------------------------------------------------

	public boolean createManualRecoveryBackup() {
		if (isConnected() && (isTutor() || isAdmin())) {
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("noBackupForTutorAdmin"));
			return false;
		}
		try {
			String progDir = getProgramDir();
			File srcDir = new File(progDir);
			if (!srcDir.exists() || !srcDir.isDirectory()) {
				Utils.showWarningDialog(LABEL("WARNING"), LABEL("noWorkspaceToBackup"));
				return false;
			}
			File[] files = srcDir.listFiles();
			if (files == null || files.length == 0) {
				Utils.showWarningDialog(LABEL("WARNING"), LABEL("emptyWorkspaceToBackup"));
				return false;
			}
			boolean hasUsefulFile = false;
			for (File f : files) {
				if (!f.getName().startsWith(".")) {
					hasUsefulFile = true;
					break;
				}
			}
			if (!hasUsefulFile) {
				Utils.showWarningDialog(LABEL("WARNING"), LABEL("emptyWorkspaceToBackup"));
				return false;
			}

			File recoveryBase = new File(RECOVERY_FOLDER);
			if (!recoveryBase.exists()) recoveryBase.mkdirs();

			String namePrefix = isConnected() ? getSession() : "local";
			if (namePrefix == null || namePrefix.isBlank() || namePrefix.equals("--")) namePrefix = "workspace";
			String timestamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
			String backupDirName = String.format("%s/%s_%s", RECOVERY_FOLDER, namePrefix, timestamp);
			File backupDir = new File(backupDirName);
			backupDir.mkdirs();
			FileUtils.copyDirectory(srcDir, backupDir);
			logger("Manual safety recovery backup created in: " + backupDirName);

			cleanOldRecoveryBackups();

			JOptionPane.showMessageDialog(FRAME,
				String.format(LABEL("manualRecoveryBackupSuccess"), backupDir.getName()),
				LABEL("menuBackups"),
				JOptionPane.INFORMATION_MESSAGE,
				ICON);
			return true;
		} catch (Exception e) {
			logger("Error creating manual recovery backup: " + e.getMessage());
			Utils.showWarningDialog(LABEL("WARNING"), e.getMessage());
			return false;
		}
	}

	public void restoreRecoveryBackup() {
		if (isConnected() && (isTutor() || isAdmin())) {
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("noBackupForTutorAdmin"));
			return;
		}
		File recoveryFolder = new File(RECOVERY_FOLDER);
		if (!recoveryFolder.exists()) {
			JOptionPane.showMessageDialog(FRAME, LABEL("noRecoveryBackupAvailable"), LABEL("menuBackups"), JOptionPane.INFORMATION_MESSAGE, ICON);
			return;
		}
		File[] backups = recoveryFolder.listFiles(File::isDirectory);
		if (backups == null || backups.length == 0) {
			JOptionPane.showMessageDialog(FRAME, LABEL("noRecoveryBackupAvailable"), LABEL("menuBackups"), JOptionPane.INFORMATION_MESSAGE, ICON);
			return;
		}
		Arrays.sort(backups, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		String[] choices = new String[backups.length];
		for (int i = 0; i < backups.length; i++) {
			choices[i] = String.format("%s (%s)", backups[i].getName(), df.format(new Date(backups[i].lastModified())));
		}

		String selected = (String) JOptionPane.showInputDialog(FRAME,
			LABEL("chooseRecoveryBackup"),
			LABEL("restoreRecoveryTitle"),
			JOptionPane.PLAIN_MESSAGE,
			ICON,
			choices,
			choices[0]);
		if (selected == null) return;

		int index = -1;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i].equals(selected)) {
				index = i;
				break;
			}
		}
		if (index == -1) return;
		File chosenBackup = backups[index];

		if (!Utils.confirmDialog(String.format(LABEL("confirmRestoreRecoveryBackup"), chosenBackup.getName()))) {
			return;
		}

		try {
			String progDir = getProgramDir();
			File srcDir = new File(progDir);

			if (srcDir.exists() && srcDir.list() != null && srcDir.list().length > 0) {
				String timestamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
				File preRestore = new File(String.format("%s/pre_restore_%s", RECOVERY_FOLDER, timestamp));
				FileUtils.copyDirectory(srcDir, preRestore);
			}

			if (!srcDir.exists()) srcDir.mkdirs();
			FileUtils.cleanDirectory(srcDir);
			FileUtils.copyDirectory(chosenBackup, srcDir);

			if (getEditor() != null) {
				getEditor().reset();
				if (getEditor().getManager() != null) {
					getEditor().getManager().populate();
				}
			}

			if (isStudent() && isConnected()) {
				uploadFilesRec(srcDir);
			}

			logger("Workspace restored from recovery backup: " + chosenBackup.getName());
			JOptionPane.showMessageDialog(FRAME,
				String.format(LABEL("restoreRecoverySuccess"), chosenBackup.getName()),
				LABEL("menuBackups"),
				JOptionPane.INFORMATION_MESSAGE,
				ICON);
		} catch (Exception e) {
			logger("Error restoring recovery backup: " + e.getMessage());
			Utils.showWarningDialog(LABEL("WARNING"), e.getMessage());
		}
	}

	private void uploadFilesRec(File dir) {
		File[] list = dir.listFiles();
		if (list == null) return;
		for (File f : list) {
			if (f.getName().startsWith(".")) continue;
			if (f.isDirectory()) uploadFilesRec(f);
			else if (client != null) client.uploadFile(f);
		}
	}

	public void cleanRecoveryFolder() {
		File file = new File(RECOVERY_FOLDER);
		if (!file.exists() || file.list() == null || file.list().length == 0) {
			JOptionPane.showMessageDialog(FRAME, LABEL("noRecoveryBackupAvailable"), LABEL("menuBackups"), JOptionPane.INFORMATION_MESSAGE, ICON);
			return;
		}
		if (!Utils.confirmDialog(LABEL("confirmCleanRecoveryBackups"))) return;
		try {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) FileUtils.deleteDirectory(f);
					else f.delete();
				}
			}
			logger("Recovery backups folder cleaned");
			JOptionPane.showMessageDialog(FRAME, LABEL("cleanRecoveryBackupsSuccess"), LABEL("menuBackups"), JOptionPane.INFORMATION_MESSAGE, ICON);
		} catch (Exception e) {
			logger("Error cleaning recovery folder: " + e.getMessage());
			Utils.showWarningDialog(LABEL("WARNING"), e.getMessage());
		}
	}

	public void exportWorkspaceZip() {
		String progDir = getProgramDir();
		File srcDir = new File(progDir);
		if (!srcDir.exists() || !srcDir.isDirectory()) {
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("noWorkspaceToBackup"));
			return;
		}
		File[] files = srcDir.listFiles();
		if (files == null || files.length == 0) {
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("emptyWorkspaceToBackup"));
			return;
		}

		String prefix = isConnected() ? getSession() : "codelab";
		if (prefix == null || prefix.isBlank() || prefix.equals("--")) prefix = "codelab";
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
		String suggestedName = String.format("%s_%s.zip", prefix, timestamp);

		JFileChooser chooser = new JFileChooser();
		File desktop = new File(System.getProperty("user.home") + "/Desktop");
		chooser.setCurrentDirectory(desktop.exists() ? desktop : new File(System.getProperty("user.home")));
		chooser.setSelectedFile(new File(suggestedName));
		chooser.setDialogTitle(LABEL("exportZipTitle"));

		if (chooser.showSaveDialog(FRAME) != JFileChooser.APPROVE_OPTION) return;
		File targetFile = chooser.getSelectedFile();
		if (targetFile == null) return;

		if (!targetFile.getName().toLowerCase().endsWith(".zip")) {
			targetFile = new File(targetFile.getAbsolutePath() + ".zip");
		}

		if (targetFile.exists()) {
			if (!Utils.confirmDialog(LABEL("confirmOverwriteZip"))) return;
			targetFile.delete();
		}

		File tempExportDir = new File(String.format("%s/export_%s", TEMP_FOLDER, timestamp));
		try {
			// 1. Recopie dans un dossier temporaire isolé
			FileUtils.copyDirectory(srcDir, tempExportDir);

			// 2. Nettoyage récursif des exclusions (__*, .exe, .orig, .bak, .class, cachés)
			cleanExcludedForExport(tempExportDir);

			File[] cleanFiles = tempExportDir.listFiles();
			if (cleanFiles == null || cleanFiles.length == 0) {
				Utils.showWarningDialog(LABEL("WARNING"), LABEL("emptyWorkspaceToBackup"));
				return;
			}

			// 3. Compression de l'arborescence épurée
			ZipFile zipFile = new ZipFile(targetFile);
			for (File f : cleanFiles) {
				if (f.isDirectory()) {
					zipFile.addFolder(f);
				} else {
					zipFile.addFile(f);
				}
			}
			zipFile.close();

			logger("Workspace exported to ZIP: " + targetFile.getAbsolutePath());
			JOptionPane.showMessageDialog(FRAME,
				String.format(LABEL("exportZipSuccess"), targetFile.getAbsolutePath()),
				LABEL("menuBackups"),
				JOptionPane.INFORMATION_MESSAGE,
				ICON);
		} catch (Exception e) {
			logger("Error exporting workspace ZIP: " + e.getMessage());
			Utils.showWarningDialog(LABEL("WARNING"), LABEL("exportZipError") + "\n" + e.getMessage());
		} finally {
			try {
				if (tempExportDir.exists()) FileUtils.deleteDirectory(tempExportDir);
			} catch (Exception ignored) {}
		}
	}

	private void cleanExcludedForExport(File dir) {
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			String name = f.getName().toLowerCase();
			if (f.getName().startsWith(".") || f.getName().startsWith("__")
					|| name.endsWith(".exe") || name.endsWith(".orig") || name.endsWith(".bak") || name.endsWith(".class")
					|| name.endsWith(".ipynb.py") || name.endsWith(".ipynb.cell.py")
					|| name.endsWith(".ipynb.c") || name.endsWith(".ipynb.cell.c")
					|| name.endsWith(".cell.c") || name.endsWith(".cell.py")) {
				if (f.isDirectory()) {
					try { FileUtils.deleteDirectory(f); } catch (Exception ignored) {}
				} else {
					f.delete();
				}
			} else if (f.isDirectory()) {
				cleanExcludedForExport(f);
			}
		}
	}

	private static void restoreHiddenFolder(String targetFolderPath, String folderLabel) {
		File targetFolder = new File(targetFolderPath);
		File initFolder = new File(targetFolderPath + "_init");

		// Si le dossier _init n'existe pas dans .hidden, tenter de le régénérer depuis les ressources
		if (!initFolder.exists() || !initFolder.isDirectory()) {
			// 1. Chercher dans BASE
			File appHiddenInit = new File(BASE, "codelab.files/hidden/" + initFolder.getName());
			if (!appHiddenInit.exists()) {
				appHiddenInit = new File(BASE, "codelab.files/.hidden/" + initFolder.getName());
			}
			if (appHiddenInit.exists() && appHiddenInit.isDirectory()) {
				MyFileUtils.copyFolder(appHiddenInit.getAbsolutePath(), initFolder.getAbsolutePath());
			}
			// 2. Mode développement (client/codelab.files/hidden/...)
			else {
				File devHiddenInit = new File("client/codelab.files/hidden/" + initFolder.getName());
				if (devHiddenInit.exists() && devHiddenInit.isDirectory()) {
					MyFileUtils.copyFolder(devHiddenInit.getAbsolutePath(), initFolder.getAbsolutePath());
				}
			}
			// 3. Repli : si le dossier cible existe déjà, s'en servir pour initialiser _init
			if (!initFolder.exists() && targetFolder.exists() && targetFolder.isDirectory()) {
				MyFileUtils.copyFolder(targetFolder.getAbsolutePath(), initFolder.getAbsolutePath());
			}
		}

		if (initFolder.exists() && initFolder.isDirectory()) {
			System.out.format("   Restoring the hidden %s folder... ", folderLabel);
			MyFileUtils.delete(targetFolder);
			boolean ok = MyFileUtils.copyFolder(initFolder.getAbsolutePath(), targetFolder.getAbsolutePath());
			System.out.println(ok ? "DONE" : "SKIPPED");
		} else {
			logger("WARNING: " + initFolder.getAbsolutePath() + " not found, keeping existing " + folderLabel + " folder.");
			if (!targetFolder.exists()) {
				targetFolder.mkdirs();
			}
		}
	}

	///////////////////////////////////////////////////
	// Méthode statique main
	///////////////////////////////////////////////////

	public static void main(String[] argv) {

		// ------------------------------------------------
		// Paramètres de la ligne de commande

		ARGV = argv;
		List<File> filesFromArgs = new ArrayList<>();
		for (String arg: argv) {
			if (arg.equals("--log")) REDIRECT = true; // Rediriger la sortie standard vers un fichier de log
			if (arg.equals("--auto")) AUTOMATOR = true; // Exécuter l'automate de saisie dans l'éditeur de textes
			if (arg.equals("--test")) TEST_REPORTING = true; // Ajouter un item Test reporting au menu CodeLab
			if (arg.equals("--load")) FORCE_DOWNLOAD = true; // Forcer une mise-à-jour depuis le site de CodeLab
			if (arg.equals("--restore")) FORCE_RESTORE = true; // Forcer la réinitialisation de codelab.files
			if (arg.equals("--relauch")) VERIFY_RUNNING = false; // Inhiber la vérification d'autres instances
			if (!arg.startsWith("-")) {
				File f = new File(arg);
				if (f.exists()) filesFromArgs.add(f);
			}
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
			first_run = true;
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
		// Initialiser le répertoire recovery (créé si absent)

		File recoveryDir = new File(RECOVERY_FOLDER);
		if (!recoveryDir.exists()) recoveryDir.mkdirs();

		// ------------------------------------------------
		// Initialiser le répertoire permanent des évaluations (créé si absent)

		File evalDir = new File(EVALUATIONS_FOLDER);
		if (!evalDir.exists()) evalDir.mkdirs();

		// ------------------------------------------------
		// Réinitialiser les répertoires système cachés (includes & templates)

		restoreHiddenFolder(INCLUDES_FOLDER, "includes");
		restoreHiddenFolder(TEMPLATES_FOLDER, "templates");

		// ------------------------------------------------
		// Chargement des propriétés

		System.out.println("   Loading properties...");
		PropertyBase.load();
		loadHiddenProperties(); // Les pptés cachées dans .hidden
		if (first_run) saveHiddenProperties();

		// Styles CSS Markdown et rapports (depuis sysconfig.properties)
		String rStyle = CodeLab.PROP("REPORT_STYLE");
		if (!rStyle.equals(UNDEFINED) && !rStyle.isBlank()) REPORT_STYLE = rStyle;
		String iStyle = CodeLab.PROP("IPYNB_STYLE");
		if (!iStyle.equals(UNDEFINED) && !iStyle.isBlank()) IPYNB_STYLE = iStyle;

		// Initialiser et synchroniser le dossier des styles de rapports CSS
		initStylesFolder();
		getReportCssFile();

		// Configuration du proxy
		initProxySettings();

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

		boolean codelab_files_obsolete = FORCE_RESTORE;

		if (!codelab_files_obsolete && !first_run) {
			int previous_major = Utils.getVersionPart(DATA_VERSION, 1);
			int previous_minor = Utils.getVersionPart(DATA_VERSION, 2);
			int current_major = Utils.getVersionPart(VERSION, 1);
			int current_minor = Utils.getVersionPart(VERSION, 2);

			if (DATA_VERSION.equals(UNDEFINED) || previous_major == -1 || previous_minor == -1) {
				codelab_files_obsolete = true;
			} else if (previous_major != current_major || previous_minor != current_minor) {
				codelab_files_obsolete = true;
			}
		}

		if (codelab_files_obsolete) {
			File bak = getCodelabFilesArchive();
			if (bak == null || !bak.exists() || !bak.canRead()) {
				logger("WARNING: codelab.files is obsolete, but reference archive is missing. Skipping restore.");
			} else {
				JOptionPane.showMessageDialog(null, LABEL("CODELAB_ERR4"), "CODELAB INFORMATION", JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
				restoreUserDirectory(); // Patcher le dossier codelab.files
				saveHiddenProperties(); // Sauvegarder la ppté DATA_VERSION
				relaunch(); // Relancer CodeLab
			}
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
		// Polices embarquées, LookAndFeel et langue de CodeLab

		ResourceUtils.initEmbeddedFonts();
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
		// Découverte et initialisation des compétences d'évaluation (Skills)

		codelab.eval.SessionEvaluator.scanSkills();

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
		directPrintln("[codelab] " + msg, new Color(0, 140, 0));
		
		Utils.wait(1000);
		WaitingDialog.close();
		logger(msg);

		List<File> toOpen = new ArrayList<>();
		toOpen.addAll(filesFromArgs);
		synchronized (PENDING_OPEN_FILES) {
			toOpen.addAll(PENDING_OPEN_FILES);
			PENDING_OPEN_FILES.clear();
		}
		logger(String.format("Files to open at startup: %d (from args: %d)", toOpen.size(), filesFromArgs.size()));
		if (!toOpen.isEmpty() && INSTANCE != null) {
			SwingUtilities.invokeLater(() -> {
				for (File f : toOpen) {
					INSTANCE.handleDroppedFile(f);
				}
			});
		}
	}
}

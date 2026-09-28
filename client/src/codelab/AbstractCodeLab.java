package codelab;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import codelab.console.Console;
import codelab.console.ConsoleInterface;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;

/**
*	Classe abstraite de CodeLab
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public abstract class AbstractCodeLab extends JFrame {

	///////////////////////////////////////////////////
	// Constantes publiques finales
	///////////////////////////////////////////////////

	public static final String VERSION = "1.4.2"; // Attribué automatiquement par ant
	public static final String BUILD = "2609281517"; // Attribué automatiquement par ant

	public static final String TITLE = String.format("CodeLab %s", VERSION);
	public static final String ARCHITECTURE = Utils.getArchitecture();
	public static final String SPLASH_INFO = String.format("CodeLab %s build %s (%s)", VERSION, BUILD, ARCHITECTURE);
	public static final String SPLASH_IMAGE = "splash.png";
	public static final int SPLASH_DELAY = 50;

	// Les variables d'environnement

	public static final String BASE = System.getenv("BASE"); // Le dossier de CodeLab
	public static final String PATH = System.getenv("PATH"); // La variable PATH du système
	public static final String HOME = System.getenv("HOME"); // Le dossier HOME de l'utilisateur
	public static final String LAUNCHER = System.getenv("LAUNCHER"); // Le lanceur utilisé (ou vide)
	public static String JAVA_HOME = System.getenv("JAVA_HOME"); // Modifiable dans checkLanguages()

	// Infos sur le système d'exploitation

	public static final String SYSTEM = Utils.getOperatingSystem();
	public static final String IP_ADDR = Utils.getHostAddress();
	public static final String HOST_NAME = Utils.getHostName();
	public static final boolean IS_OSX = SYSTEM.equals("Mac");
	public static final boolean IS_LINUX = SYSTEM.equals("Linux");
	public static final boolean IS_WINDOWS = SYSTEM.equals("Windows");
	public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();

	// Ressources dans le dossier codelab

	public static final String RESSOURCES = IS_OSX ? BASE : BASE + "/.hidden"; // Section cachée (sauf MacOS)
	public static final String PROCESSING = RESSOURCES + "/codelab/libraries/processing"; // Librairies Processing
	public static final String PROPERTY_FILE = RESSOURCES + "/codelab/sysconfig.properties"; // Propriétés système (hors du jar)
	public static final String USER_PROP_BAK = RESSOURCES + "/codelab/user.properties.bak"; // Propriétés utilisateur de remplacement
	public static final String CODELAB_FILES_BAK = RESSOURCES + "/codelab/files.zip"; // Le dossier codelab.files de remplacement
	public static final String NATIVES = RESSOURCES + "/codelab/natives"; // Dossier des librairies natives

	// Ressources système dans le dossier codelab.files/.hidden

	public static final String CODELAB_FILES = HOME + "/codelab.files"; // Le dossier utilisateur
	public static final String CODELAB_FILES_HIDDEN = CODELAB_FILES + "/.hidden"; // Sa section cachée

	public static final String CDK_FOLDER = CODELAB_FILES_HIDDEN + "/CDK"; // CodeLab Development Kit
	public static final String TEMP_FOLDER = CODELAB_FILES_HIDDEN + "/temp"; // Emplacement temporaire
	public static final String INCLUDES_FOLDER = CODELAB_FILES_HIDDEN + "/includes"; // Le dossier des API par langage
	public static final String TEMPLATES_FOLDER = CODELAB_FILES_HIDDEN + "/templates"; // Le dossier des templates (modèles)
	public static final String EXERCICES_FOLDER = CODELAB_FILES_HIDDEN + "/exercices"; // Le dossier des exemples et des exercices
	public static String PROG_DIST_FOLDER = CODELAB_FILES_HIDDEN + "/programs/dist"; // Le dossier des programmes en mode connecté (modifiable dans le constructeur)
	public static final String PROG_LOCAL_FOLDER = CODELAB_FILES_HIDDEN + "/programs/local"; // Le dossier des programmes en mode standalone
	public static final String LOGFILE = CODELAB_FILES_HIDDEN + "/codelab.log"; // Le fichier de log
	public static final String INFOFILE = CODELAB_FILES_HIDDEN + "/infos.txt"; // Trucs à transmettre (à chaque exécution de CodeLab)
	public static final String DATAFILE = CODELAB_FILES_HIDDEN + "/data.properties"; // Propriétés cachées

	// Ressources utilisateur dans le dossier codelab.files

	public static final String MODULES_FOLDER = CODELAB_FILES + "/modules"; // Le dossier où déposer les plugins
	public static final String USER_DATA_FOLDER = CODELAB_FILES + "/userdata"; // Le dossier de données utilisateur
	public static final String USER_MEDIA_FOLDER = USER_DATA_FOLDER + "/media"; // Le dossier où déposer les médias (wav, etc.)
	public static final String USER_GRAPH_FOLDER = USER_DATA_FOLDER + "/mod_graphics"; // Le dossier mod_graphics
	public static final String USER_ROBOT_FOLDER = USER_DATA_FOLDER + "/mod_robotics"; // Le dossier mod_robotics
	public static final String USER_PROP_FILE = USER_DATA_FOLDER + "/user.properties"; // Le fichier de configuration utilisateur

	// Quelques autres constantes finales

	public static final String LANGUAGE_FILE = "/data/syslang.properties"; // Fichier de langues (dans le jar)
	public static final String CODELAB_URL = "https://codelab.univ-lemans.fr"; // Le site web de CodeLab
	public static final String DOWNLOADS_URL = "https://codelab.univ-lemans.fr/downloads";
	public static final String DOWNLOADS_PAGE_URL = "https://codelab.univ-lemans.fr/downloads/downloads-%s.php";
	public static final String INFOCLIENT_URL = "https://codelab.univ-lemans.fr/doc-%s/usages/infoclient-%s";
	public static final String INFOSERVER_URL = "https://codelab.univ-lemans.fr/doc-%s/usages/infoserver-%s";

	public static final ImageIcon ICON = ResourceUtils.loadImageIcon("icons/icon_codelab.png");
	public static final int MONITOR_PERIOD = 2000; // Check du fichier de log toutes les 2 secondes

	// Propriétés initialisées dans le main() de CodeLab

	public static int TIME_OUT;
	public static String USER; // Nom inséré danas les headers
	public static String SYNTH_OSC; // Oscillateur du synthétiseur
	public static String FONT_NAME; // Police de caractères de l'éditeur
	public static int FONT_SIZE; // Taille des caractères de l'éditeur
	public static int BUFFER_SIZE; // Taille du buffer de la console
	public static int CONSOLE_SIZE; // Hauteur de la console en pixels
	public static int BACKUP_PERIOD; // Période de backup du fichier courant (en mn)
	public static int MAX_FILE_SIZE; // Taille maximum des fichiers à transférer au serveur
	public static int COMPILE_TIMEOUT; // Timeout de compilation (en secondes)
	public static int PROGRAM_TIMEOUT; // Timeout d'exécution (en secondes)
	public static String SERVER_HOST; // Adresse du serveur de fichiers
	public static String SERVER_PORT; // Port du serveur de fichiers
	public static String LOCAL_PORT; // Port du serveur interne
	public static String ADMIN_URL; // Adresse du serveur d'administration
	public static boolean CONNECTED_MODE; // Mode connecté ou non
	public static boolean REPORT_EXCEPTIONS; // Mode reporting ou non
	public static boolean RETE_FLAG; // Activation du moteur d'inférences
	public static boolean PRINT_FACTS; // Affichage de la BDF dans le log

	public static boolean WRK_SERVER_OK;
	public static boolean ERR_SERVER_OK;

	public static String PROXY_HOST;
	public static String PROXY_PORT;
	public static boolean PROXY_CONFIGURED;

	// Constantes initialisées via la ligne de commande
	public static String[] ARGV;
	public static boolean VERIFY_RUNNING = true;
	public static boolean TEST_REPORTING = false;
	public static boolean FORCE_DOWNLOAD = false;
	public static boolean FORCE_RESTORE = false;
	public static boolean AUTOMATOR = false;
	public static boolean REDIRECT = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractCodeLab() {
		// Pour avoir la barre de menu du Mac
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Pour lisser les polices sous Linux
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		// Paramétrages spécifiques pour Java3D
		System.setProperty("sun.java2d.opengl", "true"); // Active l'accélération matérielle
		System.setProperty("sun.java2d.noddraw", "true"); // Désactive l'utilisation du pipeline DirectDraw
		System.setProperty("sun.awt.noerasebackground", "true"); // Patch javax.swing flickering on resize
		System.setProperty("j3d.implicitAntialiasing", "true"); // Active le lissage Java3D
		System.setProperty("j3d.numSamples", "8"); // Nombre de passes pour le lissage
		// Pour définir le comportement des infobulles
		ToolTipManager.sharedInstance().setInitialDelay(200);
		ToolTipManager.sharedInstance().setDismissDelay(10000);
		// Pour changer la couleur des infobulles
		UIManager.put("ToolTip.background", new Color(255, 255, 180));
		// Pour intercepter les exceptions qui arrivent dans le thread de Swing
		Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueueProxy());
		Thread.setDefaultUncaughtExceptionHandler(new LastChanceHandler());
		// Pour éviter d'avoir la couleur bleu sur le Tabbedpane
		tabbedpane.setFocusable(false);

		if (IS_WINDOWS || IS_OSX) {
			// Pour charger les librairies jinput sous Windows et macOS (Apple Silicon / Intel)
			System.setProperty("net.java.games.input.librarypath", new File(NATIVES).getAbsolutePath());
			// https://rollbar.com/blog/java-unsatisfiedlinkerror-runtime-error/
			// https://github.com/jinput/jinput/issues/42
		}

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			e.printStackTrace();
			// Affiche une boîte de dialogue si possible
			javax.swing.SwingUtilities.invokeLater(() ->
				javax.swing.JOptionPane.showMessageDialog(null, "Erreur fatale: " + e.getMessage())
			);
		});
	}

	///////////////////////////////////////////////////
	// Proxy to catch uncaught exceptions
	///////////////////////////////////////////////////

	private class EventQueueProxy extends EventQueue {

		protected void dispatchEvent(AWTEvent newEvent) {
			try {
				super.dispatchEvent(newEvent);
			}
			catch (Throwable e) {
				Exception ex = new Exception(e);
				ExceptionManager.process(ex);
			}
		}
	}

	private class LastChanceHandler implements Thread.UncaughtExceptionHandler {

		public void uncaughtException(Thread t, Throwable e) {
			Exception ex = new Exception(e);
			ExceptionManager.process(ex);
		}
	}

	///////////////////////////////////////////////////
	// Internationalisation
	///////////////////////////////////////////////////

	private static final String[] IMPLEMENTED_LANG = { "en", "fr" };

	public static boolean implemented(String lang) {
		return Utils.contains(IMPLEMENTED_LANG, lang);
	}

	public static String LANG;
	static {
		Locale locale = Locale.getDefault();
		LANG = locale.getLanguage();
		if (!implemented(LANG)) LANG = "en";
	}

	private static PropertyResourceBundle LANGUAGE =
		Utils.readPropertyResourceBundle_rsc(CodeLab.LANGUAGE_FILE); // Dans CodeLab.jar

	public static String LABEL(String key) {
		// Méthode invoquée partout
		// Ne doit pas être obfusquée !!
		key = String.format("%s_%s", key, LANG);
		if (LANGUAGE.containsKey(key)) return LANGUAGE.getString(key);
		return "undefined key: " + key;
	}

	///////////////////////////////////////////////////
	// Accès à base de propriétés
	///////////////////////////////////////////////////

	public static String PROP(String key) {
		return PropertyBase.getProperty(key);
	}

	///////////////////////////////////////////////////
	// Gestion des propriétés cachées
	///////////////////////////////////////////////////

	private static Properties HIDDEN_DATA = new Properties();
	public static final String UNDEFINED = "undefined";

	// Interface de CodeLab
	public static boolean SOUND_EFFECTS = true;
	public static boolean FILE_MANAGER_ICONS = true;
	public static boolean TEXT_UNDER_TOOLBUTTON = true;
	public static String WINDOW_SIZE = "1200x850";
	public static String WINDOW_POS = UNDEFINED;

	// Propriétés de la console
	public static boolean CONSOLE_FLAG = true;
	public static boolean CONSOLE_AUTOOPEN = true;
	public static boolean CONSOLE_AUTOCLEAR = true;

	// Propriétés de l'éditeur
	public static boolean SHOW_INVISIBLE = false;
	public static boolean AUTO_COMPLETE = false;
	public static boolean CODE_FOLDING = false;
	public static boolean DIFF_FLAG = false;

	// Connexion au serveur
	public static String LOGIN = UNDEFINED;
	public static String PASSWD = UNDEFINED;
	public static boolean AUTO_LOGIN = false;

	// Autres infos
	public static String DATA_VERSION = UNDEFINED;

	public static boolean loadHiddenProperties() {
		try {
			HIDDEN_DATA.load(new FileInputStream(DATAFILE));

			// Interface de CodeLab
			SOUND_EFFECTS = getBooleanData("SOUND_EFFECTS");
			FILE_MANAGER_ICONS = getBooleanData("FILE_MANAGER_ICONS");
			TEXT_UNDER_TOOLBUTTON = getBooleanData("TEXT_UNDER_TOOLBUTTON");
			WINDOW_SIZE = getStringData("WINDOW_SIZE");
			WINDOW_POS = getStringData("WINDOW_POS");

			// Propriétés de la console
			CONSOLE_FLAG = getBooleanData("CONSOLE_FLAG");
			CONSOLE_AUTOOPEN = getBooleanData("CONSOLE_AUTOOPEN");
			CONSOLE_AUTOCLEAR = getBooleanData("CONSOLE_AUTOCLEAR");

			// Propriétés de l'éditeur
			SHOW_INVISIBLE = getBooleanData("SHOW_INVISIBLE");
			AUTO_COMPLETE = getBooleanData("AUTO_COMPLETE");
			CODE_FOLDING = getBooleanData("CODE_FOLDING");
			DIFF_FLAG = getBooleanData("DIFF_FLAG");

			// Connexion au serveur
			LOGIN = getStringData("LOGIN");
			PASSWD = getStringData("PASSWD");
			AUTO_LOGIN = getBooleanData("AUTO_LOGIN");

			// Autres infos
			DATA_VERSION = getStringData("DATA_VERSION");

			return true;
		}
		catch (IOException e) {
			// Pas de fichier data.properties
			return false;
		}
	}

	public static void saveHiddenProperties() {
		try {
			// Interface de CodeLab
			setBooleanData("SOUND_EFFECTS", SOUND_EFFECTS);
			setBooleanData("FILE_MANAGER_ICONS", FILE_MANAGER_ICONS);
			setBooleanData("TEXT_UNDER_TOOLBUTTON", TEXT_UNDER_TOOLBUTTON);
			setStringData("WINDOW_SIZE", WINDOW_SIZE);
			setStringData("WINDOW_POS", WINDOW_POS);

			// Propriétés de la console
			setBooleanData("CONSOLE_FLAG", CONSOLE_FLAG);
			setBooleanData("CONSOLE_AUTOOPEN", CONSOLE_AUTOOPEN);
			setBooleanData("CONSOLE_AUTOCLEAR", CONSOLE_AUTOCLEAR);

			// Propriétés de l'éditeur
			setBooleanData("SHOW_INVISIBLE", SHOW_INVISIBLE);
			setBooleanData("AUTO_COMPLETE", AUTO_COMPLETE);
			setBooleanData("CODE_FOLDING", CODE_FOLDING);
			setBooleanData("DIFF_FLAG", DIFF_FLAG);

			// Connexion au serveur
			setStringData("LOGIN", LOGIN);
			setStringData("PASSWD", PASSWD);
			setBooleanData("AUTO_LOGIN", AUTO_LOGIN);

			// Autres infos
			setStringData("DATA_VERSION", VERSION); // Version actuelle de CodeLab

			HIDDEN_DATA.store(new FileWriter(DATAFILE), null);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	private static void setStringData(String key, String value) {
		HIDDEN_DATA.setProperty(key, value);
	}

	private static void setBooleanData(String key, boolean value) {
		HIDDEN_DATA.setProperty(key, Boolean.toString(value));
	}

	private static String getStringData(String key) {
		return HIDDEN_DATA.getProperty(key, UNDEFINED);
	}

	private static boolean getBooleanData(String key) {
		return Boolean.valueOf(getStringData(key));
	}

	// private static void setIntData(String key, int value) {
	// 	HIDDEN_DATA.setProperty(key, Integer.toString(value));
	// }

	// private static int getIntData(String key) {
	// 	try {
	// 		return Integer.valueOf(getStringData(key));
	// 	}
	// 	catch (NumberFormatException e) {
	// 		return 100;
	// 	}
	// }

	///////////////////////////////////////////////////
	// Gestion de la taille de la fenêtre
	///////////////////////////////////////////////////

	protected void setWindowLocation() {
		Pattern p = Pattern.compile("([\\d]+)\\+([\\d]+)");
		Matcher m = p.matcher(CodeLab.WINDOW_POS);
		if (m.find()) {
			int x = Integer.parseInt(m.group(1));
			int y = Integer.parseInt(m.group(2));
			setLocation(x, y);
		}
		else setLocationRelativeTo(null); // JFrame centrée
	}

	protected void setWindowSize() {
		Pattern p = Pattern.compile("([\\d]+)x([\\d]+)");
		Matcher m = p.matcher(CodeLab.WINDOW_SIZE);
		int width = 1200; // Par défaut
		int height = 850; // Par défaut
		if (m.find()) {
			width = Integer.parseInt(m.group(1));
			height = Integer.parseInt(m.group(2));
		}
		setMinimumSize(new Dimension(800, 500));
		setPreferredSize(new Dimension(width, height));
		setSize(new Dimension(width, height));
	}

	protected void updateSizeProperties() {
		// Sauvegarder la position de la fenêtre
		Point point = getLocationOnScreen();
		CodeLab.WINDOW_POS = String.format("%d+%d", point.x, point.y);
		// Sauvegarder la taille de la fenêtre
		Dimension size = getSize();
		CodeLab.WINDOW_SIZE = String.format("%dx%d", size.width, size.height);
	}

	///////////////////////////////////////////////////
	// Gestion des modules
	///////////////////////////////////////////////////

	protected JTabbedPane tabbedpane = new JTabbedPane();
	protected HashMap<String, String> modules_disp = new HashMap<String, String>(); // Les modules disponibles
	protected ArrayList<AbstractModule> module_list = new ArrayList<AbstractModule>(); // Les modules installés
	protected AbstractModule currentModule; // Module courant
	protected int module_index = 0; // Index du module courant

	public ArrayList<String> getModuleDisp() {
		ArrayList<String> array = new ArrayList<String>(modules_disp.keySet());
		Collections.reverse(array); // Pour retrouver l'ordre initial
		return array;
	}

	public ArrayList<AbstractModule> getModuleList() {
		return module_list;
	}

	public AbstractModule getCurrentModule() {
		return currentModule;
	}

	public String getModuleTitle(String name) {
		return modules_disp.get(name);
	}

	public void focusTabbedPane() {
		tabbedpane.requestFocus();
	}

	public void addModuleDisp(String name, String title) {
		modules_disp.put(name, title);
	}

	protected void addModule(AbstractModule module) {
		addModule(module, module.getTitle());
	}

	protected void addModule(AbstractModule module, String title) {
		tabbedpane.addTab(title, null, module.getFullComponent());
		module_list.add(module);
	}

	protected void setModule(int index) {
		module_index = index;
		currentModule = module_list.get(index);
		tabbedpane.setSelectedIndex(index);
	}
	
	protected void showModule(AbstractModule module) {
		if (SwingUtilities.isEventDispatchThread()) {
			tabbedpane.setSelectedComponent(module.getFullComponent());
		} else {
			SwingUtilities.invokeLater(() -> tabbedpane.setSelectedComponent(module.getFullComponent()));
		}
	}

	protected void startModule(AbstractModule module) {
		CodeLab.logger("startModule - avant");
		module.setRunning(true);
		module.start();
		CodeLab.logger("startModule - après");
	}

	protected void stopModule(AbstractModule module) {
		module.setRunning(false);
		module.stop();
	}

	protected void enableTabbedPane(boolean enabled) {
		// Pour autoriser / interdire les changements d'atelier
		tabbedpane.setEnabled(enabled);
		for (int i = 0; i < tabbedpane.getTabCount(); i++) {
			tabbedpane.setEnabledAt(i, enabled);
		}
	}

	public void setSelectedModule(AbstractModule module) {
		tabbedpane.setSelectedComponent(module.getFullComponent());
	}

	public void updateAllToolbars() {
		// Pour cacher les textes sous tous les boutons
		toolbar_update();
		for (AbstractModule module : module_list) {
			module.getToolbar().update();
		}
		revalidate();
	}

	public boolean isDispModule(String name) {
		// Pour savoir si un module existe
		for (String moduleName : modules_disp.keySet()) {
			//System.out.println(moduleName);
			if (moduleName.equals(name)) return true;
		}
		return false;
	}

	public boolean isActiveModule(String name) {
		// Pour savoir si le module est activé dans CodeLab
		for (AbstractModule module : module_list)
			if (module.getName().equals(name)) return true;
		return false;
	}

	public void setActivableModule(String name, boolean value) {
		name = "MODULE_" + name;
		setBooleanData(name, value);
	}

	public boolean isActivableModule(String name) {
		name = "MODULE_" + name;
		// Lecture du fichier data.properties
		if (getStringData(name).equals(UNDEFINED)) {
			// Ajouter une nouvelle entrée
			setBooleanData(name, true);
			return true;
		}
		else return getBooleanData(name);
	}

	public void updateModuleProperties() {
		// Pour supprimer les propriétés obsolètes
		ArrayList<String> keys = PropertyBase.collectUserPropertiesWithPrefix("MODULE_");
		keys.forEach((key) -> {
			String moduleName = key.substring(7); // Sans le préfixe "MODULE_"
			if (!modules_disp.containsKey(moduleName)) {
				HIDDEN_DATA.remove(key);
			}
		});
	}

	///////////////////////////////////////////////////
	// Pour générer un nouveau module
	///////////////////////////////////////////////////

	public void openModulesFolder() {
		openModulesFolder(new File(CodeLab.MODULES_FOLDER));
	}

	public void openModulesFolder(String name) {
		openModulesFolder(new File(String.format("%s/%s", CodeLab.MODULES_FOLDER, name)));
	}

	private void openModulesFolder(File file) {
		try {
			Desktop.getDesktop().open(file);
		}
		catch (IOException e) {
			System.out.println("WARNING: Can't open " + file.toString());
		}
	}

	public boolean buildNewModule(String name) {
		String msg = String.format("Building new module %s...", name);
		CodeLab.logger(msg);
		consoleLog(msg);

		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(new File(CDK_FOLDER));
		Map<String,String> env = pb.environment();
		env.put("JAVA_HOME", JAVA_HOME);
		String cmd;

		switch (SYSTEM) {
			case "Mac":
			case "Linux":
				cmd = CDK_FOLDER + "/newmodule.sh " + name;
				pb.command("sh", "-c", cmd);
				break;
			case "Windows":
				cmd = "newmodule.bat " + name;
				pb.command("cmd.exe", "/c", cmd);
				break;
			default:
				return false;
		}
		try {
			Process process = pb.start();
			StringBuilder output = new StringBuilder();
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
			String line;
			boolean crlf = false;
			while ((line = reader.readLine()) != null) {
				if (crlf) output.append("\n");
				output.append(line);
				crlf = true;
			}
			System.out.println(output);
			JOptionPane.showMessageDialog(CodeLab.FRAME,
				String.format(CodeLab.LABEL("newModuleInfo"), name),
				CodeLab.LABEL("newModuleTitle"),
				JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
			openModulesFolder(name);
			return true;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return false;
		}
	}

	///////////////////////////////////////////////////
	// Gestion de la barre d'outils
	///////////////////////////////////////////////////

	protected AbstractToolbar toolbar;

	public AbstractToolbar getToolbar() { return toolbar; }

	public void changeToolBar(AbstractToolbar newtoolbar) {
		getContentPane().remove(toolbar);
		getContentPane().add(toolbar = newtoolbar, BorderLayout.NORTH);
		getContentPane().validate();
		getContentPane().repaint();
		toolbar.update();
	}

	public void toolbar_update() {
		// Pour mettre à jour les boutons
		SwingUtilities.invokeLater(() -> {
			toolbar.update();
		});
	}

	///////////////////////////////////////////////////
	// Gestion de la console partagée
	///////////////////////////////////////////////////

	protected static ConsoleInterface console;
	public ConsoleInterface getConsole() { return console; }

	private boolean alerteFlag = false; // Indicateur clignotant

	public boolean getConsoleFlag() { return CONSOLE_FLAG; }
	public boolean getAutoconsole() { return CONSOLE_AUTOOPEN; }
	public boolean getAutoclear() { return CONSOLE_AUTOCLEAR; }
	public boolean getAlerteFlag() { return alerteFlag; }

	public void setConsoleFlag(boolean flag) {
		CONSOLE_FLAG = flag;
		if (alerteFlag && flag)
			alerteFlag = false;
		toolbar_update();
	}

	public void setAutoconsole(boolean flag) {
		CONSOLE_AUTOOPEN = flag;
		toolbar_update();
	}

	public void setAutoclear(boolean flag) {
		CONSOLE_AUTOCLEAR = flag;
	}

	public void setAlerteFlag(boolean flag) {
		alerteFlag = flag;
		toolbar_update();
	}

	public void clearConsole() {
		if (CONSOLE_AUTOCLEAR) console.clear();
	}

	public void showConsole() {
		currentModule.showConsole();
	}

	// ------------------------------------------------
	// Méthodes publiques d'affichage

	public void printToConsole(String str, Color color) {
		// Méthode invoquée par les plugins
		// Ne doit pas être obfusquée !!
		printToConsole(str, color, false);
	}

	public void printlnToConsole(String str, Color color) {
		// Méthode invoquée par les plugins
		// Ne doit pas être obfusquée !!
		printlnToConsole(str, color, false);
	}

	public static void directPrint(String str, Color color) {
		// Méthode invoquée dans common.clp
		// Ne doit pas être obfusquée !!
		console.directPrint(str, color);
	}

	public static void directPrintln(String str, Color color) {
		// Méthode invoquée dans common.clp
		// Ne doit pas être obfusquée !!
		directPrint(str + "\n", color);
	}

	public void printToConsole(String str, Color color, boolean caret) {
		if (CONSOLE_AUTOOPEN && !CONSOLE_FLAG) {
			setConsoleFlag(true);
			SwingUtilities.invokeLater(() -> currentModule.showConsole());
		}
		if (!CONSOLE_AUTOOPEN && !CONSOLE_FLAG) setAlerteFlag(true);
		console.print(str, color, caret);
	}

	public void printToConsole(char c, Color color, boolean caret) {
		if (CONSOLE_AUTOOPEN && !CONSOLE_FLAG) {
			setConsoleFlag(true);
			SwingUtilities.invokeLater(() -> currentModule.showConsole());
		}
		if (!CONSOLE_AUTOOPEN && !CONSOLE_FLAG) setAlerteFlag(true);
		console.print(c, color, caret);
	}

	public void printlnToConsole(String str, Color color, boolean caret) {
		printToConsole(str + "\n", color, caret);
	}

	public void printToConsole(String str) {
		printToConsole(str, Console.COLOR_PRINT, false);
	}

	public void printlnToConsole(String str) {
		printlnToConsole(str, Console.COLOR_PRINT, false);
	}

	public void consoleLog(String str) {
		printlnToConsole("[codelab] " + str, Console.COLOR_LOG, false);
	}

	public void consoleLogErr(String str) {
		printlnToConsole("[codelab] " + str, Console.COLOR_ERROR, false);
	}
}

/*
	///////////////////////////////////////////////////
	// Interception du Cmd-R (compile & run)
	///////////////////////////////////////////////////

	private boolean cmd = false;
	private boolean cmd_R = false;
	private boolean key_R = false;
	private int cmd_code;

	public static String getShortcut() {
		// Pour l'affichage dans la bulle d'aide du bouton
		// invoqué dans updateRunTipText() de AbstractToolbar
		switch (SYSTEM) {
			case "Mac": return " [ cmd-R ]";
			case "Linux":
			case "Windows": return " [ Alt-R ]";
			default: return "";
		}
	}

	private void setRunKeyStroke() {
		// Ne fonctionne pas sous Linux => exécution dès action sur [R]
		switch (SYSTEM) {
			case "Mac": cmd_code = 157; break;
			case "Linux":
			case "Windows": cmd_code = 18; break;
		}
		Toolkit.getDefaultToolkit().addAWTEventListener(
			new AWTEventListener() {
				public void eventDispatched(AWTEvent e) {
					KeyEvent ke = (KeyEvent) e;
					int code = ke.getExtendedKeyCode();
					boolean KEY_PRESSED = (ke.getID() == KeyEvent.KEY_PRESSED);
					boolean KEY_RELEASED = (ke.getID() == KeyEvent.KEY_RELEASED);
					//System.out.println(code);
					if (KEY_PRESSED && code == cmd_code) cmd = true;
					if (KEY_PRESSED && code == 82) key_R = true;
					if (KEY_RELEASED && code == cmd_code) { cmd = false; cmd_R = false; }
					if (KEY_RELEASED && code == 82) { key_R = false; cmd_R = false; }
					if (cmd && key_R && !cmd_R) {
						runProgam();
						cmd_R = true;
					}
				}
			}, AWTEvent.KEY_EVENT_MASK);
	}

	private void runProgam() {
		// Pour exécuter un programme
		if (editor.isEmpty()) return;
		toolbar.action_run();
	}
*/

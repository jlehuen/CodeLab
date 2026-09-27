package codelab;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import codelab.controllers.devices.EventReader;
import codelab.controllers.devices.EventViewer;
import codelab.controllers.widgets.Joystick;
import codelab.controllers.widgets.Numpad;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;

/**
*	Classe abstraite des barres d'outils
*	@author Jérôme Lehuen
*	@version 10/05/25
*/

public abstract class AbstractToolbar extends JToolBar {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Méthodes de l'API empruntées à AbstractModule
	///////////////////////////////////////////////////

	public String getProperty(String key) {
		return module.getProperty(key);
	}

	public String getLangProperty(String key) {
		return module.getLangProperty(key);
	}

	public boolean getDataFlag() {
		return module.getDataFlag();
	}

	public ImageIcon loadImageIcon(String filename) {
		return module.loadImageIcon(filename);
	}

	public void printToConsole(String str, Color color) {
		module.printToConsole(str, color);
	}

	///////////////////////////////////////////////////
	// Chargement des icones
	///////////////////////////////////////////////////

	protected ImageIcon icon_start = ResourceUtils.loadImageIcon("icons/icon_start.png");
	protected ImageIcon icon_stop = ResourceUtils.loadImageIcon("icons/icon_stop.png");
	protected ImageIcon icon_reset = ResourceUtils.loadImageIcon("icons/icon_reset.png");
	protected ImageIcon icon_running = ResourceUtils.loadImageIcon("icons/icon_running.gif");

	protected ImageIcon icon_console = ResourceUtils.loadImageIcon("icons/icon_term.png");
	protected ImageIcon icon_console_auto = ResourceUtils.loadImageIcon("icons/icon_term_auto.png");
	protected ImageIcon icon_console_alert = ResourceUtils.loadImageIcon("icons/icon_term_alert.gif");

	protected ImageIcon icon_new = ResourceUtils.loadImageIcon("icons/icon_new.png");
	protected ImageIcon icon_magic = ResourceUtils.loadImageIcon("icons/icon_magic.png");
	protected ImageIcon icon_search = ResourceUtils.loadImageIcon("icons/icon_search.png");

	protected ImageIcon icon_numpad = ResourceUtils.loadImageIcon("icons/icon_numpad.png");
	protected ImageIcon icon_xbox360 = ResourceUtils.loadImageIcon("icons/icon_xbox360.png");
	protected ImageIcon icon_joystick = ResourceUtils.loadImageIcon("icons/icon_joystick.png");
	protected ImageIcon icon_dashboard = ResourceUtils.loadImageIcon("icons/icon_dashboard.png");

	protected ImageIcon icon_undo = ResourceUtils.loadImageIcon("icons/icon_undo.png");
	protected ImageIcon icon_redo = ResourceUtils.loadImageIcon("icons/icon_redo.png");

	protected ImageIcon icon_entab = ResourceUtils.loadImageIcon("icons/icon_entab.png");
	protected ImageIcon icon_detab = ResourceUtils.loadImageIcon("icons/icon_detab.png");
	protected ImageIcon icon_encom = ResourceUtils.loadImageIcon("icons/icon_encom.png");
	protected ImageIcon icon_decom = ResourceUtils.loadImageIcon("icons/icon_decom.png");

	protected ImageIcon icon_back = ResourceUtils.loadImageIcon("icons/icon_back.png");
	protected ImageIcon icon_info = ResourceUtils.loadImageIcon("icons/icon_codelab.png");
	protected ImageIcon icon_clear = ResourceUtils.loadImageIcon("icons/icon_clear.png");
	protected ImageIcon icon_tools = ResourceUtils.loadImageIcon("icons/icon_tools.png");
	protected ImageIcon icon_turtle = ResourceUtils.loadImageIcon("icons/icon_turtle.png");

	protected ImageIcon icon_help_on = ResourceUtils.loadImageIcon("icons/icon_help_on.png");
	protected ImageIcon icon_help_off = ResourceUtils.loadImageIcon("icons/icon_help_off.png");

	protected ImageIcon icon_toggle_on = ResourceUtils.loadImageIcon("icons/icon_toggle_on.png");
	protected ImageIcon icon_turtle_on = ResourceUtils.loadImageIcon("icons/icon_turtle_on.png");
	protected ImageIcon icon_toggle_off = ResourceUtils.loadImageIcon("icons/icon_toggle_off.png");
	protected ImageIcon icon_turtle_off = ResourceUtils.loadImageIcon("icons/icon_turtle_off.png");

	protected ImageIcon icon_server_on = ResourceUtils.loadImageIcon("icons/icon_server_on.png");
	protected ImageIcon icon_server_off = ResourceUtils.loadImageIcon("icons/icon_server_off.png");
	protected ImageIcon icon_server_activ = ResourceUtils.loadImageIcon("icons/icon_server_activ.png");

	protected ImageIcon icon_python_toggle_on = ResourceUtils.loadImageIcon("icons/icon_python_toggle_on.png");
	protected ImageIcon icon_python_toggle_off = ResourceUtils.loadImageIcon("icons/icon_python_toggle_off.png");

	protected ImageIcon logo_c = ResourceUtils.loadImageIcon("icons/logo_c.png");
	protected ImageIcon logo_go = ResourceUtils.loadImageIcon("icons/logo_go.png");
	protected ImageIcon logo_lua = ResourceUtils.loadImageIcon("icons/logo_lua.png");
	protected ImageIcon logo_lisp = ResourceUtils.loadImageIcon("icons/logo_lisp.png");
	protected ImageIcon logo_java = ResourceUtils.loadImageIcon("icons/logo_java.png");
	protected ImageIcon logo_clips = ResourceUtils.loadImageIcon("icons/logo_clips.png");
	protected ImageIcon logo_haskell = ResourceUtils.loadImageIcon("icons/logo_haskell.png");
	protected ImageIcon logo_python = ResourceUtils.loadImageIcon("icons/logo_python.png");
	protected ImageIcon logo_scratch = ResourceUtils.loadImageIcon("icons/logo_blocs.png");
	protected ImageIcon logo_processing = ResourceUtils.loadImageIcon("icons/logo_process.png");

	///////////////////////////////////////////////////
	// Boutons communs
	///////////////////////////////////////////////////

	protected ToolButton newButton;
	protected ToolButton startButton;
	protected ToolButton stopButton;
	protected ToolButton consoleButton;
	protected ToolButton serverButton;
	protected ToolButton infoButton;
	protected ToolButton helpButton;

	///////////////////////////////////////////////////
	// Autres propriétés
	///////////////////////////////////////////////////

	protected AbstractModule module;
	protected CodeLab codelab;

	// Le séparateur flexible
	protected JToolBar.Separator separator_flex = new JToolBar.Separator() {
		private static final long serialVersionUID = 1L;
		@Override
		public Dimension getMaximumSize(){
			return new Dimension(2000, 1);
		}
	};

	// Surchargé dans ToolBarEdit
	public void compile_on() {}
	public void compile_off() {}

	// Surchargé dans ToolBarEdit et ToolBarScratch
	public void setLang(String ext) {}

	// Surchargé dans ToolBarSim et ToolBarSim2
	public void click_action_data() {}

	// Pour simplifier...
	protected String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	private ModuleEditor getEditor() {
		return (codelab == null) ? null : codelab.getEditor();
	}

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public AbstractToolbar() {
		// Pour les panels qui ne sont pas des modules applicatifs
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		setBorderPainted(false);
		setFloatable(false);
		setBackground(new Color(0,0,0,0));
		setOpaque(false);
		helpButton = new ToolButton("HELP", null, LABEL("Btn_help_tooltip"), null, this); // Non visible par défaut
		serverButton = new ToolButton("SERVER", LABEL("Btn_server"), LABEL("Btn_server_tooltip"), icon_server_off, this);
		infoButton = new ToolButton("INFO", LABEL("Btn_info"), LABEL("Btn_info_tooltip"), icon_info, this);
	}

	public AbstractToolbar(AbstractModule module) {
		this.module = module;
		this.codelab = module.codelab;
		module.setToolbar(this);
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		setBorderPainted(false);
		setFloatable(false);
		// Boutons communs à tous les modules
		add(newButton = new ToolButton("NEW", LABEL("Btn_new"), LABEL("Btn_new_tooltip"), icon_new, this));
		add(startButton = new ToolButton("START", LABEL("Btn_start"), "--", icon_start, this));
		add(stopButton = new ToolButton("STOP", LABEL("Btn_stop"), LABEL("Btn_stop_tooltip"), icon_stop, this));
		add(consoleButton = new ToolButton("TERM", LABEL("Btn_term"), LABEL("Btn_term_tooltip"), icon_console_auto, this));
		helpButton = new ToolButton("HELP", null, LABEL("Btn_help_tooltip"), null, this); // Non visible par défaut
		serverButton = new ToolButton("SERVER", LABEL("Btn_server"), LABEL("Btn_server_tooltip"), icon_server_off, this);
		infoButton = new ToolButton("INFO", LABEL("Btn_info"), LABEL("Btn_info_tooltip"), icon_info, this);
	}

	public void updateRunTipText(String filename) {
		// Invoqué dans _post_construct() de AbstractCodeLab
		// Invoqué dans updateFrameTitle() de ModuleEditor
		String tipText;
		if (filename.isEmpty())
			tipText = LABEL("Btn_start_tooltip_bis");
		else
			tipText = String.format(LABEL("Btn_start_tooltip"), filename);
		//tipText += CodeLab.getShortcut();
		tipText += LABEL("Btn_start_tooltip_line2");
		startButton.setToolTipText(tipText);
	}

	///////////////////////////////////////////////////
	// Partie droite de la barre d'outils
	///////////////////////////////////////////////////

	protected void addSeparatorflex() {
		add(separator_flex);
	}

	protected void endToolbar() {
		add(helpButton);
		add(serverButton);
		add(infoButton);
	}

	///////////////////////////////////////////////////
	// Boutons optionnels
	///////////////////////////////////////////////////

	protected ToolButton resetButton;
	protected ToolButton dataButton;
	protected ToolButton numpadButton;
	protected ToolButton joystickButton;
	protected ToolButton controllerButton;

	protected void addButton(String ident) {
		switch (ident) {
			case "RESET":	add(resetButton = new ToolButton("RESET", LABEL("Btn_reset"), LABEL("Btn_reset_tooltip"), icon_reset, this)); break;
			case "DATA":	add(dataButton = new ToolButton("DATA", LABEL("Btn_data"), LABEL("Btn_data_tooltip"), icon_dashboard, this)); break;
			case "NUMPAD":	add(numpadButton = new ToolButton("NUMPAD", LABEL("Btn_numpad"), LABEL("Btn_numpad_tooltip"), icon_numpad, this)); break;
			case "STICK":	add(joystickButton = new ToolButton("STICK", LABEL("Btn_stick"), LABEL("Btn_stick_tooltip"), icon_joystick, this)); break;
			case "CTRL":
				add(controllerButton = new ToolButton("CTRL", LABEL("Btn_ctrl"), LABEL("Btn_ctrl_tooltip"), icon_xbox360, this));
				controllerButton.setEnabled(EventReader.natives_OK);
				break;

			default: System.out.println("ERROR unknown toolbar button ident: " + ident);
		}
	}

	private void updateOptionalButton() {
		if (numpadButton != null) {
			Numpad numpad = Numpad.INSTANCE;
			numpadButton.setEnabled(!numpad.isVisible());
		}
		if (joystickButton != null) {
			Joystick joystick = Joystick.INSTANCE;
			joystickButton.setEnabled(!joystick.isVisible());
		}
		if (EventReader.natives_OK && controllerButton != null) {
			EventViewer eventviewer = EventViewer.INSTANCE;
			controllerButton.setEnabled(!eventviewer.isVisible());
		}
	}

	///////////////////////////////////////////////////
	// Actualisation des boutons
	///////////////////////////////////////////////////

	public void update() {

		///////////////////////////////////////////////////
		// Actualisation des boutons communs

		boolean is_empty = codelab.getEditor().isEmpty();
		boolean is_executable = codelab.getEditor().isExecutable();
		boolean program_running = codelab.isRunning();
		boolean autoconsole = codelab.getAutoconsole();
		boolean is_connected = codelab.isConnected();
		boolean alerte = codelab.getAlerteFlag();
		boolean help = codelab.helpFlag;

		if (program_running) {
			startButton.setIcon(icon_running);
			startButton.setEnabled(true);
			stopButton.setEnabled(true);
			newButton.setEnabled(false);
		} else {
			startButton.setIcon(icon_start);
			if (is_empty || !is_executable) {
				// L'éditeur est vide
				startButton.setEnabled(false);
				stopButton.setEnabled(false);
			} else {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
			}
			ModuleEditor moduleEditor = getEditor();
			boolean editable_mode = (moduleEditor == null) || moduleEditor.getEditor().getEditable();
			newButton.setEnabled(!codelab.isTutor() && editable_mode);
		}
		if (autoconsole) consoleButton.setIcon(icon_console_auto);
		else if (alerte) consoleButton.setIcon(icon_console_alert);
		else consoleButton.setIcon(icon_console);

		// Actualisation des boutons optionnels
		updateOptionalButton();

		// État des boutons [server] et [help]
		serverButton.setEnabled(CodeLab.CONNECTED_MODE && CodeLab.WRK_SERVER_OK);

		if (is_connected) {
			serverButton.setIcon(icon_server_on);
			if (codelab.isStudent()) {
				helpButton.setTextProp(LABEL("Btn_help"));
				if (help) helpButton.setIcon(icon_help_on);
				else helpButton.setIcon(icon_help_off);
			}
		}
		else {
			serverButton.setIcon(icon_server_off);
			helpButton.setTextProp(null);
			helpButton.setIcon(null);
		}

		// Actualisation de la LED
		serverActivityLED();

		// Actualisation des textes en dessous
		for (Component c : getComponents())
			if (c instanceof ToolButton) ((ToolButton)c).update();
	}

	///////////////////////////////////////////////////
	// Indicateur d'activité réseau
	///////////////////////////////////////////////////

	private boolean server_flag = false;

	public void serverActivity() {
		server_flag = true;
	}

	public void serverActivityLED() {
		if (server_flag) {
			new Thread(() -> {
				Utils.wait(200);
				serverButton.setIcon(icon_server_activ);
				Utils.wait(100);
				serverButton.setIcon(icon_server_on);
				Utils.wait(100);
				serverButton.setIcon(icon_server_activ);
				Utils.wait(100);
				serverButton.setIcon(icon_server_on);
			}).start();
			server_flag = false;
		}
	}

	///////////////////////////////////////////////////
	// Gestionnaire d'actions
	///////////////////////////////////////////////////

	public void action(String ident) {
		switch (ident) {
			case "NEW":		action_new(); break;
			case "START":	action_run(); break;
			case "STOP":	action_stop(); break;
			case "RESET":	action_reset(); break;
			case "HELP":	action_help(); break;
			case "TERM":	module.toggleConsole(); break;
			case "DATA":	module.toggleDataTable(); break;
			case "NUMPAD":	action_open_widget(1); break;
			case "STICK":	action_open_widget(2); break;
			case "CTRL":	action_open_widget(3); break;
			case "INFO":	break; // Prise en charge par InfoPopupMenu
			case "SERVER":	break; // Prise en charge par ServerPopupMenu
			default: System.out.println("ERROR toolbar unknown ident: " + ident);
		}
	}

	public void action_new() {
		SwingUtilities.invokeLater(() -> {
			// Évite le blocage de focus au niveau de l'éditeur
			ModuleEditor editor = getEditor();
			codelab.setSelectedModule(editor);
			editor.new_file();
		});
	}

	public void action_run() {
		codelab.execute();
	}

	public void action_stop() {
		codelab.halt();
		module.stop(); // En fait, déjà invoqué dans executionCompleted()...
		module.dashboardUpdate();
	}

	public void action_reset() {
		codelab.halt();
		module.reset();
		module.dashboardUpdate();
	}

	public void action_help() {
		codelab.toggleHelpFlag();
		update(); // Icone help
	}

	private void action_open_widget(int i) {
		Point p1 = codelab.getLocation();
		Point p2 = numpadButton.getAbsolutePosition();
		Point p3 = new Point(p1.x + p2.x - 60, p1.y + p2.y + 40);
		switch (i) {
			case 1: Numpad.INSTANCE.open(p3); break;
			case 2: Joystick.INSTANCE.open(p3); break;
			case 3: EventViewer.INSTANCE.open(p3); break;
		}
		codelab.toolbar_update();
	}

	public void test_exception() {
		try {
			throw new Exception("test_exception");
		}
		catch (Exception e) {
			ExceptionManager.process(e);
		}
	}
}

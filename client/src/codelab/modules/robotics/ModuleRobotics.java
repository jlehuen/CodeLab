package codelab.modules.robotics;

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import codelab.AbstractModule;
import codelab.CodeLab;
import codelab.console.Console;
import codelab.modules.robotics.robots.robotLego.RobotLego;
import codelab.modules.robotics.robots.robotRovio.RobotRovio;
import codelab.utils.Utils;

/**
*	Classe du module simulateur de robots mobiles
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public class ModuleRobotics extends AbstractModule {

	public final double SMIN = 0.4; // Zoom mini
	public final double SMAX = 1.5; // Zoom maxi
	public final int ZOOM_INIT = 10; // Zoom initial (x10)

	public static final String USER_ROBOT_FOLDER = CodeLab.USER_DATA_FOLDER + "/mod_robotics/robots";
	public static final String USER_BACK_FOLDER = CodeLab.USER_DATA_FOLDER + "/mod_robotics/backgrounds";

	public static final String SYSTEM_ROBOTS_XML = "/data/simulator/robots.xml";
	public static final String USER_ROBOTS_XML = USER_ROBOT_FOLDER + "/robots.xml";

	private Simulator simulator;
	private JScrollPane scrollpane;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ModuleRobotics(CodeLab codelab) {
		super(codelab);
		System.out.print("   Loading module robotics... ");
		Utils.createDirectory(USER_ROBOT_FOLDER); // If not exists
		Utils.createDirectory(USER_BACK_FOLDER); // If not exists

		title = LABEL("Tab_simulator");
		toolbar = new ToolbarSim(this);
		scrollpane = new JScrollPane(); // Avant simulator à cause du updateViewport() du simulateur

		simulator = new Simulator(this, dashboard);
		simulator.setScale((double) ZOOM_INIT / 10);

		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setBorder(BorderFactory.createEmptyBorder());
		scrollpane.setViewportView(simulator);
		// Spécifier le composant principal
		setComponent(scrollpane);
		System.out.println("DONE");
	}

	///////////////////////////////////////////////////
	// Getters et Setters
	///////////////////////////////////////////////////

	public Simulator getSimulator() {
		return simulator;
	}

	public JViewport getViewport() {
		return scrollpane.getViewport();
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractModule à implémenter
	///////////////////////////////////////////////////

	public void init() {
		simulator.init();
	}

	public void exit() {}

	public void start() {
		simulator.start();
	}

	public void stop() {
		simulator.stop();
	}

	public void reset() {
		simulator.stop();
		simulator.reset();
		scrollpane.getHorizontalScrollBar().setValue(0);
		scrollpane.getVerticalScrollBar().setValue(0);
	}

	///////////////////////////////////////////////////
	// Autres méthodes publiques
	///////////////////////////////////////////////////

	public void moveHorizontalScrollBar(int dx) {
		SwingUtilities.invokeLater(() -> {
			try {
				JScrollBar hsb = scrollpane.getHorizontalScrollBar();
				hsb.setValue(hsb.getValue() + dx);
			}
			catch (Exception ignored) {}
		});
	}

	public void moveVerticalScrollBar(int dy) {
		SwingUtilities.invokeLater(() -> {
			try {
				JScrollBar vsb = scrollpane.getVerticalScrollBar();
				vsb.setValue(vsb.getValue() + dy);
			}
			catch (Exception ignored) {}
		});
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void errorUnknownBackground(String name) {
		String message = String.format(LABEL("ModuleRobotics_error_0"), name);
		simulator.printToConsole(message, Console.COLOR_ERROR);
	}

	private void errorUnknownRobotConfig(String name) {
		String message = String.format(LABEL("ModuleRobotics_error_1"), name);
		simulator.printToConsole(message, Console.COLOR_ERROR);
	}

	private String robotError(String cmd, String name) {
		String message = String.format(LABEL("ModuleRobotics_error_2"), cmd, name);
		simulator.printToConsole(message, Console.COLOR_ERROR);
		codelab.halt(); // On arrête l'exécution
		return "-1";
	}

	///////////////////////////////////////////////////
	// Gestionnaire de requêtes principal
	///////////////////////////////////////////////////

	public String handleRequest(Map<String, String> params) {
		String cmd = params.get("cmd");
		String name;
		int res;

		switch (cmd) {
			case "setBackground":
				name = params.get("name");
				res = simulator.setBackgroundFromName(name);
				if (res == -1) errorUnknownBackground(name);
				return String.valueOf(res);
			case "setRobotConfiguration":
				name = params.get("name");
				res = simulator.setRobotFromName(name);
				if (res == -1) errorUnknownRobotConfig(name);
				return String.valueOf(res);

			// Les toolbar.update() c'est pour la version CodeLabWS
			case "simu_start": simulator.start(); toolbar.update(); return "";
			case "simu_stop": simulator.stop(); toolbar.update(); return "";
		}
		String robot = params.get("robot");
		switch (robot) {
			case "lego": return handleLego(params);
			case "rovio": return handleRovio(params);
			default:
				String msg = String.format("ERROR: unknown robot [%s]\n", robot);
				System.out.println(msg);
				return msg;
		}
	}

	///////////////////////////////////////////////////
	// Gestionnaire de requêtes du robot Lego
	///////////////////////////////////////////////////

	private String handleLego(Map<String, String> params) {
		String cmd = params.get("cmd");
		RobotLego robot;
		int port, power, index;

		try {
			robot = (RobotLego) simulator.getRobot();
		}
		catch (ClassCastException e) {
			// Pas le bon robot dans le simulateur
			return robotError(cmd, "Lego NXT");
		}
		switch (cmd) {
			case "motorOff":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(robot.motorOff(port));
			case "motorOn":
				port = Integer.valueOf(params.get("port"));
				power = Integer.valueOf(params.get("power"));
				return String.valueOf(robot.motorOn(port, power));
			case "getSensorValue":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(robot.getSensorValue(port));
			case "getSensorArray":
				port = Integer.valueOf(params.get("port"));
				index = Integer.valueOf(params.get("index"));
				return String.valueOf(robot.getSensorArray(port, index));
			case "getMotorRotationCount":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(robot.getMotorRotationCount(port));
			case "resetMotorRotationCount":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(robot.resetMotorRotationCount(port));
			default:
				return unknownCommandError(cmd);
		}
	}

	///////////////////////////////////////////////////
	// Gestionnaire de requêtes du robot Rovio
	///////////////////////////////////////////////////

	private String handleRovio(Map<String, String> params) {
		String cmd = params.get("cmd");
		RobotRovio robot;
		int port, power;

		try {
			robot = (RobotRovio) simulator.getRobot();
		}
		catch (ClassCastException e) {
			// Pas le bon robot dans le simulateur
			return robotError(cmd, "Rovio");
		}
		switch (cmd) {
			case "motorOff":
				port = Integer.valueOf(params.get("port"));
				return String.valueOf(robot.motorOff(port));
			case "motorOn":
				port = Integer.valueOf(params.get("port"));
				power = Integer.valueOf(params.get("power"));
				return String.valueOf(robot.motorOn(port, power));
			default:
				return unknownCommandError(cmd);

		}
	}
}

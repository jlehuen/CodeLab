package codelab.modules.robotics.robots.robotLego;

import org.xml.sax.Attributes;

import codelab.DataTable;
import codelab.console.Console;
import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.Robot2weels;
import codelab.utils.xml.XMLObject;

/**
*	Classe du robot NXT à 2 roues
*	@author Jérôme Lehuen
*	@version 10/12/22
*/

// Piste: 100 x 142 cm = 870 x 1236
// Echelle: 1 cm = 8,7 pixels

public class RobotLego extends Robot2weels {

	public void setDashboard(DataTable dashboard) {}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public RobotLego(Simulator simulator) {
		super(simulator);
		CENTER_X = 136; // Centre de l'objet en pixels (déplacable)
		CENTER_Y = 70; // Centre de l'objet en pixels (déplacable)
		ENTRAXE = 120; // Entraxe du robot en pixels (13,8 cm)
		RAYON = 19.1; // Rayon des roues en pixels
		K_PUISS = 8; // Diviseur de puissance

		setImage("robot_lego");

		propKey[0] = LABEL("RobotLego_key_0");
		propKey[1] = LABEL("RobotLego_key_1");
		propKey[2] = LABEL("RobotLego_key_2");
		propKey[3] = "";
		propKey[4] = LABEL("RobotLego_key_4");
		propKey[5] = "";
		propKey[6] = "";
		propKey[7] = LABEL("RobotLego_key_7");
		propKey[8] = "";
		propKey[9] = "";
		propKey[10] = LABEL("RobotLego_key_10");
		propKey[11] = "";
		propKey[12] = "";
		propKey[13] = LABEL("RobotLego_key_13");
		propKey[14] = LABEL("RobotLego_key_14");
		propKey[15] = LABEL("RobotLego_key_15");
		propKey[16] = LABEL("RobotLego_key_16");
		nbProp = 17;
	}

	public String getInfo() {
		return "the NXT robot";
	}

	public String getConfiguration() {
		String config = LABEL("RobotLego_getConfig");
		config += " ";
		for (int i = 0 ; i < 4 ; i++) {
			if (capteurs[i] != null)
				config += capteurs[i].getDescr();
			else
				config += "NONE";
			if (i < 3) config += " / ";
		}
		return config;
	}

	public void updateValues() {
		double pos_x = (x + CENTER_X) / simulator.SCALE * 10;
		double pos_y = (y + CENTER_Y) / simulator.SCALE * 10;

		propVal[0] = LABEL("RobotLego_val_0");
		propVal[1] = String.format("%s", name);
		propVal[2] = String.format("X = %3.1f cm  |  Y = %3.1f cm", pos_x, pos_y);
		propVal[3] = String.format("Angle = %d°", getAngle());

		propVal[4] = "NONE";
		propVal[5] = "NONE";
		propVal[6] = "NONE";

		propVal[7] = LABEL("RobotLego_val_7");
		propVal[8] = String.format(LABEL("RobotLego_val_puiss"), pgauche);
		propVal[9] = String.format(LABEL("RobotLego_val_cmpt"), (int)Math.toDegrees(cgauche));

		propVal[10] = LABEL("RobotLego_val_10");
		propVal[11] = String.format(LABEL("RobotLego_val_puiss"), pdroite);
		propVal[12] = String.format(LABEL("RobotLego_val_cmpt"), (int)Math.toDegrees(cdroite));

		propVal[13] = (getSensor(0) != null) ? getSensor(0).getStringValue() : "NONE";
		propVal[14] = (getSensor(1) != null) ? getSensor(1).getStringValue() : "NONE";
		propVal[15] = (getSensor(2) != null) ? getSensor(2).getStringValue() : "NONE";
		propVal[16] = (getSensor(3) != null) ? getSensor(3).getStringValue() : "NONE";
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface XMLObject
	///////////////////////////////////////////////////

	public XMLObject add(String element, Attributes attributs) {
		super.add(element, attributs);
		if (element.equals("touch")) return new SensorTouch(this, simulator);
		else if (element.equals("color")) return new SensorColor(this, simulator);
		else if (element.equals("sonic")) return new SensorSonic(this, simulator);
		else if (element.equals("numpad")) return new SensorNumpad(this, simulator);
		else if (element.equals("compas")) return new SensorCompas(this, simulator);
		else if (element.equals("array")) {
			// Il faut adapter l'image du robot pour visualiser le capteur
			setImage("robot_lego_array");
			return new SensorArray(this, simulator);
		}
		else {
			String message = String.format(LABEL("RobotLego_Error_0"), element);
			simulator.printToConsole(message, Console.COLOR_ERROR);
			return null;
		}
	}

	public void set(String attribut, String valeur) {
		super.set(attribut, valeur);
	}

	public void end() {
		super.end();
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le serveur (moteurs)
	///////////////////////////////////////////////////

	public int motorOn(int port, int puissance) {
		if (Math.abs(puissance) > 100) {
			String message = String.format(LABEL("RobotLego_Error_1"), puissance, port);
			simulator.printToConsole(message, Console.COLOR_ERROR);
			simulator.stop();
			simulator.getModule().codelab.halt();
			return -1;
		}
		switch (port) {
			case 1: __setMotorLeft(puissance); return 1;
			case 2: __setMotorRight(puissance); return 1;
			case 5: __setMotorBoth(puissance); return 1;
		}
		String message = String.format(LABEL("RobotLego_Error_2"), port);
		simulator.printToConsole(message, Console.COLOR_ERROR);
		simulator.stop();
		simulator.getModule().codelab.halt();
		return -1;
	}

	public int motorOff(int port) {
		return motorOn(port, 0);
	}

	public int resetMotorRotationCount(int port) {
		switch (port) {
			case 1: __resetMotorLeft(); return 1;
			case 2: __resetMotorRight(); return 1;
		}
		String message = String.format(LABEL("RobotLego_Error_3"), port);
		simulator.printToConsole(message, Console.COLOR_ERROR);
		simulator.stop();
		simulator.getModule().codelab.halt();
		return -1;
	}

	public int getMotorRotationCount(int port) {
		switch (port) {
			case 1: return __getMotorLeft();
			case 2: return __getMotorRight();
		}
		String message = String.format(LABEL("RobotLego_Error_3"), port);
		simulator.printToConsole(message, Console.COLOR_ERROR);
		simulator.stop();
		simulator.getModule().codelab.halt();
		return -1;
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le serveur (capteurs)
	///////////////////////////////////////////////////

	public int getSensorValue(int port) {
		//int port = Integer.valueOf(portCode.charAt(3)) - 49;
		if (port < 0 || port > 3 || capteurs[port] == null) {
			String message = String.format(LABEL("RobotLego_Error_2"), port);
			simulator.printToConsole(message, Console.COLOR_ERROR);
			simulator.stop();
			simulator.getModule().codelab.halt();
			return -1;
		}
		return capteurs[port].getValue();
	}

	public int getSensorArray(int port, int index) {
		//int port = Integer.valueOf(portCode.charAt(3)) - 49;
		if (port < 0 || port > 3 || capteurs[port] == null) {
			String message = String.format(LABEL("RobotLego_Error_2"), port);
			simulator.printToConsole(message, Console.COLOR_ERROR);
			simulator.stop();
			simulator.getModule().codelab.halt();
			return -1;
		}
		return capteurs[port].getValue(index);
	}
}

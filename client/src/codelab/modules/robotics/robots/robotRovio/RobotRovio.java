package codelab.modules.robotics.robots.robotRovio;

import codelab.modules.robotics.*;
import codelab.modules.robotics.robots.*;
import codelab.DataTable;
import codelab.console.Console;

/**
*	Classe du robot Rovio à 3 roues
*	@author Jérôme Lehuen
*	@version 15/12/22
*/

public class RobotRovio extends Robot3weels {

	public void setDashboard(DataTable dashboard) {}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public RobotRovio(Simulator simulator) {
		super(simulator);
		CENTER_X = 180; // Centre de l'objet en pixels (déplacable)
		CENTER_Y = 132; // Centre de l'objet en pixels (déplacable) OK
		ENTRAXE_RG = 110; // Distance roue gauche au centre en pixels
		ENTRAXE_RD = 110; // Distance roue droite au centre en pixels
		ENTRAXE_RC = 110; // Distance roue centre au centre en pixels
		ANGLE_RG = -60; // Angle axe gauche au centre en degrés
		ANGLE_RD = 60; // Angle axe droite au centre en degrés
		ANGLE_RC = 180; // Angle axe centre au centre en degrés OK
		RAYON = 20; // Rayon des roues en pixels
		K_PUISS = 1; // Diviseur de puissance

		setImage("robot_rovio");

		propKey[0] = " Type du robot :";
		propKey[1] = " Position :";
		propKey[2] = "";
		propKey[3] = " Moteur gauche :";
		propKey[4] = "";
		propKey[5] = " Moteur droit :";
		propKey[6] = "";
		propKey[7] = " Moteur arrière :";
		propKey[8] = "";
		nbProp = 9;
	}

	public String getConfiguration() {
		String config = "Type de robot : Rovio\nConfiguration :";
		return config;
	}

	public void updateValues() {
		propVal[0] = " Robot Rovio";
		propVal[1] = String.format(" Centre = (%d, %d)", (int)x, (int)y);
		propVal[2] = String.format(" Angle = %d°", ((int)Math.toDegrees(theta)) % 360);
		propVal[3] = String.format(" Puissance = %d", pgauche);
		propVal[4] = String.format(" Compteur = %d°", (int)Math.toDegrees(cgauche));
		propVal[5] = String.format(" Puissance = %d", pdroite);
		propVal[6] = String.format(" Compteur = %d°", (int)Math.toDegrees(cdroite));
		propVal[7] = String.format(" Puissance = %d", pcentre);
		propVal[8] = String.format(" Compteur = %d°", (int)Math.toDegrees(ccentre));
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le serveur (moteurs)
	///////////////////////////////////////////////////

	public int motorOn(int port, int puissance) {
		switch (port) {
			case 0: __setMotorLeft(puissance); return 1;
			case 1: __setMotorRight(puissance); return 1;
			case 2: __setMotorCenter(puissance); return 1;
			case 3: __setMotorLR(puissance); return 1;
			case 4: __setMotorALL(puissance); return 1;
		}
		String message = String.format("Error: port %d does not exist", port);
		simulator.printToConsole(message, Console.COLOR_ERROR);
		simulator.stop();
		simulator.getModule().codelab.halt();
		return -1;
	}

	public int motorOff(int port) {
		return motorOn(port, 0);
	}
}

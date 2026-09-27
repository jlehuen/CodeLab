package codelab.modules.robotics.robots.robotRoomba;

import codelab.DataTable;
import codelab.modules.robotics.*;
import codelab.modules.robotics.robots.*;

/**
*	Classe du robot Roomba
*	@author Jérôme Lehuen
*	@version 10/12/22
*/

public class RobotRoomba extends Robot2weels {

	private SensorLaser laser;

	public void setDashboard(DataTable dashboard) {}

	/**
	*	Constructeur de la classe RobotLego
	*/

	public RobotRoomba(Simulator simulator) {
		super(simulator);
		CENTER_X = 125; // Centre de l'objet en pixels (déplacable)
		CENTER_Y = 125; // Centre de l'objet en pixels (déplacable)
		ENTRAXE = 180; // Entraxe du robot en pixels
		RAYON = 20; // Rayon des roues en pixels
		K_PUISS = 8; // Diviseur de puissance

		setImage("robot_roomba");
		addSensor(0, laser = new SensorLaser(this, simulator));

		propKey[0] = " Type du robot :";
		propKey[1] = " Position :";
		propKey[2] = "";
		propKey[3] = " Moteur gauche :";
		propKey[4] = "";
		propKey[5] = " Moteur droit :";
		propKey[6] = "";
		propKey[7] = " Télémètre laser :";
		nbProp = 8;
	}

	public void reset() {
		super.reset();
		laser.reset();
	}

	public String getConfiguration() {
		String config = "Type de robot : Roomba\nConfiguration :";
		return config;
	}

	public void updateValues() {
		propVal[0] = " Robot Roomba";
		propVal[1] = String.format(" Centre = (%d, %d)", (int)x, (int)y);
		propVal[2] = String.format(" Angle = %d°", ((int)Math.toDegrees(theta)) % 360);
		propVal[3] = String.format(" Puissance = %d", pgauche);
		propVal[4] = String.format(" Compteur = %d°", (int)Math.toDegrees(cgauche));
		propVal[5] = String.format(" Puissance = %d", pdroite);
		propVal[6] = String.format(" Compteur = %d°", (int)Math.toDegrees(cdroite));
		propVal[7] = laser.getStringValue();
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le serveur (moteurs)
	///////////////////////////////////////////////////

	public int setMotorLeft(int puissance) {
		if (Math.abs(puissance) > 100) {
			runtimeError(String.format("ERREUR: puissance moteur interdite: %d\n", puissance));
			simulator.stop();
			simulator.getModule().codelab.halt();
			return -1;
		}
		__setMotorLeft(puissance);
		return 1;
	}

	public int setMotorRight(int puissance) {
		if (Math.abs(puissance) > 100) {
			runtimeError(String.format("ERREUR: puissance moteur interdite: %d\n", puissance));
			simulator.stop();
			simulator.getModule().codelab.halt();
			return -1;
		}
		__setMotorRight(puissance);
		return 1;
	}

	public int resetMotorRotationCount() {
		__resetMotorLeft();
		__resetMotorRight();
		return 1;
	}

	public int getMotorLeftRotationCount() {
		return __getMotorLeft();
	}

	public int getMotorRightRotationCount() {
		return __getMotorRight();
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le serveur (capteur)
	///////////////////////////////////////////////////

	public int startScan() {
		laser.startScan();
		return 1;
	}

	public int scanning() {
		return laser.scanning();
	}

	public int getValue(int angle) {
		return laser.getValue(angle);
	}
}

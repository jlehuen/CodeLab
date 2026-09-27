package codelab.modules.robotics.robots;

import codelab.modules.robotics.*;
import java.awt.Graphics2D;
import java.awt.Point;

/**
*	Classe abstraite des robots à 2 roues
*	@author Jérôme Lehuen
*	@version $Date$
*/

public abstract class Robot2weels extends AbstractRobot {

	// Propriétés propres au robot

	public int ENTRAXE; // Entraxe du robot en pixels
	public double RAYON; // Rayon des roues en pixels
	public double K_PUISS; // Diviseur de puissance

	// Variables du modèle cinématique

	protected int pgauche = 0;
	protected int pdroite = 0;
	protected double vgauche = 0; // Vitesse angulaire de la roue gauche en radians/seconde
	protected double vdroite = 0; // Vitesse angulaire de la roue droite en radians/seconde
	protected double cgauche = 0; // Compteur de rotation de la roue gauche
	protected double cdroite = 0; // Compteur de rotation de la roue droite

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Robot2weels(Simulator simulator) {
		super(simulator);
	}

	///////////////////////////////////////////////////
	// Méhodes publiques
	///////////////////////////////////////////////////

	public void stop() {
		pgauche = 0;	vgauche = 0;
		pdroite = 0;	vdroite = 0;
	}

	public void reset() {
		x = X_RESET;
		y = Y_RESET;
		theta = 0;
		pgauche = 0;	vgauche = 0;	cgauche = 0;
		pdroite = 0;	vdroite = 0;	cdroite = 0;
	}

	public void iterate() {
		// Calcul de la position et de l'angle du robot
		double v = RAYON * (vgauche + vdroite) / 2; // Vitesse d'avancement en pixels par seconde
		double w = RAYON * (vgauche - vdroite) / ENTRAXE; // Vitesse de rotation en radians par seconde
		double dx = v * Math.cos(theta) * simulator.dt; // Déplacement en pixels
		double dy = v * Math.sin(theta) * simulator.dt; // Déplacement en pixels

		if (!repositionnement) {
			x += dx; // Nouvelle position du robot en pixels
			y += dy; // Nouvelle position du robot en pixels
			theta += w * simulator.dt; // Nouvel angle du robot en radians
			cgauche += vgauche * simulator.dt; // Compteur de rotation gauche en radians
			cdroite += vdroite * simulator.dt; // Compteur de rotation droite en radians
		}
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
		if (simulator.DEBUG) {
			// Marquage des roues et des compteurs de rotation
			Point roue_gauche = convert(new Point(0, -ENTRAXE / 2));
			Point roue_droite = convert(new Point(0, ENTRAXE / 2));
			drawMotorRotationCount(g2d, roue_gauche.x, roue_gauche.y, Math.toDegrees(cgauche));
			drawMotorRotationCount(g2d, roue_droite.x, roue_droite.y, Math.toDegrees(cdroite));
		}
	}

	///////////////////////////////////////////////////
	// Accès aux moteurs
	///////////////////////////////////////////////////

	protected void __setMotorLeft(int puissance) {
		double vitesse = (double) puissance / K_PUISS;
		pgauche = puissance;
		vgauche = vitesse;
	}

	protected void __setMotorRight(int puissance) {
		double vitesse = (double) puissance / K_PUISS;
		pdroite = puissance;
		vdroite = vitesse;
	}

	protected void __setMotorBoth(int puissance) {
		double vitesse = (double) puissance / K_PUISS;
		pgauche = puissance;
		pdroite = puissance;
		vgauche = vitesse;
		vdroite = vitesse;
	}

	protected void __resetMotorLeft() {
		__setMotorLeft(0);
		cgauche = 0;
	}

	protected void __resetMotorRight() {
		__setMotorRight(0);
		cdroite = 0;
	}

	protected int __getMotorLeft() {
		return (int) Math.toDegrees(cgauche);
	}

	protected int __getMotorRight() {
		return (int) Math.toDegrees(cdroite);
	}
}

package codelab.modules.robotics.robots;

import codelab.modules.robotics.*;
import java.awt.Graphics2D;
import java.awt.Point;
import codelab.utils.*;

/**
*	Classe abstraite des robots à 3 roues
*	@author Jérôme Lehuen
*	@version 15/12/22
*/

public abstract class Robot3weels extends AbstractRobot {

	// Propriétés propres au robot

	public int ENTRAXE_RG; // Distance roue gauche au centre en pixels
	public int ENTRAXE_RD; // Distance roue droite au centre en pixels
	public int ENTRAXE_RC; // Distance roue centre au centre en pixels
	public int ANGLE_RG; // Angle axe gauche au centre en degrés
	public int ANGLE_RD; // Angle axe droite au centre en degrés
	public int ANGLE_RC; // Angle axe centre au centre en degrés
	public double RAYON; // Rayon des roues en pixels
	public double K_PUISS; // Diviseur de puissance

	// Variables du modèle cinématique

	protected int pgauche = 0;
	protected int pdroite = 0;
	protected int pcentre = 0;
	protected double vgauche = 0; // Vitesse angulaire de la roue gauche en radians/seconde
	protected double vdroite = 0; // Vitesse angulaire de la roue droite en radians/seconde
	protected double vcentre = 0; // Vitesse angulaire de la roue arrière en radians/seconde
	protected double cgauche = 0; // Compteur de rotation de la roue gauche
	protected double cdroite = 0; // Compteur de rotation de la roue droite
	protected double ccentre = 0; // Compteur de rotation de la roue arrière

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Robot3weels(Simulator simulator) {
		super(simulator);
	}

	///////////////////////////////////////////////////
	// Méhodes publiques
	///////////////////////////////////////////////////

	public void stop() {
		pgauche = 0;	vgauche = 0;
		pdroite = 0;	vdroite = 0;
		pcentre = 0;	vcentre = 0;
	}

	public void reset() {
		x = X_RESET;
		y = Y_RESET;
		theta = 0;
		pgauche = 0;	vgauche = 0;	cgauche = 0;
		pdroite = 0;	vdroite = 0;	cdroite = 0;
		pcentre = 0;	vcentre = 0;	ccentre = 0;
	}

	public void iterate() {
		// Calcul de la position et de l'angle du robot

		double va = vgauche;
		double vb = vdroite;
		double vc = vcentre;

		// Vitesse d'avancement en pixels par seconde
		double v = Math.sqrt(Math.pow((va + vb - 2 * vc) / 3, 2) + Math.pow(va - vb, 2) / 3);

		// Vitesse de rotation en radians par seconde
		double w = (va + vb + vc) / (3 * ENTRAXE_RG);
		double dx = v * Math.cos(theta) * simulator.dt; // Déplacement en pixels
		double dy = v * Math.sin(theta) * simulator.dt; // Déplacement en pixels

		if (!repositionnement) {
			x += dx; // Nouvelle position du robot en pixels
			y += dy; // Nouvelle position du robot en pixels
			theta += w * simulator.dt; // Nouvel angle du robot en radians
			cgauche += vgauche * simulator.dt; // Compteur de rotation gauche en radians
			cdroite += vdroite * simulator.dt; // Compteur de rotation droite en radians
			ccentre += vcentre * simulator.dt; // Compteur de rotation droite en radians
		}
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
		if (simulator.DEBUG) {
			// Marquage des roues et des compteurs de rotation
			Point origine = new Point((int)x + CENTER_X, (int)y + CENTER_Y);
			Point roue_gauche = convert(GraphUtils.pol2cart(ENTRAXE_RG, ANGLE_RG));
			Point roue_droite = convert(GraphUtils.pol2cart(ENTRAXE_RD, ANGLE_RD));
			Point roue_centre = convert(GraphUtils.pol2cart(ENTRAXE_RC, ANGLE_RC));
			GraphUtils.drawLine(g2d, origine.x, origine.y, roue_gauche.x, roue_gauche.y);
			GraphUtils.drawLine(g2d, origine.x, origine.y, roue_droite.x, roue_droite.y);
			GraphUtils.drawLine(g2d, origine.x, origine.y, roue_centre.x, roue_centre.y);
			drawMotorRotationCount(g2d, roue_gauche.x, roue_gauche.y, Math.toDegrees(cgauche));
			drawMotorRotationCount(g2d, roue_droite.x, roue_droite.y, Math.toDegrees(cdroite));
			drawMotorRotationCount(g2d, roue_centre.x, roue_centre.y, Math.toDegrees(ccentre));
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

	protected void __setMotorCenter(int puissance) {
		double vitesse = (double) puissance / K_PUISS;
		pcentre = puissance;
		vcentre = vitesse;
	}

	protected void __setMotorLR(int puissance) {
		__setMotorLeft(puissance);
		__setMotorRight(puissance);
	}

	protected void __setMotorALL(int puissance) {
		__setMotorLeft(puissance);
		__setMotorRight(puissance);
		__setMotorCenter(puissance);
	}

	protected void __resetMotorLeft() {
		__setMotorLeft(0);
		cgauche = 0;
	}

	protected void __resetMotorRight() {
		__setMotorRight(0);
		cdroite = 0;
	}

	protected void __resetMotorCenter() {
		__setMotorCenter(0);
		ccentre = 0;
	}

	protected int __getMotorLeft() {
		return (int) Math.toDegrees(cgauche);
	}

	protected int __getMotorRight() {
		return (int) Math.toDegrees(cdroite);
	}

	protected int __getMotorCenter() {
		return (int) Math.toDegrees(ccentre);
	}
}

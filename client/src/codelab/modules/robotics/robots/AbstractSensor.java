package codelab.modules.robotics.robots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

import org.xml.sax.Attributes;

import codelab.modules.robotics.AbstractDeplacable;
import codelab.modules.robotics.Simulator;
import codelab.utils.GraphUtils;
import codelab.utils.xml.XMLObject;

/**
*	Classe abstraite des capteurs
*	@author Jérôme Lehuen
*	@version 07/12/20
*/

public abstract class AbstractSensor extends AbstractDeplacable implements XMLObject {

	protected String descr;
	protected AbstractRobot robot;
	protected int port;

	protected int X_ROBOT; // Position du capteur en x par rapport au centre du robot
	protected int Y_ROBOT; // Position du capteur en y par rapport au centre du robot
	protected double THETA_ROBOT; // Orientation du capteur en radians
	protected int DX; // Centre du capteur en x
	protected int DY; // Centre du capteur en y

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractSensor(AbstractRobot robot, Simulator simulator, String ident, String descr) {
		super(simulator, ident);
		this.descr = descr;
		this.robot = robot;
	}

	///////////////////////////////////////////////////
	// Méhodes publiques
	///////////////////////////////////////////////////

	public int getValue() { return 0; }
	public int getValue(int index) { return 0; }
	public String getStringValue() { return null; }
	public String getDescr() { return descr; }

	public void draw(Graphics2D g2d) {
		// Affichage du capteur dans le référentiel du robot
		AffineTransform affineTransform = (AffineTransform) robot.affineTransform.clone();
		affineTransform.translate(robot.CENTER_X + X_ROBOT - DX, robot.CENTER_Y + Y_ROBOT - DY);
		affineTransform.rotate(THETA_ROBOT, DX, DY);
		g2d.drawImage(image, affineTransform, null);
		//if (!simulator.DEBUG) g2d.drawImage(image, affineTransform, null);

		// Calcul du contour de l'objet
		transformedPath = (Path2D.Double) path.clone();
		transformedPath.transform(affineTransform);

		// Affichage des informations de débugage
		if (simulator.DEBUG) {
			g2d.setColor(Color.red);
			g2d.draw(transformedPath);

			// Marquage des extrémités des segments
			for (PathIterator it = transformedPath.getPathIterator(null); !it.isDone(); it.next()) {
				it.currentSegment(coords);
				GraphUtils.drawCircle(g2d, (int)coords[0], (int)coords[1], 1);
			}
		}
	}

	public void drawBis(Graphics2D g2d, int x, int y) {
		// Pour affichage dans le panel du modules.simulateur.proglab.modules.simulateur.RobotChooser
		AffineTransform affineTransform = new AffineTransform();
		x += robot.CENTER_X + X_ROBOT - DX;
		y += robot.CENTER_Y + Y_ROBOT - DY;
		affineTransform.translate(x, y);
		affineTransform.rotate(THETA_ROBOT, DX, DY);
		g2d.drawImage(image, affineTransform, null);

		// Calcul du contour de l'objet
		transformedPath = (Path2D.Double) path.clone();
		transformedPath.transform(affineTransform);
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface XMLObject
	///////////////////////////////////////////////////

	public XMLObject add(String element, Attributes attributs) {
		return null;
	}

	public void set(String attribut, String valeur) {
		if (attribut.equals("x")) X_ROBOT = Integer.valueOf(valeur);
		if (attribut.equals("y")) Y_ROBOT = Integer.valueOf(valeur);
		if (attribut.equals("theta")) THETA_ROBOT = Math.toRadians(Integer.valueOf(valeur));
		if (attribut.equals("port")) setPort(valeur);
	}

	public void end() {
		robot.addSensor(port, this);
	}

	private void setPort(String port) {
		if (port.equals("IN_1")) this.port = 0;
		if (port.equals("IN_2")) this.port = 1;
		if (port.equals("IN_3")) this.port = 2;
		if (port.equals("IN_4")) this.port = 3;
	}
}

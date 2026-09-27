package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;
import codelab.utils.GraphUtils;

/**
*	Classe du capteur de contact
*	@author Jérôme Lehuen
*	@version 07/12/20
*/

public class SensorTouch extends AbstractSensor {

	private boolean contact;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorTouch(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_touch", "TOUCH");
		DX = 62;
		DY = 12;
	}

	public boolean computeContact() {
		if (robot == null || simulator == null) return contact;
		if (simulator.rectangleFond == null && simulator.imageFond != null) {
			simulator.rectangleFond = new Rectangle(simulator.imageFond.getWidth(), simulator.imageFond.getHeight());
		}
		if (simulator.rectangleFond == null) return contact;

		// Calcul de la position du capteur
		Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT));
		// Détection d'un obstacle
		contact = simulator.findObstacleAt(point);
		return contact;
	}

	public int getValue() {
		computeContact();
		return (contact ? 1 : 0);
	}

	public String getStringValue() {
		return String.format(LABEL("SensorTouch_1"), getValue());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
		computeContact();
		// Affichages
		if (simulator.DEBUG) {
			Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT));
			GraphUtils.drawCircle(g2d, point.getX(), point.getY(), 3);
			if (contact) GraphUtils.drawBigPoint(g2d, point.getX(), point.getY());
		}
	}
}

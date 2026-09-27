package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;

import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;

/**
*	Classe du capteur Boussole
*	@author Jérôme Lehuen
*	@version 12/06/21
*/

public class SensorCompas extends AbstractSensor {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorCompas(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_compass", "COMPAS");
		DX = 30;
		DY = 30;
	}

	public int getValue() {
		return 360 - (robot.getAngle() + 270) % 360;
	}

	public String getStringValue() {
		int angle = getValue();
		if (angle == 90) return LABEL("SensorCompas_1");
		if (angle == 180) return LABEL("SensorCompas_2");
		if (angle == 270) return LABEL("SensorCompas_3");
		if (angle == 360) return LABEL("SensorCompas_4");
		return String.format(LABEL("SensorCompas_5"), angle);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}
}

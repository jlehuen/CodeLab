package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;

import codelab.controllers.widgets.Numpad;
import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;

/**
*	Classe du capteur Numpad
*	@author Jérôme Lehuen
*	@version 05/01/24
*/

public class SensorNumpad extends AbstractSensor {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorNumpad(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_numpad", "NUMPAD");
		DX = 24;
		DY = 31;
	}

	public int getValue() {
		return Numpad.INSTANCE.getValue();
	}

	public String getStringValue() {
		return String.format(LABEL("SensorNumpad_1"), getValue());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}
}

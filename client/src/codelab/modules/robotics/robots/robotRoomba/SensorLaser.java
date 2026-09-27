package codelab.modules.robotics.robots.robotRoomba;

import codelab.modules.robotics.*;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.robotLego.SensorSonic;

import java.awt.*;

/**
*	Classe du capteur de distance rotatif
*	@author Jérôme Lehuen
*	@version $Date$
*/

public class SensorLaser extends SensorSonic {
	
	private int theta = 0;
	private int[] data = new int[360];
	private boolean scan = false;
	
	/**
	*	Constructeur de la classe SensorLaser
	*/
	
	public SensorLaser(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator);
		descr = "LASER";
		// Image du capteur
		setImage("sensor_empty");
		// Centre du capteur
		DX = 0;
		DY = 0;
		// Position du capteur
		X_ROBOT = 0;
		Y_ROBOT = 0;
	}
	
	public void draw(Graphics2D g2d) {
		THETA_ROBOT = Math.toRadians(theta);
		if (scan) {
			data[theta] = getValue();
			theta += 2;
			if (theta >= 360) reset();
		}
		super.draw(g2d);
	}
	
	public void startScan() {
		scan = true;
	}
	
	public void reset() {
		theta = 0;
		scan = false;
	}
	
	public int scanning() {
		return (scan ? 1 : 0);
	}
	
	public int getValue(int angle) {
		return data[angle];
	}
	
	public String getStringValue() {
		int angle = (int) Math.toDegrees(THETA_ROBOT);
		if (getValue() == 100) return String.format(" distance à %d° = 100 cm (MAXI)", angle);
		else return String.format(" distance à %d° = %d cm", angle, getValue());
	}
}

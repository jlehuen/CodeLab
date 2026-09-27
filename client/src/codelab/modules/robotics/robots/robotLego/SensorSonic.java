package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;
import codelab.utils.GraphUtils;

/**
*	Classe du capteur de distance
*	@author Jérôme Lehuen
*	@version 07/12/20
*/

public class SensorSonic extends AbstractSensor {

	protected int sonic_x;
	protected int sonic_y;
	protected int MAXI = 870; // Soit 1 mètre
	protected int distance; // En pixels

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorSonic(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_sonic", "SONIC");
		DX = 40;
		DY = 27;
	}

	public int computeDistance() {
		if (robot == null || simulator == null) return distance;
		if (simulator.rectangleFond == null && simulator.imageFond != null) {
			simulator.rectangleFond = new Rectangle(simulator.imageFond.getWidth(), simulator.imageFond.getHeight());
		}
		if (simulator.rectangleFond == null) return distance;

		Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT));
		sonic_x = (int) point.getX();
		sonic_y = (int) point.getY();

		int d;
		for (d = 0 ; d < MAXI ; d += 3) {
			int x = (int) (sonic_x + d * Math.cos(robot.theta + THETA_ROBOT));
			int y = (int) (sonic_y + d * Math.sin(robot.theta + THETA_ROBOT));

			if (simulator.findObstacleAt(new Point(x, y))) {
				break;
			}
		}
		distance = d;
		return distance;
	}

	public int getValue() {
		computeDistance();
		return (int) (distance / 8.7);
	}

	public String getStringValue() {
		if (getValue() == 100) return LABEL("SensorSonic_1");
		else return String.format(LABEL("SensorSonic_2"), getValue());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);

		computeDistance();

		if (simulator.DEBUG) {
			for (int d = 0 ; d < distance ; d += 3) {
				int x = (int) (sonic_x + d * Math.cos(robot.theta + THETA_ROBOT));
				int y = (int) (sonic_y + d * Math.sin(robot.theta + THETA_ROBOT));
				GraphUtils.drawPoint(g2d, x, y);
			}
			int obsX = (int) (sonic_x + distance * Math.cos(robot.theta + THETA_ROBOT));
			int obsY = (int) (sonic_y + distance * Math.sin(robot.theta + THETA_ROBOT));
			GraphUtils.drawBigPoint(g2d, obsX, obsY);
			GraphUtils.drawCircle(g2d, sonic_x, sonic_y, 10);
			//g2d.drawString(Integer.toString(distance), sonic_x - 9, sonic_y + 4);
		}
	}
}

package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;
import codelab.utils.ColorUtils;
import codelab.utils.GraphUtils;

/**
*	Classe du capteur de ligne
*	@author Jérôme Lehuen
*	@version 14/06/21
*/

public class SensorArray extends AbstractSensor {

	private int[] data = new int[8];

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorArray(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_array", "ARRAY");
		DX = 15;
		DY = 43;
	}

	public void computeArray() {
		if (robot == null || simulator == null) return;
		if (simulator.rectangleFond == null && simulator.imageFond != null) {
			simulator.rectangleFond = new Rectangle(simulator.imageFond.getWidth(), simulator.imageFond.getHeight());
		}
		if (simulator.rectangleFond == null) return;

		// Calcul des niveaux de gris des 8 capteurs
		for (int i = 0 ; i < 8 ; i++) {
			int somme = 0;
			for (int j = -4 ; j < 5 ; j += 2) {
				Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT + i * 10 + 8 - DY + j));
				if (simulator.rectangleFond.contains(point)) {
					int rgb = simulator.imageFond.getRGB(point.x, point.y);
					somme += 100 * ColorUtils.rgb2gray(rgb) / 255;
				} else {
					somme = 0; // Capteur en dehors du fond
					break;
				}
			}
			data[i] = somme / 5;
		}
	}

	public int getValue() {
		computeArray();
		return data[0];
	}

	public int getValue(int i) {
		computeArray();
		if (i >= 0 && i < data.length) return data[i];
		return 0;
	}

	public String getStringValue() {
		computeArray();
		String chaine = LABEL("SensorArray_1");
		for (int i = 0 ; i < 7 ; i++)
			chaine += String.format(" %d,", data[i]);
		return chaine + String.format(" %d", data[7]);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);

		computeArray();

		if (simulator.DEBUG) {
			for (int i = 0 ; i < 8 ; i++) {
				Point centre = robot.convert(new Point(X_ROBOT, Y_ROBOT + i * 10 + 8 - DY));
				GraphUtils.drawCircle(g2d, centre.x, centre.y, 5);
				for (int j = -4 ; j < 5 ; j += 2) {
					Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT + i * 10 + 8 - DY + j));
					GraphUtils.drawPoint(g2d, point.x, point.y);
				}
			}
		}
	}
}

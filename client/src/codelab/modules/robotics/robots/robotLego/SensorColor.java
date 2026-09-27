package codelab.modules.robotics.robots.robotLego;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import codelab.modules.robotics.Simulator;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.AbstractSensor;
import codelab.utils.ColorCode;
import codelab.utils.ColorUtils;
import codelab.utils.GraphUtils;

/**
*	Classe du capteur de couleur
*	@author Jérôme Lehuen
*	@version 18/03/22
*/

public class SensorColor extends AbstractSensor {

	private int color_x;
	private int color_y;
	private int rgb;
	private ColorCode couleur = ColorCode.COLOR_ERROR;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SensorColor(AbstractRobot robot, Simulator simulator) {
		super(robot, simulator, "sensor_color", "COLOR");
		DX = 21;
		DY = 13;
	}

	public void computeColor() {
		if (robot == null || simulator == null) return;
		if (simulator.rectangleFond == null && simulator.imageFond != null) {
			simulator.rectangleFond = new Rectangle(simulator.imageFond.getWidth(), simulator.imageFond.getHeight());
		}

		// Calcul de la position du capteur
		Point point = robot.convert(new Point(X_ROBOT, Y_ROBOT));
		color_x = (int) point.getX();
		color_y = (int) point.getY();

		// Détection d'une balise colorée
		ColorCode code = simulator.findColoredTagAt(point);
		if (code != null) {
			rgb = code.getRGB();
			couleur = code;
		}

		// Détection d'une couleur de fond
		else if (simulator.rectangleFond != null && simulator.imageFond != null && simulator.rectangleFond.contains(new Point(color_x, color_y))) {
			rgb = simulator.imageFond.getRGB(color_x, color_y);
			couleur = ColorUtils.rgb2color(rgb);
		}

		// Capteur en dehors du fond
		else {
			couleur = ColorCode.COLOR_ERROR;
		}
	}

	public int getValue() {
		computeColor();
		return couleur.ordinal();
	}

	public String getStringValue() {
		computeColor();
		if (couleur == ColorCode.COLOR_ERROR)
			return LABEL("SensorColor_1");
		else
			return String.format(LABEL("SensorColor_2"),
			couleur.toString(),
			(rgb >> 16) & 0xFF,
			(rgb >>  8) & 0xFF,
			(rgb      ) & 0xFF);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);

		computeColor();

		if (simulator.DEBUG) {
			GraphUtils.drawCircle(g2d, color_x, color_y, 5);
		}
	}
}

package codelab.modules.robotics;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
*	Classe des obstacles ronds
*	@author Jérôme Lehuen
*	@version 06/10/20
*/

public class ObstacleRond extends AbstractDeplacable {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ObstacleRond(Simulator simulator, int x, int y, String ident) {
		super(simulator);
		this.x = x;
		this.y = y;
		setImage(ident);
		CENTER_X = largeur / 2;
		CENTER_Y = hauteur / 2;

		// Redéfinir le path qui doit être également circulaire
		Ellipse2D cercle = new Ellipse2D.Double(0, 0, largeur, hauteur);
		path = new Path2D.Double();
		path.append(cercle, false);
		simulator.addObject(this);
	}

	public final boolean isObstacle() { return true; }

	public String getInfo() {
		return "an obstacle";
	}

	protected void draw(Graphics2D g2d) {
		super.draw(g2d);
	}
}

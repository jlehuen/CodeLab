package codelab.modules.robotics;

import java.awt.Graphics2D;

/**
*	Classe des obstacles carrés
*	@author Jérôme Lehuen
*	@version 06/10/20
*/

public class ObstacleCarre extends AbstractDeplacable {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ObstacleCarre(Simulator simulator, int x, int y, String ident) {
		super(simulator);
		this.x = x;
		this.y = y;
		setImage(ident);
		CENTER_X = largeur / 2;
		CENTER_Y = hauteur / 2;
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

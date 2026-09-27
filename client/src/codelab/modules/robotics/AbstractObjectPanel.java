package codelab.modules.robotics;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
*	Classe du panel à objets déplacables
*	@author Jérôme Lehuen
*	@version 29/01/24
*/

public abstract class AbstractObjectPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	protected ModuleRobotics module;
	protected ObjectListener listener;
	protected double scale = 1;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractObjectPanel(ModuleRobotics module) {
		this.module = module;
		// Ajout du listener de souris
		listener = new ObjectListener(this);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
	}

	public ModuleRobotics getModule() {
		return module;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	protected Graphics2D getGraphics2D(Graphics graphics) {
		Graphics2D g2d = (Graphics2D) graphics;
		g2d.scale(scale, scale);
		g2d.setFont(new Font("Arial", Font.PLAIN, 10));
		// Pour avoir un beau lissage des tracés en 2D
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g2d;
	}

	// Ces méthodes doivent être surchargées dans les sous-classes

	public abstract AbstractDeplacable findObject(Point point);
	public abstract AbstractDeplacable findObjectHandle(Point point);
	public abstract void moveHorizontalScrollBar(int dx);
	public abstract void moveVerticalScrollBar(int dy);
}

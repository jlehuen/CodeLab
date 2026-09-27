package codelab.modules.graphics;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*	Classe du gestionnaire de souris
*	@author Jérôme Lehuen
*	@version 30/11/23
*/

public class TurtleListener implements MouseListener, MouseMotionListener, MouseWheelListener {

	private Turtle turtle;
	private TurtleCanvas canvas;
	private Point origin;
	private boolean pressed = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public TurtleListener(TurtleCanvas canvas, Turtle turtle) {
		this.canvas = canvas;
		this.turtle = turtle;
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener
	///////////////////////////////////////////////////

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			origin = e.getPoint();
			if (turtle.contains(origin)) {
				pressed = true;
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		pressed = false;
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseMotionListener
	///////////////////////////////////////////////////

	public void mouseMoved(MouseEvent e) {}

	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (pressed) {
				Point p1 = e.getPoint();
				Point p2 = TurtleUtils.limit(p1, canvas.getSize());
				int dx = p2.x - origin.x;
				int dy = p2.y - origin.y;
				turtle.move(dx, dy);
				origin = p2;
			}
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseWheelListener
	///////////////////////////////////////////////////

	public void mouseWheelMoved(MouseWheelEvent e) {
		int KP = 1; // Par défaut
		if (pressed) {
			if (CodeLab.IS_LINUX) KP = 5;
			if (CodeLab.IS_WINDOWS) KP = 5;
			turtle.rotate(e.getWheelRotation() * KP);
		}
	}
}

package codelab.modules.robotics;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.utils.GraphUtils;

/**
*	Classe du listener de souris
*	@author Jérôme Lehuen
*	@version 03/10/23
*/

public class ObjectListener implements MouseListener, MouseMotionListener, MouseWheelListener {

	private AbstractObjectPanel panel;
	private AbstractDeplacable objet;
	private Point origin;
	private boolean repositionnement = false;
	private boolean rotation = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	ObjectListener(AbstractObjectPanel panel) {
		this.panel = panel;
	}

	Point getScaledPosition(MouseEvent event) {
		int x_scaled = (int)(event.getX() / panel.getScale());
		int y_scaled = (int)(event.getY() / panel.getScale());
		return new Point(x_scaled, y_scaled);
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener ///////////////////////
	////////////////////////////////////////////////////////////////

	public boolean mousePressed = false;

	// Pour la gestion des poignées de rotation
	private AbstractDeplacable objet2;

	public void mouseClicked(MouseEvent event) {
		if (SwingUtilities.isLeftMouseButton(event)) {
			if (objet2 != null) objet2.hideHandles(); // Effacer anciennes poignées
			Point point = getScaledPosition(event);
			objet2 = panel.findObject(point);
			if (objet2 != null) objet2.showHandles(); // Afficher nouvelles poignées
		}
		if (SwingUtilities.isRightMouseButton(event)) {
			if (objet2 != null) objet2.hideHandles();
		}
	}

	public void mouseEntered(MouseEvent event) {}
	public void mouseExited(MouseEvent event) {}

	public void mousePressed(MouseEvent event) {
		mousePressed = true;
		if (SwingUtilities.isLeftMouseButton(event)) {
			origin = getScaledPosition(event);
			objet = panel.findObject(origin);

			if (objet != null) {
				// On a cliqué sur un objet
				if (objet2 != null) objet2.hideHandles();
				objet.prendre();
				repositionnement = true;

			} else {

				objet = panel.findObjectHandle(origin);
				if (objet != null) {
					// On a cliqué sur une poignée
					rotation = true;

				} else {

					// On a cliqué dans le vide
					if (objet2 != null) objet2.hideHandles();
				}
			}
		}
		/*
		==> Déplacé dans init() de Simulator.java
		else if (SwingUtilities.isRightMouseButton(event)) {
			new BackgroundPopupMenu(panel, event.getX(), event.getY(), panel.getScale());
		}
		*/
	}

	public void mouseReleased(MouseEvent event) {
		if (objet != null) {
			if (repositionnement) objet.deposer();
			else if (rotation) objet.hideHandles();
		}
		mousePressed = false;
		repositionnement = false;
		rotation = false;
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseMotionListener /////////////////
	////////////////////////////////////////////////////////////////

	public void mouseMoved(MouseEvent event) {}

	public void mouseDragged(MouseEvent event) {

		if (SwingUtilities.isLeftMouseButton(event)) {
			Point p = GraphUtils.limit(event.getPoint(), panel.getSize());
			int x_scaled = (int)(p.x / panel.getScale());
			int y_scaled = (int)(p.y / panel.getScale());
			Point point_scaled = new Point(x_scaled, y_scaled);

			int dx = x_scaled - origin.x;
			int dy = y_scaled - origin.y;

			if (repositionnement && objet != null) {
				// On déplace l'objet
				objet.deplacer(dx, dy);
				objet.hideHandles();
				origin = point_scaled;

			} else if (rotation && objet != null) {
				// On pivote l'objet
				objet.pivoter(point_scaled);

			} else {
				// On déplace le fond
				panel.moveHorizontalScrollBar(-dx);
				panel.moveVerticalScrollBar(-dy);
			}
		}
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseWheelListener //////////////////
	////////////////////////////////////////////////////////////////

	public void mouseWheelMoved(MouseWheelEvent event) {
		int KP = 1; // Par défaut
		if (repositionnement && objet != null) {
			if (CodeLab.IS_LINUX) KP = 5;
			if (CodeLab.IS_WINDOWS) KP = 5;
			objet.pivoter(event.getWheelRotation() * KP);
		} else {
			// Déléguer la gestion de l'évènement au simulateur
			panel.getModule().getSimulator().mouseWheelMoved(event);
		}
	}
}

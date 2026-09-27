package codelab.modules.editeur.scratch;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import codelab.modules.editeur.scratch.widgets.AbstractWidget;

/**
*	Classe du listener du GlassPaneListener
*	@author Jérôme Lehuen
*	@version 30/01/24
*/

public class GlassPaneListener implements MouseListener, MouseMotionListener {

	private static final int SEUIL = 20; // Distance mini pour détacher un widget

	private GlassPane panel;
	private AbstractWidget widget; // Le widget manipulé
	private Point previous; // La position précédente du curseur
	private boolean repositionnement; // Déplacement d'un widget ?
	private boolean free; // Le widget est-il libre ?
	private boolean disable;

	private Point getScaledPoint(MouseEvent event) {
		int x_scaled = (int)(event.getX() / panel.getScale());
		int y_scaled = (int)(event.getY() / panel.getScale());
		return new Point(x_scaled, y_scaled);
	}

	public void setEnable(boolean value) {
		disable = !value;
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	GlassPaneListener(GlassPane panel) {
		this.panel = panel;
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener ///////////////////////
	////////////////////////////////////////////////////////////////

	public void mouseEntered(MouseEvent event) {}

	public void mouseExited(MouseEvent event) {
		if (disable) return;
		panel.reset();
	}

	public void mouseClicked(MouseEvent event) {
		if (disable) return;
		// Double-clic pour éditer le widget
		if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2) {
			previous = getScaledPoint(event);
			widget = panel.findObject(previous);
			if (widget != null) widget.showEditDialog();
		}
	}

	public void mousePressed(MouseEvent event) {

		previous = getScaledPoint(event);
		widget = panel.findObject(previous);

		if (SwingUtilities.isRightMouseButton(event)) {
			if (widget == null)
				new GlassPanePopupMenu(panel, event.getX(), event.getY(), panel.getScale(), disable);
			else
				if (!disable) new WidgetPopupMenu(panel, widget, event.getX(), event.getY(), panel.getScale());
		}
		else if (SwingUtilities.isLeftMouseButton(event)) {
			if (!disable && widget != null) {
				repositionnement = true;
				widget.resetError(); // En cas d'erreur sur le widget
				widget.prendre();
				panel.repaint();
				free = false;
			}
		}
	}

	public void mouseReleased(MouseEvent event) {
		if (disable) return;
		if (repositionnement) {
			if (free) widget.deposer();
			panel.mouseReleased(widget);
			panel.widgetPaneChanged();
			panel.reset();
			repositionnement = false;
		}
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseMotionListener /////////////////
	////////////////////////////////////////////////////////////////

	public void mouseMoved(MouseEvent event) {
	}

	public void mouseDragged(MouseEvent event) {
		if (disable) return;
		Point point = getScaledPoint(event);

		if (SwingUtilities.isLeftMouseButton(event)) {
			int dx = point.x - previous.x;
			int dy = point.y - previous.y;

			if (repositionnement) {
				if (free) {
					widget.deplacer(dx, dy, true); // Va également déplacer toutes les dépendances
					previous = point;
					panel.mouseDragged(widget); // Pour trouver les widgets survolés
				} else {
					// Calcul du seuil de libération du widget
					double distance = Point2D.distance(previous.x, previous.y, point.x, point.y);
					int seuil = widget.isRoot() ? 0 : SEUIL;
					if (distance > seuil) free = true;
				}
			} else {
				panel.moveHorizontalScrollBar(-dx);
				panel.moveVerticalScrollBar(-dy);
			}
		}
		panel.repaint();
	}
}

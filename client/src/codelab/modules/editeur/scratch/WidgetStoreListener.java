package codelab.modules.editeur.scratch;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

import codelab.modules.editeur.scratch.widgets.AbstractWidget;

/**
*	Classe du listener de souris du WidgetStore
*	@author Jérôme Lehuen
*	@version 30/01/24
*/

public class WidgetStoreListener implements MouseListener, MouseMotionListener {

	private WidgetStore store;
	private GlassPane panel;
	private AbstractWidget widget; // Le widget du store
	private AbstractWidget widget1; // Le widget cloné dans le store
	private AbstractWidget widget2; // Le widget cloné dans le glasspane
	private Point origin;

	private boolean repositionnement;
	private boolean glissement;
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

	WidgetStoreListener(WidgetStore store, GlassPane panel) {
		this.store = store;
		this.panel = panel;
	}

	////////////////////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener ///////////////////////
	////////////////////////////////////////////////////////////////

	public void mouseClicked(MouseEvent event) {}
	public void mouseEntered(MouseEvent event) {}

	public void mouseExited(MouseEvent event) {
		if (disable) return;
		if (repositionnement) return; // Pour éviter un changement de couleur pendant un transfert store-glasspane
		store.reset();
	}

	public void mousePressed(MouseEvent event) {
		if (disable) return;
		if (SwingUtilities.isLeftMouseButton(event)) {

			origin = getScaledPoint(event);
			widget = store.findObject(origin);
			WidgetStoreButton button = store.findButton(origin);

			if (button != null) {
				// Clic sur une catégorie
				store.buildStore(button.getCode());
			}
			else if (widget != null) {
				// Clic sur un widget
				repositionnement = true;
				// Cloner le widget dans les 2 panels
				store.add(widget1 = widget.cloner_store(store));
				panel.add(widget2 = widget.cloner_store(panel));
				// Calcul de la position du widget dans le glasspane
				widget2.x = widget.x - (int)((store.getWidth() - panel.getHorizontalScrollBarOffset()) / panel.getScale());
				widget2.y = widget.y + (int)((panel.getVerticalScrollBarOffset() - store.editor.getViewPositionStore().y) / panel.getScale());
				// Couleur magenta pour les 2 widgets clonés
				widget.mouseover_off();
				widget1.mouseover_on();
				widget2.mouseover_on();
			}
			else {
				// Clic sur le fond
				glissement = true;
			}
		}
	}

	public void mouseReleased(MouseEvent event) {
		if (disable) return;
		if (repositionnement) {
			int frontier = (int)(store.getWidth() / store.getScale());
			if (widget1.x < frontier)
				panel.remove(widget2); // Widget non déposé dans le glasspane => suppression
			else
				panel.mouseReleased(widget2); // Widget déposé dans le glasspane => conservation

			store.remove(widget1); // Supprimer le clone du store
			store.repaint();
			panel.repaint();
			repositionnement = false;
		}
		else if (glissement) {
			glissement = false;
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
			int dx = point.x - origin.x;
			int dy = point.y - origin.y;

			if (repositionnement) {
				widget1.deplacer(dx, dy, false);
				widget2.deplacer(dx, dy, false);
				origin = point;
				panel.mouseDragged(widget2); // Pour trouver les widgets survolés
			}
			else if (glissement) {
				store.editor.moveVerticalScrollBarStore(dy);
			}
		}
		store.repaint();
		panel.repaint();
	}
}

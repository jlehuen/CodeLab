package codelab.modules.editeur.scratch;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;

import codelab.PropertyBase;
import codelab.modules.editeur.scratch.widgets.*;

/**
*	Classe abstraite du panel de widgets
*	@author Jérôme Lehuen
*	@version 30/01/24
*/

public abstract class AbstractWidgetPane extends JPanel {

	private static final long serialVersionUID = 1L;
	private final Font panelFont = new Font("Verdana", Font.PLAIN, 10);

	protected ScratchEditor editor;
	public ScratchEditor getEditor() { return editor; }

	// Pour éviter une java.util.ConcurrentModificationException dans le paintComponent de GlassPane
	public List<AbstractWidget> widgets = Collections.synchronizedList(new ArrayList<AbstractWidget>());

	public boolean pythonFlag = false; // Affichage du code Python dans les widgets
	public int indent = PropertyBase.getIntegerProperty("BLOCS_OFFSET"); // Dans sysconfig.properties

	// Pour obtenir une copie inversée d'une liste
	protected ArrayList<AbstractWidget> reverse(List<AbstractWidget> array) {
		ArrayList<AbstractWidget> copy = new ArrayList<AbstractWidget>(array);
		Collections.reverse(copy);
		return copy;
	}

	///////////////////////////////////////////////////
	// Échelle (zoom) du panel
	///////////////////////////////////////////////////

	protected double scale = ToolBarScratch.ZOOM_INIT; // Échelle de l'éditeur
	public double getScale() { return scale; }
	public void setScale(double scale) { this.scale = scale; }

	///////////////////////////////////////////////////

	public abstract boolean isStore(); // A implémenter dans les sous-classes

	public void widgetPaneChanged() {
		// Modification du contenu du panel
		// En provenance de AbstractWidgetPane (remove)
		// Egalement en provenance du GlassPaneListener (mouseReleased)
		editor.widgetPaneChanged();
	}

	public void clear() {
		widgets = new ArrayList<AbstractWidget>();
		repaint();
	}

	public void reset() {
		for (AbstractWidget widget : widgets) widget.mouseover_off();
		repaint();
	}

	public void add(AbstractWidget widget) {
		widgets.add(widget);
		widgetPaneChanged();
		//repaint();
	}

	public void remove(AbstractWidget widget) {
		widgets.remove(widget);
		widgetPaneChanged();
		//repaint();
	}

	public void moveToFront(AbstractWidget widget) {
		remove(widget);
		add(widget);
	}

	public AbstractWidget getLastWidget() {
		if (widgets.size() == 0) return null;
		return widgets.get(widgets.size() - 1);
	}

	public AbstractWidget findObject(Point point) {
		ArrayList<AbstractWidget> copy = reverse(widgets); // Pour parcourir à partir de la fin
		for (AbstractWidget widget : copy)
			if (widget.contains(point)) return widget;
		return null;
	}

	public AbstractWidget findWidget(String hashCode) {
		for (AbstractWidget widget : widgets)
			if (Integer.toHexString(widget.hashCode()).equals(hashCode))
				return widget;
		return null;
	}

	public Graphics2D getGraphics2D(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.scale(scale, scale);
		g2d.setFont(panelFont);
		// Pour avoir un beau lissage des tracés en 2D
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g2d;
	}

	public void mouseMoved(Point point) {
		// Parcourir à partir de la fin (superpositions)
		ArrayList<AbstractWidget> copy = reverse(widgets);
		for (AbstractWidget widget : copy) widget.mouseover_off();
		for (AbstractWidget widget : copy) {
			if (widget.contains(point)) {
				widget.mouseover_on();
				break;
			}
		}
		repaint();
	}
}

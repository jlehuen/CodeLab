package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget SETCOLOR
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_SETCOLOR extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	private TypeColor color = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_SETCOLOR(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_GRAPH;
	}

	public Widget_SETCOLOR(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_SETCOLOR cloner_store(AbstractWidgetPane panel) {
		return new Widget_SETCOLOR(panel, x, y);
	}

	public Widget_SETCOLOR cloner(AbstractWidgetPane panel) {
		Widget_SETCOLOR clone = new Widget_SETCOLOR(panel);
		clone.color = this.color;
		return clone;
	}

	public Widget_SETCOLOR clonerRec(AbstractWidgetPane panel) {
		Widget_SETCOLOR clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("SETCOLOR_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("setColor(%s)", color == null ? null : color.name());
		else
			return String.format(LABEL("SETCOLOR_toString"),
				color == null ? "..." : color.toString());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%ssetColor(%s)%s\n%s",
			Utils.tabulation(indent),
			color == null ? "NULL" : color.name(),
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<setcolor color='%s' x='%d' y='%d'/>\n", color == null ? null : color.ordinal(), x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "color":	color = TypeColor.values()[Integer.parseInt(valeur)]; break;
			default:		throw new Exception();
		}
	}

	///////////////////////////////////////////////////
	// Fenêtre d'édition
	///////////////////////////////////////////////////

	public void showEditDialog() {
		super.showEditDialog(); // Trucs avant
		new EditDialog(this);
	}

	private class EditDialog extends AbstractEditDialog {

		private static final long serialVersionUID = 1L;
		private Widget_SETCOLOR widget;

		private final ListePanel c1 = new ListePanel(
			LABEL("COLOR_label"),
			TypeColor.values());

		public EditDialog(Widget_SETCOLOR widget) {
			super(widget);
			this.widget = widget;
			if (widget.color != null) c1.setItem(widget.color);
			addComponent(c1);
			pack();
		}

		protected void valider() {
			widget.color = (TypeColor) c1.getSelectedItem();
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget SETWIDTH
*	@author Jérôme Lehuen
*	@version 11/05/23
*/

public class Widget_SETWIDTH extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	private Expression width = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_SETWIDTH(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_GRAPH;
	}

	public Widget_SETWIDTH(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_SETWIDTH cloner_store(AbstractWidgetPane panel) {
		return new Widget_SETWIDTH(panel, x, y);
	}

	public Widget_SETWIDTH cloner(AbstractWidgetPane panel) {
		Widget_SETWIDTH clone = new Widget_SETWIDTH(panel);
		clone.width = width == null ? null : width.cloner();
		return clone;
	}

	public Widget_SETWIDTH clonerRec(AbstractWidgetPane panel) {
		Widget_SETWIDTH clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("SETWIDTH_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("setWidth(%s)", width);
		else
			return String.format(LABEL("SETWIDTH_toString"),
                width == null ? "..." : width);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%ssetWidth(%s)%s\n%s",
			Utils.tabulation(indent),
			width == null ? "NULL" : width,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<setwidth width='%s' x='%d' y='%d'/>\n", width, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "width":	width = new Expression(valeur); break;
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
		private Widget_SETWIDTH widget;

        private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_SETWIDTH widget) {
			super(widget);
			this.widget = widget;
			if (widget.width != null) c1.setText(widget.width.toString());
			addComponent(c1);
			pack();
		}

		protected void valider() {
			String value1 = c1.getText().trim();
			widget.width = value1.isEmpty() ? null : new Expression(value1);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

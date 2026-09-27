package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget TURTLEGOTO
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_TURTLEGOTO extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	private Expression pos_x = null;
	private Expression pos_y = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_TURTLEGOTO(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_TURTL;
	}

	public Widget_TURTLEGOTO(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_TURTLEGOTO cloner_store(AbstractWidgetPane panel) {
		return new Widget_TURTLEGOTO(panel, x, y);
	}

	public Widget_TURTLEGOTO cloner(AbstractWidgetPane panel) {
		Widget_TURTLEGOTO clone = new Widget_TURTLEGOTO(panel);
		clone.pos_x = pos_x == null ? null : pos_x.cloner();
		clone.pos_y = pos_y == null ? null : pos_y.cloner();
		return clone;
	}

	public Widget_TURTLEGOTO clonerRec(AbstractWidgetPane panel) {
		Widget_TURTLEGOTO clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("TURTLEGOTO_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("turtleGoto(%s, %s)", pos_x, pos_y);
		else
			return String.format(LABEL("TURTLEGOTO_toString"),
				pos_x == null ? "..." : pos_x,
				pos_y == null ? "..." : pos_y);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sturtleGoto(%s, %s)%s\n%s",
			Utils.tabulation(indent),
			pos_x == null ? "NULL" : pos_x,
			pos_y == null ? "NULL" : pos_y,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<turtle_goto posx='%s' posy='%s' x='%d' y='%d'/>\n", pos_x, pos_y, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "posx":	pos_x = new Expression(valeur); break;
			case "posy":	pos_y = new Expression(valeur); break;
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
		private Widget_TURTLEGOTO widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);

		public EditDialog(Widget_TURTLEGOTO widget) {
			super(widget);
			this.widget = widget;
			if (widget.pos_x != null) c1.setText(widget.pos_x.toString());
			if (widget.pos_y != null) c2.setText(widget.pos_y.toString());
			addComponent(new JLabel(LABEL("TURTLE_label_2")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("TURTLE_label_3")));
			addComponent(c2);
			pack();
		}

		protected void valider() {
			String value1 = c1.getText().trim();
			String value2 = c2.getText().trim();
			widget.pos_x = value1.isEmpty() ? null : new Expression(value1);
			widget.pos_y = value2.isEmpty() ? null : new Expression(value2);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

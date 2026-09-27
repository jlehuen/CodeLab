package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget DRAWLINE
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_DRAWLINE extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private Expression x1 = null;
	private Expression y1 = null;
	private Expression x2 = null;
	private Expression y2 = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_DRAWLINE(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_GRAPH;
	}

	public Widget_DRAWLINE(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_DRAWLINE cloner_store(AbstractWidgetPane panel) {
		return new Widget_DRAWLINE(panel, x, y);
	}

	public Widget_DRAWLINE cloner(AbstractWidgetPane panel) {
		Widget_DRAWLINE clone = new Widget_DRAWLINE(panel);
		clone.x1 = x1 == null ? null : x1.cloner();
		clone.y1 = y1 == null ? null : y1.cloner();
		clone.x2 = x2 == null ? null : x2.cloner();
		clone.y2 = y2 == null ? null : y2.cloner();
		return clone;
	}

	public Widget_DRAWLINE clonerRec(AbstractWidgetPane panel) {
		Widget_DRAWLINE clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("DRAWLINE_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("drawLine(%s, %s, %s, %s)", x1, y1, x2, y2);
		else
			return String.format(LABEL("DRAWLINE_toString"),
				x1 == null ? "..." : x1,
				y1 == null ? "..." : y1,
				x2 == null ? "..." : x2,
				y2 == null ? "..." : y2);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sdrawLine(%s, %s, %s, %s)%s\n%s",
			Utils.tabulation(indent),
			x1 == null ? "NULL" : x1,
			y1 == null ? "NULL" : y1,
			x2 == null ? "NULL" : x2,
			y2 == null ? "NULL" : y2,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<drawline x1='%s' y1='%s' x2='%s' y2='%s' x='%d' y='%d'/>\n", x1, y1, x2, y2, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "x1": x1 = new Expression(valeur); break;
			case "y1": y1 = new Expression(valeur); break;
			case "x2": x2 = new Expression(valeur); break;
			case "y2": y2 = new Expression(valeur); break;
			default: throw new Exception();
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
		private Widget_DRAWLINE widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c3 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c4 = new TextFieldKeyListener(this);

		public EditDialog(Widget_DRAWLINE widget) {
			super(widget);
			this.widget = widget;
			if (widget.x1 != null) c1.setText(widget.x1.toString());
			if (widget.y1 != null) c2.setText(widget.y1.toString());
			if (widget.x2 != null) c3.setText(widget.x2.toString());
			if (widget.y2 != null) c4.setText(widget.y2.toString());
			addComponent(new JLabel(LABEL("DRAWLINE_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("DRAWLINE_label_2")));
			addComponent(c2);
			addComponent(new JLabel(LABEL("DRAWLINE_label_3")));
			addComponent(c3);
			addComponent(new JLabel(LABEL("DRAWLINE_label_4")));
			addComponent(c4);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			final String value2 = c2.getText().trim();
			final String value3 = c3.getText().trim();
			final String value4 = c4.getText().trim();
			widget.x1 = value1.isEmpty() ? null : new Expression(value1);
			widget.y1 = value2.isEmpty() ? null : new Expression(value2);
			widget.x2 = value3.isEmpty() ? null : new Expression(value3);
			widget.y2 = value4.isEmpty() ? null : new Expression(value4);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

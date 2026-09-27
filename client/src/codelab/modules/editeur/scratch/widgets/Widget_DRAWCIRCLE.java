package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget DRAWCIRCLE
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_DRAWCIRCLE extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private Expression x0 = null;
	private Expression y0 = null;
	private Expression rayon = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_DRAWCIRCLE(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_GRAPH;
	}

	public Widget_DRAWCIRCLE(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_DRAWCIRCLE cloner_store(AbstractWidgetPane panel) {
		return new Widget_DRAWCIRCLE(panel, x, y);
	}

	public Widget_DRAWCIRCLE cloner(AbstractWidgetPane panel) {
		Widget_DRAWCIRCLE clone = new Widget_DRAWCIRCLE(panel);
		clone.x0 = x0 == null ? null : x0.cloner();
		clone.y0 = y0 == null ? null : y0.cloner();
		clone.rayon = rayon == null ? null : rayon.cloner();
		return clone;
	}

	public Widget_DRAWCIRCLE clonerRec(AbstractWidgetPane panel) {
		Widget_DRAWCIRCLE clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("DRAWCIRCLE_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("drawCircle(%s, %s, %s)", x0, y0, rayon);
		else
			return String.format(LABEL("DRAWCIRCLE_toString"),
				x0 == null ? "..." : x0,
				y0 == null ? "..." : y0,
				rayon == null ? "..." : rayon);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sdrawCircle(%s, %s, %s)%s\n%s",
			Utils.tabulation(indent),
			x0 == null ? "NULL" : x0,
			y0 == null ? "NULL" : y0,
			rayon == null ? "NULL" : rayon,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<drawcircle x0='%s' y0='%s' rayon='%s' x='%d' y='%d'/>\n", x0, y0, rayon, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "x0": x0 = new Expression(valeur); break;
			case "y0": y0 = new Expression(valeur); break;
			case "rayon": rayon = new Expression(valeur); break;
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
		private Widget_DRAWCIRCLE widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c3 = new TextFieldKeyListener(this);

		public EditDialog(Widget_DRAWCIRCLE widget) {
			super(widget);
			this.widget = widget;
			if (widget.x0 != null) c1.setText(widget.x0.toString());
			if (widget.y0 != null) c2.setText(widget.y0.toString());
			if (widget.rayon != null) c3.setText(widget.rayon.toString());
			addComponent(new JLabel(LABEL("DRAWCIRCLE_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("DRAWCIRCLE_label_2")));
			addComponent(c2);
			addComponent(new JLabel(LABEL("DRAWCIRCLE_label_3")));
			addComponent(c3);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			final String value2 = c2.getText().trim();
			final String value3 = c3.getText().trim();
			widget.x0 = value1.isEmpty() ? null : new Expression(value1);
			widget.y0 = value2.isEmpty() ? null : new Expression(value2);
			widget.rayon = value3.isEmpty() ? null : new Expression(value3);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

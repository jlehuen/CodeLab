package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget IFELSE
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_IFELSE extends AbstractBlocDouble {

	private static final long serialVersionUID = 1L;

	private Expression condition = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_IFELSE(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_STRUC;
	}

	public Widget_IFELSE(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_IFELSE cloner_store(AbstractWidgetPane panel) {
		return new Widget_IFELSE(panel, x, y);
	}

	public Widget_IFELSE cloner(AbstractWidgetPane panel) {
		Widget_IFELSE clone = new Widget_IFELSE(panel);
		clone.condition = condition == null ? null : condition.cloner();
		return clone;
	}

	public Widget_IFELSE clonerRec(AbstractWidgetPane panel) {
		Widget_IFELSE clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("IF_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("if %s:", condition);
		else
			return String.format(LABEL("IF_toString"),
				condition == null ? "..." : condition);
	}

	public String toString_2(boolean pythonFlag) {
		if (pythonFlag)
			return "else:";
		else
			return LABEL("ELSE_toString");
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sif %s:%s\n%s\n%selse:\n%s\n%s",
			Utils.tabulation(indent),
			condition == null ? "NULL" : condition,
			getWidgetIdent(commentFlag),
			enfant1 == null ? pass(indent + 1) : enfant1.traduire_Python(indent + 1, commentFlag),
			Utils.tabulation(indent),
			enfant2 == null ? pass(indent + 1) : enfant2.traduire_Python(indent + 1, commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<ifelse cond='%s' x='%d' y='%d'>\n",
			condition == null ? condition : condition.toXML(), x, y));
		if (enfant1 != null) enfant1.saveXML(out);
		out.write("<else/>\n");
		if (enfant2 != null) enfant2.saveXML(out);
		out.write("</ifelse>\n");
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "cond": condition = new Expression(valeur); break;
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
		private Widget_IFELSE widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_IFELSE widget) {
			super(widget);
			this.widget = widget;
			if (widget.condition != null) c1.setText(widget.condition.toString());
			addComponent(new JLabel(LABEL("CONDITION_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			final String value = c1.getText().trim();
			widget.condition = value.isEmpty() ? null : new Expression(value);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget IF
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_IF extends AbstractBlocSimple {

	private static final long serialVersionUID = 1L;

	private Expression condition = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_IF(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_STRUC;
	}

	public Widget_IF(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_IF cloner_store(AbstractWidgetPane panel) {
		return new Widget_IF(panel, x, y);
	}

	public Widget_IF cloner(AbstractWidgetPane panel) {
		Widget_IF clone = new Widget_IF(panel);
		clone.condition = condition == null ? null : condition.cloner();
		return clone;
	}

	public Widget_IF clonerRec(AbstractWidgetPane panel) {
		Widget_IF clone = cloner(panel);
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

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sif %s:%s\n%s\n%s",
			Utils.tabulation(indent),
			condition == null ? "NULL" : condition,
			getWidgetIdent(commentFlag),
			enfant == null ? pass(indent + 1) : enfant.traduire_Python(indent + 1, commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<if cond='%s' x='%d' y='%d'>\n",
			condition == null ? condition : condition.toXML(), x, y));
		if (enfant != null) enfant.saveXML(out);
		out.write("</if>\n");
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
		private Widget_IF widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_IF widget) {
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

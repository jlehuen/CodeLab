package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget LET
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_LET extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private String var = null;
	private Expression expression = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_LET(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_OTHER;
	}

	public Widget_LET(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_LET cloner_store(AbstractWidgetPane panel) {
		return new Widget_LET(panel, x, y);
	}

	public Widget_LET cloner(AbstractWidgetPane panel) {
		Widget_LET clone = new Widget_LET(panel);
		clone.var = this.var;
		clone.expression = expression == null ? null : expression.cloner();
		return clone;
	}

	public Widget_LET clonerRec(AbstractWidgetPane panel) {
		Widget_LET clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("LET_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("%s = %s", var, expression);
		else
			return String.format(LABEL("LET_toString"),
				expression == null ? "..." : expression,
				var == null ? "..." : var);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s%s = %s%s\n%s",
			Utils.tabulation(indent), var == null ? "NULL" : var,
			expression == null ? "NULL" : expression,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<let var='%s' expr='%s' x='%d' y='%d'/>\n",
			var, expression == null ? expression : expression.toXML(), x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "var": var = valeur; break;
			case "expr": expression = new Expression(valeur); break;
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
		private Widget_LET widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);

		public EditDialog(Widget_LET widget) {
			super(widget);
			this.widget = widget;
			if (widget.var != null) c1.setText(widget.var);
			if (widget.expression != null) c2.setText(widget.expression.toString());
			addComponent(new JLabel(LABEL("EXPRESSION_label")));
			addComponent(c2);
			addComponent(new JLabel(LABEL("VARIABLE_NAME_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			final String value2 = c2.getText().trim();
			widget.var = value1.isEmpty() ? null : value1;
			widget.expression = value2.isEmpty() ? null : new Expression(value2);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

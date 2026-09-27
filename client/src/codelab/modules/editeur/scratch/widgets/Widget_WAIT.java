package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget WAIT
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_WAIT extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	public Expression expression = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_WAIT(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_OTHER;
	}

	public Widget_WAIT(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_WAIT cloner_store(AbstractWidgetPane panel) {
		return new Widget_WAIT(panel, x, y);
	}

	public Widget_WAIT cloner(AbstractWidgetPane panel) {
		Widget_WAIT clone = new Widget_WAIT(panel);
		clone.expression = expression == null ? null : expression.cloner();
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_WAIT clonerRec(AbstractWidgetPane panel) {
		Widget_WAIT clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("WAIT_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("waitFor(%s)", expression);
		else
			return String.format(LABEL("WAIT_toString"),
				expression == null ? "..." : expression);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%swaitFor(%s)%s\n%s",
			Utils.tabulation(indent),
			expression == null ? "NULL" : expression,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<wait expr='%s' x='%d' y='%d'/>\n", expression, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "expr":	expression = new Expression(valeur); break;
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
		private Widget_WAIT widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_WAIT widget) {
			super(widget);
			this.widget = widget;
			if (widget.expression != null) c1.setText(widget.expression.toString());
			addComponent(new JLabel(LABEL("WAIT_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			String value = c1.getText().trim();
			widget.expression = value.isEmpty() ? null : new Expression(value);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

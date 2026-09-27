package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget GETJOYSTICKY
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_GETJOYSTICKY extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	public String var = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_GETJOYSTICKY(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_GETJOYSTICKY(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_GETJOYSTICKY cloner_store(AbstractWidgetPane panel) {
		return new Widget_GETJOYSTICKY(panel, x, y);
	}

	public Widget_GETJOYSTICKY cloner(AbstractWidgetPane panel) {
		Widget_GETJOYSTICKY clone = new Widget_GETJOYSTICKY(panel);
		clone.var = this.var;
		return clone;
	}

	public Widget_GETJOYSTICKY clonerRec(AbstractWidgetPane panel) {
		Widget_GETJOYSTICKY clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("GETJOYSTICKY_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("%s = getJoystickValueY()", var);
		else
			return String.format(LABEL("GETJOYSTICKY_toString"),
				var == null ? "..." : var);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s%s = getJoystickValueY()%s\n%s",
			Utils.tabulation(indent),
			var == null ? "NULL" : var,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<getjoystick_y var='%s' x='%d' y='%d'/>\n", var, x, y));
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
		private Widget_GETJOYSTICKY widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_GETJOYSTICKY widget) {
			super(widget);
			this.widget = widget;
			if (widget.var != null) c1.setText(widget.var);
			addComponent(new JLabel(LABEL("VARIABLE_NAME_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			widget.var = value1.isEmpty() ? null : value1;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

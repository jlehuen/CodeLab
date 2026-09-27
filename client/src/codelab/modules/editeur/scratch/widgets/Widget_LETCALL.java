package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget LETCALL
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_LETCALL extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private String var = null;
	private String name = null;
	private String args = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_LETCALL(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_PROCE;
	}

	public Widget_LETCALL(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_LETCALL cloner_store(AbstractWidgetPane panel) {
		return new Widget_LETCALL(panel, x, y);
	}

	public Widget_LETCALL cloner(AbstractWidgetPane panel) {
		Widget_LETCALL clone = new Widget_LETCALL(panel);
		clone.var = this.var;
		clone.name = this.name;
		clone.args = this.args;
		return clone;
	}

	public Widget_LETCALL clonerRec(AbstractWidgetPane panel) {
		Widget_LETCALL clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("LETCALL_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("%s = %s(%s)", var, name, args == null ? "" : args);
		else
			return String.format(LABEL("LETCALL_toString"),
				name == null ? "..." : name,
				args == null ? "..." : args,
				var == null ? "..." : var);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s%s = %s(%s)%s\n%s",
			Utils.tabulation(indent),
			var == null ? "NULL" : var,
			name == null ? "NULL" : name,
			args == null ? "" : args,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<letcall var='%s' name='%s' args='%s' x='%d' y='%d'/>\n", var, name, args, x, y));
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
			case "name": name = valeur; break;
			case "args": args = valeur; break;
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
		private Widget_LETCALL widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c3 = new TextFieldKeyListener(this);

		public EditDialog(Widget_LETCALL widget) {
			super(widget);
			this.widget = widget;
			if (widget.name != null) c1.setText(widget.name);
			if (widget.args != null) c2.setText(widget.args);
			if (widget.var != null) c3.setText(widget.var);
			addComponent(new JLabel(LABEL("CALL_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("CALL_label_2")));
			addComponent(c2);
			addComponent(new JLabel(LABEL("VARIABLE_NAME_label")));
			addComponent(c3);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			final String value2 = c2.getText().trim();
			final String value3 = c3.getText().trim();
			widget.name = value1.isEmpty() ? null : value1;
			widget.args = value2.isEmpty() ? null : value2;
			widget.var = value3.isEmpty() ? null : value3;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget CALL
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_CALL extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private String name = null;
	private String args = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_CALL(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_PROCE;
	}

	public Widget_CALL(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_CALL cloner_store(AbstractWidgetPane panel) {
		return new Widget_CALL(panel, x, y);
	}

	public Widget_CALL cloner(AbstractWidgetPane panel) {
		Widget_CALL clone = new Widget_CALL(panel);
		clone.name = this.name;
		clone.args = this.args;
		return clone;
	}

	public Widget_CALL clonerRec(AbstractWidgetPane panel) {
		Widget_CALL clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("CALL_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("%s(%s)", name, args == null ? "" : args);
		else
			return String.format(LABEL("CALL_toString"),
				name == null ? "..." : name,
				args == null ? "..." : args);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s%s(%s)%s\n%s",
			Utils.tabulation(indent),
			name == null ? "NULL" : name,
			args == null ? "" : args,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<call name='%s' args='%s' x='%d' y='%d'/>\n", name, args, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
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
		private Widget_CALL widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);

		public EditDialog(Widget_CALL widget) {
			super(widget);
			this.widget = widget;
			if (widget.name != null) c1.setText(widget.name);
			if (widget.args != null) c2.setText(widget.args);
			addComponent(new JLabel(LABEL("CALL_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("CALL_label_2")));
			addComponent(c2);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			final String value2 = c2.getText().trim();
			widget.name = value1.isEmpty() ? null : value1;
			widget.args = value2.isEmpty() ? null : value2;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

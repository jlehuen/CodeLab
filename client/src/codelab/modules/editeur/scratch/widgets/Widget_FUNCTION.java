package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;

/**
*	Classe du widget FUNCTION
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_FUNCTION extends AbstractBlocSimple {

	private static final long serialVersionUID = 1L;

	private String name = null;
	private String args = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_FUNCTION(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_PROCE;
		// Pour supprimer les encoches supérieure et inférieure
		tabY_init[3] = 0;
		tabY_init[4] = 0;
		tabY_init[27] = 55;
		tabY_init[28] = 55;
		updatePolygon(0, 0);
	}

	public Widget_FUNCTION(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_FUNCTION cloner_store(AbstractWidgetPane panel) {
		return new Widget_FUNCTION(panel, x, y);
	}

	public Widget_FUNCTION cloner(AbstractWidgetPane panel) {
		Widget_FUNCTION clone = new Widget_FUNCTION(panel);
		clone.name = this.name;
		clone.args = this.args;
		return clone;
	}

	public Widget_FUNCTION clonerRec(AbstractWidgetPane panel) {
		Widget_FUNCTION clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("FUNCTION_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("def %s(%s):", name, args == null ? "" : args);
		else
			return String.format(LABEL("FUNCTION_toString"),
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
		return String.format("%n%ndef %s(%s):%s\n%s\n%s",
			name == null ? "NULL" : name,
			args == null ? "" : args,
			getWidgetIdent(commentFlag),
			enfant == null ? pass(indent + 1) : enfant.traduire_Python(indent + 1, commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<function name='%s' args='%s' x='%d' y='%d'>\n", name, args, x, y));
		if (enfant != null) enfant.saveXML(out);
		out.write("</function>\n");
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
		private Widget_FUNCTION widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);

		public EditDialog(Widget_FUNCTION widget) {
			super(widget);
			this.widget = widget;
			if (widget.name != null) c1.setText(widget.name);
			if (widget.args != null) c2.setText(widget.args);
			addComponent(new JLabel(LABEL("FUNCTION_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("FUNCTION_label_2")));
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

package codelab.modules.editeur.scratch.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.utils.Utils;

/**
*	Classe du widget COMMENT
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_COMMENT extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	public String texte = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_COMMENT(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = new Color(220, 220, 220); // Gris clair
	}

	public Widget_COMMENT(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_COMMENT cloner_store(AbstractWidgetPane panel) {
		return new Widget_COMMENT(panel, x, y);
	}

	public Widget_COMMENT cloner(AbstractWidgetPane panel) {
		Widget_COMMENT clone = new Widget_COMMENT(panel);
		clone.texte = this.texte;
		return clone;
	}

	public Widget_COMMENT clonerRec(AbstractWidgetPane panel) {
		Widget_COMMENT clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("COMMENT_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("# %s", texte);
		else
			return String.format(LABEL("COMMENT_toString"),
				texte == null ? "..." : texte);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s# %s\n%s",
			Utils.tabulation(indent),
			texte == null ? "NULL" : texte,
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<comment txt='%s' x='%d' y='%d'/>\n", texte, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "txt":		texte = valeur; break;
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
		private Widget_COMMENT widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_COMMENT widget) {
			super(widget);
			this.widget = widget;
			if (widget.texte != null) c1.setText(widget.texte.toString());
			addComponent(new JLabel(LABEL("COMMENT_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			String value = c1.getText().trim();
			widget.texte = value.isEmpty() ? null : value;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

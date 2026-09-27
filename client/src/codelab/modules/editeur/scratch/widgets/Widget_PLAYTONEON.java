package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget PLAYTONEON
*	@author Jérôme Lehuen
*	@version 11/05/21
*/

public class Widget_PLAYTONEON extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	private Expression freq = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONEON(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_PLAYTONEON(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONEON cloner_store(AbstractWidgetPane panel) {
		return new Widget_PLAYTONEON(panel, x, y);
	}

	public Widget_PLAYTONEON cloner(AbstractWidgetPane panel) {
		Widget_PLAYTONEON clone = new Widget_PLAYTONEON(panel);
		clone.freq = freq == null ? null : freq.cloner();
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_PLAYTONEON clonerRec(AbstractWidgetPane panel) {
		Widget_PLAYTONEON clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("PLAYTONEON_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("playToneOn(%s)", freq);
		else
			return String.format(LABEL("PLAYTONEON_toString"),
				freq == null ? "..." : freq);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%splayToneOn(%s)%s\n%s",
			Utils.tabulation(indent),
			freq == null ? "NULL" : freq,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<playtoneon freq='%s' x='%d' y='%d'/>\n", freq, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "freq":	freq = new Expression(valeur); break;
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
		private Widget_PLAYTONEON widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_PLAYTONEON widget) {
			super(widget);
			this.widget = widget;
			if (widget.freq != null) c1.setText(widget.freq.toString());
			addComponent(new JLabel(LABEL("PLAYTONE_label_1")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			String value1 = c1.getText().trim();
			widget.freq = value1.isEmpty() ? null : new Expression(value1);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

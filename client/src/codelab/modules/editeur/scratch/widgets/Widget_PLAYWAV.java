package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget PLAYWAV
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_PLAYWAV extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	public String filename = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_PLAYWAV(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_PLAYWAV(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_PLAYWAV cloner_store(AbstractWidgetPane panel) {
		return new Widget_PLAYWAV(panel, x, y);
	}

	public Widget_PLAYWAV cloner(AbstractWidgetPane panel) {
		Widget_PLAYWAV clone = new Widget_PLAYWAV(panel);
		clone.filename = this.filename;
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_PLAYWAV clonerRec(AbstractWidgetPane panel) {
		Widget_PLAYWAV clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("PLAYWAV_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("playAudioFile(\"%s\")", filename);
		else
			return String.format(LABEL("PLAYWAV_toString"),
                filename == null ? "..." : filename);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%splayAudioFile(\"%s\")%s\n%s",
			Utils.tabulation(indent),
			filename == null ? "NULL" : filename,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<playwav file='%s' x='%d' y='%d'/>\n", filename, x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			case "file":	filename = valeur; break;
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
		private Widget_PLAYWAV widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);

		public EditDialog(Widget_PLAYWAV widget) {
			super(widget);
			this.widget = widget;
			if (widget.filename != null) c1.setText(widget.filename);
			addComponent(new JLabel(LABEL("PLAYWAV_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("PLAYWAV_label_2")));
			pack();
		}

		protected void valider() {
			String value = c1.getText().trim();
			widget.filename = value.isEmpty() ? null : value;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

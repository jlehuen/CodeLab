package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget PLAYTONE
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_PLAYTONE extends AbstractInstruction {

	private static final long serialVersionUID = 1L;
	private Expression freq = null;
	private Expression time = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONE(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_PLAYTONE(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONE cloner_store(AbstractWidgetPane panel) {
		return new Widget_PLAYTONE(panel, x, y);
	}

	public Widget_PLAYTONE cloner(AbstractWidgetPane panel) {
		Widget_PLAYTONE clone = new Widget_PLAYTONE(panel);
		clone.freq = freq == null ? null : freq.cloner();
		clone.time = time == null ? null : time.cloner();
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_PLAYTONE clonerRec(AbstractWidgetPane panel) {
		Widget_PLAYTONE clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("PLAYTONE_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("playTone(%s, %s)", freq, time);
		else
			return String.format(LABEL("PLAYTONE_toString"),
				freq == null ? "..." : freq,
				time == null ? "..." : time);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%splayTone(%s, %s)%s\n%s",
			Utils.tabulation(indent),
			freq == null ? "NULL" : freq,
			time == null ? "NULL" : time,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<playtone freq='%s' time='%s' x='%d' y='%d'/>\n", freq, time, x, y));
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
			case "time":	time = new Expression(valeur); break;
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
		private Widget_PLAYTONE widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);

		public EditDialog(Widget_PLAYTONE widget) {
			super(widget);
			this.widget = widget;
			if (widget.freq != null) c1.setText(widget.freq.toString());
			if (widget.time != null) c2.setText(widget.time.toString());
			addComponent(new JLabel(LABEL("PLAYTONE_label_1")));
			addComponent(c1);
			addComponent(new JLabel(LABEL("PLAYTONE_label_2")));
			addComponent(c2);
			pack();
		}

		protected void valider() {
			String value1 = c1.getText().trim();
			String value2 = c2.getText().trim();
			widget.freq = value1.isEmpty() ? null : new Expression(value1);
			widget.time = value2.isEmpty() ? null : new Expression(value2);
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

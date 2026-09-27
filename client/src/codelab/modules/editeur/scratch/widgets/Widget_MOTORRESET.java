package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget MOTORRESET
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_MOTORRESET extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private TypeMotorPort2 port = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_MOTORRESET(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_ROBOT;
	}

	public Widget_MOTORRESET(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_MOTORRESET cloner_store(AbstractWidgetPane panel) {
		return new Widget_MOTORRESET(panel, x, y);
	}

	public Widget_MOTORRESET cloner(AbstractWidgetPane panel) {
		Widget_MOTORRESET clone = new Widget_MOTORRESET(panel);
		clone.port = this.port;
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_MOTORRESET clonerRec(AbstractWidgetPane panel) {
		Widget_MOTORRESET clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("MOTORRESET_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("resetMotorRotationCount(%s)",
				port == null ? null : port.name());
		else
			return String.format(LABEL("MOTORRESET_toString"),
				port == null ? "..." : port.toString());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sresetMotorRotationCount(%s)%s\n%s",
			Utils.tabulation(indent),
			port == null ? "NULL" : port.name(),
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<motorreset port='%s' x='%d' y='%d'/>\n",
			port == null ? null : port.ordinal(), x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "port": port = TypeMotorPort2.values()[Integer.parseInt(valeur)]; break;
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
		private Widget_MOTORRESET widget;

		private final ListePanel c1 = new ListePanel(
			LABEL("MOTOR_PORT_label"),
			TypeMotorPort2.values());

		public EditDialog(Widget_MOTORRESET widget) {
			super(widget);
			this.widget = widget;
			if (widget.port != null) c1.setItem(widget.port);
			addComponent(c1);
			pack();
		}

		protected void valider() {
			widget.port = (TypeMotorPort2) c1.getSelectedItem();
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

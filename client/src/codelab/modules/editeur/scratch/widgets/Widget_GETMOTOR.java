package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget GETMOTOR
*	@author Jérôme Lehuen
*	@version 01/12/20
*/

public class Widget_GETMOTOR extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	public String var = null;
	private TypeMotorPort2 port = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_GETMOTOR(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_ROBOT;
	}

	public Widget_GETMOTOR(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_GETMOTOR cloner_store(AbstractWidgetPane panel) {
		return new Widget_GETMOTOR(panel, x, y);
	}

	public Widget_GETMOTOR cloner(AbstractWidgetPane panel) {
		Widget_GETMOTOR clone = new Widget_GETMOTOR(panel);
		clone.var = this.var;
		clone.port = this.port;
		return clone;
	}

	public Widget_GETMOTOR clonerRec(AbstractWidgetPane panel) {
		Widget_GETMOTOR clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("GETMOTOR_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("%s = motorRotationCount(%s)", var, port == null ? null : port.name());
		else
			return String.format(LABEL("GETMOTOR_toString"),
				port == null ? "..." : port.toString(),
				var == null ? "..." : var);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%s%s = motorRotationCount(%s)%s\n%s",
			Utils.tabulation(indent),
			var == null ? "NULL" : var,
			port == null ? "NULL" : port.name(),
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<getmotor var='%s' port='%d' x='%d' y='%d'/>\n", var, port == null ? null : port.ordinal(), x, y));
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
		private Widget_GETMOTOR widget;

		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);
		private final ListePanel c1 = new ListePanel(
			LABEL("MOTOR_PORT_label"),
			TypeMotorPort2.values());


		public EditDialog(Widget_GETMOTOR widget) {
			super(widget);
			this.widget = widget;
			if (widget.port != null) c1.setItem(widget.port);
			if (widget.var != null) c2.setText(widget.var);
			addComponent(c1);
			addComponent(new JLabel(LABEL("VARIABLE_NAME_label")));
			addComponent(c2);
			pack();
		}

		protected void valider() {
			final String value2 = c2.getText().trim();
			widget.port = (TypeMotorPort2) c1.getSelectedItem();
			widget.var = value2.isEmpty() ? null : value2;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

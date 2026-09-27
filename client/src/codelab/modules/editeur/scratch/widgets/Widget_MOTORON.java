package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget MOTORON
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_MOTORON extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	public TypeMotorPort port = null;
	public Expression puissance = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_MOTORON(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_ROBOT;
	}

	public Widget_MOTORON(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_MOTORON cloner_store(AbstractWidgetPane panel) {
		return new Widget_MOTORON(panel, x, y);
	}

	public Widget_MOTORON cloner(AbstractWidgetPane panel) {
		Widget_MOTORON clone = new Widget_MOTORON(panel);
		clone.port = this.port;
		clone.puissance = puissance == null ? null : puissance.cloner();
		clonerRec(clone, panel);
		return clone;
	}

	public Widget_MOTORON clonerRec(AbstractWidgetPane panel) {
		Widget_MOTORON clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("MOTORON_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return String.format("motorOn(%s, %s)",
				port == null ? null : port.name(), puissance);
		else
			return String.format(LABEL("MOTORON_toString"),
				puissance == null ? "..." : puissance,
				port == null ? "..." : port.toString());
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%smotorOn(%s, %s)%s\n%s",
			Utils.tabulation(indent),
			port == null ? "NULL" : port.name(),
			puissance == null ? "NULL" : puissance,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<motoron port='%s' puissance='%s' x='%d' y='%d'/>\n",
				port == null ? null : port.ordinal(), puissance, x, y));
		if (suivant != null)
			suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			case "port": port = TypeMotorPort.values()[Integer.parseInt(valeur)]; break;
			case "puissance": puissance = new Expression(valeur); break;
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
		private Widget_MOTORON widget;

		private final TextFieldKeyListener c2 = new TextFieldKeyListener(this);
		private final ListePanel c1 = new ListePanel(
			LABEL("MOTOR_PORT_label"),
			TypeMotorPort.values());

		public EditDialog(Widget_MOTORON widget) {
			super(widget);
			this.widget = widget;
			if (widget.port != null) c1.setItem(widget.port);
			if (widget.puissance != null) c2.setText(widget.puissance.toString());
			addComponent(c1);
			addComponent(new JLabel(LABEL("MOTORON_label")));
			addComponent(c2);
			pack();
		}

		protected void valider() {
			final String value = c2.getText().trim();
			widget.puissance = value.isEmpty() ? null : new Expression(value);
			widget.port = (TypeMotorPort) c1.getSelectedItem();
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

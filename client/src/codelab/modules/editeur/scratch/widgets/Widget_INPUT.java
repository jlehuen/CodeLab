package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JLabel;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget INPUT
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_INPUT extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	private String var = null;
	private TypeInput typeVar = null;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_INPUT(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_INPUT(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_INPUT cloner_store(AbstractWidgetPane panel) {
		return new Widget_INPUT(panel, x, y);
	}

	public Widget_INPUT cloner(AbstractWidgetPane panel) {
		Widget_INPUT clone = new Widget_INPUT(panel);
		clone.var = this.var;
		clone.typeVar = this.typeVar;
		return clone;
	}

	public Widget_INPUT clonerRec(AbstractWidgetPane panel) {
		Widget_INPUT clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("INPUT_toString");
	}

	public String toString_1(boolean pythonFlag) {
		String format = "%s = int(input(\"> \"))";
		if (typeVar != null && typeVar.equals(TypeInput.STRING))
			format = "%s = input(\"> \")";
		if (typeVar != null && typeVar.equals(TypeInput.INTEGER))
			format = "%s = int(input(\"> \"))";

		if (pythonFlag)
			return String.format(format, var);
		else
			return String.format(LABEL("INPUT_toString"),
				typeVar == null ? "..." : typeVar.toString(),
				var == null ? "..." : var);
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		String format = "%s%s = int(input(\"> \"))%s\n%s";
		if (typeVar != null && typeVar.equals(TypeInput.STRING)) format = "%s%s = input(\"> \")%s\n%s";
		if (typeVar != null && typeVar.equals(TypeInput.INTEGER)) format = "%s%s = int(input(\"> \"))%s\n%s";
		return String.format(format,
			Utils.tabulation(indent),
			var == null ? "NULL" : var,
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<input type='%s' var='%s' x='%d' y='%d'/>\n",
			typeVar == null ? null : typeVar.ordinal(), var, x, y));
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
			case "type": typeVar = TypeInput.values()[Integer.parseInt(valeur)]; break;
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
		private Widget_INPUT widget;

		private final TextFieldKeyListener c1 = new TextFieldKeyListener(this);
		private final ListePanel c2 = new ListePanel(
			LABEL("DATA_TYPE_label"),
			TypeInput.values());

		public EditDialog(Widget_INPUT widget) {
			super(widget);
			this.widget = widget;
			if (widget.var != null) c1.setText(widget.var);
			if (widget.typeVar != null) c2.setItem(widget.typeVar);
			addComponent(c2);
			addComponent(new JLabel(LABEL("VARIABLE_NAME_label")));
			addComponent(c1);
			pack();
		}

		protected void valider() {
			final String value1 = c1.getText().trim();
			widget.typeVar = (TypeInput) c2.getSelectedItem();
			widget.var = value1.isEmpty() ? null : value1;
			widget.repaint();
			super.valider(); // Trucs après
		}
	}
}

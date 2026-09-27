package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget PLAYTONEOFF
*	@author Jérôme Lehuen
*	@version 11/05/21
*/

public class Widget_PLAYTONEOFF extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONEOFF(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_INOUT;
	}

	public Widget_PLAYTONEOFF(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_PLAYTONEOFF cloner_store(AbstractWidgetPane panel) {
		return new Widget_PLAYTONEOFF(panel, x, y);
	}

	public Widget_PLAYTONEOFF cloner(AbstractWidgetPane panel) {
		Widget_PLAYTONEOFF clone = new Widget_PLAYTONEOFF(panel);
		return clone;
	}

	public Widget_PLAYTONEOFF clonerRec(AbstractWidgetPane panel) {
		Widget_PLAYTONEOFF clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("PLAYTONEOFF_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return "playToneOff()";
		else
			return LABEL("PLAYTONEOFF_toString");
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%splayToneOff()%s\n%s",
			Utils.tabulation(indent),
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<playtoneoff x='%d' y='%d'/>\n", x, y));
		if (suivant != null) suivant.saveXML(out);
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x":		x = Integer.valueOf(valeur); break;
			case "y":		y = Integer.valueOf(valeur); break;
			default:		throw new Exception();
		}
	}
}

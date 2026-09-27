package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;
import codelab.utils.Utils;

/**
*	Classe du widget TURTLESHOW
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_TURTLESHOW extends AbstractInstruction {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_TURTLESHOW(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_TURTL;
	}

	public Widget_TURTLESHOW(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_TURTLESHOW cloner_store(AbstractWidgetPane panel) {
		return new Widget_TURTLESHOW(panel, x, y);
	}

	public Widget_TURTLESHOW cloner(AbstractWidgetPane panel) {
		Widget_TURTLESHOW clone = new Widget_TURTLESHOW(panel);
		return clone;
	}

	public Widget_TURTLESHOW clonerRec(AbstractWidgetPane panel) {
		Widget_TURTLESHOW clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("TURTLESHOW_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return "turtleShow()";
		else
			return LABEL("TURTLESHOW_toString");
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%sturtleShow()%s\n%s",
			Utils.tabulation(indent),
			getWidgetIdent(commentFlag),
			suivant == null ? "" : suivant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<turtle_show x='%d' y='%d'/>\n", x, y));
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

package codelab.modules.editeur.scratch.widgets;

import java.awt.Graphics2D;
import java.io.FileWriter;
import java.io.IOException;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.WidgetStore;

/**
*	Classe du widget MAIN
*	@author Jérôme Lehuen
*	@version 04/12/20
*/

public class Widget_MAIN extends AbstractBlocSimple {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Widget_MAIN(AbstractWidgetPane panel, int x, int y) {
		super(panel, x, y);
		couleur = WidgetStore.COLOR_PROCE;
		// Pour supprimer les encoches supérieure et inférieure
		tabY_init[3] = 0;
		tabY_init[4] = 0;
		tabY_init[27] = 55;
		tabY_init[28] = 55;
		updatePolygon(0, 0);
	}

	public Widget_MAIN(AbstractWidgetPane panel) {
		this(panel, 0, 0);
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Widget_MAIN cloner_store(AbstractWidgetPane panel) {
		return new Widget_MAIN(panel, x, y);
	}

	public Widget_MAIN cloner(AbstractWidgetPane panel) {
		Widget_MAIN clone = new Widget_MAIN(panel);
		return clone;
	}

	public Widget_MAIN clonerRec(AbstractWidgetPane panel) {
		Widget_MAIN clone = cloner(panel);
		clonerRec(clone, panel);
		return clone;
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toString() {
		return LABEL("MAIN_toString");
	}

	public String toString_1(boolean pythonFlag) {
		if (pythonFlag)
			return "# Main script";
		else
			return LABEL("MAIN_toString");
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
	}

	///////////////////////////////////////////////////
	// Traductions du widget
	///////////////////////////////////////////////////

	public String traduire_Python(int indent, boolean commentFlag) {
		return String.format("%n%n%s", enfant == null ? "" : enfant.traduire_Python(indent, commentFlag));
	}

	///////////////////////////////////////////////////
	// Sauvegarde XML
	///////////////////////////////////////////////////

	public void saveXML(FileWriter out) throws IOException {
		out.write(String.format("<main x='%d' y='%d'>\n", x, y));
		if (enfant != null)
			enfant.saveXML(out);
		out.write("</main>\n");
	}

	///////////////////////////////////////////////////
	// Chargement XML
	///////////////////////////////////////////////////

	public void set(String attribut, String valeur) throws Exception {
		switch (attribut) {
			case "x": x = Integer.valueOf(valeur); break;
			case "y": y = Integer.valueOf(valeur); break;
			default: throw new Exception();
		}
	}
}

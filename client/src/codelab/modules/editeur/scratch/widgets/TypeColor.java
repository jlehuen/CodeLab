package codelab.modules.editeur.scratch.widgets;

import codelab.CodeLab;

public enum TypeColor {

	COLOR_WHITE, COLOR_BLACK, COLOR_RED, COLOR_GREEN, COLOR_BLUE, COLOR_CYAN, COLOR_YELLOW, COLOR_MAGENTA;

	public String toString() {
		switch (this) {
			case COLOR_WHITE:	return CodeLab.LABEL("WHITE");
			case COLOR_BLACK:	return CodeLab.LABEL("BLACK");
			case COLOR_RED:		return CodeLab.LABEL("RED");
			case COLOR_GREEN:	return CodeLab.LABEL("GREEN");
			case COLOR_BLUE:	return CodeLab.LABEL("BLUE");
			case COLOR_CYAN:	return CodeLab.LABEL("CYAN");
			case COLOR_YELLOW:	return CodeLab.LABEL("YELLOW");
			case COLOR_MAGENTA:	return CodeLab.LABEL("MAGENTA");

			default: return "ERROR";
		}
	}
}

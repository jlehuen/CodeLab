package codelab.modules.editeur.scratch.widgets;

import codelab.CodeLab;

public enum TypeMotorPort {

	OUT_B, OUT_C, OUT_BC;

	public String toString() {
		switch (this) {
			case OUT_B:		return CodeLab.LABEL("MotorPortLeft");
			case OUT_C:		return CodeLab.LABEL("MotorPortRight");
			case OUT_BC:	return CodeLab.LABEL("MotorPortBoth");

			default: return "ERROR";
		}
	}
}

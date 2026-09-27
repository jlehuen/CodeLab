package codelab.modules.editeur.scratch.widgets;

import codelab.CodeLab;

public enum TypeMotorPort2 {

	OUT_B, OUT_C;

	public String toString() {
		switch (this) {
			case OUT_B:		return CodeLab.LABEL("MotorPortLeft");
			case OUT_C:		return CodeLab.LABEL("MotorPortRight");

			default: return "ERROR";
		}
	}
}

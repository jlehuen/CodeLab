package codelab.modules.editeur.scratch.widgets;

import codelab.CodeLab;

public enum TypeSensorPort {

	IN_1, IN_2, IN_3, IN_4;

	public String toString() {
		switch (this) {
			case IN_1:	return CodeLab.LABEL("SensorPort1");
			case IN_2:	return CodeLab.LABEL("SensorPort2");
			case IN_3:	return CodeLab.LABEL("SensorPort3");
			case IN_4:	return CodeLab.LABEL("SensorPort4");

			default: return "ERROR";
		}
	}
}

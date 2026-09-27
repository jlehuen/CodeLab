package codelab.modules.editeur.scratch.widgets;

import codelab.CodeLab;

public enum TypeInput {

	STRING, INTEGER;

	public String toString() {
		switch (this) {
			case STRING:		return CodeLab.LABEL("STRING");
			case INTEGER:		return CodeLab.LABEL("INTEGER");

			default: return "ERROR";
		}
	}
}

package codelab.modules.editeur.scratch.widgets;

import java.io.Serializable;

/**
*	Classe des expressions évaluables
*	@author Jérôme Lehuen
*	@version 30/08/20
*/

public class Expression implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String expr;

	public String toString() {
		return expr;
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Expression(String expr) {
		this.expr = expr;
	}

	///////////////////////////////////////////////////
	// Cloneurs
	///////////////////////////////////////////////////

	public Expression cloner() {
		return new Expression(expr);
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public String toXML() {
		return expr
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll("'", "&apos;");
	}
}

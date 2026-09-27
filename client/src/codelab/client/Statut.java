package codelab.client;

/**
*	Énumération des statuts d'utilisateur
*	@author Jérôme Lehuen
*	@version 11/05/25
*/

public enum Statut {

	STANDALONE,
	STUDENT,
	TUTOR,
	ADMIN,
	ERROR;

	public String toString() {
		switch (this) {
			case STANDALONE:	return "STANDALONE";
			case STUDENT:		return "STUDENT";
			case TUTOR:			return "TUTOR";
			case ADMIN:			return "ADMIN";
			case ERROR:			return "ERROR";
			default:			return "ERROR";
		}
	}

	public boolean isStudent() {
		return this.equals(STUDENT);
	}

	public boolean isTutor() {
		return this.equals(TUTOR) || this.equals(ADMIN);
	}

	public boolean isAdmin() {
		return this.equals(ADMIN);
	}

	public static Statut str2statut(String str) {
		// Utilisé dans le constructeur de UserData
		switch (str) {
			case "STUDENT": return STUDENT;
			case "TUTOR": return TUTOR;
			case "ADMIN": return ADMIN;
			default: return ERROR;
		}
	}
}

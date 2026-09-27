package codelab.modules.editeur.manager;

import javax.swing.JOptionPane;

import codelab.CodeLab;

public class NewFolderDialog {

	private String name;

	public String getName() {
		return name;
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public NewFolderDialog() {
		name = (String) JOptionPane.showInputDialog(
			CodeLab.FRAME,
			CodeLab.LABEL("Enter_new_name_folder"),
			CodeLab.LABEL("New_folder_title"),
			JOptionPane.PLAIN_MESSAGE, null, null, "");

		if (name == null) return;
		if (name.length() == 0) name = null;
	}
}

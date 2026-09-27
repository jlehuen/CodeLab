package codelab.console;

import java.awt.Color;

import codelab.Executeur;

/**
*	Interface des consoles
*	@author Jérôme Lehuen
*	@version 04/10/24
*/

public interface ConsoleInterface {

	public void print(String str, Color color, boolean caret);
	public void print(char ch, Color color, boolean caret);
	public void directPrint(String str, Color color);
	public void flush();
	public void reset();
	public void clear();
	public void focus();

	public void setExecuteur(Executeur executeur);

	// Gestion du copié-collé

	public void copySelection();
	public void pasteClipboard();
	public boolean isTextSelected();
	public boolean hasTransferableText();
}

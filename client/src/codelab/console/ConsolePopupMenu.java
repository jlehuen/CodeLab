package codelab.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import codelab.CodeLab;
import codelab.PropertyBase;

/**
 *	Classe des menus popup de la console
 *	@author Jérôme Lehuen
 *	@version 14/12/22
 */

public class ConsolePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	private ConsoleInterface console;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ConsolePopupMenu(CodeLab codelab, JComponent component, int x, int y) {
		this.console = codelab.getConsole();

		JCheckBoxMenuItem itemAutoconsole = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_1"));
		itemAutoconsole.setSelected(codelab.getAutoconsole()); // Etat courant
		itemAutoconsole.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				action_autoconsole();
			}
		});

		JCheckBoxMenuItem itemAutoclear = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_2"));
		itemAutoclear.setSelected(codelab.getAutoclear()); // Etat courant
		itemAutoclear.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				action_autoclear();
			}
		});

		JCheckBoxMenuItem itemCopy = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_5"));
		itemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		itemCopy.setEnabled(console.isTextSelected());
		itemCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				console.copySelection();
			}
		});

		JCheckBoxMenuItem itemPaste = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_6"));
		itemPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		itemPaste.setEnabled(((CodeLab)codelab).isRunning() && console.hasTransferableText());
		itemPaste.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				console.pasteClipboard();
			}
		});

		JMenuItem itemClearConsole = new JMenuItem(CodeLab.LABEL("ConsolePopupMenu_3"));
		itemClearConsole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				console.clear();
			}
		});

		JMenuItem itemCloseConsole = new JMenuItem(CodeLab.LABEL("ConsolePopupMenu_4"));
		itemCloseConsole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				codelab.getCurrentModule().hideConsole();
			}
		});

		add(itemAutoconsole);
		add(itemAutoclear);
		add(new JSeparator());

		add(itemClearConsole);
		add(itemCloseConsole);
		add(new JSeparator());
		add(itemCopy);
		add(itemPaste);

		show(component, x, y);
	}

	///////////////////////////////////////////////////
	// Méthodes statiques partagées avec CodeLabMenu
	///////////////////////////////////////////////////

	public static void action_autoconsole() {
		CodeLab codelab = CodeLab.INSTANCE;
		codelab.setAutoconsole(!codelab.getAutoconsole());
		PropertyBase.setUserProperty("CONSOLE_AUTOOPEN", codelab.getAutoconsole() ? "yes" : "no");
	}

	public static void action_autoclear() {
		CodeLab codelab = CodeLab.INSTANCE;
		codelab.setAutoclear(!codelab.getAutoclear());
		PropertyBase.setUserProperty("CONSOLE_AUTOCLEAR", codelab.getAutoclear() ? "yes" : "no");
	}
}

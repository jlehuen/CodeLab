package codelab;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 *	Classe du menu popup du bouton RUN
 *	@author Jérôme Lehuen
 *	@version 18/09/25
 */

public class RunPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public RunPopupMenu(CodeLab codelab, JComponent component, int x, int y) {

		JCheckBoxMenuItem itemStopOnWarning = new JCheckBoxMenuItem(CodeLab.LABEL("RunPopupMenu_1"));
		itemStopOnWarning.setSelected(Compilateur.stopOnWarning); // Etat courant
		itemStopOnWarning.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
                Compilateur.stopOnWarning = !Compilateur.stopOnWarning;
				codelab.getEditor().resetCompilationFlag();
			}
		});

		/*
		String timeoutLabel = String.format(CodeLab.LABEL("RunPopupMenu_2"), CodeLab.PROGRAM_TIMEOUT);
		JCheckBoxMenuItem itemTimeoutFlag = new JCheckBoxMenuItem(timeoutLabel);
		itemTimeoutFlag.setSelected(ExecutionMonitor.timeoutFlag); // Etat courant
		itemTimeoutFlag.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
                ExecutionMonitor.timeoutFlag = !ExecutionMonitor.timeoutFlag;
			}
		});
		*/

		add(itemStopOnWarning);
		//add(itemTimeoutFlag);
		show(component, x, y);
	}
}

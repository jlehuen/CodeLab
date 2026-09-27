package codelab.controllers.devices;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import net.java.games.input.Controller;

/**
*	Classe du menu popup du EventViewer
*	@author Jérôme Lehuen
*	@version 05/01/24
*/

public class ControllerPopupMenu extends JPopupMenu {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ControllerPopupMenu(JComponent component, int x, int y) {

		EventReader reader = EventViewer.INSTANCE.getReader();

		for (Controller c : reader.getControllers()) {
			if (c.getName() == null) continue;
			String label = String.format("%s: %s", c.getType().toString(), c.getName());
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, true);
			boolean state = reader.controlEnabled.get(c);
			item.setSelected(state); // Etat courant
			item.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					reader.controlEnabled.put(c, item.isSelected());
				}
			});
			add(item);
		}
		show(component, x, y);
	}
}

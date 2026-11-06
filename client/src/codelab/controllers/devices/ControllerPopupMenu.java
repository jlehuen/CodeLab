package codelab.controllers.devices;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import codelab.AbstractCodeLab;
import codelab.CodeLab;
import codelab.utils.MacPermissions;
import net.java.games.input.Controller;

/**
*	Classe du menu popup du EventViewer
*	@author Jérôme Lehuen
*	@version 06/11/26
*/

public class ControllerPopupMenu extends JPopupMenu {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ControllerPopupMenu(JComponent component, int x, int y) {

		EventReader reader = EventViewer.INSTANCE.getReader();
		Controller[] controllers = reader.getControllers();

		if (controllers == null || controllers.length == 0) {
			JMenuItem noDeviceItem = new JMenuItem(CodeLab.LABEL("EventViewer_no_device"));
			noDeviceItem.setEnabled(false);
			add(noDeviceItem);
		} else {
			for (Controller c : controllers) {
				if (c.getName() == null) continue;
				String label = String.format("%s: %s", c.getType().toString(), c.getName());
				JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, true);
				Boolean state = reader.controlEnabled.get(c);
				item.setSelected(state != null && state); // Etat courant
				item.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						reader.controlEnabled.put(c, item.isSelected());
					}
				});
				add(item);
			}
		}

		add(new JSeparator());

		if (AbstractCodeLab.IS_OSX) {
			JMenuItem itemMacPerm = new JMenuItem(CodeLab.LABEL("EventViewer_mac_settings_menu"));
			itemMacPerm.addActionListener(e -> MacPermissions.openInputMonitoring());
			add(itemMacPerm);
		}

		JMenuItem itemRescan = new JMenuItem(CodeLab.LABEL("EventViewer_rescan"));
		itemRescan.addActionListener(e -> {
			int nb = reader.scan();
			EventViewer.INSTANCE.append("\n" + String.format(CodeLab.LABEL("EventViewer_rescan_result"), nb) + "\n");
			CodeLab.INSTANCE.toolbar_update();
		});
		add(itemRescan);

		show(component, x, y);
	}
}

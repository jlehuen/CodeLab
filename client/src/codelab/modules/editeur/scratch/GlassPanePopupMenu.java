package codelab.modules.editeur.scratch;

import java.awt.event.*;
import javax.swing.*;

import codelab.CodeLab;
import codelab.client.ChatController;
import codelab.client.UserData;
import codelab.modules.editeur.ModuleEditor;

/**
*	Classe des menus popup du glasspane
*	@author Jérôme Lehuen
*	@version 11/05/25
*/

public class GlassPanePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	private JMenuItem itemTchat = new JMenuItem();
	private JMenuItem itemControl = new JMenuItem();
	private JMenuItem itemResetHelp = new JMenuItem();

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public GlassPanePopupMenu(GlassPane panel, int x, int y, double scale, boolean disable) {

		int x_scaled = (int)(x/scale);
		int y_scaled = (int)(y/scale);

		JMenuItem menuItem1 = new JMenuItem("Coller");
		menuItem1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				panel.past(x_scaled, y_scaled);
			}
		});
		add(menuItem1);
		menuItem1.setEnabled(!disable && panel.getWidgetToPaste() != null);

		///////////////////////////////////////////////////
		// Items spécifiques aux tuteurs
		///////////////////////////////////////////////////

		if (CodeLab.INSTANCE.isTutor() || CodeLab.INSTANCE.isAdmin()) {

			ModuleEditor editor = panel.editor.getModule();
			UserData udata = editor.getCurrentUdata();
			boolean isconnected = udata.isConnected();
			boolean iscontrolled = udata.isControlled();
			boolean helpFlag = udata.getHelpFlag();
			String label = iscontrolled ? CodeLab.LABEL("UserTable_16b") : CodeLab.LABEL("UserTable_16a");
			itemControl.setText(String.format(label, udata));
			itemControl.setEnabled(true);
			itemTchat.setText(String.format(CodeLab.LABEL("UserTable_4"), udata));
			itemTchat.setEnabled(isconnected);
			itemResetHelp.setText(String.format(CodeLab.LABEL("itemResetCallFlag"), udata));
			itemResetHelp.setEnabled(isconnected && helpFlag);

			itemTchat.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.getCurrentUdata();
					if (udata == null) return;
					ChatController.openChatWith(udata);
					CodeLab.INSTANCE.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			/*
			itemControl.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.getCurrentUdata();
					if (udata == null) return;
					boolean value = udata.isControlled();
					CodeLab.INSTANCE.askControlMode(udata.getLogin(), !value);
				}
			});
			*/

			itemResetHelp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.getCurrentUdata();
					if (udata == null) return;
					CodeLab.INSTANCE.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			boolean isClone = editor.isCloneCurrentFile();
			JMenuItem itemCloneAction = new JMenuItem(isClone ? CodeLab.LABEL("Delete_current_clone") : CodeLab.LABEL("Clone_program"));
			itemCloneAction.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (isClone) editor.deleteCurrentClone();
					else editor.cloneCurrentFile();
				}
			});

			addSeparator();
			add(itemCloneAction);
			add(itemTchat);
			add(itemResetHelp);
			add(itemControl);
		}

		show(panel, x, y);
	}
}

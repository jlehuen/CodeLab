package codelab.client;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import codelab.CodeLab;

/**
*	Pour saisir un nouveau mot de passe
*	@author Jérôme Lehuen
*	@version 04/11/26
*/

public class NewpassDialog {

	private String[] optionNames;
	private JOptionPane pane;
	private String pass_1 = "";
	private String pass_2 = "";
	private JTextField focusField;

	public String getPassword() { 
		return cancelled() ? null : pass_1; 
	}

	public boolean cancelled() {
		if (pane == null) return true;
		Object val = pane.getValue();
		if (val == null) return true;
		if (val.equals(JOptionPane.UNINITIALIZED_VALUE)) return true;
		if (val instanceof Integer && ((Integer) val) == JOptionPane.CLOSED_OPTION) return true;
		if (val.equals(CodeLab.LABEL("CANCEL")) || val.equals("Cancel") || val.equals("Annuler")) return true;
		if (optionNames != null && optionNames.length > 0 && !val.equals(optionNames[0])) return true;
		return false;
	}

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public NewpassDialog() {
		this(CodeLab.INSTANCE != null && CodeLab.INSTANCE.getClient() != null 
			? CodeLab.INSTANCE.getClient().getPassword() 
			: null);
	}

	public NewpassDialog(String currentPassword) {

		JLabel message = new JLabel(CodeLab.LABEL("NewpassDialog_2"));
		message.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel passwordLabel_0 = new JLabel(CodeLab.LABEL("NewpassDialog_6") + " ", JLabel.RIGHT);
		JLabel passwordLabel_1 = new JLabel(CodeLab.LABEL("NewpassDialog_3") + " ", JLabel.RIGHT);
		JLabel passwordLabel_2 = new JLabel(CodeLab.LABEL("NewpassDialog_4") + " ", JLabel.RIGHT);

		JTextField passwordField_0 = new JPasswordField(15);
		JTextField passwordField_1 = new JPasswordField(15);
		JTextField passwordField_2 = new JPasswordField(15);

		passwordField_0.addActionListener(e -> passwordField_1.requestFocusInWindow());
		passwordField_1.addActionListener(e -> passwordField_2.requestFocusInWindow());
		passwordField_2.addActionListener(e -> {
			if (pane != null && optionNames != null && optionNames.length > 0) {
				pane.setValue(optionNames[0]);
			}
			Window w = SwingUtilities.getWindowAncestor(passwordField_2);
			if (w != null) w.setVisible(false);
		});

		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0, 1));
		namePanel.add(passwordLabel_0);
		namePanel.add(passwordLabel_1);
		namePanel.add(passwordLabel_2);

		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridBagLayout());
		addItem(fieldPanel, passwordField_0, 0, 0, 1, 1, GridBagConstraints.WEST);
		addItem(fieldPanel, passwordField_1, 0, 1, 1, 1, GridBagConstraints.WEST);
		addItem(fieldPanel, passwordField_2, 0, 2, 1, 1, GridBagConstraints.WEST);

		JPanel panel_south = new JPanel(false);
		panel_south.setLayout(new BoxLayout(panel_south, BoxLayout.X_AXIS));
		panel_south.add(namePanel);
		panel_south.add(fieldPanel);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(message, BorderLayout.NORTH);
		panel.add(panel_south, BorderLayout.SOUTH);

		optionNames = new String[] { "Enter", CodeLab.LABEL("CANCEL") };
		focusField = passwordField_0;

		// Recommencer tant que les saisies sont invalides
		boolean ok = false;
		while (!ok) {

			pane = new JOptionPane(
				panel,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				CodeLab.ICON, optionNames,
				optionNames[0]) {
					public void selectInitialValue() {
						if (focusField != null) {
							focusField.requestFocusInWindow();
						}
					}
				};

			String title = CodeLab.LABEL("NewpassDialog_1");
			JDialog dialog = pane.createDialog(CodeLab.FRAME, title);
			dialog.setVisible(true);

			if (cancelled()) {
				pass_1 = null;
				return;
			}

			String pass_0 = passwordField_0.getText();
			pass_1 = passwordField_1.getText();
			pass_2 = passwordField_2.getText();

			if (currentPassword != null && !pass_0.equals(currentPassword)) {
				message.setText(CodeLab.LABEL("NewpassDialog_7"));
				passwordField_0.setText("");
				passwordField_1.setText("");
				passwordField_2.setText("");
				focusField = passwordField_0;
				continue;
			}

			if (pass_1.isEmpty()) {
				message.setText(CodeLab.LABEL("NewpassDialog_8"));
				passwordField_1.setText("");
				passwordField_2.setText("");
				focusField = passwordField_1;
				continue;
			}

			if (!pass_1.equals(pass_2)) {
				message.setText(CodeLab.LABEL("NewpassDialog_5"));
				passwordField_1.setText("");
				passwordField_2.setText("");
				focusField = passwordField_1;
				continue;
			}

			ok = true;
		}
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void addItem(JPanel p, JComponent c, int x, int y, int width, int height, int align) {
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = x;
		gc.gridy = y;
		gc.gridwidth = width;
		gc.gridheight = height;
		gc.weightx = 100.0;
		gc.weighty = 100.0;
		gc.insets = new Insets(3, 3, 3, 3); // Marges autour du composant
		gc.anchor = align;
		gc.fill = GridBagConstraints.NONE;
		p.add(c, gc);
	}
}

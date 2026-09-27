package codelab.client;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import codelab.CodeLab;

/**
*	Pour saisir un nouveau mot de passe
*	@author Jérôme Lehuen
*	@version 02/05/22
*/

public class NewpassDialog {

	static String[] ConnectOptionNames = { "Enter" };

	private JOptionPane pane;
	private String pass_1 = "";
	private String pass_2 = "";
	public String getPassword() { return pass_1; }

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public NewpassDialog() {

		JLabel message = new JLabel(CodeLab.LABEL("NewpassDialog_2"));
		message.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel passwordLabel_1 = new JLabel(CodeLab.LABEL("NewpassDialog_3")+" ", JLabel.RIGHT);
		JLabel passwordLabel_2 = new JLabel(CodeLab.LABEL("NewpassDialog_4")+" ", JLabel.RIGHT);

		JTextField passwordField_1 = new JPasswordField(15);
		JTextField passwordField_2 = new JPasswordField(15);

		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0, 1));
		namePanel.add(passwordLabel_1);
		namePanel.add(passwordLabel_2);

		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridBagLayout());
		addItem(fieldPanel, passwordField_1, 0, 0, 1, 1, GridBagConstraints.WEST);
		addItem(fieldPanel, passwordField_2, 0, 1, 1, 1, GridBagConstraints.WEST);

		JPanel panel_south = new JPanel(false);
		panel_south.setLayout(new BoxLayout(panel_south, BoxLayout.X_AXIS));
		panel_south.add(namePanel);
		panel_south.add(fieldPanel);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(message, BorderLayout.NORTH);
		panel.add(panel_south, BorderLayout.SOUTH);

		// Recommencer tant que les mots de passe spnt différents

		boolean ok = false;
		while (!ok) {

			pane = new JOptionPane(
				panel,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				CodeLab.ICON, ConnectOptionNames,
				ConnectOptionNames[0]) {
					public void selectInitialValue() {
						passwordField_1.requestFocusInWindow();

					}
				};
			String title = CodeLab.LABEL("NewpassDialog_1");
			JDialog dialog = pane.createDialog(CodeLab.FRAME, title);
			dialog.setVisible(true);

			pass_1 = passwordField_1.getText();
			pass_2 = passwordField_2.getText();
			ok = (pass_1.equals(pass_2));

			message.setText(CodeLab.LABEL("NewpassDialog_5"));
			passwordField_1.setText("");
			passwordField_2.setText("");
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

package codelab.client;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import codelab.CodeLab;

/**
*	Dialogue pour se connecter au serveur
*	@author Jérôme Lehuen
*	@version 20/09/22
*/

public class ConnectDialog {

	static String[] ConnectOptionNames = { "Connect", "Cancel" };

	private JOptionPane pane;
	private JTextField loginField;
	private JTextField passwordField;

	public String getLogin() { return loginField.getText(); }
	public String getPassword() { return passwordField.getText(); }

	public boolean cancelled() {
		if (pane == null) return true;
		if (pane.getValue() == null) return true;
		if (pane.getValue().equals("Cancel")) return true;
		return false;
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ConnectDialog() {
		String host = CodeLab.PROP("SERVER_HOST");
		String port = CodeLab.PROP("SERVER_PORT");
		String phost = CodeLab.PROP("PROXY_HOST");
		String pport = CodeLab.PROP("PROXY_PORT");
		boolean proxy = (phost != null) && !phost.equals("undefined") && !phost.equals("NO_PROXY");

		JLabel info1Label = new JLabel("Server : ", JLabel.RIGHT);
		JLabel info2Label = new JLabel("Proxy : ", JLabel.RIGHT);
		JLabel info1Value = new JLabel(String.format("%s : %s", host, port));
		JLabel info2Value = proxy
			? new JLabel(String.format("%s : %s", phost, pport))
			: new JLabel(CodeLab.LABEL("ConnectDialog_1"));

		JLabel userNameLabel = new JLabel("Login : ", JLabel.RIGHT);
		JLabel passwordLabel = new JLabel("Password : ", JLabel.RIGHT);
		loginField = new JTextField(20);
		passwordField = new JPasswordField(20);

		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridBagLayout());
		addItem(namePanel, info1Label, 0, 0, GridBagConstraints.EAST);
		addItem(namePanel, info2Label, 0, 1, GridBagConstraints.EAST);
		addItem(namePanel, userNameLabel, 0, 2, GridBagConstraints.EAST);
		addItem(namePanel, passwordLabel, 0, 3, GridBagConstraints.EAST);

		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridBagLayout());
		addItem(fieldPanel, info1Value, 0, 0, GridBagConstraints.WEST);
		addItem(fieldPanel, info2Value, 0, 1, GridBagConstraints.WEST);
		addItem(fieldPanel, loginField, 0, 2, GridBagConstraints.WEST);
		addItem(fieldPanel, passwordField, 0, 3, GridBagConstraints.WEST);

		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.X_AXIS));
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);

		pane = new JOptionPane(
			connectionPanel,
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			CodeLab.ICON, ConnectOptionNames,
			ConnectOptionNames[0]) {
				public void selectInitialValue() {
					loginField.requestFocusInWindow();
				}
			};
		JDialog dialog = pane.createDialog(CodeLab.FRAME, CodeLab.LABEL("ServerPopupMenu_2"));
		dialog.setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void addItem(JPanel p, JComponent c, int x, int y, int align) {
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = x;
		gc.gridy = y;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.weightx = 100.0;
		gc.weighty = 100.0;
		gc.insets = new Insets(3, 3, 3, 3); // Marges autour du composant
		gc.anchor = align;
		gc.fill = GridBagConstraints.NONE;
		p.add(c, gc);
	}
}

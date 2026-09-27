package codelab.modules.editeur.usertable;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import codelab.CodeLab;

/**
*	Pour saisir un nouvel étudiant (login + name)
*	@author Jérôme Lehuen
*	@version 23/06/22
*/

public class NewStudentDialog {

	static String[] ConnectOptionNames = { "Enter", "Cancel" };

	private JOptionPane pane;
	private JTextField field1;
	private JTextField field2;

	public String getLogin() { return field1.getText(); }
	public String getName() { return field2.getText(); }
    public boolean cancelled() { return pane.getValue().equals("Cancel"); }

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public NewStudentDialog(String sessionId) {

		JLabel message = new JLabel(String.format(CodeLab.LABEL("NewStudentDialog_2"), sessionId));
		message.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel label1 = new JLabel(CodeLab.LABEL("NewStudentDialog_3")+" ", JLabel.RIGHT);
		JLabel label2 = new JLabel(CodeLab.LABEL("NewStudentDialog_4")+" ", JLabel.RIGHT);

		field1 = new JTextField(20);
		field2 = new JTextField(20);

		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0, 1));
		namePanel.add(label1);
		namePanel.add(label2);

		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridBagLayout());
		addItem(fieldPanel, field1, 0, 0, 1, 1, GridBagConstraints.WEST);
		addItem(fieldPanel, field2, 0, 1, 1, 1, GridBagConstraints.WEST);

		JPanel panel_south = new JPanel(false);
		panel_south.setLayout(new BoxLayout(panel_south, BoxLayout.X_AXIS));
		panel_south.add(namePanel);
		panel_south.add(fieldPanel);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(message, BorderLayout.NORTH);
		panel.add(panel_south, BorderLayout.SOUTH);

        pane = new JOptionPane(
            panel,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            CodeLab.ICON, ConnectOptionNames,
            ConnectOptionNames[0]) {
                public void selectInitialValue() {
                    field1.requestFocusInWindow();
                }
            };
        String title = CodeLab.LABEL("NewStudentDialog_1");
        JDialog dialog = pane.createDialog(CodeLab.FRAME, title);
        dialog.setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes statiques publiques
	///////////////////////////////////////////////////

	public static boolean isValidIdent(String s) {
		return s != null && s.matches("^[a-zA-Z0-9_]*$");
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

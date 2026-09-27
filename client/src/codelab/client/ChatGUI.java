package codelab.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import codelab.CodeLab;

/**
*	Classe des fenêtres de discussion
*	@author Jérôme Lehuen
*	@version 17/06/25
*/

public class ChatGUI extends JDialog implements ActionListener {

	private JTextArea textArea;
	private JTextField textField;
	private JScrollPane scroolPane;

	private String login;
	private String fullname;

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ChatGUI(String login, String fullname) {
		super(CodeLab.FRAME, false);
		this.login = login;
		this.fullname = fullname;

		// La zone de texte
		textArea = new JTextArea();
		textArea.setEditable(false);
		scroolPane = new JScrollPane(textArea);
		textArea.setBorder(BorderFactory.createEmptyBorder());
		scroolPane.setBorder(BorderFactory.createEmptyBorder());

		// Le champ de saisie
		textField = new JTextField();
		textField.addActionListener(this);

		// Organisation du panel
		setLayout(new BorderLayout());
		add(scroolPane, BorderLayout.CENTER);
		add(textField, BorderLayout.SOUTH);

		// Ajouter une marge
		JPanel panel = (JPanel) getContentPane();
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		setTitle(String.format(CodeLab.LABEL("ChatGUI_title"), fullname));
		setSize(600, 400);
		setLocationRelativeTo(CodeLab.FRAME);
		setAlwaysOnTop(true);
		setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthode de ActionListener
	///////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e) {
		String msg = textField.getText();
		if (msg.isEmpty()) return; // Ne rien faire si le message est vide
		textField.selectAll();
		textField.cut();
		appendOutgoingMessage(msg);
		CodeLab.INSTANCE.getClient().chatTo(login, msg);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private String now() {
		return dateFormat.format(new Date());
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void appendIncomingMessage(String msg) {
		textArea.append(String.format("%s [ %s ] : %s\n", now(), fullname, msg));
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public void appendOutgoingMessage(String msg) {
		textArea.append(String.format("%s [ me ] : %s\n", now(), msg));
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
}

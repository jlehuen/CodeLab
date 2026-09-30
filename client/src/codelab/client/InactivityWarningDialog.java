package codelab.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

import codelab.CodeLab;

/**
 *	Dialogue de pré-alerte avant déconnexion automatique pour inactivité.
 *	Affiche un compte à rebours dynamique en secondes et un bouton pour rester connecté.
 *	@author Jérôme Lehuen + Gemini 3.8
 *	@version 30/09/26
 */

public class InactivityWarningDialog extends JDialog {

	private final JLabel countdownLabel;
	private final JButton stayConnectedButton;
	private final Timer countdownTimer;
	private int remainingSeconds;
	private final Runnable onTimeout;
	private final Runnable onDismiss;
	private boolean dismissed = false;

	public InactivityWarningDialog(int countdownSeconds, Runnable onTimeout, Runnable onDismiss) {
		super(CodeLab.FRAME, CodeLab.LABEL("Inactivity_warning_title"), false);
		this.remainingSeconds = countdownSeconds;
		this.onTimeout = onTimeout;
		this.onDismiss = onDismiss;

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		setAlwaysOnTop(true);

		// Contenu principal avec bordure aérée
		JPanel mainPanel = new JPanel(new BorderLayout(16, 16));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

		// Icône d'avertissement standard
		JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
		mainPanel.add(iconLabel, BorderLayout.WEST);

		// Centre : En-tête et compte à rebours
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		JLabel headerLabel = new JLabel(CodeLab.LABEL("Inactivity_warning_header"));
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 13f));
		centerPanel.add(headerLabel);
		centerPanel.add(Box.createVerticalStrut(8));

		countdownLabel = new JLabel();
		countdownLabel.setFont(countdownLabel.getFont().deriveFont(Font.PLAIN, 12f));
		updateCountdownText();
		centerPanel.add(countdownLabel);

		mainPanel.add(centerPanel, BorderLayout.CENTER);

		// Bouton d'action
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		stayConnectedButton = new JButton(CodeLab.LABEL("Inactivity_stay_connected"));
		stayConnectedButton.setFont(stayConnectedButton.getFont().deriveFont(Font.BOLD, 12f));
		stayConnectedButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		stayConnectedButton.addActionListener(e -> dismiss());
		buttonPanel.add(stayConnectedButton);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
		getRootPane().setDefaultButton(stayConnectedButton);

		// Raccourcis clavier (Espace, Entrée, Echap)
		KeyAdapter keyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				dismiss();
			}
		};
		stayConnectedButton.addKeyListener(keyAdapter);
		this.addKeyListener(keyAdapter);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dismiss();
			}
		});

		pack();
		setLocationRelativeTo(CodeLab.FRAME);

		// Minuteur cadencé à 1 seconde
		countdownTimer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				remainingSeconds--;
				if (remainingSeconds <= 0) {
					countdownTimer.stop();
					triggerTimeout();
				} else {
					updateCountdownText();
				}
			}
		});
		countdownTimer.start();
	}

	private void updateCountdownText() {
		String text = String.format(CodeLab.LABEL("Inactivity_warning_countdown"), remainingSeconds);
		countdownLabel.setText(String.format("<html>%s</html>", text));
	}

	public synchronized void dismiss() {
		if (dismissed) return;
		dismissed = true;
		if (countdownTimer != null && countdownTimer.isRunning()) {
			countdownTimer.stop();
		}
		dispose();
		if (onDismiss != null) {
			onDismiss.run();
		}
	}

	private synchronized void triggerTimeout() {
		if (dismissed) return;
		dismissed = true;
		if (countdownTimer != null && countdownTimer.isRunning()) {
			countdownTimer.stop();
		}
		dispose();
		if (onTimeout != null) {
			onTimeout.run();
		}
	}

	@Override
	public void dispose() {
		if (countdownTimer != null && countdownTimer.isRunning()) {
			countdownTimer.stop();
		}
		super.dispose();
	}
}

package codelab.controllers.widgets;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*	Classe du singleton Numpad
*	@author Jérôme Lehuen
*	@version 31/01/24
*/

public class Numpad extends JFrame {

	public static Numpad INSTANCE = new Numpad(); // Singleton

	private JButton[] clavier = new JButton[12];

	///////////////////////////////////////////////////
	// Constructeur privé
	///////////////////////////////////////////////////

	private Numpad() {

		// Instanciation des boutons
		for (int i = 0 ; i < 9 ; i++) {
			clavier[i] = new JButton(Integer.valueOf(i+1).toString());
		}
		clavier[9] = new JButton("*");
		clavier[10] = new JButton("0");
		clavier[11] = new JButton("#");

		// Positionnement des boutons
		JPanel panel = new JPanel(new GridLayout(4, 3));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		for (int i = 0 ; i < 12 ; i++) {
			clavier[i].setFont(new Font(Font.MONOSPACED, Font.BOLD, 25));
			clavier[i].setFocusable(false);
			panel.add(clavier[i]);
		}

		getContentPane().add(panel);
		pack();
		setTitle("Numpad");
		setSize(new Dimension(250, 200));
		setAlwaysOnTop(true);
		setResizable(false);
		setVisible(false);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				CodeLab.INSTANCE.getToolbar().update();
			}
		});
	}

	///////////////////////////////////////////////////
	// Ouverture
	///////////////////////////////////////////////////

	public void open() {
		if (isVisible()) return;
		SwingUtilities.invokeLater(() -> {
			if (isVisible()) return;
			setLocationRelativeTo(CodeLab.INSTANCE);
			setVisible(true);
			requestFocus();
		});
	}

	public void open(Point p) {
		if (isVisible()) return;
		SwingUtilities.invokeLater(() -> {
			if (isVisible()) return;
			setLocation(p);
			setVisible(true);
			requestFocus();
		});
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public int getValue() {
		for (int i = 0 ; i < 10 ; i++) {
			if (clavier[i].getModel().isPressed())
				return i+1;
		}
		if (clavier[10].getModel().isPressed()) return 0;
		if (clavier[11].getModel().isPressed()) return 11;
		return -1;
	}

	public void reset() {}

	public void exit() {
		// En cas de fermeture définitive
		setVisible(false);
		dispose();
	}
}

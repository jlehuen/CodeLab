package codelab.controllers.widgets;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*	Classe du singleton Joystick
*	@author Jérôme Lehuen
*	@version 05/01/24
*/

public class Joystick extends JFrame implements Runnable {

	public static Joystick INSTANCE = new Joystick(); // Singleton

	private static final int MAXVALUE = 100;
	private static final int LARGEUR = 200;

	private JoystickPanel stick;
	private Point position = new Point();
	private Thread thread = null;

	///////////////////////////////////////////////////
	// Constructeur privé
	///////////////////////////////////////////////////

	private Joystick() {
		stick = new JoystickPanel(MAXVALUE, LARGEUR);
		stick.addPropertyChangeListener(updateConsoleListener);
		setLayout(new BorderLayout(0, 0));
		add(stick, BorderLayout.CENTER);
		pack();
		setTitle("Joystick");
		setAlwaysOnTop(true);
		setResizable(false);
		setVisible(false);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				CodeLab.INSTANCE.getToolbar().update();
			}
		});

		// Dans son propre thread
		thread = new Thread(this);
		thread.start();
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

	public void run() {
		// Nothing to do
	}

	private final PropertyChangeListener updateConsoleListener = (evt) -> {
		position = (Point) evt.getNewValue();
	};

	public int getValueX() { return position.x; }
	public int getValueY() { return position.y; }

	public void reset() {
		stick.reset();
	}

	public void exit() {
		// En cas de fermeture définitive
		setVisible(false);
		dispose();
	}
}

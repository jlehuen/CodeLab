package codelab.controllers.devices;

import java.awt.Point;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;

import codelab.CodeLab;
import codelab.ExceptionManager;

/**
*	Classe du singleton EventViewer
*	@author Jérôme Lehuen
*	@version 05/01/24
*/

public class EventViewer extends JFrame implements MouseListener {

	public static EventViewer INSTANCE = new EventViewer(); // Singleton
	private EventReader reader;

	private static final int MAX_LINES = 50;

	private JTextArea ta;
	private int nblines = 0;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	private EventViewer() {
		ta = new JTextArea(10,50);
		ta.setMargin(new Insets(5,5,5,5));
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.addMouseListener(this);

		DefaultCaret caret = (DefaultCaret) ta.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		JScrollPane sp = new JScrollPane(ta);
		sp.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(sp);
		pack();

		setTitle(CodeLab.LABEL("EventViewer_1"));
		ta.setText(CodeLab.LABEL("EventViewer_2"));
		ta.append(CodeLab.LABEL("EventViewer_3"));
		setAlwaysOnTop(true);
		setResizable(false);
		setVisible(false);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				CodeLab.INSTANCE.getToolbar().update();
				SwingUtilities.invokeLater(() -> {
					ta.setText(CodeLab.LABEL("EventViewer_2"));
					ta.append(CodeLab.LABEL("EventViewer_3"));
				});
				nblines = 0;
			}
		});

		reader = new EventReader(this);
	}

	///////////////////////////////////////////////////
	// Ouverture
	///////////////////////////////////////////////////

	public void open() {
		if (isVisible()) return;
		reader.reset();
		SwingUtilities.invokeLater(() -> {
			// Evite des plantages aléatoires en cas de fermeture manuelle
			setLocationRelativeTo(CodeLab.INSTANCE);
			setVisible(true);
			requestFocus();
		});
	}

	public void open(Point p) {
		if (isVisible()) return;
		reader.reset();
		SwingUtilities.invokeLater(() -> {
			// Evite des plantages aléatoires en cas de fermeture manuelle
			setLocation(p);
			setVisible(true);
			requestFocus();
		});
	}
	///////////////////////////////////////////////////
	// Méthodes puliques
	///////////////////////////////////////////////////

	public EventReader getReader() {
		return reader;
	}

	public int hasNextEvent() {
		return reader.hasNextEvent();
	}

	public String getNextEvent() {
		return reader.getNextEvent();
	}

	public int reset() {
		return reader.reset();
	}

	///////////////////////////////////////////////////
	// Autres méthodes
	///////////////////////////////////////////////////

	public void append(String line) {
		if (nblines < MAX_LINES) nblines++;
		else removeFirstLine();
		ta.append(line+'\n');
		ta.getCaret().setDot(Integer.MAX_VALUE);
	}

	private void removeFirstLine() {
		try {
			ta.replaceRange("", 0, ta.getLineEndOffset(0));
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	public void exit() {
		// En cas de fermeture définitive
		setVisible(false);
		dispose();
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener
	///////////////////////////////////////////////////

	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			new ControllerPopupMenu(ta, e.getX(), e.getY());
		}
	}
}

package codelab;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import codelab.client.ServerPopupMenu;
import codelab.console.ConsolePopupMenu;
import codelab.controllers.devices.ControllerPopupMenu;
import codelab.controllers.devices.EventReader;

/**
*	Classe des boutons de la barre d'outils
*	@author Jérôme Lehuen
*	@version 14/01/24
*/

public class ToolButton extends JButton implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final ToolButton self; // Auto-référence
	public static final int size = 11;

	protected String ident;
	protected String text;
	protected AbstractToolbar toolbar;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolButton(String ident, String text, String tiptext, Icon icon, AbstractToolbar toolbar) {
		super(icon);
		self = this;
		this.text = text;
		this.ident = ident;
		this.toolbar = toolbar;

		Font font = new Font(getFont().getName(), getFont().getStyle(), size);

		setText(text);
		setFont(font);
		setForeground(Color.DARK_GRAY);
		setVerticalTextPosition(BOTTOM);
		setHorizontalTextPosition(CENTER);
		setToolTipText(tiptext);

		setRolloverEnabled(false);
		setFocusPainted(false);
		setBorderPainted(false);
		setOpaque(false);

		addActionListener(this);

		addMouseListener(new MouseAdapter() {

			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					switch (ident) {
						case "START":
							new RunPopupMenu(CodeLab.INSTANCE, (JButton)self, e.getX(), e.getY());
							break;
						case "TERM":
							new ConsolePopupMenu(CodeLab.INSTANCE, (JButton)self, e.getX(), e.getY());
							break;
						case "CTRL":
							if (!EventReader.natives_OK) return;
							new ControllerPopupMenu((JButton)self, e.getX(), e.getY());
							break;
					}
				}
				if (ident.equals("SERVER") && SwingUtilities.isLeftMouseButton(e) && CodeLab.WRK_SERVER_OK) {
					new ServerPopupMenu((JButton)self, e.getX(), e.getY());
				}
				if (ident.equals("INFO") && SwingUtilities.isLeftMouseButton(e)) {
					new CodeLabMenu((JButton)self, e.getX(), e.getY());
				}
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
		});
	}

	public void actionPerformed(ActionEvent e) {
		toolbar.action(ident);
	}

	public Point getAbsolutePosition() {
		return new Point(getLocation().x, getLocation().y);
	}

	public void setTextProp(String text) {
		// Juste pour mémoriser le texte
		this.text = text;
	}

	public void update() {
		// Pour faire apparaître ou non le texte
		if (CodeLab.TEXT_UNDER_TOOLBUTTON) setText(text);
		else setText(null);
	}
}

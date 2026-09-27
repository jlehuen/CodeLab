package codelab.modules.editeur.scratch.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*	Super-classe des fenêtres d'édition des widgets
*	@author Jérôme Lehuen
*	@version 10/06/22
*/

public abstract class AbstractEditDialog extends JDialog implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private JPanel panel = new JPanel(new BorderLayout());
	private JLabel title1 = new JJLabel();
	private JLabel title2 = new JJLabel();
	private JPanel centerPanel = new JPanel();
	private Color color = new Color(255, 253, 116); // https://htmlcolorcodes.com/fr/
	private AbstractWidget widget;

	// Pour simplifier...
	protected String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractEditDialog(AbstractWidget widget) {
		super(CodeLab.FRAME, ModalityType.DOCUMENT_MODAL);
		this.widget = widget;

		setLocation(widget.getAbsolutePosition());
		setMinimumSize(new Dimension(400, 100));
		setUndecorated(true);
		setResizable(false);
		// Ajout des listeners de souris
		addMouseListener(this);
		addMouseMotionListener(this);

		title1.setOpaque(true);
		title1.setBackground(color);
		title1.setFont(title2.getFont().deriveFont(title1.getFont().getStyle() | Font.BOLD));
		title1.setForeground(Color.DARK_GRAY);
		title1.setText("  " + LABEL("AbstractEditDialog_title"));

		title2.setOpaque(true);
		title2.setBackground(color);
		title2.setFont(title2.getFont().deriveFont(title2.getFont().getStyle() | Font.BOLD));
		title2.setForeground(Color.RED);
		title2.setText("  " + widget.toString_1(false));

		addComponent(title1, 10, 5, 0, 15);
		addComponent(title2, 0, 5, 10, 15);

		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(createButtonPanel(this), BorderLayout.SOUTH);
		add(panel);
	}

	protected void addComponent(JComponent component, int margin_top, int margin_left, int margin_bottom, int margin_right) {
		component.setBorder(BorderFactory.createEmptyBorder(margin_top, margin_left, margin_bottom, margin_right));
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		centerPanel.add(component);
	}

	protected void addComponent(JComponent component) {
		addComponent(component, 10, 0, 0, 0);
	}

	public void pack() {
		super.pack();
		setVisible(true);
	}

	protected void valider() {
		widget.valider();
	}

	///////////////////////////////////////////////////
	// Le panel du bouton de validation
	///////////////////////////////////////////////////

	protected JPanel createButtonPanel(AbstractEditDialog dialog) {

		JButton btn_ok = new JButton(LABEL("OK"));
		btn_ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.valider();
				dialog.dispose();
			}
		});

		JButton btn_cancel = new JButton(LABEL("CANCEL"));
		btn_cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		panel.add(btn_cancel);
		panel.add(btn_ok);
		return panel;
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener
	///////////////////////////////////////////////////

	private Point origine;
	private boolean deplacement = false;

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			deplacement = true;
			origine = e.getPoint();
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (deplacement) {
			deplacement = false;
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseMotionListener
	///////////////////////////////////////////////////

	public void mouseMoved(MouseEvent e) {}

	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (deplacement) {
				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen(p, panel);
				p.translate(-origine.x, -origine.y);
				setLocation(p);
			}
		}
	}

	///////////////////////////////////////////////////
	// Pour avoir un label de largeur maximum
	///////////////////////////////////////////////////

	private class JJLabel extends JLabel {
		private static final long serialVersionUID = 1L;

		@Override
		public Dimension getMaximumSize() {
        	Dimension d = super.getMaximumSize();
        	d.width = Integer.MAX_VALUE;
        	return d;
		}
	}
}

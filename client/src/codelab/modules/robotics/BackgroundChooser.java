package codelab.modules.robotics;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.Rectangle2D;

import java.util.*;
import javax.swing.*;

import codelab.*;
import codelab.utils.*;

/**
*	Fenêtre de sélection du fond
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class BackgroundChooser extends JDialog {

	private static final long serialVersionUID = 1L;

	private Simulator simulator;
	private CustomLabel label;
	private ArrayList<Background> liste;
	private int index;
	private int current_index;

	private int LARG = 400;
	private int HAUT = 300;

	private void closeFrame() {
		setVisible(false);
		dispose();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public BackgroundChooser(ModuleRobotics simupane) {
		super(CodeLab.FRAME, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		simulator = simupane.getSimulator();
		liste = simulator.getFonds(); // La liste de fonds
		index = simulator.getIndexFond(); // L'index du fond actuel
		current_index = index;

		label = new CustomLabel();
		label.setPreferredSize(new Dimension(LARG, HAUT));
		updateImage(); // label doit être instancié

		JButton bouton_gauche = new JButton();
		JButton bouton_droite = new JButton();
		bouton_gauche.setIcon(new ImageIcon(ResourceUtils.loadBufferedImageAsRessource("icons/icon_left.png")));
		bouton_droite.setIcon(new ImageIcon(ResourceUtils.loadBufferedImageAsRessource("icons/icon_right.png")));
		bouton_gauche.setFocusable(false);
		bouton_droite.setFocusable(false);

		bouton_gauche.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (--index < 0) index = liste.size() - 1;
				updateImage();
			}
		});

		bouton_droite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (++index == liste.size()) index = 0;
				updateImage();
			}
		});

		final MouseAdapter selectAdapter = new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					final Background fond = liste.get(index);
					final int selectedIndex = index;
					closeFrame();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (selectedIndex != current_index) {
								simulator.setBackgroundFromSelector(fond, selectedIndex);
								simulator.repaint();
							}
						}
					});
				}
			}
		};
		addMouseListener(selectAdapter);
		label.addMouseListener(selectAdapter);

		// Fermeture sur la touche Échap
		getRootPane().registerKeyboardAction(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeFrame();
				}
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);

		getContentPane().add(label, BorderLayout.CENTER);
		getContentPane().add(bouton_gauche, BorderLayout.WEST);
		getContentPane().add(bouton_droite, BorderLayout.EAST);
		pack();

		setTitle(CodeLab.LABEL("BackgroundChooserTitle"));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	private void updateImage() {
		BufferedImage image = liste.get(index).getImage();
		String name = liste.get(index).getName();
		image = GraphUtils.resize(image, LARG, HAUT);
		label.setIcon(new ImageIcon(image));
		label.setName(name);
	}

	///////////////////////////////////////////////////
	// Classe privée pour afficher le nom du fond
	///////////////////////////////////////////////////

	private class CustomLabel extends JLabel {

		private static final long serialVersionUID = 1L;
		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g2d = (Graphics2D) graphics;
			g2d.setFont(new Font("Arial", Font.BOLD, 18));
			FontMetrics fontMetrics = g2d.getFontMetrics();
			Rectangle2D textBounds = fontMetrics.getStringBounds(name, g2d);
			int x = (int) (getBounds().getWidth() - textBounds.getWidth()) / 2;
			g2d.setColor(Color.BLACK);
			g2d.drawString(name, x, 18);
		}
	}
}

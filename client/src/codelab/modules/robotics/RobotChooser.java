package codelab.modules.robotics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.utils.ResourceUtils;

/**
*	Fenêtre de sélection du robot
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class RobotChooser extends JDialog {

	private static final long serialVersionUID = 1L;

	private final Simulator simulator;
	private final ArrayList<AbstractRobot> robots;
	private int index;
	private AbstractRobot robot;

	private final int LARG = 400;
	private final int HAUT = 400;

	private void closeFrame() {
		setVisible(false);
		dispose();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public RobotChooser(ModuleRobotics module) {
		super(CodeLab.FRAME, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		simulator = module.getSimulator();
		robots = simulator.getRobots(); // La liste de robots
		index = simulator.getRobot().index; // La position du robot actuel
		robot = robots.get(index); // Le robot actuel

		final ChoosePanel panel = new ChoosePanel();
		final JButton bouton_gauche = new JButton();
		final JButton bouton_droite = new JButton();

		panel.setPreferredSize(new Dimension(LARG, HAUT));
		bouton_gauche.setIcon(new ImageIcon(ResourceUtils.loadBufferedImageAsRessource("icons/icon_left.png")));
		bouton_droite.setIcon(new ImageIcon(ResourceUtils.loadBufferedImageAsRessource("icons/icon_right.png")));
		bouton_gauche.setFocusable(false);
		bouton_droite.setFocusable(false);

		bouton_gauche.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (--index < 0) index = robots.size() - 1;
				robot = robots.get(index);
				panel.repaint();
			}
		});

		bouton_droite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (++index == robots.size()) index = 0;
				robot = robots.get(index);
				panel.repaint();
			}
		});

		panel.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					final AbstractRobot selectedRobot = robot;
					final int selectedIndex = index;
					closeFrame();
					// Application du robot en différé pour éviter tout conflit de modalité Cocoa AWT avec Numpad
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							simulator.setRobot(selectedRobot, selectedIndex);
							simulator.repaint();
						}
					});
				}
			}
		});

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

		getContentPane().add(panel, BorderLayout.CENTER);
		getContentPane().add(bouton_gauche, BorderLayout.WEST);
		getContentPane().add(bouton_droite, BorderLayout.EAST);
		pack();

		setTitle(CodeLab.LABEL("RobotChooserTitle"));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	class ChoosePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		public ChoosePanel() {
			setBackground(Color.WHITE);
		}

		// Pour afficher sur plusieurs lignes
		void drawString(Graphics g2d, String text, int x, int y) {
			for (String line : text.split("\n"))
				g2d.drawString(line, x, y += g2d.getFontMetrics().getHeight());
		}

		public void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			final Graphics2D g2d = (Graphics2D) graphics;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			if (robot == null) return;

			final int x = LARG / 2 - robot.CENTER_X;
			final int y = HAUT / 2 - robot.CENTER_Y;
			if (robot.image != null) {
				g2d.drawImage(robot.image, x, y, null);
			}

			// Affichage des capteurs
			for (int i = 0; i < 4; i++) {
				if (robot.getSensor(i) != null) {
					robot.getSensor(i).drawBis(g2d, x, y);
				}
			}

			// Pour afficher la liste des capteurs
			g2d.setFont(new Font("Arial", Font.BOLD, 15));
			g2d.setColor(Color.GRAY);
			final String descr = String.format("%s\n%s", robot.getName(), robot.getConfiguration());
			drawString(g2d, descr, 10, 10);
		}
	}
}

package codelab.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.Timer;

import codelab.CodeLab;

/**
*	Classe du Splash Screen
*	@author Jérôme Lehuen
*	@version 12/09/26
*/

public class SplashWindow extends JWindow {

	private static JProgressBar progressBar = new JProgressBar(0, 100);
	private static int progression = 0;
	private static Timer timer;

	private static SplashWindow INSTANCE;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public SplashWindow(String filename, String text) {

		BufferedImage image = ResourceUtils.loadBufferedImageAsRessource(filename);
		Graphics g = image.getGraphics();
		g.setFont(new Font("Verdana", Font.PLAIN, 14));
		int x = image.getWidth() - g.getFontMetrics().stringWidth(text) - 10;
		g.drawString(text, x, 20);
		g.dispose();

		JLabel label = new JLabel(new ImageIcon(image));
		label.setOpaque(false);

		progressBar.setStringPainted(true);

		getContentPane().add(label, BorderLayout.CENTER);
		getContentPane().add(progressBar, BorderLayout.SOUTH);
		pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension labelSize = label.getPreferredSize();
		setLocation(screenSize.width/2 - (labelSize.width/2),
					screenSize.height/2 - (labelSize.height/2));

		setBackground(new Color(0, 255, 0, 0)); // Préserve la transparence du canal alpha du PNG
		setVisible(true);

		// Pour faire avancer la progression automatiquement
		timer = new Timer(CodeLab.SPLASH_DELAY, e -> progressBar.setValue(progression++));
		timer.start();
		INSTANCE = this;
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public static void setProgression(int value) {
		progression = value;
	}

	public static void close() {
		if (timer != null) {
			timer.stop();
		}
		if (INSTANCE != null) {
			INSTANCE.setVisible(false);
			INSTANCE.dispose();
			INSTANCE = null;
		}
	}
}

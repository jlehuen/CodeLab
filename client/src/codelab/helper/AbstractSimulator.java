package codelab.helper;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import javax.swing.JPanel;

import codelab.DataTable;
import codelab.ExceptionManager;
import codelab.utils.Utils;

/**
*	Classe abtraite des simulateurs
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public abstract class AbstractSimulator extends JPanel implements Runnable {

	private static final long serialVersionUID = 1L;
	private static Toolkit TOOLKIT = Toolkit.getDefaultToolkit();

	private int delay = 10; // ms

	public void setDelay(int delay) {
		this.delay = delay;
	}

	///////////////////////////////////////////////////
	// Méthodes protégées
	///////////////////////////////////////////////////

	protected abstract void update(); // Doit être implémenté => Actualisation du simulateur
	protected void updateValues() {} // Peut-être surchargé => Actualisation des propriétés du dashboard

	protected Graphics2D getGraphics2D(Graphics graphics) {
		Graphics2D g2d = (Graphics2D) graphics;
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		return g2d;
	}

	///////////////////////////////////////////////////
	// Gestion du Dashboard
	///////////////////////////////////////////////////

	private DataTable dashboard;

	public void setDashboard(DataTable dashboard) {
		this.dashboard = dashboard;
	}

	private void updateDashboard() {
		updateValues(); // Actualisation des propriétés du dashboard
		if (dashboard != null) dashboard.update(); // Actualisation du dashboard
	}

	///////////////////////////////////////////////////
	// Méthode de l'interface Runnable
	///////////////////////////////////////////////////

	private Thread simuthread = null;
	private boolean isrunning = false;

	public boolean isRunning() {
		return isrunning;
	}

	public void run() {
		while (isrunning && !Thread.currentThread().isInterrupted()) {
			update(); // Actualisation du simulateur
			updateDashboard(); // Actualisation de la table
			TOOLKIT.sync(); // Synchroniser l'affichage (systèmes Linux)
			Utils.wait(delay);
		}
		updateDashboard(); // Une dernière fois
	}

	public void start() {
		if (isrunning) return;
		isrunning = true;
		simuthread = new Thread(this);
		simuthread.start();
	}

	public void stop() {
		if (!isrunning) return;
		isrunning = false;
		Thread t = simuthread;
		if (t != null) {
			t.interrupt();
			try {
				t.join(200); // Timeout défensif de 200ms pour ne jamais bloquer l'EDT
			}
			catch (InterruptedException e) {
				ExceptionManager.process(e);
			}
		}
		simuthread = null;
	}

	public void reset() {
		this.stop();
	}
}

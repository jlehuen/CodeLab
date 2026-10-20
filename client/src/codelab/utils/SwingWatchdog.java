package codelab.utils;

import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
 * Thread Watchdog de surveillance de l'Event Dispatch Thread (EDT) Swing.
 * Détecte les gels d'interface, génère un rapport de diagnostic complet
 * (deadlocks, thread dump) dans le dossier "Crash report" à la base de l'exploration
 * ainsi que dans codelab.log et crash.log, et force l'arrêt des programmes si nécessaire.
 * 
 * @author Jérôme Lehuen + Gemini 3.8
 * @version 09/10/26
 */
public class SwingWatchdog extends Thread {

	private static final long PING_INTERVAL_MS = 1000;   // Fréquence du heartbeat (1s)
	private static final long FREEZE_TIMEOUT_MS = 4000;  // Seuil de détection de freeze (4s)

	private volatile long lastHeartbeat = System.currentTimeMillis();
	private volatile boolean running = true;
	private boolean frozen = false;

	public SwingWatchdog() {
		super("CodeLab-SwingWatchdog");
		setDaemon(true); // N'empêche pas l'arrêt de la JVM
	}

	public void shutdown() {
		running = false;
		interrupt();
	}

	@Override
	public void run() {
		// Attendre quelques secondes après le démarrage avant d'activer la surveillance
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ignored) {
			return;
		}

		lastHeartbeat = System.currentTimeMillis();

		while (running) {
			long beforeSleep = System.currentTimeMillis();
			try {
				Thread.sleep(PING_INTERVAL_MS);
			} catch (InterruptedException e) {
				break;
			}
			long actualSleep = System.currentTimeMillis() - beforeSleep;

			// Si le watchdog a été suspendu par l'OS (mise en veille de la machine ou pause système)
			if (actualSleep > PING_INTERVAL_MS + 2000) {
				lastHeartbeat = System.currentTimeMillis();
				continue;
			}

			// Envoyer un battement de cœur à l'EDT
			SwingUtilities.invokeLater(() -> {
				lastHeartbeat = System.currentTimeMillis();
			});

			long elapsed = System.currentTimeMillis() - lastHeartbeat;

			if (elapsed > FREEZE_TIMEOUT_MS) {
				if (!frozen) {
					frozen = true;
					handleFreeze(elapsed);
				}
			} else {
				if (frozen) {
					frozen = false;
					CodeLab.logger("WATCHDOG: L'interface Swing s'est débloquée et répond à nouveau normalement.");
					CrashReportManager.refreshFileManager();
				}
			}
		}
	}

	private void handleFreeze(long elapsedMs) {
		CodeLab.logger(String.format("WATCHDOG ALERT: L'interface Swing (EDT) ne répond plus depuis %d ms !", elapsedMs));

		// 1. Diagnostic complet et persistant dans le dossier "Crash report" (local ou dist)
		CrashReportManager.saveFreezeReport(elapsedMs);

		// 2. Si un programme utilisateur tourne, tenter de le stopper pour libérer CodeLab
		try {
			if (CodeLab.INSTANCE != null && CodeLab.INSTANCE.isRunning()) {
				CodeLab.logger("WATCHDOG: Arrêt d'urgence du programme utilisateur en cours...");
				CodeLab.INSTANCE.halt();
			}
		} catch (Exception e) {
			CodeLab.logger("WATCHDOG: Erreur lors de l'arrêt d'urgence: " + e.getMessage());
		}
	}
}

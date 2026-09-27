package codelab.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
 * Thread Watchdog de surveillance de l'Event Dispatch Thread (EDT) Swing.
 * Détecte les gels d'interface, génère un rapport de diagnostic complet
 * (deadlocks, thread dump) dans la console, codelab.log et un fichier persistant freeze.log,
 * et force l'arrêt des programmes en cours si nécessaire.
 * 
 * @author Jérôme Lehuen + Gemini 3.8
 * @version 14/09/26
 */
public class SwingWatchdog extends Thread {

	private static final long PING_INTERVAL_MS = 1000;   // Fréquence du heartbeat (1s)
	private static final long FREEZE_TIMEOUT_MS = 4000;  // Seuil de détection de freeze (4s)
	private static final String FREEZE_LOG_FILE = CodeLab.CODELAB_FILES + "/freeze.log";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
				}
			}
		}
	}

	private void handleFreeze(long elapsedMs) {
		CodeLab.logger(String.format("WATCHDOG ALERT: L'interface Swing (EDT) ne répond plus depuis %d ms !", elapsedMs));

		// 1. Diagnostic complet et persistant (deadlocks + dump de tous les threads JVM)
		dumpFullDiagnostics(elapsedMs);

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

	private void dumpFullDiagnostics(long elapsedMs) {
		StringBuilder sb = new StringBuilder();
		String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

		sb.append("\n====================================================================\n");
		sb.append(String.format(">>> CODE-LAB FREEZE REPORT - %s <<<\n", timestamp));
		sb.append(String.format("EDT freeze duration: %d ms (timeout threshold: %d ms)\n", elapsedMs, FREEZE_TIMEOUT_MS));
		sb.append("====================================================================\n");

		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

		// 1. Détection de Deadlocks JVM
		long[] deadlockedIds = threadBean.findDeadlockedThreads();
		if (deadlockedIds != null && deadlockedIds.length > 0) {
			sb.append("\n!!! FATAL DEADLOCK DETECTED BETWEEN THREADS !!!\n");
			ThreadInfo[] dlInfos = threadBean.getThreadInfo(deadlockedIds, true, true);
			for (ThreadInfo ti : dlInfos) {
				if (ti != null) {
					sb.append(String.format("  Thread \"%s\" (ID: %d, State: %s)\n", ti.getThreadName(), ti.getThreadId(), ti.getThreadState()));
					sb.append(String.format("    - Waiting for lock: %s\n", ti.getLockName()));
					sb.append(String.format("    - Held by: \"%s\" (ID: %d)\n", ti.getLockOwnerName(), ti.getLockOwnerId()));
				}
			}
			sb.append("--------------------------------------------------------------------\n");
		} else {
			sb.append("\nNo classic Java object monitor deadlock detected.\n");
			sb.append("--------------------------------------------------------------------\n");
		}

		// 2. Dump complet de tous les threads avec verrous et stack traces
		sb.append("\nFULL THREAD DUMP (JVM):\n");
		ThreadInfo[] allThreads = threadBean.dumpAllThreads(true, true);
		for (ThreadInfo ti : allThreads) {
			if (ti == null) continue;
			sb.append(String.format("\n\"%s\" Id=%d State=%s", ti.getThreadName(), ti.getThreadId(), ti.getThreadState()));
			if (ti.getLockName() != null) {
				sb.append(String.format(" on %s", ti.getLockName()));
			}
			if (ti.getLockOwnerName() != null) {
				sb.append(String.format(" owned by \"%s\" Id=%d", ti.getLockOwnerName(), ti.getLockOwnerId()));
			}
			if (ti.isSuspended()) sb.append(" (suspended)");
			if (ti.isInNative()) sb.append(" (in native)");
			sb.append("\n");

			for (StackTraceElement ste : ti.getStackTrace()) {
				sb.append("\tat ").append(ste.toString()).append("\n");
			}
		}
		sb.append("\n==================== END OF FREEZE REPORT ====================\n\n");

		String report = sb.toString();

		// Affichage console stderr
		System.err.print(report);

		// Journalisation dans codelab.log
		CodeLab.logger(report);

		// Sauvegarde persistante dans codelab.files/freeze.log
		try {
			File logFile = new File(FREEZE_LOG_FILE);
			File parent = logFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
				out.print(report);
				out.flush();
			}
			CodeLab.logger(String.format("WATCHDOG: Rapport de freeze écrit avec succès dans '%s'", FREEZE_LOG_FILE));
		} catch (Exception e) {
			CodeLab.logger("WATCHDOG: Impossible d'écrire le rapport dans " + FREEZE_LOG_FILE + ": " + e.getMessage());
		}
	}
}

package codelab.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import codelab.CodeLab;
import codelab.modules.editeur.manager.FileDescr;
import codelab.modules.editeur.manager.FileManager;

/**
 * Gestionnaire des rapports de crash et de freeze.
 * Enregistre les diagnostics complets dans un dossier "Crash report"
 * situé à la base de l'exploration active (programs/local ou programs/dist).
 *
 * @author Jérôme Lehuen + Antigravity
 * @version 09/10/26
 */
public class CrashReportManager {

	public static final String CRASH_REPORT_DIR_NAME = "Crash report";
	private static final String PERSISTENT_CRASH_LOG = CodeLab.CODELAB_FILES + "/crash.log";

	private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final long DEBOUNCE_MS = 2500;
	private static volatile long lastCrashReportTime = 0;
	private static volatile long lastFreezeReportTime = 0;
	private static final int MAX_REPORTS = 30;

	/**
	 * Retourne le répertoire de base de l'exploration actuelle
	 * (programs/dist en mode connecté, programs/local en mode autonome).
	 */
	public static String getExplorationBase() {
		if (CodeLab.INSTANCE != null) {
			try {
				return CodeLab.INSTANCE.getProgramDir();
			} catch (Exception ignored) {}
		}
		if (CodeLab.CONNECTED_MODE) {
			return CodeLab.PROG_DIST_FOLDER;
		}
		return CodeLab.PROG_LOCAL_FOLDER;
	}

	/**
	 * Retourne et crée si nécessaire le dossier "Crash report"
	 * à la base de l'exploration active.
	 */
	public static File getCrashReportFolder() {
		String base = getExplorationBase();
		File folder = new File(base, CRASH_REPORT_DIR_NAME);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	/**
	 * Génère et enregistre un rapport de freeze (blocage EDT détecté par le Watchdog).
	 */
	public static File saveFreezeReport(long elapsedMs) {
		long now = System.currentTimeMillis();
		if (now - lastFreezeReportTime < DEBOUNCE_MS) {
			return null;
		}
		lastFreezeReportTime = now;

		String timestampFile = LocalDateTime.now().format(FILE_DATE_FORMAT);
		String timestampDisplay = LocalDateTime.now().format(DISPLAY_DATE_FORMAT);

		StringBuilder sb = new StringBuilder();
		sb.append("================================================================================\n");
		sb.append(String.format(">>> CODELAB FREEZE REPORT - %s <<<\n", timestampDisplay));
		sb.append("================================================================================\n\n");

		appendEnvironment(sb);
		sb.append(String.format("\tEDT Freeze      : %d ms (seuil d'alerte : 4000 ms)\n\n", elapsedMs));

		appendDeadlocksAndThreadDump(sb);

		sb.append("================================================================================\n");
		sb.append("END OF FREEZE REPORT\n");
		sb.append("================================================================================\n");

		String report = sb.toString();
		File folder = getCrashReportFolder();
		File reportFile = new File(folder, String.format("freeze_%s.log", timestampFile));

		writeReport(reportFile, report, "FREEZE");
		return reportFile;
	}

	/**
	 * Génère et enregistre un rapport de crash (exception non gérée ou fatale).
	 */
	public static File saveCrashReport(Throwable throwable, String context) {
		long now = System.currentTimeMillis();
		if (now - lastCrashReportTime < DEBOUNCE_MS) {
			return null;
		}
		lastCrashReportTime = now;

		String timestampFile = LocalDateTime.now().format(FILE_DATE_FORMAT);
		String timestampDisplay = LocalDateTime.now().format(DISPLAY_DATE_FORMAT);

		StringBuilder sb = new StringBuilder();
		sb.append("================================================================================\n");
		sb.append(String.format(">>> CODELAB CRASH REPORT - %s <<<\n", timestampDisplay));
		sb.append("================================================================================\n\n");

		appendEnvironment(sb);
		if (context != null && !context.isEmpty()) {
			sb.append(String.format("\tContexte        : %s\n", context));
		}
		sb.append("\n");

		sb.append("--------------------------------------------------------------------------------\n");
		sb.append("EXCEPTION DETAILS:\n");
		sb.append("--------------------------------------------------------------------------------\n");
		if (throwable != null) {
			sb.append(String.format("Exception : %s\n", throwable.getClass().getName()));
			sb.append(String.format("Message   : %s\n", throwable.getMessage()));
			sb.append("Stacktrace:\n");
			StringWriter sw = new StringWriter();
			throwable.printStackTrace(new PrintWriter(sw));
			sb.append(sw.toString());
		} else {
			sb.append("Aucune exception fournie.\n");
		}
		sb.append("\n");

		appendDeadlocksAndThreadDump(sb);

		sb.append("================================================================================\n");
		sb.append("END OF CRASH REPORT\n");
		sb.append("================================================================================\n");

		String report = sb.toString();
		File folder = getCrashReportFolder();
		File reportFile = new File(folder, String.format("crash_%s.log", timestampFile));

		writeReport(reportFile, report, "CRASH");
		return reportFile;
	}

	private static void appendEnvironment(StringBuilder sb) {
		sb.append("ENVIRONNEMENT :\n");
		sb.append(String.format("\tCodeLab Version : %s (build %s)\n", CodeLab.VERSION, CodeLab.BUILD));
		sb.append(String.format("\tSystème d'expl. : %s (%s)\n", CodeLab.SYSTEM, CodeLab.ARCHITECTURE));
		sb.append(String.format("\tJava Home       : %s\n", CodeLab.JAVA_HOME));
		sb.append(String.format("\tJava Version    : %s\n", System.getProperty("java.version")));

		boolean connected = (CodeLab.INSTANCE != null && CodeLab.INSTANCE.isConnected()) || CodeLab.CONNECTED_MODE;
		sb.append(String.format("\tMode            : %s\n", connected ? "Distant (Connecté)" : "Local (Autonome)"));
		sb.append(String.format("\tBase explorer   : %s\n", getExplorationBase()));

		if (CodeLab.INSTANCE != null && CodeLab.INSTANCE.isConnected()) {
			sb.append(String.format("\tUtilisateur     : %s (Session : %s)\n", CodeLab.INSTANCE.getUsername(), CodeLab.INSTANCE.getSession()));
			sb.append(String.format("\tIP Client       : %s\n", CodeLab.INSTANCE.getClientIP()));
		}

		try {
			if (CodeLab.INSTANCE != null && CodeLab.INSTANCE.getEditor() != null) {
				String curFile = CodeLab.INSTANCE.getEditor().getCurrentFilename();
				if (curFile != null && !curFile.isEmpty()) {
					sb.append(String.format("\tFichier actif   : %s\n", curFile));
				}
			}
		} catch (Throwable ignored) {}

		Runtime rt = Runtime.getRuntime();
		long freeMem = rt.freeMemory() / (1024 * 1024);
		long totalMem = rt.totalMemory() / (1024 * 1024);
		long maxMem = rt.maxMemory() / (1024 * 1024);
		sb.append(String.format("\tMémoire JVM     : %d Mo libre / %d Mo totale / %d Mo max\n", freeMem, totalMem, maxMem));
	}

	private static void appendDeadlocksAndThreadDump(StringBuilder sb) {
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

		// 1. Détection des deadlocks
		long[] deadlockedIds = null;
		try {
			deadlockedIds = threadBean.findDeadlockedThreads();
		} catch (Throwable ignored) {}

		if (deadlockedIds != null && deadlockedIds.length > 0) {
			sb.append("--------------------------------------------------------------------------------\n");
			sb.append("!!! BLOCAGE FATAL (DEADLOCK) ENTRE THREADS DETECTE !!!\n");
			sb.append("--------------------------------------------------------------------------------\n");
			ThreadInfo[] dlInfos = threadBean.getThreadInfo(deadlockedIds, true, true);
			for (ThreadInfo ti : dlInfos) {
				if (ti != null) {
					sb.append(String.format("  Thread \"%s\" (ID: %d, State: %s)\n", ti.getThreadName(), ti.getThreadId(), ti.getThreadState()));
					sb.append(String.format("    - En attente de verrou : %s\n", ti.getLockName()));
					sb.append(String.format("    - Détenu par : \"%s\" (ID: %d)\n", ti.getLockOwnerName(), ti.getLockOwnerId()));
				}
			}
			sb.append("\n");
		} else {
			sb.append("--------------------------------------------------------------------------------\n");
			sb.append("DEADLOCK DETECTION : Aucun deadlock de moniteurs Java détecté.\n");
			sb.append("--------------------------------------------------------------------------------\n\n");
		}

		// 2. Thread dump complet
		sb.append("--------------------------------------------------------------------------------\n");
		sb.append("DUMP COMPLET DES THREADS (JVM) :\n");
		sb.append("--------------------------------------------------------------------------------\n");
		try {
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
		} catch (Throwable t) {
			sb.append("Impossible de capturer le dump des threads : ").append(t.getMessage()).append("\n");
		}
		sb.append("\n");
	}

	private static void writeReport(File reportFile, String content, String type) {
		// 1. Sortie stderr et logger
		System.err.print(content);
		CodeLab.logger(content);

		// 2. Écriture dans le dossier Crash report
		try {
			File parent = reportFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			try (PrintWriter out = new PrintWriter(new FileWriter(reportFile, false))) {
				out.print(content);
				out.flush();
			}
			CodeLab.logger(String.format("WATCHDOG: Rapport de %s écrit avec succès dans '%s'", type, reportFile.getAbsolutePath()));
		} catch (Exception e) {
			CodeLab.logger(String.format("WATCHDOG: Impossible d'écrire le rapport dans '%s': %s", reportFile.getAbsolutePath(), e.getMessage()));
		}

		// 3. Archivage cumulatif dans codelab.files/crash.log
		try {
			File persistent = new File(PERSISTENT_CRASH_LOG);
			File pParent = persistent.getParentFile();
			if (pParent != null && !pParent.exists()) {
				pParent.mkdirs();
			}
			try (PrintWriter out = new PrintWriter(new FileWriter(persistent, true))) {
				out.print(content);
				out.println();
				out.flush();
			}
		} catch (Exception ignored) {}

		// 4. Nettoyage des anciens rapports si > MAX_REPORTS
		purgeOldReports(reportFile.getParentFile(), MAX_REPORTS);

		// 5. Rafraîchissement du FileManager pour faire apparaître le dossier et le fichier
		refreshFileManager();
	}

	private static void purgeOldReports(File dir, int maxCount) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) return;
		try {
			File[] files = dir.listFiles((d, name) -> name.endsWith(".log") || name.endsWith(".txt"));
			if (files != null && files.length > maxCount) {
				Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
				int toDelete = files.length - maxCount;
				for (int i = 0; i < toDelete; i++) {
					files[i].delete();
				}
			}
		} catch (Throwable ignored) {}
	}

	/**
	 * Actualise l'arborescence de FileManager sur l'EDT Swing
	 * et déplie automatiquement le dossier "Crash report" pour rendre
	 * le diagnostic immédiatement visible à l'utilisateur.
	 */
	public static void refreshFileManager() {
		SwingUtilities.invokeLater(() -> {
			try {
				if (CodeLab.INSTANCE != null && CodeLab.INSTANCE.getEditor() != null) {
					FileManager fm = CodeLab.INSTANCE.getEditor().getManager();
					if (fm != null) {
						fm.populate();
						expandCrashReportFolder(fm);
					}
				}
			} catch (Throwable ignored) {}
		});
	}

	private static void expandCrashReportFolder(FileManager fm) {
		try {
			DefaultMutableTreeNode root = fm.getRootNode();
			if (root == null) return;
			Enumeration<?> children = root.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				Object uo = child.getUserObject();
				if (uo instanceof FileDescr) {
					FileDescr fd = (FileDescr) uo;
					if (CRASH_REPORT_DIR_NAME.equalsIgnoreCase(fd.getFilename())) {
						fm.expandPath(new TreePath(child.getPath()));
						break;
					}
				}
			}
		} catch (Throwable ignored) {}
	}
}

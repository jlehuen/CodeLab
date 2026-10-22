package codelab.eval;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import org.apache.commons.io.FileUtils;

import codelab.CodeLab;
import codelab.console.Console;
import codelab.modules.editeur.manager.FileManager;
import codelab.utils.Utils;
import codelab.utils.WaitingDialog;

/**
 * Gestionnaire d'évaluation automatique par session, par étudiant et par dossier.
 * Orchestre l'appel au script Python eval_codelab.py en tâche de fond (non-bloquant EDT)
 * et ouvre directement le compte-rendu Markdown dans l'éditeur de CodeLab.
 *
 * @author Jérôme Lehuen
 * @version 09/10/26
 */
public class SessionEvaluator {

	private static final List<SkillDescr> availableSkills = new ArrayList<>();
	private static final Color COLOR_WARNING = new Color(220, 120, 0);

	/**
	 * Retourne la liste immuable des compétences d'évaluation découvertes.
	 */
	public static List<SkillDescr> getAvailableSkills() {
		return Collections.unmodifiableList(availableSkills);
	}

	/**
	 * Scanne le dossier de compétences de l'utilisateur (~/codelab.files/skills).
	 * Réplique automatiquement les compétences d'exemples si le dossier utilisateur est initialisé.
	 */
	public static synchronized void scanSkills() {
		availableSkills.clear();
		File userSkillsFolder = new File(CodeLab.SKILLS_FOLDER);

		// Emplacement des compétences du projet / distribution
		File projectSkills = new File(CodeLab.BASE, "codelab.files/skills");
		if (!projectSkills.exists()) {
			projectSkills = new File(CodeLab.BASE, "client/codelab.files/skills");
		}
		if (!projectSkills.exists()) {
			projectSkills = new File("client/codelab.files/skills");
		}

		if (!userSkillsFolder.exists()) {
			try {
				userSkillsFolder.mkdirs();
			} catch (Exception ignored) {}
		}

		// Initialisation / réplication des exemples et du runner vers ~/codelab.files/skills
		if (projectSkills.exists() && projectSkills.isDirectory() && userSkillsFolder.exists()) {
			try {
				File projScript = new File(projectSkills, "eval_codelab.py");
				File userScript = new File(userSkillsFolder, "eval_codelab.py");
				if (projScript.exists() && (!userScript.exists() || projScript.lastModified() > userScript.lastModified())) {
					FileUtils.copyFile(projScript, userScript);
					userScript.setExecutable(true);
				}

				File[] psubs = projectSkills.listFiles();
				if (psubs != null) {
					for (File ps : psubs) {
						if (ps.isDirectory() && new File(ps, "SKILL.md").exists()) {
							File dest = new File(userSkillsFolder, ps.getName());
							if (!dest.exists()) {
								FileUtils.copyDirectory(ps, dest);
							} else if (ps.getName().endsWith("-sample")) {
								// Mise à jour automatique des compétences d'exemples si la distribution est plus récente
								File projAudit = new File(ps, "scripts/audit_student.py");
								File userAudit = new File(dest, "scripts/audit_student.py");
								if (projAudit.exists() && (!userAudit.exists() || projAudit.lastModified() > userAudit.lastModified())) {
									FileUtils.copyDirectory(ps, dest);
								}
							}
						}
					}
				}
			} catch (Exception ignored) {}
		}

		System.out.println("   Scanning skills directory... (" + userSkillsFolder.getAbsolutePath() + ")");
		java.util.Map<String, SkillDescr> discovered = new java.util.LinkedHashMap<>();

		// 1. Scanner le dossier utilisateur
		scanDirectory(userSkillsFolder, discovered);

		// 1b. Scanner HAL/skills si présent
		File halSkills = new File(System.getProperty("user.home"), "HAL/skills");
		if (halSkills.exists() && halSkills.isDirectory()) {
			scanDirectory(halSkills, discovered);
		}

		// 1c. Scanner Desktop/skills si présent
		File desktopSkills = new File(System.getProperty("user.home"), "Desktop/skills");
		if (desktopSkills.exists() && desktopSkills.isDirectory()) {
			scanDirectory(desktopSkills, discovered);
		}

		// 2. Si le dossier projet est accessible, compléter avec les compétences d'exemples
		if (projectSkills.exists() && projectSkills.isDirectory()) {
			scanDirectory(projectSkills, discovered);
		}

		availableSkills.addAll(discovered.values());

		if (availableSkills.isEmpty()) {
			System.out.println("      No skills found.");
		} else {
			for (SkillDescr sd : availableSkills) {
				System.out.format("      - %s [%s]\n", sd.getTitle(), sd.getId());
			}
			System.out.format("   %d skill(s) registered.\n", availableSkills.size());
		}
	}

	private static void scanDirectory(File folder, java.util.Map<String, SkillDescr> map) {
		if (folder == null || !folder.exists() || !folder.isDirectory()) return;
		File[] subs = folder.listFiles();
		if (subs == null) return;
		Arrays.sort(subs);
		for (File sub : subs) {
			if (sub.isDirectory() && !sub.getName().startsWith(".") && !sub.getName().equals("__pycache__")) {
				File skillMd = new File(sub, "SKILL.md");
				if (skillMd.exists() && !map.containsKey(sub.getName())) {
					SkillDescr sd = SkillDescr.fromDirectory(sub);
					if (sd != null && !map.containsKey(sd.getId())) {
						map.put(sd.getId(), sd);
					}
				}
			}
		}
	}

	/**
	 * Remplit dynamiquement un JMenu avec tous les skills disponibles + détection automatique
	 * (compatibilité Consumer simple : mode avec IA par défaut).
	 */
	public static void populateSkillsSubmenu(JMenu parentMenu, Consumer<SkillDescr> onSelect) {
		populateSkillsSubmenu(parentMenu, (skill, useAi) -> onSelect.accept(skill));
	}

	/**
	 * Remplit dynamiquement un JMenu avec les deux modes d'évaluation :
	 * 1) Mode Déterministe (Sans IA — 0 crédit)
	 * 2) Mode Hybride IA (Avec Gemini)
	 */
	public static void populateSkillsSubmenu(JMenu parentMenu, BiConsumer<SkillDescr, Boolean> onSelect) {
		parentMenu.removeAll();
		if (availableSkills.isEmpty()) {
			scanSkills();
		}
		if (availableSkills.isEmpty()) {
			JMenuItem empty = new JMenuItem(CodeLab.LABEL("evalNoSkills"));
			empty.setEnabled(false);
			parentMenu.add(empty);
			return;
		}

		String suffixNoAi = " " + CodeLab.LABEL("evalSkillSuffixNoAi");
		String suffixAi = " " + CodeLab.LABEL("evalSkillSuffixAi");

		// 1. Section Sans IA (Déterministe — 0 crédit)
		JMenuItem itemAutoNoAi = new JMenuItem(CodeLab.LABEL("evalSkillAutoNoAi"));
		itemAutoNoAi.addActionListener(e -> onSelect.accept(null, false));
		parentMenu.add(itemAutoNoAi);

		for (SkillDescr skill : availableSkills) {
			JMenuItem item = new JMenuItem("⚡ " + skill.getTitle() + suffixNoAi);
			item.setToolTipText(skill.getDescription() + " [Mode déterministe local — 0 crédit IA]");
			item.addActionListener(e -> onSelect.accept(skill, false));
			parentMenu.add(item);
		}

		parentMenu.add(new JSeparator());

		// 2. Section Avec IA (Hybride Gemini)
		JMenuItem itemAutoAi = new JMenuItem(CodeLab.LABEL("evalSkillAutoAi"));
		itemAutoAi.addActionListener(e -> onSelect.accept(null, true));
		parentMenu.add(itemAutoAi);

		for (SkillDescr skill : availableSkills) {
			JMenuItem item = new JMenuItem("🤖 " + skill.getTitle() + suffixAi);
			item.setToolTipText(skill.getDescription() + " [Mode hybride avec synthèse narrative IA Gemini]");
			item.addActionListener(e -> onSelect.accept(skill, true));
			parentMenu.add(item);
		}
	}

	/**
	 * Localise l'exécutable Python à utiliser.
	 */
	public static String getPythonCommand() {
		String cmd = CodeLab.PROP("PYTHON_CMD");
		if (cmd != null && !cmd.isBlank() && !cmd.equals(CodeLab.UNDEFINED)) {
			return cmd;
		}
		return "python3";
	}

	/**
	 * Localise le script eval_codelab.py dans codelab.files/skills/.
	 */
	public static File getEvaluatorScript() {
		File script = new File(CodeLab.SKILLS_FOLDER, "eval_codelab.py");
		if (script.exists()) return script;

		File homeScript = new File(System.getProperty("user.home") + "/codelab.files/skills/eval_codelab.py");
		if (homeScript.exists()) return homeScript;

		File baseScript = new File(CodeLab.BASE, "codelab.files/skills/eval_codelab.py");
		if (baseScript.exists()) return baseScript;

		return script;
	}

	/**
	 * Évalue un dossier unique d'étudiant (depuis l'explorateur FileManager) en mode auto.
	 */
	public static void evaluateFolder(CodeLab codelab, File folder) {
		evaluateFolder(codelab, folder, null);
	}

	/**
	/**
	 * Recherche le nom complet d'un étudiant à partir de son login (dans UserTable ou CodelabClient).
	 */
	public static String lookupStudentName(CodeLab codelab, String studentLogin) {
		if (codelab == null || studentLogin == null || studentLogin.isBlank()) return null;
		try {
			if (codelab.getEditor() != null && codelab.getEditor().getUserTable() != null) {
				codelab.client.UserData udata = codelab.getEditor().getUserTable().findUserData(studentLogin);
				if (udata != null && udata.getName() != null && !udata.getName().isBlank()) {
					return udata.getName();
				}
			}
			if (codelab.getClient() != null && codelab.getClient().getUserDataArray() != null) {
				for (codelab.client.UserData u : codelab.getClient().getUserDataArray()) {
					if (studentLogin.equals(u.getLogin()) && u.getName() != null && !u.getName().isBlank()) {
						return u.getName();
					}
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	/**
	 * Écrit un fichier sessions.xml standard dans le dossier de session pour assurer la persistance
	 * des noms des étudiants pour les outils externes et scripts d'évaluation.
	 */
	public static void ensureSessionsXml(File sessionDir, String sessionId, CodeLab codelab) {
		if (sessionDir == null || !sessionDir.exists() || codelab == null || codelab.getClient() == null) return;
		File target = new File(sessionDir, "sessions.xml");
		if (target.exists()) return;
		codelab.client.UserData[] users = codelab.getClient().getUserDataArray();
		if (users == null || users.length == 0) return;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sb.append("<session id=\"").append(sessionId != null ? escapeXml(sessionId) : "session").append("\">\n");
			sb.append("  <users>\n");
			for (codelab.client.UserData u : users) {
				if (u == null || u.getLogin() == null) continue;
				sb.append("    <user login=\"").append(escapeXml(u.getLogin())).append("\"");
				if (u.getName() != null) sb.append(" name=\"").append(escapeXml(u.getName())).append("\"");
				if (u.getMail() != null) sb.append(" mail=\"").append(escapeXml(u.getMail())).append("\"");
				if (u.getGroup() != null) sb.append(" groups=\"").append(escapeXml(u.getGroup())).append("\"");
				if (u.isTutor()) sb.append(" role=\"tutor\"");
				else if (u.isStudent()) sb.append(" role=\"student\"");
				sb.append(" />\n");
			}
			sb.append("  </users>\n");
			sb.append("</session>\n");
			FileUtils.writeStringToFile(target, sb.toString(), StandardCharsets.UTF_8);
		} catch (Exception ignored) {}
	}

	private static String escapeXml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Évalue un dossier unique d'étudiant (depuis l'explorateur FileManager) avec un skill spécifique.
	 */
	public static void evaluateFolder(CodeLab codelab, File folder, SkillDescr skill) {
		evaluateFolder(codelab, folder, skill, true);
	}

	/**
	 * Évalue un dossier unique d'étudiant (depuis l'explorateur FileManager) avec un skill spécifique et choix du mode IA.
	 */
	public static void evaluateFolder(CodeLab codelab, File folder, SkillDescr skill, boolean useAi) {
		if (folder == null || !folder.exists() || !folder.isDirectory()) {
			Utils.showWarningDialog("CodeLab", "Dossier invalide ou inexistant.");
			return;
		}

		String sessionId = (codelab.isConnected() && codelab.getClient() != null && codelab.getClient().getSession() != null)
				? codelab.getClient().getSession() : "local";

		File evalBaseDir;
		String studentLogin = null;
		String studentName = null;
		if (codelab.isConnected()) {
			File sessionDir = new File(codelab.getProgramDir());
			ensureSessionsXml(sessionDir, sessionId, codelab);
			try {
				java.nio.file.Path rel = sessionDir.toPath().toAbsolutePath().normalize().relativize(folder.toPath().toAbsolutePath().normalize());
				if (rel.getNameCount() >= 1) {
					studentLogin = rel.getName(0).toString();
				}
			} catch (Exception ignored) {}

			if (studentLogin != null && !studentLogin.isEmpty() && !studentLogin.equals("evaluations")) {
				evalBaseDir = new File(new File(CodeLab.EVALUATIONS_FOLDER, sessionId), studentLogin);
				studentName = lookupStudentName(codelab, studentLogin);
			} else {
				evalBaseDir = new File(CodeLab.EVALUATIONS_FOLDER, sessionId);
			}
		} else {
			evalBaseDir = new File(CodeLab.EVALUATIONS_FOLDER, "local");
		}
		evalBaseDir.mkdirs();
		File reportFile = new File(evalBaseDir, folder.getName() + "_rapport.md");

		List<String> args = new ArrayList<>();
		args.add("--folder");
		args.add(folder.getAbsolutePath());
		args.add("--out");
		args.add(reportFile.getAbsolutePath());

		if (!useAi) {
			args.add("--no-ai");
		}

		if (codelab.isConnected()) {
			args.add("--dir");
			args.add(codelab.getProgramDir());
			File sessionDir = new File(codelab.getProgramDir());
			File sessionsXml = new File(sessionDir, "sessions.xml");
			if (sessionsXml.exists()) {
				args.add("--sessions");
				args.add(sessionsXml.getAbsolutePath());
			}
		}

		if (studentLogin != null && !studentLogin.isEmpty() && !studentLogin.equals("evaluations")) {
			args.add("--student");
			args.add(studentLogin);
		}

		if (studentName != null && !studentName.isBlank()) {
			args.add("--name");
			args.add(studentName);
		}

		if (skill != null) {
			args.add("--skill");
			args.add(skill.getId());
		}

		String modeLabel = useAi ? " [Avec IA]" : " [Sans IA]";
		String skillLabel = (skill != null) ? " (" + skill.getTitle() + ")" : "";
		String who = (studentName != null && !studentName.isBlank()) ? studentName + " (" + folder.getName() + ")" : folder.getName();
		String waitingMsg = String.format("Évaluation%s de %s%s...", modeLabel, who, skillLabel);
		runEvaluation(codelab, args, waitingMsg);
	}

	/**
	 * Évalue un étudiant spécifique au sein de la session active (depuis UserTable) en mode auto.
	 */
	public static void evaluateStudent(CodeLab codelab, String studentLogin, String studentName) {
		evaluateStudent(codelab, studentLogin, studentName, null, true);
	}

	/**
	 * Évalue un étudiant spécifique au sein de la session active (depuis UserTable) avec un skill donné.
	 */
	public static void evaluateStudent(CodeLab codelab, String studentLogin, String studentName, SkillDescr skill) {
		evaluateStudent(codelab, studentLogin, studentName, skill, true);
	}

	/**
	 * Évalue un étudiant spécifique au sein de la session active (depuis UserTable) avec un skill donné et choix du mode IA.
	 */
	public static void evaluateStudent(CodeLab codelab, String studentLogin, String studentName, SkillDescr skill, boolean useAi) {
		if (studentLogin == null || studentLogin.isBlank()) return;

		File sessionDir = new File(codelab.getProgramDir());
		File studentDir = new File(sessionDir, studentLogin);

		if (!studentDir.exists()) {
			Utils.showWarningDialog("CodeLab", String.format("Dossier de l'étudiant introuvable : %s", studentDir.getAbsolutePath()));
			return;
		}

		String sessionId = (codelab.isConnected() && codelab.getClient() != null && codelab.getClient().getSession() != null)
				? codelab.getClient().getSession() : "local";
		ensureSessionsXml(sessionDir, sessionId, codelab);
		File sessionEvalDir = new File(CodeLab.EVALUATIONS_FOLDER, sessionId);
		File studentEvalDir = new File(sessionEvalDir, studentLogin);
		studentEvalDir.mkdirs();
		File reportFile = new File(studentEvalDir, studentLogin + "_rapport.md");

		List<String> args = new ArrayList<>();
		args.add("--student");
		args.add(studentLogin);
		if (studentName != null && !studentName.isBlank()) {
			args.add("--name");
			args.add(studentName);
		}
		args.add("--dir");
		args.add(sessionDir.getAbsolutePath());
		args.add("--eval-dir");
		args.add(sessionEvalDir.getAbsolutePath());
		args.add("--out");
		args.add(reportFile.getAbsolutePath());

		if (!useAi) {
			args.add("--no-ai");
		}

		File sessionsXml = new File(sessionDir, "sessions.xml");
		if (sessionsXml.exists()) {
			args.add("--sessions");
			args.add(sessionsXml.getAbsolutePath());
		}

		if (skill != null) {
			args.add("--skill");
			args.add(skill.getId());
		}

		String modeLabel = useAi ? " [Avec IA]" : " [Sans IA]";
		String skillLabel = (skill != null) ? " (" + skill.getTitle() + ")" : "";
		String waitingMsg = String.format("Évaluation%s de %s (%s)%s...", modeLabel, studentName, studentLogin, skillLabel);
		runEvaluation(codelab, args, waitingMsg);
	}

	/**
	 * Évalue toute la session active de la promotion (depuis UserTable) en mode auto.
	 */
	public static void evaluateSession(CodeLab codelab, String sessionId) {
		evaluateSession(codelab, sessionId, null, true);
	}

	/**
	 * Évalue toute la session active de la promotion (depuis UserTable) avec un skill donné.
	 */
	public static void evaluateSession(CodeLab codelab, String sessionId, SkillDescr skill) {
		evaluateSession(codelab, sessionId, skill, true);
	}

	/**
	 * Évalue toute la session active de la promotion (depuis UserTable) avec un skill donné et choix du mode IA.
	 */
	public static void evaluateSession(CodeLab codelab, String sessionId, SkillDescr skill, boolean useAi) {
		File sessionDir = new File(codelab.getProgramDir());
		if (!sessionDir.exists()) {
			Utils.showWarningDialog("CodeLab", "Répertoire de session introuvable.");
			return;
		}

		String sessId = (sessionId != null && !sessionId.isBlank()) ? sessionId :
				((codelab.getClient() != null && codelab.getClient().getSession() != null) ? codelab.getClient().getSession() : "session");
		ensureSessionsXml(sessionDir, sessId, codelab);
		File sessionEvalDir = new File(CodeLab.EVALUATIONS_FOLDER, sessId);
		sessionEvalDir.mkdirs();

		List<String> args = new ArrayList<>();
		args.add("--session");
		args.add(sessId);
		args.add("--dir");
		args.add(sessionDir.getAbsolutePath());
		args.add("--eval-dir");
		args.add(sessionEvalDir.getAbsolutePath());

		if (!useAi) {
			args.add("--no-ai");
		}

		File sessionsXml = new File(sessionDir, "sessions.xml");
		if (sessionsXml.exists()) {
			args.add("--sessions");
			args.add(sessionsXml.getAbsolutePath());
		}

		if (skill != null) {
			args.add("--skill");
			args.add(skill.getId());
		}

		String modeLabel = useAi ? " [Avec IA]" : " [Sans IA]";
		String skillLabel = (skill != null) ? " (" + skill.getTitle() + ")" : "";
		String waitingMsg = String.format("Évaluation complète%s de la session %s%s...", modeLabel, sessId, skillLabel);
		runEvaluation(codelab, args, waitingMsg);
	}


	/**
	 * Exécute l'audit en tâche de fond pour préserver la réactivité de l'EDT Swing.
	 */
	private static void runEvaluation(CodeLab codelab, List<String> evalArgs, String waitingMessage) {
		File script = getEvaluatorScript();
		if (!script.exists()) {
			Utils.showWarningDialog("CodeLab", "Script d'évaluation introuvable dans :\n" + script.getAbsolutePath());
			return;
		}

		String pythonCmd = getPythonCommand();

		// Afficher immédiatement la console CodeLab et les informations de départ
		codelab.showConsole();
		codelab.printlnToConsole("[codelab] " + waitingMessage, Console.COLOR_LOG);

		final java.util.concurrent.atomic.AtomicReference<Process> procRef = new java.util.concurrent.atomic.AtomicReference<>();
		final java.util.concurrent.atomic.AtomicReference<SwingWorker<String, String>> workerRef = new java.util.concurrent.atomic.AtomicReference<>();

		Runnable cancelAction = () -> {
			codelab.printlnToConsole("[codelab] Demande d'arrêt de l'évaluation reçue...", COLOR_WARNING);
			SwingWorker<String, String> w = workerRef.get();
			if (w != null) {
				w.cancel(true);
			}
			Process p = procRef.get();
			if (p != null && p.isAlive()) {
				try {
					p.descendants().forEach(ph -> {
						try { ph.destroyForcibly(); } catch (Exception ignored) {}
					});
					p.destroyForcibly();
				} catch (Exception ignored) {}
			}
		};

		WaitingDialog.open(waitingMessage, cancelAction);

		SwingWorker<String, String> worker = new SwingWorker<String, String>() {
			private String errorMessage = null;

			@Override
			protected String doInBackground() throws Exception {
				List<String> command = new ArrayList<>();
				command.add(pythonCmd);
				command.add("-u"); // Mode non-bufférisé pour réception immédiate des flux
				command.add(script.getAbsolutePath());
				command.addAll(evalArgs);

				ProcessBuilder pb = new ProcessBuilder(command);
				pb.redirectErrorStream(true);
				pb.environment().put("PYTHONUNBUFFERED", "1");
				if (CodeLab.JAVA_HOME != null && !CodeLab.JAVA_HOME.isBlank() && !CodeLab.JAVA_HOME.equals("undefined")) {
					pb.environment().put("JAVA_HOME", CodeLab.JAVA_HOME);
				}
				if (CodeLab.JAVAC_CMD != null && !CodeLab.JAVAC_CMD.isBlank() && !CodeLab.JAVAC_CMD.equals("undefined")) {
					pb.environment().put("JAVAC_CMD", CodeLab.JAVAC_CMD);
				}

				// Transmission transparente du proxy configuré dans CodeLab
				if (CodeLab.PROXY_CONFIGURED && CodeLab.PROXY_HOST != null && !CodeLab.PROXY_HOST.isBlank() && !CodeLab.PROXY_HOST.equalsIgnoreCase("NO_PROXY")) {
					String pPort = (CodeLab.PROXY_PORT != null && !CodeLab.PROXY_PORT.isBlank()) ? CodeLab.PROXY_PORT : "3128";
					String proxyUrl = "http://" + CodeLab.PROXY_HOST + ":" + pPort;
					pb.environment().put("HTTP_PROXY", proxyUrl);
					pb.environment().put("HTTPS_PROXY", proxyUrl);
					pb.environment().put("http_proxy", proxyUrl);
					pb.environment().put("https_proxy", proxyUrl);
					pb.environment().put("PROXY_HOST", CodeLab.PROXY_HOST);
					pb.environment().put("PROXY_PORT", pPort);
					pb.environment().put("PROXY_ENABLED", "yes");
				} else {
					pb.environment().put("PROXY_HOST", "NO_PROXY");
					pb.environment().put("PROXY_ENABLED", "no");
					pb.environment().remove("HTTP_PROXY");
					pb.environment().remove("HTTPS_PROXY");
					pb.environment().remove("http_proxy");
					pb.environment().remove("https_proxy");
				}

				// Définir le répertoire de travail
				pb.directory(script.getParentFile());

				Process proc = pb.start();
				procRef.set(proc);
				String reportFilePath = null;
				StringBuilder logs = new StringBuilder();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (isCancelled()) {
							try {
								proc.descendants().forEach(ph -> {
									try { ph.destroyForcibly(); } catch (Exception ignored) {}
								});
								proc.destroyForcibly();
							} catch (Exception ignored) {}
							return null;
						}
						logs.append(line).append("\n");
						publish(line); // Affichage en temps réel dans la console
						if (line.startsWith("REPORT_FILE:")) {
							reportFilePath = line.substring("REPORT_FILE:".length()).trim();
						}
					}
				} catch (Exception e) {
					if (isCancelled()) return null;
					throw e;
				}

				if (isCancelled()) {
					return null;
				}

				int exitCode = proc.waitFor();
				if (isCancelled()) {
					return null;
				}
				if (exitCode != 0) {
					errorMessage = "Erreur d'exécution de l'évaluation (code " + exitCode + ") :\n" + logs.toString();
					return null;
				}

				return reportFilePath;
			}

			@Override
			protected void process(List<String> chunks) {
				for (String line : chunks) {
					if (line.startsWith("REPORT_FILE:")) continue;
					Color color;
					if (line.startsWith("[FAIL]") || line.startsWith("[ERROR]")) color = Console.COLOR_ERROR;
					else if (line.startsWith("[WARN]")) color = new Color(220, 120, 0);
					else if (line.startsWith("[DONE]") || line.startsWith("[OK]")) color = new Color(34, 139, 34);
					else if (line.startsWith("[PROGRESS]")) color = new Color(0, 140, 200);
					else if (line.startsWith("[INFO]")) color = new Color(0, 102, 204);
					else color = Console.COLOR_PRINT;

					codelab.printlnToConsole(line, color);

					// Mettre à jour dynamiquement la progression et le texte du dialogue d'attente
					if (line.startsWith("[PROGRESS]")) {
						String payload = line.substring("[PROGRESS]".length()).trim();
						int spaceIdx = payload.indexOf(' ');
						String ratio = (spaceIdx != -1) ? payload.substring(0, spaceIdx) : payload;
						String msg = (spaceIdx != -1) ? payload.substring(spaceIdx + 1).trim() : "";
						String[] parts = ratio.split("/");
						if (parts.length == 2) {
							try {
								int cur = Integer.parseInt(parts[0].trim());
								int tot = Integer.parseInt(parts[1].trim());
								WaitingDialog.setProgress(cur, tot);
							} catch (NumberFormatException ignored) {}
						}
						if (!msg.isEmpty()) {
							if (msg.length() > 48) msg = msg.substring(0, 45) + "...";
							WaitingDialog.setText(msg);
						}
					} else if (line.startsWith("[INFO]") || line.startsWith("[DONE]") || line.startsWith("[OK]")) {
						String clean = line.replaceFirst("^\\[(INFO|DONE|OK)\\]\\s*", "");
						if (clean.length() > 48) clean = clean.substring(0, 45) + "...";
						WaitingDialog.setText(clean);
					}
				}
			}

			@Override
			protected void done() {
				WaitingDialog.close();
				if (isCancelled()) {
					codelab.printlnToConsole("[codelab] Évaluation arrêtée par l'utilisateur.", COLOR_WARNING);
					return;
				}
				try {
					String reportPath = get();
					if (reportPath != null) {
						File reportFile = new File(reportPath);
						if (reportFile.exists()) {
							codelab.printlnToConsole("[codelab] Rapport généré avec succès : " + reportFile.getName());
							
							// Basculer l'explorateur sur la vue Évaluations et sélectionner le fichier
							if (codelab.getEditor() != null) {
								codelab.getEditor().setExplorerMode(FileManager.MODE_EVALUATIONS);
								boolean selected = false;
								if (codelab.getEditor().getManager() != null) {
									codelab.getEditor().getManager().populate();
									selected = codelab.getEditor().getManager().selectNodeFromFile(reportFile);
								}
								// Si la sélection automatique n'a pas déclenché l'ouverture, ouvrir directement
								if (!selected) {
									codelab.getEditor().open_file(reportFile, false);
								}
								codelab.showEditor();
							}
						} else {
							codelab.printlnToConsole("[IA Eval] Rapport généré introuvable : " + reportPath, Console.COLOR_ERROR);
							Utils.showWarningDialog("CodeLab", "Rapport généré introuvable :\n" + reportPath);
						}
					} else if (errorMessage != null) {
						codelab.printlnToConsole(errorMessage, Console.COLOR_ERROR);
						Utils.showWarningDialog("CodeLab", errorMessage);
					}
				} catch (Exception e) {
					codelab.printlnToConsole("[IA Eval] Exception : " + e.getMessage(), Console.COLOR_ERROR);
					Utils.showWarningDialog("CodeLab", "Exception lors de l'évaluation : " + e.getMessage());
				}
			}
		};

		workerRef.set(worker);
		worker.execute();
	}

	/**
	 * Localise ou initialise le fichier weights.json des pondérations.
	 */
	public static File getWeightsFile(String skillName) {
		if (skillName != null && !skillName.isBlank()) {
			File skillDir = new File(CodeLab.SKILLS_FOLDER, skillName);
			File w = new File(skillDir, "weights.json");
			if (w.exists()) return w;
		}

		File defaultSampleWeights = new File(new File(CodeLab.SKILLS_FOLDER, "eval-c-sample"), "weights.json");
		if (defaultSampleWeights.exists()) return defaultSampleWeights;

		File globalWeights = new File(CodeLab.SKILLS_FOLDER, "weights.json");
		if (globalWeights.exists()) return globalWeights;

		File projectSampleWeights = new File(CodeLab.BASE, "client/codelab.files/skills/eval-c-sample/weights.json");
		if (projectSampleWeights.exists()) return projectSampleWeights;

		return defaultSampleWeights;
	}

	/**
	 * Ouvre directement le fichier weights.json dans l'éditeur de CodeLab.
	 */
	public static void openWeightsFile(CodeLab codelab, String skillName) {
		File wFile = getWeightsFile(skillName);
		if (!wFile.exists()) {
			try {
				if (wFile.getParentFile() != null) wFile.getParentFile().mkdirs();
				String defaultJson = "{\n"
					+ "  \"version\": \"1.0\",\n"
					+ "  \"description\": \"Pondérations et seuils de notation CodeLab\",\n"
					+ "  \"axes\": {\n"
					+ "    \"axe1_progression_max\": 8.0,\n"
					+ "    \"axe2_architecture_max\": 8.0,\n"
					+ "    \"axe3_style_max\": 2.0,\n"
					+ "    \"axe4_tests_max\": 2.0\n"
					+ "  },\n"
					+ "  \"anti_triche\": {\n"
					+ "    \"assistance_ia\": {\n"
					+ "      \"malus_eleve\": 2.0,\n"
					+ "      \"plafond_note_eleve\": 13.5,\n"
					+ "      \"malus_modere\": 1.0,\n"
					+ "      \"plafond_note_modere\": 15.5\n"
					+ "    },\n"
					+ "    \"similarite\": {\n"
					+ "      \"seuil_detection_pct\": 85.0,\n"
					+ "      \"malus_1_fichier\": 0.5,\n"
					+ "      \"malus_2_fichiers\": 1.5,\n"
					+ "      \"malus_3_fichiers_ou_plus\": 2.5,\n"
					+ "      \"malus_max_total\": 2.5\n"
					+ "    }\n"
					+ "  },\n"
					+ "  \"profils\": {\n"
					+ "    \"note_min_excellence\": 16.0,\n"
					+ "    \"interdire_excellence_si_ia\": true,\n"
					+ "    \"interdire_excellence_si_similarite\": true\n"
					+ "  }\n"
					+ "}\n";
				FileUtils.writeStringToFile(wFile, defaultJson, StandardCharsets.UTF_8);
			} catch (Exception ignored) {}
		}

		if (codelab != null && codelab.getEditor() != null) {
			codelab.getEditor().open_file(wFile, false);
			codelab.showEditor();
			codelab.printlnToConsole("[codelab] Fichier de pondérations ouvert : " + wFile.getAbsolutePath());
		}
	}

	/**
	 * Localise le script recalculate_grades.py.
	 */
	public static File getRecalculateScript() {
		File script1 = new File(CodeLab.SKILLS_FOLDER, "recalculate_grades.py");
		if (script1.exists()) return script1;

		File script2 = new File(CodeLab.BASE, "client/codelab.files/skills/recalculate_grades.py");
		if (script2.exists()) return script2;

		return script1;
	}

	/**
	 * Résout le dossier de session d'évaluation même si target est null ou pointe sur un fichier/sous-dossier.
	 */
	public static File resolveSessionDirectory(CodeLab codelab, File target) {
		File evalRoot = new File(CodeLab.EVALUATIONS_FOLDER);
		if (!evalRoot.exists()) return null;

		// 1. Si une cible précise est fournie
		if (target != null && target.exists()) {
			try {
				String evalRootPath = evalRoot.getCanonicalPath();
				File curr = target.isFile() ? target.getParentFile() : target;
				String currPath = curr.getCanonicalPath();
				
				// Si ce n'est pas la racine globale des évaluations
				if (!currPath.equals(evalRootPath)) {
					// Remonter jusqu'au dossier enfant direct de evalRoot (la session)
					while (curr.getParentFile() != null && !curr.getParentFile().getCanonicalPath().equals(evalRootPath)) {
						curr = curr.getParentFile();
					}
					if (curr.getParentFile() != null && curr.getParentFile().getCanonicalPath().equals(evalRootPath)) {
						return curr;
					}
				}
			} catch (Exception ignored) {}
		}

		// 2. Si un fichier d'évaluation est actuellement ouvert dans l'éditeur
		if (codelab != null && codelab.getEditor() != null) {
			File currentFile = codelab.getEditor().getCurrentFile();
			if (currentFile != null && currentFile.exists()) {
				try {
					String evalRootPath = evalRoot.getCanonicalPath();
					File curr = currentFile.getParentFile();
					while (curr != null && curr.getParentFile() != null) {
						if (curr.getParentFile().getCanonicalPath().equals(evalRootPath)) {
							return curr;
						}
						curr = curr.getParentFile();
					}
				} catch (Exception ignored) {}
			}
		}

		// 3. Chercher les sessions présentes dans le dossier evaluations
		File[] subDirs = evalRoot.listFiles(File::isDirectory);
		if (subDirs == null || subDirs.length == 0) {
			return null;
		}
		if (subDirs.length == 1) {
			return subDirs[0];
		}

		// 4. Si plusieurs sessions existent, proposer le choix à l'utilisateur
		String[] names = new String[subDirs.length];
		for (int i = 0; i < subDirs.length; i++) {
			names[i] = subDirs[i].getName();
		}
		java.util.Arrays.sort(names);

		Object choice = JOptionPane.showInputDialog(
			CodeLab.FRAME,
			"Sélectionnez la session d'évaluation à recalculer :",
			"Recalculer les notes de la session",
			JOptionPane.QUESTION_MESSAGE,
			null,
			names,
			names[0]
		);
		if (choice == null) return null; // Annulé par l'utilisateur

		return new File(evalRoot, choice.toString());
	}

	/**
	 * Recalcule instantanément toutes les notes d'une session à partir de weights.json sans Clang ni LLM.
	 */
	public static void recalculateSessionGrades(CodeLab codelab, File sessionDir) {
		File actualSessionDir = resolveSessionDirectory(codelab, sessionDir);
		if (actualSessionDir == null || !actualSessionDir.exists()) {
			File evalRoot = new File(CodeLab.EVALUATIONS_FOLDER);
			File[] subDirs = evalRoot.exists() ? evalRoot.listFiles(File::isDirectory) : null;
			if (subDirs == null || subDirs.length == 0) {
				Utils.showWarningDialog("CodeLab", "Aucune session d'évaluation trouvée dans :\n" + evalRoot.getAbsolutePath());
			}
			return;
		}

		File script = getRecalculateScript();
		if (!script.exists()) {
			Utils.showWarningDialog("CodeLab", "Script de recalcul introuvable :\n" + script.getAbsolutePath());
			return;
		}

		File weightsFile = getWeightsFile(null);
		String pythonCmd = getPythonCommand();

		codelab.showConsole();
		codelab.printlnToConsole("[codelab] ⚖️ Recalcul des notes de la session : " + actualSessionDir.getName() + "...", Console.COLOR_LOG);

		final File finalSessionDir = actualSessionDir;
		final java.util.concurrent.atomic.AtomicReference<Process> procRef = new java.util.concurrent.atomic.AtomicReference<>();
		final java.util.concurrent.atomic.AtomicReference<SwingWorker<String, String>> workerRef = new java.util.concurrent.atomic.AtomicReference<>();

		Runnable cancelAction = () -> {
			codelab.printlnToConsole("[codelab] Demande d'arrêt du recalcul reçue...", COLOR_WARNING);
			SwingWorker<String, String> w = workerRef.get();
			if (w != null) w.cancel(true);
			Process p = procRef.get();
			if (p != null && p.isAlive()) {
				try { p.destroyForcibly(); } catch (Exception ignored) {}
			}
		};

		WaitingDialog.open("Recalcul des notes (" + actualSessionDir.getName() + ")...", cancelAction);

		SwingWorker<String, String> worker = new SwingWorker<String, String>() {
			private String summaryResult = null;
			private String errorMessage = null;

			@Override
			protected String doInBackground() throws Exception {
				List<String> command = new ArrayList<>();
				command.add(pythonCmd);
				command.add("-u");
				command.add(script.getAbsolutePath());
				command.add(finalSessionDir.getAbsolutePath());
				if (weightsFile.exists()) {
					command.add("--weights");
					command.add(weightsFile.getAbsolutePath());
				}

				ProcessBuilder pb = new ProcessBuilder(command);
				pb.redirectErrorStream(true);
				pb.environment().put("PYTHONUNBUFFERED", "1");
				pb.directory(script.getParentFile() != null ? script.getParentFile() : finalSessionDir);

				Process proc = pb.start();
				procRef.set(proc);
				StringBuilder logs = new StringBuilder();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (isCancelled()) {
							proc.destroyForcibly();
							return null;
						}
						logs.append(line).append("\n");
						publish(line);
						if (line.startsWith("RECALCULATE_RESULT:")) {
							summaryResult = line.substring("RECALCULATE_RESULT:".length()).trim();
						}
					}
				}

				int code = proc.waitFor();
				if (code != 0) {
					errorMessage = "Erreur lors du recalcul des notes (code " + code + ") :\n" + logs.toString();
					return null;
				}
				return summaryResult;
			}

			@Override
			protected void process(List<String> chunks) {
				for (String line : chunks) {
					if (line.startsWith("RECALCULATE_RESULT:")) continue;
					Color color = Console.COLOR_PRINT;
					if (line.startsWith("✅") || line.startsWith("[DONE]") || line.startsWith("[OK]")) color = new Color(34, 139, 34);
					else if (line.startsWith("[WARN]")) color = new Color(220, 120, 0);
					else if (line.startsWith("[PROGRESS]")) color = new Color(0, 140, 200);
					else if (line.startsWith("[FAIL]") || line.startsWith("[ERREUR]") || line.startsWith("[ERROR]")) color = Console.COLOR_ERROR;
					else if (line.startsWith("[INFO]")) color = new Color(0, 102, 204);
					codelab.printlnToConsole(line, color);

					// Mettre à jour dynamiquement la progression et le texte du dialogue d'attente
					if (line.startsWith("[PROGRESS]")) {
						String payload = line.substring("[PROGRESS]".length()).trim();
						int spaceIdx = payload.indexOf(' ');
						String ratio = (spaceIdx != -1) ? payload.substring(0, spaceIdx) : payload;
						String msg = (spaceIdx != -1) ? payload.substring(spaceIdx + 1).trim() : "";
						String[] parts = ratio.split("/");
						if (parts.length == 2) {
							try {
								int cur = Integer.parseInt(parts[0].trim());
								int tot = Integer.parseInt(parts[1].trim());
								WaitingDialog.setProgress(cur, tot);
							} catch (NumberFormatException ignored) {}
						}
						if (!msg.isEmpty()) {
							if (msg.length() > 48) msg = msg.substring(0, 45) + "...";
							WaitingDialog.setText(msg);
						}
					} else if (line.startsWith("[INFO]") || line.startsWith("[DONE]") || line.startsWith("[OK]")) {
						String clean = line.replaceFirst("^\\[(INFO|DONE|OK)\\]\\s*", "");
						if (clean.length() > 48) clean = clean.substring(0, 45) + "...";
						WaitingDialog.setText(clean);
					}
				}
			}

			@Override
			protected void done() {
				WaitingDialog.close();
				if (isCancelled()) {
					codelab.printlnToConsole("[codelab] Recalcul annulé.", COLOR_WARNING);
					return;
				}
				try {
					String res = get();
					if (errorMessage != null) {
						Utils.showWarningDialog("CodeLab", errorMessage);
						return;
					}

					// Actualisation de l'arborescence et de l'éditeur
					if (codelab.getEditor() != null) {
						if (codelab.getEditor().getManager() != null) {
							codelab.getEditor().getManager().populate();
							codelab.getEditor().getManager().collapseAll();
						}
						codelab.getEditor().reloadCurrentFile();
					}

					String infoMsg = "Recalcul des notes terminé avec succès !\n\n"
						+ "Les fiches d'évaluation individuelles et le tableau CSV ont été mis à jour\n"
						+ "selon les pondérations définies dans 'weights.json'.";

					if (res != null && res.contains("|")) {
						java.util.Map<String, String> pMap = new java.util.HashMap<>();
						for (String part : res.split("\\|")) {
							String[] kv = part.split("=", 2);
							if (kv.length == 2) pMap.put(kv[0], kv[1]);
						}
						String count = pMap.getOrDefault("students", "?");
						String oldAvg = pMap.getOrDefault("old_avg", "?");
						String newAvg = pMap.getOrDefault("new_avg", "?");
						String median = pMap.getOrDefault("median", "?");
						infoMsg = String.format(
							"Recalcul des notes terminé avec succès !\n\n"
							+ "• Session : %s\n"
							+ "• Étudiants mis à jour : %s\n"
							+ "• Évolution moyenne promo : %s/20 ➔ %s/20\n"
							+ "• Nouvelle médiane : %s/20\n\n"
							+ "Les fiches Markdown et le fichier CSV ont été recalculés.",
							finalSessionDir.getName(), count, oldAvg, newAvg, median
						);
					}

					Utils.showMessageDialog("Recalcul des Notes", infoMsg);

				} catch (Exception ex) {
					codelab.printlnToConsole("[codelab] Erreur recalcul : " + ex.getMessage(), Console.COLOR_ERROR);
					Utils.showWarningDialog("CodeLab", "Erreur lors du recalcul : " + ex.getMessage());
				}
			}
		};

		workerRef.set(worker);
		worker.execute();
	}
}

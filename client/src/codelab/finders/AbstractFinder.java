package codelab.finders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import codelab.CodeLab;
import codelab.PropertyBase;
import codelab.console.Console;
import codelab.modules.editeur.AbstractEditor;
import codelab.utils.Utils;
import codelab.utils.WaitingDialog;

/**
 * Classe abstraite de base pour la détection et configuration d'outils
 * (compilateurs et interpréteurs) multiplateformes.
 * @author Gemini 3.8
 * @version 30/09/26
 */

public abstract class AbstractFinder {

	/**
	 * Description d'un outil trouvé (chemin + version)
	 */
	public static class ToolInfo {
		public final String path;
		public final String version;

		public ToolInfo(String path, String version) {
			this.path = path;
			this.version = version;
		}

		@Override
		public String toString() {
			return String.format("%s  (%s)", path, version);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes abstraites à implémenter par chaque Finder
	///////////////////////////////////////////////////

	public abstract String getPropertyKey();
	public abstract String getTitleLabelKey();
	public abstract String getWaitingLabelKey();
	public abstract String getFoundLabelKey();
	public abstract String getMultipleLabelKey();
	public abstract String getSuccessLabelKey();
	public abstract String getNotFoundLabelKey();
	public abstract String getAdvice();
	public abstract String getInvalidLabelKey();

	public abstract List<String> getBinNames();
	public abstract List<String> getVersionArgs();
	public abstract boolean isValidVersionOutput(String output);
	public abstract void scanKnownPaths(Set<String> candidates);
	public abstract void updateCodeLabCommand(String path);
	public abstract void recheckLanguage();
	public abstract String getCurrentConfiguredCommand();

	///////////////////////////////////////////////////
	// Normalisation et ajout d'exécutable
	///////////////////////////////////////////////////

	public static String normalizePath(String path) {
		if (path == null) return null;
		String clean = path.trim();
		if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
			clean = clean.substring(1, clean.length() - 1).trim();
		}
		return clean.replace('\\', '/');
	}

	protected void checkAndAddExecutable(Set<String> candidates, File file) {
		if (file == null) return;
		try {
			if (file.exists() && !file.isDirectory()) {
				if (CodeLab.IS_WINDOWS || file.canExecute()) {
					candidates.add(normalizePath(file.getAbsolutePath()));
				}
			}
		} catch (Exception ignored) {}
	}

	///////////////////////////////////////////////////
	// Détection globale
	///////////////////////////////////////////////////

	public List<ToolInfo> detect() {
		Set<String> candidatePaths = new LinkedHashSet<>();

		// 1. Commande courante si configurée
		String current = getCurrentConfiguredCommand();
		if (current != null && !current.isBlank()
				&& !current.equals("undefined")
				&& !current.equals("DISABLED")) {
			String norm = normalizePath(current);
			File f = new File(norm);
			if (f.exists()) candidatePaths.add(normalizePath(f.getAbsolutePath()));
			else candidatePaths.add(norm);
		}

		// 2. Commande système where / which
		for (String bin : getBinNames()) {
			if (CodeLab.IS_WINDOWS) {
				runWhereCommand(candidatePaths, bin);
			} else {
				runWhichCommand(candidatePaths, bin);
			}
		}

		// 3. Parcours direct du PATH Java
		scanPathEnv(candidatePaths);

		// 4. Emplacements standards et usuels selon l'OS
		scanKnownPaths(candidatePaths);

		// 5. Sondage de chaque candidat
		List<ToolInfo> validTools = new ArrayList<>();
		Set<String> testedKeys = new LinkedHashSet<>();
		Set<String> canonicalSeen = new LinkedHashSet<>();

		for (String rawPath : candidatePaths) {
			if (rawPath == null || rawPath.isBlank()) continue;
			String normPath = normalizePath(rawPath);
			String lookupKey = CodeLab.IS_WINDOWS ? normPath.toLowerCase() : normPath;
			if (testedKeys.contains(lookupKey)) continue;
			testedKeys.add(lookupKey);

			File file = new File(normPath);
			if (file.isAbsolute()) {
				try {
					String canon = file.getCanonicalPath();
					if (CodeLab.IS_WINDOWS) canon = canon.toLowerCase();
					if (canonicalSeen.contains(canon)) continue;
					canonicalSeen.add(canon);
				} catch (Exception ignored) {}
			}

			ToolInfo info = probeExecutable(normPath);
			if (info != null) {
				validTools.add(info);
			}
		}

		return validTools;
	}

	///////////////////////////////////////////////////
	// Commandes système where / which
	///////////////////////////////////////////////////

	protected void runWhichCommand(Set<String> candidates, String binName) {
		try {
			ProcessBuilder pb = new ProcessBuilder("which", binName);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = normalizePath(line);
					if (line != null && !line.isBlank() && !line.startsWith("which:") && !line.contains("no " + binName)) {
						File f = new File(line);
						if (f.exists()) candidates.add(normalizePath(f.getAbsolutePath()));
						else candidates.add(line);
					}
				}
			}
			p.waitFor(2, TimeUnit.SECONDS);
		} catch (Exception ignored) {}
	}

	protected void runWhereCommand(Set<String> candidates, String binName) {
		List<List<String>> cmds = new ArrayList<>();
		cmds.add(Arrays.asList("where", binName));
		String sysRoot = System.getenv("SystemRoot");
		if (sysRoot != null && !sysRoot.isBlank()) {
			cmds.add(Arrays.asList(sysRoot + "\\System32\\where.exe", binName));
		}
		for (List<String> cmd : cmds) {
			try {
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true);
				Process p = pb.start();
				boolean foundAny = false;
				try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = br.readLine()) != null) {
						line = normalizePath(line);
						if (line != null && !line.isBlank() && !line.startsWith("INFO:") && (line.endsWith(".exe") || line.endsWith(".cmd") || line.endsWith(".bat"))) {
							File f = new File(line);
							if (f.exists()) candidates.add(normalizePath(f.getAbsolutePath()));
							else candidates.add(line);
							foundAny = true;
						}
					}
				}
				p.waitFor(2, TimeUnit.SECONDS);
				if (foundAny) break;
			} catch (Exception ignored) {}
		}
	}

	///////////////////////////////////////////////////
	// Parcours du PATH
	///////////////////////////////////////////////////

	protected void scanPathEnv(Set<String> candidates) {
		String pathEnv = System.getenv("PATH");
		if (pathEnv == null || pathEnv.isBlank()) return;

		String sep = Pattern.quote(File.pathSeparator);
		String[] dirs = pathEnv.split(sep);
		for (String dir : dirs) {
			if (dir == null || dir.isBlank()) continue;
			File d = new File(dir.trim());
			if (!d.isDirectory()) continue;

			for (String bin : getBinNames()) {
				if (CodeLab.IS_WINDOWS) {
					checkAndAddExecutable(candidates, new File(d, bin + ".exe"));
					checkAndAddExecutable(candidates, new File(d, bin + ".cmd"));
					checkAndAddExecutable(candidates, new File(d, bin + ".bat"));
				} else {
					checkAndAddExecutable(candidates, new File(d, bin));
				}
			}
		}
	}

	///////////////////////////////////////////////////
	// Sondage d'un candidat
	///////////////////////////////////////////////////

	public ToolInfo probeExecutable(String rawPath) {
		String path = normalizePath(rawPath);
		if (path == null || path.isBlank()) return null;

		File f = new File(path);
		if (f.isAbsolute()) {
			if (!f.exists() || f.isDirectory()) return null;
			if (!CodeLab.IS_WINDOWS && !f.canExecute()) return null;
		}

		try {
			List<String> cmd = new ArrayList<>();
			cmd.add(path);
			cmd.addAll(getVersionArgs());

			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder sb = new StringBuilder();
			Thread reader = new Thread(() -> {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line).append("\n");
					}
				} catch (Exception ignored) {}
			});
			reader.start();

			boolean finished = process.waitFor(3, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				reader.interrupt();
				return null;
			}
			reader.join(1000);

			if (process.exitValue() == 0) {
				String output = sb.toString();
				String firstLine = "";
				for (String l : output.split("\n")) {
					l = l.trim();
					if (!l.isEmpty()) {
						firstLine = l;
						break;
					}
				}
				if (firstLine.isEmpty()) return null;

				if (isValidVersionOutput(output.toLowerCase())) {
					return new ToolInfo(path, firstLine);
				}
			}
		} catch (Exception ignored) {}

		return null;
	}

	///////////////////////////////////////////////////
	// Recherche interactive et configuration
	///////////////////////////////////////////////////

	public void searchAndConfigure(CodeLab codelab) {
		WaitingDialog.open(CodeLab.LABEL(getWaitingLabelKey()));

		new Thread(() -> {
			List<ToolInfo> tools = detect();
			WaitingDialog.close();

			SwingUtilities.invokeLater(() -> {
				if (tools.isEmpty()) {
					String msg = String.format(CodeLab.LABEL(getNotFoundLabelKey()), getAdvice());
					int choice = JOptionPane.showConfirmDialog(
						CodeLab.FRAME,
						msg,
						CodeLab.LABEL(getTitleLabelKey()),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE
					);

					if (choice == JOptionPane.YES_OPTION) {
						promptManualSelection(codelab);
					}
				} else if (tools.size() == 1) {
					ToolInfo info = tools.get(0);
					String msg = String.format(CodeLab.LABEL(getFoundLabelKey()), info.path, info.version);
					int choice = JOptionPane.showConfirmDialog(
						CodeLab.FRAME,
						msg,
						CodeLab.LABEL(getTitleLabelKey()),
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						CodeLab.ICON
					);
					if (choice == JOptionPane.YES_OPTION) {
						applyTool(codelab, info.path);
					}
				} else {
					ToolInfo[] options = tools.toArray(new ToolInfo[0]);
					ToolInfo selected = (ToolInfo) JOptionPane.showInputDialog(
						CodeLab.FRAME,
						CodeLab.LABEL(getMultipleLabelKey()),
						CodeLab.LABEL(getTitleLabelKey()),
						JOptionPane.QUESTION_MESSAGE,
						CodeLab.ICON,
						options,
						options[0]
					);
					if (selected != null) {
						applyTool(codelab, selected.path);
					}
				}
			});
		}).start();
	}

	protected void promptManualSelection(CodeLab codelab) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(CodeLab.LABEL(getTitleLabelKey()));
		if (CodeLab.IS_WINDOWS) {
			chooser.setFileFilter(new FileNameExtensionFilter("Exécutables (*.exe)", "exe"));
		}
		int res = chooser.showOpenDialog(CodeLab.FRAME);
		if (res == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			if (selectedFile != null) {
				String path = normalizePath(selectedFile.getAbsolutePath());
				ToolInfo info = probeExecutable(path);
				if (info != null) {
					applyTool(codelab, info.path);
				} else {
					Utils.showWarningDialog("ATTENTION", CodeLab.LABEL(getInvalidLabelKey()));
				}
			}
		}
	}

	public void applyTool(CodeLab codelab, String path) {
		String cleanPath = normalizePath(path);
		PropertyBase.setUserProperty(getPropertyKey(), cleanPath);
		updateCodeLabCommand(cleanPath);

		// Log dans la console CodeLab
		CodeLab.directPrintln(String.format("[codelab] %s=%s", getPropertyKey(), cleanPath), Console.COLOR_LOG);

		// Re-vérification dans CodeLab
		recheckLanguage();

		// Dialogue de confirmation
		Utils.showMessageDialog(
			CodeLab.LABEL(getTitleLabelKey()),
			String.format(CodeLab.LABEL(getSuccessLabelKey()), cleanPath)
		);

		// Si user.properties est affiché dans l'éditeur, recharger le contenu
		if (codelab != null && codelab.getEditor() != null && codelab.getEditor().getEditor() != null) {
			AbstractEditor currentSubEditor = codelab.getEditor().getEditor();
			if (currentSubEditor.getFileDescriptor() != null && currentSubEditor.getFileDescriptor().getFile() != null) {
				File opened = currentSubEditor.getFileDescriptor().getFile();
				File propFile = new File(CodeLab.USER_PROP_FILE);
				if (opened.getAbsolutePath().equals(propFile.getAbsolutePath())) {
					codelab.editPropFile(false);
				}
			}
		}
	}
}

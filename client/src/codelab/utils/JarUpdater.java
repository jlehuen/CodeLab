package codelab.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import codelab.CodeLab;

/**
 * Gestionnaire de mise à jour autonome et sécurisée de codelab.jar.
 * - Télécharge le nouveau JAR dans un fichier temporaire voisin (.new)
 * - Vérifie l'intégrité ZIP/JAR (présence de codelab/CodeLab.class) et SHA-256 (si fourni)
 * - Sauvegarde l'ancien JAR en .bak et effectue le remplacement atomique (POSIX)
 *   ou différé via script détaché (Windows) avant redémarrage.
 */
public class JarUpdater {

	public static final String GITHUB_RELEASES_LATEST_URL = "https://github.com/jlehuen/CodeLab/releases/latest";
	public static final String GITHUB_JAR_DOWNLOAD_URL = "https://github.com/jlehuen/CodeLab/releases/latest/download/codelab.jar";

	private static final long MIN_JAR_BYTES = 100_000L; // 100 Ko minimum

	/**
	 * Récupère la dernière version publiée sur GitHub Releases via un simple HEAD redirect
	 * (sans consommer le quota de l'API GitHub : illimité même en salle TP).
	 * Retourne par exemple "1.5.4", ou null en cas d'erreur réseau ou d'absence de release.
	 */
	public static String fetchLatestGithubVersion(int timeoutMs) {
		HttpURLConnection conn = null;
		try {
			URL url = URI.create(GITHUB_RELEASES_LATEST_URL).toURL();
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("User-Agent", "CodeLab/" + CodeLab.VERSION);

			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_TEMP
					|| code == HttpURLConnection.HTTP_MOVED_PERM
					|| code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				if (loc != null) {
					int idx = loc.lastIndexOf("/tag/");
					if (idx >= 0) {
						String tag = loc.substring(idx + 5).trim();
						if (tag.startsWith("v") || tag.startsWith("V")) {
							tag = tag.substring(1);
						}
						if (!tag.isEmpty() && Character.isDigit(tag.charAt(0))) {
							return tag;
						}
					}
				}
			}
		} catch (Exception ignored) {
		} finally {
			if (conn != null) conn.disconnect();
		}
		return null;
	}

	/**
	 * Tente de récupérer le fichier .sha256 associé au JAR (format brut ou sha256sum).
	 * Retourne null si absent (HTTP 404) ou inaccessible.
	 */
	public static String fetchOptionalRemoteSha256(String shaUrl, int timeoutMs) {
		HttpURLConnection conn = null;
		try {
			URL url = URI.create(shaUrl.trim()).toURL();
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestProperty("User-Agent", "CodeLab/" + CodeLab.VERSION);
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
			try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
				String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
				if (content.isEmpty()) return null;
				String token = content.split("\\s+")[0].trim();
				if (token.matches("^[0-9a-fA-F]{64}$")) return token.toLowerCase();
			}
		} catch (Exception ignored) {
		} finally {
			if (conn != null) conn.disconnect();
		}
		return null;
	}

	public static File getCurrentJarFile() {
		try {
			URI uri = CodeLab.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			File f = new File(uri);
			if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
				return f;
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static boolean canSelfUpdate() {
		File jar = getCurrentJarFile();
		if (jar == null) return false;
		File parent = jar.getParentFile();
		return parent != null && parent.isDirectory() && parent.canWrite() && jar.canWrite();
	}

	public static void downloadAndInstallJar(String jarUrl, String expectedSha256, String targetVersion, String targetBuild) {
		if (SwingUtilities.isEventDispatchThread()) {
			downloadAndInstallJarOnEdt(jarUrl, expectedSha256, targetVersion, targetBuild);
		} else {
			try {
				SwingUtilities.invokeAndWait(() ->
					downloadAndInstallJarOnEdt(jarUrl, expectedSha256, targetVersion, targetBuild));
			} catch (Exception e) {
				CodeLab.logger("JarUpdater invokeAndWait error: " + e.getMessage());
			}
		}
	}

	private static void downloadAndInstallJarOnEdt(String jarUrl, String expectedSha256, String targetVersion, String targetBuild) {
		File currentJar = getCurrentJarFile();
		if (currentJar == null || !canSelfUpdate()) {
			JOptionPane.showMessageDialog(
				CodeLab.FRAME,
				CodeLab.LABEL("UPDATE_JAR_NOT_WRITABLE"),
				CodeLab.LABEL("VERSION_TITLE"),
				JOptionPane.WARNING_MESSAGE,
				CodeLab.ICON);
			return;
		}

		JDialog progressDialog = new JDialog(CodeLab.FRAME, CodeLab.LABEL("VERSION_TITLE"), true);
		progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
		JLabel statusLabel = new JLabel(String.format(CodeLab.LABEL("UPDATE_JAR_DOWNLOADING"), targetVersion));
		JProgressBar progressBar = new JProgressBar(0, 100);
		progressBar.setPreferredSize(new Dimension(340, 22));
		progressBar.setIndeterminate(true);
		panel.add(statusLabel, BorderLayout.NORTH);
		panel.add(progressBar, BorderLayout.CENTER);
		progressDialog.setContentPane(panel);
		progressDialog.pack();
		progressDialog.setLocationRelativeTo(CodeLab.FRAME);

		SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
			private File tmpFile = null;
			private String errorMsg = null;

			@Override
			protected Void doInBackground() {
				HttpURLConnection conn = null;
				try {
					tmpFile = new File(currentJar.getParentFile(), currentJar.getName() + ".new");
					conn = openConnectionWithRedirects(jarUrl.trim(), 10_000, 30_000);
					int status = conn.getResponseCode();
					if (status != HttpURLConnection.HTTP_OK) {
						errorMsg = "HTTP " + status + " (" + jarUrl + ")";
						return null;
					}

					long contentLength = conn.getContentLengthLong();
					MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
					long downloaded = 0;

					try (InputStream in = new BufferedInputStream(conn.getInputStream());
					     FileOutputStream out = new FileOutputStream(tmpFile)) {
						byte[] buf = new byte[16_384];
						int n;
						while ((n = in.read(buf)) != -1) {
							out.write(buf, 0, n);
							sha256.update(buf, 0, n);
							downloaded += n;
							if (contentLength > 0) {
								int pct = (int) Math.min(100, (downloaded * 100) / contentLength);
								publish(pct);
							}
						}
					}

					if (downloaded < MIN_JAR_BYTES) {
						errorMsg = "Fichier téléchargé trop petit (" + downloaded + " octets).";
						return null;
					}

					// Vérification SHA-256 : explicite ou récupéré automatiquement depuis <jarUrl>.sha256
					String shaToVerify = expectedSha256;
					if ((shaToVerify == null || shaToVerify.isBlank() || shaToVerify.equals("UNDEFINED"))
							&& jarUrl != null && jarUrl.endsWith(".jar")) {
						shaToVerify = fetchOptionalRemoteSha256(jarUrl + ".sha256", 5_000);
					}
					if (shaToVerify != null && !shaToVerify.isBlank() && !shaToVerify.equals("UNDEFINED")) {
						String actualHex = bytesToHex(sha256.digest());
						if (!actualHex.equalsIgnoreCase(shaToVerify.trim())) {
							errorMsg = "Empreinte SHA-256 invalide (attendu: " + shaToVerify.trim() + ", reçu: " + actualHex + ").";
							return null;
						}
					}

					// Vérification structurelle de l'archive JAR
					try (JarFile jf = new JarFile(tmpFile)) {
						if (jf.getEntry("codelab/CodeLab.class") == null) {
							errorMsg = "Archive JAR invalide (codelab/CodeLab.class introuvable).";
							return null;
						}
					}

					// Sauvegarde de secours de l'ancien JAR
					File bakFile = new File(currentJar.getParentFile(), currentJar.getName() + ".bak");
					try {
						Files.copy(currentJar.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (Exception ignored) {}

					if (!CodeLab.IS_WINDOWS) {
						// Remplacement atomique sûr sous macOS / Linux
						try {
							Files.move(tmpFile.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
						} catch (Exception e) {
							Files.move(tmpFile.toPath(), currentJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				} catch (Exception e) {
					errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
				} finally {
					if (conn != null) conn.disconnect();
				}
				return null;
			}

			@Override
			protected void process(java.util.List<Integer> chunks) {
				if (!chunks.isEmpty()) {
					int pct = chunks.get(chunks.size() - 1);
					progressBar.setIndeterminate(false);
					progressBar.setValue(pct);
				}
			}

			@Override
			protected void done() {
				progressDialog.dispose();
				if (errorMsg != null) {
					if (tmpFile != null && tmpFile.exists()) tmpFile.delete();
					JOptionPane.showMessageDialog(
						CodeLab.FRAME,
						String.format(CodeLab.LABEL("UPDATE_JAR_ERROR"), errorMsg),
						CodeLab.LABEL("VERSION_TITLE"),
						JOptionPane.ERROR_MESSAGE,
						CodeLab.ICON);
					return;
				}

				if (CodeLab.IS_WINDOWS) {
					// Sous Windows, la JVM verrouille le JAR : remplacement différé après fermeture
					JOptionPane.showMessageDialog(
						CodeLab.FRAME,
						CodeLab.LABEL("UPDATE_JAR_SUCCESS_RESTART"),
						CodeLab.LABEL("VERSION_TITLE"),
						JOptionPane.INFORMATION_MESSAGE,
						CodeLab.ICON);
					if (CodeLab.INSTANCE != null) CodeLab.INSTANCE.things_to_do_before_exiting();
					launchWindowsSwapAndRestart(tmpFile, currentJar);
					CodeLab.exit();
					return;
				}

				// Au démarrage (avant ouverture de l'IDE), relancer directement la nouvelle version
				if (CodeLab.FRAME == null) {
					if (CodeLab.LAUNCHER != null && !CodeLab.LAUNCHER.isBlank()) {
						CodeLab.relaunch();
					} else {
						JOptionPane.showMessageDialog(
							null,
							CodeLab.LABEL("UPDATE_JAR_SUCCESS_RESTART"),
							CodeLab.LABEL("VERSION_TITLE"),
							JOptionPane.INFORMATION_MESSAGE,
							CodeLab.ICON);
						CodeLab.exit();
					}
					return;
				}

				Object[] opts = { CodeLab.LABEL("BTN_LATER"), CodeLab.LABEL("UPDATE_JAR_RESTART_NOW") };
				int ans = JOptionPane.showOptionDialog(
					CodeLab.FRAME,
					CodeLab.LABEL("UPDATE_JAR_SUCCESS"),
					CodeLab.LABEL("VERSION_TITLE"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.INFORMATION_MESSAGE,
					CodeLab.ICON,
					opts,
					opts[1]);
				if (ans == 1) {
					if (CodeLab.INSTANCE != null) CodeLab.INSTANCE.things_to_do_before_exiting();
					CodeLab.relaunch();
				}
			}
		};

		worker.execute();
		progressDialog.setVisible(true);
	}

	/**
	 * Ouvre une connexion HTTP(S) en suivant explicitement les redirections multi-hôtes
	 * (ex: github.com -> objects.githubusercontent.com).
	 */
	private static HttpURLConnection openConnectionWithRedirects(String urlStr, int connectTimeoutMs, int readTimeoutMs) throws Exception {
		String current = urlStr;
		for (int hop = 0; hop < 6; hop++) {
			URL url = URI.create(current).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(connectTimeoutMs);
			conn.setReadTimeout(readTimeoutMs);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "CodeLab/" + CodeLab.VERSION);
			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_PERM
					|| code == HttpURLConnection.HTTP_MOVED_TEMP
					|| code == HttpURLConnection.HTTP_SEE_OTHER
					|| code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				conn.disconnect();
				if (loc == null || loc.isBlank()) {
					throw new java.io.IOException("Redirection HTTP sans en-tête Location (" + code + ")");
				}
				current = URI.create(current).resolve(loc.trim()).toString();
				continue;
			}
			return conn;
		}
		throw new java.io.IOException("Trop de redirections HTTP pour " + urlStr);
	}

	private static void launchWindowsSwapAndRestart(File tmpJar, File targetJar) {
		try {
			String launcher = CodeLab.LAUNCHER;
			StringBuilder cmd = new StringBuilder();
			cmd.append("timeout /t 2 /nobreak >nul & move /y \"")
			   .append(tmpJar.getAbsolutePath())
			   .append("\" \"")
			   .append(targetJar.getAbsolutePath())
			   .append("\"");
			if (launcher != null && !launcher.isBlank()) {
				cmd.append(" & start \"\" \"").append(launcher).append("\" --relauch");
			}
			new ProcessBuilder("cmd.exe", "/c", cmd.toString()).start();
		} catch (Exception e) {
			CodeLab.logger("Windows JAR swap error: " + e.getMessage());
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}

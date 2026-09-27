package codelab.utils;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.ExceptionManager;

/**
*	Classe utilitaire un peu fourre-tout...
*	@author Jérôme Lehuen
*	@version 15/01/24
*/

public class Utils {

	///////////////////////////////////////////////////
	// Informations sur l'environnement
	///////////////////////////////////////////////////

	public static String getOperatingSystem() {
		final String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows")) return "Windows";
		if (osName.startsWith("Linux")) return "Linux";
		if (osName.startsWith("Unix")) return "Linux";
		if (osName.startsWith("Mac")) return "Mac";
		fatalError(String.format("ERROR: Utils.getOperatingSystem() returns [%s]", osName));
		return null;
	}

	public static String getArchitecture() {
		final String arch = System.getProperty("os.arch", "").toLowerCase();
		final String osName = System.getProperty("os.name", "");
		if (arch.contains("aarch64") || arch.contains("arm")) {
			return osName.startsWith("Mac") ? "Apple Silicon" : "ARM64";
		}
		return "Intel";
	}

	public static String getHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			return "UnknownHostException";
		}
	}

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			return "UnknownHostException";
		}
	}

	public static String getSystemLanguage() {
		// https://howtodoinjava.com/java/date-time/how-to-get-current-user-locale-in-java/
		Locale currentLocale = Locale.getDefault();
		return currentLocale.getLanguage();
	}

	public static String getCurrentFolder() {
		final String folder = Paths.get(".").toAbsolutePath().normalize().toString();
		//System.out.println("Current folder = " + folder);
		return folder;
	}

	public static Tuple<Integer, Integer> getScreenSize() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) screenSize.getWidth();
		int height = (int) screenSize.getHeight();
		return new Tuple<Integer, Integer>(width, height);
	}

	public static String getDate() {
		// https://www.jmdoudoux.fr/java/dej/chap-utilisation_dates.htm
		final Date aujourdhui = new Date();
		final SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yy (HH:mm:ss)");
		return formater.format(aujourdhui);
	}

	public static Point getMouseLocation() {
		return MouseInfo.getPointerInfo().getLocation();
	}

	public static boolean isInternetAvailable(int timeout) {
		String host = "google.com";
		int port = 80;
		try (Socket socket = new Socket()) {
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress, timeout);
            return true;
        }
        catch(UnknownHostException e) {
            return false;
        }
		catch(SocketTimeoutException e) {
			return false;
		}
		catch (IOException e) {
			return false;
		}
	}

	public static boolean checkServer(String host, int port, int timeout) {
		if (host.isBlank() || host.equals("undefined")) {
			return false;
		}
		try {
			// Créer un socket non connecté
			Socket socket = new Socket();
			// Définir le timeout pour la connexion
			socket.connect(new InetSocketAddress(host, port), timeout);
			// Définir le timeout pour les opérations de lecture
			socket.setSoTimeout(timeout);
			try (Socket autoCloseSocket = socket) {
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
				writer.println("PING"); // Envoyer un ping au serveur
				return true;
			}
		}
		catch(UnknownHostException e) {
			return false;
		}
		catch(SocketTimeoutException e) {
			return false;
		}
		catch (IOException e) {
			return false;
		}
	}

	public static boolean isPortAvailable(int port) {
		try (var ss = new ServerSocket(port); var ds = new DatagramSocket(port)) {
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public static void wait(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void wait(int ms, int nanos) {
		try {
			Thread.sleep(ms, nanos);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	public static void fatalError(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}

	///////////////////////////////////////////////////
	// Quelques méthodes sur les fichiers
	///////////////////////////////////////////////////

	public static boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

	public static boolean folderExists(String filename) {
		File file = new File(filename);
		return file.exists() && file.isDirectory();
	}

	public static boolean deleteFile(String filename) {
		File file = new File(filename);
		return file.delete();
	}

	public static boolean initFolder(File file) {
		if (file.exists() && file.isDirectory()) {
			// Vider le répertoire non récursivement
			Arrays.stream(file.listFiles()).forEach(File::delete);
			return true;
		}
		else return file.mkdir();
	}

	public static boolean createDirectory(String name) {
		try {
			Path path = Paths.get(name);
			Files.createDirectories(path);
			return true;
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static boolean copyFile(File file1, File file2) {
		try {
			Files.copy(file1.toPath(), file2.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return true;
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static boolean moveFile(File file1, File file2) {
		try {
			Files.move(file1.toPath(), file2.toPath());
			return true;
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static boolean sameContent(File file1, File file2) {
		try {
			List<String> liste1 = Files.readAllLines(file1.toPath());
			List<String> liste2 = Files.readAllLines(file2.toPath());
			return liste1.equals(liste2);
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static boolean pathEquals(String path1, String path2) {
		// Normalisation des séparateurs pour comparaison multiplateforme
		String path1a = path1.replaceAll("\\\\", "/");
		String path2a = path2.replaceAll("\\\\", "/");
		return path1a.equals(path2a);
	}

	public static boolean printFile(File file) {
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine())
				System.out.println(scanner.nextLine());
			scanner.close();
			return true;
		}
		catch (FileNotFoundException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static String getDateReadable(File file) {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			FileTime time = attr.lastModifiedTime();
			return DateTimeFormatter.ofPattern("dd MMM (HH:mm:ss)")
				.withZone(ZoneId.systemDefault())
				.format(time.toInstant());
		}
		catch (IOException e) {
			return "ERROR";
		}
	}

	public static String getDateFormatted(File file) {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			FileTime time = attr.lastModifiedTime();
			return DateTimeFormatter.ofPattern("YYMMdd_HHmmss")
				.withZone(ZoneId.systemDefault())
				.format(time.toInstant());
		}
		catch (IOException e) {
			return "ERROR";
		}
	}

	/*
	public static boolean sameContent(File file1, File file2) {
		try {
			Path path1 = file1.toPath();
			Path path2 = file2.toPath();
			final long size = Files.size(path1);
			if (size != Files.size(path2)) return false;
			if (size < 4096) return Arrays.equals(Files.readAllBytes(path1), Files.readAllBytes(path2));
			try (InputStream is1 = Files.newInputStream(path1);
				InputStream is2 = Files.newInputStream(path2)) {
				int data;
				while ((data = is1.read()) != -1)
					if (data != is2.read()) return false;
			}
			return true;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return false;
		}
	}
	*/

	///////////////////////////////////////////////////
	// Quelques méthodes de calcul
	///////////////////////////////////////////////////

	public static int min(int[] tab) {
		final int[] dest = tab.clone();
		Arrays.sort(dest);
		return dest[0];
	}

	public static int max(int[] tab) {
		final int[] dest = tab.clone();
		Arrays.sort(dest);
		return dest[tab.length - 1];
	}

	public static double round(double value, int n) {
		return (Math.round(value * Math.pow(10, n))) / (Math.pow(10, n));
	}

	public static float round(float value, int n) {
		return (float) ((Math.round(value * Math.pow(10, n))) / (Math.pow(10, n)));
	}

	///////////////////////////////////////////////////
	// Autres méthodes utiliaires
	///////////////////////////////////////////////////

	public static int nblines(String str) {
		// https://stackoverflow.com/questions/275944/how-do-i-count-the-number-of-occurrences-of-a-char-in-a-string
		return (int) str.chars().filter(ch -> ch == '\n').count();
	}

	public static String tabulation(int length) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) sb.append("\t");
		return sb.toString();
	}

	public static String repeat(int count, String with) {
		return new String(new char[count]).replace("\0", with);
	}

	public static int[] clone(int[] source) {
		final int[] destination = new int[source.length];
		System.arraycopy(source, 0, destination, 0, source.length);
		return destination;
	}

	public static boolean contains(String[] array, String value) {
		return Arrays.asList(array).contains(value);
	}

	public static List<Object> substract(List<Object> list1, List<Object> list2) {
		List<Object> result = new ArrayList<Object>(list1);
		result.removeAll(list2);
		return result;
	}

	public static String capitalize(String line) {
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}

	public static String decapitalize(String line) {
		return Character.toLowerCase(line.charAt(0)) + line.substring(1);
	}

	private static final Pattern regex = Pattern.compile("(\\d+).(\\d+).(\\d+)");
	public static int getVersionPart(String number, int pos) {
		Matcher matcher = regex.matcher(number);
		if (!matcher.find()) return -1;
		if (pos < 1) return -1;
		if (pos > 3) return -1;
		return Integer.valueOf(matcher.group(pos));
	}

	public static boolean isPrintable(char c) {
		if (c == ' ') return true;
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		return (!Character.isISOControl(c))
			&& c != KeyEvent.CHAR_UNDEFINED
			&& block != null
			&& block != Character.UnicodeBlock.SPECIALS;
	}

	public static Map<String, String> getQueryParameters(String query) {
		Map<String, String> result = new HashMap<String, String>();
		for (String param : query.split("&")) {
			String pair[] = param.split("=");
			if (pair.length > 1) result.put(pair[0], pair[1]);
			else result.put(pair[0], "");
		}
		return result;
	}

	public static void pressTab(int amountOfClics) {
		SwingUtilities.invokeLater(() -> {
			try {
				Robot robot = new Robot();
				int i = amountOfClics;
				while (i-- > 0) {
					robot.keyPress(KeyEvent.VK_TAB);
					robot.delay(100);
					robot.keyRelease(KeyEvent.VK_TAB);
				}
			}
			catch (AWTException e) {
				ExceptionManager.process(e);
			}
		});
	}

	public static Graphics2D getGraphics2D(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		// Pour avoir un beau lissage des tracés en 2D
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_ALPHA_INTERPOLATION,
			RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		return g2d;
	}

	///////////////////////////////////////////////////
	// Pour ouvrir des fenêtres de dialogue
	///////////////////////////////////////////////////

	public static void showMessageDialog(String title, String texte) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(CodeLab.FRAME, texte, title, JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON);
		});
	}

	public static void showWarningDialog(String title, String texte) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(CodeLab.FRAME, texte, title, JOptionPane.WARNING_MESSAGE);
		});
	}

	public static boolean YesOrNoDialog(String title, String texte) {
		int res = JOptionPane.showConfirmDialog(CodeLab.FRAME, texte, title, JOptionPane.YES_NO_OPTION);
		return res == JOptionPane.YES_OPTION;
	}

	public static boolean confirmDialog(String question) {
		return JOptionPane.showConfirmDialog(
			CodeLab.FRAME, question, "ATTENTION",
			JOptionPane.YES_OPTION) == 0;
	}

	public static boolean confirmDialog(Component component, String question) {
		return JOptionPane.showConfirmDialog(
			component, question, "ATTENTION",
			JOptionPane.YES_OPTION) == 0;
	}

	///////////////////////////////////////////////////
	// Gestion des fichiers properties
	///////////////////////////////////////////////////

	public static void loadProperties1(Properties prop, String filename) {
		// Pour lire dans un fichier externe au .jar
		System.setProperty("file.encoding", "UTF-8");
		System.out.println(String.format("      Loading %s... ", filename));
		try {
			final FileReader reader = new FileReader(filename);
			prop.load(reader);
			reader.close();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			System.exit(-1);
		}
	}

	public static void loadProperties2(Properties prop, String filename) {
		// Pour lire dans le répertoire /data du .jar
		System.setProperty("file.encoding", "UTF-8");
		System.out.println(String.format("      Loading %s... ", filename));
		try {
			final InputStream inputStream = Utils.class.getResourceAsStream(filename);
			final Reader reader = new InputStreamReader(inputStream, "UTF-8");
			prop.load(reader);
			reader.close();
		}
		catch (IOException | NullPointerException e) {
			System.out.println("ERROR: Can't read the file " + filename);
			System.exit(-1);
		}
	}

	public static PropertyResourceBundle readPropertyResourceBundle(String filename) {
		System.out.print(String.format("      Loading %s... ", new File(filename).getName()));
		try {
			final InputStream inputStream = new FileInputStream(filename);
			final InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			final PropertyResourceBundle bundle = new PropertyResourceBundle(reader);
			System.out.println("DONE");
			return bundle;
		}
		catch (IOException e) {
			System.out.println("ERROR");
			return null;
		}
	}

	public static PropertyResourceBundle readPropertyResourceBundle_rsc(String filename) {
		System.out.print(String.format("      Loading %s... ", filename));
		try {
			final InputStream inputStream = Utils.class.getResourceAsStream(filename);
			final InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			final PropertyResourceBundle bundle = new PropertyResourceBundle(reader);
			System.out.println("DONE");
			return bundle;
		}
		catch (IOException e) {
			System.out.println("ERROR");
			return null;
		}
	}

	public static PropertyResourceBundle readPropertyResourceBundleOverInternet(String urlPath, int timeout) {
		System.out.print("      Loading properties over Internet... ");
		try {
			String cacheBuster = (urlPath.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
			URL url = URI.create(urlPath + cacheBuster).toURL();
			URLConnection con = url.openConnection();
			con.setUseCaches(false);
			con.setDefaultUseCaches(false);
			con.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
			con.setRequestProperty("Pragma", "no-cache");
			con.setRequestProperty("Expires", "0");
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			InputStream inputStream = con.getInputStream();
			InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
			PropertyResourceBundle bundle = new PropertyResourceBundle(reader);
			System.out.println("DONE");
			return bundle;
		}
		catch (IOException e) {
			System.out.println("ERROR");
			return null;
		}
	}

	///////////////////////////////////////////////////
	// Pour lancer des exécutions et programmes
	///////////////////////////////////////////////////

	public static void openBrowser(String url) {
		if (url == null) {
			System.err.println("ERROR: Exception #1 in openBrowser");
		}
		else if (!Desktop.isDesktopSupported()) {
			System.err.println("ERROR: Exception #2 in openBrowser");
		}
		else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			System.err.println("ERROR: Exception #3 in openBrowser");
		}
		else try {
			Desktop.getDesktop().browse(new URI(url));
		}
		catch (IOException e) {
			System.err.println("ERROR: Exception #4 in openBrowser");
		}
		catch (URISyntaxException e) {
			System.err.println("ERROR: Exception #5 in openBrowser");
		}
	}

	///////////////////////////////////////////////////
	// Découpage robuste de ligne de commande (espaces & guillemets)
	///////////////////////////////////////////////////

	public static List<String> splitCommandLine(String cmd) {
		List<String> tokens = new ArrayList<>();
		if (cmd == null || cmd.isBlank()) return tokens;
		cmd = cmd.trim();

		// Si la chaîne entière est un fichier existant sans arguments
		if (new File(cmd).isFile()) {
			tokens.add(cmd);
			return tokens;
		}

		StringBuilder current = new StringBuilder();
		boolean inDoubleQuotes = false;
		boolean inSingleQuotes = false;

		for (int i = 0; i < cmd.length(); i++) {
			char c = cmd.charAt(i);

			if (c == '\"' && !inSingleQuotes) {
				inDoubleQuotes = !inDoubleQuotes;
			} else if (c == '\'' && !inDoubleQuotes) {
				inSingleQuotes = !inSingleQuotes;
			} else if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
				if (current.length() > 0) {
					tokens.add(current.toString());
					current.setLength(0);
				}
			} else {
				current.append(c);
			}
		}

		if (current.length() > 0) {
			tokens.add(current.toString());
		}

		// Heuristique de rattrapage pour chemins Windows non entourés de guillemets
		// (ex: C:\Program Files\TDM-GCC-64\bin\gcc.exe --version)
		if (!tokens.isEmpty() && !new File(tokens.get(0)).isFile()) {
			for (int j = 1; j < tokens.size(); j++) {
				StringBuilder candidate = new StringBuilder();
				for (int k = 0; k <= j; k++) {
					if (k > 0) candidate.append(" ");
					candidate.append(tokens.get(k));
				}
				if (new File(candidate.toString()).isFile()) {
					List<String> merged = new ArrayList<>();
					merged.add(candidate.toString());
					for (int k = j + 1; k < tokens.size(); k++) {
						merged.add(tokens.get(k));
					}
					return merged;
				}
			}
		}

		return tokens;
	}

	public static String execute(String cmd) {
		return execute(cmd, 5);
	}

	public static String execute(String cmd, int timeoutSeconds) {
		try {
			List<String> array = splitCommandLine(cmd);
			ProcessBuilder pb = new ProcessBuilder(array);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder result = new StringBuilder();
			Thread reader = new Thread(() -> {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
					String line;
					while ((line = br.readLine()) != null) {
						result.append(line).append("\n");
					}
				} catch (Exception ignored) {}
			});
			reader.start();

			boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				reader.interrupt();
				return null;
			}
			reader.join(1000);
			return result.toString();
		}
		catch (Exception e) {
			return null;
		}
	}

	public static int execute2(String cmd) {
		try {
			List<String> array = splitCommandLine(cmd);
			ProcessBuilder pb = new ProcessBuilder(array);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			Thread reader = new Thread(() -> {
				try (InputStream in = process.getInputStream()) {
					byte[] buf = new byte[1024];
					while (in.read(buf) != -1) {}
				} catch (Exception ignored) {}
			});
			reader.start();

			boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				reader.interrupt();
				return -999;
			}
			return process.exitValue();
		}
		catch (Exception e) {
			return -999;
		}
	}

	public static void executeShellCmd(String cmd) {
		switch (getOperatingSystem()) {
			case "Mac":
			case "Linux": cmd = String.format("/bin/sh -c %s", cmd); break;
			case "Windows": cmd = String.format("cmd.exe /c %s", cmd); break;
		}
		try {
			ProcessBuilder builder = new ProcessBuilder(cmd);
			Process process = builder.start();
			int exitValue = process.waitFor();
			if (exitValue != 0) {
				System.out.format("ERROR %d while executing [%s]\n", exitValue, cmd);
			}
		}
		catch (Exception e) {
			// Utilisé notammement dans Compilateur.java
			// Ne rien afficher (en cas de "rm *.exe" par exemple)
		}
	}

	///////////////////////////////////////////////////
	// Pour créer des raccourcis sur le bureau
	///////////////////////////////////////////////////

	public static void createDesktopIcon() {
		try {
			switch (getOperatingSystem()) {
				case "Mac":
				case "Linux":
					String target1 = "...";
					String link1 = "...";
					createUnixSymbolicLink(target1, link1);
					break;
				case "Windows":
					String target2 = "...";
					String link2 = "...";
					createWindowsSymbolicLink(target2, link2);
					break;
			}
		}
		catch (Exception e) {
			ExceptionManager.process(e);
		}
	}

	private static int createUnixSymbolicLink(String target, String link) {
		String[] cmd = { "ln", "-s", new File(target).getAbsolutePath(), new File(link).getAbsolutePath() };
		int exitCode = 0;
		try {
			exitCode = Runtime.getRuntime().exec(cmd).waitFor();
		}
		catch (InterruptedException e) {
			return -999;
		}
		catch (IOException e) {
			return exitCode;
		}
		return exitCode;
	}

	private static void createWindowsSymbolicLink(String target, String link) {
		// Implémentation optionnelle des liens symboliques Windows via Files.createSymbolicLink
		// https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createSymbolicLink(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.attribute.FileAttribute...)
	}
}

package codelab.utils;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import codelab.ExceptionManager;

/**
*	Classe utilitaire d'accès aux ressources
*	@author Jérôme Lehuen
*	@version 19/06/25
*/

public class ResourceUtils {

	public static void readFileOverInternet(String url) {
		try (Scanner s = new Scanner(URI.create(url).toURL().openStream())) {
			while (s.hasNextLine()) {
				System.out.println(s.nextLine());
			}
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static BufferedReader getReader(String filename) {
		final InputStream is = ResourceUtils.class.getResourceAsStream("/data/" + filename);
		final InputStreamReader isr = new InputStreamReader(is);
		return new BufferedReader(isr);
	}

	public static String readResourceAsString(String filename) {
		StringBuilder out = new StringBuilder();
		String line;
		try (	InputStream is = ResourceUtils.class.getResourceAsStream(filename);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			while ((line = br.readLine()) != null) out.append(line);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
		return out.toString();
	}

	public static ArrayList<String> readResourceTextFile(String filename) {
		final ArrayList<String> list = new ArrayList<String>();
		try (	InputStream is = ResourceUtils.class.getResourceAsStream("/data/" + filename);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			String line;
			while ((line = br.readLine()) != null) list.add(line);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
		return list;
	}

	public static FileInputStream loadInputStream(String filename) {
		try {
			return new FileInputStream(filename);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}

	public static Font loadFont(String fontName) {
		try {
			final Font font = Font
					.createFont(java.awt.Font.TRUETYPE_FONT, ResourceUtils.class.getResourceAsStream("/data/" + fontName))
					.deriveFont(10f);
			final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(font);
			return font;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
		catch (FontFormatException e) {
			ExceptionManager.process(e);
		}
		return null;
	}

	public static InputStream loadInputStream_jar(String filename) {
		// return getClass().getResourceAsStream(filename);
		// return ResourceUtils.class.getResourceAsStream("/data/" + filename);
		return ResourceUtils.class.getResourceAsStream(filename);
	}

	public static BufferedImage loadBufferedImageAsRessource(String filename) {
		try {
			return ImageIO.read(ResourceUtils.class.getResourceAsStream("/data/" + filename));
		}
		catch (IOException e) {
			return null;
		}
	}

	public static BufferedImage loadBufferedImage(File file) {
		try {
			return ImageIO.read(file);
		}
		catch (IOException e) {
			return null;
		}
	}

	public static ImageIcon loadImageIcon(String filename) {
		return new ImageIcon(ResourceUtils.class.getResource("/data/" + filename));
	}

	/*
	// Version merdique qui pose des problèmes avec certaines configurations Linux !!
	public static List<String> getFiles(String base) {
		int start = base.length();
		List<String> res = new ArrayList<String>();
		String path = ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		try (JarFile jar = new JarFile(path)) {
			Enumeration<JarEntry> entries = jar.entries();
			List<JarEntry> list_entries = Collections.list(entries);
			for(JarEntry entry : list_entries) {
				if (entry.isDirectory()) continue;
				String name = entry.toString();
				if (name.startsWith(base)) res.add(name.substring(start));
			}
			return res;
		}
		catch (IOException e) {
			return res;
		}
	}
	*/

	public static List<String> getFiles(String base) {
		List<String> res = new ArrayList<String>();
		try {
			// Méthode plus robuste pour obtenir le chemin du JAR
			URI uri = ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			String path = uri.getPath();
			// System.out.println("DEBUG: Looking for files in base: " + base);
			// System.out.println("DEBUG: JAR path: " + path);
			// Vérifier si on est dans un JAR ou dans le système de fichiers
			if (path.endsWith(".jar")) {
				return getFilesFromJar(base, path); // Mode JAR (fichiers dans le JAR)
			} else {
				return getFilesFromFileSystem(base); // Mode développement (fichiers sur le système de fichiers)
			}
		}
		catch (Exception e) {
			ExceptionManager.process(e);
			return res;
		}
	}

	private static List<String> getFilesFromJar(String base, String jarPath) {
		List<String> res = new ArrayList<String>();
		int start = base.length();
		try (JarFile jar = new JarFile(new File(jarPath))) {
			Enumeration<JarEntry> entries = jar.entries();
			// System.out.println("DEBUG: Scanning JAR entries...");
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory()) continue;
				String name = entry.getName();
				// System.out.println("DEBUG: Found entry: " + name);
				if (name.startsWith(base)) {
					String filename = name.substring(start);
					res.add(filename);
					// System.out.println("DEBUG: Added file: " + filename);
				}
			}
			// System.out.println("DEBUG: Total files found in JAR: " + res.size());
			return res;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return res;
		}
	}

	private static List<String> getFilesFromFileSystem(String base) {
		List<String> res = new ArrayList<String>();
		try {
			// Mode développement - utiliser getResource pour obtenir l'URL du répertoire
			java.net.URL url = ResourceUtils.class.getResource("/" + base);
			if (url != null) {
				File dir = new File(url.toURI());
				if (dir.exists() && dir.isDirectory()) {
					File[] files = dir.listFiles();
					if (files != null) {
						for (File file : files) {
							if (file.isFile()) {
								res.add(file.getName());
								// System.out.println("DEBUG: Added file from filesystem: " + file.getName());
							}
						}
					}
				}
			}
			// System.out.println("DEBUG: Total files found in filesystem: " + res.size());
			return res;
		}
		catch (Exception e) {
			ExceptionManager.process(e);
			return res;
		}
	}
}

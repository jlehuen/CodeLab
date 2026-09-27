package codelab.modules.graphics;

import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.utils.MyFileUtils;

/**
*	Classe utilitaires pour le module graphique
*	@author Jérôme Lehuen
*	@version 13/12/23
*/

public class TurtleUtils {

	public static Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
	// https://books.google.fr/books?id=dOz-UK8Fl_UC&pg=PA22#v=onepage&q&f=false
	// TurtleUtils.TOOLKIT.sync(); // Synchroniser l'affichage (systèmes Linux)

	public static void wait(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void saveImage(BufferedImage image, String filename) {
		try {
			File outputfile = new File(filename);
			ImageIO.write(image, "png", outputfile);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static TurtlePack loadTurtlePack(String filename) {
		BufferedImage image;
		Path2D.Double path;
		try {
			image = ImageIO.read(CodeLab.class.getResourceAsStream("/data/turtles/" + filename));
		}
		catch (IOException e) {
			// Il n'y a pas de fichier image disponible
			ExceptionManager.process(e);
			return null;
		}
		try {
			filename = String.format("/data/turtles/%s.pol", MyFileUtils.noExtension(filename));
			InputStream stream = CodeLab.class.getResourceAsStream(filename);
			ObjectInputStream ois = new ObjectInputStream(stream);
			path = (Path2D.Double) ois.readObject();
			return new TurtlePack(image, path);
		}
		catch (Exception e) {
			// Il n'y a pas de fichier .pol disponible
			Rectangle2D rectangle = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
			path = new Path2D.Double();
			path.append(rectangle, false);
			return new TurtlePack(image, path);
		}
	}

	public static void createDirectory(String name) throws IOException {
		Path path = Paths.get(name);
		Files.createDirectories(path);
	}

	public static String getExtension(String filename) {
		if (filename.indexOf('.') == -1) return "";
		return filename.substring(filename.lastIndexOf('.'), filename.length());
	}

	public static boolean folderExists(String filename) {
		File file = new File(filename);
		return file.exists() && file.isDirectory();
	}

	public static File[] getFolderFiles(String folder) {
		File file = new File(folder);
		return file.listFiles();
	}

	public static BufferedImage loadBufferedImage(File file) {
		try {
			return ImageIO.read(file);
		}
		catch (IOException e) {
			System.err.println("ERROR: Can't read file: " + file);
			return null;
		}
	}

	public static BufferedImage clone(BufferedImage image) {
		BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		Graphics2D g2d = clone.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return clone;
	}

	public static BufferedImage resize(BufferedImage src, int targetWidth, int targetHeight) {
		double scaleW = (double) targetWidth / (double) src.getWidth();
		double scaleH = (double) targetHeight / (double) src.getHeight();
		double scale = scaleW < scaleH ? scaleW : scaleH;
		BufferedImage result = new BufferedImage((int) (src.getWidth() * scale), (int) (src.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(src, 0, 0, result.getWidth(), result.getHeight(), null);
		g2d.dispose();
		return result;
	}

	public static BufferedImage createBackground(Dimension d, Color color) {
		BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) image.getGraphics();
		g2d.setColor(color);
		g2d.fillRect(0, 0, d.width, d.height);
		g2d.dispose();
		return image;
	}

	public static Point limit(Point p, Dimension d) {
		if (p.x < 0) p.x = 0;
		if (p.y < 0) p.y = 0;
		if (p.x > d.width) p.x = d.width;
		if (p.y > d.height) p.y = d.height;
		return p;
	}

	public static void configureRender(Graphics2D g2d) {
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_ALPHA_INTERPOLATION,
			RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}
}

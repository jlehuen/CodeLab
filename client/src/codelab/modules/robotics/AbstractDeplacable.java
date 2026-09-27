package codelab.modules.robotics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.utils.GraphUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;

/**
*	Classe des objets déplaçables
*	@author Jérôme Lehuen
*	@version 08/10/22
*/

public abstract class AbstractDeplacable {

	public double x; // Position de l'objet en pixels
	public double y; // Position de l'objet en pixels
	public double theta; // Angle de l'objet en radians

	public int CENTER_X; // Centre de l'objet en pixels
	public int CENTER_Y; // Centre de l'objet en pixels
	public int largeur;
	public int hauteur;

	protected Simulator simulator;
	protected BufferedImage image;
	protected Path2D.Double path; // La forme originale
	protected Path2D.Double transformedPath; // La forme transformée

	public AffineTransform affineTransform; // La transformation affine
	protected float coords[] = new float[6]; // Pour afficher les segments

	// Pour simplifier...
	protected String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	protected void runtimeError(String msg) {
		CodeLab.INSTANCE.printToConsole(msg, Color.RED);
		CodeLab.INSTANCE.halt();
		Utils.beep();
	}

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public AbstractDeplacable(Simulator simulator) {
		this.simulator = simulator;
		path = new Path2D.Double(); // En cas d'appel précoce à intersects()
	}

	public AbstractDeplacable(Simulator simulator, String ident) {
		this.simulator = simulator;
		path = new Path2D.Double(); // En cas d'appel précoce à intersects()
		setImage(ident);
	}

	///////////////////////////////////////////////////
	// Méthodes protégées
	///////////////////////////////////////////////////

	protected void setImage(String ident) {
		String filename = String.format("simulator/%s.png", ident);
		image = ResourceUtils.loadBufferedImageAsRessource(filename);
		largeur = image.getWidth();
		hauteur = image.getHeight();

		try {
			// Chargement du fichier .pol associé à l'image
			filename = String.format("/data/simulator/%s.pol", ident);
			InputStream stream = getClass().getResourceAsStream(filename);
			ObjectInputStream ois = new ObjectInputStream(stream);
			path = (Path2D.Double) ois.readObject();
		}
		catch (IOException|NullPointerException e) {
			// Il n'y a pas de fichier .pol disponible
			Rectangle2D rectangle = new Rectangle2D.Double(0, 0, largeur, hauteur);
			path = new Path2D.Double();
			path.append(rectangle, false);
		}
		catch (ClassNotFoundException e) {
			ExceptionManager.process(e);
			System.exit(-1);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public boolean isColoredTag() { return false; }
	public boolean isObstacle() { return false; }
	public boolean isRobot() { return false; }

	public String getInfo() {
		// Peut être surchargé dans les sous-classes
		return "an unnamed object";
	}

	public int getAngle() {
		int angle = (int) Math.toDegrees(theta) % 360;
		if (angle < 0) angle += 360;
		return (angle == 0) ? 0 : 360 - angle;
	}

	public Rectangle getBounds() {
		if (transformedPath == null) return new Rectangle(); // Tracé non encore initialisé
		// Rectangle vertical autour de l'objet
		return transformedPath.getBounds();
	}

	public Point convert(Point point) {
		// Pour transforme un point dans le référentiel de l'objet
		double xx = x + CENTER_X + point.getX() * Math.cos(theta) - point.getY() * Math.sin(theta);
		double yy = y + CENTER_Y + point.getX() * Math.sin(theta) + point.getY() * Math.cos(theta);
		return new Point((int)xx, (int)yy);
	}

	public boolean contains(Point point) {
		if (transformedPath == null) return false; // Tracé non encore initialisé
		return transformedPath.contains(point);
	}

	///////////////////////////////////////////////////
	// Gestion de la poignée de rotation
	///////////////////////////////////////////////////

	private final int R_HANDLE = 10; // Rayon de la poignée
	private boolean handle_flag = false; // Flag d'affichage
	private double thetaInit; // Angle initial de la poignée

	public void showHandles() {
		handle_flag = true;
		simulator.repaint();
	}

	public void hideHandles() {
		handle_flag = false;
		simulator.repaint();
	}

	public void toggleHandles() {
		handle_flag = !handle_flag;
		simulator.repaint();
	}

	public boolean containsHandle(Point point) {
		// Le point est dans l'une des quatre poignées
		Point p1 = convert(new Point(-CENTER_X-R_HANDLE, -CENTER_Y-R_HANDLE));
		Point p2 = convert(new Point(-CENTER_X-R_HANDLE, hauteur-CENTER_Y+R_HANDLE));
		Point p3 = convert(new Point(largeur-CENTER_X+R_HANDLE, hauteur-CENTER_Y+R_HANDLE));
		Point p4 = convert(new Point(largeur-CENTER_X+R_HANDLE, -CENTER_Y-R_HANDLE));

		double d1 = p1.distance(point);
		double d2 = p2.distance(point);
		double d3 = p3.distance(point);
		double d4 = p4.distance(point);

		if (d1 < R_HANDLE) {
			thetaInit = Math.PI - Math.atan((double)CENTER_Y / (double)CENTER_X);
			return true;
		}
		if (d2 < R_HANDLE) {
			thetaInit = - Math.PI + Math.atan((double)CENTER_Y / (double)CENTER_X);
			return true;
		}
		if (d3 < R_HANDLE) {
			thetaInit = - Math.atan((double)CENTER_Y / (double)(largeur-CENTER_X));
			return true;
		}
		if (d4 < R_HANDLE) {
			thetaInit = Math.atan((double)CENTER_Y / (double)(largeur-CENTER_X));
			return true;
		}
		return false;
	}

	///////////////////////////////////////////////////
	// Détection des collisions
	///////////////////////////////////////////////////

	public boolean detectCollision() {
		if (transformedPath == null) return false; // Tracé non encore initialisé
		for (PathIterator it = transformedPath.getPathIterator(null); !it.isDone(); it.next()) {
			int type = it.currentSegment(coords); // Récupérer le type et les coordonnées du point
			if (type == PathIterator.SEG_LINETO) {
				Point point = new Point((int)coords[0], (int)coords[1]);
				if (simulator.findObstacleAt(point)) {
					//CodeLab.INSTANCE.printlnToConsoleRed("COLLISION");
					return true;
				}
			}
		}
		return false;
	}

	///////////////////////////////////////////////////
	// Tracé de l'objet
	///////////////////////////////////////////////////

	protected void draw(Graphics2D g2d) {

		// Calcul de la transformation
		affineTransform = new AffineTransform();
		affineTransform.setToTranslation(x, y);
		affineTransform.rotate(theta, CENTER_X, CENTER_Y);

		// Affichage de l'objet
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.drawImage(image, affineTransform, null);
		//if (!simulator.DEBUG) g2d.drawImage(image, affineTransform, null);

		// Calcul du contour de l'objet
		transformedPath = (Path2D.Double) path.clone();
		transformedPath.transform(affineTransform);

		// Affichage des informations de débugage
		if (simulator.DEBUG) {
			g2d.setColor(Color.red);
			g2d.draw(transformedPath);

			// Marquage du centre de l'objet
			GraphUtils.drawCircle(g2d, x + CENTER_X, y + CENTER_Y, 2);

			// Marquage des extrémités des segments
			for (PathIterator it = transformedPath.getPathIterator(null); !it.isDone(); it.next()) {
				int type = it.currentSegment(coords);
				if (type == PathIterator.SEG_LINETO)
				GraphUtils.drawCircle(g2d, (int)coords[0], (int)coords[1], 1);
			}
		}

		// Affichage des 4 poignées
		if (handle_flag) {
			g2d.setColor(Color.red);
			Point p1 = convert(new Point(-CENTER_X-R_HANDLE, -CENTER_Y-R_HANDLE));
			Point p2 = convert(new Point(-CENTER_X-R_HANDLE, hauteur-CENTER_Y+R_HANDLE));
			Point p3 = convert(new Point(largeur-CENTER_X+R_HANDLE, hauteur-CENTER_Y+R_HANDLE));
			Point p4 = convert(new Point(largeur-CENTER_X+R_HANDLE, -CENTER_Y-R_HANDLE));
			GraphUtils.drawCircle(g2d, x + CENTER_X, y + CENTER_Y, 2); // Marquage du centre de l'objet
			GraphUtils.drawCircle(g2d, p1.x, p1.y, R_HANDLE);
			GraphUtils.drawCircle(g2d, p2.x, p2.y, R_HANDLE);
			GraphUtils.drawCircle(g2d, p3.x, p3.y, R_HANDLE);
			GraphUtils.drawCircle(g2d, p4.x, p4.y, R_HANDLE);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes invoquées par le listener de souris
	///////////////////////////////////////////////////

	protected boolean repositionnement = false;

	public void prendre() {
		repositionnement = true;
	}

	public void deplacer(int dx, int dy) {
		x += dx;
		y += dy;
		simulator.repaint();
		simulator.update_dataTable();
	}

	public void pivoter(int dtheta) {
		theta += Math.toRadians(dtheta);
		simulator.repaint();
		simulator.update_dataTable();
	}

	public void pivoter(Point p) {
		double xx = p.x - x - CENTER_X;
		double yy = p.y - y - CENTER_Y;
		theta = Math.atan(yy / xx);
		if (xx < 0) theta += Math.PI;
		theta += thetaInit;
		simulator.repaint();
		simulator.update_dataTable();
	}

	public void deposer() {
		repositionnement = false;
	}
}

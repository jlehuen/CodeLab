package codelab.modules.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

import codelab.CodeLab;
import codelab.utils.ColorCode;
import codelab.utils.ColorUtils;
import codelab.utils.Utils;

/**
*	Classe de la tortue graphique
*	@author Jérôme Lehuen
*	@version 21/01/24
*/

public class Turtle {

	private TurtleCanvas canvas;
	private AffineTransform transform;
	private BufferedImage image;
	private Path2D.Double path;
	private Path2D.Double transformedPath;
	private float coords[] = new float[6];

	private int TURTLE_WIDTH;
	private int TURTLE_HEIGHT;
	private int TURTLE_CENTER_X;
	private int TURTLE_CENTER_Y;

	private double x;
	private double y;
	private double theta; // Degrés

	private double x0;
	private double y0;

	private int sensorC_x;
	private int sensorC_y;
	//private int sensorL_x;
	//private int sensorL_y;
	//private int sensorR_x;
	//private int sensorR_y;

	public boolean visible = true;
	public boolean pendown = true;
	private boolean walking = false;
	private boolean collision = false;

	private final int delay = 3; // Réglage de la vitesse

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Turtle(TurtleCanvas canvas, int index) {
		this.canvas = canvas;
		TurtlePack pack = canvas.getTurtlePack(index);
		setTurtle(pack);
	}

	///////////////////////////////////////////////////
	// Détection des collisions
	///////////////////////////////////////////////////

	private boolean detectCollision() {
		if (transformedPath == null) return false;
		for (PathIterator it = transformedPath.getPathIterator(null); !it.isDone(); it.next()) {
			int type = it.currentSegment(coords); // Récupérer le type et les coordonnées du point
			if (type == PathIterator.SEG_LINETO) {
				Point point = new Point((int)coords[0], (int)coords[1]);
				if (findObstacleAt(point)) return true;
			}
		}
		return false;
	}

	private boolean findObstacleAt(Point point) {
		if (canvas.contains(point)) {
			int hex = canvas.getBackgroundImage().getRGB(point.x, point.y);
			int r = (hex & 0xFF0000) >> 16;
			int g = (hex & 0xFF00) >> 8;
			int b = (hex & 0xFF);
			// J'utilise les valeurs RGB pour corriger le "bug de Meriem" (cyan dans le dégradé de gris)...
			if (r < 50 && g > 200 && b > 200 && ColorUtils.rgb2color(hex) == ColorCode.COLOR_CYAN) return true;
		}
		return false;
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void setTurtle(TurtlePack pack) {
		image = pack.getImage();
		path = pack.getPath();
		TURTLE_WIDTH = image.getWidth();
		TURTLE_HEIGHT = image.getHeight();
		TURTLE_CENTER_X = TURTLE_WIDTH / 2;
		TURTLE_CENTER_Y = TURTLE_HEIGHT / 2;
		canvas.repaint();
	}

	public void init() {
		// Pour positionner la tortue au centre du canvas à l'initialisation
		Dimension d = canvas.getSize();
		int console_height = CodeLab.CONSOLE_FLAG ? CodeLab.CONSOLE_SIZE : 0;
		x = (d.width - TURTLE_WIDTH) / 2;
		y = (d.height - console_height - TURTLE_HEIGHT) / 2;
		theta = -90;
	}

	public void reset() {
		// Pour repositionner la tortue au centre
		Dimension d = canvas.getSize();
		x = (d.width - TURTLE_WIDTH) / 2;
		y = (d.height - TURTLE_HEIGHT) / 2;
		theta = -90;
	}

	public void gotoXY(int x, int y, int theta) {
		this.x = x - TURTLE_WIDTH;
		this.y = y - TURTLE_HEIGHT;
		this.theta = theta;
	}

	public boolean contains(Point point) {
		if (transformedPath == null) return false;
		return transformedPath.contains(point);
	}

	public void move(int dx, int dy) {
		x += dx;
		y += dy;
		canvas.repaint();
	}

	public void rotate(int delta) {
		theta += delta;
		canvas.repaint();
	}

	public int getColor() {
		if (canvas.getBackgroundRectangle().contains(new Point(sensorC_x, sensorC_y))) {
			int rgb = canvas.getBackgroundImage().getRGB(sensorC_x, sensorC_y);
			return ColorUtils.rgb2color(rgb).ordinal();
		}
		else return ColorCode.COLOR_ERROR.ordinal();
	}

	public int getBrightness() {
		if (canvas.getBackgroundRectangle().contains(new Point(sensorC_x, sensorC_y))) {
			int rgb = canvas.getBackgroundImage().getRGB(sensorC_x, sensorC_y);
			return ColorUtils.getBrightness(rgb);
		}
		else return -1;
	}

	public void draw(Graphics2D g2d, Color color, int width) {
		if (!visible) return;

		// Calcul de la transformation
		transform = new AffineTransform();
		transform.setToTranslation(x, y);
		transform.rotate(Math.toRadians(theta), TURTLE_CENTER_X, TURTLE_CENTER_Y);

		// Calcul du point de capteur
		sensorC_x = (int) (x + TURTLE_CENTER_X + (TURTLE_WIDTH + 10)/2 * Math.cos(Math.toRadians(theta)));
		sensorC_y = (int) (y + TURTLE_CENTER_Y + (TURTLE_WIDTH + 10)/2 * Math.sin(Math.toRadians(theta)));
		/*
		sensorL_x = (int) (x + TURTLE_CENTER_X + 46 * Math.cos(Math.toRadians(theta-30)));
		sensorL_y = (int) (y + TURTLE_CENTER_Y + 46 * Math.sin(Math.toRadians(theta-30)));
		sensorR_x = (int) (x + TURTLE_CENTER_X + 46 * Math.cos(Math.toRadians(theta+30)));
		sensorR_y = (int) (y + TURTLE_CENTER_Y + 46 * Math.sin(Math.toRadians(theta+30)));
		*/

		// Calcul du contour de la tortue
		transformedPath = (Path2D.Double) path.clone();
		transformedPath.transform(transform);

		if (pendown && walking) {
			// Affichage du tracé intermédiaire
			g2d.setColor(color);
			g2d.setStroke(new BasicStroke(width));
			int x1 = (int) x0 + TURTLE_CENTER_X;
			int y1 = (int) y0 + TURTLE_CENTER_Y;
			int x2 = (int) x + TURTLE_CENTER_X;
			int y2 = (int) y + TURTLE_CENTER_Y;
			g2d.drawLine(x1, y1, x2, y2);
		}
		// Affichage de la tortue
		g2d.drawImage(image, transform, null);

		if (canvas.DEBUG) {
			// Affichage des tracés rouges
			g2d.setColor(Color.red);
			g2d.setStroke(new BasicStroke(1));
			g2d.draw(transformedPath);
			g2d.drawOval(sensorC_x-2, sensorC_y-2, 4, 4);
			//g2d.drawOval(sensorL_x-2, sensorL_y-2, 4, 4);
			//g2d.drawOval(sensorR_x-2, sensorR_y-2, 4, 4);
			int xc = (int)x + TURTLE_CENTER_X;
			int yc = (int)y + TURTLE_CENTER_Y;
			g2d.drawOval(xc-2, yc-2, 4, 4);
		}
	}

	public void forward(int dist) {
		x0 = x;
		y0 = y;
		if (visible) {
			int r = 0;
			walking = true;
			collision = false;
			if (dist > 0) while (r < dist) {
				// Marche en avant
				x = x0 + r * Math.cos(Math.toRadians(theta));
				y = y0 + r * Math.sin(Math.toRadians(theta));
				r += 1;
				canvas.repaint();
				if (detectCollision()) {
					collision = true;
					break;
				}
				Utils.wait(delay);
			}
			else while (r > dist) {
				// Marche en arrière
				x = x0 + r * Math.cos(Math.toRadians(theta));
				y = y0 + r * Math.sin(Math.toRadians(theta));
				r -= 1;
				canvas.repaint();
				if (detectCollision()) {
					collision = true;
					break;
				}
				Utils.wait(delay);
			}
			walking = false;
		}
		if (collision) canvas.collision();
		else {
			x = x0 + dist * Math.cos(Math.toRadians(theta));
			y = y0 + dist * Math.sin(Math.toRadians(theta));
			int x1 = (int) x0 + TURTLE_CENTER_X;
			int y1 = (int) y0 + TURTLE_CENTER_Y;
			int x2 = (int) x + TURTLE_CENTER_X;
			int y2 = (int) y + TURTLE_CENTER_Y;
			if (pendown) canvas.drawLine(x1, y1, x2, y2);
		}
	}

	public void turn(int angle) {
		double theta_fin = theta + angle;
		if (visible) {
			if (angle > 0) {
				collision = false;
				while (theta < theta_fin) {
					theta += 1;
					canvas.repaint();
					if (detectCollision()) {
						collision = true;
						break;
					}
					Utils.wait(delay);
				}
			} else {
				collision = false;
				while (theta > theta_fin) {
					theta -= 1;
					canvas.repaint();
					if (detectCollision()) {
						collision = true;
						break;
					}
					Utils.wait(delay);
				}
			}
		}
		theta = theta_fin;
		if (collision) canvas.collision();
	}
}

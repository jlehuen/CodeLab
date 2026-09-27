package codelab.utils;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
*	Classe utilitaire graphique
*	@author Jérôme Lehuen
*	@version 21/01/24
*/

public class GraphUtils {

	public static void drawPoint(Graphics2D g2d, int x, int y) {
		g2d.drawLine(x, y, x, y);
	}

	public static void drawLine(Graphics2D g2d, double x1, double y1, double x2, double y2) {
		g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
	}

	public static void drawLine(Graphics2D g2d, Point p1, Point p2) {
		g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
	}

	public static void drawCircle(Graphics2D g2d, int x, int y, int rayon) {
		final int diametre = 2 * rayon;
		g2d.drawOval(x - rayon, y - rayon, diametre, diametre);
	}

	public static void drawCircle(Graphics2D g2d, double x, double y, int rayon) {
		drawCircle(g2d, (int) x, (int) y, rayon);
	}

	public static void drawBigPoint(Graphics2D g2d, double x, double y) {
		drawCircle(g2d, x, y, 1);
		drawCircle(g2d, x, y, 2);
		drawCircle(g2d, x, y, 3);
	}

	public static void drawBigPoint(Graphics2D g2d, int x, int y) {
		drawCircle(g2d, x, y, 1);
		drawCircle(g2d, x, y, 2);
		drawCircle(g2d, x, y, 3);
	}

	public static Point pol2cart(int r, double theta) {
		theta = Math.toRadians(theta);
		return new Point((int) (r * Math.cos(theta)), (int) (r * Math.sin(theta)));
	}

	public static BufferedImage resize(BufferedImage src, int targetWidth, int targetHeight) {
		final double scaleW = (double) targetWidth / (double) src.getWidth();
		final double scaleH = (double) targetHeight / (double) src.getHeight();
		final double scale = scaleW < scaleH ? scaleW : scaleH;
		final BufferedImage result = new BufferedImage((int) (src.getWidth() * scale), (int) (src.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = result.createGraphics();
		g2d.drawImage(src, 0, 0, result.getWidth(), result.getHeight(), null);
		g2d.dispose();
		return result;
	}

	public static BufferedImage rotate270(BufferedImage image) {
		// https://stackoverflow.com/questions/20959796/rotate-90-degree-to-right-image-in-java
		final int width = image.getWidth();
		final int height = image.getHeight();
		final BufferedImage result = new BufferedImage(height, width, image.getType());
		final Graphics2D g2d = result.createGraphics();
		g2d.translate((width - height) / 2, (width - height) / 2);
		g2d.rotate(-Math.PI/2, height / 2, width / 2);
		g2d.drawRenderedImage(image, null);
		return result;
	}

	///////////////////////////////////////////////////
	// Pour limiter un Point dans une Dimension
	///////////////////////////////////////////////////

	public static Point limit(Point p, Dimension d) {
		if (p.x < 0) p.x = 0;
		if (p.y < 0) p.y = 0;
		if (p.x > d.width) p.x = d.width;
		if (p.y > d.height) p.y = d.height;
		return p;
	}

	///////////////////////////////////////////////////
	// Pour dessiner une flèche
	///////////////////////////////////////////////////

	public static void drawArrow(Graphics2D g2d, Point start, Point end, Stroke lineStroke, Stroke arrowStroke, float arrowSize) {

		final double startx = start.getX();
		final double starty = start.getY();

		g2d.setStroke(arrowStroke);
		final double deltax = startx - end.getX();
		final double result;
		if (deltax == 0.0d)
			result = Math.PI / 2;
		else
			result = Math.atan((starty - end.getY()) / deltax) + (startx < end.getX() ? Math.PI : 0);

		final double angle = result;

		final double arrowAngle = Math.PI / 12.0d;

		final double x1 = arrowSize * Math.cos(angle - arrowAngle);
		final double y1 = arrowSize * Math.sin(angle - arrowAngle);
		final double x2 = arrowSize * Math.cos(angle + arrowAngle);
		final double y2 = arrowSize * Math.sin(angle + arrowAngle);

		final double cx = (arrowSize / 2.0f) * Math.cos(angle);
		final double cy = (arrowSize / 2.0f) * Math.sin(angle);

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo(end.getX(), end.getY());
		polygon.lineTo(end.getX() + x1, end.getY() + y1);
		polygon.lineTo(end.getX() + x2, end.getY() + y2);
		polygon.closePath();
		g2d.fill(polygon);

		g2d.setStroke(lineStroke);
		g2d.drawLine((int) startx, (int) starty, (int) (end.getX() + cx), (int) (end.getY() + cy));
	}

	///////////////////////////////////////////////////
	// Pour dessiner des polygones à coins arrondis
	///////////////////////////////////////////////////

	private static final float RADIUS = 6;

	public static GeneralPath getRoundedGeneralPath(Polygon polygon) {
		final List<int[]> l = new ArrayList<int[]>();
		for (int i = 0; i < polygon.npoints; i++) {
			l.add(new int[] { polygon.xpoints[i], polygon.ypoints[i] });
		}
		return getRoundedGeneralPath(l);
	}

	public static GeneralPath getRoundedGeneralPathFromPoints(List<Point> l) {
		l.add(l.get(0));
		l.add(l.get(1));
		final GeneralPath p = new GeneralPath();
		p.moveTo(l.get(0).x, l.get(0).y);
		for (int pointIndex = 1; pointIndex < l.size() - 1; pointIndex++) {
			final Point p1 = l.get(pointIndex - 1);
			final Point p2 = l.get(pointIndex);
			final Point p3 = l.get(pointIndex + 1);
			Point mPoint = calculatePoint(p1, p2);
			p.lineTo(mPoint.x, mPoint.y);
			mPoint = calculatePoint(p3, p2);
			p.curveTo(p2.x, p2.y, p2.x, p2.y, mPoint.x, mPoint.y);
		}
		return p;
	}

	public static GeneralPath getRoundedGeneralPath(List<int[]> l) {
		final List<Point> list = new ArrayList<Point>();
		for (int[] point : l) {
			list.add(new Point(point[0], point[1]));
		}
		return getRoundedGeneralPathFromPoints(list);
	}

	private static Point calculatePoint(Point p1, Point p2) {
		final float arcSize = RADIUS;
		final double d1 = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
		final double per = arcSize / d1;
		final double d_x = (p1.x - p2.x) * per;
		final double d_y = (p1.y - p2.y) * per;
		final int xx = (int) (p2.x + d_x);
		final int yy = (int) (p2.y + d_y);
		return new Point(xx, yy);
	}
}

/*
	public static void main(String args[]) {
		JFrame f = new JFrame("Rounded Corner Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel contentPane = new JPanel() {@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				GradientPaint gp = new GradientPaint(0, 0, getBackground().brighter().brighter(), 0, getHeight(), getBackground().darker().darker());
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				int[][] a = {{10,10},{100,10},{100,30},{30,30},{30,50},{100,50},{100,70},{10,70}};
				GeneralPath p = GraphUtils.getRoundedGeneralPath(Arrays.asList(a));
				g2d.setColor(Color.red);
				g2d.fill(p);
				super.paintComponent(g);
			}
		};

		contentPane.setOpaque(false);
		f.setContentPane(contentPane);
		f.setSize(200, 200);
		f.setVisible(true);
	}
*/

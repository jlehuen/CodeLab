package codelab.modules.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*	Classe du panel graphique
*	@author Jérôme Lehuen
*	@version 20/01/24
*/

public class TurtleCanvas extends JPanel {

	public final int WIDTH = 2000;
	public final int HEIGHT = 1500;

	private ModuleGraphics module;
	private BufferedImage background;
	private BufferedImage background_saved;
	private Rectangle background_rectangle;
	private int background_index = 0;
	private int turtle_index = 1;

	private Graphics2D backgroundG2D;
	private Color color = Color.BLACK;
	private int width = 1;

	private TurtleListener listener;
	private Turtle turtle;

	public boolean DEBUG = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public TurtleCanvas(ModuleGraphics module) {
		this.module = module;
		turtle = new Turtle(this, turtle_index);

		// Background blanc par défaut
		Dimension size = new Dimension(WIDTH, HEIGHT);
		setBackgroundImage(TurtleUtils.createBackground(size, Color.WHITE));

		// Ajout du listener de souris
		listener = new TurtleListener(this, turtle);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(listener);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void init() {
		width = 1;
		color = Color.BLACK;
		turtle.init();
		repaint();
	}

	public void reset() {
		width = 1;
		color = Color.BLACK;
		turtle.reset();
		repaint();
	}

	public void collision() {
		module.codelab.printlnToConsole("COLLISION!", Color.RED);
		module.codelab.halt(); // Arrêter l'exécution du programme => executionCompleted()
		CodeLab.playAudio("audiofiles/game_over.wav");
	}

	public void turtleSwitch() {
		turtle.visible = !turtle.visible;
		repaint();
	}

	public boolean isTurtleVisible() {
		return turtle.visible;
	}

	public void setBackgroundImage(BufferedImage image) {
		int largeur = image.getWidth();
		int hauteur = image.getHeight();
		background_saved = image;
		background_rectangle = new Rectangle(largeur, hauteur);
		clearScreen();
	}

	public void setBackgroundFromSelector(BufferedImage image, int index) {
		setBackgroundImage(image);
		background_index = index;
	}

	public void setTurtleFromSelector(TurtlePack pack, int index) {
		turtle.setTurtle(pack);
		turtle_index = index;
	}

	public BufferedImage getBackgroundImage() {
		return background_saved;
	}

	public Rectangle getBackgroundRectangle() {
		return background_rectangle;
	}

	public int getBackgroundIndex() {
		return background_index;
	}

	public int getTurtleIndex() {
		return turtle_index;
	}

	public TurtlePack getTurtlePack(int index) {
		return module.getTurtlePack(index);
	}

	public void paint(Graphics g) {
		Dimension d = getSize();
		Image offScreenImage = createImage(d.width, d.height);
		Graphics offScreenGraphics = offScreenImage.getGraphics();
		offScreenGraphics.drawImage(background, 0, 0, null);
		if (DEBUG) drawCoordinates(offScreenGraphics);
		drawTurtle(offScreenGraphics);
		g.drawImage(offScreenImage, 0, 0, null);
	}

	private void drawTurtle(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		TurtleUtils.configureRender(g2d);
		turtle.draw(g2d, color, width);
	}

	private void drawCoordinates(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.red);
		g2d.setFont(new Font("Arial", Font.PLAIN, 10));
		g2d.drawString("px", 112, 22);
		g2d.drawString("px", 34, 104);

		for (int x = 100; x < WIDTH; x += 100) {
			g2d.drawLine(x, 0, x, 10);
			g2d.drawString(Integer.toString(x), x - 8, 22);

			for (int y = 100; y < HEIGHT; y += 100) {
				g2d.drawLine(x-2, y, x+2, y);
				g2d.drawLine(x, y-2, x, y+2);
			}
		}

		for (int y = 100; y < HEIGHT; y += 100) {
			g2d.drawLine(0, y, 10, y);
			g2d.drawString(Integer.toString(y), 14, y + 4);
		}
	}

	///////////////////////////////////////////////////
	// Instructions de l'API
	///////////////////////////////////////////////////

	public int clearScreen() {
		background = TurtleUtils.clone(background_saved);
		backgroundG2D = (Graphics2D) background.getGraphics();
		TurtleUtils.configureRender(backgroundG2D);
		repaint();
		return 1;
	}

	public int setColor(int code) {
		switch (code) {
			case 0: color = Color.WHITE; break;
			case 1: color = Color.BLACK; break;
			case 2: color = Color.RED; break;
			case 3: color = Color.GREEN; break;
			case 4: color = Color.BLUE; break;
			case 5: color = Color.CYAN; break;
			case 6: color = Color.YELLOW; break;
			case 7: color = Color.MAGENTA; break;
			default: color = Color.BLACK;
		}
		return 1;
	}

	public int setWidth(int width) {
		this.width = width;
		return 1;
	}

	public int getColor() {
		return turtle.getColor();
	}

	public int getBrightness() {
		return turtle.getBrightness();
	}

	public int turtleForward(int dist) {
		turtle.forward(dist);
		return 1;
	}

	public int turtleTurnRight(int angle) {
		turtle.turn(angle);
		return 1;
	}

	public int turtleTurnLeft(int angle) {
		turtle.turn(-angle);
		return 1;
	}

	public int turtleShow() {
		turtle.visible = true;
		SwingUtilities.invokeLater(() -> ((ToolbarGraph)module.getToolbar()).turtleOn());
		repaint();
		return 1;
	}

	public int turtleHide() {
		turtle.visible = false;
		SwingUtilities.invokeLater(() -> ((ToolbarGraph)module.getToolbar()).turtleOff());
		repaint();
		return 1;
	}

	public int turtleReset() {
		turtle.reset();
		return 1;
	}

	public int turtlePenUp() {
		turtle.pendown = false;
		return 1;
	}

	public int turtlePenDown() {
		turtle.pendown = true;
		return 1;
	}

	public int turtleGoto(int x, int y, int theta) {
		turtle.gotoXY(x, y, theta);
		return 1;
	}

	public int drawPoint(int x, int y) {
		backgroundG2D.setColor(color);
		backgroundG2D.setStroke(new BasicStroke(width));
		backgroundG2D.drawLine(x, y, x, y);
		repaint();
		return 1;
	}

	public int drawLine(int x1, int y1, int x2, int y2) {
		backgroundG2D.setColor(color);
		backgroundG2D.setStroke(new BasicStroke(width));
		backgroundG2D.drawLine(x1, y1, x2, y2);
		repaint();
		return 1;
	}

	public int drawCircle(int x, int y, int rayon) {
		backgroundG2D.setColor(color);
		backgroundG2D.setStroke(new BasicStroke(width));
		backgroundG2D.drawOval(x - rayon, y - rayon, 2 * rayon, 2 * rayon);
		repaint();
		return 1;
	}
}

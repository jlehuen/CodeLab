package codelab.modules.robotics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.xml.sax.Attributes;

import codelab.CodeLab;
import codelab.DataTable;
import codelab.ExceptionManager;
import codelab.controllers.widgets.Numpad;
import codelab.modules.robotics.robots.AbstractRobot;
import codelab.modules.robotics.robots.robotLego.RobotLego;
import codelab.modules.robotics.robots.robotRoomba.RobotRoomba;
import codelab.modules.robotics.robots.robotRovio.RobotRovio;
import codelab.utils.ColorCode;
import codelab.utils.ColorUtils;
import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;
import codelab.utils.xml.XMLObject;
import codelab.utils.xml.XMLParser;
import codelab.utils.xml.XMLUtils;

/**
*	Classe du simulateur de robots mobiles
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public class Simulator extends AbstractObjectPanel implements Runnable, XMLObject {

	private static final long serialVersionUID = 1L;

	private static Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
	// https://books.google.fr/books?id=dOz-UK8Fl_UC&pg=PA22#v=onepage&q&f=false

	public final int SCALE = 87; // Échelle pixel/cm
	private final int FPS = 60; // Images par seconde

	public boolean DEBUG = false;

	private DataTable dataTable;
	private Simulator simulator;
	private ToolbarSim toolbar;

	public void printToConsole(String str, Color color) {
		module.printToConsole(str, color, false);
	}

	public void update_dataTable() {
		// Utilisé dans deplacer et pivoter de AbstractDeplacable
		dataTable.update();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Simulator(ModuleRobotics module, DataTable dataTable) {
		super(module);
		this.toolbar = (ToolbarSim) module.getToolbar();
		this.dataTable = dataTable;
		this.simulator = this;
		//addMouseWheelListener(this);
	}

	public void init() {
		//System.out.print("   Loading robots collection... ");
		//boolean b1, b2 = true;

		 // Les robots prédéfinis
		InputStream stream1 = ResourceUtils.loadInputStream_jar(ModuleRobotics.SYSTEM_ROBOTS_XML);
		//b1 = new XMLParser(this).parse(stream1);
		new XMLParser(this).parse(stream1);

		// Les robots utilisateurs
		//System.out.format("[%s]", ModuleRobotics.USER_ROBOTS_XML);
		if (Utils.fileExists(ModuleRobotics.USER_ROBOTS_XML)) {
			FileInputStream stream2 = ResourceUtils.loadInputStream(ModuleRobotics.USER_ROBOTS_XML);
			//b2 = new XMLParser(this).parse(stream2);
			new XMLParser(this).parse(stream2);
		}

		//if (b1 && b2) System.out.println("DONE");

		loadUserBackgrouds(); // Les fonds utilisateur
		setFondFromIndex(0); // Le premier fond de la liste
		setRobotFromIndex(0); // Le premier robot de la liste

		// Pour ouvrir le menu contexuel
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				if (SwingUtilities.isRightMouseButton(event)) {
					new BackgroundPopupMenu(simulator, event.getX(), event.getY(), scale);
				}
			}
		});
	}

	///////////////////////////////////////////////////
	// Gestion des robots
	///////////////////////////////////////////////////

	private ArrayList<AbstractRobot> robots = new ArrayList<AbstractRobot>();
	public int indexRobot;
	private AbstractRobot robot;

	public void addRobot(AbstractRobot robot) {
		robot.index = robots.size(); // Donner au robot sa position dans la liste
		robots.add(robot);
	}

	public ArrayList<AbstractRobot> getRobots() {
		return robots;
	}

	public AbstractRobot getRobot() {
		return robot;
	}

	public int getIndexRobot() {
		return indexRobot;
	}

	public void setRobot(AbstractRobot robot, int index) {
		if (this.robot != null) {
			// Repositionner le nouveau robot
			robot.x = this.robot.x;
			robot.y = this.robot.y;
			robot.theta = this.robot.theta;
		}
		// Remplacer le robot par le nouveau
		this.robot = robot;
		indexRobot = index;
		if (robot.hasNumPad()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Numpad.INSTANCE.open();
				}
			});
		}
		dataTable.setSystem(robot); // Informe la table du nouveau système à visualiser
		dataTable.update(); // Affiche les premières valeurs
		repaint(); // Utile en cas de changement via l'instruction setRobotConfiguration
	}

	public void setRobotFromIndex(int index) throws ArrayIndexOutOfBoundsException {
		if ((index >= 0) && (index < robots.size())) {
			AbstractRobot robot = robots.get(index);
			setRobot(robot, index);
		}
		else throw new ArrayIndexOutOfBoundsException();
	}

	public int setRobotFromName(String name) {
		for (int index = 0 ; index < robots.size() ; index++) {
			AbstractRobot robot = robots.get(index);
			if (robot.getName().contains(name)) {
				setRobot(robot, index);
				return 0;
			}
		}
		return -1;
	}

	///////////////////////////////////////////////////
	// Gestion des fonds
	///////////////////////////////////////////////////

	private ArrayList<Background> fonds = new ArrayList<Background>();
	public int indexFond;
	public BufferedImage imageFond;
	public Rectangle rectangleFond; // Pour délimiter les tests sur l'image de fond
	public int hauteur;
	public int largeur;

	public void addFond(Background fond) {
		fonds.add(fond);
	}

	public ArrayList<Background> getFonds() {
		return fonds;
	}

	public int getIndexFond() {
		return indexFond;
	}

	public void setBackgroundFromSelector(Background fond, int index) {
		objets = new ArrayList<AbstractDeplacable>(); // Supprimer les objets placés
		setFond(fond, index);
	}

	public void setFondFromIndex(int index) throws ArrayIndexOutOfBoundsException {
		if ((index >= 0) && (index < fonds.size())) {
			Background fond = fonds.get(index);
			setFond(fond, index);
		}
		else throw new ArrayIndexOutOfBoundsException();
	}

	public int setBackgroundFromName(String name) {
		for (int index = 0 ; index < fonds.size() ; index++) {
			Background fond = fonds.get(index);
			if (fond.getName().contains(name)) {
				setFond(fond, index);
				return 0;
			}
		}
		return -1;
	}

	private void setFond(Background fond, int index) {
		imageFond = fond.getImage();
		indexFond = index;
		largeur = imageFond.getWidth();
		hauteur = imageFond.getHeight();
		rectangleFond = new Rectangle(largeur, hauteur);
		repaint(); // Utils en cas de changement via l'instruction setBackground
		updateViewport();
	}

	private void loadUserBackgrouds() {
		String dir = ModuleRobotics.USER_BACK_FOLDER;
		if (!Utils.folderExists(dir)) return;
		for (File file : MyFileUtils.getFolderFiles(dir)) {
			boolean isImage = MyFileUtils.getExtension(file.getName()).equals(".png");
			if (file.isFile() && isImage) fonds.add(new Background(file));
		}
	}

	///////////////////////////////////////////////////
	// Gestion des objets (obstacles et balises)
	///////////////////////////////////////////////////

	private ArrayList<AbstractDeplacable> objets = new ArrayList<AbstractDeplacable>();

	public void addObject(AbstractDeplacable objet) {
		objets.add(objet);
		repaint();
	}

	public void removeObject(AbstractDeplacable objet) {
		objets.remove(objet);
		repaint();
	}

	public AbstractDeplacable findObject(Point point) {
		if (robot.contains(point)) return robot;
		if (objets.isEmpty()) return null;
		for (AbstractDeplacable obstacle : objets)
			if (obstacle.contains(point)) return obstacle;
		return null;
	}

	public AbstractDeplacable findObjectHandle(Point point) {
		if (robot.containsHandle(point)) return robot;
		if (objets.isEmpty()) return null;
		for (AbstractDeplacable obstacle : objets)
			if (obstacle.containsHandle(point)) return obstacle;
		return null;
	}

	public boolean findObstacleAt(Point point) {
		// Cas 1 = Recherche d'un objet déplacable
		AbstractDeplacable objet = findObject(point);
		if (objet != null && objet.isObstacle()) return true;
		// Cas 2 = Recherche d'un fond de couleur
		if (rectangleFond.contains(point)) {
			int hex = imageFond.getRGB(point.x, point.y);
			int r = (hex & 0xFF0000) >> 16;
			int g = (hex & 0xFF00) >> 8;
			int b = (hex & 0xFF);
			// J'utilise les valeurs RGB pour corriger le "bug de Meriem" (cyan dans le dégradé de gris)...
			if (r < 50 && g > 200 && b > 200 && ColorUtils.rgb2color(hex) == ColorCode.COLOR_CYAN) return true;
		}
		return false;
	}

	public ColorCode findColoredTagAt(Point point) {
		// Recherche d'une balise colorée
		AbstractDeplacable objet = findObject(point);
		if (objet != null && objet.isColoredTag()) {
			return ((ColoredTag) objet).getColor();
		}
		return null;
	}

	///////////////////////////////////////////////////
	// Affichage de la vue
	///////////////////////////////////////////////////

	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g2d = getGraphics2D(graphics);
		g2d.drawImage(imageFond, 0, 0, null);

		if (DEBUG) {
			g2d.setColor(Color.red);
//			g2d.drawString(String.format("%d FPS", FPS), 10, 17);
			g2d.drawString("cm", 95, 22);
			g2d.drawString("cm", 28, 90);

			Rectangle rectangle = module.getViewport().getViewRect();
			int largeur_scaled = (int) ((rectangle.x + rectangle.width) / scale);
			int hauteur_scaled = (int) ((rectangle.y + rectangle.height) / scale);
			// Règle horizontale
			for (int x = SCALE; x < largeur_scaled; x += SCALE) {
				g2d.drawLine(x, 0, x, 10);
				g2d.drawString(Integer.toString(x / SCALE * 10), x - 6, 22);
				// Les petites croix
				for (int y = SCALE; y < hauteur_scaled; y += SCALE) {
					g2d.drawLine(x-1, y, x+1, y);
					g2d.drawLine(x, y-1, x, y+1);
				}
			}
			// Règle verticale
			for (int y = SCALE; y < hauteur_scaled; y += SCALE) {
				g2d.drawLine(0, y, 10, y);
				g2d.drawString(Integer.toString(y / SCALE * 10), 14, y + 3);
			}
		}
		// Les objets et le robot
		for (AbstractDeplacable objet : objets) objet.draw(g2d);
		robot.draw(g2d);
		g2d.dispose();

		// Détection des collisions
		collisionFlag = robot.detectCollision();
	}

	///////////////////////////////////////////////////
	// Méthode de l'interface Runnable
	///////////////////////////////////////////////////

	private Thread simuthread = null;
	private volatile boolean isrunning = false;
	private boolean collisionFlag = false;

	public boolean isRunning() {
		return isrunning;
	}

	public void run() {
		t0 = System.currentTimeMillis();
		while (isrunning && !Thread.currentThread().isInterrupted()) {
			robot.iterate();    // Calculer les données du robot
			dataTable.update(); // Actualiser la table de propriétés
			adjustScrollBars(); // Ajuster les barres de défilement
			repaint();          // Redessiner le robot
			TOOLKIT.sync();     // Synchroniser l'affichage (systèmes Linux)
			delayFPS();         // Attendre en fonction de la valeur de FPS
			if (!isrunning || Thread.currentThread().isInterrupted()) break;
			if (detectCollision()) break;  // Détecter les collisions et stopper la boucle immédiatement
		}
		robot.stop();
		dataTable.update();
	}

	public void start() {
		if (isrunning) return;
		isrunning = true;
		simuthread = new Thread(this);
		simuthread.start();
	}

	public void stop() {
		if (!isrunning) return;
		isrunning = false;
		Thread t = simuthread;
		if (t != null) {
			t.interrupt();
			try {
				t.join(200); // Timeout défensif de 200ms pour ne jamais bloquer l'EDT
			}
			catch (InterruptedException e) {
				ExceptionManager.process(e);
			}
		}
		simuthread = null;
	}

	public void reset() {
		this.stop();
		robot.reset();
		repaint();
	}

	///////////////////////////////////////////////////
	// Calcul du délai en fonction du FPS
	///////////////////////////////////////////////////

	private double t0, t1;
	private double dtms; // Pédiode de rafraichissement en ms
	public double dt; // Pédiode de rafraichissement en secondes

	private void delayFPS() {
		t1 = System.currentTimeMillis();
		dtms = t1 - t0;
		dt = dtms / 1000;
		int sleep = (int) (1000/FPS - dtms);
		sleep = Math.max(sleep, 5); // Even if lagging sleep a little
		Utils.wait(sleep);
		t0 = t1;
	}

	///////////////////////////////////////////////////
	// Détection des collisions
	///////////////////////////////////////////////////

	private boolean detectCollision() {
		if (collisionFlag) {
			collisionFlag = false; // Réarmement immédiat pour éviter l'avalanche de dialogues
			isrunning = false;     // Arrêt immédiat de la boucle du simulateur
			module.codelab.halt(); // Arrêter l'exécution du programme => executionCompleted()
			// Note : l'arrêt du simulateur est déjà orchestré par executionCompleted()
			CodeLab.playAudio("audiofiles/game_over.wav");
			Utils.showMessageDialog("ATTENTION", "Collision !");
			printToConsole("[codelab] Collision !\n", Color.RED);
			return true;
		}
		return false;
	}

	///////////////////////////////////////////////////
	// Gestion du Viewport et des ScrollBars
	///////////////////////////////////////////////////

	private final int MARGE = 100;

	public void updateViewport() {
		// Après une mise à l'échelle
		int largeur_scaled = (int) (largeur * scale);
		int hauteur_scaled = (int) (hauteur * scale);
		setPreferredSize(new Dimension(largeur_scaled, hauteur_scaled));
		module.getViewport().updateUI();
	}

	private void adjustScrollBars() {
		if (listener.mousePressed) return; // Pour ne pas interférer avec le scrooling à la souris

		JViewport viewport = module.getViewport();
		Rectangle viewport_rect = viewport.getViewRect();
		Rectangle robot_rect = robot.getBounds();

		int xr1 = robot_rect.x;
		int yr1 = robot_rect.y;
		int xr2 = xr1 + robot_rect.width;
		int yr2 = yr1 + robot_rect.height;
		int xv1 = (int) (viewport_rect.x / scale) + MARGE;
		int yv1 = (int) (viewport_rect.y / scale) + MARGE;
		int xv2 = (int) (xv1 + viewport_rect.width / scale) - 2 * MARGE;
		int yv2 = (int) (yv1 + viewport_rect.height / scale) - 2 * MARGE;

		if (xr1 < xv1) module.moveHorizontalScrollBar(xr1 - xv1); // Trop à gauche
		if (xr2 > xv2) module.moveHorizontalScrollBar(xr2 - xv2); // Trop à droite
		if (yr1 < yv1) module.moveVerticalScrollBar(yr1 - yv1); // Trop en haut
		if (yr2 > yv2) module.moveVerticalScrollBar(yr2 - yv2); // Trop en bas
	}

	public void moveHorizontalScrollBar(int dx) {
		module.moveHorizontalScrollBar(dx);
	}

	public void moveVerticalScrollBar(int dy) {
		module.moveVerticalScrollBar(dy);
	}

	///////////////////////////////////////////////////
	// Méthode de l'interface MouseWheelListener
	///////////////////////////////////////////////////

	public void mouseWheelMoved(MouseWheelEvent e) {
		// Invoquée par le ObjectListener du Panel
		int pas = e.getWheelRotation();
		double delta;
		if (CodeLab.IS_OSX) delta = (float) pas / 100;
		else delta = (float) pas / 20;
		scale -= delta;
		if (scale > module.SMAX) scale = module.SMAX;
		if (scale < module.SMIN) scale = module.SMIN;
		toolbar.setSlider((int)(scale * 10));
		setScale(scale);
		updateViewport();
		repaint();
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface XMLObject
	///////////////////////////////////////////////////

	public XMLObject add(String element, Attributes attributs) {
		if (element.equals("robot")) {
			if (XMLUtils.contains(attributs, "type", "Lego")) return new RobotLego(this);
			else if (XMLUtils.contains(attributs, "type", "Rovio")) return new RobotRovio(this);
			else if (XMLUtils.contains(attributs, "type", "Roomba")) return new RobotRoomba(this);
			else {
				Utils.fatalError("\nERROR: Robot type unspecifiyed or not allowed: " + XMLUtils.toString(attributs));
				return null;
			}
		}
		else if (element.equals("back")) return new Background(this);
		else return null;
	}

	public void set(String attribut, String valeur) {}
	public void end() {}
}

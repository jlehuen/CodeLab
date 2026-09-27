package codelab.modules.robotics.robots;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.xml.sax.Attributes;

import codelab.AbstractCodeLab;
import codelab.DataTableInterface;
import codelab.modules.robotics.AbstractDeplacable;
import codelab.modules.robotics.Simulator;
import codelab.utils.GraphUtils;
import codelab.utils.xml.XMLObject;

/**
*	Classe abstraite des robots génériques
*	@author Jérôme Lehuen
*	@version 08/11/21
*/

public abstract class AbstractRobot extends AbstractDeplacable implements XMLObject, DataTableInterface {

	protected Simulator simulator;
	protected BufferedImage image3D;
	protected String name; // Pour l'affichage dans le RobotChooser

	public final static int NBSENS = 4; // Nombre maximum de capteurs
	public final static int NBPROP = 100; // Nombre maximum de propriétés

	protected final static int X_RESET = 100;
	protected final static int Y_RESET = 100;

	public int index; // La position du robot dans la liste
	protected AbstractSensor[] capteurs = new AbstractSensor[NBSENS];

	protected int nbProp; // Nombre de propriétés
	protected String[] propKey = new String[NBPROP];
	protected String[] propVal = new String[NBPROP];

	public boolean hasNumPad() {
		for (int i = 0; i < 4; i++) {
			final AbstractSensor sensor = capteurs[i];
			if (sensor != null && sensor.getDescr().equals("NUMPAD"))
				return true;
		}
		return false;
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractRobot(Simulator simulator) {
		super(simulator);
		this.simulator = simulator;

		for (int i = 0; i < NBPROP; i++) {
			propKey[0] = "";
			propVal[0] = "";
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface DataTableInterface
	///////////////////////////////////////////////////

	public int getNbProp() {
		return nbProp;
	}

	public String getPropKey(int i) {
		return " " + propKey[i];
	}

	public String getPropVal(int i) {
		return " " + propVal[i];
	}

	///////////////////////////////////////////////////
	// Autres méhodes publiques
	///////////////////////////////////////////////////

	public final boolean isRobot() {
		return true;
	}

	protected void setImage(String ident) {
		super.setImage(ident);
	}

	public void addSensor(int port, AbstractSensor capteur) {
		if (capteurs[port] == null)
			capteurs[port] = capteur;
		else {
			System.out.println("ERROR: port déjà utilisé");
		}
	}

	public AbstractSensor getSensor(int port) {
		return capteurs[port];
	}

	public String getName() {
		return name;
	}

	public boolean detectCollision() {
		if (super.detectCollision()) return true; // Le robot lui-même
		for (int i = 0; i < 4; i++)
			if (capteurs[i] != null && capteurs[i].detectCollision())
				return true; // Un capteur
		return false;
	}

	public void draw(Graphics2D g2d) {
		super.draw(g2d);
		// Affichage et actualisation des capteurs
		for (int i = 0; i < 4; i++)
			if (capteurs[i] != null)
				capteurs[i].draw(g2d);
	}

	protected void drawMotorRotationCount(Graphics2D g2d, int x, int y, double degres) {
		final double a = Math.toRadians(degres);
		GraphUtils.drawCircle(g2d, x, y, 2);
		GraphUtils.drawCircle(g2d, x, y, 25);
		GraphUtils.drawLine(g2d, x, y, x + 25 * Math.cos(a), y + 25 * Math.sin(a));
		// g2d.drawString(Integer.toString((int)degres), x - 5, y + 35);
	}

	///////////////////////////////////////////////////
	// Méthodes avec surcharge obligatoire
	///////////////////////////////////////////////////

	public abstract String getConfiguration();
	public abstract void iterate();
	public abstract void reset();
	public abstract void stop();

	///////////////////////////////////////////////////
	// Méthodes de l'interface XMLObject
	///////////////////////////////////////////////////

	public XMLObject add(String element, Attributes attributs) {
		return null;
	}

	public void set(String attribut, String valeur) {
		if (attribut.equals("x")) x = Integer.valueOf(valeur);
		if (attribut.equals("y")) y = Integer.valueOf(valeur);
		if (attribut.equals("theta")) theta = Math.toRadians(Integer.valueOf(valeur));
		if (attribut.equals("name_" + AbstractCodeLab.LANG)) name = valeur;
		if (attribut.equals("name")) name = valeur; // Robots utilisateurs
	}

	public void end() {
		simulator.addRobot(this);
	}
}


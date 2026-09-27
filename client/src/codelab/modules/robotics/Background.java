package codelab.modules.robotics;

import java.awt.image.BufferedImage;
import java.io.File;

import org.xml.sax.Attributes;

import codelab.AbstractCodeLab;
import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.xml.XMLObject;

/**
*	Classe des fond du simulateur
*	@author Jérôme Lehuen
*	@version 08/11/21
*/

public class Background implements XMLObject {

	private Simulator simulator;
	private BufferedImage image;
	private String name;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public Background(Simulator simulator) {
		// Pour les fonds du dossier data
		this.simulator = simulator;
	}

	public Background(File file) {
		// Pour les fonds utilisateur
		image = ResourceUtils.loadBufferedImage(file);
		name = MyFileUtils.noExtension(file.getName());
	}

	///////////////////////////////////////////////////

	private void loadImage(String filename) {
		image = ResourceUtils.loadBufferedImageAsRessource("backgrounds/" + filename);
	}

	public BufferedImage getImage() {
		return image;
	}
	public String getName() {
		return name;
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface XMLObject
	///////////////////////////////////////////////////

	public XMLObject add(String element, Attributes attributs) {
		return null;
	}

	public void set(String attribut, String valeur) {
		if (attribut.equals("name_" + AbstractCodeLab.LANG)) name = valeur;
		if (attribut.equals("filename")) loadImage(valeur);
	}

	public void end() {
		simulator.addFond(this);
	}
}

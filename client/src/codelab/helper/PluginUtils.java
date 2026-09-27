package codelab.helper;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import codelab.AbstractModule;
import codelab.ExceptionManager;

/**
*	Classe utilitaire pour les plugins
*	@author Jérôme Lehuen
*	@version 02/03/21
*/

public class PluginUtils {

	public static ImageIcon loadImageIcon(AbstractModule module, String filename) {
		return new ImageIcon(module.getClass().getResource("/data/" + filename));
	}

	public static BufferedImage loadBufferedImage(AbstractModule module, String filename) {
		try {
			return ImageIO.read(module.getClass().getResourceAsStream("/data/" + filename));
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}
}

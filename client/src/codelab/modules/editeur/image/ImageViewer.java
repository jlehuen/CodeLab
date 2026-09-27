package codelab.modules.editeur.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;

import javax.swing.tree.DefaultMutableTreeNode;

import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;

/**
*	Classe du viewer d'images
*	@author Jérôme Lehuen
*	@version 21/12/23
*/

public class ImageViewer extends AbstractEditor implements MouseWheelListener {

	private BufferedImage image, resizedImage;
	private double scale = 1.0;

	private boolean img_flag = true; // Flag pour les images
	private String descr = ""; // Description pour les non-images

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public ImageViewer(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		this.module = module;
		toolbar = new ToolBarViewer(module, this);
		addMouseWheelListener(this);
		editable = false;
	}

	public ImageViewer(ModuleEditor module, DefaultMutableTreeNode node, String filename, String descr) {
		super(node);
		this.module = module;
		toolbar = new ToolBarViewer(module, this);
		editable = false;

		img_flag = false; // Cas des matrices Numpy par exemple
		image = ResourceUtils.loadBufferedImageAsRessource("icons/" + filename);
		resizedImage = image;
		this.descr = descr;
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void loadImage(File file) {
		image = ResourceUtils.loadBufferedImage(file);
		resizedImage = image; // Pour le centrage
	}

	private void resizeImage() {
		int newWidth = (int) (image.getWidth() * scale);
		int newHeight = (int) (image.getHeight() * scale);
		Image scaledInstance = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
		resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		resizedImage.getGraphics().drawImage(scaledInstance, 0, 0, null);
	}

	///////////////////////////////////////////////////
	// Méthode d'affichage
	///////////////////////////////////////////////////

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image == null) return;
		int x = (getWidth() - resizedImage.getWidth()) / 2;
		int y = (getHeight() - resizedImage.getHeight()) / 2;
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.drawImage(resizedImage, x, y, this);
		g2d.setFont(new Font("Verdana", Font.PLAIN, 14));
		g2d.drawString("Read only file (not editable)", 10, 30);
		if (img_flag) {
			// Affichage du nom et de la taille de l'image
			String txt2 = String.format("Image size: %dx%d", image.getWidth(), image.getHeight());
			g2d.drawString(txt2, 10, 50);
		}
		else g2d.drawString(descr, 10, 50); // Affichage de la description (.npy par exemple)
		g2d.dispose();
	}

	///////////////////////////////////////////////////////////
	// Méthode de l'interface MouseWheelListener
	///////////////////////////////////////////////////////////

	public void mouseWheelMoved(MouseWheelEvent e) {
		int rot = e.getWheelRotation();
		if (rot > 0) scale += 0.1;
		if (rot < 1 && scale > 0.2) scale -= 0.1;
		resizeImage();
		repaint();
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor
	///////////////////////////////////////////////////

	public synchronized void load_content() {
		if (img_flag) {
			File file = getFile();
			if (file == null) return;
			loadImage(file);
		}
	}

	public synchronized void load_template(File file) {
		// Utilisé pour les dropped files
		if (file == null) return;
		File dest = getFile();
		MyFileUtils.copyFile(file, dest);
		if (img_flag) loadImage(file);
	}

	public synchronized void save_content() {}
	public synchronized void save_content(File file) {}
	public synchronized void load_content(File file) {}
	public synchronized void load_backup(int indice) {}
	public File save_backup() { return null; }
	public boolean isBlank() { return false; }
	public boolean isTextEditor() { return false; }
	public boolean compile_file() { return false; }
	public boolean convertSpaceToTab(int nbspaces) { return false; }
	public boolean convertTabToSpace(int nbspaces) { return false; }
	public void changeFont(String name, int size) {}
	public void setInvisible(boolean value) {}
	public void setCodeFoldingEnabled(boolean value) {}
	public void setEditableConfiguration(boolean value) {}
	public void addLineHighlight(int line, Color color) {}
	public void removeAllLineHighlights() {}
}

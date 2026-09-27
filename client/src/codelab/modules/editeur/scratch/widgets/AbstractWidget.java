package codelab.modules.editeur.scratch.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.modules.editeur.scratch.ScratchEditor;
import codelab.utils.GraphUtils;
import codelab.utils.Utils;

/**
*	Classe abstraite des widgets
*	@author Jérôme Lehuen
*	@version 31/01/24
*/

public abstract class AbstractWidget implements Serializable {

	private static final long serialVersionUID = 1L;
	//protected Font defaultFont = new Font("Verdana", Font.PLAIN, 10);

	public int x; // Position du widget en x
	public int y; // Position du widget en y
	protected String uid; // identificateur unique
	protected Color couleur; // Couleur du widget (fixé)
	protected int tailleX; // Largeur du widget (calculé)
	protected int tailleY; // Hauteur du widget (calculé)
	protected Polygon forme; // Forme du widget (calculé)
	protected transient GeneralPath path; // Tracé final du widget (calculé, non sauvegardé)
	protected transient AbstractWidgetPane glasspane; // Le panel d'édition (non sauvegardé)

	protected AbstractWidget suivant = null; // Le widget suivant (ou null)
	protected AbstractWidget precedent = null; // Le widget précédent ou parent (ou null)

	protected boolean error = false; // Flag erreur à l'éxécution
	protected boolean mouseover = false;
	public void mouseover_on() { mouseover = true; }
	public void mouseover_off() { mouseover = false; }
	//protected Image editButton = Utils.loadImageIcon("minikey.png").getImage();

	public boolean isRoot() { return precedent == null; }

	public void setError() { error = true; }
	public void resetError() { error = false; }

	public void setGlassPane(AbstractWidgetPane glasspane) {
		this.glasspane = glasspane;
	}

	protected ScratchEditor getScratchEditor() {
		return glasspane.getEditor();
	}

	protected Rectangle getViewportBorderBounds() {
		return getScratchEditor().getViewportBorderBounds();
	}

	protected double getScale() {
		return glasspane.getScale();
	}

	protected String pass(int indent) {
		// Utilisé dans les méthodes traduire_Python des widgets
		return Utils.tabulation(indent) + "pass";
	}

	protected String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	protected void soundEffectIn() {
		CodeLab.playAudio("audiofiles/widget_in.wav");
	}

	protected void soundEffectOut() {
		CodeLab.playAudio("audiofiles/widget_out.wav");
	}

	///////////////////////////////////////////////////
	// Les méthodes suivantes doivent être
	// définies dans les sous-classe
	///////////////////////////////////////////////////

	public abstract String traduire_Python(int indent, boolean flag);
	public abstract String toString();
	public abstract String toString_1(boolean pythonFlag);
	public abstract void clonerRec(AbstractWidget widget, AbstractWidgetPane panel);
	public abstract AbstractWidget clonerRec(AbstractWidgetPane panel);
	public abstract void pasteRec(AbstractWidgetPane panel);

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractWidget(AbstractWidgetPane glasspane, int x, int y) {
		uid = Integer.toHexString(hashCode());
		this.glasspane = glasspane;
		this.x = x;
		this.y = y;
	}

	public void setPosition(Point p) {
		this.x = p.x;
		this.y = p.y;
	}

	public Point getPosition() {
		return new Point(x, y);
	}

	public Point getPositionSuivant() {
		return new Point(x, y + tailleY);
	}

	// Calcule la position du widget dans le référentiel de l'écran

	public Point getAbsolutePosition() {
		final Point position = new Point(glasspane.getLocation());
		SwingUtilities.convertPointToScreen(position, glasspane);
		int dx = (int) (x * glasspane.getScale());
		int dy = (int) ((y + 25) * glasspane.getScale()); // Hauteur du widget = 25
		dx += glasspane.getEditor().getHorizontalScrollBarOffset();
		dy += glasspane.getEditor().getVerticalScrollBarOffset();
		position.translate(dx, dy);
		return position;
	}

	public void setSuivant(AbstractWidget widget) {
		suivant = widget;
	}

	public void setPrecedent(AbstractWidget widget) {
		precedent = widget;
	}

	public boolean contains(Point2D point) {
		return path.contains(point);
	}

	public AbstractWidget cloner(AbstractWidgetPane panel) {
		return null;
	}

	public AbstractWidget cloner_store(AbstractWidgetPane panel) {
		return null;
	}

	public String getUID() {
		return uid;
	}

	public void valider() {
		// Invoqué par valider() de AbstractEditDialog
		// Choses à faire ??
	}

	///////////////////////////////////////////////////
	// Tracé du widget
	///////////////////////////////////////////////////

	public void draw(Graphics2D g2d) {
		path = GraphUtils.getRoundedGeneralPath(forme);
		final AffineTransform affineTransform = new AffineTransform();
		affineTransform.setToTranslation(x, y);
		path.transform(affineTransform);
		if (mouseover) g2d.setColor(Color.MAGENTA); // Drag and Drop
		else if (error) g2d.setColor(Color.RED); // Erreur à l'exécution
		else g2d.setColor(couleur);
		g2d.fill(path);
		// Pour n'afficher le contour blanc que dans le glasspane
		if (!glasspane.isStore()) {
			g2d.setColor(Color.WHITE);
			g2d.draw(path);
		}
	}

	public void drawOver(Graphics2D g2d) {
		// Surchargé dans les sous-classes
		// Pour dessiner au dessus des widgets
	}

	protected void drawArrow(Graphics2D g2d, int x, int y) {
		// Une petite flèche pour indiquer un emplacement
		g2d.setColor(Color.MAGENTA);
		final BasicStroke bs = new BasicStroke(3f);
		GraphUtils.drawArrow(g2d, new Point(x - 40, y), new Point(x, y), bs, bs, 20);
	}
	protected void repaint() {
		// Invoqué dans les méthodes valider() des éditeurs de widgets
		glasspane.repaint();
	}

	///////////////////////////////////////////////////
	// Gestion des widgets voisins
	///////////////////////////////////////////////////

	protected AbstractWidget getDernier() {
		if (suivant == null) return this;
		else return suivant.getDernier();
	}

	public AbstractWidget getRacine() {
		if (precedent == null) return this;
		else return precedent.getRacine();
	}

	protected int getTotalSize() {
		if (suivant == null) return tailleY;
		else return tailleY + suivant.getTotalSize();
	}

	protected String getWidgetIdent(boolean commentFlag) {
		if (!commentFlag) return "";
		return String.format(" # Widget_%s", uid);
	}

	///////////////////////////////////////////////////
	// Méthodes surchargée dans les sous-classe
	///////////////////////////////////////////////////

	public void survole_par(AbstractWidget widget) {}
	public void deplacer(int dx, int dy, boolean keep_inside) {}
	public void moveToFront() {}
	public void deleteRec() {}
	public void prendre() {}
	public void accepte(AbstractWidget widget) {}
	public void libere(AbstractWidget widget) {}
	public void reorganize() {}

	public abstract void saveXML(FileWriter fw) throws IOException;
	public abstract void set(String attribut, String valeur) throws Exception;

	///////////////////////////////////////////////////

	public void insertSuivant(AbstractWidget widget) {
		final AbstractWidget dernier = widget.getDernier(); // Le dernier widget du bloc à insérer
		if (suivant != null) {
			dernier.setSuivant(suivant);
			suivant.setPrecedent(dernier);
		}
		setSuivant(widget);
		widget.setPrecedent(this);
		soundEffectIn();
	}

	///////////////////////////////////////////////////

	public void deposer() {
		if (precedent != null) {
			precedent.libere(this); // Dépend du type du widget précédent
			precedent = null;
		}
		if (x < 0 || y < 0) { // Dépot hors de la zone du GlassPane
			if (!supprimer()) {
				x = 10;
				y = 10;
				reorganize();
			}
		}
	}

	public boolean supprimer() {
		final int reply = JOptionPane.showConfirmDialog(
			CodeLab.FRAME,
			LABEL("Confirm_block_del"),
			LABEL("Delete_block"),
			JOptionPane.YES_NO_OPTION);

		if (reply == JOptionPane.NO_OPTION) return false;
		deleteRec();
		return true;
	}

	///////////////////////////////////////////////////
	// Fenêtre d'édition
	///////////////////////////////////////////////////

	public void showEditDialog() {
	}
}

package codelab.modules.editeur.scratch.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import codelab.modules.editeur.scratch.AbstractWidgetPane;
import codelab.utils.GraphUtils;
import codelab.utils.Utils;

/**
*	Classe abstraite des structures simples
*	@author Jérôme Lehuen
*	@version 31/01/24
*/

public abstract class AbstractBlocSimple extends AbstractWidget {

	private static final long serialVersionUID = 1L;

	protected int[] tabX_init = new int[] {0,5,30,35,45,50,130,135,135,130,55,50,40,35,10, 5, 5,10,35,40,50,55,130,135,135,130,50,45,35,30, 5, 0};
	protected int[] tabY_init = new int[] {5,0, 0, 5, 5, 0,  0,  5, 20, 25,25,30,30,25,25,30,35,40,40,40,40,40, 40, 45, 50, 55,55,60,60,55,55,50};

	protected AbstractWidget enfant = null; // Le premier widget enfant du bloc (ou null)

	protected boolean focus_suivant;
	protected boolean focus_enfant;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractBlocSimple(AbstractWidgetPane glasspane, int x, int y) {
		super(glasspane, x, y);
		updatePolygon(0, 0);
	}

	public void setEnfant(AbstractWidget widget) {
		enfant = widget;
	}

	// Redimentionnement du polygone invoqué par la méthode reorganize()

	protected void updatePolygon(int dx, int dy) {
		// Recopie des coordonnées de base
		final int[] tabX = Utils.clone(tabX_init);
		final int[] tabY = Utils.clone(tabY_init);
		// Adaptation des coordonnées
		for (int i = 6; i < 10; i++) tabX[i] += dx;
		for (int i = 22; i < 26; i++) tabX[i] += dx;
		for (int i = 16; i < tabY.length; i++) tabY[i] += dy;
		// Adapter la largeur de l'indentation
		tabX[10] += glasspane.indent;
		tabX[11] += glasspane.indent;
		tabX[12] += glasspane.indent;
		tabX[13] += glasspane.indent;
		tabX[14] += glasspane.indent;
		tabX[15] += glasspane.indent;
		tabX[16] += glasspane.indent;
		tabX[17] += glasspane.indent;
		// Calcul des dimensions
		tailleX = Utils.max(tabX) - Utils.min(tabX);
		tailleY = Utils.max(tabY) - Utils.min(tabY) - 5; // Taille de la poignée
		// Construction de la forme
		forme = new Polygon(tabX, tabY, tabX.length);
	}

	public void updatePolygonWidth(Graphics2D g2d) {
		final int[] tabX = forme.xpoints;
		final int[] tabY = forme.ypoints;
		final int width = g2d.getFontMetrics().stringWidth(this.toString_1(glasspane.pythonFlag)) + 25; // Largeur du bloc
		tabX[6] = tabX[0] + width;
		tabX[7] = tabX[0] + width + 5;
		tabX[8] = tabX[0] + width + 5;
		tabX[9] = tabX[0] + width;
		tabX[22] = tabX[0] + width;
		tabX[23] = tabX[0] + width + 5;
		tabX[24] = tabX[0] + width + 5;
		tabX[25] = tabX[0] + width;
		forme = new Polygon(tabX, tabY, tabX.length);
	}

	public void updatePolygonHandles() {
		final int[] tabX = forme.xpoints;
		final int[] tabY = forme.ypoints;
		if (enfant == null) {
			tabY[11] = tabY[10];
			tabY[12] = tabY[10];
		} else {
			tabY[11] = tabY[10] + 5;
			tabY[12] = tabY[10] + 5;
		}
		if (suivant == null) {
			tabY[27] = tabY[26];
			tabY[28] = tabY[26];
		} else {
			tabY[27] = tabY[26] + 5;
			tabY[28] = tabY[26] + 5;
		}
		forme = new Polygon(tabX, tabY, tabX.length);
	}

	private Point getPositionEnfant() {
		int xx = x + 5 + glasspane.indent; // Indentation barre verticale
		return new Point(xx, y + 25);
	}

	///////////////////////////////////////////////////
	// Modifications de la structure
	///////////////////////////////////////////////////

	public void accepte(AbstractWidget widget) {
		if (focus_suivant) {
			focus_suivant = false;
			insertSuivant(widget);
			reorganize(); // Redimentionnement du widget
			getRacine().reorganize(); // Réorganisation de l'arbre
		}
		if (focus_enfant) {
			focus_enfant = false;
			insertEnfant(widget);
			reorganize(); // Redimentionnement du widget
			getRacine().reorganize(); // Réorganisation de l'arbre
		}
	}

	private void insertEnfant(AbstractWidget widget) {
		final AbstractWidget dernier = widget.getDernier(); // Le dernier widget du bloc à insérer
		if (enfant != null) {
			dernier.setSuivant(enfant);
			enfant.setPrecedent(dernier);
		}
		setEnfant(widget);
		widget.setPrecedent(this);
		soundEffectIn();
	}

	public void libere(AbstractWidget widget) {
		if (suivant == widget) suivant = null;
		else if (enfant == widget) enfant = null;
		reorganize(); // Redimentionnement du widget
		getRacine().reorganize(); // Réorganisation de l'arbre
		soundEffectOut();
	}

	public void reorganize() {
		if (enfant == null)
			updatePolygon(0, 0);
		else {
			enfant.setPosition(getPositionEnfant());
			enfant.reorganize();
			updatePolygon(0, enfant.getTotalSize() - 15);
		}
		if (suivant != null) {
			suivant.setPosition(getPositionSuivant());
			suivant.reorganize();
		}
	}

	public void survole_par(AbstractWidget widget) {
		focus_enfant = accepteEnfant(widget);
		if (this instanceof Widget_MAIN) return; // Un widget MAIN n'a pas de suivant
		if (this instanceof Widget_FUNCTION) return; // Un widget FUNCTION n'a pas de suivant
		focus_suivant = accepteSuivant(widget);
	}

	protected boolean accepteSuivant(AbstractWidget widget) {
		final Rectangle zone = new Rectangle(x, y + tailleY - 15, tailleX, 15);
		return zone.contains(widget.getPosition());
	}

	protected boolean accepteEnfant(AbstractWidget widget) {
		final Rectangle zone = new Rectangle(x, y, tailleX, 25);
		return zone.contains(widget.getPosition());
	}

	///////////////////////////////////////////////////
	// Tracé du widget
	///////////////////////////////////////////////////

	public void draw(Graphics2D g2d) {
		updatePolygonHandles(); // Adapter la forme des poignées
		updatePolygonWidth(g2d); // Adapter la largeur au texte (la fonte est définie dans g2d)
		super.draw(g2d); // Pour tracer la forme
		g2d.setColor(Color.BLACK);
		if (error) g2d.setColor(Color.WHITE); // Erreur à l'exécution
		g2d.drawString(toString_1(glasspane.pythonFlag), x + 10, y + 17);
	}

	public void drawOver(Graphics2D g2d) {
		super.drawOver(g2d);
		if (focus_suivant) drawArrow(g2d, x, y + tailleY);
		if (focus_enfant) drawArrow(g2d, x, y + 25);
		// Visualisation des accroches
		// if (suivant == null) drawBulletSuivant(g2d);
		// if (enfant == null) drawBulletEnfant(g2d);
	}

	protected void drawBulletSuivant(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		final Point p = getPositionSuivant();
		GraphUtils.drawBigPoint(g2d, p.x + 40, p.y + 2);
	}

	protected void drawBulletEnfant(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		final Point p = getPositionEnfant();
		GraphUtils.drawBigPoint(g2d, p.x + 40, p.y + 2);
	}

	///////////////////////////////////////////////////

	public void prendre() {
		moveToFront();
		mouseover_on();
		if (suivant != null) suivant.prendre();
		if (enfant != null) enfant.prendre();
	}

	public void deplacer(int dx, int dy, boolean keep_inside) {
		if (isRoot() && keep_inside) {
			// Pour ne pas sortir de la zone de travail
			Rectangle rect = getViewportBorderBounds();
			double scale = getScale();
			double xmax = rect.width - 50 * scale;
			double ymax = rect.height - 50 * scale;
			int x2 = (int) (x * scale) + dx;
			int y2 = (int) (y * scale) + dy;
			if (x2 > xmax) dx = 0; // On peut remettre dans le store
			if (y2 > ymax || y2 < 0) dy = 0;
		}
		x += dx;
		y += dy;
		if (suivant != null) suivant.deplacer(dx, dy, keep_inside);
		if (enfant != null) enfant.deplacer(dx, dy, keep_inside);
	}

	public void moveToFront() {
		glasspane.moveToFront(this);
		if (suivant != null) suivant.moveToFront();
		if (enfant != null) enfant.moveToFront();
	}

	public void deleteRec() {
		glasspane.remove(this);
		if (suivant != null) suivant.deleteRec();
		if (enfant != null) enfant.deleteRec();
	}

	public void clonerRec(AbstractWidget clone, AbstractWidgetPane panel) {
		if (suivant != null) {
			final AbstractWidget clone_suivant = suivant.clonerRec(panel);
			((AbstractBlocSimple) clone).setSuivant(clone_suivant);
			clone_suivant.setPrecedent(clone);
		}
		if (enfant != null) {
			final AbstractWidget clone_enfant = enfant.clonerRec(panel);
			((AbstractBlocSimple) clone).setEnfant(clone_enfant);
			clone_enfant.setPrecedent(clone);
		}
	}

	public void pasteRec(AbstractWidgetPane panel) {
		panel.add(this);
		if (suivant != null) suivant.pasteRec(panel);
		if (enfant != null) enfant.pasteRec(panel);
	}
}

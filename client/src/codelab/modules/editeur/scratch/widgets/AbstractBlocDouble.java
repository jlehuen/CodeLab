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
*	Classe abstraite des structures doubles
*	@author Jérôme Lehuen
*	@version 31/01/24
*/

public abstract class AbstractBlocDouble extends AbstractWidget {

	private static final long serialVersionUID = 1L;

	protected int[] tabX_init = new int[] {0,5,30,35,45,50,130,135,135,130,55,50,40,35,10, 5, 5,10,35,40,50,55,130,135,135,130,55,50,40,35,10, 5, 5,10,35,40,50,55,130,135,135,130,50, 45, 35,30, 5, 0};
	protected int[] tabY_init = new int[] {5,0, 0, 5, 5, 0,  0,  5, 20, 25,25,30,30,25,25,30,35,40,40,40,40,40, 40, 45, 60, 65,65,70,70,65,65,70,75,80,80,80,80,80, 80, 85, 90, 95,95,100,100,95,95,90};

	protected AbstractWidget enfant1 = null; // Le premier widget enfant du premier bloc (ou null)
	protected AbstractWidget enfant2 = null; // Le premier widget enfant du second bloc (ou null)

	protected boolean focus_suivant;
	protected boolean focus_enfant1;
	protected boolean focus_enfant2;

	///////////////////////////////////////////////////
	// Les méthodes suivantes doivent être
	// définies dans les sous-classe
	///////////////////////////////////////////////////

	public abstract String toString_1(boolean pythonFlag);
	public abstract String toString_2(boolean pythonFlag);

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AbstractBlocDouble(AbstractWidgetPane glasspane, int x, int y) {
		super(glasspane, x, y);
		updatePolygon(0, 0, 0);
	}

	public void setEnfant1(AbstractWidget widget) {
		enfant1 = widget;
	}

	public void setEnfant2(AbstractWidget widget) {
		enfant2 = widget;
	}

	// Redimentionnement du polygone invoqué par la méthode reorganize()

	protected void updatePolygon(int dx, int dy1, int dy2) {
		// Recopie des coordonnées de base
		final int[] tabX = Utils.clone(tabX_init);
		final int[] tabY = Utils.clone(tabY_init);
		// Adaptation des coordonnées
		for (int i = 6; i < 10; i++) tabX[i] += dx;
		for (int i = 22; i < 26; i++) tabX[i] += dx;
		for (int i = 38; i < 42; i++) tabX[i] += dx;
		for (int i = 16; i < tabY.length; i++) tabY[i] += dy1;
		for (int i = 32; i < tabY.length; i++) tabY[i] += dy2;
		// Adapter la largeur de l'indentation
		tabX[10] += glasspane.indent;
		tabX[11] += glasspane.indent;
		tabX[12] += glasspane.indent;
		tabX[13] += glasspane.indent;
		tabX[14] += glasspane.indent;
		tabX[15] += glasspane.indent;
		tabX[16] += glasspane.indent;
		tabX[17] += glasspane.indent;
		tabX[26] += glasspane.indent;
		tabX[27] += glasspane.indent;
		tabX[28] += glasspane.indent;
		tabX[29] += glasspane.indent;
		tabX[30] += glasspane.indent;
		tabX[31] += glasspane.indent;
		tabX[32] += glasspane.indent;
		tabX[33] += glasspane.indent;
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
		tabX[38] = tabX[0] + width;
		tabX[39] = tabX[0] + width + 5;
		tabX[40] = tabX[0] + width + 5;
		tabX[41] = tabX[0] + width;
		forme = new Polygon(tabX, tabY, tabX.length);
	}

	public void updatePolygonHandles() {
		final int[] tabX = forme.xpoints;
		final int[] tabY = forme.ypoints;
		if (enfant1 == null) {
			tabY[11] = tabY[10];
			tabY[12] = tabY[10];
		} else {
			tabY[11] = tabY[10] + 5;
			tabY[12] = tabY[10] + 5;
		}
		if (enfant2 == null) {
			tabY[27] = tabY[26];
			tabY[28] = tabY[26];
		} else {
			tabY[27] = tabY[26] + 5;
			tabY[28] = tabY[26] + 5;
		}
		if (suivant == null) {
			tabY[43] = tabY[42];
			tabY[44] = tabY[42];
		} else {
			tabY[43] = tabY[42] + 5;
			tabY[44] = tabY[42] + 5;
		}
		forme = new Polygon(tabX, tabY, tabX.length);
	}

	protected int get2ndBlocOffset() {
		final int[] tabY = forme.ypoints;
		return tabY[18] - tabY[2]; // Distance entre les deux barres
	}

	private Point getPositionEnfant1() {
		int xx = x + 5 + glasspane.indent; // Indentation barre verticale
		return new Point(xx, y + 25);
	}

	private Point getPositionEnfant2() {
		int xx = x + 5 + glasspane.indent; // Indentation barre verticale
		if (enfant1 == null)
			return new Point(xx, y + 65);
		else
			return new Point(xx, y + enfant1.getTotalSize() + 50);
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
		if (focus_enfant1) {
			focus_enfant1 = false;
			insertEnfant1(widget);
			reorganize(); // Redimentionnement du widget
			getRacine().reorganize(); // Réorganisation de l'arbre
		}
		if (focus_enfant2) {
			focus_enfant2 = false;
			insertEnfant2(widget);
			reorganize(); // Redimentionnement du widget
			getRacine().reorganize(); // Réorganisation de l'arbre
		}
	}

	private void insertEnfant1(AbstractWidget widget) {
		final AbstractWidget dernier = widget.getDernier(); // Le dernier widget du bloc à insérer
		if (enfant1 != null) {
			dernier.setSuivant(enfant1);
			enfant1.setPrecedent(dernier);
		}
		setEnfant1(widget);
		widget.setPrecedent(this);
		soundEffectIn();
	}

	private void insertEnfant2(AbstractWidget widget) {
		final AbstractWidget dernier = widget.getDernier(); // Le dernier du bloc à insérer
		if (enfant2 != null) {
			dernier.setSuivant(enfant2);
			enfant2.setPrecedent(dernier);
		}
		setEnfant2(widget);
		widget.setPrecedent(this);
		soundEffectIn();
	}

	public void libere(AbstractWidget widget) {
		if (suivant == widget) suivant = null;
		else if (enfant1 == widget) enfant1 = null;
		else if (enfant2 == widget) enfant2 = null;
		reorganize(); // Redimentionnement du widget
		getRacine().reorganize(); // Réorganisation de l'arbre
		soundEffectOut();
	}

	public void reorganize() {
		int dy1 = 0;
		int dy2 = 0;
		if (enfant1 != null) {
			enfant1.setPosition(getPositionEnfant1());
			enfant1.reorganize();
			dy1 = enfant1.getTotalSize() - 15; // Hauteur de l'enfant au final
		}
		if (enfant2 != null) {
			enfant2.setPosition(getPositionEnfant2());
			enfant2.reorganize();
			dy2 = enfant2.getTotalSize() - 15; // Hauteur de l'enfant au final
		}
		updatePolygon(0, dy1, dy2);
		if (suivant != null) {
			suivant.setPosition(getPositionSuivant());
			suivant.reorganize();
		}
	}

	public void survole_par(AbstractWidget widget) {
		focus_suivant = accepteSuivant(widget);
		focus_enfant1 = accepteEnfant1(widget);
		focus_enfant2 = accepteEnfant2(widget);
	}

	protected boolean accepteSuivant(AbstractWidget widget) {
		final Rectangle zone = new Rectangle(x, y + tailleY - 15, tailleX, 15);
		return zone.contains(widget.getPosition());
	}

	protected boolean accepteEnfant1(AbstractWidget widget) {
		final Rectangle zone = new Rectangle(x, y, tailleX, 25);
		return zone.contains(widget.getPosition());
	}

	protected boolean accepteEnfant2(AbstractWidget widget) {
		final Point p = getPositionEnfant2();
		final Rectangle zone = new Rectangle(x, p.y - 15, tailleX, 15);
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
		g2d.drawString(toString_2(glasspane.pythonFlag), x + 10, y + get2ndBlocOffset() + 17);
	}

	public void drawOver(Graphics2D g2d) {
		super.drawOver(g2d);
		if (focus_suivant) drawArrow(g2d, x, y + tailleY);
		if (focus_enfant1) drawArrow(g2d, x, y + 25);
		if (focus_enfant2) drawArrow(g2d, x, getPositionEnfant2().y);
		// Visualisation des accroches
		// if (suivant == null) drawBulletSuivant(g2d);
		// if (enfant1 == null) drawBulletEnfant1(g2d);
		// if (enfant2 == null) drawBulletEnfant2(g2d);
	}

	protected void drawBulletSuivant(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		final Point p = getPositionSuivant();
		GraphUtils.drawBigPoint(g2d, p.x + 40, p.y + 2);
	}

	protected void drawBulletEnfant1(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		final Point p = getPositionEnfant1();
		GraphUtils.drawBigPoint(g2d, p.x + 40, p.y + 2);
	}

	protected void drawBulletEnfant2(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		final Point p = getPositionEnfant2();
		GraphUtils.drawBigPoint(g2d, p.x + 40, p.y + 2);
	}

	///////////////////////////////////////////////////

	public void prendre() {
		moveToFront();
		mouseover_on();
		if (suivant != null) suivant.prendre();
		if (enfant1 != null) enfant1.prendre();
		if (enfant2 != null) enfant2.prendre();
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
		if (enfant1 != null) enfant1.deplacer(dx, dy, keep_inside);
		if (enfant2 != null) enfant2.deplacer(dx, dy, keep_inside);
	}

	public void moveToFront() {
		glasspane.moveToFront(this);
		if (suivant != null) suivant.moveToFront();
		if (enfant1 != null) enfant1.moveToFront();
		if (enfant2 != null) enfant2.moveToFront();
	}

	public void deleteRec() {
		glasspane.remove(this);
		if (suivant != null) suivant.deleteRec();
		if (enfant1 != null) enfant1.deleteRec();
		if (enfant2 != null) enfant2.deleteRec();
	}

	public void clonerRec(AbstractWidget clone, AbstractWidgetPane panel) {
		if (suivant != null) {
			final AbstractWidget clone_suivant = suivant.clonerRec(panel);
			((AbstractBlocDouble) clone).setSuivant(clone_suivant);
			clone_suivant.setPrecedent(clone);
		}
		if (enfant1 != null) {
			final AbstractWidget clone_enfant1 = enfant1.clonerRec(panel);
			((AbstractBlocDouble) clone).setEnfant1(clone_enfant1);
			clone_enfant1.setPrecedent(clone);
		}
		if (enfant2 != null) {
			final AbstractWidget clone_enfant2 = enfant2.clonerRec(panel);
			((AbstractBlocDouble) clone).setEnfant2(clone_enfant2);
			clone_enfant2.setPrecedent(clone);
		}
	}

	public void pasteRec(AbstractWidgetPane panel) {
		panel.add(this);
		if (suivant != null) suivant.pasteRec(panel);
		if (enfant1 != null) enfant1.pasteRec(panel);
		if (enfant2 != null) enfant2.pasteRec(panel);
	}
}

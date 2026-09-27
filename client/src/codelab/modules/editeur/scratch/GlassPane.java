package codelab.modules.editeur.scratch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JViewport;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.modules.editeur.scratch.widgets.AbstractWidget;
import codelab.modules.editeur.scratch.widgets.Widget_FUNCTION;
import codelab.modules.editeur.scratch.widgets.Widget_MAIN;
import codelab.utils.Utils;

/**
*	Classe du panel de l'éditeur de type Scratch
*	@author Jérôme Lehuen
*	@version 05/02/24
*/

public class GlassPane extends AbstractWidgetPane implements MouseWheelListener {

	private static final Dimension DEFAULT_SIZE = new Dimension(1600, 1200);

	private GlassPaneListener listener;
	private int hauteur; // Dimensions de base pour la mise à l'échelle
	private int largeur;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public GlassPane(ScratchEditor editor, boolean editable) {
		this.editor = editor;
		setSize(DEFAULT_SIZE);
		setBorder(BorderFactory.createEmptyBorder());
		setLayout(null);
		setOpaque(true);
		setVisible(true);

		listener = new GlassPaneListener(this);
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addMouseWheelListener(this);
		setEditable(editable);
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public boolean isStore() { return false; }
	public boolean isEmpty() { return widgets.isEmpty(); }

	public void setEditable(boolean value) {
		listener.setEnable(value);
		if (value) setBackground(Color.WHITE);
		else setBackground(Color.LIGHT_GRAY);
		repaint();
	}

	///////////////////////////////////////////////////
	// Gestion des widgets
	///////////////////////////////////////////////////

	public void mouseDragged(AbstractWidget widget) {
		if (widget instanceof Widget_MAIN) return; // Pour ne pas insérer un widget MAIN
		if (widget instanceof Widget_FUNCTION) return; // Pour ne pas insérer un widget FUNCTION
		for (AbstractWidget w : widgets)
			if (w != widget) w.survole_par(widget);
	}

	public void mouseReleased(AbstractWidget widget) {
		widget.mouseover_off();

		// Pour ne pas placer plusieurs widgets MAIN
		boolean stop = false;
		if (widget instanceof Widget_MAIN) {
			for (AbstractWidget w : widgets) {
				if (w.equals(widget)) continue;
				if (w instanceof Widget_MAIN) {
					Utils.showMessageDialog("ATTENTION", CodeLab.LABEL("GlassPane_1"));
					stop = true;
				}
			}
		}
		if (stop) {
			this.remove(widget);
			return;
		}

		// Insérer le widget
		for (AbstractWidget w : widgets)
			if (w != widget) w.accepte(widget);
	}

	///////////////////////////////////////////////////
	// Mise en évidence d'une erreur dans un widget
	///////////////////////////////////////////////////

	public String setError(String uid) {
		for (AbstractWidget widget : widgets) {
			if (widget.getUID().equals(uid)) {
				widget.setError();
				repaint();
				return widget.toString_1(pythonFlag);
			}
		}
		return "Unidentified widget";
	}

	///////////////////////////////////////////////////
	// Méthodes pour le copier-coller
	///////////////////////////////////////////////////

	public void setWidgetToPaste(AbstractWidget widget) {
		// Vers le ScratchEditor pour faire du copier-coller entre panels
		editor.setWidgetToPaste(widget);
	}

	public AbstractWidget getWidgetToPaste() {
		return editor.getWidgetToPaste();
	}

	public void past(int x, int y) {
		AbstractWidget widget = editor.getWidgetToPaste();
		AbstractWidget clone = widget.clonerRec(this);
		clone.x = x;
		clone.y = y;
		clone.pasteRec(this); // Collage des widgets
		clone.reorganize(); // Placement des widgets
		repaint();
	}

	///////////////////////////////////////////////////
	// Affichage du panel
	///////////////////////////////////////////////////

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = getGraphics2D(g);
		synchronized (widgets) {
			// Pour éviter une java.util.ConcurrentModificationException sur les ArrayList...
			for (AbstractWidget widget : widgets) widget.draw(g2d); // Dessiner les widgets
			for (AbstractWidget widget : widgets) widget.drawOver(g2d); // Tracés au premier plan
		}
		g2d.dispose();
	}

	///////////////////////////////////////////////////
	// Gestion du scrollPane
	///////////////////////////////////////////////////

	public JViewport getViewport() {
		return editor.getViewport();
	}

	public int getHorizontalScrollBarOffset() {
		return editor.getHorizontalScrollBarOffset();
	}

	public int getVerticalScrollBarOffset() {
		return editor.getVerticalScrollBarOffset();
	}

	public void updateViewport() {
		// Après une mise à l'échelle
		int largeur_scaled = (int) (largeur * scale);
		int hauteur_scaled = (int) (hauteur * scale);
		setPreferredSize(new Dimension(largeur_scaled, hauteur_scaled));
		editor.getViewport().updateUI(); // Indispensable...
	}

	public void moveHorizontalScrollBar(int dx) {
		editor.moveHorizontalScrollBar(dx);
	}

	public void moveVerticalScrollBar(int dy) {
		editor.moveVerticalScrollBar(dy);
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseWheelListener
	///////////////////////////////////////////////////

	public void mouseWheelMoved(MouseWheelEvent e) {
		int pas = e.getWheelRotation();
		double delta;
		if (CodeLab.IS_OSX) delta = (float) pas / 100;
		else delta = (float) pas / 20;
		scale -= delta;
		if (scale > ToolBarScratch.SMAX) scale = ToolBarScratch.SMAX;
		if (scale < ToolBarScratch.SMIN) scale = ToolBarScratch.SMIN;
		editor.setScale(scale);
	}

	///////////////////////////////////////////////////
	// Sauvegarde et chargement
	///////////////////////////////////////////////////

	public void saveProgram(File file) {
		try {
			FileWriter out = new FileWriter(file);
			out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			out.write("<scratch>\n\n");
			for (AbstractWidget widget : widgets) {
				if (widget.isRoot()) {
					widget.saveXML(out);
					out.write("<end/>\n\n");
				}
			}
			out.write("</scratch>\n");
			out.close();
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			System.out.println(CodeLab.LABEL("GlassPane_2"));
		}
	}

	public void loadProgram(File file) {
		clear(); // Supprimer tous les widgets
		new XMLLoader(file, this);
		for (AbstractWidget widget : widgets)
			if (widget.isRoot())
				widget.reorganize();
		repaint();
	}

	///////////////////////////////////////////////////
	// Traduction du programme en Python
	///////////////////////////////////////////////////

	public void traduire(File file) {
		try {
			FileWriter writer = new FileWriter(file);
			writer.write("# --------------------------------\n");
			writer.write("# Automatic translation from Blocs\n");
			writer.write("# --------------------------------\n");
			writer.write("from codelab.moduleRobotics.robotLego import *\n");
			writer.write("from codelab.moduleGraphics.graph2D import *\n");
			writer.write("from codelab.utils import *\n");
			writer.write("from math import *\n");

			// Traduction des widgets Function
			for (AbstractWidget widget : widgets)
				if (widget instanceof Widget_FUNCTION)
					traduire(widget, writer);

			// Traduction du widget Main
			Widget_MAIN main = findWidgetMain();
			if (main != null) traduire(main, writer);

			writer.close();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	private void traduire(AbstractWidget widget, FileWriter writer) throws IOException {
		writer.write(widget.traduire_Python(0, true));
	}

	// Pour retrouver le widget Main
	private Widget_MAIN findWidgetMain() {
		for (AbstractWidget widget : widgets)
			if (widget instanceof Widget_MAIN)
				return (Widget_MAIN) widget;
		return null;
	}
}

/*
	///////////////////////////////////////////////////
	// Sérialisation et désérialisation (unused)
	///////////////////////////////////////////////////

	public void saveProgram(File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(widgets);
			oos.close();
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			System.out.println("ERREUR pendant la sérialisation Scratch");
		}
	}

	public void loadProgram(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			ArrayList<AbstractWidget> liste = (ArrayList<AbstractWidget>) ois.readObject();
			ois.close();

			// Ajouter les widgets au GlassPane
			clear();
			for (AbstractWidget widget : liste) {
				widget.setGlassPane(this);
				widget.reorganize();
				add(widget);
			}
			repaint();
		}
		catch (ClassNotFoundException e) { ExceptionManager.process(e); }
		catch (IOException e) {
			//ExceptionManager.process(e);
			System.out.println("ERREUR pendant le désérialisation Scratch");
		}
	}
*/

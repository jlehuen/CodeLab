package codelab.modules.editeur.scratch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.scratch.widgets.AbstractWidget;

/**
*	Classe de l'éditeur Scratch
*	@author Jérôme Lehuen
*	@version 31/01/24
*/

public class ScratchEditor extends AbstractEditor {

	private static final long serialVersionUID = 1L;

	private GlassPane glasspane;
	private WidgetStore store;
	private JScrollPane storesp;
	private JScrollPane sp;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ScratchEditor(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		this.module = module;

		toolbar = new ToolBarScratch(module, this);
		glasspane = new GlassPane(this, editable);
		store = new WidgetStore(this, glasspane, editable);

		// Le JScrollPane du store
		storesp = new JScrollPane(store);
		storesp.getVerticalScrollBar().setUnitIncrement(20); // Incrément de la molette
		storesp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		storesp.getVerticalScrollBar().setPreferredSize(new Dimension(0,0)); // Pas de barre
		storesp.setBorder(null); // Pas de trait
		store.init(); // Premier store

		showLockIcon = true;

		setLayout(new BorderLayout());
		add(storesp, BorderLayout.WEST);
		add(wrapWithLockLayer(sp = new JScrollPane(glasspane)), BorderLayout.CENTER);
		sp.setBorder(BorderFactory.createEmptyBorder());
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public WidgetStore getStore() {
		return store;
	}

	public GlassPane getGlassPane() {
		return glasspane;
	}

	public Rectangle getViewportBorderBounds() {
		return sp.getViewportBorderBounds();
	}

	public boolean isBlank() {
		return glasspane.isEmpty();
	}

	public boolean isTextEditor() { return false; }

	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor
	///////////////////////////////////////////////////

	public synchronized void save_content() {
		// Attention: accès conccurents => synchronized
		glasspane.saveProgram(getFile());
	}

	public synchronized void load_content() {
		// Attention: accès conccurents => synchronized
		glasspane.loadProgram(getFile());
	}

	public synchronized void save_content(File file) {
		// N'est utilisé que dans le cas d'un fichier texte
	}

	public void load_content(File file) {
		// N'est utilisé que dans le cas d'un fichier texte
	}

	public void load_template(File file) {
		glasspane.loadProgram(file);
	}

	public boolean compile_file() {
		// Doit actualiser le flag compilation_ok de AbstractEditor
		String path = getFile().getAbsolutePath();
		File pythonFile = new File(path + ".py");
		glasspane.traduire(pythonFile);
		compilation_ok = true;
		return true;
	}

	// Méthodes non implémentables du TextEditor
	public void changeFont(String name, int size) {}
	public void setInvisible(boolean value) {}
	public void setCodeFoldingEnabled(boolean value) {}
	public void addLineHighlight(int line, Color color) {}
	public void removeAllLineHighlights() {}
	public boolean convertSpaceToTab(int nbspaces) { return false; }
	public boolean convertTabToSpace(int nbspaces) { return false; }

	public void setEditableConfiguration(boolean value) {
		this.editable = value;
		glasspane.setEditable(value);
		store.setEditable(value);
		if (!value) { modified = false; }
		updateLockBadge();
		repaint();
	}

	///////////////////////////////////////////////////
	// Autres méthodes publiques
	///////////////////////////////////////////////////

	public void widgetPaneChanged() {
		// En provenance de AbstractWidgetPane
		modified = true;
	}

	public void setScale(double scale) {
		store.setScale(scale);
		store.updateUI(); // Indispensable...
		glasspane.setScale(scale);
		glasspane.updateUI(); // Indispensable...
		validate(); // Indispensable...
		// Ajuster le slider de la barre d'outils
		((ToolBarScratch)module.getToolbar()).setZoom(scale);
	}

	public void setIndent(int offset) {
		System.out.println(offset);
		SwingUtilities.invokeLater(() -> {
			glasspane.indent = offset;
			glasspane.repaint();
		});
	}

	public void setWidgetToPaste(AbstractWidget widget) {
		// Vers le EditorPane pour faire du copier-coller entre panels
		module.widgetToPaste = widget;
	}

	public AbstractWidget getWidgetToPaste() {
		return module.widgetToPaste;
	}

	public String setError(String uid) {
		// Pour marquer le widget en rouge
		return glasspane.setError(uid);
	}

	///////////////////////////////////////////////////
	// Gestion du scrollPane du store
	///////////////////////////////////////////////////

	public JViewport getViewportStore() {
		return storesp.getViewport();
	}

	public Point getViewPositionStore() {
		return storesp.getViewport().getViewPosition();
	}

	public void moveVerticalScrollBarStore(int dy) {
		JScrollBar vsb = storesp.getVerticalScrollBar();
		vsb.setValue(vsb.getValue() - dy);
	}

	///////////////////////////////////////////////////
	// Gestion du scrollPane du glasspane
	///////////////////////////////////////////////////

	public JViewport getViewport() {
		return sp.getViewport();
	}

	public int getHorizontalScrollBarOffset() {
		JScrollBar hsb = sp.getHorizontalScrollBar();
		return hsb.getValue();
	}

	public int getVerticalScrollBarOffset() {
		JScrollBar vsb = sp.getVerticalScrollBar();
		return vsb.getValue();
	}

	public void moveHorizontalScrollBar(int dx) {
		JScrollBar hsb = sp.getHorizontalScrollBar();
		hsb.setValue(hsb.getValue() + dx);
	}

	public void moveVerticalScrollBar(int dy) {
		JScrollBar vsb = sp.getVerticalScrollBar();
		vsb.setValue(vsb.getValue() + dy);
	}
}

package codelab.modules.editeur;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import codelab.CodeLab;
import codelab.modules.editeur.text.ToolBarEdit;
import codelab.utils.ResourceUtils;

/**
*	Classe de l'éditeur vide
*	@author Jérôme Lehuen
*	@version 19/12/23
*/

public class EmptyEditor extends AbstractEditor {

	private ImageIcon image;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public EmptyEditor(ModuleEditor module, int code) {
		super(null); // Pas de référence vers un node

		toolbar = new ToolBarEdit(module, null);
		// Afficher un fond d'éditeur vide
		String filename = String.format("welcome_%d_%s.png", code, CodeLab.LANG);
		image = ResourceUtils.loadImageIcon(filename);
		JLabel label = new JLabel(image);
		setLayout(new BorderLayout());
		add(label, BorderLayout.CENTER);

		label.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
		});
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor à implémenter
	///////////////////////////////////////////////////

	public boolean isBlank() { return false; }
	public boolean isModified() { return false; }
	public boolean isTextEditor() { return false; }
	public void resetModified() {}
	public void save_content() {}
	public void load_content() {}
	public void load_content(File file) {}
	public void load_template(File file) {}
	public void save_content(File file) {}
	public void memorizeView() {}
	public void repositionView() {}
	public void changeFont(String name, int size) {}
	public void setInvisible(boolean value) {}
	public void setCodeFoldingEnabled(boolean value) {}
	public void addLineHighlight(int line, Color color) {}
	public void removeAllLineHighlights() {}
	public boolean compile_file() { return false; }
	public boolean convertSpaceToTab(int nbspaces) { return false; }
	public boolean convertTabToSpace(int nbspaces) { return false; }

	public void setEditableConfiguration(boolean value) {
		editable = value;
	}
}

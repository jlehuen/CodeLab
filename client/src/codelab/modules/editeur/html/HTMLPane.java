package codelab.modules.editeur.html;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;

import codelab.ExceptionManager;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;

/**
*	Classe du viewer HTML
*	@author Jérôme Lehuen
*	@version 09/12/23
*/

public class HTMLPane extends AbstractEditor {

	private JEditorPane jep;
	private JScrollPane sp;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public HTMLPane(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		this.module = module;
		toolbar = new ToolBarHTML(module, this);
		editable = false;

		jep = new JEditorPane();
 		jep.setEditable(true);
		jep.setContentType("text/html");
 		jep.setBackground(Color.WHITE);

		setLayout(new BorderLayout());
		add(sp = new JScrollPane(jep), BorderLayout.CENTER);
		sp.setBorder(BorderFactory.createEmptyBorder());
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////


	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor
	///////////////////////////////////////////////////

	public synchronized void save_content() {
		File file = getFile();
		Document doc = jep.getDocument();
		HTMLEditorKit kit = (HTMLEditorKit) jep.getEditorKit();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			kit.write(writer, doc, 0, doc.getLength());
		}
		catch (Exception e) {
			ExceptionManager.process(e);
		}
	}

	public synchronized void load_content() {
		File file = getFile();
		if (file == null) return;
		try {
			jep.setPage(file.toURI().toURL());
		}
 		catch (IOException e) {
			jep.setContentType("text/html");
			jep.setText("Imposible de visualiser la page");
		}
	}

	public synchronized void save_content(File file) {
	}

	public synchronized void load_content(File file) {
	}

	public synchronized void load_template(File file) {
		// Utilisé pour les dropped files
		if (file == null) return;
		try {
			jep.setPage(file.toURI().toURL());
		}
 		catch (IOException e) {
			jep.setContentType("text/html");
			jep.setText("<b>Imposible de charger ou de visualiser la page</b>");
		}
	}

	public synchronized void load_backup(int indice) {
	}

	public File save_backup() {
		return null;
	}

	public boolean isBlank() {
		return false;
	}

	public boolean isTextEditor() {
 		return false;
	}

	public boolean compile_file() {
		return false;
	}

	public boolean convertSpaceToTab(int nbspaces) {
		return false;
	}

	public boolean convertTabToSpace(int nbspaces) {
		return false;
	}

	public void changeFont(String name, int size) {
	}

	public void setInvisible(boolean value) {
	}

	public void setCodeFoldingEnabled(boolean value) {
	}

	public void setEditableConfiguration(boolean value) {
	}

	public void addLineHighlight(int line, Color color) {
	}

	public void removeAllLineHighlights() {
	}
}

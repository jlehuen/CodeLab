package codelab.modules.editeur.manager;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import javax.swing.ImageIcon;

import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;

/**
*	Classe des descripteurs de fichier
*	@author Jérôme Lehuen
*	@version 11/01/24
*/

public class FileDescr {

	private File file;
	private UUID uid;

	private boolean modified = false; // Flag de modification exterieure

	public static final ImageIcon icon_c = ResourceUtils.loadImageIcon("icons/mini_icon_c.png");
	public static final ImageIcon icon_go = ResourceUtils.loadImageIcon("icons/mini_icon_go.png");
	public static final ImageIcon icon_lua = ResourceUtils.loadImageIcon("icons/mini_icon_lua.png");
	public static final ImageIcon icon_lisp = ResourceUtils.loadImageIcon("icons/mini_icon_lisp.png");
	public static final ImageIcon icon_java = ResourceUtils.loadImageIcon("icons/mini_icon_java.png");
	public static final ImageIcon icon_clips = ResourceUtils.loadImageIcon("icons/mini_icon_clips.png");
	public static final ImageIcon icon_python = ResourceUtils.loadImageIcon("icons/mini_icon_python.png");
	public static final ImageIcon icon_jupyter = ResourceUtils.loadImageIcon("icons/mini_icon_jupyter.png");
	public static final ImageIcon icon_haskell = ResourceUtils.loadImageIcon("icons/mini_icon_haskell.png");
	public static final ImageIcon icon_blocs = ResourceUtils.loadImageIcon("icons/mini_icon_blocs.png");
	public static final ImageIcon icon_processing = ResourceUtils.loadImageIcon("icons/mini_icon_process.png");
	public static final ImageIcon icon_folder = ResourceUtils.loadImageIcon("icons/mini_icon_folder.png");
	public static final ImageIcon icon_text = ResourceUtils.loadImageIcon("icons/mini_icon_text.png");
	public static final ImageIcon icon_html = ResourceUtils.loadImageIcon("icons/mini_icon_html.png");
	public static final ImageIcon icon_image = ResourceUtils.loadImageIcon("icons/mini_icon_eye.png");
	public static final ImageIcon icon_array = ResourceUtils.loadImageIcon("icons/mini_icon_numpy.png");

	private String label = null;

	public FileDescr(File file) {
		this(file, null);
	}

	public FileDescr(File file, String label) {
		this.file = file;
		this.label = label;
		uid = UUID.randomUUID(); // Clé unique pour le HashMap de la classe Editeur
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public void setFile(File file) { this.file = file; }
	public void setModified(boolean value) { modified = value; }
	public void setLabel(String label) { this.label = label; }
	public String getLabel() { return label; }

	public File getFile() { return file; }
	public Path getPath() { return file.toPath(); }
	public String getFilename() { return file.getName(); }
	public String getExtension() { return MyFileUtils.getExtension(getFilename()); }
	public String getDirectory() { return file.getAbsoluteFile().getParent(); }
	public String getUID() { return uid.toString(); }

	public boolean isFile() { return file.isFile(); }
	public boolean isDirectory() { return file.isDirectory(); }
	public boolean isModified() { return modified; }

	public ImageIcon getIcon() {
		if (isDirectory()) return icon_folder; // Remplacé par un traitement dans le CodeLabTreeRenderer du FileManager
		switch (getExtension()) {
			case ".c":
			case ".h": return icon_c;
			case ".go": return icon_go;
			case ".py": return icon_python;
			case ".ipynb": return icon_jupyter;
			case ".hs": return icon_haskell;
			case ".lua": return icon_lua;
			case ".lsp": return icon_lisp;
			case ".clp": return icon_clips;
			case ".pde": return icon_processing;
			case ".java": return icon_java;
			case ".blocs": return icon_blocs;
			case ".txt":
			case ".csv":
			case ".log": return icon_text;
			case ".css":
			case ".html": return icon_html;
			case ".png":
			case ".jpg":
			case ".gif": return icon_image;
			case ".npy": return icon_array;
			default: return null;
		}
	}

	// Texte pour les info-bulles
	public String getInfo() {
		//return String.format("Modification: %s", FileUtils.getInfo(file));
		return String.format("[%s] File = %s", uid.toString(), file.toString());
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public String toString() {
		if (label != null) return label;
		return file.getName();
	}

	public boolean fileExists() {
		return file.exists();
	}

	public void delete() {
		MyFileUtils.moveToTrashOrDelete(file);
	}

	public void rename(String name) {
		String path = getDirectory() + File.separator + name;
		File newfile = new File(path);
		file.renameTo(newfile);
		file = newfile;
	}

	public void moveTo(FileDescr folder) {
		if (!folder.isDirectory()) return; // Ne doit pas arriver en fait...
		File newfile = new File(folder.getFile() + File.separator + file.getName());
		Utils.moveFile(file, newfile);
		setFile(newfile);
	}
}

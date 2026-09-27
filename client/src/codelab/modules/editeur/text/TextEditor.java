package codelab.modules.editeur.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import codelab.CodeLab;
import codelab.Compilateur;
import codelab.ExceptionManager;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe de l'éditeur texte
*	@author Jérôme Lehuen
*	@version 19/09/25
*/

// https://github.com/bobbylight/RSyntaxTextArea/wiki/Adding-Syntax-Highlighting-for-a-new-Language

public class TextEditor extends AbstractEditor {

	private static final long serialVersionUID = 1L;
	private static final Color COLOR_LOCKED = new Color(242, 242, 242);

	private CodeLabTextArea textarea; // Le composant texte
	private RTextScrollPane spta; // Son scrollpane

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public TextEditor(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		this.module = module;

		toolbar = new ToolBarEdit(module, this);
		textarea = new CodeLabTextArea(this, getExtension());

		// Quelques raccourcis-clavier pour le Mac (meta = cmd)
		InputMap im = textarea.getInputMap();
		ActionMap am = textarea.getActionMap();
		im.put(KeyStroke.getKeyStroke("meta Z"), "UNDO");
		im.put(KeyStroke.getKeyStroke("shift meta Z"), "REDO");
		am.put("UNDO", new RSyntaxTextAreaEditorKit.UndoAction());
		am.put("REDO", new RSyntaxTextAreaEditorKit.RedoAction());

		showLockIcon = true;

		// Placer le RSyntaxTextArea dans un RTextScrollPane dans le JPanel
		setLayout(new BorderLayout());
		add(wrapWithLockLayer(spta = new RTextScrollPane(textarea)), BorderLayout.CENTER);
		spta.setBorder(BorderFactory.createEmptyBorder());
		spta.setIconRowHeaderEnabled(false);
		spta.getGutter().setBookmarkingEnabled(false);

		// Automate Lorem Ipsum
		if (CodeLab.AUTOMATOR) new Automator(textarea).start();

		/*
		textarea.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {}
		});
		*/
	}

	public TextEditor(ModuleEditor module, String ext) {
		this.module = module;
		toolbar = new ToolBarEdit(module, this);
		textarea = new CodeLabTextArea(this, ext);

		showLockIcon = true;

		// Placer le RSyntaxTextArea dans un RTextScrollPane dans le JPanel
		setLayout(new BorderLayout());
		add(wrapWithLockLayer(spta = new RTextScrollPane(textarea)), BorderLayout.CENTER);
		spta.setBorder(BorderFactory.createEmptyBorder());
		spta.setIconRowHeaderEnabled(false);
		spta.getGutter().setBookmarkingEnabled(false);
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public CodeLabTextArea getCodeLabTextArea() {
		// Utilisé par ReplaceDialog
		return textarea;
	}

	public JTextArea getTextArea() {
		// Utilisé par Automator Lorem Ipsum
		return textarea;
	}

	public boolean isBlank() {
		return textarea.isBlank();
	}

	public boolean isTextEditor() {
		return true;
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor
	///////////////////////////////////////////////////

	public File extern_file; // Utilisation de l'éditeur hors-filemanager

	public synchronized void save_content() {
		// Méthode synchronisée pour garantir la cohérence des accès concurrents
		// et prévenir les pertes de fichiers aléatoires
		File file = getFile();
		if (file == null) {
			if (extern_file == null) return;
			textarea.save_content(extern_file);
		}
		else {
			textarea.save_content(file);
			setModified(false);
		}
	}

	public synchronized void save_content(File file) {
		// Copie pour le diff
		textarea.save_content(file);
	}

	public synchronized void load_content() {
		// Méthode synchronisée pour garantir la cohérence des accès concurrents
		// et prévenir les pertes de fichiers aléatoires
		File file = getFile();
		if (file == null) return;
		textarea.load_content(file);
		extern_file = null; // Réinitialisation éventuelle du fichier hors-filemanager
	}

	public synchronized void load_content(File file) {
		// Utilisé pour charger un fichier hors-filemanager
		textarea.load_content(file);
		extern_file = file;
	}

	public void load_template(File file) {
		textarea.load_content(file);
		textarea.setCaretPosition(0);
		String filename = getFilename();
		String username = CodeLab.INSTANCE.getUsername();
		findAndReplace("_fileName_", filename);
		findAndReplace("_author_", username);
		findAndReplace("_date_", Utils.getDate());
		findAndReplace("_className_", MyFileUtils.noExtension(filename)); // Pour Java
	}

	/*
	public boolean compile_file() {
		// Doit actualiser le flag compilation_ok de AbstractEditor
		compilation_ok = new Compilateur(module, getFile()).compile();
		return compilation_ok;
	}
	*/
	
	public boolean compile_file() {
		// Timeout de compilation en secondes
		int timeout = CodeLab.COMPILE_TIMEOUT;
		
		// Utiliser un ExecutorService pour gérer le timeout
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = null;
		
		try {
			// Lancer la compilation dans un thread séparé
			future = executor.submit(() -> {
				Compilateur compilateur = new Compilateur(module, getFile());
				return compilateur.compile();
			});
			
			// Attendre le résultat avec timeout
			compilation_ok = future.get(timeout, TimeUnit.SECONDS);
			return compilation_ok;
			
		} catch (TimeoutException e) {
			// Timeout atteint - arrêter la compilation
			if (future != null) {
				future.cancel(true);
			}
			
			// Afficher un message d'erreur
			module.codelab.consoleLogErr(String.format(
				"Compilation timeout (%d seconds) - compilation stopped", timeout));
			compilation_ok = false;
			return false;
			
		} catch (InterruptedException e) {
			// Thread interrompu
			if (future != null) {
				future.cancel(true);
			}
			compilation_ok = false;
			return false;
			
		} catch (ExecutionException e) {
			// Erreur lors de l'exécution
			module.codelab.consoleLogErr("Compilation error: " + e.getCause().getMessage());
			compilation_ok = false;
			return false;
			
		} finally {
			// Nettoyer l'executor
			executor.shutdownNow();
			try {
				if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					module.codelab.consoleLogErr("Executor did not terminate gracefully");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void changeFont(String name, int size) {
		Font font = new Font(name, Font.PLAIN, size);
		textarea.setFont(font);
	}

	public void setCodeFoldingEnabled(boolean value) {
		textarea.setCodeFoldingEnabled(value);
	}

	public void setInvisible(boolean value) {
		textarea.setWhitespaceVisible(value);
	}

	private boolean initialized = false;

	public void setEditableConfiguration(boolean value) {
		if (initialized && value == editable) return; // Pas de changement => bye bye
		initialized = true;

		SwingUtilities.invokeLater(() -> {
			this.editable = value;
			if (value) {
				textarea.setBackground(Color.WHITE);
				textarea.removeAllLineHighlights();
				textarea.setCaretPosition(0);
				textarea.setEditable(true);
				textarea.updateCaret(true);
				textarea.requestFocusInWindow();
			} else {
				textarea.setBackground(COLOR_LOCKED);
				textarea.removeAllLineHighlights();
				textarea.setEditable(false);
				textarea.updateCaret(false); // Évite les clignotements
				modified = false; // Réinitialisation de l'indicateur de modification
			}
			textarea.setBracketMatchingEnabled(value);
			textarea.setHighlightCurrentLine(value);
			textarea.setEnabled(value);

			((ToolBarEdit)toolbar).update(); // Pour les boutons spécifiques

			updateLockBadge();
			repaint();
			CodeLab.FRAME.revalidate(); // Rafraîchissement complet du cadre de la fenêtre
		});
	}

	///////////////////////////////////////////////////
	// Méthodes publiques de la barre d'outils
	///////////////////////////////////////////////////

	public void undo() {
		textarea.undoLastAction();
	}

	public void redo() {
		textarea.redoLastAction();
	}

	public void entab() {
		int deb = textarea.getSelectionStart();
		int fin = textarea.getSelectionEnd();
		int line_deb = getLineOfOffset(deb);
		int line_fin = getLineOfOffset(fin);

		for (int line = line_deb; line <= line_fin; line++)
			textarea.insert("\t", getLineStartOffset(line));

		textarea.setSelectionStart(getLineStartOffset(line_deb));
		textarea.setSelectionEnd(getLineEndOffset(line_fin) - 1);
	}

	public void detab() {
		int deb = textarea.getSelectionStart();
		int fin = textarea.getSelectionEnd();
		int line_deb = getLineOfOffset(deb);
		int line_fin = getLineOfOffset(fin);

		try {
			for (int line = line_deb; line <= line_fin; line++) {
				int offset = getLineStartOffset(line);
				char c = textarea.getText().charAt(offset);
				if (c == '\t') {
					StringBuilder sb = new StringBuilder(textarea.getText());
					textarea.setText(sb.deleteCharAt(offset).toString());
				}
			}
			textarea.setSelectionStart(getLineStartOffset(line_deb));
			textarea.setSelectionEnd(getLineEndOffset(line_fin) - 1);
		}
		catch (StringIndexOutOfBoundsException e) {
			// C'est déjà arrivé sur le textarea.getText().charAt();
			// java.lang.StringIndexOutOfBoundsException: String index out of range: ...
		}
	}

	public void comment() {
		int deb = textarea.getSelectionStart();
		int fin = textarea.getSelectionEnd();
		int line_deb = getLineOfOffset(deb);
		int line_fin = getLineOfOffset(fin);

		for (int line = line_deb; line <= line_fin; line++)
			textarea.insert(textarea.getCommentStr(), getLineStartOffset(line));

		textarea.setSelectionStart(getLineStartOffset(line_deb));
		textarea.setSelectionEnd(getLineEndOffset(line_fin) - 1);
	}

	public void uncomment() {
		int deb = textarea.getSelectionStart();
		int fin = textarea.getSelectionEnd();
		int line_deb = getLineOfOffset(deb);
		int line_fin = getLineOfOffset(fin);

		try {
			for (int line = line_deb; line <= line_fin; line++) {
				int offset = getLineStartOffset(line);
				String substr = textarea.getText().substring(offset, offset + 2);
				if (substr.equals(textarea.getCommentStr())) {
					StringBuilder sb = new StringBuilder(textarea.getText());
					textarea.setText(sb.deleteCharAt(offset).toString());
					textarea.setText(sb.deleteCharAt(offset).toString());
				}
			}

			textarea.setSelectionStart(getLineStartOffset(line_deb));
			textarea.setSelectionEnd(getLineEndOffset(line_fin) - 1);
		}
		catch (StringIndexOutOfBoundsException e) {
			// C'est déjà arrivé sur le textarea.getText().substring();
			// java.lang.StringIndexOutOfBoundsException: String index out of range: ...
		}
	}

	public void addLineHighlight(int line, Color color) {
		SwingUtilities.invokeLater(() -> {
			try { textarea.addLineHighlight(line, color); }
			catch (BadLocationException e) {}
		});
	}

	public void removeAllLineHighlights() {
		textarea.removeAllLineHighlights();
	}

	public void magicFormat() {
		// Reformate le code source C ou Java (configuration dans sysconfig.properties)
		String MAGIC_CMD = MyFileUtils.absolute(CodeLab.PROP("MAGIC_CMD"));
		String MAGIC_OPTIONS = CodeLab.PROP("MAGIC_OPTIONS");
		String filename = getFile().toString();
		String ext = getExtension();
		String cmd = "";
		switch (ext) {
			case ".c":
			case ".pde":
			case ".java": cmd = String.format("\"%s\" %s \"%s\"", MAGIC_CMD, MAGIC_OPTIONS, filename); break;
			case ".py": cmd = String.format("black \"%s\"", filename); break;
		}
		module.pauseTimerTasks();
		save_content();
		synchronized (this) {
			// Modifie le fichier sur le disque
			Utils.execute(cmd);
		}
		load_content();
		module.resumeTimerTasks();
	}

	public boolean convertSpaceToTab(int nbspaces) {
		SearchContext context = new SearchContext();
		context.setSearchFor(" ".repeat(nbspaces));
		context.setReplaceWith("\t");
		return SearchEngine.replaceAll(textarea, context).wasFound();
	}

	public boolean convertTabToSpace(int nbspaces) {
		SearchContext context = new SearchContext();
		context.setSearchFor("\t");
		context.setReplaceWith(" ".repeat(nbspaces));
		return SearchEngine.replaceAll(textarea, context).wasFound();
	}

	public void findAndReplaceDialog() {
		if (ReplaceDialog.window == null)
			new ReplaceDialog(module);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private int getLineOfOffset(int offset) {
		try {
			return textarea.getLineOfOffset(offset);
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
			return -1;
		}
	}

	private int getLineStartOffset(int line) {
		try {
			return textarea.getLineStartOffset(line);
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
			return -1;
		}
	}

	private int getLineEndOffset(int line) {
		try {
			return textarea.getLineEndOffset(line);
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
			return -1;
		}
	}

	private void findAndReplace(String oldstr, String newstr) {
		//CodeLab.INSTANCE.printlnToConsole(String.format("[%s] -> [%s]", oldstr, newstr), Color.BLUE);
		textarea.findAndReplace(oldstr, newstr);
	}
}

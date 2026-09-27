package codelab.modules.editeur.text;

import codelab.AbstractToolbar;
import codelab.CodeLab;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.Utils;

/**
*	Classe de la barre d'outils de l'éditeur
*	@author Jérôme Lehuen
*	@version 14/01/24
*/

public class ToolBarEdit extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	private final ModuleEditor editorpane;
	private final TextEditor editor;

	private ToolButton langButton;
	private ToolButton b1, b2, b3, b4, b5, b6, b7, b8;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolBarEdit(ModuleEditor module, TextEditor editor) {
		super(module);
		editorpane = module;
		this.editor = editor;

		addSeparator();
		addButton("NUMPAD");
		addButton("STICK");
		addButton("CTRL");
		addSeparator();
		add(b1 = new ToolButton("UNDO", LABEL("Btn_undo"), LABEL("Btn_undo_tooltip"), icon_undo, this));
		add(b2 = new ToolButton("REDO", LABEL("Btn_redo"), LABEL("Btn_redo_tooltip"), icon_redo, this));
		add(b3 = new ToolButton("LEFT", LABEL("Btn_untab"), LABEL("Btn_untab_tooltip"), icon_detab, this));
		add(b4 = new ToolButton("RIGHT", LABEL("Btn_tab"), LABEL("Btn_tab_tooltip"), icon_entab, this));
		add(b6 = new ToolButton("COMM", LABEL("Btn_com"), LABEL("Btn_com_tooltip"), icon_encom, this));
		add(b5 = new ToolButton("UNCOM", LABEL("Btn_uncom"), LABEL("Btn_uncom_tooltip"), icon_decom, this));
		add(b7 = new ToolButton("FIND", LABEL("Btn_find"), LABEL("Btn_find_tooltip"), icon_search, this));
		add(b8 = new ToolButton("MAGIC", LABEL("Btn_magic"), LABEL("Btn_magic_tooltip"), icon_magic, this));
		addSeparatorflex();
		add(langButton = new ToolButton("LANG", null, "", null, this));
		endToolbar();

		if (editor == null) {
			// Pour le EmptyEditor
			b1.setEnabled(false);
			b2.setEnabled(false);
			b3.setEnabled(false);
			b4.setEnabled(false);
			b5.setEnabled(false);
			b6.setEnabled(false);
			b7.setEnabled(false);
			b8.setEnabled(false);
		}
	}

	///////////////////////////////////////////////////
	// Actualisation des boutons
	///////////////////////////////////////////////////

	public void update() {
		super.update();
		boolean editable_mode = editorpane.getEditor().getEditable();
		boolean enable = (editor != null) && editable_mode;
		boolean extern = (editor != null) && (editor.extern_file != null); // Fichier hors-filemanager
		String ext = extern ? "" : enable ? editor.getExtension() : "";

		b1.setEnabled(enable);
		b2.setEnabled(enable);
		b3.setEnabled(enable);
		b4.setEnabled(enable);
		b5.setEnabled(enable);
		b6.setEnabled(enable);
		b7.setEnabled(enable);
		b8.setEnabled(enable && (
			ext.equals(".c") ||
			ext.equals(".pde") ||
			ext.equals(".java") ||
			ext.equals(".py")));
	}

	public void action(String ident) {
		switch (ident) {
			case "UNDO":	editor.undo(); break;
			case "REDO":	editor.redo(); break;
			case "LEFT":	editor.detab(); break;
			case "RIGHT":	editor.entab(); break;
			case "COMM":	editor.comment(); break;
			case "UNCOM":	editor.uncomment(); break;
			case "MAGIC":	editor.magicFormat(); break;
			case "FIND":	editor.findAndReplaceDialog(); break;
			case "LANG":	Utils.openBrowser(langButton.getToolTipText()); break;
			default: super.action(ident);
		}
	}

	///////////////////////////////////////////////////
	// Actualisation en fonction du langage
	///////////////////////////////////////////////////

	public void setLang(String ext) {
		switch (ext) {
			case ".c":
			case ".h":
				langButton.setEnabled(true);
				langButton.setIcon(logo_c);
				langButton.setTextProp("Lang C");
				langButton.setToolTipText(CodeLab.PROP("URL_C"));
				break;
			case ".go":
				langButton.setEnabled(true);
				langButton.setIcon(logo_go);
				langButton.setTextProp("Go");
				langButton.setToolTipText(CodeLab.PROP("URL_GO"));
				break;
			case ".py":
				langButton.setEnabled(true);
				langButton.setIcon(logo_python);
				langButton.setTextProp("Python");
				langButton.setToolTipText(CodeLab.PROP("URL_PYTHON"));
				break;
			case ".hs":
				langButton.setEnabled(true);
				langButton.setIcon(logo_haskell);
				langButton.setTextProp("Haskell");
				langButton.setToolTipText(CodeLab.PROP("URL_HASKELL"));
				break;
			case ".java":
				langButton.setEnabled(true);
				langButton.setIcon(logo_java);
				langButton.setTextProp("Java");
				langButton.setToolTipText(CodeLab.PROP("URL_JAVA"));
				break;
			case ".pde":
				langButton.setEnabled(true);
				langButton.setIcon(logo_processing);
				langButton.setTextProp("Processing");
				langButton.setToolTipText(CodeLab.PROP("URL_PROCESS"));
				break;
			case ".lsp":
				langButton.setEnabled(true);
				langButton.setIcon(logo_lisp);
				langButton.setTextProp("Lisp");
				langButton.setToolTipText(CodeLab.PROP("URL_LISP"));
				break;
			case ".clp":
				langButton.setEnabled(true);
				langButton.setIcon(logo_clips);
				langButton.setTextProp("CLIPS");
				langButton.setToolTipText(CodeLab.PROP("URL_CLIPS"));
				break;
			case ".lua":
				langButton.setEnabled(true);
				langButton.setIcon(logo_lua);
				langButton.setTextProp("LUA");
				langButton.setToolTipText(CodeLab.PROP("URL_LUA"));
				break;
			default:
				langButton.setEnabled(false);
				langButton.setIcon(null);
				langButton.setTextProp(null);
		}
	}
}

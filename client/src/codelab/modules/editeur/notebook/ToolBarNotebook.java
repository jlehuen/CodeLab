package codelab.modules.editeur.notebook;

import codelab.AbstractToolbar;
import codelab.CodeLab;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.Utils;

/**
 * Barre d'outils de l'éditeur de carnets Jupyter (.ipynb).
 * Identique à celle de l'éditeur de texte (commandes d'édition, recherche, contrôleurs, Jupyter), sans Magic.
 * @author Jérôme Lehuen + Antigravity
 */
public class ToolBarNotebook extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	private final NotebookEditor editor;

	private ToolButton langButton;
	private ToolButton b1, b2, b3, b4, b5, b6, b7, b8;

	public ToolBarNotebook(ModuleEditor module, NotebookEditor editor) {
		super(module);
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
		add(b8 = new ToolButton("CLEAR_COUNTS", LABEL("Btn_clear_counts"), LABEL("Btn_clear_counts_tooltip"), icon_clear, this));
		addSeparatorflex();
		add(langButton = new ToolButton("LANG", "Python", CodeLab.PROP("URL_PYTHON") != null ? CodeLab.PROP("URL_PYTHON") : "https://python.org", logo_python, this));
		endToolbar();

		if (editor == null) {
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

	@Override
	public void update() {
		super.update();
		if (editor != null) {
			editor.onExecutionStateChanged();
		}
		boolean editable_mode = (editor != null) && editor.getEditable();
		boolean enable = (editor != null) && editable_mode;

		if (b1 != null) b1.setEnabled(enable);
		if (b2 != null) b2.setEnabled(enable);
		if (b3 != null) b3.setEnabled(enable);
		if (b4 != null) b4.setEnabled(enable);
		if (b5 != null) b5.setEnabled(enable);
		if (b6 != null) b6.setEnabled(enable);
		if (b7 != null) b7.setEnabled(enable);
		if (b8 != null) b8.setEnabled(enable);
		if (langButton != null) {
			langButton.setEnabled(true);
			NotebookCellPane active = (editor != null) ? editor.getActiveCell() : null;
			boolean isC;
			if (active != null && "code".equals(active.getType())) {
				isC = "c".equalsIgnoreCase(active.getLanguage());
			} else if (active != null) {
				isC = "c".equalsIgnoreCase(active.getLanguage());
			} else if (editor != null) {
				isC = editor.isCNotebook();
			} else {
				isC = false;
			}

			if (isC) {
				langButton.setIcon(logo_c);
				langButton.setTextProp("Lang C");
				langButton.setToolTipText(CodeLab.PROP("URL_C"));
			} else {
				langButton.setIcon(logo_python);
				langButton.setTextProp("Python");
				langButton.setToolTipText(CodeLab.PROP("URL_PYTHON"));
			}
		}
	}

	@Override
	public void action(String ident) {
		switch (ident) {
			case "UNDO":	if (editor != null) editor.undo(); break;
			case "REDO":	if (editor != null) editor.redo(); break;
			case "LEFT":	if (editor != null) editor.detab(); break;
			case "RIGHT":	if (editor != null) editor.entab(); break;
			case "COMM":	if (editor != null) editor.comment(); break;
			case "UNCOM":	if (editor != null) editor.uncomment(); break;
			case "FIND":	if (editor != null) editor.findAndReplaceDialog(); break;
			case "CLEAR_COUNTS":	if (editor != null) editor.resetExecutionCounts(); break;
			case "LANG":	if (langButton != null) Utils.openBrowser(langButton.getToolTipText()); break;
			default: super.action(ident);
		}
	}

	@Override
	public void setLang(String ext) {
		update();
	}
}

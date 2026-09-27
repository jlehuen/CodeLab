package codelab.modules.editeur.html;

import codelab.AbstractToolbar;
import codelab.CodeLab;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;

public class ToolBarHTML extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolBarHTML(ModuleEditor module, HTMLPane editor) {
		super(module);

		add(separator_flex);
		add(helpButton);
		if (CodeLab.CONNECTED_MODE) add(serverButton);
		add(new ToolButton("INFO", LABEL("Btn_info"), LABEL("Btn_info_tooltip"), icon_info, this));
    }
}

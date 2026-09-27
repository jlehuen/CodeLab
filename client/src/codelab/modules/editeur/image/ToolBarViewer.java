package codelab.modules.editeur.image;

import codelab.AbstractToolbar;
import codelab.CodeLab;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;

public class ToolBarViewer extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ToolBarViewer(ModuleEditor module, ImageViewer viewer) {
		super(module);

		add(separator_flex);
		add(helpButton);
		if (CodeLab.CONNECTED_MODE) add(serverButton);
		add(new ToolButton("INFO", LABEL("Btn_info"), LABEL("Btn_info_tooltip"), icon_info, this));
	}
}

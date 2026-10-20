package codelab.modules.editeur.markdown;

import codelab.AbstractToolbar;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.ResourceUtils;

/**
 * Barre d'outils dédiée au visualiseur / éditeur Markdown.
 * Permet de basculer entre l'aperçu HTML riche et le code source,
 * de rafraîchir le rendu ou d'ouvrir le rapport dans le navigateur par défaut.
 *
 * @author Jérôme Lehuen + Antigravity
 * @version 09/10/26
 */
public class ToolBarMarkdown extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	private MarkdownViewer viewer;
	private ToolButton toggleButton;

	public ToolBarMarkdown(ModuleEditor module, MarkdownViewer viewer) {
		super(module);
		this.viewer = viewer;

		// Bouton bascule Aperçu / Code source
		toggleButton = new ToolButton("TOGGLE", "Source", "Basculer entre l'aperçu formaté et le code source Markdown", ResourceUtils.loadImageIcon("icons/icon_magic.png"), this);
		add(toggleButton);

		// Bouton Actualiser le rendu
		add(new ToolButton("REFRESH", "Actualiser", "Rafraîchir le rendu HTML Markdown", icon_reset, this));

		// Bouton Ouvrir dans le navigateur externe (pour impression PDF, etc.)
		add(new ToolButton("BROWSER", "Navigateur", "Consulter dans le navigateur Web par défaut", icon_search, this));

		addSeparatorflex();
		endToolbar();
	}

	@Override
	public void action(String ident) {
		switch (ident) {
			case "TOGGLE":
				viewer.toggleView();
				toggleButton.setText(viewer.isShowingPreview() ? "Source" : "Aperçu");
				break;
			case "REFRESH":
				viewer.renderMarkdown();
				break;
			case "BROWSER":
				viewer.openInBrowser();
				break;
			default:
				super.action(ident);
		}
	}
}

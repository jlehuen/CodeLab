package codelab.modules.editeur.markdown;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import codelab.AbstractToolbar;
import codelab.ToolButton;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.ResourceUtils;

/**
 * Barre d'outils dédiée au visualiseur Markdown (lecture seule).
 * Permet de naviguer dans l'historique (Précédent / Suivant),
 * de rafraîchir le rendu ou d'ouvrir le document dans le navigateur par défaut.
 *
 * @author Jérôme Lehuen + Antigravity
 * @version 27/11/26
 */
public class ToolBarMarkdown extends AbstractToolbar {

	private static final long serialVersionUID = 1L;

	private MarkdownViewer viewer;
	private ToolButton backButton;
	private ToolButton forwardButton;

	public ToolBarMarkdown(ModuleEditor module, MarkdownViewer viewer) {
		super(module);
		this.viewer = viewer;

		addSeparator();

		// Boutons de navigation historique (Précédent / Suivant), alignés sur la hauteur standard (30 px)
		int targetH = (icon_reset != null && icon_reset.getIconHeight() > 0) ? icon_reset.getIconHeight() : 30;
		ImageIcon iconPrev = scaleIconToHeight(ResourceUtils.loadImageIcon("icons/icon_left.png"), targetH);
		ImageIcon iconNext = scaleIconToHeight(ResourceUtils.loadImageIcon("icons/icon_right.png"), targetH);

		backButton = new ToolButton("BACK", "Précédent", "Revenir au document Markdown précédent", iconPrev, this);
		backButton.setEnabled(MarkdownViewer.canNavigateBack());
		add(backButton);

		forwardButton = new ToolButton("FORWARD", "Suivant", "Aller au document Markdown suivant", iconNext, this);
		forwardButton.setEnabled(MarkdownViewer.canNavigateForward());
		add(forwardButton);

		// Bouton Actualiser le rendu
		add(new ToolButton("REFRESH", "Actualiser", "Rafraîchir le rendu HTML Markdown", icon_redo, this));

		addSeparatorflex();
		endToolbar();
	}

	private static ImageIcon scaleIconToHeight(ImageIcon src, int targetH) {
		if (src == null || src.getIconHeight() <= 0 || src.getIconHeight() == targetH) return src;
		int targetW = Math.max(1, Math.round(src.getIconWidth() * (targetH / (float) src.getIconHeight())));
		BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(src.getImage(), 0, 0, targetW, targetH, null);
		g.dispose();
		return new ImageIcon(dst);
	}

	@Override
	public void action(String ident) {
		switch (ident) {
			case "BACK":
				viewer.navigateBack();
				break;
			case "FORWARD":
				viewer.navigateForward();
				break;
			case "REFRESH":
				viewer.load_content();
				break;
			default:
				super.action(ident);
		}
	}
}

package codelab.modules.editeur.scratch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import codelab.modules.editeur.scratch.widgets.AbstractWidget;

/**
*	Classe des menus popup des widgets
*	@author Jérôme Lehuen
*	@version 12/10/23
*/

public class WidgetPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeur (invoqué dans GlassPaneListener)
	///////////////////////////////////////////////////

	public WidgetPopupMenu(GlassPane panel, AbstractWidget widget, int x, int y, double scale) {

		//int x_scaled = (int)(x/scale);
		//int y_scaled = (int)(y/scale);
		String texte;

		texte = String.format("Editer le widget");
		JMenuItem menuItem1 = new JMenuItem(texte);
		menuItem1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				widget.mouseover_off(); // Pour que le widget ne soit pas magenta
				panel.repaint();
				widget.showEditDialog();
			}
		});

		texte = String.format("Copier le widget");
		JMenuItem menuItem2 = new JMenuItem(texte);
		menuItem2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// On ne mémorise pas l'original (qui peut être supprimé)
				panel.setWidgetToPaste(widget.cloner(panel));
			}
		});

		texte = String.format("Copier le widget et ses dépendances");
		JMenuItem menuItem3 = new JMenuItem(texte);
		menuItem3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// On ne mémorise pas l'original (qui peut être supprimé)
				panel.setWidgetToPaste(widget.clonerRec(panel));
			}
		});

		texte = String.format("Supprimer le widget et ses dépendances");
		JMenuItem menuItem4 = new JMenuItem(texte);
		menuItem4.setEnabled(widget.isRoot()); // On ne supprime que les widgets libres
		menuItem4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				widget.supprimer();
				panel.repaint();
			}
		});

		add(menuItem1);
		add(menuItem2);
		add(menuItem3);
		add(menuItem4);
		show(panel, x, y);
	}
}

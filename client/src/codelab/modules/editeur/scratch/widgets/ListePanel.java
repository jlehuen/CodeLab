package codelab.modules.editeur.scratch.widgets;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
*	Classe des listes déroulantes
*	@author Jérôme Lehuen
*	@version 01/12/20
*/

public class ListePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JComboBox<Object> box;

	public ListePanel(String text, Object[] items) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(new JLabel(text + "  "));
		add(box = new JComboBox<Object>(items));
	}

	public Object getSelectedItem() {
		return box.getSelectedItem();
	}

	public void setItem(Object item) {
		box.getModel().setSelectedItem(item);
	}
}

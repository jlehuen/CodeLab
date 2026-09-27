package codelab.modules.editeur.scratch.widgets;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

/**
*   Classe des  champs de saisie avec validation
*   @author Jérôme Lehuen
*   @version 07/10/20
*/

public class TextFieldKeyListener extends JTextField implements KeyListener {

    private static final long serialVersionUID = 1L;
    private final AbstractEditDialog dialog;

    ///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public TextFieldKeyListener(AbstractEditDialog dialog) {
        setColumns(20);
        this.dialog = dialog;
        addKeyListener(this);
    }

    ///////////////////////////////////////////////////
	// Méthodes de l'interface KeyListener
    ///////////////////////////////////////////////////

    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            dialog.valider(); // Valider le contenu
            dialog.dispose(); // Fermer la fenêtre
        }
    }
}

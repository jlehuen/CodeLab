package codelab.modules.editeur.text;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import codelab.CodeLab;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.ResourceUtils;
import codelab.utils.swing.GBHelper;

/**
*	Fenêtre de dialogue (singleton) du chercher-remplacer
*	@author Jérôme Lehuen
*	@version 12/01/24
*/

// https://javadoc.fifesoft.com/rsyntaxtextarea/org/fife/ui/rtextarea/SearchEngine.html
// https://javadoc.fifesoft.com/rsyntaxtextarea/org/fife/ui/rtextarea/SearchResult.html

public class ReplaceDialog extends JDialog implements ActionListener {

	public static ReplaceDialog window = null; // Singleton

	private static final ImageIcon SMILEY_SAD = ResourceUtils.loadImageIcon("icons/icon_smiley_sad.png");

	private JLabel label1     = new JLabel(CodeLab.LABEL("ReplaceDialog_LABEL1"), JLabel.LEFT);
	private JLabel label2     = new JLabel(CodeLab.LABEL("ReplaceDialog_LABEL2"), JLabel.LEFT);
	private JTextField champ1 = new JTextField(20);
	private JTextField champ2 = new JTextField(20);

	private JButton findBtn     = new JButton(CodeLab.LABEL("ReplaceDialog_BUTTON1"));
	private JButton replaceBtn  = new JButton(CodeLab.LABEL("ReplaceDialog_BUTTON2"));
	private JButton replAllBtn  = new JButton(CodeLab.LABEL("ReplaceDialog_BUTTON3"));
	private JButton markAllBtn  = new JButton(CodeLab.LABEL("ReplaceDialog_BUTTON4"));
	private JButton closeBtn    = new JButton(CodeLab.LABEL("ReplaceDialog_BUTTON5"));

	private JCheckBox matchCaseCB = new JCheckBox(CodeLab.LABEL("ReplaceDialog_CHECKBOX1"));
	private JCheckBox wholeWrdsCB = new JCheckBox(CodeLab.LABEL("ReplaceDialog_CHECKBOX2"));
	private JCheckBox backwardsCB = new JCheckBox(CodeLab.LABEL("ReplaceDialog_CHECKBOX3"));
	private JCheckBox regexCB     = new JCheckBox(CodeLab.LABEL("ReplaceDialog_CHECKBOX4"));

	private ModuleEditor module = null;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ReplaceDialog(ModuleEditor module) {
		super(CodeLab.FRAME, false);
		ReplaceDialog.window = this;
		this.module = module;

		// Commandes du gestionnaire
		findBtn.setActionCommand("FINDNEXT");
		findBtn.addActionListener(this);
		markAllBtn.setActionCommand("MARKALL");
		markAllBtn.addActionListener(this);
		replaceBtn.setActionCommand("REPLACE");
		replaceBtn.addActionListener(this);
		replAllBtn.setActionCommand("REPLACEALL");
		replAllBtn.addActionListener(this);
		closeBtn.setActionCommand("CLOSE");
		closeBtn.addActionListener(this);

		// Panel des boutons
		final int SPACE = 6;
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(5, 1, 0, SPACE));
		buttonPanel.add(findBtn);
		buttonPanel.add(replaceBtn);
		buttonPanel.add(markAllBtn);
		buttonPanel.add(replAllBtn);
		buttonPanel.add(closeBtn);

		// Panel des cases à cocher
		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new GridLayout(4, 2));
		checkBoxPanel.add(wholeWrdsCB);
		checkBoxPanel.add(matchCaseCB);
		checkBoxPanel.add(regexCB);
		checkBoxPanel.add(backwardsCB);

		// Conteneur principal
		final int BORDER = 14;
		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
		GBHelper pos = new GBHelper();

		// Première ligne
		content.add(label1, pos);
		content.add(new Gap(GAP), pos.nextCol());
		content.add(champ1, pos.nextCol());
		content.add(new Gap(GAP), pos.nextCol());
		content.add(buttonPanel, pos.nextCol().height(6).align(GridBagConstraints.NORTH));
		content.add(new Gap(GAP), pos.nextRow());

		// Deuxième ligne
		content.add(label2, pos.nextRow());
		content.add(new Gap(GAP), pos.nextCol());
		content.add(champ2, pos.nextCol());
		content.add(new Gap(GAP), pos.nextRow());

		// Troisième ligne
		content.add(checkBoxPanel, pos.nextRow().nextCol().nextCol());

		getContentPane().add(content);
		pack();

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				removeAllMarks();
				window.dispose();
				window = null;
			}
		});

		setTitle(CodeLab.LABEL("ReplaceDialog_title"));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void removeAllMarks() {
		AbstractEditor editor = module.getEditor();
		if (!editor.isTextEditor()) return; // Pas un editeur de textes

		CodeLabTextArea textArea = ((TextEditor)editor).getCodeLabTextArea();
		if (textArea == null) return; // Pas de fichier en cours d'édition

		SearchContext context = new SearchContext();
		context.setMarkAll(false);
		SearchEngine.markAll(textArea, context);
	}

	private void textNotFound(String txt) {
		JOptionPane.showMessageDialog(this,
			String.format(CodeLab.LABEL("ReplaceDialog_notFind"), txt),
			CodeLab.LABEL("ReplaceDialog_title"),
			JOptionPane.INFORMATION_MESSAGE, SMILEY_SAD);
	}

	///////////////////////////////////////////////////
	// Classe interne Gap (espace)
	///////////////////////////////////////////////////

	private static final int GAP = 10;

	private class Gap extends JComponent {
		public Gap(int size) {
			Dimension dim = new Dimension(size, size);
			setMinimumSize(dim);
			setPreferredSize(dim);
			setMaximumSize(dim);
		}
	}

	///////////////////////////////////////////////////
	// Gestionnaire de commandes
	///////////////////////////////////////////////////

	public void actionPerformed(ActionEvent e) {

		AbstractEditor editor = module.getEditor();
		if (!editor.isTextEditor()) return; // Pas un editeur de textes

		CodeLabTextArea textArea = ((TextEditor)editor).getCodeLabTextArea();
		if (textArea == null) return; // Pas de fichier en cours d'édition

		String action = e.getActionCommand();

		if (action.equals("CLOSE")) {
			// Fermeture de la fenêtre de dialogue
			dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
			ReplaceDialog.window = null;
		}

		SearchContext context = new SearchContext();
		String text1 = champ1.getText();
		String text2 = champ2.getText();
		if (text1.length() == 0) return; // Rien à rechercher

		context.setMarkAll(false);
		context.setSearchFor(text1);
		context.setReplaceWith(text2);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setWholeWord(wholeWrdsCB.isSelected());
		context.setSearchForward(!backwardsCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());

		boolean found;
		switch (action) {

			case "MARKALL":
				context.setMarkAll(true);
				int nb = SearchEngine.markAll(textArea, context).getMarkedCount();
				if (nb == 0) textNotFound(text1);
				break;

			case "FINDNEXT":
				found = SearchEngine.find(textArea, context).wasFound();
				if (!found) textNotFound(text1);
				break;

			case "REPLACE":
				found = SearchEngine.replace(textArea, context).wasFound();
				if (!found) textNotFound(text1);
				break;

			case "REPLACEALL":
				module.saveCurrentFile(); // Par précaution
				found = SearchEngine.replaceAll(textArea, context).wasFound();
				if (!found) textNotFound(text1);
				break;
		}
	}
}

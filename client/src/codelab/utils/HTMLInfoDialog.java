package codelab.utils;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import codelab.CodeLab;

/**
 *	Classe du visualiseur HTML
 *	@author Jérôme Lehuen + Gemini 3.8
 *	@version 14/09/26
 */

public class HTMLInfoDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String HTML = "/data/mentions/mentions-%s.html";
	private static final String BULLET = "/data/mentions/bullet.png";

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public HTMLInfoDialog() {
		super(CodeLab.FRAME, true);

		// Résolution de l'URL de base pour le chargeur HTML (compatible obfuscation)
		String path = String.format(HTML, CodeLab.LANG);
		String base = HTMLInfoDialog.class.getResource(path).toString();
		String bullet = HTMLInfoDialog.class.getResource(BULLET).toString();

		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet styleSheet = kit.getStyleSheet();
		styleSheet.addRule("body { font-size:10px; color:black; font-family:Verdana,Arial,Helvetica,sans-serif; }");
		styleSheet.addRule(".cite { font-size:12px; color:rgb(45,150,233); font-style:italic; }");
		styleSheet.addRule("h1 { color:rgb(45,150,233); }");
		styleSheet.addRule("ul { list-style-image:url(" + bullet + "); margin:0px 20px; }");

		String htmlString = String.format("<html><head><base href=\"%s\"></head><body>", base);
		htmlString += ResourceUtils.readResourceAsString(path);
		htmlString += "</body></html>";

		JEditorPane htmlPane = new JEditorPaneWithLink();
		htmlPane.setEditorKit(kit);
		htmlPane.setEditable(false);
		htmlPane.setText(htmlString); // Pour charger le contenu HTML
		htmlPane.setCaretPosition(0); // Pour avoir le scrollPane en bas

		JScrollPane scrollPane = new JScrollPane(htmlPane);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(scrollPane);
		pack();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(CodeLab.LABEL("INFO_TITLE"));
		setSize(new Dimension(650, 500));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);

		// Fermeture propre sur la touche Échap
		getRootPane().registerKeyboardAction(e -> {
			setVisible(false);
			dispose();
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		setVisible(true);
	}
}

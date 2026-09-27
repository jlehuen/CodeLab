package codelab.modules.editeur.manager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import codelab.CodeLab;
import codelab.utils.MyFileUtils;
import codelab.utils.SeparatorComboBox;
import codelab.utils.Utils;

/**
*	Classe de la fenêtre NewFileDialog
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class NewFileDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	// Les éléments du dialogue
	private JTextField textfield = new JTextField();
	private ButtonGroup group_lang = new ButtonGroup();
	private SeparatorComboBox comboBox = new SeparatorComboBox();
	private JButton btn_ok; // = new JButton(LABEL("OK"));

	// Le résultat
	private String filename;
	private File template;

	public String getFilename() { return filename; }
	public File getTemplate() { return template; }

	// Le flag de validité
	private boolean valide_name = false;

	private void btn_ok_actualise() {
		btn_ok.setEnabled(valide_name);
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public NewFileDialog(String parentPath) {
		super(CodeLab.FRAME, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		textfield.setPreferredSize(new Dimension(300, 30));
		comboBox.setPreferredSize(new Dimension(300, 30));

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createCenterPanel(), BorderLayout.CENTER);
		panel.add(createButtonPanel(), BorderLayout.SOUTH);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel);
		pack();

		// Fermeture sur la touche Échap
		getRootPane().registerKeyboardAction(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeFrame();
				}
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);

		String title;
		String parent = new File(parentPath).getName();
		boolean isRoot = parent.equals("local") || parent.equals("dist");
		if (isRoot) title = LABEL("NewFileDialogRootTitle");
		else title = String.format(LABEL("NewFileDialogTitle"), parent);
		setTitle(title);

		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void valider() {
		// Invoqué par touche ENTER ou bouton OK
		String name = MyFileUtils.noExtension(textfield.getText());
		String ext = group_lang.getSelection().getActionCommand();
		filename = name + ext;

		// Capitaliser si Java ou Processing
		if (ext == ".java") filename = Utils.capitalize(filename);
		if (ext == ".pde") filename = Utils.capitalize(filename);

		template = ((TemplateItem)comboBox.getSelectedItem()).getFile();
	}

	private void __scanTemplates__(File dir, String base, String ext, ArrayList<TemplateItem> liste, boolean isroot) {
		// Pour remplir la liste des items (méthode récursive)
		File[] files = dir.listFiles();
		Arrays.sort(files);
		for (File file : files) {
			if (file.isDirectory()) {
				String name = file.getName();
				// Pour identifier un nom de module
				if (CodeLab.INSTANCE.isDispModule(name.toUpperCase())) {
					name = name.toUpperCase();
					String title = CodeLab.INSTANCE.getModuleTitle(name);
					if (title != null) name = title; // Titre du module
				}
				// Pour ne pas placer de slash au début
				String sep = isroot ? "" : " / ";
				// Pour visiter les sous-dossiers
				__scanTemplates__(file, base + sep + name, ext, liste, false);
			}
			else if (MyFileUtils.getExtension(file).equals(ext)) {
				// Pour créer l'item
				TemplateItem item = new TemplateItem(base, file);
				liste.add(item);
			}
		}
	}

	private void scanTemplates(File dir, String ext, SeparatorComboBox comboBox) {
		ArrayList<TemplateItem> liste = new ArrayList<TemplateItem>(); // Résultat à trier
		__scanTemplates__(dir, "", ext, liste, true);
		// Ordonner la liste des items
		Collections.sort(liste);
		// Remplir la comboBox
		for (TemplateItem item : liste) {
			comboBox.addItem(item);
			if (item.isDefault())
				comboBox.setSelectedItem(item);
		}
	}

	private boolean notEmpty(File dir, String ext) {
		File[] files = dir.listFiles();
		for(File file : files) {
			if (MyFileUtils.getExtension(file).equals(ext))
				return true;
			else if (file.isDirectory())
				return notEmpty(file, ext);
		}
		return false;
	}

	private void closeFrame() {
		setVisible(false);
		dispose();
	}

	private String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	///////////////////////////////////////////////////
	// Le panel central
	///////////////////////////////////////////////////

	private JPanel createCenterPanel() {

		// Choix du langage
		JPanel languagePanel = new JPanel(new GridLayout(0, 1));
		languagePanel.setBorder(new CompoundBorder(
			new TitledBorder(String.format(" %s ", LABEL("NewFileDialogTitle_LANG"))),
			new EmptyBorder(10, 20, 10, 10)));

		JSeparator separator = new JSeparator();
		separator.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_C"), ".c", true));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Go"), ".go"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Java"), ".java"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Haskell"), ".hs"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Python"), ".py"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_CLIPS"), ".clp"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Blocs"), ".blocs"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Process"), ".pde"));
		//languagePanel.add(separator);
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_HTML"), ".html"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_CSS"), ".css"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_Text"), ".txt"));
		languagePanel.add(new LanguageRadioButton(LABEL("NewFileDialog_H"), ".h"));

		// Choix du template
		JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel templateLabel = new JLabel(LABEL("NewFileDialogTitle_TEMPLATE"));
		templatePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Top margin
		templatePanel.add(templateLabel);
		templatePanel.add(comboBox);
		scanTemplates(new File(CodeLab.TEMPLATES_FOLDER), ".c", comboBox);
		if (notEmpty(new File(CodeLab.EXERCICES_FOLDER), ".c")) comboBox.addItem(new JSeparator(JSeparator.HORIZONTAL));
		scanTemplates(new File(CodeLab.EXERCICES_FOLDER), ".c", comboBox);

		// Nom du programme
		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel nameLabel = new JLabel(LABEL("NewFileDialogTitle_NAME"));
		namePanel.add(nameLabel);
		namePanel.add(textfield);

		// Aligner les largueurs des labels
		int width = Math.max(templateLabel.getPreferredSize().width, nameLabel.getPreferredSize().width);
		templateLabel.setPreferredSize(new Dimension(width, 30));
		nameLabel.setPreferredSize(new Dimension(width, 30));

		// Listener de changement du JTextField
		textfield.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) { update(); }
			public void removeUpdate(DocumentEvent e) { update(); }
			public void insertUpdate(DocumentEvent e) { update(); }
			public void update() {
				valide_name = (textfield.getText().length() > 0) && (!textfield.getText().contains("."));
				btn_ok_actualise();
			}
		});

		// Listener de la touche ENTER
		textfield.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (btn_ok.isEnabled()) {
						valider();
						closeFrame();
					}
				}
			}
		});

		// Construction du panel central
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(languagePanel);
		panel.add(templatePanel);
		panel.add(namePanel);
		return panel;
	}

	///////////////////////////////////////////////////
	// Le panel inférieur
	///////////////////////////////////////////////////

	private JPanel createButtonPanel() {

		// Le bouton OK
		btn_ok = new JButton(LABEL("OK"));
		btn_ok.setEnabled(false);
		btn_ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				valider();
				closeFrame();
			}
		});

		// Le bouton CANCEL
		JButton btn_cancel = new JButton(LABEL("CANCEL"));
		btn_cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeFrame();
			}
		});

		// Construction du panel inférieur
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		panel.add(btn_cancel);
		panel.add(btn_ok);
		return panel;
	}

	///////////////////////////////////////////////////
	// Classe interne des boutons radio
	///////////////////////////////////////////////////

	private class LanguageRadioButton extends JRadioButton {

		private static final long serialVersionUID = 1L;

		LanguageRadioButton(String label, String ext) {
			super(label);
			group_lang.add(this);
			setActionCommand(ext);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					comboBox.removeAllItems();
					scanTemplates(new File(CodeLab.TEMPLATES_FOLDER), ext, comboBox);
					if (notEmpty(new File(CodeLab.EXERCICES_FOLDER), ext)) comboBox.addItem(new JSeparator(JSeparator.HORIZONTAL));
					scanTemplates(new File(CodeLab.EXERCICES_FOLDER), ext, comboBox);
					btn_ok_actualise();
				}
			});
		}

		LanguageRadioButton(String label, String ext, boolean selected) {
			this(label, ext);
			setSelected(true);
		}
	}

	///////////////////////////////////////////////////
	// Classe interne des éléments du ComboBox
	///////////////////////////////////////////////////

	private class TemplateItem implements Comparable<TemplateItem> {

		private File file;
		private String tostring;
		private boolean default_flag = false;

		public File getFile() { return file; }
		public String toString() { return tostring; }
		public boolean isDefault() { return default_flag; }

		TemplateItem(String base, File file) {
			this.file = file;
			tostring = String.format("<html>%s<b><font color=#4F9EE3> ➜ %s</font></b></html>", base, file.getName());
			if (tostring.contains("empty")) default_flag = true; // Template par défaut
		}

		public int compareTo(TemplateItem item) {
			return tostring.compareTo(item.tostring);
		}
	}
}

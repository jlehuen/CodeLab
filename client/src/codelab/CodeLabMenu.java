package codelab;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import codelab.console.ConsolePopupMenu;
import codelab.utils.MyFileUtils;
import codelab.utils.HTMLInfoDialog;
import codelab.utils.FontChooser;
import codelab.utils.Utils;

/**
*	Classe du menu CodeLab
*	@author Jérôme Lehuen
*	@version 12/09/26
*/

public class CodeLabMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	private CodeLab codelab;

	public class BiduleException extends Exception {
		public BiduleException(String errorMessage) {
			super(errorMessage);
		}
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CodeLabMenu(JComponent component, int x, int y) {

		codelab = CodeLab.INSTANCE;

		// ---------------------------------------
		// Effets sonores

		JCheckBoxMenuItem itemSoundButton = new JCheckBoxMenuItem(CodeLab.LABEL("itemSoundButton"), true);
		itemSoundButton.setSelected(CodeLab.SOUND_EFFECTS); // Etat courant
		itemSoundButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemSoundButton.getState();
				CodeLab.SOUND_EFFECTS = value;
			}
		});

		// ---------------------------------------
		// Texte sous les boutons

		JCheckBoxMenuItem itemTextButton = new JCheckBoxMenuItem(CodeLab.LABEL("itemToolButton"), true);
		itemTextButton.setSelected(CodeLab.TEXT_UNDER_TOOLBUTTON); // Etat courant
		itemTextButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemTextButton.getState();
				CodeLab.TEXT_UNDER_TOOLBUTTON = value;
				codelab.updateAllToolbars();
			}
		});

		// ---------------------------------------
		// Icones dans le file manager

		JCheckBoxMenuItem itemIconButton = new JCheckBoxMenuItem(CodeLab.LABEL("itemIconFilemanager"), true);
		itemIconButton.setSelected(CodeLab.FILE_MANAGER_ICONS); // Etat courant
		itemIconButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemIconButton.getState();
				CodeLab.FILE_MANAGER_ICONS = value;
				codelab.getEditor().getManager().repaint();
			}
		});

		// ---------------------------------------
		// Police de l'éditeur

		JMenuItem itemFontButton = new JMenuItem(CodeLab.LABEL("itemFontButton"));
		itemFontButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				FontChooser dialog = new FontChooser();
				if (dialog.getValidation()) {
					String name = dialog.getSelectedName();
					int size = dialog.getSelectedSize();
					CodeLab.FONT_NAME = name;
					CodeLab.FONT_SIZE = size;
					codelab.getEditor().changeFont(name, size);
					PropertyBase.setUserProperty("FONT_NAME", name);
					PropertyBase.setUserProperty("FONT_SIZE", Integer.toString(size));
				}
			}
		});

		// ---------------------------------------
		// Code Folding de l'éditeur

		JCheckBoxMenuItem itemCodeFolding = new JCheckBoxMenuItem(CodeLab.LABEL("itemCodeFolding"), true);
		itemCodeFolding.setSelected(CodeLab.CODE_FOLDING); // Etat courant
		itemCodeFolding.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemCodeFolding.getState();
				CodeLab.CODE_FOLDING = value;
				codelab.getEditor().setCodeFoldingEnabled(value);
			}
		});

		// ---------------------------------------
		// Auto-complétion de l'éditeur

		JCheckBoxMenuItem itemAutoCompletion = new JCheckBoxMenuItem(CodeLab.LABEL("itemAutoCompletion"), true);
		itemAutoCompletion.setSelected(CodeLab.AUTO_COMPLETE); // Etat courant
		itemAutoCompletion.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemAutoCompletion.getState();
				CodeLab.AUTO_COMPLETE = value;
			}
		});

		// ---------------------------------------
		// Caractères invisibles de l'éditeur

		JCheckBoxMenuItem itemInvisibles = new JCheckBoxMenuItem(CodeLab.LABEL("itemInvisibles"), false);
		itemInvisibles.setSelected(CodeLab.SHOW_INVISIBLE); // Etat courant
		itemInvisibles.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemInvisibles.getState();
				CodeLab.SHOW_INVISIBLE = value;
				codelab.getEditor().setInvisible(value);
			}
		});

		// ---------------------------------------
		// Menu console

		JCheckBoxMenuItem itemAutoconsole = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_1"));
		itemAutoconsole.setSelected(codelab.getAutoconsole()); // Etat courant
		itemAutoconsole.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				ConsolePopupMenu.action_autoconsole();
			}
		});

		JCheckBoxMenuItem itemAutoclear = new JCheckBoxMenuItem(CodeLab.LABEL("ConsolePopupMenu_2"));
		itemAutoclear.setSelected(codelab.getAutoclear()); // Etat courant
		itemAutoclear.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				ConsolePopupMenu.action_autoclear();
			}
		});

		// ---------------------------------------
		// Activation des modules

		JMenuItem itemModules = new JMenuItem(CodeLab.LABEL("itemModules"));
		itemModules.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				ArrayList<JCheckBoxMenuItem> checkList = new ArrayList<JCheckBoxMenuItem>();
				String state1 = ""; // États des modules avant
				String state2 = ""; // États des modules après

				JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				for (String name : CodeLab.INSTANCE.getModuleDisp()) {
					String title = CodeLab.INSTANCE.getModuleTitle(name);
					boolean isactive = codelab.isActiveModule(name);
					JCheckBoxMenuItem item = new JCheckBoxMenuItem(title, isactive);
					item.setName(name); // Le nom lisible du module
					panel.add(item);
					checkList.add(item);
					state1 += isactive;
				}
				JOptionPane.showMessageDialog(CodeLab.FRAME,
					panel,
					CodeLab.LABEL("MODULEPANEL_TITLE"),
					JOptionPane.INFORMATION_MESSAGE,
					CodeLab.ICON);

				for (JCheckBoxMenuItem item : checkList)
					state2 += item.getState();

				if (!state1.equals(state2))
					changeModuleState(checkList);
			}
		});

		JMenuItem itemDownloadModules = new JMenuItem(CodeLab.LABEL("itemDownloadModules"));
		itemDownloadModules.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String url = String.format("%s/downloads/downloads-%s.php#plugins", CodeLab.CODELAB_URL, CodeLab.LANG);
				Utils.openBrowser(url);
			}
		});

		// ---------------------------------------
		// Fichier de logs

		JMenuItem itemShowLogfile = new JMenuItem(CodeLab.LABEL("itemShowLogfile"));
		itemShowLogfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (ConsoleLog.OPENNED) ConsoleLog.raise();
				else ConsoleLog.open(CodeLab.IS_WINDOWS);
			}
		});

		// ---------------------------------------
		// Ouvrir le dossier des plugins

		JMenuItem itemModuleFolder = new JMenuItem(CodeLab.LABEL("itemModuleFolder"));
		itemModuleFolder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				codelab.openModulesFolder();
			}
		});

		// ---------------------------------------
		// Créer un nouveau module

		JMenuItem itemNewModule = new JMenuItem(CodeLab.LABEL("itemNewModule"));
		itemNewModule.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String name = (String) JOptionPane.showInputDialog(codelab,
					CodeLab.LABEL("newModuleMessage"),
					CodeLab.LABEL("newModuleTitle"),
					JOptionPane.PLAIN_MESSAGE,
					CodeLab.ICON, null,
					"ModuleName");

				String path = String.format("%s/%s", CodeLab.MODULES_FOLDER, name);
				if (name == null || name.isBlank() || name.contains(" "))  {
					Utils.showWarningDialog("ATTENTION", CodeLab.LABEL("moduleNameERR1"));
					return;
				}
				if (Utils.folderExists(path) || Utils.fileExists(path + ".pac"))  {
					Utils.showWarningDialog("ATTENTION", CodeLab.LABEL("moduleNameERR2"));
					return;
				}
				if (!codelab.buildNewModule(name)) {
					Utils.showWarningDialog("ATTENTION", CodeLab.LABEL("moduleNameERR3"));
					return;
				}
			}
		});

		// ---------------------------------------
		// Éditer le fichier de configuration

		JMenuItem itemEditConfigFile = new JMenuItem(CodeLab.LABEL("itemEditConfigFile"));
		itemEditConfigFile.setEnabled(!codelab.isRunning());
		itemEditConfigFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				codelab.getEditor().getManager().resetSelectedNode(); // Déselectionner le node
				codelab.editPropFile(true);  // Avec pré-sauvegarde
			}
		});

		// ---------------------------------------
		// Restaurer le fichier de configuration

		JMenuItem itemRestoreConfigFile = new JMenuItem(CodeLab.LABEL("itemRestoreConfigFile"));
		itemRestoreConfigFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String message = CodeLab.LABEL("confirmRestoreConfigFile");
				if (Utils.confirmDialog(message)) {
					MyFileUtils.copy(CodeLab.USER_PROP_BAK, CodeLab.USER_PROP_FILE);
					codelab.editPropFile(false); // Sans pré-sauvegarde
				}
			}
		});

		// ---------------------------------------
		// Changement de langue

		JMenuItem itemLanguage = new JMenuItem(CodeLab.LABEL("itemLanguage"));
		itemLanguage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {

				JPanel panel = new JPanel();
				BoxLayout boxlayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
				panel.setLayout(boxlayout);

				ButtonGroup group = new ButtonGroup();
				JRadioButton en = new JRadioButton("English");
				JRadioButton fr = new JRadioButton("Français");
				panel.add(en); group.add(en); if (CodeLab.LANG.equals("en")) en.setSelected(true);
				panel.add(fr); group.add(fr); if (CodeLab.LANG.equals("fr")) fr.setSelected(true);

				JOptionPane.showMessageDialog(CodeLab.FRAME,
					panel,
					CodeLab.LABEL("LANGPANEL_TITLE"),
					JOptionPane.INFORMATION_MESSAGE,
					CodeLab.ICON);
				if (en.isSelected() && !CodeLab.LANG.equals("en")) changeLanguage("en");
				if (fr.isSelected() && !CodeLab.LANG.equals("fr")) changeLanguage("fr");
			}
		});

		// ---------------------------------------
		// Site web de CodeLab

		JMenuItem itemGotoHomepage = new JMenuItem(CodeLab.LABEL("itemGotoHomepage"));
		itemGotoHomepage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Utils.openBrowser(CodeLab.CODELAB_URL);
			}
		});

		// ---------------------------------------
		// Fenêtre d'informations

		JMenuItem itemInformations = new JMenuItem(CodeLab.LABEL("itemInformations"));
		itemInformations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				new HTMLInfoDialog();
			}
		});

		// ---------------------------------------
		// Vérifier la version

		JMenuItem itemCheckVersion = new JMenuItem(CodeLab.LABEL("itemCheckVersion"));
		itemCheckVersion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				switch (CodeLab.checkVersion()) {
					case 1: JOptionPane.showMessageDialog(CodeLab.FRAME,
								CodeLab.LABEL("NO_NEW_VERSION"),
								CodeLab.LABEL("VERSION_TITLE"),
								JOptionPane.INFORMATION_MESSAGE, CodeLab.ICON); break;
					case 2: JOptionPane.showMessageDialog(CodeLab.FRAME,
								CodeLab.LABEL("NO_NETWORK"),
								CodeLab.LABEL("VERSION_TITLE"),
								JOptionPane.WARNING_MESSAGE, CodeLab.ICON); break;
				}
			}
		});

		// ---------------------------------------
		// Quitter / Relancer / Test reporting

		JMenuItem itemRelaunchCodeLab = new JMenuItem(CodeLab.LABEL("itemRelaunchCodeLab"));
		itemRelaunchCodeLab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				codelab.things_to_do_before_exiting();
				CodeLab.relaunch();
			}
		});

		JMenuItem itemQuitCodeLab = new JMenuItem(CodeLab.LABEL("itemQuitCodeLab"));
		itemQuitCodeLab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				codelab.things_to_do_before_exiting();
				CodeLab.exit();
			}
		});

		JMenuItem itemTest1 = new JMenuItem("Test reporting #1");
		itemTest1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				BiduleException ex = new BiduleException("---------- brain is null ----------");
				ExceptionManager.process(ex);
			}
		});

		JMenuItem itemTest2 = new JMenuItem("Test reporting #2");
		itemTest2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ExceptionManager.process(null);
			}
		});

		// ---------------------------------------
		// Création et affichage du menu

		JMenu menuConfigFile = new JMenu(CodeLab.LABEL("menuConfigFile"));
		menuConfigFile.add(itemLanguage);
		menuConfigFile.add(itemEditConfigFile);
		menuConfigFile.add(itemRestoreConfigFile);
		if (CodeLab.REDIRECT) menuConfigFile.add(itemShowLogfile);

		JMenu menuEditor = new JMenu(CodeLab.LABEL("menuEditor"));
		menuEditor.add(itemAutoCompletion);
		menuEditor.add(itemCodeFolding);
		menuEditor.add(itemInvisibles);
		menuEditor.add(itemFontButton);

		JMenu menuConsole = new JMenu(CodeLab.LABEL("Btn_term"));
		menuConsole.add(itemAutoconsole);
		menuConsole.add(itemAutoclear);

		JMenu menuPlugins = new JMenu(CodeLab.LABEL("menuPlugins"));
		menuPlugins.add(itemModules);
		menuPlugins.add(itemDownloadModules);
		menuPlugins.add(itemNewModule);
		if (Desktop.isDesktopSupported()) menuPlugins.add(itemModuleFolder);

		add(itemSoundButton);
		add(itemTextButton);
		//add(itemIconButton);
		add(new JSeparator());
		add(menuEditor);
		add(menuConsole);
		add(menuPlugins);
		add(menuConfigFile);
		add(new JSeparator());
		add(itemGotoHomepage);
		add(itemInformations);
		add(itemCheckVersion);
		add(new JSeparator());
		add(itemRelaunchCodeLab);
		add(itemQuitCodeLab);
		if (CodeLab.TEST_REPORTING) add(itemTest1);
		if (CodeLab.TEST_REPORTING) add(itemTest2);

		show(component, x, y);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void changeLanguage(String lang) {
		// Changement de langue
		if (MyFileUtils.findAndReplace(CodeLab.USER_PROP_FILE, "LANG=\\w+", "LANG="+lang)) {
			JOptionPane.showMessageDialog(CodeLab.FRAME,
				CodeLab.LABEL("RELAUNCH"),
				CodeLab.LABEL("LANGPANEL_TITLE"),
				JOptionPane.INFORMATION_MESSAGE,
				CodeLab.ICON);
			codelab.things_to_do_before_exiting();
			CodeLab.relaunch();
		}
	}

	private void changeModuleState(ArrayList<JCheckBoxMenuItem> checkList) {
		// Activation des modules
		for (JCheckBoxMenuItem item : checkList) {
			codelab.setActivableModule(item.getName(), item.getState());
		}
		JOptionPane.showMessageDialog(CodeLab.FRAME,
			CodeLab.LABEL("RELAUNCH"),
			CodeLab.LABEL("MODULEPANEL_TITLE"),
			JOptionPane.INFORMATION_MESSAGE,
			CodeLab.ICON);
		codelab.things_to_do_before_exiting();
		CodeLab.relaunch();
	}
}

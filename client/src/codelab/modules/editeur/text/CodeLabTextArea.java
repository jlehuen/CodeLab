package codelab.modules.editeur.text;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultCaret;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.client.ChatController;
import codelab.client.UserData;
import codelab.utils.ResourceUtils;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
*	Classe héritée du RSyntaxTextArea
*	@author Jérôme Lehuen
*	@version 11/05/25

extended by javax.swing.JComponent
	extended by javax.swing.text.JTextComponent
		extended by javax.swing.JTextArea
			extended by org.fife.ui.rtextarea.RTextArea
				extended by org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
					extended by codelab.modules.editeur.CodeLabTextArea
*/

// http://bobbylight.github.io/RSyntaxTextArea/

// https://github.com/bobbylight/RSyntaxTextArea
// https://github.com/bobbylight/RSyntaxTextArea/wiki

// https://github.com/bobbylight/RSTAUI
// https://github.com/bobbylight/AutoComplete
// https://github.com/bobbylight/RSTALanguageSupport

// http://javadoc.fifesoft.com/rsyntaxtextarea/org/fife/ui/rtextarea/RTextArea.html
// http://javadoc.fifesoft.com/rsyntaxtextarea/org/fife/ui/rsyntaxtextarea/RSyntaxTextArea.html

// https://javadoc.fifesoft.com/autocomplete/org/fife/ui/autocomplete/package-summary.html
// https://javadoc.io/static/com.fifesoft/languagesupport/3.0.1/overview-summary.html

public class CodeLabTextArea extends RSyntaxTextArea {

	private String syntaxe;
	private String comment;
	private AutoCompletion ac = null;

	private boolean ctrl = false;

	private JMenu menuConvert = new JMenu();
	private JMenuItem itemTchat = new JMenuItem();
	private JMenuItem itemControl = new JMenuItem();
	private JMenuItem itemResetHelp = new JMenuItem();
	private JMenuItem itemClone = new JMenuItem();
	private JMenuItem itemDeleteClone = new JMenuItem();
	private JMenuItem itemConvertSpaceToTab = new JMenuItem();
	private JMenuItem itemConvertTabToSpace = new JMenuItem();
	private JCheckBoxMenuItem itemDiff = new JCheckBoxMenuItem();
	private JCheckBoxMenuItem itemInvisibles = new JCheckBoxMenuItem();

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CodeLabTextArea(TextEditor editor, String ext) {
		super();
		switch (ext) {
			case ".py":
				comment = "##";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_PYTHON;
				autoCompletion(provider_PYTHON);
				break;
			case ".java":
			case ".pde":
				comment = "//";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_JAVA;
				autoCompletion(provider_JAVA);
				break;
			case ".lsp":
			case ".clp":
				comment = ";;";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_LISP;
				autoCompletion(provider_LISP);
				break;
			case ".lua":
				comment = "--";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_LUA;
				autoCompletion(provider_LUA);
				break;
			case ".c":
			case ".h":
				comment = "//";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_C;
				autoCompletion(provider_C);
				break;
			case ".html":
				comment = "<!--";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_HTML;
				//autoCompletion(provider_HTML);
				break;
			case ".css":
				comment = "/*";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_CSS;
				//autoCompletion(provider_CSS);
				break;
			case ".go":
				comment = "//";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_GO;
				autoCompletion(provider_GO);
				break;
			default:
				comment = "??";
				syntaxe = SyntaxConstants.SYNTAX_STYLE_NONE;
		}

		setMarginLineEnabled(true);
		setCloseCurlyBraces(true);
		setAutoIndentEnabled(true);
		setFadeCurrentLineHighlight(true);
		setAntiAliasingEnabled(true);

		setSyntaxEditingStyle(syntaxe);
		setFont(new Font(CodeLab.FONT_NAME, Font.PLAIN, CodeLab.FONT_SIZE));
		setCodeFoldingEnabled(CodeLab.CODE_FOLDING);
		setWhitespaceVisible(CodeLab.SHOW_INVISIBLE);

		setEditable(true);
		setEnabled(true);
		requestFocus();

		// KeyListener pour vérifier la touche [ctrl]
		addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}

			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_META ||
				    code == KeyEvent.VK_CONTROL) ctrl = true;
			}

			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_META ||
					code == KeyEvent.VK_CONTROL) ctrl = false;
			}
		});

		// KeyListener pour l'auto-complétion
		addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {
				if (CodeLab.AUTO_COMPLETE && (ac != null) && !ctrl) {
					char ch = e.getKeyChar();
					if (Character.isAlphabetic(ch)) {
						ac.doCompletion();
					}
				}
			}
		});

		/*
		// MouseListener pour l'actualisation
		addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {
				System.out.println("coucou");
			}
		});
		*/

		// DocumentListener pour le flag de modification
		getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {}
			public void insertUpdate(DocumentEvent e) {}
			public void changedUpdate(DocumentEvent e) {
				editor.setModified(true); // Fichier modifié
				editor.resetCompilationFlag(); // Doit être recompilé
			}
		});

		///////////////////////////////////////////////////
		// Enrichissement du menu contextuel
		///////////////////////////////////////////////////

		JPopupMenu popup = getPopupMenu();

		popup.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				itemInvisibles.setText(CodeLab.LABEL("itemInvisibles"));
				itemInvisibles.setState(CodeLab.SHOW_INVISIBLE);
				itemConvertSpaceToTab.setText(CodeLab.LABEL("itemConvertSpaceToTab"));
				itemConvertTabToSpace.setText(CodeLab.LABEL("itemConvertTabToSpace"));
				menuConvert.setText(CodeLab.LABEL("itemConvert"));
				menuConvert.setEnabled(editor.getEditable());
			}
		});

		itemInvisibles.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean value = itemInvisibles.getState();
				CodeLab.SHOW_INVISIBLE = value;
				CodeLab.INSTANCE.getEditor().setInvisible(value);
			}
		});

		itemConvertSpaceToTab.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int nbspaces = 4;
				CodeLab.INSTANCE.getEditor().convertSpaceToTab(nbspaces);
			}
		});

		itemConvertTabToSpace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int nbspaces = 4;
				CodeLab.INSTANCE.getEditor().convertTabToSpace(nbspaces);
			}
		});

		menuConvert.add(itemConvertSpaceToTab);
		menuConvert.add(itemConvertTabToSpace);
		popup.add(menuConvert);
		popup.add(itemInvisibles);

		if (CodeLab.INSTANCE.isTutor() || CodeLab.INSTANCE.isAdmin()) {

			popup.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					// Mise à jour des items au moment de l'ouverture du menu
					UserData udata = editor.module.getCurrentUdata();
					boolean isconnected = (udata != null) && udata.isConnected();
					boolean helpFlag = (udata != null) && udata.getHelpFlag();
					boolean iscontrolled = (udata != null) && udata.isControlled();
					String label = iscontrolled ? CodeLab.LABEL("UserTable_16b") : CodeLab.LABEL("UserTable_16a");
					itemControl.setText(String.format(label, udata));
					itemControl.setEnabled(true);
					itemTchat.setText(String.format(CodeLab.LABEL("UserTable_4"), udata));
					itemTchat.setEnabled(isconnected);
					itemResetHelp.setText(String.format(CodeLab.LABEL("itemResetCallFlag"), udata));
					itemResetHelp.setEnabled(isconnected && helpFlag);
					itemDiff.setText(CodeLab.LABEL("itemDiff"));
					itemDiff.setState(editor.module.diff);

					boolean isClone = editor.module.isCloneCurrentFile();
					if (isClone) {
						itemClone.setVisible(false);
						itemDeleteClone.setVisible(true);
						itemDeleteClone.setText(CodeLab.LABEL("Delete_current_clone"));
					} else {
						itemClone.setVisible(true);
						itemClone.setText(CodeLab.LABEL("Clone_program"));
						itemDeleteClone.setVisible(false);
					}
				}
			});

			itemClone.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					editor.module.cloneCurrentFile();
				}
			});

			itemDeleteClone.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					editor.module.deleteCurrentClone();
				}
			});

			itemTchat.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.module.getCurrentUdata();
					if (udata == null) return;
					ChatController.openChatWith(udata);
					CodeLab.INSTANCE.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			itemControl.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.module.getCurrentUdata();
					if (udata == null) return;
					boolean value = udata.isControlled();
					CodeLab.INSTANCE.getClient().askControlMode(udata.getLogin(), !value);
				}
			});

			itemResetHelp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = editor.module.getCurrentUdata();
					if (udata == null) return;
					CodeLab.INSTANCE.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			itemDiff.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					editor.module.diff = itemDiff.getState();
					if (!editor.module.diff)
						editor.removeAllLineHighlights(); // Effacer le précédent marquage
				}
			});

			popup.add(itemDiff);
			popup.add(itemClone);
			popup.add(itemDeleteClone);
			popup.addSeparator();
			popup.add(itemTchat);
			popup.add(itemResetHelp);
			popup.add(itemControl);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private static DefaultCompletionProvider provider_PYTHON = createProvider("keywords/keywords_python.txt");
	private static DefaultCompletionProvider provider_JAVA = createProvider("keywords/keywords_java.txt");
	private static DefaultCompletionProvider provider_LISP = createProvider("keywords/keywords_lisp.txt");
	private static DefaultCompletionProvider provider_LUA = createProvider("keywords/keywords_lua.txt");
	private static DefaultCompletionProvider provider_GO = createProvider("keywords/keywords_go.txt");
	private static DefaultCompletionProvider provider_C = createProvider("keywords/keywords_c.txt");

	private static DefaultCompletionProvider createProvider(String filename) {
		//System.out.format("Loading %s\n", filename);
		DefaultCompletionProvider provider = new DefaultCompletionProvider();
		ArrayList<String> keywords = ResourceUtils.readResourceTextFile(filename);
		for (String keyword : keywords) {
			if (!keyword.isEmpty() && !keyword.isBlank()) {
				//System.out.println(keyword);
				provider.addCompletion(new BasicCompletion(provider, keyword));
			}
		}
		return provider;
	}

	private void autoCompletion(DefaultCompletionProvider provider) {
		ac = new AutoCompletion(provider);
		ac.setAutoCompleteSingleChoices(false);
		ac.install(this);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public boolean isBlank() {
		return getText().isBlank();
	}

	public String getCommentStr() {
		return comment;
	}

	public void setCodeFoldingEnabled(boolean value) {
		super.setCodeFoldingEnabled(value);
		getFoldManager().reparse();
	}

	public void updateCaret(boolean flag) {
		// For maintaing JTextArea scroll position
		DefaultCaret caret = (DefaultCaret) getCaret();
		if (flag) caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		else caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
	}

	public void updateMenuItems(String text) {
///		itemControl.setText(text);
	}

	public void findAndReplace(String oldstr, String newstr) {
		SwingUtilities.invokeLater(() -> {
			String content = getText();
			setText(content.replace(oldstr, newstr));
		});
	}

	public void paintComponent(Graphics g) {
		try {
			super.paintComponent(g);
		}
		catch (Exception e) {
			// Nothing can be done, so just ignore it...
			// Pour masquer ces ... d'exceptions de ... sur le RSyntaxTextArea !!
		}
	}

	///////////////////////////////////////////////////
	// Sauvegarde et rechargement
	///////////////////////////////////////////////////

	public synchronized void save_content(File file) {
		try (	FileOutputStream fos = new FileOutputStream(file);
				Writer out = new OutputStreamWriter(fos, "UTF-8")) {

			write(out);
		}
		catch (Exception e) {
			ExceptionManager.process(e);
		}
	}

	public synchronized void load_content(File file) {
		try (	FileReader reader = new FileReader(file);
				BufferedReader in = new BufferedReader(reader)) {

			read(in, null);
		}
		catch (Exception e) {
			ExceptionManager.process(e);
		}
	}
}

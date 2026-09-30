package codelab.console;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import codelab.AbstractCodeLab;
import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.Executeur;
import codelab.utils.Utils;

/**
*	Classe de la console texte optimisée, fluide et ordonnée
*	@author Jérôme Lehuen + Antigravity
*	@version 25/09/26
*/

public class Console extends JScrollPane implements ConsoleInterface, KeyListener, MouseListener, Runnable, ClipboardOwner {

	public static final Color COLOR_PRINT = getColorProperty("CONSOLE_PRINTCOLOR", Color.GREEN);
	public static final Color COLOR_INPUT = getColorProperty("CONSOLE_INPUTCOLOR", Color.MAGENTA);
	public static final Color COLOR_ERROR = getColorProperty("CONSOLE_ERRORCOLOR", new Color(255, 50, 50)); // Ne doit pas être obfusqué !!
	public static final Color COLOR_LOG = getColorProperty("CONSOLE_FGCOLOR", Color.LIGHT_GRAY);
	public static final Color COLOR_BACKGROUND = getColorProperty("CONSOLE_BGCOLOR", new Color(30, 40, 50));

	private CodeLab codelab;
	private JTextPane textPane;
	private StyledDocument document;
	private Style defaultStyle;
	private String fontName;

	private Executeur executeur = null; // Exécuteur courant pour la saisie
	private boolean prev_caret = false; // Présence d'un curseur
	private String saisie = ""; // Buffer de saisie

	///////////////////////////////////////////////////
	// Gestion de la file d'attente fluide (Batching FIFO)

	private static class Chunk {
		final String text;
		final Color color;
		final boolean caret;
		final String linkUrl; // null si texte standard

		Chunk(String text, Color color, boolean caret, String linkUrl) {
			this.text = text;
			this.color = color;
			this.caret = caret;
			this.linkUrl = linkUrl;
		}
	}

	private static final int QUEUE_SIZE = 10000; // Taille maximale de la file d'attente
	private final BlockingQueue<Chunk> queue = new LinkedBlockingQueue<Chunk>(QUEUE_SIZE);
	private final Timer flushTimer;

	private void enqueue(Chunk chunk) {
		if (chunk == null || chunk.text == null || chunk.text.isEmpty()) return;
		if (SwingUtilities.isEventDispatchThread()) {
			if (queue.remainingCapacity() <= 0) {
				drainQueue();
			}
			queue.offer(chunk);
		}
		else {
			try {
				queue.put(chunk);
			}
			catch (InterruptedException e) {
				ExceptionManager.process(e);
			}
		}
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Console(CodeLab codelab) {
		this.codelab = codelab;
		document = new DefaultStyledDocument(new StyleContext());
		textPane = new JTextPane(document);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		textPane.setBackground(COLOR_BACKGROUND);
		textPane.addMouseListener(this);
		textPane.addKeyListener(this);
		textPane.setFocusable(true);
		textPane.setEditable(false);
		textPane.setHighlighter(new DefaultHighlighter() {
			@Override
			public void paint(java.awt.Graphics g) {
				try {
					super.paint(g);
				}
				catch (ArrayIndexOutOfBoundsException ignored) {}
			}
		});

		fontName = AbstractCodeLab.SYSTEM.equals("Windows") ? "Consolas" : "Monospaced";

		// Style par défaut
		defaultStyle = textPane.addStyle("Style", null);
		StyleConstants.setFontFamily(defaultStyle, fontName);
		StyleConstants.setFontSize(defaultStyle, 14);
		textPane.setActionMap(new ActionMap()); // Pour ne pas avoir le beep en cas de Backspace / Enter

		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		setBorder(BorderFactory.createEmptyBorder());
		setViewportView(textPane);
		addMouseListener(this);

		JScrollBar vertical = getVerticalScrollBar();
		vertical.setValue(vertical.getMaximum());

		// ---------------------------------------------------------------------------------------------------------
		// Aimant de focus global (quand un programme s'exécute, toute frappe clavier est captée par la console)
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (executeur == null) return false; // Hors exécution, comportement normal
			if (textPane.isFocusOwner()) return false; // La console a déjà le focus

			Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (focusOwner != null) {
				Window w = SwingUtilities.getWindowAncestor(focusOwner);
				if (w instanceof JDialog) return false; // Laisser les boîtes de dialogue
			}

			int id = e.getID();
			int code = e.getKeyCode();
			boolean isCtrl = e.isControlDown() || (CodeLab.IS_OSX && e.isMetaDown());

			if (code == KeyEvent.VK_ESCAPE) return false;
			if (isCtrl && code != KeyEvent.VK_C && code != KeyEvent.VK_V) {
				return false; // Laisser passer les raccourcis système / menus
			}

			if (id == KeyEvent.KEY_PRESSED) {
				focus();
				keyPressed(e);
				return true;
			}
			else if (id == KeyEvent.KEY_RELEASED) {
				keyReleased(e);
				return true;
			}
			else if (id == KeyEvent.KEY_TYPED) {
				return true;
			}
			return false;
		});

		// Timer de rafraîchissement fluide et groupé de la console (~60 FPS)
		flushTimer = new Timer(16, e -> drainQueue());
		flushTimer.start();
	}

	///////////////////////////////////////////////////
	// Méthode de Runnable (compatibilité)
	///////////////////////////////////////////////////

	public void run() {
		flush();
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void setExecuteur(Executeur executeur) {
		this.executeur = executeur;
	}

	public void focus() {
		SwingUtilities.invokeLater(() -> {
			textPane.grabFocus();
			textPane.requestFocus();
			textPane.requestFocusInWindow();
		});
	}

	public void reset() {
		queue.clear();
		resetInputLine();
		if (prev_caret) {
			delCaret();
			prev_caret = false;
		}
	}

	public void clear() {
		queue.clear();
		resetInputLine();
		prev_caret = false;
		Runnable clearTask = () -> {
			try {
				document.remove(0, document.getLength());
			}
			catch (BadLocationException e) {
				ExceptionManager.process(e);
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			clearTask.run();
		} else {
			SwingUtilities.invokeLater(clearTask);
		}
	}

	public void flush() {
		if (SwingUtilities.isEventDispatchThread()) {
			drainQueue();
		}
		else {
			try {
				SwingUtilities.invokeAndWait(this::drainQueue);
			}
			catch (Exception ignored) {
				// En cas d'interruption, flushTimer videra la file en toute sécurité sur l'EDT au prochain tick (<= 16ms)
			}
		}
	}

	public void print(String str, Color color, boolean caret) {
		if (str == null || str.isEmpty()) return;
		enqueue(new Chunk(str, color, caret, null));
	}

	public void print(char c, Color color, boolean caret) {
		enqueue(new Chunk(String.valueOf(c), color, caret, null));
	}

	public void directPrint(String str, Color color) {
		if (str == null || str.isEmpty()) return;

		// Analyseur de liens [[texte](url)]
		int state = 0;
		StringBuilder normalText = new StringBuilder();
		StringBuilder linkText = new StringBuilder();
		StringBuilder linkUrl = new StringBuilder();

		for (char ch : str.toCharArray()) {
			if (state == 0 && ch == '[') {
				state = 1;
			}
			else if (state == 1 && ch == '[') {
				state = 2;
				linkText.setLength(0);
			}
			else if (state == 1 && ch != '[') {
				normalText.append('[').append(ch);
				state = 0;
			}
			else if (state == 2 && ch == ']') {
				state = 3;
			}
			else if (state == 3 && ch == '(') {
				state = 4;
				linkUrl.setLength(0);
			}
			else if (state == 4 && ch == ')') {
				state = 5;
			}
			else if (state == 5 && ch == ']') {
				state = 0;
				// On a un lien complet [[linkText](linkUrl)]
				if (normalText.length() > 0) {
					enqueue(new Chunk(normalText.toString(), color, false, null));
					normalText.setLength(0);
				}
				enqueue(new Chunk(linkText.toString(), Color.CYAN, false, linkUrl.toString()));
			}
			else {
				if (state == 2) linkText.append(ch);
				else if (state == 4) linkUrl.append(ch);
				else normalText.append(ch);
			}
		}

		// Traiter les résidus éventuels d'états incomplets
		if (state == 1) normalText.append('[');
		else if (state == 2) normalText.append("[[").append(linkText);
		else if (state == 3) normalText.append("[[").append(linkText).append(']');
		else if (state == 4) normalText.append("[[").append(linkText).append("](").append(linkUrl);
		else if (state == 5) normalText.append("[[").append(linkText).append("](").append(linkUrl).append(')');

		if (normalText.length() > 0) {
			enqueue(new Chunk(normalText.toString(), color, false, null));
		}
	}

	public void directPrintln(String str, Color color) {
		directPrint(str + "\n", color);
	}

	///////////////////////////////////////////////////
	// Traitement groupé de la file (Exécuté sur l'EDT)
	///////////////////////////////////////////////////

	private synchronized void drainQueue() {
		if (queue.isEmpty()) {
			return;
		}

		List<Chunk> batch = new ArrayList<>();
		queue.drainTo(batch);

		if (batch.isEmpty()) {
			return;
		}

		try {
			if (prev_caret) {
				delCaret();
				prev_caret = false;
			}

			boolean lastCaret = false;
			StringBuilder textBuffer = new StringBuilder();
			Color currentColor = null;

			for (Chunk chunk : batch) {
				lastCaret = chunk.caret;

				if (chunk.linkUrl != null) {
					// Vider le buffer accumulé
					if (textBuffer.length() > 0) {
						StyleConstants.setForeground(defaultStyle, currentColor != null ? currentColor : COLOR_PRINT);
						document.insertString(document.getLength(), textBuffer.toString(), defaultStyle);
						textBuffer.setLength(0);
						currentColor = null;
					}

					// Insérer le lien cliquable
					SimpleAttributeSet linkAttrs = new SimpleAttributeSet();
					StyleConstants.setFontFamily(linkAttrs, fontName);
					StyleConstants.setFontSize(linkAttrs, 14);
					StyleConstants.setForeground(linkAttrs, Color.CYAN);
					StyleConstants.setUnderline(linkAttrs, true);
					linkAttrs.addAttribute("linkact", new ConsoleLinkAction(chunk.linkUrl));
					document.insertString(document.getLength(), chunk.text, linkAttrs);
				}
				else {
					// Chunk de texte ordinaire
					if (currentColor == null) {
						currentColor = chunk.color;
						textBuffer.append(chunk.text);
					}
					else if (currentColor.equals(chunk.color)) {
						textBuffer.append(chunk.text);
					}
					else {
						// Changement de couleur : vider le buffer précédent
						StyleConstants.setForeground(defaultStyle, currentColor);
						document.insertString(document.getLength(), textBuffer.toString(), defaultStyle);
						textBuffer.setLength(0);
						currentColor = chunk.color;
						textBuffer.append(chunk.text);
					}
				}
			}

			// Vider le reste du buffer
			if (textBuffer.length() > 0) {
				StyleConstants.setForeground(defaultStyle, currentColor != null ? currentColor : COLOR_PRINT);
				document.insertString(document.getLength(), textBuffer.toString(), defaultStyle);
			}

			// Limiter le nombre de lignes dans le buffer
			Element root = document.getDefaultRootElement();
			int lineCount = root.getElementCount();
			int maxLines = (CodeLab.BUFFER_SIZE > 0) ? CodeLab.BUFFER_SIZE : 200;
			if (lineCount > maxLines) {
				int excess = lineCount - maxLines;
				int endOffset = root.getElement(excess - 1).getEndOffset();
				document.remove(0, endOffset);
			}

			// Ajouter le curseur si demandé
			if (lastCaret) {
				addCaret();
				prev_caret = true;
			}

			// Faire défiler vers le bas si aucun texte n'est sélectionné
			if (textPane.getSelectedText() == null) {
				textPane.getCaret().setDot(document.getLength());
			}
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Gestion du copier-coller
	///////////////////////////////////////////////////

	public boolean isTextSelected() {
		String text = textPane.getSelectedText();
		return text != null;
	}

	public boolean hasTransferableText() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		return (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
	}

	public void copySelection() {
		String text = textPane.getSelectedText();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(text), this);
	}

	public void pasteClipboard() {
		String str = getClipboardContent();
		if (executeur == null) {
			// Hors exécution
			print(str, COLOR_INPUT, false);
		} else {
			// Mode exécution
			drainQueue();
			_print_(str, COLOR_INPUT, true);
			saisie += str;
		}
	}

	private String getClipboardContent() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		String text = "";
		if (hasTransferableText) {
			try {
				text = (String) contents.getTransferData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException | IOException e) {
				ExceptionManager.process(e);
			}
		}
		return text;
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private static Color getColorProperty(String name, Color color) {
		try {
			String str = CodeLab.PROP(name);
			if (str == null) return color;
			Pattern p = Pattern.compile("([\\d]+):([\\d]+):([\\d]+)");
			Matcher m = p.matcher(str);
			if (m.find()) {
				int r = Integer.parseInt(m.group(1));
				int v = Integer.parseInt(m.group(2));
				int b = Integer.parseInt(m.group(3));
				return new Color(r, v, b);
			}
		}
		catch (Exception ignored) {}
		return color;
	}

	private void resetInputLine() {
		saisie = "";
	}

	private void delCaret() {
		try {
			int pos = document.getLength() - 1;
			if (pos >= 0) document.remove(pos, 1);
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	private void addCaret() {
		try {
			StyleConstants.setForeground(defaultStyle, Color.GREEN);
			document.insertString(document.getLength(), "_", defaultStyle);
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	private void _print_(String str, Color color, boolean caret) {
		try {
			if (prev_caret) delCaret();
			StyleConstants.setForeground(defaultStyle, color);
			document.insertString(document.getLength(), str, defaultStyle);
			if (caret) addCaret();
			prev_caret = caret;
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
		if (textPane.getSelectedText() == null) {
			textPane.getCaret().setDot(document.getLength());
		}
	}

	private void _backspace_() {
		try {
			if (prev_caret) delCaret();
			if (document.getLength() > 0) {
				document.remove(document.getLength() - 1, 1);
			}
			if (prev_caret) addCaret();
		}
		catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
		if (textPane.getSelectedText() == null) {
			textPane.getCaret().setDot(document.getLength());
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface KeyListener
	///////////////////////////////////////////////////

	private boolean CTRL = false; // Pour le copier-coller

	public void keyTyped(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		int code = e.getKeyCode();
		if (code == KeyEvent.VK_CONTROL) CTRL = false;
		if (code == KeyEvent.VK_META && CodeLab.IS_OSX) CTRL = false;
	}

	public void keyPressed(KeyEvent e) {
		char c = e.getKeyChar();
		int code = e.getKeyCode();

		// Pour copier dans le clipboard
		if (code == KeyEvent.VK_CONTROL) CTRL = true;
		if (code == KeyEvent.VK_META && CodeLab.IS_OSX) CTRL = true;
		if (CTRL && code == KeyEvent.VK_C) copySelection();

		if (executeur == null) return; // Plus rien à tester hors-exécution

		if (CTRL && code == KeyEvent.VK_V && hasTransferableText()) {
			// Coller le contenu du clipboard
			pasteClipboard();
		}
		else if (!CTRL && Utils.isPrintable(c) && c != '^' && c != '¨') {
			// Caractère imprimable
			String str = String.valueOf(c);
			drainQueue();
			_print_(str, COLOR_INPUT, true);
			saisie += str;
		}
		else if (code == KeyEvent.VK_ENTER) {
			// Touche ENTER
			drainQueue();
			_print_("\n", COLOR_INPUT, true);
			executeur.write(saisie + "\n");
			resetInputLine();
		}
		else if (code == KeyEvent.VK_BACK_SPACE) {
			// Touche BACKSPACE
			if (saisie.length() > 0) {
				drainQueue();
				_backspace_();
				saisie = saisie.substring(0, saisie.length() - 1);
			}
			else Utils.beep();
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface MouseListener
	///////////////////////////////////////////////////

	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mouseClicked(MouseEvent e) {
		Element element = document.getCharacterElement(textPane.viewToModel2D(e.getPoint()));
		AttributeSet attset = element.getAttributes();
		ConsoleLinkAction action = (ConsoleLinkAction) attset.getAttribute("linkact");
		if (action != null) action.execute(); // On a cliqué sur un lien
	}

	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			focus();
		}
		else if (SwingUtilities.isRightMouseButton(e)) {
			// Passer le textPane pour corriger le décalage vertical du JScrollPane
			new ConsolePopupMenu(codelab, textPane, e.getX(), e.getY());
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface ClipboardOwner
	///////////////////////////////////////////////////

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// Do nothing
	}
}

package codelab.modules.editeur.notebook;

import java.awt.Adjustable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.Arrays;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import codelab.CodeLab;
import codelab.utils.CodeBlockUtils;
import codelab.utils.LatexRenderer;
import codelab.utils.MarkdownUtils;
import codelab.utils.MermaidRenderer;

/**
 * Composant graphique représentant une cellule de carnet Jupyter (Code ou Markdown).
 * @author Jérôme Lehuen + Antigravity
 */
public class NotebookCellPane extends JPanel {

	private static final long serialVersionUID = 1L;

	public static final Color BORDER_NORMAL = new Color(218, 224, 233);
	private static final Color BORDER_FOCUSED = new Color(66, 133, 244);
	private static final Color BG_HEADER = new Color(248, 249, 250);
	private static final Color BG_HEADER_SELECTED = new Color(232, 240, 254);
	private static final Color BORDER_HEADER_SELECTED = new Color(195, 218, 251);
	//private static final Color COLOR_CODE_BADGE = new Color(30, 115, 190);
	//private static final Color COLOR_MD_BADGE = new Color(130, 60, 180);

	private static final Insets CARD_MARGIN = new Insets(3, 4, 4, 4);
	private static final int CARD_ARC = 14;

	private static final Parser MD_PARSER;
	private static final HtmlRenderer HTML_RENDERER;
	static {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
		options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
		MD_PARSER = Parser.builder(options).build();
		HTML_RENDERER = HtmlRenderer.builder(options).build();
	}

	private static class CssCacheEntry {
		final long lastModified;
		final String rawCss;

		CssCacheEntry(long lastModified, String rawCss) {
			this.lastModified = lastModified;
			this.rawCss = rawCss;
		}
	}

	private static final Map<String, CssCacheEntry> CSS_CACHE = new ConcurrentHashMap<>();
	private static String DEFAULT_CSS_RESOURCE = null;

	private static class NotebookHtmlEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 1L;
		private static final ViewFactory FACTORY = new HTMLFactory() {
			@Override
			public View create(Element elem) {
				AttributeSet attrs = elem.getAttributes();
				Object o = attrs.getAttribute(StyleConstants.NameAttribute);
				if (o instanceof HTML.Tag) {
					HTML.Tag kind = (HTML.Tag) o;
					if (kind == HTML.Tag.HTML || kind == HTML.Tag.BODY || kind == HTML.Tag.PRE || kind == HTML.Tag.TABLE) {
						// Permet au document, tableaux et blocs préformatés de s'adapter fluidement à toute largeur sans buter sur un minimumSpan
						return new BlockView(elem, View.Y_AXIS) {
							@Override
							public float getMinimumSpan(int axis) {
								if (axis == View.X_AXIS) return 0f;
								return super.getMinimumSpan(axis);
							}
						};
					}
				}
				View v = super.create(elem);
				if (v instanceof ImageView) {
					return new MathImageView(elem);
				}
				return v;
			}
		};

		@Override
		public ViewFactory getViewFactory() {
			return FACTORY;
		}
	}

	private static class MathImageView extends ImageView {
		public MathImageView(Element elem) {
			super(elem);
		}

		@Override
		public float getAlignment(int axis) {
			if (axis == View.Y_AXIS) {
				AttributeSet attr = getElement().getAttributes();
				Object bl = attr.getAttribute("data-baseline");
				if (bl != null) {
					try {
						return Float.parseFloat(bl.toString());
					} catch (Exception ignored) {
					}
				}
				Object align = attr.getAttribute(HTML.Attribute.ALIGN);
				if ("middle".equals(String.valueOf(align))) {
					return 0.72f;
				}
			}
			return super.getAlignment(axis);
		}
	}

	private final NotebookEditor editor;
	private String type; // "code" ou "markdown"
	private String language = "python"; // "python" ou "c"
	private boolean editable = true;

	public static final String OPT_PYTHON = "Langage Python";
	public static final String OPT_C = "Langage C";
	public static final String OPT_TEXT = "Markdown";

	// Éléments du bandeau supérieur
	private JPanel headerPanel;
	private JPanel leftBox, centerBox, rightBox;
	private JComboBox<String> typeSelector;
	private JButton btnRunCell;
	private JButton btnUp, btnDown, btnAddCode, btnAddMd, btnDelete;
	private CellLockButton btnLock;
	private boolean locked = false;

	// Conteneur central
	private JPanel centerPanel;

	// Composants cellule de code
	private RSyntaxTextArea codeArea;
	private RTextScrollPane codeScroll;

	// Composants cellule Markdown
	private CardLayout mdCardLayout;
	private JPanel mdCardPanel;
	private JEditorPane mdHtmlView;
	private RSyntaxTextArea mdArea;
	private RTextScrollPane mdScroll;
	private JButton btnValidateMd;
	private boolean isEditingMd = false;

	public NotebookCellPane(NotebookEditor editor, String type, String initialSource) {
		this(editor, type, "c".equalsIgnoreCase(type) ? "c" : "python", initialSource);
	}

	public NotebookCellPane(NotebookEditor editor, String type, String language, String initialSource) {
		this.editor = editor;
		this.type = "markdown".equalsIgnoreCase(type) ? "markdown" : "code";
		this.language = "c".equalsIgnoreCase(language) ? "c" : "python";

		setLayout(new BorderLayout());
		setOpaque(false);
		setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
		setBorder(BorderFactory.createEmptyBorder(CARD_MARGIN.top, CARD_MARGIN.left, CARD_MARGIN.bottom, CARD_MARGIN.right));

		createHeader();
		createContent(initialSource);

		add(headerPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
			}
		});

		installMouseWheelForwarder(this);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		java.awt.Container parent = getParent();
		if (parent != null && parent.getWidth() > 0) {
			java.awt.Insets insets = parent.getInsets();
			int available = parent.getWidth() - (insets != null ? insets.left + insets.right : 0);
			if (available > 0) {
				d.width = available;
			}
		}
		return d;
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(50, getPreferredSize().height);
	}

	public String getType() {
		return type;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String lang) {
		this.language = "c".equalsIgnoreCase(lang) ? "c" : "python";
		if (typeSelector != null) {
			if ("markdown".equals(type)) {
				typeSelector.setSelectedItem(OPT_TEXT);
			} else if ("c".equals(language)) {
				typeSelector.setSelectedItem(OPT_C);
			} else {
				typeSelector.setSelectedItem(OPT_PYTHON);
			}
		}
		if (codeArea != null && "code".equals(type)) {
			codeArea.setSyntaxEditingStyle("c".equals(language) ? SyntaxConstants.SYNTAX_STYLE_C : SyntaxConstants.SYNTAX_STYLE_PYTHON);
		}
	}

	public String getSource() {
		if ("markdown".equals(type)) {
			return mdArea.getText();
		} else {
			return codeArea.getText();
		}
	}

	public void setSource(String source) {
		if (source == null) source = "";
		if ("markdown".equals(type)) {
			mdArea.setText(source);
			updateHtmlView();
		} else {
			codeArea.setText(source);
			updateCodeAreaRows();
		}
	}

	public RSyntaxTextArea getActiveTextArea() {
		if ("markdown".equals(type)) {
			if (!isEditingMd) {
				switchToMdEdit();
			}
			return mdArea;
		} else {
			return codeArea;
		}
	}

	public void setEditable(boolean value) {
		this.editable = value;
		if (btnLock != null) btnLock.setEnabled(value);
		if (typeSelector != null) typeSelector.setEnabled(value && !locked);
		if (btnRunCell != null) btnRunCell.setEnabled(value);
		if (btnUp != null) btnUp.setEnabled(value);
		if (btnDown != null) btnDown.setEnabled(value);
		if (btnAddCode != null) btnAddCode.setEnabled(value);
		if (btnAddMd != null) btnAddMd.setEnabled(value);
		if (btnDelete != null) {
			btnDelete.setEnabled(value && !locked);
			btnDelete.setToolTipText(locked ? "Cellule verrouillée (déverrouiller pour supprimer)" : "Supprimer cette cellule");
		}

		if (codeArea != null) {
			codeArea.setEditable(value && !locked);
			codeArea.setEnabled(value);
		}
		if (mdArea != null) {
			mdArea.setEditable(value && !locked);
			mdArea.setEnabled(value);
		}
		if (btnValidateMd != null) btnValidateMd.setEnabled(value && !locked);
		updateActiveState();
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
		if (btnLock != null) {
			btnLock.setLocked(locked);
		}
		if (codeArea != null) {
			codeArea.setEditable(this.editable && !locked);
		}
		if (mdArea != null) {
			mdArea.setEditable(this.editable && !locked);
		}
		if (typeSelector != null) {
			typeSelector.setEnabled(this.editable && !locked);
		}
		if (btnDelete != null) {
			btnDelete.setEnabled(this.editable && !locked);
			btnDelete.setToolTipText(locked ? "Cellule verrouillée (déverrouiller pour supprimer)" : "Supprimer cette cellule");
		}
		if (btnValidateMd != null) {
			btnValidateMd.setEnabled(this.editable && !locked);
		}
		if (locked && isEditingMd) {
			switchToMdView();
		}
		if (mdHtmlView != null) {
			mdHtmlView.setToolTipText(locked ? "Cellule verrouillée contre toute modification" : "Double-cliquez pour modifier ce texte");
		}
		updateActiveState();
	}

	public void focusEditor() {
		if (editor != null) editor.setActiveCell(this);
		updateActiveState();
		if ("markdown".equals(type)) {
			if (editable && !locked) {
				switchToMdEdit();
				mdArea.requestFocusInWindow();
			} else {
				mdHtmlView.requestFocusInWindow();
			}
		} else {
			codeArea.requestFocusInWindow();
		}
	}

	private void createHeader() {
		headerPanel = new JPanel(new BorderLayout()) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(50, 44);
			}

			@Override
			public void doLayout() {
				int width = getWidth();
				int height = getHeight();
				if (width <= 0 || height <= 0) return;

				// Adapter le libellé des boutons selon la largeur disponible
				if (width < 440) {
					if (!"+ MD".equals(btnAddMd.getText())) btnAddMd.setText("+ MD");
					btnAddMd.setPreferredSize(new Dimension(54, 26));
					btnAddMd.setMinimumSize(new Dimension(54, 26));
				} else {
					if (!"+ Markdown".equals(btnAddMd.getText())) btnAddMd.setText("+ Markdown");
					btnAddMd.setPreferredSize(new Dimension(84, 26));
					btnAddMd.setMinimumSize(new Dimension(84, 26));
				}

				if (btnValidateMd != null && btnValidateMd.isVisible()) {
					if (width < 450) {
						if (!"Valider".equals(btnValidateMd.getText())) btnValidateMd.setText("Valider");
						btnValidateMd.setPreferredSize(new Dimension(68, 26));
						btnValidateMd.setMinimumSize(new Dimension(68, 26));
					} else if (width < 560) {
						if (!"Valider [⇧↵]".equals(btnValidateMd.getText())) btnValidateMd.setText("Valider [⇧↵]");
						btnValidateMd.setPreferredSize(new Dimension(105, 26));
						btnValidateMd.setMinimumSize(new Dimension(105, 26));
					} else {
						if (!"Valider l'affichage [shift]+[entrée]".equals(btnValidateMd.getText())) {
							btnValidateMd.setText("Valider l'affichage [shift]+[entrée]");
						}
						btnValidateMd.setPreferredSize(new Dimension(210, 26));
						btnValidateMd.setMinimumSize(new Dimension(210, 26));
					}
				}

				int rightW = rightBox.getPreferredSize().width;
				int rightX = width - rightW;

				int leftMaxW = Math.max(80, rightX - 6);
				int baseTypeW = 135;
				int runW = (btnRunCell != null && btnRunCell.isVisible()) ? 38 : 0;
				int actualTypeW = Math.min(baseTypeW, Math.max(75, leftMaxW - runW - 9));
				typeSelector.setPreferredSize(new Dimension(actualTypeW, 27));

				int leftW = leftBox.getPreferredSize().width;
				leftBox.setBounds(0, 0, leftW, height);
				leftBox.doLayout();

				int actualRightX = Math.max(leftW, rightX);
				rightBox.setBounds(actualRightX, 0, rightW, height);
				rightBox.doLayout();

				if (centerBox != null && centerBox.isVisible()) {
					if (actualRightX > leftW) {
						centerBox.setBounds(leftW, 0, actualRightX - leftW, height);
						centerBox.doLayout();
					} else {
						centerBox.setBounds(0, 0, 0, 0);
					}
				}
			}
		};
		headerPanel.setOpaque(false);
		headerPanel.setPreferredSize(new Dimension(100, 44));
		headerPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
			}
		});

		// Côté gauche : sélecteur de type + bouton RUN de cellule
		leftBox = new JPanel(new GridBagLayout());
		leftBox.setOpaque(false);
		leftBox.setBorder(BorderFactory.createEmptyBorder(3, 9, 0, 0));

		GridBagConstraints gbcL = new GridBagConstraints();
		gbcL.gridy = 0;
		gbcL.anchor = GridBagConstraints.CENTER;
		gbcL.insets = new Insets(0, 0, 0, 4);

		typeSelector = new JComboBox<>(new String[] { OPT_PYTHON, OPT_C, OPT_TEXT });
		if ("markdown".equals(type)) {
			typeSelector.setSelectedItem(OPT_TEXT);
		} else if ("c".equalsIgnoreCase(language)) {
			typeSelector.setSelectedItem(OPT_C);
		} else {
			typeSelector.setSelectedItem(OPT_PYTHON);
		}
		typeSelector.setFont(new Font("SansSerif", Font.PLAIN, 11));
		typeSelector.setFocusable(false);
		typeSelector.putClientProperty("JComponent.sizeVariant", "small");
		typeSelector.setPreferredSize(new Dimension(135, 27));
		typeSelector.setMinimumSize(new Dimension(75, 27));
		typeSelector.setMaximumSize(new Dimension(135, 27));
		typeSelector.addActionListener(e -> {
			String selected = (String) typeSelector.getSelectedItem();
			if (OPT_TEXT.equals(selected)) {
				changeType("markdown", language);
			} else if (OPT_C.equals(selected)) {
				changeType("code", "c");
			} else {
				changeType("code", "python");
			}
		});
		leftBox.add(typeSelector, gbcL);

		btnRunCell = new CellRunButton();
		btnRunCell.setVisible("code".equals(type));
		btnRunCell.addActionListener(e -> editor.runSingleCell(this));
		leftBox.add(btnRunCell, gbcL);

		// Côté droit : boutons d'action
		rightBox = new JPanel(new GridBagLayout());
		rightBox.setOpaque(false);
		rightBox.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 6));

		GridBagConstraints gbcR = new GridBagConstraints();
		gbcR.gridy = 0;
		gbcR.anchor = GridBagConstraints.CENTER;
		gbcR.insets = new Insets(0, 0, 0, 4);

		btnUp = new CellMoveButton(CellMoveButton.Direction.UP);
		btnUp.addActionListener(e -> editor.moveCellUp(this));
		rightBox.add(btnUp, gbcR);

		btnDown = new CellMoveButton(CellMoveButton.Direction.DOWN);
		btnDown.addActionListener(e -> editor.moveCellDown(this));
		rightBox.add(btnDown, gbcR);

		btnAddCode = new CellHeaderButton("+ Code", "Insérer une cellule de code après", 64, false, false);
		btnAddCode.addActionListener(e -> editor.addCellAfter(this, "code", this.language));
		rightBox.add(btnAddCode, gbcR);

		btnAddMd = new CellHeaderButton("+ Markdown", "Insérer une cellule Markdown après", 84, false, false);
		btnAddMd.addActionListener(e -> editor.addCellAfter(this, "markdown"));
		rightBox.add(btnAddMd, gbcR);

		btnDelete = new CellHeaderButton("✕", "Supprimer cette cellule", 28, true, true);
		btnDelete.addActionListener(e -> editor.removeCell(this));
		rightBox.add(btnDelete, gbcR);

		gbcR.insets = new Insets(0, 0, 0, 0);
		btnLock = new CellLockButton();
		btnLock.addActionListener(e -> {
			if (editor != null) {
				editor.setActiveCell(NotebookCellPane.this);
				editor.setModified(true);
			}
			setLocked(!isLocked());
		});
		rightBox.add(btnLock, gbcR);

		// Au milieu : bouton de validation de l'affichage Markdown
		centerBox = new JPanel(new GridBagLayout());
		centerBox.setOpaque(false);
		centerBox.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		centerBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
			}
		});

		GridBagConstraints gbcC = new GridBagConstraints();
		gbcC.gridy = 0;
		gbcC.anchor = GridBagConstraints.CENTER;

		btnValidateMd = new CellHeaderButton("Valider l'affichage [shift]+[entrée]", "Afficher le rendu Markdown (Shift+Entrée)", 210, false, false);
		btnValidateMd.addActionListener(e -> switchToMdView());
		btnValidateMd.setVisible("markdown".equals(type) && isEditingMd);
		centerBox.add(btnValidateMd, gbcC);

		headerPanel.add(leftBox, BorderLayout.WEST);
		headerPanel.add(centerBox, BorderLayout.CENTER);
		headerPanel.add(rightBox, BorderLayout.EAST);
	}

	private void createContent(String source) {
		centerPanel = new JPanel(new BorderLayout());
		centerPanel.setOpaque(false);

		DocumentListener docListener = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { 
				if (editor != null) editor.setModified(true); 
				updateCodeAreaRows();
				NotebookCellPane.this.repaint();
			}
			public void removeUpdate(DocumentEvent e) { 
				if (editor != null) editor.setModified(true); 
				updateCodeAreaRows();
				NotebookCellPane.this.repaint();
			}
			public void changedUpdate(DocumentEvent e) { 
				if (editor != null) editor.setModified(true); 
				updateCodeAreaRows();
				NotebookCellPane.this.repaint();
			}
		};

		// 1. Code cell setup
		codeArea = new RSyntaxTextArea();
		codeArea.setSyntaxEditingStyle("c".equalsIgnoreCase(language) ? SyntaxConstants.SYNTAX_STYLE_C : SyntaxConstants.SYNTAX_STYLE_PYTHON);
		codeArea.setAntiAliasingEnabled(true);
		codeArea.setCodeFoldingEnabled(true);
		codeArea.setAutoIndentEnabled(true);
		codeArea.setCloseCurlyBraces(true);
		codeArea.setHighlightCurrentLine(false);
		codeArea.setFadeCurrentLineHighlight(true);
		codeArea.setFont(new Font(CodeLab.FONT_NAME != null ? CodeLab.FONT_NAME : "Monospaced", Font.PLAIN, CodeLab.FONT_SIZE > 0 ? CodeLab.FONT_SIZE : 13));
		codeArea.getDocument().addDocumentListener(docListener);

		InputMap codeIm = codeArea.getInputMap();
		ActionMap codeAm = codeArea.getActionMap();
		codeIm.put(KeyStroke.getKeyStroke("meta Z"), "UNDO");
		codeIm.put(KeyStroke.getKeyStroke("shift meta Z"), "REDO");
		codeAm.put("UNDO", new RSyntaxTextAreaEditorKit.UndoAction());
		codeAm.put("REDO", new RSyntaxTextAreaEditorKit.RedoAction());

		codeArea.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
				updateActiveState();
			}
			public void focusLost(FocusEvent e) {
				updateActiveState();
			}
		});
		codeArea.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					editor.runSingleCell(NotebookCellPane.this);
				}
			}
		});

		codeScroll = new RTextScrollPane(codeArea);
		codeScroll.setOpaque(false);
		codeScroll.getViewport().setOpaque(false);
		codeScroll.getGutter().setOpaque(false);
		codeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		codeScroll.setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 6));
		codeScroll.setIconRowHeaderEnabled(false);
		codeScroll.getGutter().setBookmarkingEnabled(false);

		// 2. Markdown cell setup
		mdCardLayout = new CardLayout();
		mdCardPanel = new JPanel(mdCardLayout);
		mdCardPanel.setOpaque(false);

		// Vue HTML
		mdHtmlView = new JEditorPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getMinimumSize() {
				return new Dimension(0, 0);
			}

			@Override
			public boolean getScrollableTracksViewportWidth() {
				return true;
			}

			@Override
			public Dimension getPreferredSize() {
				int w = getWidth();
				if (w <= 0 && getParent() != null) {
					w = getParent().getWidth();
				}
				if (w <= 0) {
					Container cell = NotebookCellPane.this;
					if (cell.getWidth() > 0) {
						w = cell.getWidth() - CARD_MARGIN.left - CARD_MARGIN.right - 32;
					} else {
						Container parent = cell.getParent();
						if (parent != null && parent.getWidth() > 0) {
							Insets pInsets = parent.getInsets();
							int pW = parent.getWidth() - (pInsets != null ? pInsets.left + pInsets.right : 0);
							w = pW - CARD_MARGIN.left - CARD_MARGIN.right - 32;
						} else if (editor != null && editor.getScrollPane() != null) {
							int vpW = editor.getScrollPane().getViewport().getWidth();
							if (vpW > 0) {
								int sidePad = (editor.getCellsPanel() != null && editor.getCellsPanel().getInsets() != null)
									? (editor.getCellsPanel().getInsets().left + editor.getCellsPanel().getInsets().right)
									: (NotebookEditor.MARGIN_SIDE * 2);
								w = vpW - sidePad - CARD_MARGIN.left - CARD_MARGIN.right - 32;
							}
						}
					}
				}
				if (w > 0) {
					setSize(w, Short.MAX_VALUE);
					Dimension d = super.getPreferredSize();
					d.width = w;
					return d;
				}
				return super.getPreferredSize();
			}
		};
		mdHtmlView.setEditorKit(new NotebookHtmlEditorKit());
		mdHtmlView.setEditable(false);
		mdHtmlView.setOpaque(false);
		mdHtmlView.setBorder(BorderFactory.createEmptyBorder(6, 18, 10, 14));
		mdHtmlView.setToolTipText("Double-cliquez pour modifier ce texte");
		mdHtmlView.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
				if (e.getClickCount() == 2 && editable && !locked) {
					switchToMdEdit();
				}
			}
		});

		// Édition Markdown
		mdArea = new RSyntaxTextArea();
		mdArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
		mdArea.setLineWrap(true);
		mdArea.setWrapStyleWord(true);
		mdArea.setAntiAliasingEnabled(true);
		mdArea.setAutoIndentEnabled(true);
		mdArea.setHighlightCurrentLine(false);
		mdArea.setFadeCurrentLineHighlight(true);
		mdArea.setFont(new Font(CodeLab.FONT_NAME != null ? CodeLab.FONT_NAME : "Monospaced", Font.PLAIN, CodeLab.FONT_SIZE > 0 ? CodeLab.FONT_SIZE : 13));
		mdArea.getDocument().addDocumentListener(docListener);

		InputMap mdIm = mdArea.getInputMap();
		ActionMap mdAm = mdArea.getActionMap();
		mdIm.put(KeyStroke.getKeyStroke("meta Z"), "UNDO");
		mdIm.put(KeyStroke.getKeyStroke("shift meta Z"), "REDO");
		mdAm.put("UNDO", new RSyntaxTextAreaEditorKit.UndoAction());
		mdAm.put("REDO", new RSyntaxTextAreaEditorKit.RedoAction());

		mdArea.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				if (editor != null) editor.setActiveCell(NotebookCellPane.this);
				updateActiveState();
			}
			public void focusLost(FocusEvent e) {
				updateActiveState();
			}
		});
		mdArea.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					switchToMdView();
				}
			}
		});

		mdScroll = new RTextScrollPane(mdArea);
		mdScroll.setOpaque(false);
		mdScroll.getViewport().setOpaque(false);
		mdScroll.getGutter().setOpaque(false);
		mdScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		mdScroll.setBorder(BorderFactory.createEmptyBorder(2, 6, 8, 6));
		mdScroll.setIconRowHeaderEnabled(false);
		mdScroll.getGutter().setBookmarkingEnabled(false);

		JPanel editContainer = new JPanel(new BorderLayout());
		editContainer.setOpaque(false);
		editContainer.add(mdScroll, BorderLayout.CENTER);

		mdCardPanel.add(mdHtmlView, "VIEW");
		mdCardPanel.add(editContainer, "EDIT");

		// Initialisation du contenu
		if ("markdown".equals(type)) {
			mdArea.setText(source != null ? source : "");
			updateHtmlView();
			centerPanel.add(mdCardPanel, BorderLayout.CENTER);
			mdCardLayout.show(mdCardPanel, "VIEW");
		} else {
			codeArea.setText(source != null ? source : "");
			centerPanel.add(codeScroll, BorderLayout.CENTER);
		}
	}

	private void changeType(String newType, String newLang) {
		String source = getSource();
		this.type = newType;
		if (newLang != null) {
			this.language = "c".equalsIgnoreCase(newLang) ? "c" : "python";
		}
		if (typeSelector != null) {
			if ("markdown".equals(type)) {
				typeSelector.setSelectedItem(OPT_TEXT);
			} else if ("c".equalsIgnoreCase(language)) {
				typeSelector.setSelectedItem(OPT_C);
			} else {
				typeSelector.setSelectedItem(OPT_PYTHON);
			}
		}
		btnRunCell.setVisible("code".equals(type));

		centerPanel.removeAll();
		if ("markdown".equals(type)) {
			mdArea.setText(source);
			updateHtmlView();
			centerPanel.add(mdCardPanel, BorderLayout.CENTER);
			switchToMdView();
		} else {
			if (btnValidateMd != null) {
				btnValidateMd.setVisible(false);
			}
			codeArea.setSyntaxEditingStyle("c".equalsIgnoreCase(language) ? SyntaxConstants.SYNTAX_STYLE_C : SyntaxConstants.SYNTAX_STYLE_PYTHON);
			codeArea.setText(source);
			centerPanel.add(codeScroll, BorderLayout.CENTER);
		}
		centerPanel.revalidate();
		centerPanel.repaint();
		installMouseWheelForwarder(centerPanel);
		if (editor != null) {
			editor.setActiveCell(this);
			editor.setModified(true);
			if (editor.getToolbar() != null) editor.getToolbar().update();
		}
	}

	public void switchToMdEdit() {
		if (!"markdown".equals(type)) return;
		if (!editable || locked) return;
		isEditingMd = true;
		if (editor != null) editor.setActiveCell(this);
		updateActiveState();
		if (btnValidateMd != null) {
			btnValidateMd.setVisible(true);
			if (btnValidateMd.getParent() != null) {
				btnValidateMd.getParent().revalidate();
				btnValidateMd.getParent().repaint();
			}
		}
		updateCodeAreaRows();
		mdCardLayout.show(mdCardPanel, "EDIT");
		mdArea.requestFocusInWindow();
	}

	public void switchToMdView() {
		if (!"markdown".equals(type)) return;
		isEditingMd = false;
		if (btnValidateMd != null) {
			btnValidateMd.setVisible(false);
			if (btnValidateMd.getParent() != null) {
				btnValidateMd.getParent().revalidate();
				btnValidateMd.getParent().repaint();
			}
		}
		updateHtmlView();
		mdCardLayout.show(mdCardPanel, "VIEW");
		updateActiveState();
		if (editor != null) {
			editor.revalidateContainer();
		}
	}

	private void updateHtmlView() {
		String raw = mdArea.getText();
		if (raw.trim().isEmpty()) {
			mdHtmlView.setText("<html><body style='color:#8c959f; font-style:italic; font-family:sans-serif;'>" +
				"<i>(Cellule Markdown vide - double-cliquez pour rédiger)</i></body></html>");
			return;
		}

		try {
			int baseFontSize = CodeLab.FONT_SIZE > 0 ? CodeLab.FONT_SIZE : 13;
			int fontSize = Math.max(10, baseFontSize - 2);
			String css = getMarkdownCss(editor, fontSize);
			float globalScale = parseCssFloat(css, "--math-scale", 1.0f);
			float inlineScale = parseCssFloat(css, "--math-inline-scale", 2.20f) * globalScale;
			float displayScale = parseCssFloat(css, "--math-display-scale", 3.40f) * globalScale;
			float mermaidScale = parseCssFloat(css, "--mermaid-scale", 3.40f);

			// Diagrammes Mermaid en premier (avant LaTeX, pour que d'éventuels "$" du diagramme ne soient pas interprétés)
			int viewW = mdHtmlView.getWidth() > 0 ? mdHtmlView.getWidth() : 700;
			int maxDiagramW = Math.max(200, viewW - 40);
			String withDiagrams = MermaidRenderer.renderMarkdownMermaid(MarkdownUtils.underline(raw), maxDiagramW, mermaidScale, this, this::refreshHtmlView);

			String processed = LatexRenderer.renderMarkdownMath(withDiagrams, fontSize, inlineScale, displayScale);
			Node document = MD_PARSER.parse(processed);
			String body = CodeBlockUtils.tagLanguage(HTML_RENDERER.render(document));

			// Supprimer les commentaires CSS pour éviter que d'éventuels chevrons (<...>) dans les commentaires ne perturbent le parseur HTML de Swing
			String cleanCss = css != null ? css.replaceAll("/\\*[\\s\\S]*?\\*/", "") : "";
			cleanCss += "\npre, table, img { max-width: 100%; }\n";
			String fullHtml = "<html><head><style>\n" + cleanCss + "\n</style></head><body>" + body + "</body></html>";
			mdHtmlView.setText(fullHtml);
			mdHtmlView.setCaretPosition(0);

			if (editor != null && mdHtmlView.getDocument() instanceof HTMLDocument) {
				File nbFile = editor.getFile() != null ? editor.getFile() : editor.extern_file;
				if (nbFile != null && nbFile.getParentFile() != null) {
					try {
						((HTMLDocument) mdHtmlView.getDocument()).setBase(nbFile.getParentFile().toURI().toURL());
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			mdHtmlView.setText("<html><body><pre>" + raw + "</pre></body></html>");
		}
	}

	private static float parseCssFloat(String css, String property, float defaultValue) {
		if (css == null) return defaultValue;
		// Supprimer les commentaires CSS /* ... */ pour ne pas matcher d'éventuels exemples
		String cleanCss = css.replaceAll("/\\*[\\s\\S]*?\\*/", "");
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(property) + "\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
		java.util.regex.Matcher m = p.matcher(cleanCss);
		if (m.find()) {
			try {
				return Float.parseFloat(m.group(1));
			} catch (Exception ignored) {
			}
		}
		return defaultValue;
	}

	public static String getMarkdownCss(NotebookEditor editor, int fontSize) {
		// S'assurer que les polices embarquées (Inter) sont bien enregistrées dans Swing
		codelab.utils.ResourceUtils.initEmbeddedFonts();

		// Fichier de style configuré pour les carnets Jupyter (IPYNB_STYLE)
		File cssFile = CodeLab.getIpynbCssFile();

		// Lecture avec cache basé sur la date de dernière modification
		String css = null;
		if (cssFile != null && cssFile.exists()) {
			String path = cssFile.getAbsolutePath();
			long mod = cssFile.lastModified();
			CssCacheEntry entry = CSS_CACHE.get(path);
			if (entry != null && entry.lastModified == mod) {
				css = entry.rawCss;
			} else {
				try {
					css = Files.readString(cssFile.toPath(), StandardCharsets.UTF_8);
					CSS_CACHE.put(path, new CssCacheEntry(mod, css));
				} catch (Exception ignored) {
				}
			}
		}

		// Repli sur la ressource du jar si nécessaire
		if (css == null) {
			if (DEFAULT_CSS_RESOURCE == null) {
				try (InputStream is = NotebookCellPane.class.getResourceAsStream("/data/markdown.css")) {
					if (is != null) {
						DEFAULT_CSS_RESOURCE = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					}
				} catch (Exception ignored) {
				}
			}
			css = DEFAULT_CSS_RESOURCE;
		}

		// Repli de secours minimal
		if (css == null) {
			css = "body { font-family: sans-serif; font-size: ${FONT_SIZE}px; color: #24292f; margin: 4px; }\n" +
				  "h1 { font-size: 1.4em; color: #0969da; border-bottom: 1px solid #d0d7de; }\n" +
				  "h2 { font-size: 1.25em; color: #1f2328; border-bottom: 1px solid #d0d7de; }\n" +
				  "code, pre { font-family: monospace; background-color: #f6f8fa; }\n";
		}

		if (css.contains("${FONT_SIZE}")) {
			css = css.replace("${FONT_SIZE}", String.valueOf(fontSize));
		}

		return css;
	}

	public static void clearCssCache() {
		CSS_CACHE.clear();
	}

	public void refreshHtmlView() {
		if ("markdown".equals(type) && !isEditingMd) {
			updateHtmlView();
		}
	}

	public void updateFont(String name, int size) {
		Font font = new Font(name, Font.PLAIN, size);
		if (codeArea != null) {
			codeArea.setFont(font);
			updateCodeAreaRows();
		}
		if (mdArea != null) {
			mdArea.setFont(font);
			updateCodeAreaRows();
		}
		if ("markdown".equals(type) && !isEditingMd) {
			updateHtmlView();
		}
	}

	private void updateCodeAreaRows() {
		if (codeArea != null) {
			int lines = Math.max(3, codeArea.getLineCount());
			codeArea.setRows(lines);
		}
		if (mdArea != null && isEditingMd) {
			int lines = Math.max(3, mdArea.getLineCount());
			mdArea.setRows(lines);
		}
		if (editor != null) {
			editor.revalidateContainer();
		}
	}

	public boolean isHighlighted() {
		return (editor != null && editor.isCellActive(this));
	}

	public void updateActiveState() {
		boolean active = isHighlighted();
		boolean highlight = active && editable && !locked;
		if (codeArea != null) {
			codeArea.setHighlightCurrentLine(highlight);
		}
		if (mdArea != null) {
			mdArea.setHighlightCurrentLine(highlight);
		}
		repaint();
	}

	public int getLineCount() {
		if ("code".equals(type) && codeArea != null) {
			return codeArea.getLineCount();
		} else if ("markdown".equals(type) && mdArea != null) {
			return mdArea.getLineCount();
		}
		return 0;
	}

	public void addLineHighlight(int line, Color color) {
		Runnable task = () -> {
			try {
				if ("code".equals(type) && codeArea != null) {
					if (line >= 0 && line < codeArea.getLineCount()) {
						codeArea.addLineHighlight(line, color);
					}
				} else if ("markdown".equals(type) && mdArea != null) {
					if (line >= 0 && line < mdArea.getLineCount()) {
						mdArea.addLineHighlight(line, color);
					}
				}
			} catch (BadLocationException ignored) {}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}

	public void removeAllLineHighlights() {
		Runnable task = () -> {
			if (codeArea != null) {
				codeArea.removeAllLineHighlights();
			}
			if (mdArea != null) {
				mdArea.removeAllLineHighlights();
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = CARD_MARGIN.left;
		int y = CARD_MARGIN.top;
		int w = getWidth() - CARD_MARGIN.left - CARD_MARGIN.right;
		int h = getHeight() - CARD_MARGIN.top - CARD_MARGIN.bottom;

		if (w > 0 && h > 0) {
			// Ombre portée subtile
			if (isHighlighted()) {
				g2.setColor(new Color(66, 133, 244, 30));
				g2.fill(new RoundRectangle2D.Float(x - 1, y, w + 2, h + 2, CARD_ARC + 2, CARD_ARC + 2));
			} else {
				g2.setColor(new Color(0, 0, 0, 10));
				g2.fill(new RoundRectangle2D.Float(x, y + 1.5f, w, h, CARD_ARC, CARD_ARC));
			}

			// Fond de la carte
			Shape cardShape = new RoundRectangle2D.Float(x, y, w, h, CARD_ARC, CARD_ARC);
			g2.setColor(Color.WHITE);
			g2.fill(cardShape);

			// Fond de l'en-tête (arrondi en haut)
			int headerH = (headerPanel != null && headerPanel.isVisible()) ? headerPanel.getHeight() : 44;
			Shape oldClip = g2.getClip();
			g2.clip(cardShape);
			Color headerBg = isHighlighted() ? BG_HEADER_SELECTED : BG_HEADER;
			Color dividerColor = isHighlighted() ? BORDER_HEADER_SELECTED : BORDER_NORMAL;
			g2.setColor(headerBg);
			g2.fillRect(x, y, w, headerH);
			// Séparateur sous l'en-tête
			g2.setColor(dividerColor);
			g2.drawLine(x, y + headerH - 1, x + w, y + headerH - 1);
			g2.setClip(oldClip);
		}
		g2.dispose();
		super.paintComponent(g);
	}

	@Override
	protected void paintChildren(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = CARD_MARGIN.left;
		int y = CARD_MARGIN.top;
		int w = getWidth() - CARD_MARGIN.left - CARD_MARGIN.right;
		int h = getHeight() - CARD_MARGIN.top - CARD_MARGIN.bottom;

		if (w > 0 && h > 0) {
			Shape round = new RoundRectangle2D.Float(x, y, w, h, CARD_ARC, CARD_ARC);
			g2.clip(round);
		}
		super.paintChildren(g2);
		g2.dispose();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = CARD_MARGIN.left;
		int y = CARD_MARGIN.top;
		int w = getWidth() - CARD_MARGIN.left - CARD_MARGIN.right;
		int h = getHeight() - CARD_MARGIN.top - CARD_MARGIN.bottom;

		if (w > 0 && h > 0) {
			if (isHighlighted()) {
				g2.setColor(BORDER_FOCUSED);
				g2.setStroke(new BasicStroke(2.0f));
				g2.draw(new RoundRectangle2D.Float(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, CARD_ARC, CARD_ARC));
			} else {
				g2.setColor(BORDER_NORMAL);
				g2.setStroke(new BasicStroke(1.0f));
				g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1.0f, h - 1.0f, CARD_ARC, CARD_ARC));
			}
		}
		g2.dispose();
	}

	/**
	 * Bouton moderne aux coins arrondis pour les actions secondaires de l'en-tête (+ Code, + Texte, Supprimer).
	 */
	private static class CellHeaderButton extends JButton {
		private static final long serialVersionUID = 1L;

		private final boolean isDanger;
		private final boolean isCross;
		private boolean isHover = false;
		private boolean isPressed = false;

		public CellHeaderButton(String text, String tooltip, int width, boolean isDanger, boolean isCross) {
			super(text);
			this.isDanger = isDanger;
			this.isCross = isCross;
			Dimension d = new Dimension(width, 26);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
			setContentAreaFilled(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setOpaque(false);
			setFont(new Font("SansSerif", Font.PLAIN, 11));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(tooltip);

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if (isEnabled()) {
						isHover = true;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					isHover = false;
					isPressed = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (isEnabled()) {
						isPressed = true;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isPressed = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			int w = getWidth();
			int h = getHeight();

			Color bg;
			Color border;
			Color fg;

			if (!isEnabled()) {
				bg = new Color(245, 246, 248);
				border = new Color(225, 228, 232);
				fg = new Color(185, 190, 195);
			} else if (isDanger) {
				if (isPressed) {
					bg = new Color(250, 215, 218);
					border = new Color(230, 140, 150);
					fg = new Color(180, 20, 30);
				} else if (isHover) {
					bg = new Color(254, 238, 240);
					border = new Color(240, 175, 185);
					fg = new Color(210, 35, 45);
				} else {
					bg = Color.WHITE;
					border = new Color(225, 210, 215);
					fg = new Color(215, 50, 65);
				}
			} else {
				if (isPressed) {
					bg = new Color(220, 226, 234);
					border = new Color(180, 188, 198);
					fg = new Color(30, 40, 50);
				} else if (isHover) {
					bg = new Color(235, 240, 246);
					border = new Color(195, 202, 212);
					fg = new Color(30, 40, 50);
				} else {
					bg = Color.WHITE;
					border = new Color(210, 216, 224);
					fg = new Color(75, 85, 100);
				}
			}

			int arc = 8;
			g2.setColor(bg);
			g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

			g2.setColor(fg);

			if (isCross) {
				// Croix vectorielle nette et centrée
				float strokeW = 1.4f;
				g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				int sz = 7;
				float cx = (w - sz) / 2.0f;
				float cy = (h - sz) / 2.0f;
				Path2D.Float p = new Path2D.Float();
				p.moveTo(cx, cy);
				p.lineTo(cx + sz, cy + sz);
				p.moveTo(cx + sz, cy);
				p.lineTo(cx, cy + sz);
				g2.draw(p);
			} else {
				// Texte centré (+ Code, + Texte)
				FontMetrics fm = g2.getFontMetrics(getFont());
				int tw = fm.stringWidth(getText());
				int th = fm.getAscent();
				int tx = (w - tw) / 2;
				int ty = (h + th) / 2 - 2;
				g2.drawString(getText(), tx, ty);
			}

			g2.dispose();
		}
	}

	/**
	 * Bouton d'exécution d'une cellule de code (coins arrondis arc=8 comme les autres, vert sauge doux désaturé).
	 */
	private static class CellRunButton extends JButton {
		private static final long serialVersionUID = 1L;

		private boolean isHover = false;
		private boolean isPressed = false;

		public CellRunButton() {
			super();
			Dimension d = new Dimension(28, 26);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
			setContentAreaFilled(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Exécuter cette cellule (Shift+Entrée)");

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if (isEnabled()) {
						isHover = true;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					isHover = false;
					isPressed = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (isEnabled()) {
						isPressed = true;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isPressed = false;
					repaint();
				}
			});
		}

		private static Shape createRoundedTriangle(float x, float y, float width, float height, float radius) {
			Path2D.Float p = new Path2D.Float();
			float r = Math.min(radius, width / 3.0f);
			float x1 = x, y1 = y;
			float x2 = x + width, y2 = y + height / 2.0f;
			float x3 = x, y3 = y + height;

			p.moveTo(x1 + r, y1 + r * 0.5f);
			p.lineTo(x2 - r * 1.5f, y2 - r * 0.7f);
			p.quadTo(x2, y2, x2 - r * 1.5f, y2 + r * 0.7f);
			p.lineTo(x3 + r, y3 - r * 0.5f);
			p.quadTo(x3, y3, x3, y3 - r);
			p.lineTo(x1, y1 + r);
			p.quadTo(x1, y1, x1 + r, y1 + r * 0.5f);
			p.closePath();
			return p;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			int w = getWidth();
			int h = getHeight();
			int arc = 8; // Même rayon de courbure que les autres boutons

			if (!isEnabled()) {
				// État désactivé (carnet verrouillé ou programme en cours)
				g2.setColor(new Color(245, 246, 248));
				g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

				g2.setColor(new Color(225, 228, 232));
				g2.setStroke(new BasicStroke(1.0f));
				g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

				g2.setColor(new Color(185, 190, 195));
				Shape tri = createRoundedTriangle((w - 9) / 2.0f + 1.0f, (h - 10) / 2.0f, 9, 10, 1.2f);
				g2.fill(tri);
				g2.dispose();
				return;
			}

			Color bg;
			Color border;

			if (isPressed) {
				bg = new Color(78, 140, 198);
				border = new Color(58, 120, 178);
			} else if (isHover) {
				bg = new Color(105, 168, 225);
				border = new Color(78, 142, 202);
			} else {
				bg = new Color(91, 155, 213);
				border = new Color(68, 132, 192);
			}

			// Fond aux coins arrondis identiques aux autres boutons
			g2.setColor(bg);
			g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

			// Filet de bordure extérieur identique
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

			// Triangle Play blanc aux sommets arrondis
			g2.setColor(Color.WHITE);
			Shape tri = createRoundedTriangle((w - 9) / 2.0f + 1.0f, (h - 10) / 2.0f, 9, 10, 1.2f);
			g2.fill(tri);

			g2.dispose();
		}
	}

	/**
	 * Bouton moderne pour déplacer une cellule vers le haut ou le bas (triangles non pleins / contour vectoriel).
	 */
	private static class CellMoveButton extends JButton {
		private static final long serialVersionUID = 1L;

		public enum Direction { UP, DOWN }

		private final Direction direction;
		private boolean isHover = false;
		private boolean isPressed = false;

		public CellMoveButton(Direction direction) {
			super();
			this.direction = direction;
			Dimension d = new Dimension(28, 26);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
			setContentAreaFilled(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(direction == Direction.UP ? "Déplacer la cellule vers le haut" : "Déplacer la cellule vers le bas");

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if (isEnabled()) {
						isHover = true;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					isHover = false;
					isPressed = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (isEnabled()) {
						isPressed = true;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isPressed = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			int w = getWidth();
			int h = getHeight();

			Color bg;
			Color border;
			Color iconColor;

			if (!isEnabled()) {
				bg = new Color(245, 246, 248);
				border = new Color(225, 228, 232);
				iconColor = new Color(185, 190, 195);
			} else if (isPressed) {
				bg = new Color(220, 226, 234);
				border = new Color(180, 188, 198);
				iconColor = new Color(30, 40, 50);
			} else if (isHover) {
				bg = new Color(235, 240, 246);
				border = new Color(195, 202, 212);
				iconColor = new Color(30, 40, 50);
			} else {
				bg = Color.WHITE;
				border = new Color(210, 216, 224);
				iconColor = new Color(80, 90, 105);
			}

			// Fond légèrement arrondi
			int arc = 8;
			g2.setColor(bg);
			g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

			// Triangle creux (non plein) tracé au trait
			float strokeW = 1.4f;
			g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(iconColor);

			int triW = 9;
			int triH = 8;
			float cx = (w - triW) / 2.0f;
			float cy = (h - triH) / 2.0f;

			Path2D.Float p = new Path2D.Float();
			if (direction == Direction.UP) {
				p.moveTo(cx + triW / 2.0f, cy);
				p.lineTo(cx + triW, cy + triH);
				p.lineTo(cx, cy + triH);
				p.closePath();
			} else {
				p.moveTo(cx, cy);
				p.lineTo(cx + triW, cy);
				p.lineTo(cx + triW / 2.0f, cy + triH);
				p.closePath();
			}
			g2.draw(p);

			g2.dispose();
		}
	}

	/**
	 * Bouton moderne cadenas (ouvert / fermé) pour verrouiller la cellule contre toute modification.
	 */
	private static class CellLockButton extends JButton {
		private static final long serialVersionUID = 1L;

		private boolean locked = false;
		private boolean isHover = false;
		private boolean isPressed = false;

		public CellLockButton() {
			super();
			Dimension d = new Dimension(28, 26);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
			setContentAreaFilled(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			updateTooltip();

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					if (isEnabled()) {
						isHover = true;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					isHover = false;
					isPressed = false;
					repaint();
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (isEnabled()) {
						isPressed = true;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					isPressed = false;
					repaint();
				}
			});
		}

		public void setLocked(boolean locked) {
			this.locked = locked;
			updateTooltip();
			repaint();
		}
		
		private void updateTooltip() {
			setToolTipText(locked ? "Cellule verrouillée (cliquer pour déverrouiller)" 
			                      : "Verrouiller la cellule contre toute modification");
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			int w = getWidth();
			int h = getHeight();

			Color bg;
			Color border;
			Color iconColor;

			if (!isEnabled()) {
				bg = new Color(245, 246, 248);
				border = new Color(225, 228, 232);
				iconColor = new Color(185, 190, 195);
			} else if (locked) {
				if (isPressed) {
					bg = new Color(253, 230, 138);
					border = new Color(217, 119, 6);
					iconColor = new Color(180, 83, 9);
				} else if (isHover) {
					bg = new Color(254, 243, 199);
					border = new Color(245, 158, 11);
					iconColor = new Color(180, 83, 9);
				} else {
					bg = new Color(255, 251, 235);
					border = new Color(245, 158, 11);
					iconColor = new Color(217, 119, 6);
				}
			} else {
				if (isPressed) {
					bg = new Color(220, 226, 234);
					border = new Color(180, 188, 198);
					iconColor = new Color(30, 40, 50);
				} else if (isHover) {
					bg = new Color(235, 240, 246);
					border = new Color(195, 202, 212);
					iconColor = new Color(30, 40, 50);
				} else {
					bg = Color.WHITE;
					border = new Color(210, 216, 224);
					iconColor = new Color(110, 120, 135);
				}
			}

			// Fond légèrement arrondi
			int arc = 8;
			g2.setColor(bg);
			g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

			float cx = w / 2.0f;
			float bw = 10.0f;
			float bh = 7.5f;
			float bx = cx - bw / 2.0f;
			float by = 11.5f;

			// Anse (shackle)
			g2.setColor(iconColor);
			g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Path2D.Float shackle = new Path2D.Float();
			float sw = 6.0f;
			float lx = cx - sw / 2.0f;
			float rx = cx + sw / 2.0f;

			if (locked) {
				// Cadenas fermé
				shackle.moveTo(lx, by + 1.0f);
				shackle.lineTo(lx, by - 3.0f);
				shackle.quadTo(cx, by - 6.5f, rx, by - 3.0f);
				shackle.lineTo(rx, by + 1.0f);
				g2.draw(shackle);
			} else {
				// Cadenas ouvert (anse soulevée et ouverte à droite)
				shackle.moveTo(lx, by + 1.0f);
				shackle.lineTo(lx, by - 4.5f);
				shackle.quadTo(cx, by - 8.0f, rx, by - 4.5f);
				shackle.lineTo(rx, by - 2.0f);
				g2.draw(shackle);
			}

			// Corps du cadenas (body)
			g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 2.5f, 2.5f));

			// Trou de serrure (keyhole)
			g2.setColor(bg);
			float kw = 2.0f;
			g2.fill(new Ellipse2D.Float(cx - kw / 2.0f, by + 2.0f, kw, kw));
			g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawLine((int)cx, (int)(by + 3.0f), (int)cx, (int)(by + 5.2f));

			g2.dispose();
		}
	}

	private void installMouseWheelForwarder(Component comp) {
		if (comp == null) return;
		if (comp instanceof JScrollPane) {
			JScrollPane sp = (JScrollPane) comp;
			for (MouseWheelListener l : sp.getMouseWheelListeners()) {
				sp.removeMouseWheelListener(l);
			}
		}
		comp.addMouseWheelListener(e -> {
			if (e.isShiftDown()) {
				RTextScrollPane internalScroll = findParentRTextScrollPane(e.getComponent());
				if (internalScroll != null && internalScroll.getHorizontalScrollBar().isVisible()) {
					Adjustable hbar = internalScroll.getHorizontalScrollBar();
					int amount = e.getScrollAmount() * e.getWheelRotation() * 18;
					hbar.setValue(hbar.getValue() + amount);
					e.consume();
					return;
				}
			}
			if (editor != null) {
				editor.forwardMouseWheel(e);
			}
		});
		if (comp instanceof Container) {
			for (Component child : ((Container) comp).getComponents()) {
				installMouseWheelForwarder(child);
			}
		}
	}

	private RTextScrollPane findParentRTextScrollPane(Component c) {
		while (c != null) {
			if (c instanceof RTextScrollPane) return (RTextScrollPane) c;
			c = c.getParent();
		}
		return null;
	}
}

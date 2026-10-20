package codelab.modules.editeur.markdown;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Arrays;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.text.CodeLabTextArea;
import codelab.utils.CodeBlockUtils;
import codelab.utils.LatexRenderer;
import codelab.utils.MarkdownUtils;
import codelab.utils.MermaidRenderer;
import codelab.utils.Utils;

/**
 * Visualiseur & éditeur riche pour fichiers Markdown (.md) et comptes-rendus d'évaluation.
 * Sous-classe d'AbstractEditor offrant :
 *  1. Un rendu visuel HTML5 élégant basé sur Flexmark et la feuille de style CodeLab (Inter + tableaux nets).
 *  2. La prise en charge des formules mathématiques LaTeX ($inline$ et $$display$$)
 *     et des diagrammes Mermaid (blocs ```mermaid, rendus par MermaidRenderer).
 *  3. Un mode double (Aperçu formaté / Code source Markdown éditable).
 *
 * @author Jérôme Lehuen + Antigravity
 * @version 09/10/26
 */
public class MarkdownViewer extends AbstractEditor {

	private static final long serialVersionUID = 1L;

	private static final String VIEW_PREVIEW = "PREVIEW";
	private static final String VIEW_CODE = "CODE";

	private static final Parser MD_PARSER;
	private static final HtmlRenderer HTML_RENDERER;
	static {
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
		options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
		MD_PARSER = Parser.builder(options).build();
		HTML_RENDERER = HtmlRenderer.builder(options).build();
	}

	private static class ReportHtmlEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 1L;

		private static final ViewFactory FACTORY = new HTMLFactory() {
			@Override
			public View create(Element elem) {
				AttributeSet attrs = elem.getAttributes();
				Object o = attrs.getAttribute(StyleConstants.NameAttribute);
				if (o instanceof HTML.Tag) {
					HTML.Tag kind = (HTML.Tag) o;
					if (kind == HTML.Tag.HTML || kind == HTML.Tag.BODY || kind == HTML.Tag.PRE) {
						// Permet au document et blocs préformatés de s'adapter fluidement à toute largeur sans buter sur un minimumSpan
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

	private CardLayout cardLayout;
	private JPanel cardPanel;

	// Composants Vue HTML
	private JEditorPane htmlView;
	private JScrollPane htmlScrollPane;

	// Composants Vue Code Source
	private CodeLabTextArea codeArea;
	private RTextScrollPane codeScrollPane;

	private boolean showingPreview = true;
	public File extern_file;

	// Diagrammes Mermaid : largeur de la vue lors du dernier rendu, et temporisation du re-rendu au redimensionnement
	private int lastDiagramViewportWidth = -1;
	private Timer diagramRelayoutTimer;

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public MarkdownViewer(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		init(module, ".md");
	}

	public MarkdownViewer(ModuleEditor module, String ext) {
		super();
		init(module, ext != null ? ext : ".md");
	}

	private void init(ModuleEditor module, String ext) {
		this.module = module;
		this.toolbar = new ToolBarMarkdown(module, this);

		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);

		// 1. Vue HTML Prévisualisation
		htmlView = new JEditorPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean getScrollableTracksViewportWidth() {
				// Adapte la largeur du rapport à la largeur de la fenêtre et supprime le slider horizontal
				return true;
			}
		};
		htmlView.setEditorKit(new ReportHtmlEditorKit());
		htmlView.setEditable(false);
		htmlView.setBackground(Color.WHITE);
		htmlView.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

		// Prise en charge des liens hypertextes
		htmlView.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
					if (e.getURL() != null) {
						Utils.openBrowser(e.getURL().toString());
					} else if (e.getDescription() != null) {
						Utils.openBrowser(e.getDescription());
					}
				}
			}
		});

		htmlScrollPane = new JScrollPane(htmlView);
		htmlScrollPane.setBorder(BorderFactory.createEmptyBorder());
		htmlScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		htmlScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// Les diagrammes Mermaid sont plafonnés à la largeur de la vue au moment du rendu :
		// on refait le rendu (instantané, images en cache) si cette largeur change notablement.
		htmlScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				scheduleDiagramRelayout();
			}
		});

		// 2. Vue Code Source Markdown
		codeArea = new CodeLabTextArea(".md");
		codeArea.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate(DocumentEvent e) { setModified(true); }
			public void insertUpdate(DocumentEvent e) { setModified(true); }
			public void changedUpdate(DocumentEvent e) { setModified(true); }
		});
		codeScrollPane = new RTextScrollPane(codeArea);
		codeScrollPane.setBorder(BorderFactory.createEmptyBorder());
		codeScrollPane.setIconRowHeaderEnabled(false);
		codeScrollPane.getGutter().setBookmarkingEnabled(false);

		// Assemblage des cartes
		cardPanel.add(htmlScrollPane, VIEW_PREVIEW);
		cardPanel.add(codeScrollPane, VIEW_CODE);

		setLayout(new BorderLayout());
		add(cardPanel, BorderLayout.CENTER);

		showPreview();
	}

	///////////////////////////////////////////////////
	// Bascule Aperçu / Code
	///////////////////////////////////////////////////

	public boolean isShowingPreview() {
		return showingPreview;
	}

	public void showPreview() {
		renderMarkdown();
		cardLayout.show(cardPanel, VIEW_PREVIEW);
		showingPreview = true;
	}

	public void showSource() {
		cardLayout.show(cardPanel, VIEW_CODE);
		showingPreview = false;
		SwingUtilities.invokeLater(() -> codeArea.requestFocusInWindow());
	}

	public void toggleView() {
		if (showingPreview) {
			showSource();
		} else {
			showPreview();
		}
	}

	///////////////////////////////////////////////////
	// Moteur de rendu Markdown vers HTML & Styles CSS
	///////////////////////////////////////////////////

	public static String getRawReportCss() {
		File cssFile = codelab.CodeLab.getReportCssFile();
		if (cssFile != null && cssFile.exists() && cssFile.length() > 0) {
			try {
				return Files.readString(cssFile.toPath(), StandardCharsets.UTF_8);
			} catch (Exception ignored) {
			}
		}

		try (InputStream is = MarkdownViewer.class.getResourceAsStream("/data/report.css")) {
			if (is != null) {
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (Exception ignored) {
		}

		return null;
	}

	public static String prepareCssForSwing(String rawCss, int fontSize) {
		if (rawCss == null) rawCss = "";

		// Nettoyage des commentaires CSS pour préserver le parseur Swing
		String css = rawCss.replaceAll("/\\*[\\s\\S]*?\\*/", "");

		if (css.contains("${FONT_SIZE}")) {
			css = css.replace("${FONT_SIZE}", String.valueOf(fontSize));
		}

		// Patch de compatibilité Swing
		StringBuilder patch = new StringBuilder();
		patch.append("\n/* Codelab Swing Compatibility Patch */\n");
		patch.append("body { font-size: ").append(fontSize).append("px; }\n");
		patch.append(".markdown-body { padding: 18px 24px; }\n");
		patch.append("li p { margin-top: 0; margin-bottom: 8px; }\n");
		patch.append("ol { padding-left: 28px; margin-top: 14px; margin-bottom: 14px; }\n");
		patch.append("ol li { margin-top: 14px; margin-bottom: 14px; }\n");
		patch.append("ol li p { margin-top: 0; margin-bottom: 10px; }\n");
		patch.append("ul { padding-left: 28px; }\n");
		patch.append("ul li { margin-top: 2px; margin-bottom: 4px; }\n");
		patch.append("pre { font-family: monospace; max-width: 100%; }\n");
		patch.append("code, tt { font-family: monospace; }\n");

		return css + patch.toString();
	}

	/**
	 * Feuille de style dédiée aux rapports d'évaluation Markdown avec patch de compatibilité Swing.
	 * Chargée en fonction du style sélectionné (REPORT_STYLE) avec repli élégant.
	 */
	public static String getReportCss(int fontSize) {
		codelab.utils.ResourceUtils.initEmbeddedFonts();

		String raw = getRawReportCss();
		if (raw == null || raw.isBlank()) {
			raw = "body { font-family: Helvetica, arial, sans-serif; font-size: ${FONT_SIZE}px; line-height: 1.6; color: #333333; background-color: #ffffff; margin: 0; padding: 0; }\n"
				+ "a { color: #4183c4; text-decoration: none; }\n"
				+ "h1, h2, h3, h4, h5, h6 { margin: 20px 0 10px; padding: 0; font-weight: bold; }\n"
				+ "h1 { font-size: 2.0em; color: #000000; }\n"
				+ "h2 { font-size: 1.7em; border-bottom: 1px solid #cccccc; color: #000000; padding-bottom: 4px; }\n"
				+ "h3 { font-size: 1.3em; color: #000000; }\n"
				+ "h4 { font-size: 1.15em; color: #333333; }\n"
				+ "h5 { font-size: 1.0em; color: #333333; }\n"
				+ "h6 { font-size: 1.0em; color: #777777; }\n"
				+ "p { margin: 14px 0; line-height: 1.6; }\n"
				+ "blockquote { border-left: 4px solid #dddddd; padding: 0 15px; color: #777777; margin: 15px 0; }\n"
				+ "blockquote p { margin: 6px 0; }\n"
				+ "hr { border: 0; border-top: 1px solid #cccccc; height: 1px; margin: 20px 0; }\n"
				+ "ul { margin: 12px 0; padding-left: 28px; }\n"
				+ "ul li { margin-top: 1px; margin-bottom: 3px; line-height: 1.35; }\n"
				+ "ul li p { margin-top: 0; margin-bottom: 4px; }\n"
				+ "ol { margin-top: 18px; margin-bottom: 16px; padding-left: 28px; }\n"
				+ "ol li { margin-top: 18px; margin-bottom: 18px; line-height: 1.5; }\n"
				+ "ol li p { margin-top: 0; margin-bottom: 12px; }\n"
				+ "li { margin: 0; }\n"
				+ "li p { margin-top: 0; margin-bottom: 8px; }\n"
				+ "table { border-collapse: collapse; margin: 15px 0; width: 100%; }\n"
				+ "table tr { border-top: 1px solid #cccccc; background-color: #ffffff; }\n"
				+ "table tr th { font-weight: bold; border: 1px solid #cccccc; padding: 6px 13px; background-color: #f8f8f8; text-align: left; }\n"
				+ "table tr td { border: 1px solid #cccccc; padding: 6px 13px; text-align: left; }\n"
				+ "code, tt { font-family: monospace; font-size: 0.90em; margin: 0 2px; padding: 1px 5px; border: 1px solid #eaeaea; background-color: #f8f8f8; color: #333333; }\n"
				+ "pre { font-family: monospace; font-size: 0.90em; background-color: #f8f8f8; border: 1px solid #cccccc; line-height: 1.45; padding: 8px 12px; margin: 15px 0; }\n"
				+ "pre code, pre tt { font-family: monospace; font-size: 1.0em; background: transparent; border: none; padding: 0; margin: 0; }\n"
				+ ".markdown-body { padding: 18px 24px; }\n";
		}

		return prepareCssForSwing(raw, fontSize);
	}

	/**
	 * Pré-traite le texte Markdown pour garantir un rendu irréprochable :
	 * - Protège les blocs de code clôturés.
	 * - Insère une ligne vide avant les items de liste numérotée s'ils sont collés au paragraphe précédent.
	 * - Insère un saut de paragraphe net entre les titres numérotés en gras (ex: 1. **Titre :**)
	 *   et le texte explicatif qui suit, évitant qu'il ne soit collé sur la même ligne.
	 */
	public static String preprocessMarkdown(String text) {
		if (text == null || text.isEmpty()) return text;

		// 1. Isoler et protéger les blocs de code clôturés
		java.util.List<String> codeBlocks = new java.util.ArrayList<>();
		java.util.regex.Pattern codeFence = java.util.regex.Pattern.compile(
			"(?m)^([ \t]*)(`{3,}|~{3,})([a-zA-Z0-9_-]*)[ \t]*\r?\n([\\s\\S]*?)\r?\n([ \t]*)\\2[ \t]*$"
		);
		java.util.regex.Matcher m = codeFence.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			codeBlocks.add(m.group(0));
			m.appendReplacement(sb, "@@@CODELAB_CODE_BLOCK_" + (codeBlocks.size() - 1) + "@@@");
		}
		m.appendTail(sb);
		text = sb.toString();

		// 2. Insérer une ligne vide avant les items de liste numérotée (1., 2.) s'ils sont collés au texte précédent
		text = java.util.regex.Pattern.compile("([^\n])\r?\n([ \t]*[0-9]+\\.\\s+)").matcher(text).replaceAll("$1\n\n$2");

		// 3. Traiter les titres numérotés en gras collés au paragraphe suivant
		// Cas A : sur la même ligne (ex: "1. **Titre :** Texte...")
		text = java.util.regex.Pattern.compile("(?m)^([ \t]*[0-9]+\\.\\s+\\*\\*[^*\n]+?\\*\\*:?)[ \t]+([A-Za-zÀ-ÿ0-9\"«'(\\[].+)$")
					  .matcher(text).replaceAll("$1\n\n   $2");

		// Cas B : sur la ligne suivante sans ligne vide (ex: "1. **Titre :**\nTexte...")
		text = java.util.regex.Pattern.compile("(?m)^([ \t]*[0-9]+\\.\\s+\\*\\*[^*\n]+?\\*\\*:?)[ \t]*\r?\n[ \t]*([^\n\r#\\s`].*)$")
					  .matcher(text).replaceAll("$1\n\n   $2");

		// Cas C : avec ligne vide mais sans indentation requise pour imbriquer le paragraphe dans l'item
		text = java.util.regex.Pattern.compile("(?m)^([ \t]*[0-9]+\\.\\s+\\*\\*[^*\n]+?\\*\\*:?)[ \t]*\r?\n[ \t]*\r?\n([ \t]{0,2})([^\n\r#\\s`].*)$")
					  .matcher(text).replaceAll("$1\n\n   $3");

		// 3. Restaurer les blocs de code
		for (int i = 0; i < codeBlocks.size(); i++) {
			text = text.replace("@@@CODELAB_CODE_BLOCK_" + i + "@@@", codeBlocks.get(i));
		}

		return text;
	}

	public void renderMarkdown() {
		renderMarkdown(false);
	}

	/**
	 * Re-rendu différé quand la largeur de la vue change : les diagrammes Mermaid sont plafonnés
	 * à cette largeur. Sans effet pour les documents sans diagramme.
	 */
	private void scheduleDiagramRelayout() {
		if (!showingPreview || codeArea == null) return;
		int width = htmlScrollPane.getViewport().getWidth();
		if (width <= 0 || Math.abs(width - lastDiagramViewportWidth) < 20) return;
		if (!MermaidRenderer.containsMermaid(codeArea.getText())) return;
		if (diagramRelayoutTimer == null) {
			diagramRelayoutTimer = new Timer(250, e -> renderMarkdown(true));
			diagramRelayoutTimer.setRepeats(false);
		}
		diagramRelayoutTimer.restart();
	}

	/**
	 * @param keepScroll conserver la position de défilement (rafraîchissements dus aux diagrammes Mermaid)
	 */
	public synchronized void renderMarkdown(boolean keepScroll) {
		String raw = codeArea != null ? codeArea.getText() : "";
		if (raw == null || raw.trim().isEmpty()) {
			htmlView.setText("<html><body style='color:#8c959f; font-style:italic; font-family:sans-serif; padding:20px;'>"
				+ "<i>(Document Markdown vide)</i></body></html>");
			return;
		}

		try {
			int baseFontSize = CodeLab.FONT_SIZE > 0 ? CodeLab.FONT_SIZE : 14;
			int fontSize = Math.max(10, Math.round(baseFontSize * 0.80f));
			String css = getReportCss(fontSize);

			// Prétraitement, diagrammes Mermaid (avant LaTeX, pour que d'éventuels "$" des diagrammes
			// ne soient pas interprétés comme des formules), puis formules mathématiques éventuelles
			int viewportWidth = htmlScrollPane.getViewport().getWidth();
			lastDiagramViewportWidth = viewportWidth;
			int maxDiagramWidth = viewportWidth > 0 ? Math.max(200, viewportWidth - 80) : 700;
			String preprocessed = MarkdownUtils.underline(preprocessMarkdown(raw));
			float mermaidScale = MermaidRenderer.cssScale(css);
			String withDiagrams = MermaidRenderer.renderMarkdownMermaid(preprocessed, maxDiagramWidth, mermaidScale, this, () -> renderMarkdown(true));
			String processed = LatexRenderer.renderMarkdownMath(withDiagrams, fontSize, 2.2f, 3.4f);
			Node document = MD_PARSER.parse(processed);
			String body = CodeBlockUtils.tagLanguage(HTML_RENDERER.render(document));

			// Nettoyage des commentaires CSS pour préserver le parseur Swing
			String cleanCss = css != null ? css.replaceAll("/\\*[\\s\\S]*?\\*/", "") : "";
			String fullHtml = "<html><head><style>\n" + cleanCss + "\n</style></head><body>\n"
				+ "<div class='markdown-body'>\n" + body + "\n</div>\n</body></html>";

			int scrollValue = keepScroll ? htmlScrollPane.getVerticalScrollBar().getValue() : 0;
			htmlView.setText(fullHtml);
			htmlView.setCaretPosition(0);
			if (keepScroll) {
				// La mise en page du nouveau contenu est différée : on restaure la position une fois qu'elle est faite
				SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(
					() -> htmlScrollPane.getVerticalScrollBar().setValue(scrollValue)));
			}

			File current = getFile() != null ? getFile() : extern_file;
			if (current != null && current.getParentFile() != null && htmlView.getDocument() instanceof HTMLDocument) {
				try {
					((HTMLDocument) htmlView.getDocument()).setBase(current.getParentFile().toURI().toURL());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception e) {
			htmlView.setText("<html><body><pre style='padding:20px;'>" + raw + "</pre></body></html>");
		}
	}

	/**
	 * Ouvre le compte-rendu rendu en HTML dans le navigateur externe par défaut (idéal pour imprimer en PDF).
	 */
	public void openInBrowser() {
		// Le rendu des diagrammes Mermaid est bloquant : il se fait dans un thread dédié, jamais sur l'EDT
		final String sourceText = codeArea.getText();
		new Thread(() -> openInBrowserWorker(sourceText), "MarkdownExport").start();
	}

	private void openInBrowserWorker(String raw) {
		try {
			File tempHtml = File.createTempFile("codelab_report_", ".html");
			tempHtml.deleteOnExit();

			int baseFontSize = CodeLab.FONT_SIZE > 0 ? CodeLab.FONT_SIZE : 14;
			int fontSize = Math.max(11, Math.round(baseFontSize * 0.90f));
			String rawCss = getRawReportCss();
			String css = rawCss != null && !rawCss.isBlank() ? rawCss : getReportCss(fontSize);
			String preprocessed = MarkdownUtils.underline(preprocessMarkdown(raw));
			String withDiagrams = MermaidRenderer.renderMarkdownMermaidBlocking(preprocessed, 780, MermaidRenderer.cssScale(css), 120);
			String processed = LatexRenderer.renderMarkdownMath(withDiagrams, fontSize, 2.2f, 3.4f);
			Node document = MD_PARSER.parse(processed);
			String body = CodeBlockUtils.tagLanguage(HTML_RENDERER.render(document));

			String fullHtml = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='utf-8'>\n<title>Rapport d'évaluation CodeLab</title>\n<style>\n"
				+ (css != null ? css : "") + "\n"
				+ "body { max-width: 860px; margin: 30px auto; padding: 25px 45px; background-color: #ffffff; box-shadow: 0 0 14px rgba(0,0,0,0.08); border-radius: 6px; }\n"
				+ "code, pre { font-family: ui-monospace, SFMono-Regular, \"SF Mono\", Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace !important; }\n"
				+ "@media print { body { max-width: 100% !important; margin: 0 !important; padding: 0 !important; box-shadow: none !important; border-radius: 0 !important; } }\n"
				+ "</style>\n</head>\n<body>\n<div class='markdown-body'>\n" + body + "\n</div>\n</body>\n</html>";

			Files.writeString(tempHtml.toPath(), fullHtml, StandardCharsets.UTF_8);
			final String url = tempHtml.toURI().toString();
			SwingUtilities.invokeLater(() -> Utils.openBrowser(url));
		} catch (Exception e) {
			final String message = e.getMessage();
			SwingUtilities.invokeLater(() -> Utils.showWarningDialog("CodeLab", "Impossible d'ouvrir dans le navigateur : " + message));
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractEditor
	///////////////////////////////////////////////////

	@Override
	public synchronized void save_content() {
		File file = getFile() != null ? getFile() : extern_file;
		if (file == null) return;
		save_content(file);
	}

	@Override
	public synchronized void save_content(File file) {
		if (file == null) return;
		try {
			Files.writeString(file.toPath(), codeArea.getText(), StandardCharsets.UTF_8);
			setModified(false);
		} catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	@Override
	public synchronized void load_content() {
		File file = getFile() != null ? getFile() : extern_file;
		if (file == null || !file.exists()) return;
		load_content(file);
	}

	@Override
	public synchronized void load_content(File file) {
		if (file == null || !file.exists()) return;
		try {
			this.extern_file = file;
			String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			codeArea.setText(content);
			codeArea.setCaretPosition(0);
			setModified(false);
			renderMarkdown();
		} catch (IOException e) {
			codeArea.setText("# Erreur de lecture\nImpossible de charger : " + file.getName());
			renderMarkdown();
		}
	}

	@Override
	public synchronized void load_template(File file) {
		load_content(file);
	}

	@Override
	public boolean isBlank() {
		return codeArea.getText().trim().isEmpty();
	}

	@Override
	public boolean isTextEditor() {
		return false;
	}

	@Override
	public boolean compile_file() {
		return false;
	}

	@Override
	public boolean convertSpaceToTab(int nbspaces) {
		SearchContext context = new SearchContext();
		context.setSearchFor(" ".repeat(nbspaces));
		context.setReplaceWith("\t");
		return SearchEngine.replaceAll(codeArea, context).wasFound();
	}

	@Override
	public boolean convertTabToSpace(int nbspaces) {
		SearchContext context = new SearchContext();
		context.setSearchFor("\t");
		context.setReplaceWith(" ".repeat(nbspaces));
		return SearchEngine.replaceAll(codeArea, context).wasFound();
	}

	@Override
	public void changeFont(String name, int size) {
		codeArea.setFont(new java.awt.Font(name, java.awt.Font.PLAIN, size));
		renderMarkdown();
	}

	@Override
	public void setInvisible(boolean value) {
		codeArea.setWhitespaceVisible(value);
	}

	@Override
	public void setCodeFoldingEnabled(boolean value) {
		codeArea.setCodeFoldingEnabled(value);
	}

	@Override
	public void setEditableConfiguration(boolean value) {
		this.editable = value;
		codeArea.setEditable(value);
	}

	@Override
	public void addLineHighlight(int line, Color color) {
	}

	@Override
	public void removeAllLineHighlights() {
	}

	@Override
	public RSyntaxTextArea getActiveRSyntaxTextArea() {
		return codeArea;
	}
}

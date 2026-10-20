package codelab.modules.editeur.notebook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import codelab.CodeLab;
import codelab.Compilateur;
import codelab.ExceptionManager;
import codelab.modules.editeur.AbstractEditor;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.text.ReplaceDialog;
import codelab.utils.JupyterUtils;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
 * Éditeur visuel interactif natif de carnets Jupyter (.ipynb).
 * Organise le carnet sous forme de cellules de Code (Python) et de Markdown rendues
 * avec support de déplacement, insertion, suppression et exécution pas à pas ou globale.
 * 
 * @author Jérôme Lehuen + Antigravity
 */
public class NotebookEditor extends AbstractEditor {

	private static final long serialVersionUID = 1L;

	private static final Color CANVAS_BG = NotebookCellPane.BORDER_NORMAL;

	public static final int MARGIN_TOP = 20;
	public static final int MARGIN_SIDE = 28;
	public static final int MARGIN_BOTTOM = 24;
	public static final int CELL_SPACING = 18;

	private final List<NotebookCellPane> cellPanes = new ArrayList<>();
	private NotebookCellPane activeCell = null;
	private ObjectNode notebookMetadata = null;

	private static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
		private static final long serialVersionUID = 1L;

		public ScrollablePanel(java.awt.LayoutManager layout) {
			super(layout);
		}

		@Override
		public java.awt.Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 18;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 72;
		}

		@Override
		public java.awt.Dimension getMinimumSize() {
			return new java.awt.Dimension(50, super.getMinimumSize().height);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	private JPanel cellsPanel;
	private ScrollablePanel topWrapper;
	private JScrollPane scrollPane;
	public File extern_file;

	public NotebookEditor(ModuleEditor module, DefaultMutableTreeNode node) {
		super(node);
		this.module = module;
		initUI();
	}

	public NotebookEditor(ModuleEditor module, String ext) {
		super();
		this.module = module;
		initUI();
	}

	private void initUI() {
		if (module != null) {
			toolbar = new ToolBarNotebook(module, this);
		}
		showLockIcon = true;

		cellsPanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(50, super.getMinimumSize().height);
			}
		};
		cellsPanel.setLayout(new BoxLayout(cellsPanel, BoxLayout.Y_AXIS));
		cellsPanel.setBackground(CANVAS_BG);
		cellsPanel.setBorder(BorderFactory.createEmptyBorder(MARGIN_TOP, MARGIN_SIDE, MARGIN_BOTTOM, MARGIN_SIDE));

		topWrapper = new ScrollablePanel(new BorderLayout());
		topWrapper.setBackground(CANVAS_BG);
		topWrapper.add(cellsPanel, BorderLayout.NORTH);

		scrollPane = new JScrollPane(topWrapper);
		scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(18);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setBackground(CANVAS_BG);
		scrollPane.getViewport().setBackground(CANVAS_BG);

		scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				revalidateContainer();
			}
		});

		setLayout(new BorderLayout());
		setBackground(CANVAS_BG);
		add(wrapWithLockLayer(scrollPane), BorderLayout.CENTER);
	}

	public JPanel getCellsPanel() {
		return cellsPanel;
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(50, 50);
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	public void forwardMouseWheel(java.awt.event.MouseWheelEvent e) {
		if (scrollPane != null) {
			scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane));
			e.consume();
		}
	}

	///////////////////////////////////////////////////
	// Gestion des cellules
	///////////////////////////////////////////////////

	public synchronized void addCell(int index, String type, String language, String source) {
		NotebookCellPane cell = new NotebookCellPane(this, type, language, source);
		cell.setEditable(this.editable);
		if (index >= 0 && index <= cellPanes.size()) {
			cellPanes.add(index, cell);
		} else {
			cellPanes.add(cell);
		}
		refreshCellsDisplay();
		SwingUtilities.invokeLater(cell::focusEditor);
	}

	public synchronized void addCell(int index, String type, String source) {
		String defaultLang = isCNotebook() ? "c" : "python";
		addCell(index, type, "c".equalsIgnoreCase(type) ? "c" : defaultLang, source);
	}

	public void addCellAfter(NotebookCellPane current, String type, String language) {
		int idx = cellPanes.indexOf(current);
		if (idx < 0) idx = cellPanes.size() - 1;
		addCell(idx + 1, type, language, "");
		setModified(true);
	}

	public void addCellAfter(NotebookCellPane current, String type) {
		String lang = (current != null) ? current.getLanguage() : (isCNotebook() ? "c" : "python");
		addCellAfter(current, type, lang);
	}

	public void addCellAtEnd(String type) {
		NotebookCellPane last = cellPanes.isEmpty() ? null : cellPanes.get(cellPanes.size() - 1);
		String lang = (last != null) ? last.getLanguage() : (isCNotebook() ? "c" : "python");
		addCell(cellPanes.size(), type, lang, "");
		setModified(true);
	}

	public synchronized void removeCell(NotebookCellPane cell) {
		if (cell != null && cell.isLocked()) return;
		if (cell == this.activeCell) {
			this.activeCell = null;
		}
		if (cellPanes.size() <= 1) {
			cell.setSource("");
			cell.focusEditor();
		} else {
			int idx = cellPanes.indexOf(cell);
			cellPanes.remove(cell);
			refreshCellsDisplay();
			if (!cellPanes.isEmpty()) {
				int nextIdx = Math.min(idx, cellPanes.size() - 1);
				cellPanes.get(nextIdx).focusEditor();
			}
		}
		setModified(true);
	}

	public synchronized void moveCellUp(NotebookCellPane cell) {
		int idx = cellPanes.indexOf(cell);
		if (idx > 0) {
			Collections.swap(cellPanes, idx, idx - 1);
			refreshCellsDisplay();
			cell.focusEditor();
			setModified(true);
		}
	}

	public synchronized void moveCellDown(NotebookCellPane cell) {
		int idx = cellPanes.indexOf(cell);
		if (idx >= 0 && idx < cellPanes.size() - 1) {
			Collections.swap(cellPanes, idx, idx + 1);
			refreshCellsDisplay();
			cell.focusEditor();
			setModified(true);
		}
	}

	public void refreshCellsDisplay() {
		cellsPanel.removeAll();
		for (int i = 0; i < cellPanes.size(); i++) {
			NotebookCellPane cp = cellPanes.get(i);
			cp.setAlignmentX(Component.LEFT_ALIGNMENT);
			cellsPanel.add(cp);
			if (i < cellPanes.size() - 1) {
				Component strut = Box.createVerticalStrut(CELL_SPACING);
				if (strut instanceof javax.swing.JComponent) {
					((javax.swing.JComponent) strut).setAlignmentX(Component.LEFT_ALIGNMENT);
				}
				cellsPanel.add(strut);
			}
		}
		cellsPanel.revalidate();
		cellsPanel.repaint();
		if (topWrapper != null) {
			topWrapper.revalidate();
			topWrapper.repaint();
		}
		if (toolbar != null) toolbar.update();
	}

	public void revalidateContainer() {
		for (NotebookCellPane cp : cellPanes) {
			cp.revalidate();
		}
		cellsPanel.revalidate();
		cellsPanel.repaint();
		if (topWrapper != null) {
			topWrapper.revalidate();
			topWrapper.repaint();
		}
	}

	///////////////////////////////////////////////////
	// Exécution des cellules
	///////////////////////////////////////////////////

	public boolean isCNotebook() {
		int cCount = 0;
		int pyCount = 0;
		for (NotebookCellPane cp : cellPanes) {
			if ("code".equals(cp.getType())) {
				if ("c".equalsIgnoreCase(cp.getLanguage())) {
					cCount++;
				} else {
					pyCount++;
				}
			}
		}
		return cCount > 0 && cCount >= pyCount;
	}

	public void runAllCells() {
		if (module != null && module.codelab != null) {
			module.codelab.execute();
		}
	}

	public void runSingleCell(NotebookCellPane targetCell) {
		if (module == null || module.codelab == null) return;
		if (module.codelab.isRunning()) {
			Utils.showMessageDialog("ATTENTION", "Un programme est déjà en cours d'exécution.");
			return;
		}

		if ("c".equalsIgnoreCase(targetCell.getLanguage())) {
			runSingleCellC(targetCell);
		} else {
			runSingleCellPython(targetCell);
		}
	}

	private void runSingleCellPython(NotebookCellPane targetCell) {
		save_content();

		int targetIndex = cellPanes.indexOf(targetCell);
		if (targetIndex < 0) return;

		StringBuilder code = new StringBuilder();
		code.append("# %% [markdown]\n");
		code.append("# CodeLab Jupyter Notebook - Single Cell Runner\n");
		code.append("import sys\n\n");

		for (int i = 0; i <= targetIndex; i++) {
			NotebookCellPane cp = cellPanes.get(i);
			if ("code".equals(cp.getType())) {
				code.append(String.format("# In[%d]:\n", i + 1));
				code.append(JupyterUtils.cleanCellCode(cp.getSource())).append("\n\n");
			}
		}

		String dir = (getFile() != null) ? getDirectory() : (extern_file != null) ? extern_file.getParent() : CodeLab.PROG_LOCAL_FOLDER;
		String baseName = (getFile() != null) ? getFilename() : (extern_file != null) ? extern_file.getName() : "temp.ipynb";
		File cellScript = new File(dir, baseName + ".cell.py");
		MyFileUtils.writeln(cellScript, code.toString());

		module.codelab.execute(cellScript.getName());
	}

	private void runSingleCellC(NotebookCellPane targetCell) {
		save_content();

		int targetIndex = cellPanes.indexOf(targetCell);
		if (targetIndex < 0) return;

		String dir = (getFile() != null) ? getDirectory() : (extern_file != null) ? extern_file.getParent() : CodeLab.PROG_LOCAL_FOLDER;
		String baseName = (getFile() != null) ? getFilename() : (extern_file != null) ? extern_file.getName() : "temp.ipynb";
		File cellScript = new File(dir, baseName + ".cell.c");

		String cSource = JupyterUtils.generateProgressiveC(cellPanes, targetIndex);
		MyFileUtils.writeln(cellScript, cSource);

		Compilateur comp = new Compilateur(module, cellScript);
		boolean ok = comp.compile();
		if (!ok) {
			if (module.codelab.getConsole() != null) {
				module.codelab.getConsole().flush();
			}
			return;
		}

		module.codelab.execute(cellScript.getName());
	}

	///////////////////////////////////////////////////
	// Cellule active et barre d'outils
	///////////////////////////////////////////////////

	public NotebookCellPane getActiveCell() {
		if (activeCell != null && cellPanes.contains(activeCell)) {
			return activeCell;
		}
		return null;
	}

	public boolean isCellActive(NotebookCellPane cell) {
		return cell != null && this.activeCell == cell && cellPanes.contains(cell);
	}

	public NotebookCellPane getActionTargetCell() {
		NotebookCellPane active = getActiveCell();
		if (active != null) return active;
		if (!cellPanes.isEmpty()) return cellPanes.get(0);
		return null;
	}

	public void setActiveCell(NotebookCellPane cell) {
		if (this.activeCell == cell) return;
		NotebookCellPane prev = this.activeCell;
		this.activeCell = cell;
		if (prev != null) {
			prev.updateActiveState();
		}
		if (cell != null) {
			cell.updateActiveState();
		}
		if (toolbar != null) {
			toolbar.update();
		}
	}

	public List<NotebookCellPane> getCellPanes() {
		return Collections.unmodifiableList(cellPanes);
	}

	@Override
	public RSyntaxTextArea getActiveRSyntaxTextArea() {
		NotebookCellPane cell = getActionTargetCell();
		return (cell != null) ? cell.getActiveTextArea() : null;
	}

	///////////////////////////////////////////////////
	// Méthodes publiques de la barre d'outils
	///////////////////////////////////////////////////

	public void undo() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta != null && ta.canUndo()) {
			ta.undoLastAction();
		}
	}

	public void redo() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta != null && ta.canRedo()) {
			ta.redoLastAction();
		}
	}

	public void entab() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return;

		int deb = ta.getSelectionStart();
		int fin = ta.getSelectionEnd();
		try {
			int line_deb = ta.getLineOfOffset(deb);
			int line_fin = ta.getLineOfOffset(fin);

			for (int line = line_deb; line <= line_fin; line++) {
				ta.insert("\t", ta.getLineStartOffset(line));
			}

			ta.setSelectionStart(ta.getLineStartOffset(line_deb));
			ta.setSelectionEnd(ta.getLineEndOffset(line_fin) - 1);
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	public void detab() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return;

		int deb = ta.getSelectionStart();
		int fin = ta.getSelectionEnd();
		try {
			int line_deb = ta.getLineOfOffset(deb);
			int line_fin = ta.getLineOfOffset(fin);

			for (int line = line_fin; line >= line_deb; line--) {
				int start = ta.getLineStartOffset(line);
				int end = ta.getLineEndOffset(line);
				if (start < end) {
					String lineText = ta.getText(start, end - start);
					if (lineText.startsWith("\t")) {
						ta.replaceRange("", start, start + 1);
					} else if (lineText.startsWith("    ")) {
						ta.replaceRange("", start, start + 4);
					}
				}
			}

			ta.setSelectionStart(ta.getLineStartOffset(line_deb));
			ta.setSelectionEnd(Math.max(ta.getLineStartOffset(line_deb), ta.getLineEndOffset(line_fin) - 1));
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	public void comment() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return;

		String commentStr = "c".equalsIgnoreCase(cell.getLanguage()) ? "// " : "# ";
		int deb = ta.getSelectionStart();
		int fin = ta.getSelectionEnd();
		try {
			int line_deb = ta.getLineOfOffset(deb);
			int line_fin = ta.getLineOfOffset(fin);

			for (int line = line_deb; line <= line_fin; line++) {
				ta.insert(commentStr, ta.getLineStartOffset(line));
			}

			ta.setSelectionStart(ta.getLineStartOffset(line_deb));
			ta.setSelectionEnd(ta.getLineEndOffset(line_fin) - 1);
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	public void uncomment() {
		NotebookCellPane cell = getActionTargetCell();
		if (cell == null || cell.isLocked()) return;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return;

		int deb = ta.getSelectionStart();
		int fin = ta.getSelectionEnd();
		try {
			int line_deb = ta.getLineOfOffset(deb);
			int line_fin = ta.getLineOfOffset(fin);

			for (int line = line_fin; line >= line_deb; line--) {
				int start = ta.getLineStartOffset(line);
				int end = ta.getLineEndOffset(line);
				if (start < end) {
					String lineText = ta.getText(start, end - start);
					if (lineText.startsWith("// ")) {
						ta.replaceRange("", start, start + 3);
					} else if (lineText.startsWith("//")) {
						ta.replaceRange("", start, start + 2);
					} else if (lineText.startsWith("# ")) {
						ta.replaceRange("", start, start + 2);
					} else if (lineText.startsWith("#")) {
						ta.replaceRange("", start, start + 1);
					}
				}
			}

			ta.setSelectionStart(ta.getLineStartOffset(line_deb));
			ta.setSelectionEnd(Math.max(ta.getLineStartOffset(line_deb), ta.getLineEndOffset(line_fin) - 1));
		} catch (BadLocationException e) {
			ExceptionManager.process(e);
		}
	}

	public void findAndReplaceDialog() {
		if (ReplaceDialog.window == null) {
			new ReplaceDialog(module);
		} else {
			ReplaceDialog.window.toFront();
			ReplaceDialog.window.requestFocus();
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de chargement et sauvegarde
	///////////////////////////////////////////////////

	@Override
	public synchronized void save_content() {
		File file = getFile();
		if (file == null) {
			if (extern_file == null) return;
			save_content(extern_file);
		} else {
			save_content(file);
			setModified(false);
		}
	}

	@Override
	public synchronized void save_content(File file) {
		if (file == null) return;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode root = mapper.createObjectNode();
			ArrayNode cellsArray = root.putArray("cells");

			for (NotebookCellPane cp : cellPanes) {
				ObjectNode c = mapper.createObjectNode();
				String cType = cp.getType();
				c.put("cell_type", cType);
				ObjectNode meta = mapper.createObjectNode();

				if ("code".equals(cType)) {
					c.putNull("execution_count");
					c.putArray("outputs");
					meta.put("language", cp.getLanguage());
				}
				if (cp.isLocked()) {
					meta.put("editable", false);
				}
				c.set("metadata", meta);

				ArrayNode srcArr = c.putArray("source");
				String text = cp.getSource();
				if (text != null && !text.isEmpty()) {
					String[] lines = text.split("\n", -1);
					for (int i = 0; i < lines.length; i++) {
						String line = lines[i];
						if (i < lines.length - 1) {
							srcArr.add(line + "\n");
						} else if (!line.isEmpty()) {
							srcArr.add(line);
						}
					}
				}
				cellsArray.add(c);
			}

			if (notebookMetadata != null) {
				root.set("metadata", notebookMetadata);
			} else if (isCNotebook()) {
				ObjectNode meta = root.putObject("metadata");
				ObjectNode kernelspec = meta.putObject("kernelspec");
				kernelspec.put("display_name", "C");
				kernelspec.put("language", "c");
				kernelspec.put("name", "c");
				ObjectNode langInfo = meta.putObject("language_info");
				langInfo.put("name", "c");
				langInfo.put("version", "11");
			} else {
				ObjectNode meta = root.putObject("metadata");
				ObjectNode kernelspec = meta.putObject("kernelspec");
				kernelspec.put("display_name", "Python 3");
				kernelspec.put("language", "python");
				kernelspec.put("name", "python3");
				ObjectNode langInfo = meta.putObject("language_info");
				langInfo.put("name", "python");
				langInfo.put("version", "3");
			}

			root.put("nbformat", 4);
			root.put("nbformat_minor", 4);

			mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
			setModified(false);
		} catch (Exception e) {
			ExceptionManager.process(e);
		}
	}

	@Override
	public synchronized void load_content() {
		File file = getFile();
		if (file == null) return;
		load_content(file);
		extern_file = null;
	}

	@Override
	public synchronized void load_content(File file) {
		if (file == null || !file.exists()) return;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(file);
			if (root == null || !root.isObject()) return;

			boolean isNotebookC = false;
			if (root.has("metadata") && root.get("metadata").isObject()) {
				notebookMetadata = (ObjectNode) root.get("metadata").deepCopy();
				String klang = notebookMetadata.path("kernelspec").path("language").asText("");
				String llang = notebookMetadata.path("language_info").path("name").asText("");
				if ("c".equalsIgnoreCase(klang) || "c".equalsIgnoreCase(llang)) {
					isNotebookC = true;
				}
			} else {
				notebookMetadata = null;
			}

			int savedScroll = (scrollPane != null && scrollPane.getVerticalScrollBar() != null) ? scrollPane.getVerticalScrollBar().getValue() : -1;
			int activeIndex = (this.activeCell != null) ? cellPanes.indexOf(this.activeCell) : -1;

			this.activeCell = null;
			cellPanes.clear();
			JsonNode cellsNode = root.get("cells");
			if (cellsNode != null && cellsNode.isArray()) {
				for (JsonNode cNode : cellsNode) {
					String cellType = cNode.path("cell_type").asText("code");
					String cellLang = cNode.path("metadata").path("language").asText("");
					if (cellLang.isEmpty() && isNotebookC) {
						cellLang = "c";
					}
					if (cellLang.isEmpty()) {
						cellLang = "python";
					}
					boolean isCellLocked = false;
					JsonNode metaNode = cNode.path("metadata");
					if (metaNode.has("editable") && !metaNode.path("editable").asBoolean(true)) {
						isCellLocked = true;
					} else if (metaNode.path("locked").asBoolean(false)) {
						isCellLocked = true;
					}
					String source = JupyterUtils.extractSourceText(cNode.get("source"));
					NotebookCellPane cp = new NotebookCellPane(this, cellType, cellLang, source);
					cp.setEditable(editable);
					if (isCellLocked) {
						cp.setLocked(true);
					}
					cellPanes.add(cp);
				}
			}

			if (cellPanes.isEmpty()) {
				NotebookCellPane cp = new NotebookCellPane(this, "code", isNotebookC ? "c" : "python", "");
				cp.setEditable(editable);
				cellPanes.add(cp);
			}

			refreshCellsDisplay();
			if (activeIndex >= 0 && activeIndex < cellPanes.size()) {
				setActiveCell(cellPanes.get(activeIndex));
			}
			if (savedScroll >= 0 && scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
				SwingUtilities.invokeLater(() -> {
					if (scrollPane != null && scrollPane.getVerticalScrollBar() != null) {
						scrollPane.getVerticalScrollBar().setValue(savedScroll);
					}
				});
			}
			setModified(false);
		} catch (Exception e) {
			ExceptionManager.process(e);
		}
	}

	@Override
	public void load_template(File file) {
		load_content(file);
		String filename = getFilename();
		String username = (CodeLab.INSTANCE != null) ? CodeLab.INSTANCE.getUsername() : "";
		String date = Utils.getDate();

		for (NotebookCellPane cp : cellPanes) {
			String src = cp.getSource();
			if (src != null) {
				if (filename != null) src = src.replace("_fileName_", filename);
				if (username != null) src = src.replace("_author_", username);
				src = src.replace("_date_", date);
				cp.setSource(src);
			}
		}
		setModified(true);
	}

	@Override
	public void setModified(boolean value) {
		super.setModified(value);
		if (value) {
			resetCompilationFlag();
		}
	}

	@Override
	public boolean compilation_OK() {
		if (isCNotebook()) {
			File file = getFile();
			if (file == null) file = extern_file;
			if (file != null) {
				File exeFile = new File(file.getParent(), MyFileUtils.noExtension(file.getName() + ".c") + "_c.exe");
				if (!exeFile.exists()) return false;
			}
		}
		return compilation_ok;
	}

	@Override
	public boolean compile_file() {
		save_content();
		File file = getFile();
		if (file == null) file = extern_file;
		if (file != null) {
			if (isCNotebook()) {
				File cFile = new File(file.getAbsolutePath() + ".c");
				try {
					JupyterUtils.extractRunnableC(file, cFile);
					Compilateur comp = new Compilateur(module, cFile);
					compilation_ok = comp.compile();
					return compilation_ok;
				} catch (Exception e) {
					ExceptionManager.process(e);
					compilation_ok = false;
					return false;
				}
			} else {
				File pyFile = new File(file.getAbsolutePath() + ".py");
				try {
					JupyterUtils.extractRunnablePython(file, pyFile);
				} catch (Exception e) {
					ExceptionManager.process(e);
					compilation_ok = false;
					return false;
				}
			}
		}
		compilation_ok = true;
		return true;
	}

	///////////////////////////////////////////////////
	// Implémentation du contrat AbstractEditor
	///////////////////////////////////////////////////

	@Override
	public boolean isBlank() {
		if (cellPanes.isEmpty()) return true;
		for (NotebookCellPane cp : cellPanes) {
			if (!cp.getSource().trim().isEmpty()) return false;
		}
		return true;
	}

	@Override
	public boolean isTextEditor() {
		return false;
	}

	@Override
	public boolean convertSpaceToTab(int nbspaces) {
		NotebookCellPane cell = getActiveCell();
		if (cell == null || cell.isLocked()) return false;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return false;
		SearchContext context = new SearchContext();
		context.setSearchFor(" ".repeat(nbspaces));
		context.setReplaceWith("\t");
		return SearchEngine.replaceAll(ta, context).wasFound();
	}

	@Override
	public boolean convertTabToSpace(int nbspaces) {
		NotebookCellPane cell = getActiveCell();
		if (cell == null || cell.isLocked()) return false;
		RSyntaxTextArea ta = cell.getActiveTextArea();
		if (ta == null) return false;
		SearchContext context = new SearchContext();
		context.setSearchFor("\t");
		context.setReplaceWith(" ".repeat(nbspaces));
		return SearchEngine.replaceAll(ta, context).wasFound();
	}

	@Override
	public void changeFont(String name, int size) {
		for (NotebookCellPane cp : cellPanes) {
			cp.updateFont(name, size);
		}
	}

	@Override
	public void setInvisible(boolean value) {}

	@Override
	public void setCodeFoldingEnabled(boolean value) {}

	@Override
	public void setEditableConfiguration(boolean value) {
		this.editable = value;
		removeAllLineHighlights();
		for (NotebookCellPane cp : cellPanes) {
			cp.setEditable(value);
		}
		if (toolbar != null) {
			toolbar.update();
		}
		updateLockBadge();
		repaint();
	}

	public List<String> getCellSources() {
		List<String> sources = new ArrayList<>(cellPanes.size());
		for (NotebookCellPane cp : cellPanes) {
			sources.add(cp.getSource() != null ? cp.getSource() : "");
		}
		return sources;
	}

	@Override
	public void addLineHighlight(int line, Color color) {
		NotebookCellPane target = getActionTargetCell();
		if (target != null) {
			target.addLineHighlight(line, color);
		}
	}

	@Override
	public void removeAllLineHighlights() {
		for (NotebookCellPane cp : cellPanes) {
			cp.removeAllLineHighlights();
		}
	}

	public void highlightDifferences(List<String> oldSources, Color color) {
		removeAllLineHighlights();
		if (oldSources == null) return;
		int minCount = Math.min(oldSources.size(), cellPanes.size());
		for (int c = 0; c < minCount; c++) {
			String oldSrc = oldSources.get(c);
			NotebookCellPane cellPane = cellPanes.get(c);
			String newSrc = cellPane.getSource() != null ? cellPane.getSource() : "";

			if (oldSrc.equals(newSrc)) {
				continue;
			}

			List<String> original = Arrays.asList(oldSrc.split("\r?\n", -1));
			List<String> revised = Arrays.asList(newSrc.split("\r?\n", -1));
			Patch<String> patch = DiffUtils.diff(original, revised);

			for (AbstractDelta<String> delta : patch.getDeltas()) {
				int targetPos = delta.getTarget().getPosition();
				int targetSize = delta.getTarget().size();
				if (targetSize > 0) {
					for (int i = 0; i < targetSize; i++) {
						cellPane.addLineHighlight(targetPos + i, color);
					}
				} else {
					cellPane.addLineHighlight(targetPos, color);
				}
			}
		}

		if (cellPanes.size() > oldSources.size()) {
			for (int c = oldSources.size(); c < cellPanes.size(); c++) {
				NotebookCellPane cellPane = cellPanes.get(c);
				int lineCount = cellPane.getLineCount();
				for (int i = 0; i < lineCount; i++) {
					cellPane.addLineHighlight(i, color);
				}
			}
		}
	}

	public void refreshMarkdownViews() {
		for (NotebookCellPane cp : cellPanes) {
			cp.refreshHtmlView();
		}
		revalidateContainer();
	}
}

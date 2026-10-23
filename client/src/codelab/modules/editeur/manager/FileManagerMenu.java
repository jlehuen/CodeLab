package codelab.modules.editeur.manager;

import java.awt.Point;
import java.io.File;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.io.FileUtils;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.eval.SessionEvaluator;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.JupyterUtils;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

import static codelab.CodeLab.LABEL;

/**
 * Gestionnaire des menus contextuels (clic droit) du FileManager.
 * Construit dynamiquement les menus selon le mode (Programmes vs Évaluations),
 * le statut de l'utilisateur (étudiant, tuteur, administrateur) et le contexte de clic
 * (fond de l'arborescence, dossier, fichier ou sélection multiple).
 *
 * @author Jérôme Lehuen
 */
public class FileManagerMenu {

	private final FileManager manager;
	private final ModuleEditor editor;
	private final CodeLab codelab;

	public FileManagerMenu(FileManager manager, ModuleEditor editor, CodeLab codelab) {
		this.manager = manager;
		this.editor = editor;
		this.codelab = codelab;
	}

	/**
	 * Point d'entrée principal : affiche le menu contextuel approprié.
	 */
	public void showPopupMenu(Point point, TreePath path) {
		// Gestion de la sélection sous le curseur
		if (path != null) {
			if (!manager.isPathSelected(path)) {
				manager.setSelectionPath(path);
			}
			manager.setSelectedNode((DefaultMutableTreeNode) path.getLastPathComponent());
		} else {
			manager.clearSelection();
			manager.setSelectedNode(null);
		}

		DefaultMutableTreeNode selectedNode = manager.getSelectedNode();

		if (manager.getMode() == FileManager.MODE_EVALUATIONS) {
			showEvaluationsMenu(point, selectedNode);
		} else if (codelab.isTutor() || codelab.isAdmin()) {
			showTutorMenu(point, selectedNode);
		} else if (isMenuAllowed()) {
			showStudentMenu(point, selectedNode);
		}
	}

	private boolean isMenuAllowed() {
		return !codelab.isTutor() && !codelab.isAdmin() && !editor.controlled;
	}

	// -----------------------------------------------------------
	// 1. Menu Mode Évaluations
	// -----------------------------------------------------------

	private void showEvaluationsMenu(Point point, DefaultMutableTreeNode selectedNode) {
		JPopupMenu menu = new JPopupMenu();

		// 1. Retour vers l'arborescence des programmes
		JMenuItem itemReturn = new JMenuItem(LABEL("menuViewPrograms"));
		itemReturn.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_PROGRAMS));
		menu.add(itemReturn);
		menu.add(new JSeparator());

		// 2. Navigation : Recharger, Développer, Replier
		addNavigationControls(menu, true);
		menu.add(new JSeparator());

		// 3. Outils d'évaluation : Éditer les pondérations & Recalculer les notes
		File targetDir = null;
		if (selectedNode != null) {
			FileDescr fd = manager.getFileDescr(selectedNode);
			if (fd != null) {
				targetDir = fd.isDirectory() ? fd.getFile() : fd.getFile().getParentFile();
			}
		}
		addEvaluationTools(menu, targetDir);

		// 4. Suppression et ouverture du dossier
		if (selectedNode == null) {
			JMenuItem itemDelAll = new JMenuItem(LABEL("itemDeleteAllEvaluations"));
			itemDelAll.addActionListener(ev -> deleteAllEvaluations());
			menu.add(itemDelAll);

			JMenuItem itemOpen = new JMenuItem(LABEL("menuOpenEvaluationsFolder"));
			itemOpen.addActionListener(ev -> FileManager.openFileOrFolder(new File(CodeLab.EVALUATIONS_FOLDER)));
			menu.add(itemOpen);
		} else {
			FileDescr fd = manager.getFileDescr(selectedNode);
			if (fd == null) return;

			if (fd.isDirectory()) {
				boolean isRootEval = fd.getFile().getAbsolutePath().equals(new File(CodeLab.EVALUATIONS_FOLDER).getAbsolutePath());
				if (isRootEval) {
					JMenuItem itemDelAll = new JMenuItem(LABEL("itemDeleteAllEvaluations"));
					itemDelAll.addActionListener(ev -> deleteAllEvaluations());
					menu.add(itemDelAll);
				} else {
					JMenuItem itemDel = new JMenuItem(String.format(LABEL("Delete_folder"), fd.getFilename()));
					itemDel.addActionListener(ev -> deleteEvaluationFolder(fd.getFile(), fd.getFilename()));
					menu.add(itemDel);
				}

				JMenuItem itemOpen = new JMenuItem(LABEL("menuOpenEvaluationsFolder"));
				itemOpen.addActionListener(ev -> FileManager.openFileOrFolder(fd.getFile()));
				menu.add(itemOpen);
			} else if (fd.isFile()) {
				JMenuItem itemDel = new JMenuItem(LABEL("itemDeleteEvaluation"));
				itemDel.addActionListener(ev -> deleteEvaluationFile(fd.getFile(), fd.getFilename()));
				menu.add(itemDel);

				JMenuItem itemOpen = new JMenuItem(LABEL("menuOpenEvaluationsFolder"));
				itemOpen.addActionListener(ev -> FileManager.openFileOrFolder(fd.getFile().getParentFile()));
				menu.add(itemOpen);
			}
		}

		menu.show(manager, point.x, point.y);
	}

	/**
	 * Ajoute les entrées d'édition des pondérations et de recalcul des notes.
	 */
	private void addEvaluationTools(JPopupMenu menu, File targetDir) {
		JMenuItem itemEditWeights = new JMenuItem(LABEL("menuEditWeights"));
		itemEditWeights.addActionListener(ev -> SessionEvaluator.openWeightsFile(codelab, null));
		menu.add(itemEditWeights);

		JMenuItem itemRecalc = new JMenuItem(LABEL("menuRecalculateGrades"));
		itemRecalc.addActionListener(ev -> SessionEvaluator.recalculateSessionGrades(codelab, targetDir));
		menu.add(itemRecalc);
	}

	// -----------------------------------------------------------
	// 2. Menu Tuteur / Administrateur (Mode Programmes)
	// -----------------------------------------------------------

	private void showTutorMenu(Point point, DefaultMutableTreeNode selectedNode) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem itemEval = new JMenuItem(LABEL("menuViewEvaluations"));
		itemEval.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_EVALUATIONS));
		menu.add(itemEval);
		menu.add(new JSeparator());

		if (selectedNode == null) {
			addNavigationControls(menu, false);
		} else {
			FileDescr fd = manager.getFileDescr(selectedNode);
			if (fd == null) return;

			if (fd.isDirectory()) {
				if (!FileManager.isEvaluation(fd.getFile())) {
					JCheckBoxMenuItem itemUseAi = new JCheckBoxMenuItem(LABEL("evalUseAiCheckbox"), SessionEvaluator.isUseAi());
					itemUseAi.addItemListener(e -> SessionEvaluator.setUseAi(itemUseAi.getState()));
					menu.add(itemUseAi);
					JMenu subMenuEval = new JMenu(LABEL("menuEvalFolder"));
					SessionEvaluator.populateSkillsSubmenu(subMenuEval, (skill, useAi) -> SessionEvaluator.evaluateFolder(codelab, fd.getFile(), skill, useAi));
					menu.add(subMenuEval);
					menu.add(new JSeparator());
				}
				addNavigationControls(menu, false);
			} else if (fd.isFile()) {
				if (FileManager.isClone(fd.getFile())) {
					JMenuItem itemDel = new JMenuItem(String.format(LABEL("Delete_clone"), fd.getFilename()));
					itemDel.addActionListener(ev -> manager.delete_clone(selectedNode, fd));
					menu.add(itemDel);
					menu.add(new JSeparator());
				} else if (!FileManager.isEvaluation(fd.getFile())) {
					JMenuItem itemCln = new JMenuItem(String.format(LABEL("Clone_program_file"), fd.getFilename()));
					itemCln.addActionListener(ev -> manager.clone_file(fd, selectedNode));
					menu.add(itemCln);
					menu.add(new JSeparator());

					if (fd.getFilename().toLowerCase().endsWith(".ipynb")) {
						boolean isC = JupyterUtils.isCNotebook(fd.getFile());
						JMenuItem itemExport = new JMenuItem(isC ? LABEL("Export_notebook_c") : LABEL("Export_notebook_py"));
						itemExport.setIcon(isC ? FileDescr.icon_c : FileDescr.icon_python);
						itemExport.addActionListener(ev -> exportNotebookToScript(fd.getFile(), isC));
						menu.add(itemExport);
						menu.add(new JSeparator());
					}
				}
				addNavigationControls(menu, false);
			}
		}

		menu.show(manager, point.x, point.y);
	}

	// -----------------------------------------------------------
	// 3. Menu Étudiant (Mode Programmes)
	// -----------------------------------------------------------

	private void showStudentMenu(Point point, DefaultMutableTreeNode selectedNode) {
		JPopupMenu menu = new JPopupMenu();

		if (selectedNode == null) {
			// Clic dans le fond de l'arbre
			JMenuItem itemEvalRoot = new JMenuItem(LABEL("menuViewEvaluations"));
			itemEvalRoot.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_EVALUATIONS));
			menu.add(itemEvalRoot);
			menu.add(new JSeparator());

			JMenuItem item8 = new JMenuItem(LABEL("New_root_program"));
			item8.addActionListener(ev -> manager.new_root_file());
			menu.add(item8);

			JMenuItem item9 = new JMenuItem(LABEL("New_root_folder"));
			item9.addActionListener(ev -> manager.new_root_folder());
			menu.add(item9);
			menu.add(new JSeparator());

			addNavigationControls(menu, false);

			if (!codelab.isConnected()) {
				menu.add(new JSeparator());
				JMenuItem item12 = new JMenuItem(LABEL("Copy_dist_folder"));
				item12.addActionListener(ev -> manager.copyDist());
				menu.add(item12);
			}

			menu.show(manager, point.x, point.y);
		} else {
			int[] selRows = manager.getSelectionRows();
			boolean selectionMultiple = (selRows != null && selRows.length > 1);

			if (selectionMultiple) {
				JMenuItem itemEvalMulti = new JMenuItem(LABEL("menuViewEvaluations"));
				itemEvalMulti.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_EVALUATIONS));
				menu.add(itemEvalMulti);
				menu.add(new JSeparator());

				JMenuItem itemDelMulti = new JMenuItem(LABEL("Delete_multiple_files"));
				itemDelMulti.addActionListener(ev -> manager.delete_multiple_files());
				menu.add(itemDelMulti);

				menu.show(manager, point.x, point.y);
			} else {
				FileDescr fd = manager.getFileDescr(selectedNode);
				if (fd == null) return;
				String filename = fd.getFilename();

				if (fd.isDirectory()) {
					JMenuItem itemEvalFolder = new JMenuItem(LABEL("menuViewEvaluations"));
					itemEvalFolder.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_EVALUATIONS));
					menu.add(itemEvalFolder);
					menu.add(new JSeparator());

					if (!codelab.isConnected()) {
						JCheckBoxMenuItem itemUseAi = new JCheckBoxMenuItem(LABEL("evalUseAiCheckbox"), SessionEvaluator.isUseAi());
						itemUseAi.addItemListener(e -> SessionEvaluator.setUseAi(itemUseAi.getState()));
						menu.add(itemUseAi);
						JMenu menuEvalFolder = new JMenu(LABEL("menuEvalFolder"));
						SessionEvaluator.populateSkillsSubmenu(menuEvalFolder, (skill, useAi) -> SessionEvaluator.evaluateFolder(codelab, fd.getFile(), skill, useAi));
						menu.add(menuEvalFolder);
						menu.add(new JSeparator());
					}

					JMenuItem itemNewProg = new JMenuItem(String.format(LABEL("New_program"), filename));
					itemNewProg.addActionListener(ev -> manager.new_file());
					menu.add(itemNewProg);

					JMenuItem itemNewFolder = new JMenuItem(String.format(LABEL("New_folder"), filename));
					itemNewFolder.addActionListener(ev -> manager.new_folder());
					menu.add(itemNewFolder);
					menu.add(new JSeparator());

					JMenuItem itemRenFolder = new JMenuItem(String.format(LABEL("Rename_folder"), filename));
					itemRenFolder.addActionListener(ev -> manager.rename_folder());
					menu.add(itemRenFolder);

					JMenuItem itemDelFolder = new JMenuItem(String.format(LABEL("Delete_folder"), filename));
					itemDelFolder.addActionListener(ev -> manager.delete_folder());
					menu.add(itemDelFolder);

					JMenuItem itemDownFolder = new JMenuItem(String.format(LABEL("Download_folder"), filename));
					itemDownFolder.addActionListener(ev -> manager.download_folder());
					menu.add(itemDownFolder);
					menu.add(new JSeparator());

					addNavigationControls(menu, false);
					menu.show(manager, point.x, point.y);
				} else if (fd.isFile()) {
					JMenuItem itemEvalFile = new JMenuItem(LABEL("menuViewEvaluations"));
					itemEvalFile.addActionListener(ev -> editor.setExplorerMode(FileManager.MODE_EVALUATIONS));
					menu.add(itemEvalFile);
					menu.add(new JSeparator());

					if (filename.toLowerCase().endsWith(".ipynb")) {
						boolean isC = JupyterUtils.isCNotebook(fd.getFile());
						JMenuItem itemExport = new JMenuItem(isC ? LABEL("Export_notebook_c") : LABEL("Export_notebook_py"));
						itemExport.setIcon(isC ? FileDescr.icon_c : FileDescr.icon_python);
						itemExport.addActionListener(ev -> exportNotebookToScript(fd.getFile(), isC));
						menu.add(itemExport);
						menu.add(new JSeparator());
					}

					JMenuItem itemRen = new JMenuItem(String.format(LABEL("Rename_program"), filename));
					itemRen.addActionListener(ev -> manager.rename_file());
					menu.add(itemRen);

					JMenuItem itemDel = new JMenuItem(String.format(LABEL("Delete_program"), filename));
					itemDel.addActionListener(ev -> manager.delete_file());
					menu.add(itemDel);

					JMenuItem itemDup = new JMenuItem(String.format(LABEL("Duplicate_program"), filename));
					itemDup.addActionListener(ev -> manager.duplicate_file());
					menu.add(itemDup);

					JMenuItem itemBak = new JMenuItem(String.format(LABEL("Backup_program"), filename));
					itemBak.addActionListener(ev -> manager.backup_file());
					menu.add(itemBak);

					// Sous-menu backups locaux
					List<File> backups = BackupManager.backupsList(fd.getFile());
					if (backups != null && !backups.isEmpty()) {
						JMenu menuBak = new JMenu(String.format(LABEL("Backups"), filename));
						for (File bFile : backups) {
							long size = MyFileUtils.getSize(bFile);
							String date = BackupManager.getDateFormat(bFile);
							String txt = size < 1024 ?
								String.format(LABEL("Restore_backup_1"), date, size) :
								String.format(LABEL("Restore_backup_2"), date, size / 1024);
							JMenuItem itemR = new JMenuItem(txt);
							itemR.addActionListener(ev -> manager.restore_backup(bFile, date));
							menuBak.add(itemR);
						}
						menu.add(menuBak);
					} else {
						JMenuItem itemNoBak = new JMenuItem(LABEL("No_backup"));
						itemNoBak.setEnabled(false);
						menu.add(itemNoBak);
					}

					menu.add(new JSeparator());
					addNavigationControls(menu, false);
					menu.show(manager, point.x, point.y);
				}
			}
		}
	}

	/**
	 * Contrôles de navigation : Recharger, Développer tout, Replier tout.
	 */
	private void addNavigationControls(JPopupMenu menu, boolean isEvalMode) {
		JMenuItem itemRef = new JMenuItem(LABEL("Refresh_manager"));
		JMenuItem itemExp = new JMenuItem(LABEL("Expand_manager"));
		JMenuItem itemCol = new JMenuItem(LABEL("Collapse_manager"));

		if (isEvalMode) {
			itemRef.addActionListener(ev -> { manager.populate(); manager.collapseAll(); });
		} else {
			itemRef.addActionListener(ev -> { manager.populate(); editor.reset(); });
		}
		itemExp.addActionListener(ev -> manager.expandAll());
		itemCol.addActionListener(ev -> manager.collapseAll());

		menu.add(itemRef);
		menu.add(itemExp);
		menu.add(itemCol);
	}

	// -----------------------------------------------------------
	// Actions utilitaires de suppression d'évaluation
	// -----------------------------------------------------------

	private void deleteAllEvaluations() {
		int resp = JOptionPane.showConfirmDialog(manager,
			LABEL("confirmDeleteAllEvaluations"),
			LABEL("itemDeleteAllEvaluations"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (resp == JOptionPane.YES_OPTION) {
			try {
				File evalDir = new File(CodeLab.EVALUATIONS_FOLDER);
				if (evalDir.exists()) {
					FileUtils.cleanDirectory(evalDir);
				}
				if (editor.getCurrentFile() != null && FileManager.isEvaluation(editor.getCurrentFile())) {
					editor.closeCurrentEditor(codelab.isTutor() ? 2 : 0);
				}
				manager.populate();
				manager.collapseAll();
			} catch (Exception ex) {
				Utils.showWarningDialog("CodeLab", "Erreur lors de la suppression : " + ex.getMessage());
			}
		}
	}

	private void deleteEvaluationFolder(File folder, String name) {
		int resp = JOptionPane.showConfirmDialog(manager,
			String.format(LABEL("confirmDeleteEvaluation"), name),
			LABEL("itemDeleteEvaluation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (resp == JOptionPane.YES_OPTION) {
			try {
				FileUtils.deleteDirectory(folder);
				manager.populate();
				manager.collapseAll();
			} catch (Exception ex) {
				Utils.showWarningDialog("CodeLab", "Erreur lors de la suppression : " + ex.getMessage());
			}
		}
	}

	private void deleteEvaluationFile(File file, String name) {
		int resp = JOptionPane.showConfirmDialog(manager,
			String.format(LABEL("confirmDeleteEvaluation"), name),
			LABEL("itemDeleteEvaluation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (resp == JOptionPane.YES_OPTION) {
			try {
				file.delete();
				if (editor.getCurrentFile() != null && editor.getCurrentFile().equals(file)) {
					editor.closeCurrentEditor(codelab.isTutor() ? 2 : 0);
				}
				manager.populate();
				manager.collapseAll();
			} catch (Exception ex) {
				Utils.showWarningDialog("CodeLab", "Erreur lors de la suppression : " + ex.getMessage());
			}
		}
	}

	private void exportNotebookToScript(File ipynbFile, boolean isC) {
		if (ipynbFile == null || !ipynbFile.exists()) return;

		// Sauvegarder le carnet si c'est le fichier en cours d'édition
		if (editor.getCurrentFile() != null && editor.getCurrentFile().equals(ipynbFile)) {
			editor.saveCurrentFile();
		}

		String targetExt = isC ? ".c" : ".py";
		String baseName = ipynbFile.getName();
		if (baseName.toLowerCase().endsWith(".ipynb")) {
			baseName = baseName.substring(0, baseName.length() - 6);
		}
		File targetFile = new File(ipynbFile.getParentFile(), baseName + targetExt);

		if (targetFile.exists()) {
			int resp = JOptionPane.showConfirmDialog(
				codelab,
				String.format(LABEL("Confirm_export_overwrite"), targetFile.getName()),
				LABEL("Confirm_export_title"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);
			if (resp != JOptionPane.YES_OPTION) return;
		}

		try {
			if (isC) {
				JupyterUtils.extractRunnableC(ipynbFile, targetFile);
			} else {
				JupyterUtils.extractRunnablePython(ipynbFile, targetFile);
			}
			manager.populate();
			manager.selectNodeFromFile(targetFile);
			if (codelab.isConnected()) {
				codelab.getClient().uploadFile(targetFile);
			}
			codelab.consoleLog(String.format(LABEL("Export_notebook_success"), targetFile.getName()));
		} catch (Exception ex) {
			ExceptionManager.process(ex);
		}
	}
}

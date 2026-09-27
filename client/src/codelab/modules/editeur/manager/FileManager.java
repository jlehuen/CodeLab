package codelab.modules.editeur.manager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.client.UserData;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.MyFileUtils;
import codelab.utils.ResourceUtils;
import codelab.utils.Utils;

/**
*	Classe de l'explorateur de fichiers
*	@author Jérôme Lehuen
*	@version 03/10/25
*/

public class FileManager extends JTree {

	private static final long serialVersionUID = 1L;
	private static final boolean TOOLTIPS = false;
	private static final Color VERY_LIGHT_BLUE = new Color(218,228,237);
	private static final ImageIcon icon_folder_1 = ResourceUtils.loadImageIcon("icons/mini_icon_folder.png");
	private static final ImageIcon icon_folder_2 = ResourceUtils.loadImageIcon("icons/mini_icon_folder_closed.png");

	private CodeLab codelab;
	private ModuleEditor editor;
	private String base; // Dossier de départ
	private JTree self; // Référence pour les listeners

	private String rootPath; // Construit à partir de rootNode
	private DefaultMutableTreeNode rootNode; // Le noeud racine
	private DefaultMutableTreeNode selectedNode; // Le noeud sélectionné

	private JPopupMenu menu_root, menu_file, menu_folder;
	private JMenuItem itemF0, itemF1, itemF2, itemF3, itemF4, itemF5;
	private JMenuItem item3, item4, item5, item6, item7, item8, item9, item12;
	private JMenuItem item10a, item11a, item10b, item11b, item10c, item11c;

	// Extensions autorisées et exclusions
	private List<String> folders_to_exclude = Arrays.asList("__pycache__");
	private List<String> files_to_exclude = Arrays.asList("__TODO__", "__temp.c");
	private List<String> extensions = Arrays.asList(
		".c", ".h", ".go", ".hs", ".py", ".java", ".pde", ".clp", ".blocs", ".npy",
		".png", ".jpg", ".gif", ".txt", ".csv", ".log", ".html", ".css");

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public FileManager(ModuleEditor editor, CodeLab codelab) {
		this.editor = editor;
		this.codelab = codelab;
		this.self = this;
		setRootVisible(false);
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setTransferHandler(new FileTransferHandler(this));
		setFocusable(false);
		setBase(codelab.getProgramDir()); // Base par défaut
		populate();

		if (TOOLTIPS) javax.swing.ToolTipManager.sharedInstance().registerComponent(this);

		// -----------------------------------------------------------
		// Le listener de changement du JTree
		// -----------------------------------------------------------

		addTreeSelectionListener(new TreeSelectionListener() {

			public void valueChanged(TreeSelectionEvent e) {
				if (refreshFlag) return; // En cours de rafraichissement
				if (!e.isAddedPath()) return; // Ignorer les désélections (ex: resetSelectedNode)
				selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				if (selectedNode == null) return;
				FileDescr fd = (FileDescr) selectedNode.getUserObject();
				if (fd == null) return;

				if (fd.isFile()) {
					editor.change_file(selectedNode);
					if (!isClone(fd.getFile())) updateEditedFile(); // Information vers les tuteurs connectés
					// Pour récupérer le fichier distant en mode tuteur connecté (seulement si pas un clone)
					if (codelab.isConnected() && codelab.isTutor() && !isClone(fd.getFile())) {
						UserData udata = editor.getUserTable().getSelectedUser();
						if (udata.isConnected()) {
							codelab.getClient().requestFile(fd.getFile());
						}
					}
				}
			}
		});

		// -----------------------------------------------------------
		// Le listener de souris pour le menu contextuel
		// -----------------------------------------------------------

		addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {

				Point point = new Point(e.getX(), e.getY());
				boolean isLeftMouseButton = SwingUtilities.isLeftMouseButton(e);
				boolean isRightMouseButton = SwingUtilities.isRightMouseButton(e);
				boolean menu_allowed = !codelab.isTutor() && !codelab.isAdmin() && !editor.controlled;

				TreePath path = getPathForLocation(point.x, point.y);
				boolean selection_multiple = (getSelectionRows().length > 1);

				// -----------------------------------------------------------
				// Clic sur le bouton gauche
				// -----------------------------------------------------------

				if (isLeftMouseButton) {

					if (path == null) {
						clearSelection(); // On déselectionne dans le JTree
						selectedNode = null; // On annule la référence mémorisée
					}
					else if (selectedNode != null) {

						FileDescr fd = (FileDescr) selectedNode.getUserObject();
						boolean isFile = fd.isFile();

						fd.setModified(false); // Pour enlever la coloration rouge
						updateTreeAfterSelected(); // Pour actualiser la coloration

						// Double-clic pour exécution (si exécutable)
						if (e.getClickCount() == 2 && !e.isConsumed()) {
							if (isFile && editor.isExecutable()) {
								e.consume();
								codelab.getToolbar().action_run(); // Exécuter le programme
								codelab.getConsole().focus(); // Activer la console
							}
						}
					}
				}

				// -----------------------------------------------------------
				// Clic sur le bouton droit
				// -----------------------------------------------------------

				else if (isRightMouseButton) {

					if (path != null) {
						if (!isPathSelected(path)) {
							setSelectionPath(path);
						}
						selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
					}

					if (codelab.isTutor() || codelab.isAdmin()) {
						if (selectedNode == null) {
							JPopupMenu menu = new JPopupMenu();
							JMenuItem itemRef = new JMenuItem(LABEL("Refresh_manager"));
							JMenuItem itemExp = new JMenuItem(LABEL("Expand_manager"));
							itemRef.addActionListener(ev -> { populate(); editor.reset(); });
							itemExp.addActionListener(ev -> expandAll());
							menu.add(itemRef);
							menu.add(itemExp);
							menu.show(self, point.x, point.y);
						} else {
							FileDescr fd = (FileDescr) selectedNode.getUserObject();
							if (fd.isDirectory()) {
								JPopupMenu menu = new JPopupMenu();
								JMenuItem itemRef = new JMenuItem(LABEL("Refresh_manager"));
								JMenuItem itemExp = new JMenuItem(LABEL("Expand_manager"));
								itemRef.addActionListener(ev -> { populate(); editor.reset(); });
								itemExp.addActionListener(ev -> expandAll());
								menu.add(itemRef);
								menu.add(itemExp);
								menu.show(self, point.x, point.y);
							} else if (fd.isFile()) {
								JPopupMenu menu = new JPopupMenu();
								if (isClone(fd.getFile())) {
									JMenuItem itemDel = new JMenuItem(String.format(LABEL("Delete_clone"), fd.getFilename()));
									itemDel.addActionListener(ev -> delete_clone(selectedNode, fd));
									menu.add(itemDel);
								} else {
									JMenuItem itemCln = new JMenuItem(String.format(LABEL("Clone_program_file"), fd.getFilename()));
									itemCln.addActionListener(ev -> clone_file(fd, selectedNode));
									menu.add(itemCln);
								}
								menu.add(new JSeparator());
								JMenuItem itemRef = new JMenuItem(LABEL("Refresh_manager"));
								JMenuItem itemExp = new JMenuItem(LABEL("Expand_manager"));
								itemRef.addActionListener(ev -> { populate(); editor.reset(); });
								itemExp.addActionListener(ev -> expandAll());
								menu.add(itemRef);
								menu.add(itemExp);
								menu.show(self, point.x, point.y);
							}
						}
					}

					else if (menu_allowed) {

						if (selectedNode == null) menu_root.show(self, point.x, point.y);

						else {

						FileDescr fd = (FileDescr) selectedNode.getUserObject();
						String filename = fd.getFilename();
						boolean isDirectory = fd.isDirectory();
						boolean isFile = fd.isFile();

						if (selection_multiple) {

							menu_file = new JPopupMenu();
							menu_file.add(itemF5 = new JMenuItem(LABEL("Delete_multiple_files")));
							itemF5.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { delete_multiple_files(); }});
							menu_file.show(self, point.x, point.y);

						} else if (isDirectory) {

							item3.setText(String.format(LABEL("New_program"), filename));
							item4.setText(String.format(LABEL("New_folder"), filename));
							item5.setText(String.format(LABEL("Rename_folder"), filename));
							item6.setText(String.format(LABEL("Delete_folder"), filename));
							item7.setText(String.format(LABEL("Download_folder"), filename));
							menu_folder.show(self, point.x, point.y);

						} else if (isFile) {

							menu_file = new JPopupMenu();
							menu_file.add(itemF0 = new JMenuItem());
							menu_file.add(itemF1 = new JMenuItem());
							menu_file.add(itemF2 = new JMenuItem());
							menu_file.add(itemF3 = new JMenuItem());

							itemF0.setText(String.format(LABEL("Rename_program"), filename));
							itemF1.setText(String.format(LABEL("Delete_program"), filename));
							itemF2.setText(String.format(LABEL("Duplicate_program"), filename));
							itemF3.setText(String.format(LABEL("Backup_program"), filename));

							itemF0.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { rename_file(); }});
							itemF1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { delete_file(); }});
							itemF2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicate_file(); }});
							itemF3.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { backup_file(); }});

							// Sous-menu backups locaux
							boolean backups_avalable = false;
							JMenu menuBak = new JMenu(String.format(LABEL("Backups"), filename));
							for (File file : BackupManager.backupsList(fd.getFile())) {
								long size = MyFileUtils.getSize(file);
								String date = BackupManager.getDateFormat(file);
								String txt = size < 1024 ?
									String.format(LABEL("Restore_backup_1"), date, size) :
									String.format(LABEL("Restore_backup_2"), date, size / 1024);
								JMenuItem item = new JMenuItem(txt);
								item.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										restore_backup(file, date);
									}
								});
								menuBak.add(item);
								backups_avalable = true;
							}
							if (backups_avalable) {
								menu_file.add(menuBak);
							} else {
								itemF4 = new JMenuItem(LABEL("No_backup"));
								itemF4.setEnabled(false);
								menu_file.add(itemF4);
							}

							menu_file.add(new JSeparator());
							menu_file.add(item11a);
							menu_file.add(item10a);
							menu_file.show(self, point.x, point.y);
						}
					}
				}
			}
		}
	});

		// -----------------------------------------------------------
		// Les menus contextuels et leurs méthodes
		// -----------------------------------------------------------

		menu_folder = new JPopupMenu();
		menu_folder.add(item3 = new JMenuItem());
		menu_folder.add(item4 = new JMenuItem());
		menu_folder.add(new JSeparator());
		menu_folder.add(item5 = new JMenuItem());
		menu_folder.add(item6 = new JMenuItem());
		menu_folder.add(item7 = new JMenuItem());
		menu_folder.add(new JSeparator());
		menu_folder.add(item11b = new JMenuItem(LABEL("Refresh_manager")));
		menu_folder.add(item10b = new JMenuItem(LABEL("Expand_manager")));

		menu_root = new JPopupMenu();
		menu_root.add(item8 = new JMenuItem(LABEL("New_root_program")));
		menu_root.add(item9 = new JMenuItem(LABEL("New_root_folder")));
		menu_root.add(new JSeparator());
		menu_root.add(item11c = new JMenuItem(LABEL("Refresh_manager")));
		menu_root.add(item10c = new JMenuItem(LABEL("Expand_manager")));

		if (!codelab.isConnected()) {
			// Item pour récupérer la dernière session distante
			menu_root.add(item12 = new JMenuItem(LABEL("Copy_dist_folder")));
			item12.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { copyDist(); }});
		}

		item11a = new JMenuItem(LABEL("Refresh_manager"));
		item10a = new JMenuItem(LABEL("Expand_manager"));

		item3.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { new_file(); }});
		item4.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { new_folder(); }});
		item5.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { rename_folder(); }});
		item6.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { delete_folder(); }});
		item7.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { download_folder(); }});
		item8.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { new_root_file(); }});
		item9.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { new_root_folder(); }});
		item10a.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { expandAll(); }});
		item10b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { expandAll(); }});
		item10c.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { expandAll(); }});
		item11a.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { populate(); editor.reset(); }});
		item11b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { populate(); editor.reset(); }});
		item11c.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { populate(); editor.reset(); }});
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public void setBase(String base) {
		this.base = base;
	}

	public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultMutableTreeNode getSelectedNode() {
		return selectedNode;
	}

	public boolean isSelected() {
		return (selectedNode != null);
	}

	public String getCurrentDirectory() {
		FileDescr fd = selectedFileDescr();
		return (fd == null) ? null : fd.getDirectory();
	}

	///////////////////////////////////////////////////
	// Quelques méthodes privées
	///////////////////////////////////////////////////

	private String LABEL(String key) {
		return CodeLab.LABEL(key);
	}

	private void updateCurrentNode() {
		// Pour actualiser le noeud courant après un renommage
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		((DefaultTreeModel)getModel()).nodeChanged(node);
	}

	private void removeCurrentNode() {
		// Pour détacher et suprimer le noeud courant
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
		((DefaultTreeModel)getModel()).removeNodeFromParent(node);
	}

	private FileDescr getFileDescr(DefaultMutableTreeNode node) {
		// Retourne le FileDescr d'un noeud passé en paramètre
		return (FileDescr) node.getUserObject();
	}

	private FileDescr selectedFileDescr() {
		// Retourne le FileDescr du noeud courant ou bien null
		return (selectedNode == null) ? null : getFileDescr(selectedNode);
	}

	private boolean exists(String filename) {
		String dir = getCurrentDirectory();
		String path = dir + File.separator + filename;
		File file = new File(path);
		return file.exists();
	}

	private boolean isExpanded(DefaultMutableTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		return isExpanded(path);
	}

	private boolean populate2() {
		// Méthode privée invoquée par populate()
		File file = new File(base);
		if (file.exists()) {
			setModel(new DefaultTreeModel(addNodes(null, file), true));
			setCellRenderer(new CodeLabTreeRenderer());
			rootNode = (DefaultMutableTreeNode) getModel().getRoot();
			File rootFile = ((FileDescr) rootNode.getUserObject()).getFile();
			rootPath = rootFile.getAbsoluteFile().toString();
			selectedNode = null;
			return true;
		}
		else return false;
	}

	private DefaultMutableTreeNode addNodes(DefaultMutableTreeNode top, File dir) {
		// Pour ajouter des noeuds à top à partir du répertoire dir -> utilisé par populate()

		// Accrochage du répertoire courant
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileDescr(dir));
		if (top != null) top.add(node);

		// Collecte et tri du contenu du répertoire
		File[] files = dir.listFiles();
		Arrays.sort(files);

		// Première passe pour les répertoires
		for (File file : files) {
			if (file.isDirectory() && !folders_to_exclude.contains(file.getName()))
				addNodes(node, file);
		}

		// Deuxième passe pour les fichiers
		for (File file : files) {
			String filename = file.getName();
			String ext = MyFileUtils.getExtension(filename);
			if (file.isFile() && extensions.contains(ext)) {
				if (files_to_exclude.contains(filename)) continue;
				if (filename.endsWith(".blocs.py")) continue;
				if (filename.startsWith(".")) continue;
				node.add(new DefaultMutableTreeNode(new FileDescr(file), false));
			}
		}
		return node;
	}

	public DefaultMutableTreeNode searchNode_file(File file) {
		// Recherche un noeud à partir d'un fichier
		Enumeration<TreeNode> enumeration = rootNode.breadthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
			File file2 = getFileDescr(node).getFile();
			if (file.equals(file2)) return node;
		}
		return null;
	}

	private DefaultMutableTreeNode searchNode_content(File file) {
		// Recherche un noeud à partir d'un fichier référence qui doit avoir
		// le même nom et le même contenu que le fichier associé à ce noeud
		Enumeration<TreeNode> enumeration = rootNode.breadthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
			FileDescr fd = getFileDescr(node);
			if (fd.isFile()
				&& fd.getFilename().equals(file.getName())
				&& Utils.sameContent(fd.getFile(), file))
				return node;
		}
		return null;
	}

	private void updateTreeAfterSelected() {
		// Pour actualiser les flags de modification des dossiers après une sélection dans le tree
		Enumeration<TreeNode> enumeration1 = rootNode.breadthFirstEnumeration();
		while (enumeration1.hasMoreElements()) {
			// Pour tous les dossiers
			DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) enumeration1.nextElement();
			FileDescr fd1 = getFileDescr(node1);
			if (fd1.isDirectory()) {
				fd1.setModified(false);
				Enumeration<TreeNode> enumeration2 = node1.breadthFirstEnumeration();
				while (enumeration2.hasMoreElements()) {
					// Pour tous les fichiers descendants
					DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) enumeration2.nextElement();
					FileDescr fd2 = getFileDescr(node2);
					if (fd2.isFile()
						&& fd2.isModified()
						&& fd2.getPath().toString().contains(fd1.getPath().toString()))
						fd1.setModified(true);
				}
			}
		}
	}

	private String getPath(DefaultMutableTreeNode node) {
		// Pour reconstruire le path complet d'un node
		FileDescr descr = (FileDescr) node.getUserObject();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
		if (parent == null) return "";
		String path = String.format("%s%s%s", getPath(parent), File.separator, descr.getFilename());
		return path.charAt(0) == '/' ? path : '/' + path;
	}

	private void expandAndSelectNode(DefaultMutableTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
			expandPath(new TreePath(parent.getPath()));
			TreeNode[] nodes = ((DefaultTreeModel) getModel()).getPathToRoot(node);
			TreePath tpath = new TreePath(nodes);
			scrollPathToVisible(tpath);
			setSelectionPath(tpath);
		});
	}

	private void updateEditedFile() {
		FileDescr fd = selectedFileDescr();
		// Pour transmettre au serveur le fichier en cours d'édition
		if (codelab.isConnected() && (fd != null) && fd.isFile()) {
			String path = fd.getFile().getPath().replaceAll("\\\\", "/"); // Normalisation des séparateurs sous Windows
			String name = path.substring(path.lastIndexOf("/dist/") + 5);
			codelab.getClient().setEditedFile(name);
		}
	}

	private boolean consolidate(File dir) {
		// Pour détecter des nouveaux fichiers créés pendant une exécution
		boolean fileFound = false;
		File[] files = dir.listFiles();
		Arrays.sort(files);
		for (File file : files) {
			String filename = file.getName();
			String ext = MyFileUtils.getExtension(filename);
			if (file.isFile() && extensions.contains(ext)) {
				if (files_to_exclude.contains(filename)) continue;
				if (filename.endsWith(".blocs.py")) continue;
				if (filename.startsWith(".")) continue;
				if (searchNode_file(file) == null) {
					// Affichage dans la console
					String name = file.toString().substring(base.length() + 1);
					CodeLab.INSTANCE.consoleLog(String.format(CodeLab.LABEL("Newfile_detected"), name));
					// Création et accrochage d'un nouveau noeud
					File parent = new File(file.getParent());
					DefaultMutableTreeNode node = searchNode_file(parent);
					node.add(new DefaultMutableTreeNode(new FileDescr(file), false));
					// Mise à jour de la vue
					invokeLater_updateUI();
					fileFound = true;
				}
			}
			// Appel récursif vers les sous-dossiers autorisés
			else if (file.isDirectory() && !folders_to_exclude.contains(file.getName()))
				fileFound = fileFound || consolidate(file);
		}
		return fileFound;
	}

	///////////////////////////////////////////////////
	// Classe interne du renderer
	///////////////////////////////////////////////////

	private class CodeLabTreeRenderer extends DefaultTreeCellRenderer {

		private static final String SPAN_FORMAT = "<html><span style='color:%s;'>%s</span></html>";

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

				if (node.getUserObject() instanceof FileDescr) {
					FileDescr fd = (FileDescr) node.getUserObject();
					String filename = fd.getFilename();

					setToolTipText(fd.getInfo()); // Info-bulle

					// Affectation de l'icone du node
					if (CodeLab.FILE_MANAGER_ICONS) {
						if (fd.isDirectory())
							setIcon(isExpanded(node) ? icon_folder_1 : icon_folder_2);
						else if (fd.getIcon() != null) setIcon(fd.getIcon());
					}

					if (selected) setBackground(VERY_LIGHT_BLUE); // Personnalisation du fond de sélection

					// Mise en évidence du fichier en cours d'édition (pour les tuteurs)
					if (editor.getUserTable() != null) {

						// Path au niveau du noeud avec le login -> [login/chemin]
						String path = fd.getFile().toString();
						path = path.replaceAll("\\\\", "/"); // Normalisation des séparateurs sous Windows
						String name = path.substring(path.lastIndexOf("/dist/") + 6);

						// Path du fichier édité par le user sélectionné -> [login/chemin]
						UserData udata = editor.getUserTable().getSelectedUser();
						if (udata == null) return comp; // Protection défensive si aucune donnée utilisateur sélectionnée

						String edited = udata.getLogin() + udata.getEditedFile();
						edited = edited.replaceAll("\\\\", "/"); // Normalisation des séparateurs sous Windows

						// Comparaison des 2 chemins et visualisation du fichier édité
						if (name.equals(edited)) setText(String.format(SPAN_FORMAT, "red", filename));
					}

					// Mise en évidence des fichiers clones (bac à sable tuteur)
					if (isClone(fd.getFile())) {
						setText(String.format("<html><span style='color:#0066CC;'><b>%s</b></span></html>", filename));
						setToolTipText(LABEL("Clone_program"));
					}

					// En cas de modification du fichier par un tuteur (pour les students)
					if (isModified_rec(node)) setText(String.format(SPAN_FORMAT, "red", filename));
				}
			}
			return comp;
		}

		private boolean isModified_rec(DefaultMutableTreeNode node) {
			// Pour savoir si le noeud (ou sa descendance) a été modifié
			if (getFileDescr(node).isModified()) return true;
			// Itérer sur les noeuds fils
			Enumeration<TreeNode> children = node.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
				FileDescr fd = getFileDescr(child);
				if (fd.isFile() && fd.isModified()) return true;
				if (fd.isDirectory()) return isModified_rec(child);
			}
			return false;
		}
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	private boolean refreshFlag = false;

	public synchronized void populate() {
		// Synchronized car invoqué à de multiples endroits
		// Y compris depuis le serveur
		refreshFlag = true; // Inhiber le TreeSelectionListener
		if (!populate2()) CodeLab.logger(String.format("ERROR while scanning programs (%s)", base));
		refreshFlag = false;
	}

	public boolean consolidate() {
		// Invoqué après une exécution
		// Pour détecter des nouveaux fichiers créés par programme
		refreshFlag = true; // Inhiber le TreeSelectionListener
		boolean fileFound = consolidate(new File(base));
		refreshFlag = false;
		return fileFound;
	}

	public void expandAll() {
		for (int i = 0; i < getRowCount(); i++) {
			try { expandRow(i); }
			catch (Exception e) {
				ExceptionManager.process(e); // C'est déjà arrivé
			}
		}
	}

	public void clear() {
		setModel(null);
		setCellRenderer(null);
	}

	public void resetSelectedNode() {
		clearSelection();
		selectedNode = null;
	}

	public void invokeLater_updateUI() {
		 // Un simple repaint() ne redimentionne pas la largeur des labels
		SwingUtilities.invokeLater(() -> {
			updateUI();
		});
	}

	public void setTransferHandlerCapability(boolean transferable) {
		if (transferable) setTransferHandler(new FileTransferHandler(this));
		else setTransferHandler(null);
	}

	public DefaultMutableTreeNode reloadFile(File temp) {
		// Rechercher un noeud sur la base d'un fichier
		DefaultMutableTreeNode node = searchNode_content(temp);
		if (node == null) return null;
		// Sélectionner le noeud identifié
		expandAndSelectNode(node);
		// Mettre à jour les données courantes
		selectedNode = node;
		return node;
	}

	public void updateAllFileDescr() {
		// Pour actualiser tous les FileDescr du JTree
		Enumeration<TreeNode> enumeration = rootNode.breadthFirstEnumeration();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
			FileDescr fd = getFileDescr(node);
			fd.setFile(new File(codelab.getProgramDir() + getPath(node)));
		}
	}

	public boolean selectNodeFromFile(File file) {
		DefaultMutableTreeNode node = searchNode_file(file);
		if (node == null) return false;
		// Sélectionner le noeud identifié
		expandAndSelectNode(node);
		return true;
	}

	public void new_droppedFile(File file, DefaultMutableTreeNode parent) {
		if (parent == null) parent = rootNode;
		// Nom et extension du fichier à importer
		String filename = MyFileUtils.getFilename(file).replace(' ', '_');
		String ext = MyFileUtils.getExtension(filename);
		// Emplacement de la destination
		String parentPath = getFileDescr(parent).getFile().toString();
		File newfile = new File(parentPath + File.separator + filename);
		// Fichier ou dossier ?
		if (file.isFile() && extensions.contains(ext)) {
			codelab.consoleLog(String.format(CodeLab.LABEL("Loading_file"), file));
			if(_NEW_FILE_(newfile, file, parent) == null) return;
			// Uploader le fichier si possible
			if (codelab.isConnected()) {
				// Vérifier la taille du fichier
				long size_Ko = file.length() / 1024;
				long max = CodeLab.MAX_FILE_SIZE * 1024;
				if (file.length() > max) {
					Utils.showMessageDialog(
						LABEL("WARNING"),
						String.format(LABEL("UPLOAD_ERROR"), size_Ko, CodeLab.MAX_FILE_SIZE));
					return;
				}
				else codelab.getClient().uploadFile(newfile);
			}
		}
		else if (file.isDirectory()) {
			DefaultMutableTreeNode newParent = _NEW_FOLDER_(newfile, parent);
			// Appels récursifs sur les fichiers du dossier
			File[] files = file.listFiles();
			Arrays.sort(files);
			for (File f : files) new_droppedFile(f, newParent);
		}
	}

	///////////////////////////////////////////////////
	// Actions du file manager
	///////////////////////////////////////////////////

	private interface Statements {
		Object execute();
	}

	// Pour exécuter un bloc en mode safe

	private Object safeExecute(Statements bloc) {
		editor.pauseTimerTasks(); // Stopper les tâches répétitives
		Object value = bloc.execute(); // Exécuter le bloc d'instructions
		editor.resumeTimerTasks(); // Reprendre les tâches répétitives
		return value;
	}

	// -----------------------------------------------------------
	// Action nouveau fichier
	// -----------------------------------------------------------

	public void new_file() {
		// Déterminer le node parent
		DefaultMutableTreeNode parent;
		String parentPath;
		FileDescr fd = selectedFileDescr();
		if (fd == null) {
			parent = rootNode;
			parentPath = rootPath;
		} else if (fd.isDirectory()) {
			parent = selectedNode;
			parentPath = fd.getFile().toString();
		} else {
			parent = (DefaultMutableTreeNode) selectedNode.getParent();
			parentPath = getFileDescr(parent).getFile().toString();
		}
		// Demander le nom du fichier
		NewFileDialog dialog = new NewFileDialog(parentPath);
		String filename = dialog.getFilename();
		File template = dialog.getTemplate();
		if (filename == null) return; // Annulation

		filename = MyFileUtils.normaliseFilename(filename);
		File newfile = new File(parentPath + File.separator + filename);
		if (_NEW_FILE_(newfile, template, parent) == null) return;
		codelab.getClient().uploadFile(newfile); // Si mode connecté
	}

	private void new_root_file() {
		NewFileDialog dialog = new NewFileDialog(rootPath);
		String filename = dialog.getFilename();
		File template = dialog.getTemplate();
		if (filename == null) return; // Annulation

		filename = MyFileUtils.normaliseFilename(filename);
		File newfile = new File(rootPath + File.separator + filename);
		if (_NEW_FILE_(newfile, template, rootNode) == null) return;
		codelab.getClient().uploadFile(newfile); // Si mode connecté
	}

	private DefaultMutableTreeNode _NEW_FILE_(File newfile, File template, DefaultMutableTreeNode parent) {
		Statements bloc = new Statements() {
			public Object execute() {

				// Vérifier l'existance du fichier
				if (newfile.exists()) {
					Utils.showMessageDialog(LABEL("WARNING"),
						String.format(LABEL("Filename_exists"), newfile.getName()));
					return null;
				}
				// Nouveau descripteur
				FileDescr newFD = new FileDescr(newfile);
				// Insertion du nouveau noeud
				DefaultMutableTreeNode newnode = new DefaultMutableTreeNode(newFD, false); // Pas de fils
				DefaultTreeModel model = (DefaultTreeModel) getModel();
				model.insertNodeInto(newnode, parent, parent.getChildCount());
				// Sélectionner le nouveau noeud
				setExpandsSelectedPaths(true);
				setSelectionPath(new TreePath(newnode.getPath()));
				// Ajout dans l'éditeur + template
				editor.new_file(newnode, template);
				// Notifications vers le serveur MEPA
				long max = CodeLab.MAX_FILE_SIZE * 1024;
				if (newfile.length() <= max) {
					editor.newFileDist(newfile);
					updateEditedFile();
				}
				return newnode;
			}
		};
		Object value = safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
		return (DefaultMutableTreeNode) value;
	}

	// -----------------------------------------------------------
	// Action nouveau dossier
	// -----------------------------------------------------------

	private void new_folder() {
		NewFolderDialog dialog = new NewFolderDialog();
		String filename = dialog.getName();
		if (filename == null) return; // Annulation

		filename = MyFileUtils.normaliseFilename(filename);
		FileDescr selected = selectedFileDescr();
		File newfile = new File(selected.getFile() + File.separator + filename);
		_NEW_FOLDER_(newfile, selectedNode);
	}

	private void new_root_folder() {
		NewFolderDialog dialog = new NewFolderDialog();
		String filename = dialog.getName();
		if (filename == null) return; // Annulation

		filename = MyFileUtils.normaliseFilename(filename);
		File newfile = new File(rootPath + File.separator + filename);
		_NEW_FOLDER_(newfile, rootNode);
	}

	private DefaultMutableTreeNode _NEW_FOLDER_(File newfile, DefaultMutableTreeNode parent) {
		Statements bloc = new Statements() {
			public Object execute() {

				// Vérifier l'existance du fichier
				if (newfile.exists()) {
					Utils.showMessageDialog(LABEL("WARNING"),
						String.format(LABEL("Filename_exists"), newfile.getName()));
					return null;
				}
				// Nouveau descripteur
				FileDescr newFD = new FileDescr(newfile);
				// Insertion du nouveau noeud
				DefaultMutableTreeNode newnode = new DefaultMutableTreeNode(newFD);
				DefaultTreeModel model = (DefaultTreeModel) getModel();
				model.insertNodeInto(newnode, parent, parent.getChildCount());
				// Sélectionner le nouveau noeud
				setExpandsSelectedPaths(true);
				setSelectionPath(new TreePath(newnode.getPath()));
				// Créer le dossier
				newfile.mkdir();
				// Notification vers le serveur MEPA
				editor.createFolderDist(newfile);
				return newnode;
			}
		};
		Object value = safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
		return (DefaultMutableTreeNode) value;
	}

	// -----------------------------------------------------------
	// Action renommer un fichier
	// -----------------------------------------------------------

	private void rename_file() {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr fd = selectedFileDescr();
				String ext = fd.getExtension();
				String filename = (String) JOptionPane.showInputDialog(CodeLab.FRAME,
						LABEL("Enter_new_filename"), LABEL("Rename_file_title"),
						JOptionPane.PLAIN_MESSAGE, null, null, MyFileUtils.noExtension(fd.toString()));
				if ((filename == null) || (filename.length() == 0)) return false;

				filename = MyFileUtils.normaliseFilename(filename);
				filename = filename + ext;

				if (ext.equals(".java"))
					filename = Utils.capitalize(filename);
				if (exists(filename)) {
					Utils.showMessageDialog("ATTENTION", String.format(LABEL("Filename_exists"), filename));
					return null;
				}

				// Notification préalable vers le serveur MEPA
				editor.renameFileDist(fd.getFile(), filename);

				// Renommer les backups AVANT de renommer le fichier principal
				BackupManager.renameBackups(fd.getFile(), filename);

				fd.rename(filename); // Renommage effectif
				updateCurrentNode(); // Avertir le JTree
				editor.filenameChanged(); // Avertir l'éditeur
				return true;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action renommer un dossier
	// -----------------------------------------------------------

	private void rename_folder() {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr fd = selectedFileDescr();
				if (fd == null) return false; // Par sécurité
				String filename = (String) JOptionPane.showInputDialog(CodeLab.FRAME,
						LABEL("Enter_new_folder_name"), LABEL("Rename_folder_title"),
						JOptionPane.PLAIN_MESSAGE, null, null, fd.toString());
				if ((filename == null) || (filename.length() == 0)) return false;

				// Nouveau fichier
				filename = MyFileUtils.normaliseFilename(filename);
				File newfile = new File(fd.getDirectory() + File.separator + filename);
				if (newfile.exists()) {
					Utils.showMessageDialog("ATTENTION", String.format(LABEL("Filename_exists"), filename));
					return false;
				}
				// Notification préalable vers le serveur MEPA
				editor.renameFileDist(fd.getFile(), filename);
				editor.saveCurrentFile(); // Avant le renommage
				fd.rename(filename); // Renommage effectif
				// Avertir le JTree du changement
				updateCurrentNode();
				// Pour actualiser tous les FileDescr
				updateAllFileDescr();
				return true;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action supprimer un fichier
	// -----------------------------------------------------------

	private void delete_file() {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr fd = selectedFileDescr();
				if (fd == null) return false; // Par sécurité
				int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
						String.format(LABEL("Confirm_file_del"), fd.toString()),
						LABEL("Delete_file_title"), JOptionPane.YES_NO_OPTION);

				if (reply == JOptionPane.YES_OPTION) {
					// Notification vers le serveur MEPA
					editor.deleteFileDist(selectedFileDescr().getFile());
					// Suppression dans l'éditeur
					editor.remove_file(fd.getUID());
					// Suppressions des sauvegardes locales
					for (File file : BackupManager.backupsList(fd.getFile())) file.delete();
					// Suppression effective
					fd.delete();
					// Suppression dans le JTree
					removeCurrentNode();
					selectedNode = null;
					return true;
				}
				return false;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action supprimer un dossier
	// -----------------------------------------------------------

	private void delete_folder() {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr fd = selectedFileDescr();
				if (fd == null) return false; // Par sécurité
				int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
						String.format(LABEL("Confirm_folder_del"), fd.toString()),
						LABEL("Delete_folder_title"), JOptionPane.YES_NO_OPTION);

				if (reply == JOptionPane.YES_OPTION) {
					// Notification préalable pour le serveur MEPA
					editor.deleteFileDist(fd.getFile());
					// Suppression effective
					fd.delete();
					// Suppression dans le JTree
					removeCurrentNode();
					selectedNode = null;
					editor.updateHashMap();
					return true;
				}
				return false;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action supprimer plusieurs noeuds
	// -----------------------------------------------------------

	private void delete_multiple_files() {
		int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
			LABEL("Confirm_multiple_del"),
			LABEL("WARNING"), JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.YES_OPTION) __DELETE_FILES_();
	}

	private void deleteNode(DefaultMutableTreeNode node) {
		((DefaultTreeModel)getModel()).removeNodeFromParent(node);
	}

	private void __DELETE_FILES_() {
		Statements bloc = new Statements() {
			public Object execute() {

				// Construire la liste des noeuds à supprimer
				List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
				TreePath[] paths = getSelectionPaths();
				for (int i = 0; i < paths.length; i++) {
					DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
					toRemove.add(next);
				}
				// Lancer la suppression des noeuds à supprimer
				for (DefaultMutableTreeNode node : toRemove) {
					FileDescr fd = getFileDescr(node);
					// Confirmation pour des dossiers
					if (fd.isDirectory()) {
						int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
							String.format(LABEL("Confirm_folder_del"), fd.toString()),
							LABEL("WARNING"), JOptionPane.YES_NO_OPTION);
						if (reply != JOptionPane.YES_OPTION) continue;
					}
					// Notification vers le serveur MEPA
					editor.deleteFileDist(fd.getFile());
					// Suppression dans l'éditeur
					if (fd.isFile()) {
						editor.remove_file(fd.getUID());
						// Suppressions des sauvegardes locales
						File parent = new File(fd.getDirectory());
						String wildcard = fd.getFilename() + ".bak%d";
						MyFileUtils.deleteFiles(parent, wildcard);
					}
					fd.delete(); // Suppression effective
					deleteNode(node); // Suppression dans le JTree
					editor.updateHashMap(); // Humm ?
				}
				return true;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action dupliquer un fichier
	// -----------------------------------------------------------

	private void duplicate_file() {
		FileDescr fd = selectedFileDescr();
		if (fd == null) return; // Par sécurité

		String filename = fd.getFilename();
		String base = MyFileUtils.noExtension(filename);
		String ext = MyFileUtils.getExtension(filename);
		File file = fd.getFile();

		int cmpt = 1;
		File newfile = null;
		while (newfile == null || newfile.exists()) {
			String newfilename = String.format("%s_copy%d%s", base, cmpt, ext);
			newfile = new File(fd.getDirectory() + File.separator + newfilename);
			cmpt++;
		}
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
		if (_NEW_FILE_(newfile, file, parent) == null) return;
		codelab.getClient().uploadFile(newfile); // Si mode connecté
	}

	// -----------------------------------------------------------
	// Action sauvegarder un fichier
	// -----------------------------------------------------------

	private void backup_file() {
		editor.saveCurrentFile();
		editor.backupCurrentFile();
	}

	// -----------------------------------------------------------
	// Action restaurer une sauvegarde
	// -----------------------------------------------------------

	private void restore_backup(File file, String date) {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr fd = selectedFileDescr();
				if (fd == null) return false; // Par sécurité

				String newfilename = MyFileUtils.noExtension(file);
				File newfile = new File(newfilename);
				//CodeLab.INSTANCE.consoleLog(newfile.toString());

				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
				if (_NEW_FILE_(newfile, file, parent) == null) return false;
				codelab.getClient().uploadFile(newfile); // Si mode connecté

				return true;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action déplacer un fichier
	// -----------------------------------------------------------

	public void move_file(DefaultMutableTreeNode sourceNode, DefaultMutableTreeNode targetNode) {
		Statements bloc = new Statements() {
			public Object execute() {

				FileDescr source = getFileDescr(sourceNode);
				FileDescr folder = getFileDescr(targetNode);
				editor.pauseTimerTasks();
				
				// Notification pour le serveur MEPA (AVANT le moveTo)
				editor.moveFileDist(source.getFile(), folder.getFile());
				
				// Déplacer les backups AVANT de déplacer le fichier principal
				if (source.isFile()) {
					BackupManager.moveBackups(source.getFile(), folder.getFile());
				}
				// Déplacement effectif du fichier ou du dossier
				source.moveTo(folder);
				return true;
			}
		};
		safeExecute(bloc); // Pour exécuter en mode safe (stopper les tâches répétitives)
		updateUI(); // Parfois utile...
	}

	// -----------------------------------------------------------
	// Action télécharger un dossier
	// -----------------------------------------------------------

	private void download_folder() {
		FileDescr fd = selectedFileDescr();
		if (fd == null) return; // Par sécurité

		// Choix de la destination
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		chooser.setDialogTitle(LABEL("Download_folder_title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(CodeLab.FRAME) != JFileChooser.APPROVE_OPTION) return;
		if (chooser.getSelectedFile() == null) return;

		// Nettoyage de printemps
		File dir = fd.getFile();
		MyFileUtils.deleteFilesRec(dir, "*.bak");
		MyFileUtils.deleteFilesRec(dir, "*.exe");
		MyFileUtils.deleteFilesRec(dir, "*.pyc");
		MyFileUtils.deleteFilesRec(dir, "*.orig");
		MyFileUtils.deleteFilesRec(dir, "*.class");
		MyFileUtils.deleteFilesRec(dir, "*.blocs.py");
		MyFileUtils.deleteFilesRec(dir, ".DS_Store");
		MyFileUtils.deleteFilesRec(dir, "__*");

		// Compression du dossier
		File source = fd.getFile();
		String zipname = fd.getFilename() + ".zip";
		String target = chooser.getSelectedFile() + "/" + zipname;
		//codelab.printlnToConsole("Zipname=" + zipname, Console.ORANGE);
		//codelab.printlnToConsole("Source=" + source, Console.ORANGE);
		//codelab.printlnToConsole("Target=" + target, Console.ORANGE);
		if (MyFileUtils.zipFolder(target, source))
			codelab.consoleLog("Folder zipped to " + target);
		else
			codelab.consoleLog("ERROR: Can't zip " + source);
	}

	// -----------------------------------------------------------
	// Action copier le dossier dist dans local
	// -----------------------------------------------------------

	public void copyDist() {
		final int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
			LABEL("Confirm_copyDist"),
			LABEL("Confirm_copyDist_title"),
			JOptionPane.YES_NO_OPTION);

		if (reply == JOptionPane.YES_OPTION) {
			int cmpt = 0;
			SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyMMdd_HHmmss");
			String date = DATEFORMAT.format(new Date());
			String destination = String.format("%s%simport_%s", CodeLab.PROG_LOCAL_FOLDER, File.separator, date);
			while (new File(destination).exists()) {
				// Générer un nouveau nom de dossier numéroté
				destination = String.format("%s%simport_%s_%d", CodeLab.PROG_LOCAL_FOLDER, File.separator, date, cmpt++);
			}
			MyFileUtils.copyFolder(CodeLab.PROG_DIST_FOLDER, destination);
			populate();
		}
	}

	// -----------------------------------------------------------
	// Gestion des clones (mode bac à sable pour tuteurs)
	// -----------------------------------------------------------

	public static final Pattern CLONE_PATTERN = Pattern.compile("^(.+?)_clone(\\d*)(\\.[^.]+)?$");

	public static boolean isClone(File file) {
		if (file == null) return false;
		return isClone(file.getName());
	}

	public static boolean isClone(FileDescr fd) {
		if (fd == null) return false;
		return isClone(fd.getFilename());
	}

	public static boolean isClone(String filename) {
		if (filename == null) return false;
		return CLONE_PATTERN.matcher(filename).matches();
	}

	public static String generateCloneFilename(File file) {
		String filename = file.getName();
		Matcher matcher = CLONE_PATTERN.matcher(filename);
		String base;
		String ext;
		if (matcher.matches()) {
			base = matcher.group(1);
			ext = (matcher.group(3) != null) ? matcher.group(3) : "";
		} else {
			base = MyFileUtils.noExtension(filename);
			ext = MyFileUtils.getExtension(filename);
		}

		File dir = file.getParentFile();
		String candidate = base + "_clone" + ext;
		File target = new File(dir, candidate);
		if (!target.exists()) {
			return candidate;
		}

		int count = 2;
		while (true) {
			candidate = String.format("%s_clone%d%s", base, count, ext);
			target = new File(dir, candidate);
			if (!target.exists()) {
				return candidate;
			}
			count++;
		}
	}

	public static File getOriginalFile(File cloneFile) {
		if (cloneFile == null) return null;
		String filename = cloneFile.getName();
		Matcher matcher = CLONE_PATTERN.matcher(filename);
		if (!matcher.matches()) return null;
		String base = matcher.group(1);
		String ext = (matcher.group(3) != null) ? matcher.group(3) : "";
		File orig = new File(cloneFile.getParentFile(), base + ext);
		return orig.exists() ? orig : null;
	}

	public void clone_file(FileDescr fd, DefaultMutableTreeNode targetNode) {
		if (fd == null) return;
		File file = fd.getFile();
		if (!file.exists() || !file.isFile()) return;

		String newfilename = generateCloneFilename(file);
		File newfile = new File(file.getParentFile(), newfilename);
		String ext = fd.getExtension();

		try {
			if (ext.equals(".java")) {
				String content = Files.readString(file.toPath());
				String originalClassName = MyFileUtils.noExtension(file.getName());
				String cloneClassName = MyFileUtils.noExtension(newfilename);
				String modifiedContent = content.replaceAll("\\bpublic\\s+class\\s+" + Pattern.quote(originalClassName) + "\\b", "public class " + cloneClassName);
				Files.writeString(newfile.toPath(), modifiedContent);
			} else {
				FileUtils.copyFile(file, newfile);
			}
		} catch (Exception e) {
			codelab.consoleLogErr("ERROR: Unable to clone file " + file.getName() + " : " + e.getMessage());
			return;
		}

		DefaultMutableTreeNode parentNode = null;
		if (targetNode != null && targetNode.getParent() instanceof DefaultMutableTreeNode) {
			parentNode = (DefaultMutableTreeNode) targetNode.getParent();
		} else {
			parentNode = rootNode;
		}

		FileDescr newFD = new FileDescr(newfile);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFD, false);
		DefaultTreeModel treeModel = (DefaultTreeModel) getModel();
		treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

		setExpandsSelectedPaths(true);
		setSelectionPath(new TreePath(newNode.getPath()));
		selectedNode = newNode;

		editor.change_file(newNode);
		codelab.consoleLog(String.format(LABEL("Clone_created_msg"), newfilename));
	}

	public void delete_clone(DefaultMutableTreeNode node, FileDescr fd) {
		if (fd == null) return;
		File file = fd.getFile();
		String filename = fd.getFilename();

		int reply = JOptionPane.showConfirmDialog(CodeLab.FRAME,
			String.format(LABEL("Confirm_delete_clone"), filename),
			LABEL("Delete_clone_title"),
			JOptionPane.YES_NO_OPTION);

		if (reply != JOptionPane.YES_OPTION) return;

		Statements bloc = new Statements() {
			public Object execute() {
				boolean isCurrent = false;
				File currentFile = editor.getCurrentFile();
				if (currentFile != null && currentFile.equals(file)) {
					isCurrent = true;
				}

				editor.remove_file(fd.getUID());

				String ext = fd.getExtension();
				if (ext.equals(".java")) {
					File classFile = new File(fd.getDirectory(), MyFileUtils.noExtension(filename) + ".class");
					if (classFile.exists()) classFile.delete();
				} else if (ext.equals(".c")) {
					File binFile = new File(fd.getDirectory(), MyFileUtils.noExtension(filename));
					if (binFile.exists() && binFile.isFile()) binFile.delete();
				}

				fd.delete();
				deleteNode(node);
				selectedNode = null;

				if (isCurrent) {
					File originalFile = getOriginalFile(file);
					boolean restored = false;
					if (originalFile != null) {
						restored = selectNodeFromFile(originalFile);
					}
					if (!restored) {
						editor.closeCurrentEditor(codelab.isTutor() ? 2 : 0);
					}
				}

				editor.updateHashMap();
				codelab.consoleLog(String.format(LABEL("Clone_deleted_msg"), filename));
				return true;
			}
		};
		safeExecute(bloc);
		updateUI();
	}
}

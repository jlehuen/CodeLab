package codelab.modules.editeur.manager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import codelab.ExceptionManager;

// https://docs.oracle.com/javase/tutorial/uiswing/dnd/intro.html
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/dataflavor.html
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/toplevel.html
// https://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree

/**
*	Classe du gestionnaire de Drag and Drop
*	@author Jérôme Lehuen
*	@version 25/01/23
*/

public class FileTransferHandler extends TransferHandler {

	private FileManager manager; // Hérite de JTree

	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	private DefaultMutableTreeNode[] nodesToRemove;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public FileTransferHandler(FileManager manager) {
		this.manager = manager;
		manager.setDragEnabled(true);
		manager.setTransferHandler(this);
		manager.setDropMode(DropMode.ON_OR_INSERT);
		//manager.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		manager.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);

		try {
			String mimeType = String.format("%s;class=\"%s\"",
				DataFlavor.javaJVMLocalObjectMimeType,
				javax.swing.tree.DefaultMutableTreeNode[].class.getName());
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		}
		catch (ClassNotFoundException e) {
			ExceptionManager.process(e);
		}
	}

    ///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private TreePath getDropPath(TransferSupport support) {
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		return dl.getPath();
	}

	private FileDescr getFileDescr(DefaultMutableTreeNode node) {
		return (FileDescr) node.getUserObject();
	}

	private boolean sameFilenames(DefaultMutableTreeNode node1, DefaultMutableTreeNode node2) {
		String name1 = getFileDescr(node1).getFilename();
		String name2 = getFileDescr(node2).getFilename();
		return name1.equals(name2);
	}

	private DefaultMutableTreeNode copy(DefaultMutableTreeNode node, HashSet<TreeNode> doneItems, JTree tree) {
        DefaultMutableTreeNode copy = (DefaultMutableTreeNode) node.clone();
		doneItems.add(node);
		for (int i=0; i<node.getChildCount(); i++) {
			copy.add(copy((DefaultMutableTreeNode) ((TreeNode) node).getChildAt(i), doneItems, tree));
		}
		int row = tree.getRowForPath(new TreePath(copy.getPath()));
		tree.expandRow(row);
		return copy;
	}

	///////////////////////////////////////////////////
	// Méthodes de TransferHandler
	///////////////////////////////////////////////////

    public String toString() {
		return getClass().getName();
	}

    public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}

	// ======================================================================
	// Méthode canImport
	// ======================================================================

	private boolean DEBUG = false;

	public boolean canImport(TransferHandler.TransferSupport support) {

		if (!support.isDrop()) {
			if (DEBUG) System.out.println("DROP_ERROR 0");
			return false;
		}

		if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			// Glissé de fichiers depuis le bureau de l'ordinateur
			if (DEBUG) System.out.println("DROP_VALID");
			support.setShowDropLocation(false);
			return true;
		}

		if (!support.isDataFlavorSupported(nodesFlavor)) {
			if (DEBUG) System.out.println("DROP_ERROR 1");
			return false;
		}
		support.setShowDropLocation(true);

		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		JTree tree = (JTree)support.getComponent();
		int dropRow = tree.getRowForPath(dl.getPath());
		int[] selRows = tree.getSelectionRows();

		if (selRows.length == 0) {
			// Permet d'éviter une ArrayIndexOutOfBoundsException
			return false;
		}

		// Récupérer le noeud cible
		DefaultMutableTreeNode target = (DefaultMutableTreeNode) getDropPath(support).getLastPathComponent();

		// Pas sur un fichier
		if (getFileDescr(target).isFile()) {
			if (DEBUG) System.out.println("DROP_ERROR 2");
			return false;
		}

		// Pas à côté d'un fichier du même nom
		List<TreeNode> children = Collections.list(target.children());
		for (TreeNode c : children) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) c;
			// Itérer sur les noeuds à déplacer
			for (int i = 0; i < selRows.length; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(selRows[i]).getLastPathComponent();
				if (sameFilenames(child, node)) {
					if (DEBUG) System.out.println("DROP_ERROR 3");
					return false;
				}
			}
		}

		// Dépôt à la racine possible
		String name = getFileDescr(target).getFilename();
		if (name.equals("local") || name.equals("dist")) {
			if (DEBUG) System.out.println("DROP_VALID");
			return true;
		}

		// Ne pas autoriser un dépôt sur les sélections de la source
		for (int i = 0; i < selRows.length; i++) {
			if (selRows[i] == dropRow) return false;
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(selRows[i]).getLastPathComponent();
			for (TreeNode offspring : Collections.list(node.depthFirstEnumeration())) {
				if (tree.getRowForPath(new TreePath(((DefaultMutableTreeNode) offspring).getPath())) == dropRow) {
					if (DEBUG) System.out.println("DROP_ERROR 4");
					return false;
				}
			}
		}
		if (DEBUG) System.out.println("DROP_VALID");
		return true;
	}

	// ======================================================================
	// Méthode createTransferable
	// ======================================================================

	protected Transferable createTransferable(JComponent c) {
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null) return null;

		// Constituer un tableau de noeuds de copies pour le transfert
		// et un autre pour les noeuds qui seront supprimés

		List<DefaultMutableTreeNode> copies = new ArrayList<DefaultMutableTreeNode>();
		List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
		DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
		HashSet<TreeNode> doneItems = new LinkedHashSet<>(paths.length);
		DefaultMutableTreeNode copy = copy(firstNode, doneItems, tree);
		copies.add(copy);
		toRemove.add(firstNode);

		for (int i = 1; i < paths.length; i++) {
			DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
			if (doneItems.contains(next)) continue;

			// Ne pas ajouter des noeuds de niveau supérieur
			if (next.getLevel() < firstNode.getLevel()) break;

			else if (next.getLevel() > firstNode.getLevel()) {
				// Contient déjà un fils
				copy.add(copy(next, doneItems, tree));
			}
			else {
				// Noeud frère
				copies.add(copy(next, doneItems, tree));
				toRemove.add(next);
			}
			doneItems.add(next);
		}
		DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
		nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
		return new NodesTransferable(nodes);
	}

	// ======================================================================
	// Méthode exportDone
	// ======================================================================

	protected void exportDone(JComponent source, Transferable data, int action) {
		if ((action & MOVE) == MOVE) {
			JTree tree = (JTree)source;
			DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

			// Suppression des noeuds identifiés dans createTransferable
			for (int i = 0; i < nodesToRemove.length; i++) {
				//System.out.println("Removing " + nodesToRemove[i].getUserObject());
				model.removeNodeFromParent(nodesToRemove[i]);
			}
			manager.updateAllFileDescr(); // Actualise tous les FileDescr du FileManager
		}
	}

	// ======================================================================
	// Méthode importData
	// ======================================================================

	@SuppressWarnings("unchecked")
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) return false;

		// ----------------------------------------------------------------------------
		// Dépot de fichiers depuis le bureau de l'ordinateur

		if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			try {
				Transferable transferable = support.getTransferable();
				List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
				for (File file : files) manager.new_droppedFile(file, null); // null => à la racine
			}
			catch (UnsupportedFlavorException e) { ExceptionManager.process(e); }
			catch (IOException e) { ExceptionManager.process(e); }
			return true;
		}

		// ----------------------------------------------------------------------------
		// Dépot de noeuds depuis le JTree

		DefaultMutableTreeNode[] nodes = null;
		try {
			Transferable transferable = support.getTransferable();
			nodes = (DefaultMutableTreeNode[]) transferable.getTransferData(nodesFlavor);
        }
		catch (UnsupportedFlavorException e) { ExceptionManager.process(e); }
		catch (IOException e) { ExceptionManager.process(e); }

		// Identifier la destination
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		int childIndex = dl.getChildIndex();
		TreePath dest = dl.getPath();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
		JTree tree = (JTree) support.getComponent();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

		// Configurer le mode
		int index = childIndex; // DropMode.INSERT
		if (childIndex == -1) { // DropMode.ON
			index = parent.getChildCount();
		}

		// Déplacer les noeuds
		for (int i = 0; i < nodes.length; i++) {
			model.insertNodeInto(nodes[i], parent, index++);
			manager.move_file(nodes[i], parent); // Déplacement effectif des fichiers
		}
		manager.resetSelectedNode();
		return true;
	}

	///////////////////////////////////////////////////
	// Classe interne des noeuds transférables
	///////////////////////////////////////////////////

	public class NodesTransferable implements Transferable {
		DefaultMutableTreeNode[] nodes;

		public NodesTransferable(DefaultMutableTreeNode[] nodes) {
			this.nodes = nodes;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return nodes;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}
}

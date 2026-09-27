package codelab.modules.editeur;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import codelab.CodeLab;
import codelab.AbstractToolbar;
import codelab.modules.editeur.manager.FileDescr;
import codelab.utils.ResourceUtils;

/**
*	Super-classe abstraite des éditeurs
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 15/09/26
*/

public abstract class AbstractEditor extends JPanel {

	public ModuleEditor module;
	protected AbstractToolbar toolbar;

	private DefaultMutableTreeNode node; // Unique référence vers le node du fichier

	// Cette référence est la seule pote d'entrée vers les informations du fichier
	// Ces informations sont stockées dans l'objet FileDescr stocké dans le node

	protected boolean editable = true; // Non éditable si mode tuteur
	protected boolean modified = false; // Flag de modification
	protected boolean controlled = false; // Flag de prise de contrôle
	protected boolean compilation_ok = false; // Flag de compilation
	protected boolean new_content = true; // Pour inhiber le premier diff
	protected boolean showLockIcon = false; // Activé pour les éditeurs de code (texte et scratch)
	protected JLabel lockBadge;

	public static final String EMPTY = "EMPTY"; // Utilisé dans closeCurrentEditor() de ModuleEditor

	///////////////////////////////////////////////////
	// Intégration du badge de verrouillage
	///////////////////////////////////////////////////
	
	private static final BufferedImage lockedImg =
		ResourceUtils.loadBufferedImageAsRessource("icons/icon_locked.png");

	protected JComponent wrapWithLockLayer(JComponent comp) {
		ImageIcon icon = (lockedImg != null) ? new ImageIcon(lockedImg) : null;
		lockBadge = new JLabel(icon);
		lockBadge.setOpaque(false);
		lockBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lockBadge.setToolTipText(CodeLab.LABEL("Clone_program"));
		lockBadge.setVisible(showLockIcon && !isEmpty() && !editable);

		lockBadge.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (module != null && (CodeLab.INSTANCE.isTutor() || CodeLab.INSTANCE.isAdmin())) {
					module.cloneCurrentFile();
				}
			}
		});

		JLayeredPane layeredPane = new JLayeredPane() {
			@Override
			public void doLayout() {
				super.doLayout();
				for (Component c : getComponents()) {
					if (c == lockBadge) {
						int w = c.getPreferredSize().width;
						int h = c.getPreferredSize().height;
						int marginX = 35;
						int marginY = 15;
						c.setBounds(getWidth() - w - marginX, marginY, w, h);
					} else {
						c.setBounds(0, 0, getWidth(), getHeight());
					}
				}
			}

			@Override
			public Dimension getPreferredSize() {
				return comp.getPreferredSize();
			}

			@Override
			public Dimension getMinimumSize() {
				return comp.getMinimumSize();
			}

			@Override
			public boolean isOptimizedDrawingEnabled() {
				return false;
			}
		};

		layeredPane.add(comp, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(lockBadge, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
	}

	public void updateLockBadge() {
		if (lockBadge != null) {
			lockBadge.setVisible(showLockIcon && !isEmpty() && !editable);
			if (lockBadge.getParent() != null) {
				lockBadge.getParent().repaint();
			}
		}
	}

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public AbstractEditor(DefaultMutableTreeNode node) {
		this.node = node;
	}

	public AbstractEditor() {
		this.node = null;
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public AbstractToolbar getToolbar() { return toolbar; }
	public ModuleEditor getModule() { return module; }
	public boolean getEditable() { return editable; }
	public boolean isModified() { return modified; }
	public boolean isControlled() { return controlled; }
	public boolean compilation_OK() { return compilation_ok; }
	public void resetCompilationFlag() { compilation_ok = false; }
	public void setControlled(boolean value) { controlled = value; }
	public void setModified(boolean value) { modified = value; }

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public boolean isEmpty() {
		return (node == null);
	}

	public boolean isHTMLFile() {
		if (node == null) return false;
		return getExtension().equals(".html");
	}

	public boolean isTextFile() {
		if (node == null) return false;
		String ext = getExtension();
		return ext.equals(".txt") ||
			   ext.equals(".csv") ||
			   ext.equals(".log");
	}

	public FileDescr getFileDescriptor() {
		if (node == null) return null;
		return (FileDescr) node.getUserObject();
	}

	public String getUID() {
		if (node == null) return null;
		return getFileDescriptor().getUID();
	}

	public File getFile() {
		if (node == null) return null;
		return getFileDescriptor().getFile();
	}

	public String getFilename() {
		if (node == null) return null;
		return getFileDescriptor().getFilename();
	}

	public String getExtension() {
		if (node == null) return null;
		return getFileDescriptor().getExtension();
	}

	public String getDirectory() {
		if (node == null) return null;
		return getFileDescriptor().getDirectory();
	}

	///////////////////////////////////////////////////
	// Méthodes à implémenter
	///////////////////////////////////////////////////

	public abstract void save_content(); // Attention: accès conccurents
	public abstract void load_content(); // Attention: accès conccurents

	public abstract void save_content(File file);
	public abstract void load_content(File file);
	public abstract void load_template(File file);

	public abstract boolean isBlank();
	public abstract boolean isTextEditor();
	public abstract boolean compile_file();
	public abstract boolean convertSpaceToTab(int nbspaces);
	public abstract boolean convertTabToSpace(int nbspaces);
	public abstract void changeFont(String name, int size);
	public abstract void setInvisible(boolean value);
	public abstract void setCodeFoldingEnabled(boolean value);
	public abstract void setEditableConfiguration(boolean value);
	public abstract void addLineHighlight(int line, Color color);
	public abstract void removeAllLineHighlights();
}

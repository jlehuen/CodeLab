package codelab.modules.editeur;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import codelab.AbstractModule;
import codelab.CodeLab;
import codelab.Executeur;
import codelab.client.CodelabClient;
import codelab.client.Statut;
import codelab.client.UserData;
import codelab.console.Console;
import codelab.modules.editeur.image.ImageViewer;
import codelab.modules.editeur.manager.FileDescr;
import codelab.modules.editeur.manager.FileManager;
import codelab.modules.editeur.manager.TypeLang;
import codelab.modules.editeur.manager.BackupManager;
import codelab.modules.editeur.scratch.ScratchEditor;
import codelab.modules.editeur.scratch.widgets.AbstractWidget;
import codelab.modules.editeur.text.TextEditor;
import codelab.modules.editeur.usertable.UserTable;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe du module éditeur
*	@author Jérôme Lehuen
*	@version 02/06/25
*/

public class ModuleEditor extends AbstractModule {

	private JSplitPane splitpane_vertical;
	private JSplitPane splitpane_horizontal;
	private final int VPOS_INIT = 300; // Position initiale

	private HashMap<String, AbstractEditor> hashMapEditor;
	private AbstractEditor editor; // Éditeur courant
	private FileManager manager;
	private JScrollPane managerScrollPane;

	private UserTable usertable = null;
	private UserData currentUserData;

	public AbstractWidget widgetToPaste = null; // Pour le copier-coller

	public boolean diff = true; // Surlignage des différences en jaune
	public boolean controlled = false; // Si prise de contrôle (côté student)
	protected String tutor; // Nom du tuteur en cas de contrôle

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ModuleEditor(CodeLab codelab) {
		super(codelab);
		name = "EDITOR";
		title = LABEL("Tab_editor");
		manager = new FileManager(this, codelab);
		editor = new EmptyEditor(this, 0);
		toolbar = editor.getToolbar();
		hashMapEditor = new HashMap<>();

		managerScrollPane = new JScrollPane(manager);
		managerScrollPane.setBorder(BorderFactory.createEmptyBorder());

		// Fichiers en haut et liste des utilisateurs (null par défaut) en bas
		splitpane_horizontal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, managerScrollPane, null);
		splitpane_horizontal.setBorder(BorderFactory.createEmptyBorder());
		splitpane_horizontal.setDividerSize(0);

		// File manager à gauche et éditeur à droite
		splitpane_vertical = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitpane_horizontal, (JPanel) editor);
		splitpane_vertical.setBorder(BorderFactory.createEmptyBorder());
		splitpane_vertical.setOneTouchExpandable(false);
		splitpane_vertical.setDividerLocation(VPOS_INIT);
		setComponent(splitpane_vertical);
	}

	///////////////////////////////////////////////////
	// Getters et Setters
	///////////////////////////////////////////////////

	public FileManager getManager() {
		return manager;
	}

	public UserTable getUserTable() {
		return usertable;
	}

	public UserData getSelectedUser() {
		return currentUserData;
	}

	public AbstractEditor getEditor() {
		return editor;
	}

	// Les informations du fichier courant ne sont accessibles que via l'éditeur courant
	// Elles sont stockées dans un objet FileDescr stocké dans l'attribut node de AbstractEditor

	public String getCurrentDirectory() {
		return editor.getDirectory();
	}

	public File getCurrentFile() {
		return editor.getFile();
	}

	public String getCurrentFilename() {
		return editor.getFilename();
	}

	public String getCurrentExtension() {
		return editor.getExtension();
	}

	public TypeLang getCurrentLanguage() {
		String ext = getCurrentExtension();
		return TypeLang.identify(ext);
	}

	public boolean isBlocs() {
		return getCurrentLanguage().equals(TypeLang.BLOCS);
	}

	public int getDividerLocation() {
		return splitpane_vertical.getDividerLocation();
	}

	// ------------------------------------------------
	// Pour la mise à jour du menu du CodeLabTextArea

	private UserData udata = null;

	public UserData getCurrentUdata() {
		return udata;
	}
	public void updateUserData(UserData udata) {
		this.udata = udata;
	}

	///////////////////////////////////////////////////
	// Quelques prédicats
	///////////////////////////////////////////////////

	public boolean isEmpty() {
		return editor.isEmpty();
	}

	public boolean isModified() {
		return editor.isModified();
	}

	public boolean isCompilable() {
		if (isEmpty()) return false;
		return getCurrentLanguage().isCompilable();
	}

	public boolean isExecutable() {
		if (isEmpty()) return false;
		return getCurrentLanguage().isExecutable();
	}

	///////////////////////////////////////////////////
	// Méthodes de sauvegarde et de backup
	///////////////////////////////////////////////////

	public synchronized void saveCurrentFile() {

		if (editor.isBlank()) return;
		if (!editor.editable) return;

		// Sauvegarde en local
		editor.save_content();
		editor.resetCompilationFlag();

		File file = getCurrentFile();
		if (file == null) return; // Si édition du fichier de configuration

		if (codelab.isTutor()) return;
		if (codelab.isAdmin()) return;

		// Sauvegarde sur le serveur (si mode connecté)
		uploadFile(file);
	}

	public synchronized void backupCurrentFile() {
		File file1 = getCurrentFile();
		if (file1 == null) return; // Si édition du fichier de configuration

		// Générer un backup daté et le transmettre au serveur
		File file2 = BackupManager.backup(file1);
		if (file2 == null) return; // Pas de backup
		uploadFile(file2);
	}

	public void saveAllFiles() {
		for (AbstractEditor editor : hashMapEditor.values()) {
			editor.save_content();
		}
	}

	///////////////////////////////////////////////////
	// Réinitialisation de l'éditeur
	///////////////////////////////////////////////////

	public void reinit(Statut statut, UserData[] array, String sessionId, boolean openned) {

		// La liste est null si statut n'est pas un TUTOR
		int vpos = splitpane_vertical.getDividerLocation();
		switch (statut) {
			case STANDALONE:
			case STUDENT:
				// Construction du File Manager
				manager = new FileManager(this, codelab);
				manager.setTransferHandlerCapability(true);
				managerScrollPane = new JScrollPane(manager);
				managerScrollPane.setBorder(BorderFactory.createEmptyBorder());
				// Arranger la partie gauche
				splitpane_horizontal.setTopComponent(managerScrollPane);
				splitpane_horizontal.setBottomComponent(null);
				splitpane_horizontal.setDividerSize(0);
				splitpane_vertical.setDividerLocation(vpos);
				reset(); // Réinitialiser l'éditeur
				break;
			case TUTOR:
			case ADMIN:
				// Construction du File Manager
				manager = new FileManager(this, codelab);
				manager.setTransferHandlerCapability(false);
				managerScrollPane = new JScrollPane(manager);
				managerScrollPane.setBorder(BorderFactory.createEmptyBorder());
				// Construction de la Table des utilisateurs
				List<UserData> ulist = Arrays.asList(array);
				usertable = new UserTable(this, ulist, sessionId, openned);
				// Arranger la partie gauche
				splitpane_horizontal.setTopComponent(managerScrollPane);
				splitpane_horizontal.setBottomComponent(usertable);
				splitpane_horizontal.setDividerSize(DIV_SIZE);
				splitpane_horizontal.setDividerLocation(0.5);
				splitpane_vertical.setDividerLocation(vpos);
				reset(); // Rénitialiser l'éditeur
				usertable.adjustColumnsWidth(); // Visibilité des colonnes
				UserData udata = usertable.getSelectedUser();
				if (udata != null) changeUser(udata);
				break;
			case ERROR:
				// Pas censé arriver
				break;
		}
		updateFrameTitle();
	}

	public void reset() {
		// Notamment invoqué après un repopulate() du FileManager
		clearHashMap();
		closeCurrentEditor(codelab.isTutor() ? 2 : 0);
		//manager.setBase(CodeLab.getProgramDir());
		toolbar.update();
	}

	public void changeUser(UserData udata) {
		CodelabClient client = codelab.getClient();
		client.stopAllTasks();

		// Mémoriser si possible le fichier en cours de visualisation pour le retrouver
		if (currentUserData != null && getCurrentFile() != null)
			usertable.setLastFile(currentUserData.getLogin(), getCurrentFile());

		// Actualiser le nouveau userData
		currentUserData = udata;

		switch (udata.getStatut()) {

			case STUDENT:
				// Le tuteur a sélectionné un étudiant
				String base = codelab.getProgramDir() + "/" + udata.getLogin();
				manager.setBase(base);
				manager.populate();
				manager.expandAll();
				manager.invokeLater_updateUI();
				clearHashMap(); // Après les actions sur le manager à cause du listener de changement

				// Récupérer si possible le dernier fichier visualisé pour cet étudiant
				File lastFile = usertable.getLastFile(udata.getLogin());
				if (lastFile != null) manager.selectNodeFromFile(lastFile);
 				else closeCurrentEditor(2);

				if (udata.isControlled())
					// Active l'actualisation si le student était déjà sous contrôle du tuteur
					client.startControlTimer(udata.getLogin());
				else
					client.startRegularTimer();
				break;

			case TUTOR:
			case ADMIN:
				// Le tuteur a sélectionné un autre tuteur
				manager.clear();
				clearHashMap(); // Après les actions sur le manager à cause du listener de changement
				closeCurrentEditor(1); // Affichage spécifique
				break;

			default: // Ne peut pas arriver
		}
	}

	///////////////////////////////////////////////////
	// Méthodes en cas de modification du Jtree
	///////////////////////////////////////////////////

	public File keepCurrentFile() {
		// Méthode invoquée par le MepaClient avant un refresh du FileManager
		if (isEmpty()) return null;
		File temp = new File(CodeLab.TEMP_FOLDER + File.separator + getCurrentFilename());
		Utils.copyFile(getCurrentFile(), temp);
		return temp;
	}

	public void reloadFile(File temp) {
		// Méthode invoquée par le MepaClient après un refresh du FileManager
		clearHashMap();
		closeCurrentEditor(codelab.isTutor() ? 2 : 0);
		if (temp == null) return;
		DefaultMutableTreeNode node = manager.reloadFile(temp);
		if (node != null) change_file(node);
	}

	public void updateHashMap() {
		// Méthode invoquée par le FileManager après une suppression
		// En 2 étapes sinon ConcurrentModificationException sur HashMap
		List<FileDescr> filesToRemove = new ArrayList<>();
		for (AbstractEditor editor : hashMapEditor.values()) {
			FileDescr fd = editor.getFileDescriptor();
			if (!fd.fileExists()) {
				// Le fichier n'existe plus
				filesToRemove.add(fd);
			}
		}
		for (FileDescr fd : filesToRemove)
			remove_file(fd.getUID());
	}

	///////////////////////////////////////////////////
	// Méthodes de AbstractModule à implémenter
	///////////////////////////////////////////////////

	public void init() {}
	public void exit() {}
	public void stop() {}
	public void resized() {}

	public void start() {
		if (isEmpty()) return;
		String ext = getCurrentExtension();
		if (ext.equals(".blocs")) compileFile(); // Traduire en Python
	}

	public String handleRequest(Map<String, String> params) {
		// Pas de gestionnaire de requêtes
		return "";
	}

	///////////////////////////////////////////////////
	// Autres méthodes publiques
	///////////////////////////////////////////////////

	// Pour mettre eu pause les tâches répétitives
	private boolean pause = false;
	public boolean isPaused() { return pause; }
	public void pauseTimerTasks() { pause = true; }
	public void resumeTimerTasks() { pause = false; }

	public void clearConsole() {
		console.clear();
	}

	public void new_file() {
		manager.new_file();
	}

	public void reloadCurrentFile() {
		editor.load_content();
	}

	public void resetCompilationFlag() {
		editor.resetCompilationFlag();
	}

	public void clearHashMap() {
		hashMapEditor = new HashMap<>();
	}

	public void changeFont(String name, int size) {
		for (AbstractEditor editor : hashMapEditor.values()) {
			editor.changeFont(name, size);
		}
	}

	public void setCodeFoldingEnabled(boolean value) {
		for (AbstractEditor editor : hashMapEditor.values()) {
			editor.setCodeFoldingEnabled(value);
		}
	}

	public void setInvisible(boolean value) {
		for (AbstractEditor editor : hashMapEditor.values()) {
			editor.setInvisible(value);
		}
	}

	public boolean compileFile() {
		// Modifie le flag compilation_ok de AbstractEditor
		return editor.compile_file();
	}

	public boolean convertSpaceToTab(int nbspaces) {
		return editor.convertSpaceToTab(nbspaces);
	}

	public boolean convertTabToSpace(int nbspaces) {
		return editor.convertTabToSpace(nbspaces);
	}

	public boolean compilation_OK() {
		// Retourne le résultat de la dernière compilation
		return editor.compilation_OK();
	}

	public void updateContent(String path) throws IOException {
		// Invoqué par le updateFile() de CodelabClient en mode tuteur

		File current = getCurrentFile();
		if (current == null) return;
		String currentPath = current.getAbsolutePath().replaceAll("\\\\", "/");
		String updatedPath = new File(path).getAbsolutePath().replaceAll("\\\\", "/");
		if (!currentPath.equals(updatedPath)) return;

		if (!diff) {
			// Pas de surlignage des différences
			reloadCurrentFile(); // Rechargement du nouveau contenu
			return;
		}

		if (editor.new_content) {
			// Pas de différences à rechercher
			editor.new_content = false;
			reloadCurrentFile(); // Rechargement du nouveau contenu
			return;
		}

		// Enregistrement des textes à comparer
		File file_old = new File(CodeLab.TEMP_FOLDER + "/temp_old.txt"); // Fichier courant
		File file_new = new File(CodeLab.TEMP_FOLDER + "/temp_new.txt"); // Fichier modifié

		editor.save_content(file_old); // Backup du contenu courant
		reloadCurrentFile(); // Rechargement du nouveau contenu
		editor.save_content(file_new); // Backup du nouveau contenu

		// Recherche des différences
		List<String> original = Files.readAllLines(file_old.toPath());
		List<String> revised = Files.readAllLines(file_new.toPath());
		Patch<String> patch = DiffUtils.diff(original, revised);
		//boolean changes = patch.getDeltas().size() > 0;
		//if (!changes) return; // Pas de modification

		// Surlignage des différences
		Color color = Color.decode("#FFFFE0"); // Jaune clair
		editor.removeAllLineHighlights(); // Effacer le précédent surlignage
		for (AbstractDelta<String> delta : patch.getDeltas()) {
			int line = delta.getSource().getPosition();
			int size = delta.getTarget().size();
			//codelab.printlnToConsole("DIFF: " + delta.toString(), Color.YELLOW);
			editor.addLineHighlight(line, color);
			for (int i = 0 ; i < size ; i++) {
				editor.addLineHighlight(line+i, color);
			}
		}
	}

	public void sendCurrentControlledFile(String user_login) {
		// Transmettre un fichier directement à un étudiant
		if (isEmpty()) return; // Pas d'éditeur
		if (editor.isModified()) {
			editor.save_content();
			uploadControlledFile(getCurrentFile(), user_login);
		}
	}

	public void setControlled(boolean value, String fullname) {
		// Méthode invoquée en cas de prise de contrôle (côté student)
		controlled = value;
		if (value) tutor = fullname; // Pour updateFrameTitle
		manager.setTransferHandlerCapability(!value); // Pas de déplacement dans ce mode
		editor.setEditableConfiguration(getEditableStrategy());
		codelab.getToolbar().update(); // Actualisation de la barre d'outils
		updateFrameTitle();
	}

	public void setControlMode(boolean value, String login) {
		// Méthode invoquée en cas de prise de contrôle (côté tuteur)
		usertable.setControlMode(login, value); // Modifie l'apparence de l'item login
		editor.setEditableConfiguration(getEditableStrategy());
		codelab.getToolbar().update(); // Actualisation de la barre d'outils
	}

	public void open_file(File file, boolean save_flag) {
		// Pour ouvrir un fichier hors-filemanager (.properties par exemple)
		if (save_flag) editor.save_content();
		if (manager != null) manager.resetSelectedNode();
		TextEditor textEditor = new TextEditor(this, MyFileUtils.getExtension(file));
		editor = textEditor;
		editor.load_content(file);

		// La nouvelle barre d'outils
		toolbar = editor.getToolbar();
		codelab.changeToolBar(toolbar);

		// Quelques actualisations
		editor.setEditableConfiguration(true);
		editor.getToolbar().setLang("--");
		editor.resetCompilationFlag();
		updateFrameTitle();
		update_UI();
		SwingUtilities.invokeLater(() -> textEditor.getCodeLabTextArea().requestFocusInWindow());
	}

	public void updateFrameTitle() {
		// Actualise le titre de la fenêtre de l'application
		String title = CodeLab.TITLE;

		if (codelab.isConnected()) {
			title += String.format(" – %s [ %s ]", codelab.getUsername(), codelab.getSession());
		}
		FileDescr fd = editor.getFileDescriptor();
		if (fd == null) {
			editor.getToolbar().updateRunTipText(""); // Tooltip du bouton RUN
		} else {
			title += " – " + fd.getFilename();
			editor.getToolbar().updateRunTipText(fd.getFilename()); // Tooltip du bouton RUN
			if (codelab.isRunning()) title += " – RUNNING";
		}
		if (controlled) title += " – controlled by " + tutor;

		codelab.setTitle(title);
	}

	///////////////////////////////////////////////////
	// Méthodes en provenance du file manager
	///////////////////////////////////////////////////

	public void new_file(DefaultMutableTreeNode node, File template) {
		change_file(node); // Créer un nouvel éditeur
		if (template != null)
			editor.load_template(template); // Charger le template
		editor.save_content(); // Sauvegarder le nouveau fichier
	}

	public void change_file(DefaultMutableTreeNode node) {
		if ((!codelab.isTutor() && !codelab.isAdmin()) || editor.getFileDescriptor() == null || isClone(editor.getFileDescriptor())) // Si pas mode tuteur/admin ou si fichier hors-filemanager ou si clone...
			editor.save_content(); // Sauvegarder le fichier courant

		FileDescr fd = (FileDescr) node.getUserObject();
		String ext = fd.getExtension();
		String uid = fd.getUID();

		if (hashMapEditor.containsKey(uid)) {
			// Récupérer l'éditeur dans la table de hachage
			editor = hashMapEditor.get(uid);
			// Cas des fichiers qui peuvent avoir été modifiés lors d'une exécution
			if (editor.isTextFile()) editor.load_content();
		}
		else {
			// Créer un nouvel éditeur et le référencer
			editor = newEditor(ext, node);
			hashMapEditor.put(uid, editor);
			if (fd.fileExists()) editor.load_content();
		}
		// La nouvelle barre d'outils
		toolbar = editor.getToolbar();
		codelab.changeToolBar(toolbar);

		// Quelques actualisations
		editor.setEditableConfiguration(getEditableStrategy());
		editor.getToolbar().setLang(ext);
		editor.resetCompilationFlag();
		updateFrameTitle();
		update_UI();
	}

	public void remove_file(String uid) {
		hashMapEditor.remove(uid);
		if (editor.getUID() == null) return;
		if (editor.getUID().equals(uid)) {
			// Fichier en cours d'édition
			closeCurrentEditor(codelab.isTutor() ? 2 : 0);
		}
	}

	public void filenameChanged() {
		updateFrameTitle();
	}

	// ------------------------------------------------
	// Pour informer le serveur

	public void createFolderDist(File file) {
		codelab.getClient().createFolderDist(file);
	}

	public void newFileDist(File file) {
		codelab.getClient().newFileDist(file);
	}

	public void deleteFileDist(File file) {
		codelab.getClient().deleteFileDist(file);
	}

	public void renameFileDist(File file, String name) {
		codelab.getClient().renameFileDist(file, name);
	}

	public void moveFileDist(File file1, File file2) {
		codelab.getClient().moveFileDist(file1, file2);
	}

	public void uploadFile(File file) {
		codelab.getClient().uploadFile(file);
	}

	public void uploadControlledFile(File file, String user_login) {
		codelab.getClient().uploadControlledFile(file, user_login);
	}

	///////////////////////////////////////////////////
	// Gestion des erreurs générées par Scratch
	///////////////////////////////////////////////////

	public void checkBlocsErrors() {
		File errfile = Executeur.ERR_FILE;
		if (errfile == null) return;
		if (errfile.length() == 0) return;

		// Recherche des éléments par regex
		Pattern regexp1 = Pattern.compile("# Widget_(.*)");
		Pattern regexp2 = Pattern.compile("(.*Error:.*)"); // Les erreurs Python
		Pattern regexp3 = Pattern.compile("(.*Exception:.*)"); // Les user exceptions
		Matcher matcher1 = regexp1.matcher(MyFileUtils.readFile(errfile));
		Matcher matcher2 = regexp2.matcher(MyFileUtils.readFile(errfile));
		Matcher matcher3 = regexp3.matcher(MyFileUtils.readFile(errfile));

		if (matcher1.find()) {
			// Colorise en rouge le widget
			String widget_id = matcher1.group(1);
			ScratchEditor screditor = (ScratchEditor)editor;
			String instruction = screditor.setError(widget_id).replaceAll("\"", "\\\\\"");

			// Récupère le message d'erreur
			String error_msg = null;
			if (matcher2.find()) error_msg = matcher2.group(1);
			if (matcher3.find()) error_msg = matcher3.group(1);

			// Lance le moteur Jess
			CodeLab codelab = CodeLab.INSTANCE;
			codelab.getRete().assertFact("(error_blocs \"" + error_msg + "\")");
			codelab.getRete().assertFact("(instruction \"" + instruction + "\")");
			codelab.getRete().assertFact("(blocs)");
			codelab.getRete().run();
		}
		// Autres erreurs Python non interprétées -> support CodeLab ?
		else if (matcher2.find()) {
			CharSequence sequence = MyFileUtils.readFile(errfile);
			CodeLab.INSTANCE.printlnToConsole(CodeLab.LABEL("printError5"));
			CodeLab.INSTANCE.printToConsole(sequence.toString(), Console.COLOR_ERROR);
		}
	}

	///////////////////////////////////////////////////
	// Autres méthodes privées
	///////////////////////////////////////////////////

	public void closeCurrentEditor(int code) {
		editor = new EmptyEditor(this, code); // Pour le fichier welcome_%d_fr.png
		toolbar = editor.getToolbar();
		updateFrameTitle();
		update_UI();
		// Pour informer le serveur
		codelab.getClient().setEditedFile(AbstractEditor.EMPTY);
	}

	public boolean isClone(File file) {
		return FileManager.isClone(file);
	}

	public boolean isClone(FileDescr fd) {
		return FileManager.isClone(fd);
	}

	public boolean isCloneCurrentFile() {
		File file = getCurrentFile();
		return isClone(file);
	}

	public void cloneCurrentFile() {
		File file = getCurrentFile();
		if (file == null || manager == null) return;
		DefaultMutableTreeNode node = manager.searchNode_file(file);
		FileDescr fd = (node != null) ? (FileDescr) node.getUserObject() : new FileDescr(file);
		manager.clone_file(fd, node);
	}

	public void deleteCurrentClone() {
		File file = getCurrentFile();
		if (file == null || !isClone(file) || manager == null) return;
		DefaultMutableTreeNode node = manager.searchNode_file(file);
		if (node != null) {
			FileDescr fd = (FileDescr) node.getUserObject();
			manager.delete_clone(node, fd);
		}
	}

	// Retourne un nouvel éditeur en fonction de l'extension
	private AbstractEditor newEditor(String ext, DefaultMutableTreeNode node) {
		switch (ext) {
			case ".png":
			case ".jpg":
			case ".gif": return new ImageViewer(this, node);
			case ".npy": return new ImageViewer(this, node, "logo_numpy.png", "Numpy Array (not viewable data)");
			case ".blocs": return new ScratchEditor(this, node);
			default: return new TextEditor(this, node);
		}
	}

	// Pour savoir si l'éditeur doit-être éditable ou non
	private boolean getEditableStrategy() {
		if (editor != null && editor.isTextEditor() && editor.getFileDescriptor() == null)
			return true;
		switch (codelab.getStatut()) {
			case STANDALONE: return true;
			case STUDENT: return !controlled;
			case TUTOR: return (currentUserData != null) && (currentUserData.isControlled() || isCloneCurrentFile());
			case ADMIN: return (currentUserData != null) && (currentUserData.isControlled() || isCloneCurrentFile());
			default: return false;
		}
	}

	// Actualise l'interface après chaque changement de fichier
	private void update_UI() {
		SwingUtilities.invokeLater(() -> {
			int pos = splitpane_vertical.getDividerLocation(); // Position du séparateur vertical
			splitpane_vertical.setDividerLocation(pos); // Position du séparateur vertical
			splitpane_vertical.setRightComponent((JPanel) editor); // Placer l'éditeur à droite
			codelab.changeToolBar(toolbar);
		});
	}
}

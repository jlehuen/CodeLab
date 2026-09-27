package codelab.modules.editeur.usertable;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableModel;

import codelab.CodeLab;
import codelab.client.ChatController;
import codelab.client.NewpassDialog;
import codelab.client.UserData;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.Utils;

/**
*	Classe de la table des utlisateurs
*	@author Jérôme Lehuen
*	@version 23/10/25
*/

// https://www.codejava.net/java-se/swing/6-techniques-for-sorting-jtable-you-should-know
// https://mkyong.com/java/java-find-location-using-ip-address/
// https://ipgeolocation.io/documentation/ip-geolocation-api-java-sdk.html
// https://github.com/IPGeolocation/ip-geolocation-api-java-sdk

public class UserTable extends JScrollPane {

	private JTable table;
	private DefaultTableModel model;
	private TableRowSorter<TableModel> sorter;
	private List<String> groups = new ArrayList<String>();
	private CodeLab codelab = CodeLab.INSTANCE;
	private ModuleEditor editor;

	private String sessionId;
	private boolean openned;

	public boolean isSessionClosed() {
		return !openned;
	}

	// Visibilité des colonnes
	private boolean AFF_NAME = true;
	private boolean AFF_LOGIN = true;
	private boolean AFF_GROUP = false;
	private boolean AFF_ADRESS = true;
	private Boolean visible[] = { AFF_NAME, AFF_LOGIN, AFF_GROUP, AFF_ADRESS };

	// https://www.w3schools.com/colors/colors_picker.asp
	public static final Color VERY_LIGHT_GREY = new Color(212,212,212);
	public static final Color COLOR_CLOSED = new Color(255, 230, 230);

	// Items du menu popup
	private JMenuItem info_0 = new JMenuItem();
	private JMenuItem info_1 = new JMenuItem(); // Nom
	private JMenuItem info_2 = new JMenuItem(); // Date
	private JMenuItem info_3 = new JMenuItem(); // Adresse
	private JMenuItem info_4 = new JMenuItem(); // Session
	private JMenuItem info_5 = new JMenuItem(); // Statut

	private JMenuItem itemTchat = new JMenuItem();
	private JMenuItem itemControl = new JMenuItem();
	private JMenuItem itemResetPasswd= new JMenuItem();
	private JMenuItem itemDeconnexion = new JMenuItem();
	private JMenuItem itemOpenSession = new JMenuItem();
	private JMenuItem itemNewStudent = new JMenuItem();
	private JMenuItem itemResetHelpFlag = new JMenuItem();
	private JMenuItem itemResetTutorPasswd = new JMenuItem(CodeLab.LABEL("itemResetTutorPasswd"));
	//private JMenuItem itemEnableControlAll = new JMenuItem(CodeLab.LABEL("UserTable_17a"));
	private JMenuItem itemDisableControlAll = new JMenuItem(CodeLab.LABEL("UserTable_17b"));
	private JMenuItem itemDeconnexionAll = new JMenuItem(CodeLab.LABEL("UserTable_7"));
	private JMenuItem itemNameFilter = new JMenuItem(CodeLab.LABEL("UserTable_8a"));
	private JMenuItem itemGroupFilter = new JMenuItem(CodeLab.LABEL("UserTable_8b"));
	private JMenuItem itemRemoveFilter = new JMenuItem(CodeLab.LABEL("UserTable_9"));
	private JMenuItem itemMessage = new JMenuItem(CodeLab.LABEL("itemMessage"));

	private JMenu menuStudent = new JMenu();
	private JMenu menuGroup = new JMenu(CodeLab.LABEL("UserTable_menuGroup"));
	private JMenu menuConfig = new JMenu(CodeLab.LABEL("UserTable_menuConfig"));

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public UserTable(ModuleEditor editor, List<UserData> ulist, String sessionId, boolean openned) {

		this.sessionId = sessionId;
		this.openned = openned;
		this.editor = editor;

		// Créer la table
		table = new JTable();
		table.setEnabled(true);
		table.setShowGrid(false);
		table.setFocusable(false);
		table.setDefaultEditor(Object.class, null); // Non éditable
		table.setFillsViewportHeight(true);
		table.getTableHeader().setEnabled(true); // Actions sur le header (tris, etc.)
		table.getTableHeader().setReorderingAllowed(false);
		updateBackgroundColor(); // Selon si la session est ouverte ou fermée

		// Remplir la table
		model = new DefaultTableModel();
		model.addColumn(CodeLab.LABEL("UserTable_t1")); // Noms
		model.addColumn(CodeLab.LABEL("UserTable_t2")); // Logins
		model.addColumn(CodeLab.LABEL("UserTable_t3")); // Groupes
		model.addColumn(CodeLab.LABEL("UserTable_t4")); // Adresses IP
		table.setModel(model);
		String loginToExclude = codelab.getLogin();
		for (UserData udata : ulist) {
			if (udata.getLogin().equals(loginToExclude)) continue;
			String login = udata.getLogin();
			String group = udata.getGroup();
			String IP = isPresent(udata) ? udata.getAddress() : "--";
			Object[] vector = new Object[]{ udata, login, group, IP };
			model.addRow(vector);
			if(!groups.contains(group)) groups.add(group);
		}

		// Modèle de rendu
		UserCellRenderer renderer = new UserCellRenderer();
		table.getColumnModel().getColumn(0).setCellRenderer(renderer);
		table.getColumnModel().getColumn(1).setCellRenderer(renderer);
		table.getColumnModel().getColumn(2).setCellRenderer(renderer);
		table.getColumnModel().getColumn(3).setCellRenderer(renderer);

		// Modèle de tri
		sorter = new TableRowSorter<TableModel>(model);
		sorter.setSortsOnUpdates(true);

		// Comparateur sur la colonne nom
		sorter.setComparator(0, new Comparator<UserData>() {
			public int compare(UserData udata1, UserData udata2) {
				boolean b1 = isPresent(udata1);
				boolean b2 = isPresent(udata2);
				if (b1 == b2) return udata1.getName().compareTo(udata2.getName());
				else return Boolean.compare(b2, b1);
			}
		});

		// Comparateur sur la colonne adresse IP
		sorter.setComparator(3, new Comparator<String>() {
			public int compare(String addr1, String addr2) {
				boolean no_addr1 = addr1.equals("--");
				boolean no_addr2 = addr2.equals("--");
				if (no_addr1 && no_addr2) return 0;
				if (no_addr1) return 999;
				if (no_addr2) return -999;
				String[] ip1 = addr1.split("\\.");
				String[] ip2 = addr2.split("\\.");
				String ipFormatted1 = String.format("%3s.%3s.%3s.%3s", ip1[0],ip1[1],ip1[2],ip1[3]);
				String ipFormatted2 = String.format("%3s.%3s.%3s.%3s",  ip2[0],ip2[1],ip2[2],ip2[3]);
				return ipFormatted1.compareTo(ipFormatted2);
			}
		});

		table.setRowSorter(sorter);
		table.getRowSorter().toggleSortOrder(0); // Trier sur les noms au démarrage

		// Modèle de sélection
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// Sélectionner la première ligne avant d'attacher le ListSelectionListener
		if (model.getRowCount() > 0)
			table.changeSelection(0, 0, false, false);
		// Ajouter un listener de sélection
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				int row = table.getSelectedRow();
				if (!event.getValueIsAdjusting() && row > -1) {
					UserData udata = (UserData) table.getValueAt(row, 0);
					editor.changeUser(udata);
					editor.getManager().invokeLater_updateUI(); // Pour adapter la largeur des labels...
					updateMenuItems();
				}
			}
		});

		// Menu popup
		UserPopupMenu popupMenu = new UserPopupMenu();
		table.setComponentPopupMenu(popupMenu);
		itemRemoveFilter.setEnabled(false); // Pas de filtre au début
		updateMenuItems(); // Une première fois

		// Juste des informations
		info_0.setEnabled(false);
		info_1.setEnabled(false);
		info_2.setEnabled(false);
		info_3.setEnabled(false);
		info_4.setEnabled(false);
		info_5.setEnabled(false);

		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setBorder(BorderFactory.createEmptyBorder());
		setViewportView(table);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private UserData findUserData(String login) {
		// Trouver un utilisateur
		for (int i = 0; i < table.getRowCount(); i++) {
			UserData udata = (UserData) table.getValueAt(i, 0);
			if (udata.getLogin().equals(login))
				return udata;
		}
		return null;
	}

	private int findRow(String login) {
		// Trouver la ligne d'un utilisateur
		for (int i = 0; i < table.getRowCount(); i++) {
			UserData udata = (UserData) table.getValueAt(i, 0);
			if (udata.getLogin().equals(login))
				return table.convertRowIndexToModel(i);
		}
		return -1;
	}

	private void setFilteredName(String text) {
		// Filtrer sur le nom
		RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
			public boolean include(Entry<? extends Object, ? extends Object> entry) {
				// Le UserData est en colonne 0
				UserData udata = (UserData) entry.getValue(0);
				return udata.getName().toUpperCase().contains(text.toUpperCase());
			}
		};
		sorter.setRowFilter(filter);
		sorter.sort();
	}

	private void setFilteredGroup(String group) {
		// Filtrer sur le groupe
		RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
			public boolean include(Entry<? extends Object, ? extends Object> entry) {
				// Le UserData est en colonne 0
				UserData udata = (UserData) entry.getValue(0);
				return udata.getGroup().equals(group);
			}
		};
		sorter.setRowFilter(filter);
		sorter.sort();
	}

	private void removeFilter() {
		// Réinitialiser le fitrage
		sorter.setRowFilter(null);
		sorter.sort();
	}

	private void updateBackgroundColor() {
		if (openned) table.setBackground(Color.WHITE);
		else table.setBackground(COLOR_CLOSED);
	}

	private boolean isPresent(UserData udata) {
		// Un client présent est connecté dans la session courante
		boolean isconnected = udata.isConnected();
		String session = udata.getSessionId();
		return isconnected && (sessionId.equals(session));
	}

	private void updateMenuItems() {
		int row = table.getSelectedRow();
		UserData udata = (UserData) table.getValueAt(row, 0);
		boolean isstudent = udata.isStudent();
		boolean isconnected = udata.isConnected();
		boolean iscontrolled = udata.isControlled();
		boolean helpFlag = udata.getHelpFlag();
		boolean ispresent = isPresent(udata);

		// Clients autorisés à forcer la déconnection de tous les utilisateurs
		boolean client_Lehuen = codelab.getLogin().equals("lehuen"); // I'm god :)
		boolean client_Admin = codelab.isAdmin();

		itemTchat.setEnabled(ispresent);
		itemControl.setEnabled(isstudent);
		itemResetHelpFlag.setEnabled(ispresent && helpFlag);
		itemResetPasswd.setEnabled(isstudent || client_Admin || client_Lehuen);
		itemDeconnexion.setEnabled((ispresent && isstudent) || client_Admin || client_Lehuen);

		String label0 = openned ? CodeLab.LABEL("UserTable_18b") : CodeLab.LABEL("UserTable_18a");
		String label1 = isconnected ? CodeLab.LABEL("UserTable_0a1") : CodeLab.LABEL("UserTable_0a2");
		String label2 = iscontrolled ? CodeLab.LABEL("UserTable_16b") : CodeLab.LABEL("UserTable_16a");

		//Date date = udata.getDate();
		//String sdate = (date == null) ? "--" : UserData.DATEFORMAT.format(date);
		String sdate = udata.getDate();

		info_0.setText(String.format(label0, sessionId));
		info_1.setText(String.format(label1, udata.getName()));
		info_2.setText(String.format(CodeLab.LABEL("UserTable_0b"), sdate));
		info_3.setText(String.format(CodeLab.LABEL("UserTable_0c"), udata.getAddress()));
		info_4.setText(String.format(CodeLab.LABEL("UserTable_0d"), udata.getSessionId()));
		info_5.setText(String.format(CodeLab.LABEL("UserTable_0e"), udata.getStatut()));

		menuStudent.setText(String.format(CodeLab.LABEL("UserTable_menuStudent"), udata));
		itemControl.setText(String.format(label2, udata));
		itemTchat.setText(String.format(CodeLab.LABEL("UserTable_4"), udata));
		itemDeconnexion.setText(String.format(CodeLab.LABEL("UserTable_5"), udata));
		itemResetPasswd.setText(String.format(CodeLab.LABEL("UserTable_6"), udata));
		itemResetHelpFlag.setText(String.format(CodeLab.LABEL("itemResetCallFlag"), udata));

		// Ouvrir ou fermer la session + Ajouter un étudiant
		String label3 = openned ? CodeLab.LABEL("UserTable_19b") : CodeLab.LABEL("UserTable_19a");
		itemOpenSession.setText(String.format(label3, sessionId));
		itemNewStudent.setText(String.format(CodeLab.LABEL("itemNewStudent"), sessionId));

		// Pour le menu contextuel de l'éditeur
		editor.updateUserData(udata);
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void connect(String login, String address, String date) {
		// Actualisation suite à une connexion
		UserData udata = findUserData(login);
		if (udata == null) return;

		model.setValueAt(address, findRow(login), 3); // Actualiser l'adresse IP
		udata.setConnected(true);
		udata.setSessionID(sessionId);
		udata.setAddress(address);
		udata.setDate(date);
		sorter.sort();
		updateMenuItems();
		table.repaint();
	}

	public void disconnect(String login) {
		// Actualisation suite à une déconnexion
		UserData udata = findUserData(login);
		if (udata == null) return;

		editor.getManager().repaint(); // Pour avertir le CodeLabTreeRenderer
		model.setValueAt("--", findRow(login), 3); // Actualiser l'adresse IP
		udata.setConnected(false);
		udata.setHelpFlag(false);
		udata.setEditedFile(null);
		sorter.sort();
		updateMenuItems();
		table.repaint();
	}

	public void setHelpFlag(String login, boolean value) {
		// Actualisation d'un flag d'aide
		UserData udata = findUserData(login);
		if (udata != null) {
			udata.setHelpFlag(value);
			updateMenuItems();
			table.repaint();
		}
	}

	public void setControlMode(String login, boolean value) {
		// Actualisation suite à une prise de contrôle
		UserData udata = findUserData(login);
		if (udata != null) {
			udata.setControlled(value);
			updateMenuItems();
			table.repaint();
		}
	}

	public void setEditedFile(String login, String path) {
		// Un utilisateur a changé de fichier
		UserData udata = findUserData(login);
		if (udata != null) {
			udata.setEditedFile(path);
		}
	}

	/*
	public void controlEverybody() {
		// Pour prendre la main sur tous les clients
		for (int i = 0; i < table.getRowCount(); i++) {
			UserData udata = (UserData) table.getValueAt(i, 0);
			if (udata.isStudent() && !udata.isControlled()) {
				codelab.askControlMode(udata.getLogin(), true);
			}
		}
	}
	*/

	public void disableAllControled() {
		// Pour redonner la main aux clients controlés
		for (int i = 0; i < table.getRowCount(); i++) {
			UserData udata = (UserData) table.getValueAt(i, 0);
			if (udata.isControlled()) {
				codelab.getClient().askControlMode(udata.getLogin(), false);
				Utils.wait(100); // Une petite pause entre chaque demande
			}
		}
	}

	public void newStudent(UserData udata) {
		// Un nouvel étudiant a été créé à la volée
		SwingUtilities.invokeLater(() -> {
			// Ajouter le student dans la table
			String login = udata.getLogin();
			Object[] vector = new Object[]{ udata, login, "", "--"};
			model.addRow(vector);
			table.repaint();
		});
		// Créer le dossier local du student
		String base = CodeLab.PROG_DIST_FOLDER;
		String path2login = String.format("%s/%s", base, udata.getLogin());
		new File(path2login).mkdir();
	}

	public UserData getSelectedUser() {
		try {
			int row = table.getSelectedRow();
			return (UserData) table.getValueAt(row, 0);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	// Pour retrouver le dernier fichier consulté par un tuteur pour un student donné
	private HashMap<String, File> lastFiles = new HashMap<String, File>();

	public File getLastFile(String login) {
		return lastFiles.get(login);
	}

	public void setLastFile(String login, File file) {
		lastFiles.put(login, file);
	}

	public void adjustColumnsWidth() {
		// Invoqué également par reinit() de ModuleEditor
		TableColumnModel model = table.getColumnModel();
		int nbColumns = (int) Arrays.stream(visible).filter(flag -> flag).count();
		int width = table.getWidth() / nbColumns;
		for (int i = 0; i < table.getColumnCount(); i++) {
			TableColumn column = model.getColumn(i);
			if (visible[i]) {
				column.setMinWidth(20);
				column.setMaxWidth(800);
				column.setPreferredWidth(width);
				column.setWidth(width);
			} else {
				column.setMinWidth(0);
				column.setMaxWidth(0);
				column.setWidth(0);
			}
		}
	}

	///////////////////////////////////////////////////
	// Classe interne du renderer
	///////////////////////////////////////////////////

	private class UserCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			UserData udata = (UserData) table.getValueAt(row, 0);
			boolean iscontrolled = udata.isControlled();
			boolean helpFlag = udata.getHelpFlag();

			// Couleur du fond (selon si la session est ouverte ou fermée)
			if (openned) setBackground(Color.WHITE);
			else setBackground(COLOR_CLOSED);
			if (isSelected) setBackground(VERY_LIGHT_GREY);
			if (iscontrolled) setBackground(Color.YELLOW);
			if (iscontrolled && isSelected) setBackground(new Color(250, 234, 115));

			// Couleur du texte si l'utilisateur est présent (connecté dans la session courante)
			if (isPresent(udata)) {
				if (helpFlag) setForeground(Color.RED);
				else setForeground(Color.BLACK);
			}
			else setForeground(Color.GRAY);

			// Position dans la cellule
			if (column == 0) setHorizontalAlignment(JLabel.LEFT);
			if (column == 1) setHorizontalAlignment(JLabel.CENTER);
			if (column == 2) setHorizontalAlignment(JLabel.CENTER);
			if (column == 3) setHorizontalAlignment(JLabel.CENTER);

			// Ajout d'un Tootip
			//((JLabel)comp).setToolTipText(String.format("Connected from %s", udata.getAddress()));
			return comp;
		}
	}

	///////////////////////////////////////////////////
	// Classe interne du menu
	///////////////////////////////////////////////////

	private class UserPopupMenu extends JPopupMenu {

		public UserPopupMenu() {

			// Ouvrir ou fermer la session
			itemOpenSession.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					openned = !openned;
					codelab.getClient().setSessionOpenned(openned);
					updateMenuItems();
					updateBackgroundColor();
					editor.getEditor().repaint(); // Pour le cadenas
				}
			});

			// Ajouter un nouvel étudiant
			itemNewStudent.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					NewStudentDialog nsd = new NewStudentDialog(sessionId);
					if (nsd.cancelled()) return;

					String ulogin = nsd.getLogin();
					String uname = nsd.getName();
					if (!NewStudentDialog.isValidIdent(ulogin)) {
						Utils.showWarningDialog("LOGIN ERROR", CodeLab.LABEL("NewStudentDialog_5"));
						return;
					}
					if (uname.isEmpty()) uname = "unnamed";
					codelab.getClient().addNewStudent(ulogin, uname);
					// L'ajout effectif dans la table doit venir du serveur
				}
			});

			// Changer le password tuteur
			itemResetTutorPasswd.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					NewpassDialog npd = new NewpassDialog();
					String password = npd.getPassword();
					codelab.getClient().changePassword(password);
				}
			});

			// Ouvrir une discussion
			itemTchat.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = getSelectedUser();
					if (udata == null) return;
					ChatController.openChatWith(udata);
					codelab.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			// Baisser un flag d'appel
			itemResetHelpFlag.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = getSelectedUser();
					if (udata == null) return;
					codelab.getClient().resetHelpFlag(udata.getLogin());
				}
			});

			// Prendre ou redonner le contrôle
			itemControl.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = getSelectedUser();
					if (udata == null) return;
					boolean value = udata.isControlled();
					codelab.getClient().askControlMode(udata.getLogin(), !value);
				}
			});

			/*
			// Prendre le contrôle sur tout le monde
			itemEnableControlAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					controlEverybody();
				}
			});
			*/

			// Rendre la main à tout le monde
			itemDisableControlAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					disableAllControled();
				}
			});

			// Envoyer un message à tout le monde
			itemMessage.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = JOptionPane.showInputDialog(
						CodeLab.FRAME,
						CodeLab.LABEL("itemMessage_text"),
						"MESSAGE",
						JOptionPane.QUESTION_MESSAGE);
					if(msg == null || msg.isBlank()) return;
					codelab.getClient().messageToAll(msg);
				}
			});

			// Réintialiser le mot de passe
			itemResetPasswd.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = getSelectedUser();
					if (udata == null) return;
					String message = String.format(CodeLab.LABEL("UserTable_10"), udata);
					if (Utils.confirmDialog(message))
						codelab.getClient().resetPassword(udata.getLogin());
				}
			});

			// Forcer une déconnection
			itemDeconnexion.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					UserData udata = getSelectedUser();
					if (udata == null) return;
					String message = String.format(CodeLab.LABEL("UserTable_11"), udata);
					if (Utils.confirmDialog(message))
						codelab.getClient().forceDisconnect(udata.getLogin());
				}
			});

			// Déconnecter tout le monde
			itemDeconnexionAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String message = CodeLab.LABEL("UserTable_12");
					if (Utils.confirmDialog(message))
						codelab.getClient().forceDisconnectAll();
				}
			});

			// Affichage des logins
			JCheckBoxMenuItem itemLogin = new JCheckBoxMenuItem(CodeLab.LABEL("itemLogin"), visible[1]);
			itemLogin.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					visible[1] = itemLogin.getState();
					adjustColumnsWidth();
				}
			});

			// Affichage des groupes
			JCheckBoxMenuItem itemGroup = new JCheckBoxMenuItem(CodeLab.LABEL("itemGroup"), visible[2]);
			itemGroup.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					visible[2] = itemGroup.getState();
					adjustColumnsWidth();
				}
			});

			// Affichage des adresses
			JCheckBoxMenuItem itemAddr = new JCheckBoxMenuItem(CodeLab.LABEL("itemAddr"), visible[3]);
			itemAddr.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					visible[3] = itemAddr.getState();
					adjustColumnsWidth();
				}
			});

			// Filtrer sur les noms
			itemNameFilter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String text = JOptionPane.showInputDialog(
						null,
						CodeLab.LABEL("UserTable_14"),
						CodeLab.LABEL("UserTable_13"),
						JOptionPane.PLAIN_MESSAGE);
					if (text == null) return;
					setFilteredName(text);
					itemRemoveFilter.setEnabled(true);
				}
			});

			// Filtrer sur les groupes
			itemGroupFilter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String[] options = groups.toArray(String[]::new);
					String choice = (String) JOptionPane.showInputDialog(
						null,
						CodeLab.LABEL("UserTable_15"),
						CodeLab.LABEL("UserTable_13"),
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[0]);
					if (choice == null) return;
					setFilteredGroup(choice);
					itemRemoveFilter.setEnabled(true);
				}
			});

			// Supprimer les filtres
			itemRemoveFilter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					removeFilter();
					itemRemoveFilter.setEnabled(false);
				}
			});

			// Construction du menu

			menuStudent.add(itemTchat);
			menuStudent.add(itemResetHelpFlag);
			menuStudent.add(itemControl);
			menuStudent.add(itemResetPasswd);
			menuStudent.add(itemDeconnexion);

			menuGroup.add(itemMessage);
//			menuGroup.add(itemEnableControlAll);
			menuGroup.add(itemDisableControlAll);
			menuGroup.add(itemDeconnexionAll);

			menuConfig.add(itemLogin);
			menuConfig.add(itemGroup);
			menuConfig.add(itemAddr);
			menuConfig.add(itemNameFilter);
			menuConfig.add(itemGroupFilter);
			menuConfig.add(itemRemoveFilter);

			add(info_0);
			add(itemOpenSession);
			add(itemNewStudent);
			add(new JSeparator());
			add(info_1);
			add(info_2);
			add(info_3);
			add(info_4);
			//add(info_5);
			add(new JSeparator());
			add(menuStudent);
			add(menuGroup);
			add(new JSeparator());
			add(menuConfig);
			add(itemResetTutorPasswd);
		}
	}
}

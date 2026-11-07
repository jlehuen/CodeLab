package codelab.modules.editeur.usertable;

import codelab.utils.IpLocationService;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JScrollPane;
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

import com.formdev.flatlaf.ui.FlatMenuItemUI;

import codelab.CodeLab;
import codelab.client.UserData;
import codelab.modules.editeur.ModuleEditor;
import codelab.utils.Utils;

/**
*	Classe de la table des utlisateurs
*	@author Jérôme Lehuen
*	@version 09/10/26
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
	private boolean readOnly;

	public boolean isSessionClosed() {
		return !openned;
	}

	public boolean isOpenned() {
		return openned;
	}

	public String getSessionId() {
		return sessionId;
	}

	public List<String> getGroups() {
		return groups;
	}

	public ModuleEditor getEditor() {
		return editor;
	}

	public JTable getTable() {
		return table;
	}

	public UserTableMenu getUserTableMenu() {
		return menu;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean flag) {
		this.readOnly = flag;
		updateMenuItems();
		updateBackgroundColor();
		table.repaint();
	}

	public void setOpenned(boolean flag) {
		this.openned = flag;
		updateMenuItems();
		updateBackgroundColor();
		editor.getEditor().repaint();
		table.repaint();
	}

	// Visibilité des colonnes
	private boolean AFF_NAME = true;
	private boolean AFF_LOGIN = true;
	private boolean AFF_GROUP = false;
	private boolean AFF_ADRESS = true;
	private boolean AFF_ALERT = true;
	private Boolean visible[] = { AFF_NAME, AFF_LOGIN, AFF_GROUP, AFF_ADRESS, AFF_ALERT };

	public boolean isColumnVisible(int col) {
		if (col >= 0 && col < visible.length) return visible[col];
		return false;
	}

	public void setColumnVisible(int col, boolean val) {
		if (col >= 0 && col < visible.length) visible[col] = val;
	}

	// https://www.w3schools.com/colors/colors_picker.asp
	public static final Color VERY_LIGHT_GREY = new Color(212,212,212);
	public static final Color COLOR_CLOSED = new Color(255, 230, 230);
	public static final Color COLOR_CLOSED_FG = new Color(175, 25, 25);
	public static final Color COLOR_READONLY = new Color(220, 235, 252);
	public static final Color COLOR_READONLY_FG = new Color(10, 55, 120);
	public static final Color COLOR_OPEN = new Color(225, 248, 225);
	public static final Color COLOR_OPEN_FG = new Color(20, 105, 35);

	/**
	 * Fabrique un élément de menu stylisé pour le choix d'une session (fond pastel + texte coloré contrasté).
	 */
	public static JMenuItem createSessionMenuItem(String sId, boolean isActive, boolean isOpen, boolean isReadOnly, ActionListener listener) {
		JMenuItem item = new JMenuItem(isActive ? "=> " + sId : sId) {
			private static final long serialVersionUID = 1L;

			@Override
			public void updateUI() {
				setUI(new FlatMenuItemUI() {
					@Override
					public void paint(Graphics g, JComponent c) {
						this.disabledForeground = c.getForeground();
						super.paint(g, c);
					}
				});
			}
		};
		item.setEnabled(!isActive);
		item.setOpaque(true);

		if (!isOpen) {
			item.setBackground(COLOR_CLOSED);
			item.setForeground(COLOR_CLOSED_FG);
		} else if (isReadOnly) {
			item.setBackground(COLOR_READONLY);
			item.setForeground(COLOR_READONLY_FG);
		} else {
			item.setBackground(COLOR_OPEN);
			item.setForeground(COLOR_OPEN_FG);
		}

		if (listener != null) {
			item.addActionListener(listener);
		}
		return item;
	}

	// Menu contextuel
	private UserTableMenu menu;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public UserTable(ModuleEditor editor, List<UserData> ulist, String sessionId, boolean openned) {
		this(editor, ulist, sessionId, openned, false);
	}

	public UserTable(ModuleEditor editor, List<UserData> ulist, String sessionId, boolean openned, boolean readOnly) {

		this.sessionId = sessionId;
		this.openned = openned;
		this.readOnly = readOnly;
		this.editor = editor;

		// Créer la table
		table = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Point getToolTipLocation(MouseEvent event) {
				if (event != null) {
					// Décaler l'info-bulle vers la droite pour éviter qu'en descendant la souris
					// celle-ci ne passe dessus et ne bloque les événements / listeners de la table
					return new Point(event.getX() + 35, event.getY());
				}
				return super.getToolTipLocation(event);
			}
		};
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
		model.addColumn(CodeLab.LABEL("UserTable_t5")); // Alertes
		table.setModel(model);
		String loginToExclude = codelab.getLogin();
		for (UserData udata : ulist) {
			if (udata.getLogin().equals(loginToExclude)) continue;
			String login = udata.getLogin();
			String group = udata.getGroup();
			String IP = isPresent(udata) ? udata.getAddress() : "--";
			String alertStr = formatAlertDisplay(udata);
			Object[] vector = new Object[]{ udata, login, group, IP, alertStr };
			model.addRow(vector);
			if(!groups.contains(group)) groups.add(group);
		}

		// Modèle de rendu
		UserCellRenderer renderer = new UserCellRenderer();
		table.getColumnModel().getColumn(0).setCellRenderer(renderer);
		table.getColumnModel().getColumn(1).setCellRenderer(renderer);
		table.getColumnModel().getColumn(2).setCellRenderer(renderer);
		table.getColumnModel().getColumn(3).setCellRenderer(renderer);
		table.getColumnModel().getColumn(4).setCellRenderer(renderer);

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

		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int r = table.rowAtPoint(e.getPoint());
					if (r >= 0 && r < table.getRowCount()) {
						table.setRowSelectionInterval(r, r);
					}
					updateMenuItems();
				}
			}
		});

		// Menu popup
		menu = new UserTableMenu(this);
		table.setComponentPopupMenu(menu);
		table.getTableHeader().setComponentPopupMenu(menu.getHeaderPopupMenu());
		table.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
					updateMenuItems();
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					updateMenuItems();
				}
			}
		});
		updateMenuItems(); // Une première fois

		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setBorder(BorderFactory.createEmptyBorder());
		setViewportView(table);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	public UserData findUserData(String login) {
		if (login == null || login.isBlank()) return null;
		// Recherche prioritaire dans le modèle complet (insensible aux filtres de vue)
		if (model != null) {
			for (int i = 0; i < model.getRowCount(); i++) {
				Object obj = model.getValueAt(i, 0);
				if (obj instanceof UserData) {
					UserData udata = (UserData) obj;
					if (login.equals(udata.getLogin()))
						return udata;
				}
			}
		}
		// Recherche de secours sur les lignes visibles de la table
		for (int i = 0; i < table.getRowCount(); i++) {
			UserData udata = (UserData) table.getValueAt(i, 0);
			if (udata != null && login.equals(udata.getLogin()))
				return udata;
		}
		return null;
	}

	public String getStudentName(String login) {
		UserData udata = findUserData(login);
		if (udata != null && udata.getName() != null && !udata.getName().isBlank()) {
			return udata.getName().trim();
		}
		return null;
	}

	public List<UserData> getAllUsers() {
		List<UserData> list = new ArrayList<>();
		for (int i = 0; i < table.getRowCount(); i++) {
			list.add((UserData) table.getValueAt(i, 0));
		}
		return list;
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

	public enum FilterType { NONE, NAME, GROUP, ALERTS }
	private FilterType currentFilterType = FilterType.NONE;
	private String currentFilterValue = null;

	public FilterType getCurrentFilterType() {
		return currentFilterType;
	}

	public String getCurrentFilterValue() {
		return currentFilterValue;
	}

	public void setFilteredName(String text) {
		// Filtrer sur le nom
		currentFilterType = FilterType.NAME;
		currentFilterValue = text;
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

	public void setFilteredGroup(String group) {
		// Filtrer sur le groupe
		currentFilterType = FilterType.GROUP;
		currentFilterValue = group;
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

	public void setFilteredAlerts() {
		// Filtrer sur les alertes
		currentFilterType = FilterType.ALERTS;
		currentFilterValue = null;
		RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
			public boolean include(Entry<? extends Object, ? extends Object> entry) {
				// Le UserData est en colonne 0
				UserData udata = (UserData) entry.getValue(0);
				if (udata == null) return false;
				return udata.hasAlert() || (udata.isConnected() && udata.getSAuth() >= 0.0 && udata.getSAuth() < 40.0);
			}
		};
		sorter.setRowFilter(filter);
		sorter.sort();
	}

	public void removeFilter() {
		// Réinitialiser le filtrage
		currentFilterType = FilterType.NONE;
		currentFilterValue = null;
		sorter.setRowFilter(null);
		sorter.sort();
	}

	public boolean isFiltered() {
		return currentFilterType != FilterType.NONE && sorter != null && sorter.getRowFilter() != null;
	}

	public void updateBackgroundColor() {
		if (!openned) table.setBackground(COLOR_CLOSED);
		else if (readOnly) table.setBackground(COLOR_READONLY);
		else table.setBackground(Color.WHITE);
	}

	public boolean isPresent(UserData udata) {
		// Un client présent est connecté dans la session courante
		boolean isconnected = udata.isConnected();
		String session = udata.getSessionId();
		return isconnected && (sessionId.equals(session));
	}

	public void updateMenuItems() {
		UserData udata = getSelectedUser();
		if (udata != null) {
			editor.updateUserData(udata);
		}
		if (menu != null) {
			menu.updateMenuItems();
		}
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

	public static String formatAlertDisplay(UserData udata) {
		if (udata == null) return "--";
		if (udata.hasAlert()) {
			UserData.LiveAlert highest = udata.getHighestAlert();
			if (highest != null) {
				String label;
				if ("BURST".equalsIgnoreCase(highest.code)) label = "🚨 Burst";
				else if ("PASTE".equalsIgnoreCase(highest.code)) label = "📋 Paste";
				else if ("THRASH".equalsIgnoreCase(highest.code)) label = "⚠️ Blocage";
				else if ("DROP".equalsIgnoreCase(highest.code)) label = "📥 Drop";
				else label = "⚠️ " + highest.code;

				int total = udata.getAlertCount();
				if (total > 1) {
					return String.format("%s (+%d)", label, total - 1);
				}
				return label;
			}
		}
		if (udata.isConnected() && udata.getSAuth() >= 0.0 && udata.getSAuth() < 100.0) {
			if (udata.getSAuth() < 40.0) {
				return String.format(java.util.Locale.US, "⚠️ %.0f%%", udata.getSAuth());
			}
			return String.format(java.util.Locale.US, "%.0f%%", udata.getSAuth());
		}
		return "--";
	}

	public void setUserAlert(String login, String code, String message, String level, String time) {
		UserData udata = findUserData(login);
		if (udata != null) {
			udata.addAlert(code, message, level, time);
			String display = formatAlertDisplay(udata);
			for (int i = 0; i < model.getRowCount(); i++) {
				Object obj = model.getValueAt(i, 0);
				if (obj instanceof UserData && ((UserData) obj).getLogin().equals(login)) {
					model.setValueAt(display, i, 4);
					break;
				}
			}
			updateMenuItems();
			table.repaint();
		}
	}

	public void resetUserAlert(String login, String code) {
		UserData udata = findUserData(login);
		if (udata != null) {
			if (code == null || code.isEmpty() || "ALL".equalsIgnoreCase(code)) {
				udata.clearAlerts();
			} else {
				udata.removeAlert(code);
			}
			String display = formatAlertDisplay(udata);
			for (int i = 0; i < model.getRowCount(); i++) {
				Object obj = model.getValueAt(i, 0);
				if (obj instanceof UserData && ((UserData) obj).getLogin().equals(login)) {
					model.setValueAt(display, i, 4);
					break;
				}
			}
			updateMenuItems();
			table.repaint();
		}
	}

	public void newStudent(UserData udata) {
		// Un nouvel étudiant a été créé à la volée
		SwingUtilities.invokeLater(() -> {
			// Ajouter le student dans la table
			String login = udata.getLogin();
			String alertStr = formatAlertDisplay(udata);
			Object[] vector = new Object[]{ udata, login, "", "--", alertStr};
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

	private int getUsersCountWithIp(String ip) {
		if (ip == null || ip.isBlank() || ip.equals("--")) return 0;
		int count = 0;
		for (int row = 0; row < model.getRowCount(); row++) {
			Object val = model.getValueAt(row, 3);
			if (ip.equals(val)) count++;
		}
		return count;
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

			// Couleur du fond (selon si la session est ouverte, fermée ou en lecture seule)
			if (!openned) setBackground(COLOR_CLOSED);
			else if (readOnly) setBackground(COLOR_READONLY);
			else setBackground(Color.WHITE);
			if (isSelected) setBackground(VERY_LIGHT_GREY);
			if (iscontrolled) setBackground(Color.YELLOW);
			if (iscontrolled && isSelected) setBackground(new Color(250, 234, 115));

			// Couleur du texte si l'utilisateur est présent (connecté dans la session courante)
			if (isPresent(udata)) {
				if (helpFlag) setForeground(Color.RED);
				else setForeground(Color.BLACK);
			}
			else setForeground(Color.GRAY);

			// Position dans la cellule et infobulle
			if (column == 0) {
				setHorizontalAlignment(JLabel.LEFT);
				((JLabel)comp).setToolTipText(udata.buildUserInfoTooltipHtml(isPresent(udata)));
			} else if (column == 3) {
				setHorizontalAlignment(JLabel.CENTER);
				int ipCount = getUsersCountWithIp(udata.getAddress());
				((JLabel)comp).setToolTipText(IpLocationService.buildIpTooltipHtml(udata, isPresent(udata), ipCount));
			} else if (column == 4) {
				setHorizontalAlignment(JLabel.CENTER);
				if (udata.hasAlert()) {
					if ("CRIT".equalsIgnoreCase(udata.getHighestAlertLevel())) setForeground(new Color(200, 20, 20));
					else setForeground(new Color(210, 110, 0));
				} else if (udata.getSAuth() >= 0.0 && udata.getSAuth() < 40.0) {
					setForeground(new Color(200, 20, 20));
				} else if (udata.getSAuth() >= 0.0 && udata.getSAuth() < 75.0) {
					setForeground(new Color(180, 100, 0));
				} else if (udata.getSAuth() >= 75.0) {
					setForeground(new Color(40, 140, 40));
				} else {
					setForeground(Color.GRAY);
				}
				((JLabel)comp).setToolTipText(udata.buildTooltipHtml());
			} else {
				setHorizontalAlignment(JLabel.CENTER);
				((JLabel)comp).setToolTipText(null);
			}
			return comp;
		}
	}
}

package codelab.modules.editeur.usertable;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.formdev.flatlaf.ui.FlatMenuItemUI;

import codelab.CodeLab;
import codelab.client.ChatController;
import codelab.client.CodelabClient;
import codelab.client.UserData;
import codelab.eval.SessionEvaluator;
import codelab.utils.Utils;

/**
 * Menu contextuel (clic droit) de la table des utilisateurs.
 * Organisé pour un accès direct aux actions de l'étudiant sélectionné (1er niveau),
 * tout en regroupant les actions collectives/session et les filtres d'affichage.
 *
 * @author Jérôme Lehuen
 */
public class UserTableMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	private static final Color DANGER_RED = new Color(205, 35, 35);
	private static final Color STATUS_BG = new Color(220, 235, 252); // Bleu clair
	private static final Color STATUS_FG = new Color(10, 55, 120);   // Bleu foncé

	// Couleurs d'état de la session
	private static final Color SESSION_OPEN_BG = UserTable.COLOR_OPEN;       // Vert clair
	private static final Color SESSION_OPEN_FG = new Color(20, 105, 35);     // Vert foncé
	private static final Color SESSION_CLOSED_BG = UserTable.COLOR_CLOSED;   // Rouge clair / pastel
	private static final Color SESSION_CLOSED_FG = new Color(175, 25, 25);   // Rouge foncé
	private static final Color SESSION_RO_BG = UserTable.COLOR_READONLY;     // Jaune clair
	private static final Color SESSION_RO_FG = new Color(135, 80, 0);        // Marron / Jaune foncé

	private final UserTable userTable;
	private final CodeLab codelab = CodeLab.INSTANCE;

	private static JMenuItem createHeaderMenuItem() {
		JMenuItem item = new JMenuItem() {
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
		item.setEnabled(false);
		item.setOpaque(true);
		item.setBackground(STATUS_BG);
		item.setForeground(STATUS_FG);
		return item;
	}

	// En-tête contextuel
	private final JMenuItem itemHeader = createHeaderMenuItem();

	// Actions directes sur l'étudiant sélectionné (1er niveau)
	private final JMenuItem itemTchat = new JMenuItem();
	private final JMenuItem itemControl = new JMenuItem();
	private final JMenuItem itemResetHelpFlag = new JMenuItem();
	private final JMenuItem itemResetUserAlert = new JMenuItem();
	private final JMenu menuEvalStudent = new JMenu();

	// Sous-menu Copier (désactivé)
	/*
	private final JMenu menuCopy = new JMenu(CodeLab.LABEL("UserTable_menuCopy"));
	private final JMenuItem itemCopyLogin = new JMenuItem();
	private final JMenuItem itemCopyName = new JMenuItem();
	private final JMenuItem itemCopyIp = new JMenuItem();
	*/

	// Sous-menu Sécurité & Accès
	private final JMenu menuSecurity = new JMenu(CodeLab.LABEL("UserTable_menuSecurity"));
	private final JMenuItem itemResetPasswd = new JMenuItem();
	private final JMenuItem itemDeconnexion = new JMenuItem();

	// Sous-menu Actions collectives
	private final JMenu menuCohort = new JMenu(CodeLab.LABEL("UserTable_menuCohort"));
	private final JMenuItem itemMessage = new JMenuItem(CodeLab.LABEL("itemMessage"));
	private final JMenuItem itemDisableControlAll = new JMenuItem(CodeLab.LABEL("UserTable_17b"));
	private final JMenu menuEvalSession = new JMenu(CodeLab.LABEL("menuEvalSession"));
	private final JMenuItem itemDeconnexionAll = new JMenuItem(CodeLab.LABEL("UserTable_7"));

	// Sous-menu Session
	private final JMenu menuSession = new JMenu(CodeLab.LABEL("UserTable_menuSession"));
	private final JMenuItem itemSessionStatus = createHeaderMenuItem();
	private final JMenuItem itemOpenSession = new JMenuItem();
	private final JMenuItem itemReadOnlySession = new JMenuItem();
	private final JMenuItem itemNewStudent = new JMenuItem();
	private final JMenu menuChangeSession = new JMenu(CodeLab.LABEL("ServerPopupMenu_4"));

	// Sous-menu Apparence et filtrage
	private final JMenu menuConfig = new JMenu(CodeLab.LABEL("UserTable_menuConfig"));
	private final JMenu menuColumns = new JMenu(CodeLab.LABEL("UserTable_menuColumns"));
	private final JCheckBoxMenuItem itemLogin;
	private final JCheckBoxMenuItem itemGroup;
	private final JCheckBoxMenuItem itemAddr;
	private final JCheckBoxMenuItem itemAlert;
	private final JMenuItem itemNameFilter = new JMenuItem(CodeLab.LABEL("UserTable_8a"));
	private final JMenuItem itemGroupFilter = new JMenuItem(CodeLab.LABEL("UserTable_8b"));
	private final JMenuItem itemRemoveFilter = new JMenuItem(CodeLab.LABEL("UserTable_9"));

	public UserTableMenu(UserTable userTable) {
		this.userTable = userTable;

		// Initialisation des cases à cocher de colonnes
		itemLogin = new JCheckBoxMenuItem(CodeLab.LABEL("itemLogin"), userTable.isColumnVisible(1));
		itemGroup = new JCheckBoxMenuItem(CodeLab.LABEL("itemGroup"), userTable.isColumnVisible(2));
		itemAddr = new JCheckBoxMenuItem(CodeLab.LABEL("itemAddr"), userTable.isColumnVisible(3));
		itemAlert = new JCheckBoxMenuItem(CodeLab.LABEL("UserTable_t5"), userTable.isColumnVisible(4));

		// Style d'alerte rouge pour les actions de déconnexion
		itemDeconnexion.setForeground(DANGER_RED);
		itemDeconnexionAll.setForeground(DANGER_RED);

		initListeners();
		buildMenu();
	}

	private void initListeners() {
		// Ouvrir une discussion
		itemTchat.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				ChatController.openChatWith(udata);
				codelab.getClient().resetHelpFlag(udata.getLogin());
			}
		});

		// Prendre ou redonner le contrôle
		itemControl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				boolean value = udata.isControlled();
				codelab.getClient().askControlMode(udata.getLogin(), !value);
			}
		});

		// Baisser un flag d'appel
		itemResetHelpFlag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				codelab.getClient().resetHelpFlag(udata.getLogin());
			}
		});

		// Acquitter une alerte
		itemResetUserAlert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				codelab.getClient().resetUserAlert(udata.getLogin(), "ALL");
				userTable.resetUserAlert(udata.getLogin(), "ALL");
			}
		});

		/*
		// Copier le login
		itemCopyLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata != null) copyToClipboard(udata.getLogin());
			}
		});

		// Copier le nom
		itemCopyName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata != null) copyToClipboard(udata.getName());
			}
		});

		// Copier l'IP
		itemCopyIp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata != null) copyToClipboard(udata.getAddress());
			}
		});
		*/

		// Réinitialiser le mot de passe
		itemResetPasswd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				String message = String.format(CodeLab.LABEL("UserTable_10"), udata);
				if (Utils.confirmDialog(message)) {
					codelab.getClient().resetPassword(udata.getLogin());
				}
			}
		});

		// Forcer la déconnexion d'un étudiant
		itemDeconnexion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserData udata = userTable.getSelectedUser();
				if (udata == null) return;
				String message = String.format(CodeLab.LABEL("UserTable_11"), udata);
				if (Utils.confirmDialog(message)) {
					codelab.getClient().forceDisconnect(udata.getLogin());
				}
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
				if (msg == null || msg.isBlank()) return;
				codelab.getClient().messageToAll(msg);
			}
		});

		// Rendre la main à tout le monde
		itemDisableControlAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				userTable.disableAllControled();
			}
		});

		// Ouvrir ou fermer la session
		itemOpenSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean newOpenned = !userTable.isOpenned();
				userTable.setOpenned(newOpenned);
				codelab.getClient().setSessionOpenned(newOpenned);
			}
		});

		// Mettre la session en lecture seule ou en écriture
		itemReadOnlySession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean newReadOnly = !userTable.isReadOnly();
				userTable.setReadOnly(newReadOnly);
				codelab.getClient().setSessionReadOnly(newReadOnly);
			}
		});

		// Ajouter un nouvel étudiant
		itemNewStudent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NewStudentDialog nsd = new NewStudentDialog(userTable.getSessionId());
				if (nsd.cancelled()) return;

				String ulogin = nsd.getLogin();
				String uname = nsd.getName();
				if (!NewStudentDialog.isValidIdent(ulogin)) {
					Utils.showWarningDialog("LOGIN ERROR", CodeLab.LABEL("NewStudentDialog_5"));
					return;
				}
				if (uname.isEmpty()) uname = "unnamed";
				codelab.getClient().addNewStudent(ulogin, uname);
			}
		});

		// Déconnecter tout le monde
		itemDeconnexionAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String message = CodeLab.LABEL("UserTable_12");
				if (Utils.confirmDialog(message)) {
					codelab.getClient().forceDisconnectAll();
				}
			}
		});

		// Colonnes
		itemLogin.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				userTable.setColumnVisible(1, itemLogin.getState());
				userTable.adjustColumnsWidth();
			}
		});

		itemGroup.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				userTable.setColumnVisible(2, itemGroup.getState());
				userTable.adjustColumnsWidth();
			}
		});

		itemAddr.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				userTable.setColumnVisible(3, itemAddr.getState());
				userTable.adjustColumnsWidth();
			}
		});

		itemAlert.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				userTable.setColumnVisible(4, itemAlert.getState());
				userTable.adjustColumnsWidth();
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
				userTable.setFilteredName(text);
				itemRemoveFilter.setEnabled(true);
			}
		});

		// Filtrer sur les groupes
		itemGroupFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] options = userTable.getGroups().toArray(String[]::new);
				if (options.length == 0) return;
				String choice = (String) JOptionPane.showInputDialog(
					null,
					CodeLab.LABEL("UserTable_15"),
					CodeLab.LABEL("UserTable_13"),
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]);
				if (choice == null) return;
				userTable.setFilteredGroup(choice);
				itemRemoveFilter.setEnabled(true);
			}
		});

		// Supprimer les filtres
		itemRemoveFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				userTable.removeFilter();
				itemRemoveFilter.setEnabled(false);
			}
		});
	}

	private void buildMenu() {
		add(itemHeader);

		// 1. Actions individuelles sur l'étudiant sélectionné (1er niveau)
		add(itemTchat);
		add(itemResetHelpFlag);
		add(itemResetUserAlert);
		add(itemControl);
		add(menuEvalStudent);

		menuSecurity.add(itemResetPasswd);
		menuSecurity.add(itemDeconnexion);
		add(menuSecurity);

		/*
		menuCopy.add(itemCopyLogin);
		menuCopy.add(itemCopyName);
		menuCopy.add(itemCopyIp);
		add(menuCopy);
		*/

		add(new JSeparator());

		// 2. Actions collectives
		menuCohort.add(itemMessage);
		menuCohort.add(itemDisableControlAll);
		menuCohort.add(menuEvalSession);
		menuCohort.add(new JSeparator());
		menuCohort.add(itemDeconnexionAll);
		add(menuCohort);

		// Changer de session
		add(menuChangeSession);

		// 3. Gestion de la session
		menuSession.add(itemSessionStatus);
		menuSession.add(itemOpenSession);
		menuSession.add(itemReadOnlySession);
		menuSession.add(itemNewStudent);
		add(menuSession);

		add(new JSeparator());

		// 4. Apparence et filtrage
		menuColumns.add(itemLogin);
		menuColumns.add(itemGroup);
		menuColumns.add(itemAddr);
		menuColumns.add(itemAlert);
		menuConfig.add(menuColumns);
		menuConfig.add(itemNameFilter);
		menuConfig.add(itemGroupFilter);
		menuConfig.add(itemRemoveFilter);
		add(menuConfig);

		itemRemoveFilter.setEnabled(false);
	}

	public void updateMenuItems() {
		UserData udata = userTable.getSelectedUser();
		String sessionId = userTable.getSessionId();
		boolean openned = userTable.isOpenned();
		boolean readOnly = userTable.isReadOnly();

		boolean client_Lehuen = codelab.getLogin().equals("lehuen");
		boolean client_Admin = codelab.isAdmin();

		if (udata == null) {
			itemHeader.setText(CodeLab.LABEL("UserTable_noUser"));
			itemTchat.setEnabled(false);
			itemControl.setEnabled(false);
			itemResetHelpFlag.setEnabled(false);
			itemResetUserAlert.setEnabled(false);
			menuEvalStudent.setEnabled(false);
			// menuCopy.setEnabled(false);
			menuSecurity.setEnabled(false);
		} else {
			boolean isstudent = udata.isStudent();
			boolean iscontrolled = udata.isControlled();
			boolean helpFlag = udata.getHelpFlag();
			boolean ispresent = userTable.isPresent(udata);

			itemHeader.setText(udata.toString2());

			itemTchat.setEnabled(ispresent);
			itemTchat.setText(String.format(CodeLab.LABEL("UserTable_4"), udata));

			itemControl.setEnabled(isstudent);
			String labelControl = iscontrolled ? CodeLab.LABEL("UserTable_16b") : CodeLab.LABEL("UserTable_16a");
			itemControl.setText(String.format(labelControl, udata));

			itemResetHelpFlag.setEnabled(ispresent && helpFlag);
			itemResetHelpFlag.setText(String.format(CodeLab.LABEL("itemResetCallFlag"), udata));

			itemResetUserAlert.setEnabled(ispresent && udata.hasAlert());
			if (udata.getAlertCount() > 1) {
				itemResetUserAlert.setText(String.format("Acquitter toutes les alertes de %s (%d)", udata.getName(), udata.getAlertCount()));
			} else {
				itemResetUserAlert.setText(String.format(CodeLab.LABEL("itemResetUserAlert"), udata));
			}

			menuEvalStudent.setEnabled(isstudent);
			menuEvalStudent.setText(String.format(CodeLab.LABEL("menuEvalStudent"), udata.getName()));
			SessionEvaluator.populateSkillsSubmenu(menuEvalStudent, (skill, useAi) -> SessionEvaluator.evaluateStudent(codelab, udata.getLogin(), udata.getName(), skill, useAi));

			/*
			menuCopy.setEnabled(true);
			itemCopyLogin.setText(String.format(CodeLab.LABEL("UserTable_copyLogin"), udata.getLogin()));
			itemCopyName.setText(String.format(CodeLab.LABEL("UserTable_copyName"), udata.getName()));
			boolean hasIp = udata.getAddress() != null && !udata.getAddress().isBlank() && !udata.getAddress().equals("--");
			itemCopyIp.setText(String.format(CodeLab.LABEL("UserTable_copyIp"), hasIp ? udata.getAddress() : "--"));
			itemCopyIp.setEnabled(hasIp);
			*/

			menuSecurity.setEnabled(true);
			itemResetPasswd.setEnabled(isstudent || client_Admin || client_Lehuen);
			itemResetPasswd.setText(String.format(CodeLab.LABEL("UserTable_6"), udata));
			itemDeconnexion.setEnabled((ispresent && isstudent) || client_Admin || client_Lehuen);
			itemDeconnexion.setText(String.format(CodeLab.LABEL("UserTable_5"), udata));
		}

		// Actions collectives
		menuCohort.setText(CodeLab.LABEL("UserTable_menuCohort"));
		menuEvalSession.setText(CodeLab.LABEL("menuEvalSession"));
		SessionEvaluator.populateSkillsSubmenu(menuEvalSession, (skill, useAi) -> SessionEvaluator.evaluateSession(codelab, sessionId, skill, useAi));

		// Session
		menuSession.setText(CodeLab.LABEL("UserTable_menuSession"));
		String statusLabel;
		if (!openned) {
			statusLabel = String.format(CodeLab.LABEL("UserTable_sessionStatus_closed"), sessionId);
			itemSessionStatus.setBackground(SESSION_CLOSED_BG);
			itemSessionStatus.setForeground(SESSION_CLOSED_FG);
		} else if (readOnly) {
			statusLabel = String.format(CodeLab.LABEL("UserTable_sessionStatus_ro"), sessionId);
			itemSessionStatus.setBackground(SESSION_RO_BG);
			itemSessionStatus.setForeground(SESSION_RO_FG);
		} else {
			statusLabel = String.format(CodeLab.LABEL("UserTable_sessionStatus_rw"), sessionId);
			itemSessionStatus.setBackground(SESSION_OPEN_BG);
			itemSessionStatus.setForeground(SESSION_OPEN_FG);
		}
		itemSessionStatus.setText(statusLabel);

		String labelOpen = openned ? CodeLab.LABEL("UserTable_19b") : CodeLab.LABEL("UserTable_19a");
		itemOpenSession.setText(String.format(labelOpen, sessionId));

		String labelReadOnly = readOnly ? CodeLab.LABEL("UserTable_19d") : CodeLab.LABEL("UserTable_19c");
		itemReadOnlySession.setText(String.format(labelReadOnly, sessionId));
		itemReadOnlySession.setEnabled(openned);

		itemNewStudent.setText(String.format(CodeLab.LABEL("itemNewStudent"), sessionId));

		// Sous-menu changement de session
		menuChangeSession.setText(CodeLab.LABEL("ServerPopupMenu_4"));
		menuChangeSession.removeAll();
		CodelabClient client = codelab.getClient();
		if (client != null && client.isConnected()) {
			String[] sessions = client.getSessions();
			String curSession = client.getSession();
			if (curSession == null || curSession.isEmpty()) curSession = sessionId;
			menuChangeSession.setEnabled(client.nbSessions() > 1);
			if (sessions != null) {
				for (String sId : sessions) {
					boolean isOpen = client.isOpen(sId);
					boolean isRo = client.isReadOnly(sId);
					boolean isActive = sId.equals(curSession);
					JMenuItem item = new JMenuItem(sId);
					if (isActive) item.setText("=> " + sId);
					item.setEnabled(!isActive);
					if (!isOpen) item.setBackground(UserTable.COLOR_CLOSED);
					else if (isRo) item.setBackground(UserTable.COLOR_READONLY);
					else item.setBackground(UserTable.COLOR_OPEN);
					item.setOpaque(true);
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							client.actionChangeSession(sId);
						}
					});
					menuChangeSession.add(item);
				}
			}
		} else {
			menuChangeSession.setEnabled(false);
		}
	}

	/*
	private void copyToClipboard(String text) {
		if (text == null || text.isBlank() || text.equals("--")) return;
		try {
			StringSelection selection = new StringSelection(text);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		} catch (Exception ex) {
			ExceptionManager.process(ex);
		}
	}
	*/
}

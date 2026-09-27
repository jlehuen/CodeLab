package codelab.client;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import codelab.CodeLab;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.usertable.UserTable;
import codelab.utils.Utils;

/**
*	Classe du menu popup serveur
*	@author Jérôme Lehuen
*	@version 15/05/25
*/

public class ServerPopupMenu extends JPopupMenu {

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ServerPopupMenu(JComponent component, int x, int y) {

		CodeLab codelab = CodeLab.INSTANCE;
		ModuleEditor editor = codelab.getEditor();
		CodelabClient client = codelab.getClient();

		if (!client.isConnected()) {

			// ---------------------------------------
			// Item connexion

			JMenuItem connexion = new JMenuItem(CodeLab.LABEL("ServerPopupMenu_2"));
			connexion.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					codelab.halt();
					codelab.setSelectedModule(editor);
					codelab.connection();
				}
			});

			// ---------------------------------------
			// Case à cocher connexion automatique

			JCheckBoxMenuItem autologin = new JCheckBoxMenuItem(CodeLab.LABEL("ServerPopupMenu_8"), CodeLab.AUTO_LOGIN);
			autologin.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					boolean value = autologin.getState();
					CodeLab.AUTO_LOGIN = value;
					if (!value) {
						CodeLab.LOGIN = CodeLab.UNDEFINED;
						CodeLab.PASSWD = CodeLab.UNDEFINED;
					}
				}
			});

			add_disable(CodeLab.LABEL("ServerPopupMenu_1"));
			add(new JSeparator());
			add(autologin);
			add(connexion);

		} else {

			String session = client.getSession();
			String[] sessions = client.getSessions();

			add_disable(String.format("Server name: %s", client.getNickName()));
			add_disable(String.format("Server host: %s", client.getHost()));
			add_disable(String.format("Server port: %d", client.getPort()));
			if (CodeLab.PROXY_CONFIGURED) {
				add_disable(String.format("Proxy host: %s", CodeLab.PROXY_HOST));
				add_disable(String.format("Proxy port: %d", CodeLab.PROXY_PORT));
			}
			add_disable(String.format("Client login: %s", client.getLogin()));
			add_disable(String.format("Client statut: %s", client.getStatut()));
			add_disable(String.format("Client name: %s", client.getUsername()));
			add_disable(String.format("Client session: %s", session));
			add(new JSeparator());

			// Sou-menu changement de session
			JMenu item4 = new JMenu(CodeLab.LABEL("ServerPopupMenu_4"));
			item4.setEnabled(client.nbSessions() > 1);
			for (String sessionId : sessions) {
				boolean isOpen =  client.isOpen(sessionId);
				boolean isActive = sessionId.equals(session);
				JMenuItem item = new JMenuItem(sessionId);
				if (isActive) item.setText("=> " + sessionId);
				item.setEnabled(!isActive); // Disable la session courante
				item.setBackground(isOpen ? Color.WHITE : UserTable.COLOR_CLOSED); // Couleur session fermée
				item.setOpaque(true); // Sinon pas de couleur de fond
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						client.actionChangeSession(sessionId);
					}
				});
				item4.add(item);
			}
			add(item4);

			// Item administration
			if (client.getStatut() == Statut.ADMIN) {
				JMenuItem item7 = new JMenuItem(CodeLab.LABEL("ServerPopupMenu_7"));
				item7.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						Utils.openBrowser(CodeLab.ADMIN_URL);
					}
				});
				add(item7);
			}

			// Item déconnexion
			JMenuItem item3 = new JMenuItem(CodeLab.LABEL("ServerPopupMenu_3"));
			item3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					//AdminFrame.close();
					client.closeConnection();
				}
			});
			add(item3);
		}
		add(new JSeparator());

		// Item information client
		JMenuItem item5 = new JMenuItem(CodeLab.LABEL("ServerPopupMenu_5"));
		item5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Utils.openBrowser(String.format(CodeLab.INFOCLIENT_URL, CodeLab.LANG, CodeLab.LANG));
			}
		});
		add(item5);

        show(component, x, y);
    }

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void add_disable(String str) {
		JMenuItem item = new JMenuItem(str);
		item.setEnabled(false);
		add(item);
	}
}

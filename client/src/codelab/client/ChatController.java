package codelab.client;

import java.util.HashMap;

/**
*	Classe du gestionnaire de discussions
*	@author Jérôme Lehuen
*	@version 22/05/25
*/

public class ChatController {

	// Table de hachage des fenêtres de discussion par login
	private static HashMap<String, ChatGUI> hashMapChatGUI = new HashMap<String, ChatGUI>();

	private static ChatGUI openChat(String login, String fullname) {
		ChatGUI chat;
		if (hashMapChatGUI.containsKey(login)) {
			chat = hashMapChatGUI.get(login);
			chat.setVisible(true);
		} else {
			chat = new ChatGUI(login, fullname);
			hashMapChatGUI.put(login, chat);
		}
        return chat;
	}

    public static void openChatWith(UserData udata) {
        openChat(udata.getLogin(), udata.getName());
    }

    public static void chatFrom(String login, String fullname, String msg) {
        openChat(login, fullname).appendIncomingMessage(msg);
    }

	public static void closeAll() {
		for (ChatGUI chat : hashMapChatGUI.values())
			chat.setVisible(false);
	}
}

package codelab.client;

import java.net.Socket;

/**
*	Façade du serveur CodeLab qui contient
*	toutes les commandes envoyées au serveur
*	@author Jérôme Lehuen
*	@version 17/06/25
*/

public class ServerFacade extends AbstractServerFacade {

	ServerFacade(Socket socket, CodelabClient client) {
		super(socket, client);
	}

	///////////////////////////////////////////////////
	// Commandes à destination du serveur
	///////////////////////////////////////////////////

	public void ping() {
		Object[] data = {};
		sendCommand("PING", data);
	}

	public void bidule(String arg1, int arg2, boolean arg3) {
		Object[] data = {arg1, arg2, arg3};
		sendCommand("BIDULE", data);
	}

	public void forceDisconnect(String user_login) {
		Object[] data = {user_login};
		sendCommand("FORCE_DISCONNECT", data);
	}

	public void forceDisconnectAll() {
		Object[] data = {};
		sendCommand("FORCE_DISCONNECT_ALL", data);
	}

	public void resetPassword(String user_login) {
		Object[] data = {user_login};
		sendCommand("RESET_PASSWORD", data);
	}

	public void changePassword(String pass_SHA256) {
		Object[] data = {pass_SHA256};
		sendCommand("CHANGE_PASSWORD", data);
	}
	
	public void changeSession(String sessionId) {
		Object[] data = {sessionId};
		sendCommand("CHANGE_SESSION", data);
	}

	public void addNewStudent(String new_login, String new_name) {
		Object[] data = {new_login, new_name};
		sendCommand("ADD_NEW_STUDENT", data);
	}

	public void chatTo(String dest_login, String msg) {
		Object[] data = {dest_login, msg};
		sendCommand("CHAT_TO", data);
	}

	public void messageTo(String dest_login, String msg) {
		Object[] data = {dest_login, msg};
		sendCommand("MESSAGE_TO", data);
	}

	public void messageToAll(String msg) {
		Object[] data = {msg};
		sendCommand("MESSAGE_TO_ALL", data);
	}

	public void messageGlobal(String msg) {
		Object[] data = {msg};
		sendCommand("MESSAGE_GLOBAL", data);
	}

	public void setSessionOpenned(boolean flag) {
		Object[] data = {flag};
		sendCommand("SET_SESSION_OPENNED", data);
	}

	public void setEditedFile(String path) {
		Object[] data = {path};
		sendCommand("SET_EDITED_FILE", data);
	}

	public void setHelpFlag(boolean flag) {
		Object[] data = {flag};
		sendCommand("SET_HELP_FLAG", data);
	}

	public void resetHelpFlag(String user_login) {
		Object[] data = {user_login};
		sendCommand("RESET_HELP_FLAG", data);
	}

	public void createFolder(String path) {
		Object[] data = {path};
		sendCommand("NEW_FOLDER", data);
	}

	public void newFile(String path) {
		Object[] data = {path};
		sendCommand("NEW_FILE", data);
	}

	public void deleteFile(String path) {
		Object[] data = {path};
		sendCommand("DELETE_FILE", data);
	}

	public void renameFile(String path, String new_name) {
		Object[] data = {path, new_name};
		sendCommand("RENAME_FILE", data);
	}

	public void moveFile(String file_path, String dest_path) {
		Object[] data = {file_path, dest_path};
		sendCommand("MOVE_FILE", data);
	}

	public void requestFile(String serverPath, String clientPath) {
		Object[] data = {serverPath, clientPath};
		sendCommand("REQUEST_FILE", data);
	}

	public void uploadFile(byte[] content, String filename) {
		Object[] data = {filename, content};
		sendCommand("UPLOAD_FILE", data);
	}

	public void log(String texte) {
		// Inutilisé pour l'instant
		Object[] data = {texte};
		sendCommand("LOG", data);
	}

	public void exceptionReport(String content) {
		Object[] data = {content};
		sendCommand("EXCEPTION_REPORT", data);
	}

	public void askControlMode(String user_login, boolean flag) {
		Object[] data = {user_login, flag};
		sendCommand("ASK_CONTROL_MODE", data);
	}
	
	public void uploadControlledFile(byte[] content, String filename, String user_login) {
		Object[] data = {filename, content, user_login};
		sendCommand("UPLOAD_CONTROLLED_FILE", data);
	}
}

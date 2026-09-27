package codelab.client;

import java.util.Map;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
*	Classe des descripteurs d'utilisateur
*	@author Jérôme Lehuen
*	@version 11/05/25
*/

public class UserData {

	public static DateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yy (HH:mm:ss)");

	private String login;
	private Statut statut;
	private String groups;
	private String fullname;
	private String mail;
	private String date;
	private String addr;
	private String sessionId;
	private String editedFile;
	private boolean helpFlag = false;
	private boolean connected = false;
	private boolean controlled = false;

	///////////////////////////////////////////////////
	// Constructeurs de UserData
	///////////////////////////////////////////////////

	public UserData(
			String login, Statut statut, String groups, String fullname,
			String mail, String date, String addr, String sessionId, String editedFile,
			boolean connected, boolean helpFlag) {
		this.login = login;
		this.statut = statut;
		this.groups = groups;
		this.fullname = fullname;
		this.mail = mail;
		this.date = date;
		this.addr = addr;
		this.sessionId = sessionId;
		this.editedFile = editedFile;
		this.connected = connected;
		this.helpFlag = helpFlag;
	}

	public UserData(Map<String, Object> map) {
        this.login = (String) map.get("login");
        this.statut = Statut.str2statut((String) map.get("status"));
        this.groups = (String) map.get("groups");
        this.fullname = (String) map.get("fullname");
        this.mail = (String) map.get("mail");
        this.date = (String) map.get("date");
        this.addr = (String) map.get("addr");
        this.sessionId = (String) map.get("session_id");
		this.editedFile = (String) map.get("edited_file");
		this.connected = (boolean) map.get("connected");
		this.helpFlag = (boolean) map.get("help_flag");
    }

	///////////////////////////////////////////////////
	// Pour convertir un Object[] data en UserData[]
	///////////////////////////////////////////////////

	public static UserData[] buildArray(Object[] data) {
		return Arrays.stream(data)
			.map(UserData::from)
			.toArray(UserData[]::new);
	}

    @SuppressWarnings("unchecked")
    public static UserData from(Object data) {
		// Convertit un Map<String, Object> en UserData
		return new UserData((Map<String, Object>) data);
    }

	///////////////////////////////////////////////////
	// Méthodes pour afficher un UserData
	///////////////////////////////////////////////////

	public String toString() {
		if (isAdmin()) return fullname + " [admin]";
		if (isTutor()) return fullname + " [tutor]";
		return fullname;
	}

	public String toString2() {
		return String.format("%s (%s)%s", fullname, login, isAdmin() ? " A" : isTutor() ? " T" : "");
	}

	public String toString3() {
	    String fstr = "    user: login=%s statut=%s groups=[%s] fullname=\"%s\" mail=%s addr=%s sessionId=%s date=%s editedFile=%s connected=%s helpFlag=%s";
	    return String.format(fstr, login, statut, groups, fullname, mail, addr, sessionId, date, editedFile, connected, helpFlag);
	}

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public String getLogin() { return login; }
	public String getName() { return fullname; }
	public String getGroup() { return groups; }
	public Statut getStatut() { return statut; }
	public String getDate() { return date; }
	public String getAddress() { return addr; }
	public String getSessionId() { return sessionId; }
	public String getEditedFile() { return editedFile; }
	public boolean getHelpFlag() { return helpFlag; }

	public boolean isTutor() { return statut.isTutor(); }
	public boolean isAdmin() { return statut.isAdmin(); }
	public boolean isStudent() { return statut.isStudent(); }
	public boolean isConnected() { return connected; }
	public boolean isControlled() { return controlled; }

	public void setDate(String date) { this.date = date; }
	public void setAddress(String address) { this.addr = address; }
	public void setEditedFile(String name) { this.editedFile = name; }
	public void setSessionID(String session) { sessionId = session; }
	public void setConnected(boolean value) { connected = value; }
	public void setControlled(boolean value) { controlled = value; }

	public void setHelpFlag(boolean value) { helpFlag = value; }
}

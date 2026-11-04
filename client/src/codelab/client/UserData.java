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
	private double sAuth = -1.0;
	private double iNav = 0.0;
	private Map<String, LiveAlert> activeAlerts = new java.util.LinkedHashMap<>();

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
		if (map.containsKey("s_auth") && map.get("s_auth") != null) {
			this.sAuth = ((Number) map.get("s_auth")).doubleValue();
		}
		if (map.containsKey("i_nav") && map.get("i_nav") != null) {
			this.iNav = ((Number) map.get("i_nav")).doubleValue();
		}
		Object ajson = map.get("alerts_json");
		if (ajson instanceof String && !((String) ajson).isBlank()) {
			parseAlertsJson((String) ajson);
		} else if (map.containsKey("alert_code") && map.get("alert_code") != null) {
			String c = (String) map.get("alert_code");
			String m = map.get("alert_msg") != null ? (String) map.get("alert_msg") : "";
			String l = map.get("alert_level") != null ? (String) map.get("alert_level") : "WARN";
			if (!c.isBlank()) addAlert(c, m, l, "");
		}
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
	public String getMail() { return mail; }
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

	///////////////////////////////////////////////////
	// Gestion des alertes multi-niveaux en temps réel
	///////////////////////////////////////////////////

	public static class LiveAlert {
		public String code;
		public String message;
		public String level; // "CRIT", "WARN", "INFO"
		public String timestamp;

		public LiveAlert(String code, String message, String level, String timestamp) {
			this.code = (code != null) ? code : "";
			this.message = (message != null) ? message : "";
			this.level = (level != null) ? level : "WARN";
			this.timestamp = (timestamp != null) ? timestamp : "";
		}

		public boolean isCritical() {
			return "CRIT".equalsIgnoreCase(level);
		}

		public String getShortTime() {
			if (timestamp != null && timestamp.length() >= 19) {
				return timestamp.substring(11, 19);
			}
			return (timestamp != null) ? timestamp : "";
		}
	}

	public void addAlert(String code, String msg, String level, String time) {
		if (code != null && !code.isBlank()) {
			activeAlerts.put(code, new LiveAlert(code, msg, level, time));
		}
	}

	public void removeAlert(String code) {
		if (code != null) {
			activeAlerts.remove(code);
		}
	}

	public void clearAlerts() {
		activeAlerts.clear();
	}

	public boolean hasAlert() {
		return !activeAlerts.isEmpty();
	}

	public int getAlertCount() {
		return activeAlerts.size();
	}

	public LiveAlert getHighestAlert() {
		if (activeAlerts.isEmpty()) return null;
		LiveAlert highest = null;
		for (LiveAlert a : activeAlerts.values()) {
			if (highest == null) {
				highest = a;
			} else if (a.isCritical() && !highest.isCritical()) {
				highest = a;
			}
		}
		return highest;
	}

	public String getHighestAlertLevel() {
		LiveAlert highest = getHighestAlert();
		return (highest != null) ? highest.level : "";
	}

	public double getSAuth() {
		return sAuth;
	}

	public void setSAuth(double sAuth) {
		this.sAuth = sAuth;
	}

	public double getINav() {
		return iNav;
	}

	public void setINav(double iNav) {
		this.iNav = iNav;
	}

	public String buildUserInfoTooltipHtml(boolean isPresent) {
		StringBuilder sb = new StringBuilder("<html><div style='padding:5px; font-family:sans-serif;'>");
		sb.append("<b>").append(fullname != null ? fullname : "").append("</b>");
		if (login != null && !login.isEmpty()) {
			sb.append(" <font color='#555555'>(").append(login).append(")</font>");
		}
		sb.append("<hr style='margin:4px 0;'/>");

		// info_1 : Nom + connexion
		if (isPresent) {
			sb.append("<font color='#27ae60'><b>• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0a1"), fullname != null ? fullname : "")).append("</b></font><br/>");
		} else {
			sb.append("<font color='#7f8c8d'>• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0a2"), fullname != null ? fullname : "")).append("</font><br/>");
		}

		// info_2 : Date
		String sdate = (date != null && !date.isBlank()) ? date : "--";
		sb.append("• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0b"), sdate)).append("<br/>");

		// info_3 : Adresse
		String saddr = (addr != null && !addr.isBlank()) ? addr : "--";
		sb.append("• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0c"), saddr)).append("<br/>");

		// info_4 : Session
		String ssession = (sessionId != null && !sessionId.isBlank()) ? sessionId : "--";
		sb.append("• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0d"), ssession)).append("<br/>");

		// info_5 : Statut
		if (statut != null) {
			sb.append("• ").append(String.format(codelab.CodeLab.LABEL("UserTable_0e"), statut.toString())).append("<br/>");
		}

		if (groups != null && !groups.isBlank()) {
			sb.append("• ").append(codelab.CodeLab.LABEL("UserTable_t3")).append(" : <b>").append(groups).append("</b><br/>");
		}
		if (editedFile != null && !editedFile.isBlank()) {
			sb.append("• Fichier : <b>").append(editedFile).append("</b><br/>");
		}
		sb.append("</div></html>");
		return sb.toString();
	}

	public String buildUserInfoTooltipHtml() {
		return buildUserInfoTooltipHtml(isConnected());
	}

	public String buildTooltipHtml() {
		StringBuilder sb = new StringBuilder("<html><div style='padding:5px; font-family:sans-serif;'>");
		sb.append("<b>Indicateurs en direct :</b><br/>");
		if (sAuth >= 0.0) {
			sb.append("• Authenticité du code : <b>").append(String.format(java.util.Locale.US, "%.1f%%", sAuth)).append("</b>");
			if (sAuth >= 75.0) {
				sb.append(" <font color='#27ae60'>(Élevée / Saisie autonome)</font>");
			} else if (sAuth >= 45.0) {
				sb.append(" <font color='#d35400'>(Modérée)</font>");
			} else {
				sb.append(" <font color='#c0392b'><b>(Faible / Attention)</b></font>");
			}
		} else {
			sb.append("• Authenticité du code : <b>--</b> <font color='#7f8c8d'>(En attente d'activité)</font>");
		}
		sb.append("<br/>");
		if (iNav > 0.0) {
			sb.append("• Temps hors de l'IDE : <b>").append(String.format(java.util.Locale.US, "%.1f%%", iNav)).append("</b><br/>");
		}
		if (!activeAlerts.isEmpty()) {
			sb.append("<hr style='margin:4px 0;'/>");
			sb.append("<b>Alertes actives (").append(activeAlerts.size()).append(") :</b><br/>");
			for (LiveAlert a : activeAlerts.values()) {
				String color = a.isCritical() ? "#D32F2F" : "#F57C00";
				String timeStr = a.getShortTime();
				String timePrefix = !timeStr.isEmpty() ? " <i>(" + timeStr + ")</i>" : "";
				sb.append("• <font color='").append(color).append("'><b>").append(a.code).append("</b></font>")
				  .append(timePrefix).append(" : ").append(a.message).append("<br/>");
			}
		}
		sb.append("</div></html>");
		return sb.toString();
	}

	private void parseAlertsJson(String json) {
		if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) return;
		try {
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{[^\\}]+\\}");
			java.util.regex.Matcher m = p.matcher(json);
			while (m.find()) {
				String block = m.group();
				String code = extractJsonField(block, "code");
				String msg = extractJsonField(block, "message");
				String level = extractJsonField(block, "level");
				String ts = extractJsonField(block, "timestamp");
				if (code != null && !code.isBlank()) {
					addAlert(code, msg, level, ts);
				}
			}
		} catch (Exception ignored) {}
	}

	private static String extractJsonField(String block, String key) {
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
		java.util.regex.Matcher m = p.matcher(block);
		return m.find() ? m.group(1) : "";
	}
}

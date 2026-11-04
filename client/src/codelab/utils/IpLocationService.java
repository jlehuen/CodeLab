package codelab.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codelab.client.UserData;

/**
 * Service de qualification et localisation des adresses IP.
 * Fonctionne de façon non-bloquante avec cache asynchrone pour ne jamais figer l'interface Swing.
 */
public class IpLocationService {

	public static class IpInfo {
		public final String ip;
		public final String networkType; // "Machine locale", "Réseau local (Salle de TP)", "Réseau universitaire", "Connexion distante"
		public final boolean isPrivate;
		public volatile String hostName = null;
		public volatile String city = null;
		public volatile String region = null;
		public volatile String country = null;
		public volatile String isp = null;
		public volatile boolean isResolved = false;

		public IpInfo(String ip, String networkType, boolean isPrivate) {
			this.ip = ip;
			this.networkType = networkType;
			this.isPrivate = isPrivate;
		}
	}

	private static final Map<String, IpInfo> CACHE = new ConcurrentHashMap<>();
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "IpLocationResolver");
		t.setDaemon(true);
		return t;
	});

	/**
	 * Détermine le type de réseau de manière synchrone et instantanée (0 ms).
	 */
	public static String classifyNetwork(String ip) {
		if (ip == null || ip.isBlank() || ip.equals("--")) return "Inconnue";
		ip = ip.trim();
		if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.equalsIgnoreCase("localhost")) {
			return "Machine locale (localhost)";
		}
		if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
			return "Réseau local (Salle de TP)";
		}
		if (ip.startsWith("172.")) {
			try {
				String[] parts = ip.split("\\.");
				int second = Integer.parseInt(parts[1]);
				if (second >= 16 && second <= 31) {
					return "Réseau local (Salle de TP)";
				}
			} catch (Exception ignored) {}
		}
		if (ip.startsWith("194.57.")) {
			return "Réseau universitaire (Le Mans Université / Renater)";
		}
		return "Connexion distante (Internet)";
	}

	public static boolean isPrivateIp(String ip) {
		if (ip == null || ip.isBlank() || ip.equals("--")) return true;
		ip = ip.trim();
		if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.equalsIgnoreCase("localhost")) return true;
		if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
		if (ip.startsWith("172.")) {
			try {
				String[] parts = ip.split("\\.");
				int second = Integer.parseInt(parts[1]);
				return (second >= 16 && second <= 31);
			} catch (Exception ignored) {}
		}
		return false;
	}

	/**
	 * Récupère ou déclenche la résolution asynchrone d'une IP.
	 */
	public static IpInfo getIpInfo(String rawIp) {
		if (rawIp == null || rawIp.isBlank() || rawIp.equals("--")) return null;
		final String ip = rawIp.trim();

		IpInfo cached = CACHE.get(ip);
		if (cached != null) return cached;

		boolean priv = isPrivateIp(ip);
		String netType = classifyNetwork(ip);
		IpInfo newInfo = new IpInfo(ip, netType, priv);
		CACHE.put(ip, newInfo);

		// Lancer la résolution en arrière-plan
		EXECUTOR.submit(() -> resolveInBackground(newInfo));
		return newInfo;
	}

	private static void resolveInBackground(IpInfo info) {
		try {
			// 1. Résolution DNS inverse (nom de la machine dans la salle de TP ou sur le réseau)
			try {
				InetAddress addr = InetAddress.getByName(info.ip);
				String canon = addr.getCanonicalHostName();
				if (canon != null && !canon.equalsIgnoreCase(info.ip) && !canon.equalsIgnoreCase("localhost")) {
					info.hostName = canon;
				}
			} catch (Exception ignored) {}

			// 2. Si connexion distante publique (non privée), tenter la géolocalisation ip-api.com
			if (!info.isPrivate && !info.ip.startsWith("194.57.")) {
				queryGeoApi(info);
			}
		} finally {
			info.isResolved = true;
		}
	}

	private static void queryGeoApi(IpInfo info) {
		HttpURLConnection conn = null;
		try {
			URL url = URI.create("http://ip-api.com/json/" + info.ip + "?fields=status,city,regionName,country,isp,org").toURL();
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(1800);
			conn.setReadTimeout(1800);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder json = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) json.append(line);
				reader.close();

				String status = extractJson(json.toString(), "status");
				if ("success".equalsIgnoreCase(status)) {
					info.city = extractJson(json.toString(), "city");
					info.region = extractJson(json.toString(), "regionName");
					info.country = extractJson(json.toString(), "country");
					String isp = extractJson(json.toString(), "isp");
					String org = extractJson(json.toString(), "org");
					info.isp = (isp != null && !isp.isBlank()) ? isp : org;
				}
			}
		} catch (Exception ignored) {
		} finally {
			if (conn != null) conn.disconnect();
		}
	}

	private static String extractJson(String json, String key) {
		Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
		Matcher m = p.matcher(json);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Construit le contenu HTML de l'infobulle pour la colonne Adresse IP.
	 */
	public static String buildIpTooltipHtml(UserData udata, boolean isPresent, int usersOnSameIp) {
		if (udata == null) return null;
		String ip = udata.getAddress();
		if (ip == null || ip.isBlank() || ip.equals("--")) {
			return "<html><div style='padding:5px; font-family:sans-serif;'>"
				+ "<b>Informations réseau :</b><br/>"
				+ "<hr style='margin:4px 0;'/>"
				+ "• Adresse IP : <b>--</b><br/>"
				+ "• État : <font color='#7f8c8d'>Non connecté</font>"
				+ "</div></html>";
		}

		IpInfo info = getIpInfo(ip);
		StringBuilder sb = new StringBuilder("<html><div style='padding:5px; font-family:sans-serif;'>");
		sb.append("<b>Informations réseau :</b><br/>");
		sb.append("<hr style='margin:4px 0;'/>");
		sb.append("• Adresse IP : <b>").append(ip).append("</b><br/>");

		if (info != null) {
			sb.append("• Type : <b>").append(info.networkType).append("</b><br/>");
			if (info.hostName != null && !info.hostName.isBlank()) {
				sb.append("• Machine / Hôte : <b>").append(info.hostName).append("</b><br/>");
			}
			if (info.city != null && !info.city.isBlank()) {
				sb.append("• Localisation : <b>").append(info.city);
				if (info.region != null && !info.region.isBlank()) sb.append(" (").append(info.region).append(")");
				if (info.country != null && !info.country.isBlank()) sb.append(", ").append(info.country);
				sb.append("</b><br/>");
			}
			if (info.isp != null && !info.isp.isBlank()) {
				sb.append("• Fournisseur / Réseau : <b>").append(info.isp).append("</b><br/>");
			}
		}

		if (isPresent) {
			sb.append("• État : <font color='#27ae60'><b>Connecté(e)</b></font>");
			if (udata.getDate() != null && !udata.getDate().isBlank()) {
				sb.append(" <i>(depuis le ").append(udata.getDate()).append(")</i>");
			}
			sb.append("<br/>");
		} else {
			sb.append("• État : <font color='#7f8c8d'>Non connecté(e)</font>");
			if (udata.getDate() != null && !udata.getDate().isBlank()) {
				sb.append(" <i>(dernière : ").append(udata.getDate()).append(")</i>");
			}
			sb.append("<br/>");
		}

		if (usersOnSameIp > 1) {
			sb.append("<hr style='margin:4px 0;'/>");
			sb.append("<font color='#d35400'><b>⚠ ").append(usersOnSameIp)
			  .append(" utilisateurs connectés sur cette même IP</b></font><br/>");
		}

		sb.append("</div></html>");
		return sb.toString();
	}
}

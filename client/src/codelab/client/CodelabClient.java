package codelab.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.File;
import java.awt.Color;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.io.FileUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.ExceptionManager;
import codelab.PropertyBase;
import codelab.console.Console;
import codelab.modules.editeur.ModuleEditor;
import codelab.modules.editeur.manager.FileManager;
import codelab.utils.WaitingDialog;
import codelab.utils.MyFileUtils;
import codelab.utils.Utils;

/**
*	Classe du client CodeLab
*	@author Jérôme Lehuen
*	@version 30/09/26
*/

public class CodelabClient {

	private static String SERVER_NAME;
	private static String SERVER_NICKNAME;
	private static int SERVER_PORT;

	private static int BACKUP_PERIOD; // Fréquence backup (dans sysconfig.properties)
	private static int UPDATE_PERIOD; // Fréquence synchro en mode normal (dans sysconfig.properties)
	private static int UPDATE_PERIOD_FAST; // Fréquence synchro en mode contrôle (dans sysconfig.properties)

	private CodeLab codelab;
	private ModuleEditor editor; // Inititalisé dans le protocole de connexion
	private ServerFacade server; // Facade du serveur CodeLab

	private String login;
	private String password; // Conservé pour la reconnexion transparente
	private Statut statut = Statut.STANDALONE; // Statut par défaut
	private String session; // Session courante (avec les underscores)
	private String username;

	// Reconnexion automatique transparente
	private volatile boolean intentionalDisconnect = false;
	private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
	private static final int MAX_RECONNECT_ATTEMPTS = 5;
	private static final int RECONNECT_DELAY_MS = 2000;

	// Détecteur d'inactivité utilisateur
	private InactivityDetector inactivityDetector = null;

	public static final boolean DEBUG = false;

	///////////////////////////////////////////////////
	// Getters et setters
	///////////////////////////////////////////////////

	public int getPort() { return SERVER_PORT; }
	public String getHost() { return SERVER_NAME; }
	public String getLogin() { return login; }
	public Statut getStatut() { return statut; }
	public String getSession() { return session; }
	public String getUsername() { return username; }
	public String getClientIP() {
		if (socket != null && socket.isConnected()) {
			try {
				return socket.getLocalAddress().getHostAddress();
			} catch (Exception ignored) {}
		}
		return CodeLab.IP_ADDR;
	}
	public String getNickName() { return SERVER_NICKNAME; }
	public boolean isStudent() { return statut.isStudent(); }
	public boolean isTutor() { return statut.isTutor(); }
	public boolean isAdmin() { return statut.isAdmin(); }
	public boolean isConnected() {
		return
			server != null &&
			server.isConnected();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public CodelabClient(CodeLab codelab) {
		this.codelab = codelab;

		SERVER_NAME = CodeLab.PROP("SERVER_HOST");
		SERVER_PORT = PropertyBase.getIntegerProperty("SERVER_PORT");
		BACKUP_PERIOD = PropertyBase.getIntegerProperty("BACKUP_PERIOD") * 60000; // En minutes
		UPDATE_PERIOD = PropertyBase.getIntegerProperty("UPDATE_PERIOD"); // En millisecondes
		UPDATE_PERIOD_FAST = PropertyBase.getIntegerProperty("UPDATE_PERIOD_FAST");

		inactivityDetector = new InactivityDetector(this);
	}

	///////////////////////////////////////////////////
	// Tâches répétitives
	///////////////////////////////////////////////////

	private Timer timer = null;

	public void stopAllTasks() {
		if (inactivityDetector != null) {
			inactivityDetector.stop();
		}
		if (timer == null) return;
		timer.cancel();
		timer = null;
	}

	// -------------------------------------
	// Mode normal
	// -------------------------------------

	public void startRegularTimer() {
		stopAllTasks();

		// Tâche côté student
		TimerTask task_1 = new TimerTask() {
			public void run() {
				if (editor.isPaused()) return; // Opération en cours
				if (!editor.isModified()) return; // Pas de modification
				editor.saveCurrentFile(); // Sauvegarde du fichier courant
			}
		};

		// Tâche côté student
		TimerTask task_2 = new TimerTask() {
			public void run() {
				if (editor.isPaused()) return; // Opération en cours
				if (!editor.isModified()) return; // Pas de modification
				editor.backupCurrentFile(); // Backup du fichier courant
			}
		};

		// Tâche côté tuteur
		TimerTask task_3 = new TimerTask() {
			public void run() {
				if (editor.isPaused()) return; // Opération en cours
				downloadCurrentFile(); // Récupération du fichier distant
			}
		};

		// Tâche de heartbeat périodique (maintien de la connexion et des tables NAT Wi-Fi)
		TimerTask task_heartbeat = new TimerTask() {
			public void run() {
				if (isConnected()) ping();
			}
		};

		// Tâche de surveillance de l'inactivité utilisateur (étudiants seulement)
		TimerTask task_inactivity = new TimerTask() {
			public void run() {
				if (inactivityDetector != null) {
					inactivityDetector.check();
				}
			}
		};

		// Démarrage des tâches répétitives
		timer = new Timer(false);
		if (isStudent() || isTutor() || isAdmin()) timer.schedule(task_1, 1000, UPDATE_PERIOD); // Sauvegarder le fichier si modifié
		if (isStudent()) timer.schedule(task_2, BACKUP_PERIOD, BACKUP_PERIOD); // Faire un backup du fichier si student
		if (isTutor() || isAdmin()) timer.schedule(task_3, 1000, UPDATE_PERIOD); // Récupérer le fichier si tuteur
		timer.schedule(task_heartbeat, 15000, 30000); // Ping régulier toutes les 30s
		if (isStudent()) {
			inactivityDetector.start();
			timer.schedule(task_inactivity, 5000, 5000); // Vérification de l'inactivité toutes les 5s
		}
	}

	// -------------------------------------
	// Mode contrôle
	// -------------------------------------

	public void startControlTimer(String login) {
		stopAllTasks();
		TimerTask task = new TimerTask() {
			public void run() {
				if (editor.isPaused()) return; // Opération en cours
				editor.sendCurrentControlledFile(login); // Envoi du fichier courant
			}
		};
		timer = new Timer(false);
		timer.schedule(task, 1000, UPDATE_PERIOD_FAST);
	}

	///////////////////////////////////////////////////
	// Pour envoyer un message synchrone
	///////////////////////////////////////////////////

	private String sendString(String msg) throws IOException {
		//CodeLab.logger(String.format("Send [%s] to the server", msg));

		writer.println(msg); // Envoyer le message au serveur
		String response = reader.readLine(); // Lire la réponse du serveur
		
		if (response == null) throw new IOException();
		//else CodeLab.logger(String.format("Received [%s] from the server", response));
		return response;
	}

	///////////////////////////////////////////////////
	// Pour extraire une session avec réconciliation
	///////////////////////////////////////////////////

	private static class SessionContext {
		final String login;
		final String session;
		final String role;

		SessionContext(String login, String session, String role) {
			this.login = (login != null) ? login.trim() : "";
			this.session = (session != null) ? session.trim() : "";
			this.role = (role != null) ? role.trim() : "";
		}
	}

	private SessionContext getLastSessionContext() {
		File file = new File(CodeLab.CODELAB_FILES_HIDDEN + "/last_session.txt");
		if (!file.exists()) return null;
		try {
			String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
			String[] parts = content.split(":", 3);
			if (parts.length == 3) {
				return new SessionContext(parts[0], parts[1], parts[2]);
			} else if (parts.length == 2) {
				return new SessionContext(parts[0], parts[1], "");
			} else if (parts.length == 1 && !parts[0].isEmpty()) {
				// Format de transition
				return new SessionContext("", parts[0], "");
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private void setLastSessionContext(String userLogin, String sessionId, boolean isStudent) {
		File file = new File(CodeLab.CODELAB_FILES_HIDDEN + "/last_session.txt");
		try {
			if (file.getParentFile() != null) file.getParentFile().mkdirs();
			String role = isStudent ? "STUDENT" : "TUTOR";
			String content = String.format("%s:%s:%s", userLogin != null ? userLogin : "", sessionId != null ? sessionId : "", role);
			Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignored) {}
	}

	private void safetyBackup(String destination, String sessionId) {
		if (!isStudent()) return; // Aucun recovery pour les tuteurs et admins
		try {
			File destDir = new File(destination);
			if (!destDir.exists() || !destDir.isDirectory()) return;
			File[] files = destDir.listFiles();
			if (files == null || files.length == 0) return;

			boolean hasUsefulFile = false;
			for (File f : files) {
				if (!f.getName().startsWith(".")) {
					hasUsefulFile = true;
					break;
				}
			}
			if (!hasUsefulFile) return;

			File recoveryBase = new File(CodeLab.RECOVERY_FOLDER);
			if (!recoveryBase.exists()) recoveryBase.mkdirs();

			String timestamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
			String backupDirName = String.format("%s/%s_%s", CodeLab.RECOVERY_FOLDER, sessionId, timestamp);
			File backupDir = new File(backupDirName);
			backupDir.mkdirs();
			FileUtils.copyDirectory(destDir, backupDir);
			CodeLab.logger("Safety recovery backup created in: " + backupDirName);

			cleanOldRecoveryBackups();
		} catch (Exception e) {
			System.err.println("Erreur safety backup : " + e.getMessage());
		}
	}

	private void cleanOldRecoveryBackups() {
		CodeLab.cleanOldRecoveryBackups();
	}

	private boolean extractSession(Object[] archive, String filename, String destination, String sessionId, boolean isStudent) {
		byte[] fileContent = MessagePackUtils.convertObjectArrayToByteArray(archive);
		String temp = String.format("%s/%s", CodeLab.TEMP_FOLDER, filename);
		try (FileOutputStream fos = new FileOutputStream(temp)) {
			fos.write(fileContent);
			try {
				if (isStudent) {
					SessionContext last = getLastSessionContext();
					boolean isSameStudentAndSession = last != null
						&& "STUDENT".equals(last.role)
						&& this.login != null && !this.login.isEmpty() && this.login.equals(last.login)
						&& sessionId != null && !sessionId.isEmpty() && sessionId.equals(last.session);

					if (isSameStudentAndSession) {
						// Snapshot préventif de sécurité UNIQUEMENT pour un étudiant qui se reconnecte à sa propre session
						safetyBackup(destination, sessionId);

						// Même étudiant et même session (reconnexion) : réconciliation intelligente sans écrasement
						CodeLab.logger(String.format("Reconnecting student %s to session %s: reconciling workspace", this.login, sessionId));
						TarZstExtractor.extractAndReconcile(temp, destination);
					} else {
						// Changement de session, nouvel étudiant ou bascule depuis tuteur : nettoyage complet et extraction (sans recovery parasite)
						CodeLab.logger(String.format("New student workspace for %s (session %s, prev=%s): cleaning directory",
							this.login, sessionId, (last != null ? last.login + "@" + last.session : "none")));
						MessagePackUtils.cleanDirectory(destination);
						TarZstExtractor.extract(temp, destination);
					}

					setLastSessionContext(this.login, sessionId, true);
				} else {
					// Pour les tuteurs / admins : aucun recovery n'est utile, l'archive serveur fait foi, nettoyage complet
					CodeLab.logger(String.format("Tutor workspace for %s (session %s): cleaning directory for full tree", this.login, sessionId));
					MessagePackUtils.cleanDirectory(destination);
					TarZstExtractor.extract(temp, destination);
					setLastSessionContext(this.login, sessionId, false);
				}

				new File(temp).delete(); // Suppression de l'archive temporaire
				return true;
			}
			catch (IOException e) {
				System.err.println("Erreur lors de la décompression : " + e.getMessage());
				return false;
			}
		}
		catch (IOException e) {
			System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
			return false;
		}
	}

	///////////////////////////////////////////////////
	// Gestion des sessions
	///////////////////////////////////////////////////
	
	private HashMap<String, Boolean> sessions;

	private String CLOSED_TAG = "[closed]"; // Ajouté par le serveur
	private String CLOSED_TAG2 = " [closed]"; // Juste pour l'affichage

	private void storeSessions(String str) {
		// Remplissage de la HashMap à partir de la chaîne en provenance du serveur
		sessions = new HashMap<>();
		for (String sessionId : str.split(";")) {
			String name = sessionId.replace(CLOSED_TAG, "");
			boolean opened = !sessionId.endsWith(CLOSED_TAG);
			sessions.put(name, opened);
		}
	}

	public int nbSessions() {
		return sessions.size();
	}

	public String[] getSessions() {
		Set<String> keys = sessions.keySet();
		String[] result = keys.toArray(new String[keys.size()]);
		Arrays.sort(result);
		return result;
	}

	private void openSession(String sessionId, boolean new_value) {
		// Commande en provenance du serveur
		if (!sessions.containsKey(sessionId)) return;
		sessions.put(sessionId, new_value);
	}

	public boolean isOpen(String sessionId) {
		if (sessions.containsKey(sessionId))
			return sessions.get(sessionId);
		return false;
	}

	private boolean isSessionAvailable(String sessionId) {
		return sessions != null && sessions.containsKey(sessionId);
	}

	private String chooseSession() {
		// Choix à la connexion
		String[] array = getSessions();
		// Une seule session disponible
		if (array.length == 1) return array[0];
		// Ajouter les tags [closed]
		int i = 0;
		for (String sessionId : array) {
			if (!isOpen(sessionId)) array[i] += CLOSED_TAG2;
			i++;
		}

		String choice = (String) JOptionPane.showInputDialog(
			CodeLab.FRAME,
			CodeLab.LABEL("CodeLab_session"), "CodeLab",
			JOptionPane.QUESTION_MESSAGE,
			CodeLab.ICON,
			array,
			array[0]);

		if (choice == null) return CANCELLED; // Annulation
		return choice.replace(CLOSED_TAG2, "");
	}

	public boolean actionChangeSession(String newSession) {
		// Invoqué via le ServerPopupMenu
		if (newSession == null) return false;
		if (newSession.equals(session)) return false; // Pas de changement
		codelab.halt(); // On stoppe l'exécution en cours
		codelab.showEditor(); // On bascule sur l'éditeur
		WaitingDialog.open("Downloading session...");
		changeSession(newSession); // On demande la nouvelle session
		session = newSession;
		return true;
	}

	///////////////////////////////////////////////////
	// Fermetures de connexion
	///////////////////////////////////////////////////

	public void onInactivityTimeout() {
		if (!isConnected() || !isStudent()) return;

		String logMsg = String.format("Inactivity timeout reached: disconnecting student %s", login);
		codelab.consoleLog("Déconnexion automatique de la session pour inactivité prolongée.");
		CodeLab.logger(logMsg);

		// 1. Sauvegarde locale de sécurité
		try {
			if (editor != null) {
				editor.saveCurrentFile();
			}
		} catch (Exception e) {
			CodeLab.logger("Error saving current file on inactivity timeout: " + e.getMessage());
		}

		// 2. Fermeture ordonnée de la session distante et basculement en mode autonome
		closeConnection();

		// 3. Information explicative à l'étudiant
		SwingUtilities.invokeLater(() -> {
			Utils.showMessageDialog("INFORMATION", CodeLab.LABEL("MepaClient_inactivity_disconnected"));
		});
	}

	public void closeConnection() {
		intentionalDisconnect = true;
		stopAllTasks(); // Arrêter les tâches répétitives
		ChatController.closeAll(); // Fermer les fenêtres de discussion

		editor.saveCurrentFile(); // Sauvegarde + envoi vers le serveur
		if (server != null) server.closeConnection(); // Ferme la connexion avec le serveur
		
		String msg = "Disconnected from the server, goto standalone mode";
		codelab.consoleLog(msg);
		CodeLab.logger(msg);

		this.session = null;

		SwingUtilities.invokeLater(() -> {
			codelab.helpFlag = false;
			statut = Statut.STANDALONE;
			editor.reinit(statut, null, "--", false);
		});
	}

	public synchronized void connectionBroken() {
		if (intentionalDisconnect) return;
		if (isReconnecting.get()) return;
		if (statut == Statut.STANDALONE) return;

		// Si on dispose des identifiants et de la session, on lance une reconnexion automatique en arrière-plan
		if (login != null && password != null && session != null) {
			isReconnecting.set(true);
			stopAllTasks();
			if (server != null) {
				server.closeConnection();
			}

			new Thread(this::attemptAutoReconnect, "AutoReconnectThread").start();
		} else {
			closeConnection();
			Utils.showMessageDialog("NETWORK ERROR", CodeLab.LABEL("MepaClientCommandHandler_0"));
		}
	}

	private void attemptAutoReconnect() {
		// Tentative de reconnexion automatique transparente
		// Proposé et codé par Gemini le 14/09/26
		CodeLab.logger("Starting transparent auto-reconnect...");
		codelab.consoleLog("Connexion interrompue. Tentative de reconnexion automatique...");

		// Sauvegarde locale de sécurité des fichiers ouverts
		try {
			if (editor != null) editor.saveAllFiles();
		} catch (Exception ignored) {}

		boolean reconnected = false;
		for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS; attempt++) {
			if (intentionalDisconnect) {
				isReconnecting.set(false);
				return;
			}

			String msg = String.format("Auto-reconnect attempt %d/%d...", attempt, MAX_RECONNECT_ATTEMPTS);
			CodeLab.logger(msg);
			codelab.consoleLog(msg);

			if (performConnectProtocol(login, password, session, true)) {
				reconnected = true;
				break;
			}

			try {
				Thread.sleep(RECONNECT_DELAY_MS);
			} catch (InterruptedException e) {
				break;
			}
		}

		isReconnecting.set(false);

		if (reconnected) {
			CodeLab.logger("Auto-reconnect successful!");
			codelab.consoleLog("Reconnexion automatique réussie !");
		} else {
			CodeLab.logger("Auto-reconnect failed after maximum attempts.");
			SwingUtilities.invokeLater(() -> {
				closeConnection();
				Utils.showMessageDialog("NETWORK ERROR", CodeLab.LABEL("MepaClientCommandHandler_0"));
			});
		}
	}

	///////////////////////////////////////////////////
	// Protocole de connexion
	///////////////////////////////////////////////////

	private final String ASK_LOGIN = "?LOGIN";
	private final String ASK_PASSWD = "?PASSWD";
	private final String ASK_NEWPASS = "?NEWPASS";
	private final String ASK_SESSION = "?SESSION:";
	private final String CONNECTION_OK = "CONNECTION_OK";

	private final String EJECTION = "EJECTION";
	private final String CANCELLED = "CANCELLED";
	private final String ERROR_LOGIN = "ERROR_LOGIN";
	private final String ERROR_PASSWD = "ERROR_PASSWD";
	private final String ERROR_NOSESSION = "ERROR_NOSESSION";
	private final String ERROR_CONNECTED = "ERROR_CONNECTED";

	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;

	private void closeSocket() {
		// Utilisé lors du dialogue de connexion
		try {
			if (socket != null) {
				socket.setSoTimeout(0);
				socket.close();
				socket = null;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean connection(String login, String passwrd) {
		this.intentionalDisconnect = false;
		this.login = login;
		this.password = passwrd;

		// Parceque l'éditeur est instancié après le client
		this.editor	= codelab.getEditor();

		return performConnectProtocol(login, passwrd, null, false);
	}

	private boolean performConnectProtocol(String login, String passwrd, String targetSession, boolean isAutoReconnect) {

		// ----------------------------------------------------
		// Ouverture du socket
		// ----------------------------------------------------
		
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(SERVER_NAME, SERVER_PORT), 5000);
			socket.setKeepAlive(true);
			socket.setTcpNoDelay(true);
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}
		catch (UnknownHostException e1) {
			System.out.println("ERROR (unknown host)");
			if (!isAutoReconnect) {
				String msg = String.format(CodeLab.LABEL("MepaClient_errorCode_1a"), SERVER_NAME);
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}
		catch (SocketTimeoutException e2) {
			System.out.println("ERROR (timeout)");
			if (!isAutoReconnect) {
				String msg = String.format(CodeLab.LABEL("MepaClient_errorCode_1b"), SERVER_PORT);
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}
		catch (IOException e3) {
			System.out.println("ERROR (socket error)");
			if (!isAutoReconnect) {
				String msg = CodeLab.LABEL("MepaClient_errorCode_1c");
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}
		catch (IllegalArgumentException e4) {
			System.out.println("ERROR (invalid address)");
			if (!isAutoReconnect) {
				String msg = CodeLab.LABEL("MepaClient_errorCode_1d");
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}
		catch (SecurityException e5) {
			System.out.println("ERROR (security)");
			if (!isAutoReconnect) {
				String msg = CodeLab.LABEL("MepaClient_errorCode_1e");
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}

		CodeLab.logger("Socket opened");
		CodeLab.logger("Start protocol...");
		
		try {

			// ----------------------------------------------------
			// Initialisation du protocole d'authentification
			// ----------------------------------------------------

			String response = sendString("START");

			// ----------------------------------------------------
			// Transmission du login
			// ----------------------------------------------------

			if (response.equals(ASK_LOGIN)) {
				response = sendString(login);

				if (response.equals(ERROR_LOGIN)) {
					CodeLab.logger("Login error");
					if (!isAutoReconnect) {
						String msg = CodeLab.LABEL("MepaClient_errorCode_3");
						Utils.showMessageDialog("CONNECTION ERROR", msg);
						CodeLab.AUTO_LOGIN = false; // Pour ne pas tourner en boucle
					}
					closeSocket();
					return false;
				}
			}
			else {
				CodeLab.logger("Protocol error");
				if (!isAutoReconnect) {
					String msg = CodeLab.LABEL("MepaClient_errorCode_0");
					Utils.showMessageDialog("CONNECTION ERROR", msg);
				}
				closeSocket();
				return false;
			}

			// ----------------------------------------------------
			// Transmission du mot de passe
			// ----------------------------------------------------

			if (response.equals(ASK_PASSWD)) {
				String pass256 = MessagePackUtils.encodeSHA256(passwrd);
				response = sendString(pass256);

				if (response.equals(ERROR_PASSWD)) {
					CodeLab.logger("Password error");
					if (!isAutoReconnect) {
						String msg = CodeLab.LABEL("MepaClient_errorCode_5");
						Utils.showMessageDialog("CONNECTION ERROR", msg);
					}
					closeSocket();
					return false;
				}

				if (response.equals(ASK_NEWPASS)) {
					if (isAutoReconnect) {
						closeSocket();
						return false;
					}
					// Première connexion (ou password réinitialisé)
					NewpassDialog npd = new NewpassDialog();
					passwrd = npd.getPassword();
					this.password = passwrd;
					pass256 = MessagePackUtils.encodeSHA256(passwrd);
					response = sendString(pass256);
				}
			}
			else {
				CodeLab.logger("Protocol error");
				if (!isAutoReconnect) {
					String msg = CodeLab.LABEL("MepaClient_errorCode_0");
					Utils.showMessageDialog("CONNECTION ERROR", msg);
				}
				closeSocket();
				return false;
			}

			// ----------------------------------------------------
			// Connexion déjà ouverte avec ces identifiants
			// ----------------------------------------------------

			if (response.equals(ERROR_CONNECTED)) {
				if (isAutoReconnect) {
					// En reconnexion automatique, l'ancienne connexion est notre propre socket tombé
					CodeLab.logger("Auto-reconnect: ejecting phantom connection on server...");
					sendString(EJECTION);
					closeSocket();
					try { Thread.sleep(500); } catch (InterruptedException ignored) {}
					return false;
				}
				if (askForEjection(login)) sendString(EJECTION);
				else sendString("OK"); // Car le serveur attend une réponse

				CodeLab.logger("ERROR (already used)");
				String msg = String.format(CodeLab.LABEL("MepaClient_errorCode_4bis"), login);
				Utils.showMessageDialog("INFORMATION", msg);
				closeSocket();
				return false;
			}

			// ----------------------------------------------------
			// Pas de session disponible
			// ----------------------------------------------------

			if (response.equals(ERROR_NOSESSION)) {
				CodeLab.logger("No session available");
				if (!isAutoReconnect) {
					String msg = CodeLab.LABEL("MepaClient_errorCode_6");
					Utils.showMessageDialog("CONNECTION ERROR", msg);
				}
				closeSocket();
				return false;
			}

			// ----------------------------------------------------
			// Une ou plusieurs sessions disponibles
			// ----------------------------------------------------

			if (response.startsWith(ASK_SESSION)) {
				response = response.substring(ASK_SESSION.length()); // On enlève ?SESSION
				storeSessions(response);

				if (isAutoReconnect && targetSession != null && isSessionAvailable(targetSession)) {
					// Reconnexion automatique : sélection transparente de la session courante
					session = targetSession;
					response = sendString(session);
				} else {
					CodeLab.logger("Choosing session...");
					session = chooseSession(); // Pas de dialogue si 1 seule session
					response = sendString(session);

					if (session.equals(CANCELLED)) {
						CodeLab.logger("Session dialog cancelled");
						closeSocket();
						return false;
					}
				}
			}

			// ----------------------------------------------------
			// Fin du protocole
			// ----------------------------------------------------

			if (response.equals(CONNECTION_OK)) {
				server = new ServerFacade(socket, this);
				return true;
			}
			else {
				CodeLab.logger("Unexpected protocol response: " + response);
				closeSocket();
				return false;
			}
		}
		catch (IOException e) {
			// Exception levée dans un sendString
			CodeLab.logger("Socket error: " + e.getMessage());
			if (!isAutoReconnect) {
				String msg = CodeLab.LABEL("MepaClient_errorCode_1c");
				Utils.showMessageDialog("CONNECTION ERROR", msg);
			}
			closeSocket();
			return false;
		}
	}

	///////////////////////////////////////////////////
	// Quelques méthodes privées
	///////////////////////////////////////////////////

	private boolean askForEjection(String login) {
		String msg = String.format(CodeLab.LABEL("MepaClient_errorCode_4"), login);
		int res = JOptionPane.showConfirmDialog(CodeLab.FRAME, msg, "CONNECTION ERROR", JOptionPane.YES_NO_OPTION);
		return (res == JOptionPane.YES_OPTION);
	}

	private void downloadCurrentFile() {
		File file = editor.getCurrentFile();
		UserData udata = editor.getUserTable().getSelectedUser();
		// On actualise seulement si l'auteur du fichier est connecté et que ce n'est pas un clone
		if (file != null && !FileManager.isClone(file) && udata.isConnected()) {
			requestFile(file);
		}
	}

	private String name4server(File file) {
		try {
			if (file.toString().endsWith("dist")) // Cas à la racine
				return String.format("%s/%s", session, login);
			String path = file.toString().replaceAll("\\\\", "/"); // Normalisation des séparateurs sous Windows
			String endpath = path.split("dist/", 0)[1]; // Fin du chemin
			String result = String.format("%s/%s/%s", session, login, endpath);
			return result;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return null; // Pas censé arriver
		}
	}

	private String name4update(File file) {
		// Adapte le path à destination d'une update tuteur
		String path = file.toString().replaceAll("\\\\", "/"); // Normalisation des séparateurs sous Windows
		String endpath = path.split("dist/", 0)[1]; // Fin du chemin
		String result = String.format("%s/%s", session, endpath);
		return result;
	}

	private String localPath(String path) {
		// Le path reçu est : [session]/[login]/...
		// Le path retourné est : PROG_DIST_FOLDER/[login]/...
		return CodeLab.PROG_DIST_FOLDER
			+ File.separator
			+ path.substring(path.indexOf(File.separator) + 1);
	}

	private boolean isTooBig(File file) {
		long size_Ko = file.length() / 1024;
		long maxSize = CodeLab.MAX_FILE_SIZE * 1024; // Bytes
		if (file.length() > maxSize) {
			codelab.printlnToConsole(
				String.format(CodeLab.LABEL("UPLOAD_ERROR_TERM"), size_Ko),
				Console.COLOR_ERROR, false);
			return true;
		}
		return false;
	}

	private synchronized void refreshFilemanager(String login, File temp) {
		// Rechargement du JTree après modification structurelle par login (si en cours de visualisation)
		// Synchronized car invoqué à de multiples endroits, y compris depuis le serveur
		if (editor.getUserTable() == null) return; // Protection défensive si la table utilisateur n'est pas instanciée
		UserData udata = editor.getUserTable().getSelectedUser();
		if (udata == null) return;
		if (udata.getLogin().equals(login)) {
			SwingUtilities.invokeLater(() -> {
				editor.getManager().populate();
				editor.getManager().expandAll();
				editor.reloadFile(temp); // Rechargement du fichier en cours de visualisation
			});
		}
	}

	private void soundIncomingMsg() {
		CodeLab.playAudio("audiofiles/incoming_msg.wav");
	}

	private void soundUserArrived() {
		CodeLab.playAudio("audiofiles/user_arrived.wav");
	}

	private void soundUserExited() {
		CodeLab.playAudio("audiofiles/user_exited.wav");
	}

	///////////////////////////////////////////////////
	// Commandes à destination du serveur
	///////////////////////////////////////////////////

	public void ping() {
		if (!isConnected()) return;
		server.ping();
	}

	public void bidule(String arg1, int arg2, boolean arg3) {
		if (!isConnected()) return;
		server.bidule(arg1, arg2, arg3);
	}

	public void forceDisconnect(String user_login) {
		if (!isConnected()) return;
		server.forceDisconnect(user_login);
	}

	public void forceDisconnectAll() {
		if (!isConnected()) return;
		server.forceDisconnectAll();
	}

	public void resetPassword(String user_login) {
		if (!isConnected()) return;
		server.resetPassword(user_login);
	}

	public void changePassword(String new_passwd) {
		if (!isConnected()) return;
		String pass_SHA256 = MessagePackUtils.encodeSHA256(new_passwd);
		server.changePassword(pass_SHA256);
	}
	
	public void changeSession(String sessionId) {
		if (!isConnected()) return;
		server.changeSession(sessionId);
	}

	public void addNewStudent(String new_login, String new_name) {
		if (!isConnected()) return;
		server.addNewStudent(new_login, new_name);
	}

	public void chatTo(String dest_login, String msg) {
		if (!isConnected()) return;
		server.chatTo(dest_login, msg);
	}

	public void messageTo(String dest_login, String msg) {
		if (!isConnected()) return;
		server.messageTo(dest_login, msg);
	}

	public void messageToAll(String msg) {
		if (!isConnected()) return;
		server.messageToAll(msg);
	}

	public void messageGlobal(String msg) {
		if (!isConnected()) return;
		server.messageGlobal(msg);
	}

	public void setSessionOpenned(boolean flag) {
		if (!isConnected()) return;
		server.setSessionOpenned(flag);
	}

	public void setEditedFile(String path) {
		if (!isConnected()) return;
		server.setEditedFile(path);
	}

	public void setHelpFlag(boolean flag) {
		// Action de la part d'un STUDENT
		if (!isConnected()) return;
		server.setHelpFlag(flag);
	}

	public void resetHelpFlag(String user_login) {
		// Action de la part d'un TUTOR
		if (!isConnected()) return;
		server.resetHelpFlag(user_login);
	}

	public void createFolderDist(File file) {
		if (!isConnected()) return;
		String name4server = name4server(file);
		if (name4server == null) return;
		server.createFolder(name4server);
	}

	public void newFileDist(File file) {
		if (!isConnected()) return;
		String name4server = name4server(file);
		if (name4server == null) return;
		server.newFile(name4server);
	}

	public void deleteFileDist(File file) {
		if (!isConnected()) return;
		String name4server = name4server(file);
		if (name4server == null) return;
		server.deleteFile(name4server);
	}

	public void renameFileDist(File file, String new_name) {
		if (!isConnected()) return;
		String name4server = name4server(file);
		if (name4server == null) return;
		server.renameFile(name4server, new_name);
	}

	public void moveFileDist(File file, File dest) {
		if (!isConnected()) return;
		String name4server1 = name4server(file);
		String name4server2 = name4server(dest);
		if (name4server1 == null) return;
		if (name4server2 == null) return;
		server.moveFile(name4server1, name4server2);
	}

	public void requestFile(File file) {
		if (!isConnected()) return;
		String serverPath = name4update(file); // Nom du fichier sur le serveur
		String clientPath = file.toString(); // Pour enregistrer le fichier reçu
		server.requestFile(serverPath, clientPath);
	}

	public void uploadFile(File file) {
		if (!isConnected()) return;
		if (isTooBig(file)) return; // Fichier trop volumineux
		try {
			byte[] content = Files.readAllBytes(Paths.get(file.toString()));
			String name4server = name4server(file);
			server.uploadFile(content, name4server);
		}
		catch (IOException e) {
			System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
		}
	}

	public void log2server(String texte) {
		if (!isConnected()) return;
		// server.log(texte);
		// Journalisation distante désactivée par défaut
	}

	public void exceptionReport(String content) {
		if (!isConnected()) return;
		server.exceptionReport(content);
	}

	public void askControlMode(String user_login, boolean flag) {
		if (!isConnected()) return;
		server.askControlMode(user_login, flag);
	}

	public void uploadControlledFile(File file, String user_login) {
		if (!isConnected()) return;
		if (isTooBig(file)) return; // Fichier trop volumineux
		try {
			//codelab.printlnToConsole("* Uploading file to " + user_login, Color.BLUE, false);
			byte[] content = Files.readAllBytes(Paths.get(file.toString()));
			String name4server = name4server(file);
			server.uploadControlledFile(content, name4server, user_login);
		}
		catch (IOException e) {
			System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
		}
	}

	///////////////////////////////////////////////////
	// Commandes en provenance du serveur
	///////////////////////////////////////////////////

	private UserData[] userDataArray = null; // Utilisateurs de la session

	@SuppressWarnings("unused")
	private void printUserDataArray() {
		if (userDataArray != null) {
			for (UserData user : userDataArray) {
				System.out.println(user);
			}
		}
	}
	
	// ----------------------------------------------------
	// Commandes à destination des clients
	// ----------------------------------------------------

	public synchronized void PONG() {
		// Méthode utilisée pour tester la connectivité
		// Dans is_socket_alive (serveur: connected.rs)
	}

	public synchronized void INIT_SESSION(
		String sessionId, String statut, String username,
		String serverName, String filename, Object[] file_content,
		boolean openned, String controller) {
		// Version pour les students (sans liste d'utilisateurs)
		// Le paramètre controller est le fullname d'un éventuel contrôleur (ou "NONE")

		File activeFile = (editor != null) ? editor.getCurrentFile() : null;

		// Extraction de l'arborescence de la session
		String destination = codelab.getProgramDir();
		if (extractSession(file_content, filename, destination, sessionId, true)) {

			this.statut = Statut.str2statut(statut);
			this.session = sessionId;
			this.username = username;
			SERVER_NICKNAME = serverName;
			// Réinitialisation de Codelab avec la session demandée
			javax.swing.SwingUtilities.invokeLater(() -> {
				editor.reinit(this.statut, userDataArray, sessionId, openned);
				if (activeFile != null && activeFile.exists()) {
					editor.open_file(activeFile, false);
				}
				if (controller.equals("NONE")) {
					// Pas de contrôle, on démarre les tâches répétitives
					startRegularTimer();
				} else {
					// Mode contrôle, on démarre le timer de contrôle
					editor.setControlled(true, controller);
					this.MESSAGE_FROM_SERVER("ServerWarningMessage_3", controller);
				}
			});

			// Affichage de l'état de la session
			String msg = String.format("Connected to session %s with status %s", sessionId, statut);
			codelab.consoleLog(msg);
			CodeLab.logger(msg);			
		} else {
			codelab.printlnToConsole("INTERNAL ERROR: Unable to restaure distant folder. Try to delete your \"codelab.files\" folder.", Color.RED);
			server.closeConnection();
		}
		WaitingDialog.close();
	}

	public synchronized void INIT_SESSION(
		String sessionId, String statut, String username,
		String serverName, String filename, Object[] file_content,
		boolean openned, String controller, Object[] user_list) {
		// Version pour les tuteurs (avec liste d'utilisateurs)
		// La paramètre controller serz toujours à "NONE" pour les tuteurs

		File activeFile = (editor != null) ? editor.getCurrentFile() : null;

		// Extraction de l'arborescence de la session
		String destination = codelab.getProgramDir();
	    if (extractSession(file_content, filename, destination, sessionId, false)) {

			this.statut = Statut.str2statut(statut);
			this.session = sessionId;
			this.username = username;
			SERVER_NICKNAME = serverName;
			// Construction de la liste des users de la session
			userDataArray = UserData.buildArray(user_list);
			// Réinitialisation de Codelab avec la session demandée
			javax.swing.SwingUtilities.invokeLater(() -> {
				editor.reinit(this.statut, userDataArray, sessionId, openned);
				if (activeFile != null && activeFile.exists()) {
					editor.open_file(activeFile, false);
				}
				startRegularTimer(); // Tâches répétitives
			});

			// Affichage de l'état de la session
			String msg = String.format("Connected to session %s with status %s", sessionId, statut);
			codelab.consoleLog(msg);
			CodeLab.logger(msg);
	    } else {
	        codelab.printlnToConsole("INTERNAL ERROR: Unable to restaure distant folder. Try to delete your \"codelab.files\" folder.", Color.RED);
	        server.closeConnection();
	    }
		WaitingDialog.close();
	}
	
	public synchronized void CHAT_FROM(String login, String fullname, String msg) {
		ChatController.chatFrom(login, fullname, msg);
		soundIncomingMsg();
	}
	
	public synchronized void MESSAGE_FROM(String login, String fullname, String msg) {
		String text = String.format(CodeLab.LABEL("MepaClient_7"), fullname, msg);
		Utils.showWarningDialog("MESSAGE", text);
		soundIncomingMsg();
	}
	
	public synchronized void MESSAGE_FROM_SERVER(String msgId) {
		// Nouveau message d'alerte (identificateur de message)
		String msg = CodeLab.LABEL(msgId);
		// Pour rendre la fenêtre non-modale
		new Thread(() -> JOptionPane.showMessageDialog(CodeLab.FRAME, msg)).start();
		soundIncomingMsg();
	}
	
	public synchronized void MESSAGE_FROM_SERVER(String msgId, String arg) {
		// Nouveau message d'alerte (identificateur de message)
		String msg = String.format(CodeLab.LABEL(msgId), arg);
		// Pour rendre la fenêtre non-modale
		new Thread(() -> JOptionPane.showMessageDialog(CodeLab.FRAME, msg)).start();
		soundIncomingMsg();
	}
	
	public synchronized void MESSAGE_FROM_SERVER(String msgId, String arg1, String arg2) {
		// Nouveau message d'alerte (identificateur de message)
		String msg = String.format(CodeLab.LABEL(msgId), arg1, arg2);
		// Pour rendre la fenêtre non-modale
		new Thread(() -> JOptionPane.showMessageDialog(CodeLab.FRAME, msg)).start();
		soundIncomingMsg();
	}
	
	public synchronized void MESSAGE_TO_CONSOLE(String msg) {
		// Réservé pour l'affichage direct de messages serveur dans la console
	}
	
	public synchronized void RESET_HELP_FLAG() {
		// Juste baisser le flag d'appel
		SwingUtilities.invokeLater(() -> {
			codelab.helpFlag = false;
			editor.getToolbar().update();
		});
	}
	
	public synchronized void SET_CONTROLLED(boolean flag, String fullname) {
		// Prise de contrôle ou libération de l'éditeur (pour les students)
		if (flag) {
			stopAllTasks();
			this.MESSAGE_FROM_SERVER("ServerWarningMessage_3", fullname);
		} else {
			startRegularTimer();
			this.MESSAGE_FROM_SERVER("ServerWarningMessage_4");
		}
		editor.setControlled(flag, fullname);
	}

	public synchronized void OPEN_SESSION(String sessionId, boolean flag) {
		openSession(sessionId, flag);
	}
	
	public synchronized void UPDATE_FILE(Object[] array, String path) {
		// Récupérer et enregistrer le fichier reçu dans un Object[]
		byte[] fileContent = MessagePackUtils.convertObjectArrayToByteArray(array);
		try (FileOutputStream fos = new FileOutputStream(path)) {
			fos.write(fileContent);
		}
		catch (IOException e) {
			CodeLab.logger("Erreur lors de l'écriture du fichier : " + e.getMessage());
			return;
		}
		// Nécessaire de mettre un invokeLater sinon éditeur blink
		javax.swing.SwingUtilities.invokeLater(() -> {
			try {
				editor.updateContent(path);
				codelab.serverActivity();
			}
			catch (IOException e) {
				codelab.printlnToConsole("INTERNAL ERROR: Updating current file failed", Color.RED);
				ExceptionManager.process(e);
			}
		});
	}

	public synchronized void UPDATE_FILE_CONTROLLED(Object[] array, String path) {
		// Récupérer un fichier en mode contrôle
		String newpath = String.format("%s%s", CodeLab.PROG_DIST_FOLDER, path);
		this.UPDATE_FILE(array, newpath);
	}

	public synchronized void SERVER_SHUTDOWN() {
		closeConnection();
		SwingUtilities.invokeLater(() -> {
			Utils.showMessageDialog("INFORMATION", CodeLab.LABEL("MepaClient_0"));
		});
	}
	
	// ----------------------------------------------------
	// Commandes à destination des clients-tuteurs
	// ----------------------------------------------------
	
	public synchronized void CLIENT_ARRIVED(String login, String addr, String date) {
		//codelab.printlnToConsole(String.format("CLIENT: %s (%s) vient de rejoindre la session%n", login, addr), Color.YELLOW);
		javax.swing.SwingUtilities.invokeLater(() -> {
			editor.getUserTable().connect(login, addr, date);
			soundUserArrived();
		});
	}
	
	public synchronized void CLIENT_EXITED(String login) {
		//codelab.printlnToConsole(String.format("CLIENT: %s vient de quitter la session%n", login), Color.YELLOW);
		javax.swing.SwingUtilities.invokeLater(() -> {
			editor.getUserTable().disconnect(login);
			soundUserExited();
		});
	}

	public synchronized void NEW_STUDENT(Object map) {
		// Un nouvel étudiant a été créé à la volée par un tuteur
		javax.swing.SwingUtilities.invokeLater(() -> {
			UserData udata = UserData.from(map);
			editor.getUserTable().newStudent(udata);
		});
	}
	
	public synchronized void SET_HELP_FLAG(String login, boolean flag) {
		// Un utilisateur a modifié l'état du flag d'appel
		editor.getUserTable().setHelpFlag(login, flag);
		if (flag) soundIncomingMsg();
	}
	
	public synchronized void SET_CONTROL_MODE(boolean flag, String login) {
		// Mettre à jour le mode de contrôle de login (pour les tuteurs)
		editor.setControlMode(flag, login);
		if (flag) startControlTimer(login);
		else startRegularTimer();
	}
	
	public synchronized void SET_EDITED_FILE(String login, String path) {
		// Un utilisateur a sélectionné un fichier dans le file manager
		if (editor.getUserTable() == null) return; // Protection défensive si la table utilisateur n'est pas instanciée
		editor.getUserTable().setEditedFile(login, path); // path peut être = AbstractEditor.EMPTY
		editor.getManager().invokeLater_updateUI();
	}
	
	public synchronized void NEW_FOLDER(String login, String path) {
		// Un utilisateur a créé un dossier
		File temp = editor.keepCurrentFile(); // Pour le rechargement
		File file = new File(localPath(path)); // Le dossier à créer
		file.mkdir();
		refreshFilemanager(login, temp);
	}
	
	public synchronized void NEW_FILE(String login, String path) {
		// Un utilisateur a créé un fichier (actuellement vide)
		// Le fichier sera mis-à-jour par la suite dans UPDATE_FILE
		File temp = editor.keepCurrentFile(); // Pour le rechargement
		File file = new File(localPath(path)); // Le fichier à créer
		MyFileUtils.touch(file);
		refreshFilemanager(login, temp);
		// Pour un programme Blocs avec l'en-tête XML
		if (MyFileUtils.getExtension(path).equals(".blocs")) {
			String content = "<?xml version='1.0' encoding='UTF-8'?><scratch/>";
			MyFileUtils.write(file, content);
		}
	}
	
	public synchronized void DELETE_FILE(String login, String path) {
		// Un utilisateur a supprimé un fichier ou un dossier
		File temp = editor.keepCurrentFile(); // Pour le rechargement
		File file = new File(localPath(path)); // Le fichier à supprimer
		MyFileUtils.delete(file);
		refreshFilemanager(login, temp);
	}
	
	public synchronized void RENAME_FILE(String login, String path, String filename) {
		// Un utilisateur a renommé un fichier ou un dossier
		File temp = editor.keepCurrentFile(); // Pour le rechargement
		File file = new File(localPath(path)); // Le fichier à renommer
		File newfile = new File(file.getParentFile().getPath() + File.separator + filename);
		file.renameTo(newfile);
		refreshFilemanager(login, temp);
		// Pour afficher en rouge le fichier renommé (et donc sélectionné)
		String name = file.getParentFile().getPath() + File.separator + filename;
		name = name.split(login, 0)[1]; // Juste après le login
		SET_EDITED_FILE(login, name);
	}
	
	public synchronized void MOVE_FILE(String login, String path1, String path2) {
		// Un utilisateur a déplacé un fichier ou un dossier
		File temp = editor.keepCurrentFile(); // Pour le rechargement
		File file = new File(localPath(path1)); // le fichier à déplacer
		File newfile = new File(localPath(path2 + File.separator + file.getName()));
		Utils.moveFile(file, newfile);
		refreshFilemanager(login, temp);
	}
}

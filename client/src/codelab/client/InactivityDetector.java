package codelab.client;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.PropertyBase;

/**
*	Détecteur d'inactivité utilisateur pour CodeLab.
*	Surveille les interactions physiques (clavier, souris, molette) via l'AWT Toolkit
*	et déclenche la déconnexion automatique des étudiants après un délai d'inactivité.
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 30/09/26
*/

public class InactivityDetector {

	private final CodelabClient client;
	private final int timeoutMinutes;
	private final int warningDelayMinutes;

	private final long timeoutMs;
	private final long warningThresholdMs;

	private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
	private volatile boolean active = false;
	private volatile boolean warningShown = false;

	private InactivityWarningDialog warningDialog = null;

	private final AWTEventListener awtListener = new AWTEventListener() {
		@Override
		public void eventDispatched(AWTEvent event) {
			recordActivity();
		}
	};

	public InactivityDetector(CodelabClient client) {
		this.client = client;

		int timeout = PropertyBase.getIntegerProperty("INACTIVITY_TIMEOUT");
		this.timeoutMinutes = (timeout > 0) ? timeout : 20;

		int warningDelay = PropertyBase.getIntegerProperty("INACTIVITY_WARNING_DELAY");
		this.warningDelayMinutes = (warningDelay > 0) ? warningDelay : 1;

		this.timeoutMs = (long) this.timeoutMinutes * 60 * 1000;
		long warningDelayMs = (long) this.warningDelayMinutes * 60 * 1000;
		this.warningThresholdMs = Math.max(0, this.timeoutMs - warningDelayMs);
	}

	public synchronized void start() {
		if (timeoutMinutes <= 0) return; // Désactivé si timeout <= 0
		stop(); // Réinitialisation propre au préalable

		lastActivityTime.set(System.currentTimeMillis());
		warningShown = false;
		active = true;

		try {
			Toolkit.getDefaultToolkit().addAWTEventListener(
				awtListener,
				AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK
			);
			CodeLab.logger(String.format("InactivityDetector started (timeout: %d min, warning: %d min)",
				timeoutMinutes, warningDelayMinutes));
		} catch (Exception e) {
			CodeLab.logger("Failed to add AWTEventListener for InactivityDetector: " + e.getMessage());
		}
	}

	public synchronized void stop() {
		if (!active) return;
		active = false;
		warningShown = false;

		try {
			Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
		} catch (Exception ignored) {}

		dismissWarningDialog();
		CodeLab.logger("InactivityDetector stopped");
	}

	public void recordActivity() {
		lastActivityTime.set(System.currentTimeMillis());
		if (warningShown) {
			dismissWarningDialog();
		}
	}

	public void check() {
		if (!active || timeoutMinutes <= 0) return;
		if (!client.isConnected() || !client.isStudent()) return;

		long now = System.currentTimeMillis();
		long elapsed = now - lastActivityTime.get();

		if (elapsed >= timeoutMs) {
			// Seuil total atteint -> déconnexion
			dismissWarningDialog();
			stop();
			client.onInactivityTimeout();
		} else if (elapsed >= warningThresholdMs && !warningShown) {
			// Seuil de pré-alerte atteint -> afficher le dialogue avec compte à rebours
			long remainingMs = timeoutMs - elapsed;
			int remainingSec = (int) Math.max(1, remainingMs / 1000);
			showWarningDialog(remainingSec);
		}
	}

	private synchronized void showWarningDialog(int remainingSec) {
		if (warningShown) return;
		warningShown = true;

		SwingUtilities.invokeLater(() -> {
			if (!active || !client.isConnected() || !client.isStudent()) {
				warningShown = false;
				return;
			}
			warningDialog = new InactivityWarningDialog(
				remainingSec,
				() -> {
					// Callback quand le compte à rebours expire
					dismissWarningDialog();
					stop();
					client.onInactivityTimeout();
				},
				() -> {
					// Callback quand l'utilisateur clique sur "Rester connecté" ou agit
					recordActivity();
				}
			);
			warningDialog.setVisible(true);
		});
	}

	public synchronized void dismissWarningDialog() {
		warningShown = false;
		if (warningDialog != null) {
			InactivityWarningDialog d = warningDialog;
			warningDialog = null;
			SwingUtilities.invokeLater(() -> {
				try {
					d.dispose();
				} catch (Exception ignored) {}
			});
		}
	}
}

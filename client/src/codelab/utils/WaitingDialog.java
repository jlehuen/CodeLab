package codelab.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

import codelab.CodeLab;

/**
 * Overlay modal de chargement et d'attente (GlassPane)
 * Intégré directement à la fenêtre principale CodeLab.FRAME :
 * - Modalité complète (interception de tous les clics souris et frappes clavier)
 * - Suit naturellement la fenêtre principale sans nécessiter de repositionnement manuel
 * - Carte centrale FlatLaf avec ombre portée douce et spinner vectoriel haute résolution
 * 
 * @author Jérôme Lehuen
 * @version 10/10/26
 */

public class WaitingDialog extends JPanel {

	private static WaitingDialog singleton = null;
	private static Component previousFocusOwner = null;

	private static final ProgressSpinner spinner = new ProgressSpinner();
	private static final JLabel messageLabel = new JLabel();
	private static final JProgressBar progressBar = new JProgressBar();
	private static final JButton cancelButton = new JButton("Annuler");
	private static final CardPanel cardPanel = new CardPanel();
	private static Runnable currentCancelAction = null;

	/**
	 * Récupère l'instance singleton du GlassPane WaitingDialog.
	 */
	public static synchronized WaitingDialog getInstance() {
		if (singleton == null) {
			singleton = new WaitingDialog();
		}
		return singleton;
	}

	/**
	 * Ouvre l'overlay modal avec le message spécifié (sans bouton d'annulation).
	 */
	public static void open(String text) {
		open(text, null);
	}

	/**
	 * Ouvre l'overlay modal avec le message spécifié et une action d'annulation optionnelle.
	 */
	public static void open(String text, Runnable onCancel) {
		Runnable r = () -> {
			WaitingDialog instance = getInstance();

			if (CodeLab.FRAME != null) {
				if (CodeLab.FRAME.getGlassPane() != instance) {
					CodeLab.FRAME.setGlassPane(instance);
				}
			}

			// Mémoriser le focus actif avant ouverture pour le restaurer à la fermeture
			Component currentFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (currentFocus != instance && currentFocus != cardPanel && currentFocus != cancelButton) {
				previousFocusOwner = currentFocus;
			}

			currentCancelAction = onCancel;
			if (onCancel != null) {
				cancelButton.setText("Annuler");
				cancelButton.setEnabled(true);
				cancelButton.setVisible(true);
			} else {
				cancelButton.setVisible(false);
			}

			setIndeterminateInternal(true);
			setTextInternal(text);
			instance.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			instance.setVisible(true);
			spinner.start();
			instance.requestFocusInWindow();
			instance.revalidate();
			instance.repaint();

			// Forcer le rendu immédiat sur la fenêtre si elle est déjà affichée
			if (CodeLab.FRAME != null && instance.getWidth() > 0 && instance.getHeight() > 0) {
				instance.paintImmediately(0, 0, instance.getWidth(), instance.getHeight());
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Déclenche l'action d'annulation enregistrée.
	 */
	public static void triggerCancel() {
		if (currentCancelAction == null) return;
		Runnable action = currentCancelAction;
		currentCancelAction = null;
		cancelButton.setEnabled(false);
		cancelButton.setText("Arrêt en cours...");
		setTextInternal("Arrêt en cours...");
		new Thread(() -> {
			try {
				action.run();
			} catch (Exception ignored) {}
		}, "WaitingDialog-CancelThread").start();
	}

	/**
	 * Met à jour le message d'attente dynamiquement.
	 */
	public static void setText(String text) {
		if (singleton == null) return;
		Runnable r = () -> setTextInternal(text);
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	private static void setTextInternal(String text) {
		if (messageLabel != null) {
			boolean dark = FlatLaf.isLafDark();
			messageLabel.setForeground(dark ? new Color(230, 235, 245) : new Color(40, 45, 55));
			messageLabel.setText(text != null ? text : "");
			if (cardPanel != null) {
				cardPanel.revalidate();
				cardPanel.repaint();
			}
			if (singleton != null && singleton.isVisible() && singleton.getWidth() > 0) {
				singleton.paintImmediately(0, 0, singleton.getWidth(), singleton.getHeight());
			}
		}
	}

	/**
	 * Définit la progression courante sur un total (ex: 12 / 45).
	 * Bascule automatiquement la barre fine en mode déterminé (remplissage proportionnel).
	 */
	public static void setProgress(int current, int total) {
		if (singleton == null) return;
		Runnable r = () -> setProgressInternal(current, total);
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Définit la progression en pourcentage (0 à 100).
	 * Bascule automatiquement la barre fine en mode déterminé.
	 */
	public static void setProgress(int percent) {
		setProgress(percent, 100);
	}

	/**
	 * Active ou désactive le mode indéterminé (va-et-vient).
	 */
	public static void setIndeterminate(boolean indeterminate) {
		if (singleton == null) return;
		Runnable r = () -> setIndeterminateInternal(indeterminate);
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Indique si la barre de progression est actuellement en mode indéterminé.
	 */
	public static boolean isIndeterminate() {
		return progressBar.isIndeterminate();
	}

	/**
	 * Retourne la valeur actuelle de progression (0 si indéterminé).
	 */
	public static int getProgress() {
		return progressBar.getValue();
	}

	private static void setProgressInternal(int current, int total) {
		if (progressBar == null) return;

		int max = Math.max(1, total);
		int val = Math.max(0, Math.min(max, current));

		if (progressBar.isIndeterminate()) {
			progressBar.setIndeterminate(false);
		}

		progressBar.setMinimum(0);
		progressBar.setMaximum(max);
		progressBar.setValue(val);
		progressBar.setStringPainted(false);

		if (cardPanel != null) {
			cardPanel.revalidate();
			cardPanel.repaint();
		}
		if (singleton != null && singleton.isVisible() && singleton.getWidth() > 0) {
			singleton.paintImmediately(0, 0, singleton.getWidth(), singleton.getHeight());
		}
	}

	private static void setIndeterminateInternal(boolean indeterminate) {
		if (progressBar == null) return;
		progressBar.setIndeterminate(indeterminate);
		progressBar.setStringPainted(false);
		progressBar.setString(null);

		if (cardPanel != null) {
			cardPanel.revalidate();
			cardPanel.repaint();
		}
		if (singleton != null && singleton.isVisible() && singleton.getWidth() > 0) {
			singleton.paintImmediately(0, 0, singleton.getWidth(), singleton.getHeight());
		}
	}

	/**
	 * Ferme l'overlay modal et restaure l'interactivité de l'application.
	 */
	public static void close() {
		if (singleton == null) return;
		Runnable r = () -> {
			spinner.stop();
			setIndeterminateInternal(true);
			currentCancelAction = null;
			cancelButton.setVisible(false);
			singleton.setVisible(false);
			singleton.setCursor(Cursor.getDefaultCursor());

			// Restauration du focus
			if (previousFocusOwner != null && previousFocusOwner.isShowing()) {
				previousFocusOwner.requestFocusInWindow();
			} else if (CodeLab.FRAME != null) {
				CodeLab.FRAME.requestFocusInWindow();
			}
			previousFocusOwner = null;

			if (CodeLab.FRAME != null) {
				CodeLab.FRAME.repaint();
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Rafraîchit l'affichage de l'overlay (conservation pour rétrocompatibilité).
	 */
	public static void updateLocation() {
		if (singleton == null) return;
		Runnable r = () -> {
			if (singleton.isVisible()) {
				singleton.revalidate();
				singleton.repaint();
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Indique si l'overlay d'attente est actuellement affiché.
	 */
	public static boolean isOpened() {
		return singleton != null && singleton.isVisible();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	private WaitingDialog() {
		setOpaque(false);
		setLayout(new GridBagLayout()); // Centrage parfait et automatique du HUD dans la fenêtre

		// Interception et neutralisation absolue des événements souris (modalité)
		MouseAdapter mouseBlocker = new MouseAdapter() {
			private boolean isOverCancel(MouseEvent e) {
				if (cancelButton != null && cancelButton.isShowing() && cancelButton.isEnabled()) {
					Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cancelButton);
					return cancelButton.contains(p);
				}
				return false;
			}

			@Override public void mouseClicked(MouseEvent e) {
				if (isOverCancel(e)) {
					triggerCancel();
				}
				e.consume();
			}
			@Override public void mousePressed(MouseEvent e) {
				if (!isOverCancel(e)) e.consume();
			}
			@Override public void mouseReleased(MouseEvent e) {
				if (!isOverCancel(e)) e.consume();
			}
			@Override public void mouseEntered(MouseEvent e) { e.consume(); }
			@Override public void mouseExited(MouseEvent e) { e.consume(); }
			@Override public void mouseMoved(MouseEvent e) {
				if (isOverCancel(e)) {
					singleton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else {
					singleton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				e.consume();
			}
			@Override public void mouseDragged(MouseEvent e) { e.consume(); }
			@Override public void mouseWheelMoved(MouseWheelEvent e) { e.consume(); }
		};
		addMouseListener(mouseBlocker);
		addMouseMotionListener(mouseBlocker);
		addMouseWheelListener(mouseBlocker);

		// Interception des frappes clavier
		setFocusable(true);
		setFocusTraversalKeysEnabled(false);
		addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) { e.consume(); }
			@Override public void keyReleased(KeyEvent e) { e.consume(); }
			@Override public void keyTyped(KeyEvent e) { e.consume(); }
		});

		// Filtre global des raccourcis clavier pendant toute la durée d'affichage (Echap pour annuler)
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (singleton != null && singleton.isVisible()) {
					if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						if (currentCancelAction != null && cancelButton.isEnabled()) {
							triggerCancel();
						}
					}
					e.consume();
					return true;
				}
				return false;
			}
		});

		// Construction de la carte centrale
		buildCard();
		add(cardPanel);
	}

	private void buildCard() {
		cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
		cardPanel.setOpaque(false);
		cardPanel.setBorder(BorderFactory.createEmptyBorder(22, 32, 22, 32));

		// Spinner vectoriel animé
		spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Libellé de message
		Font labelFont = UIManager.getFont("Label.font");
		if (labelFont == null) {
			labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
		} else {
			labelFont = labelFont.deriveFont(Font.BOLD, 14f);
		}
		messageLabel.setFont(labelFont);
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Barre de progression FlatLaf fine et épurée (4 px, sans texte)
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		progressBar.setPreferredSize(new Dimension(280, 4));
		progressBar.setMaximumSize(new Dimension(320, 4));
		progressBar.setMinimumSize(new Dimension(200, 4));
		progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Bouton d'annulation stylisé FlatLaf
		cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		cancelButton.setFocusable(false);
		cancelButton.setVisible(false);
		cancelButton.putClientProperty("JButton.buttonType", "roundRect");
		cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		cancelButton.addActionListener(e -> triggerCancel());
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				if (cancelButton.isEnabled() && singleton != null && singleton.isVisible()) {
					singleton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
			}
			@Override
			public void mouseExited(MouseEvent e) {
				if (singleton != null && singleton.isVisible()) {
					singleton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
			}
		});

		cardPanel.add(spinner);
		cardPanel.add(Box.createVerticalStrut(14));
		cardPanel.add(messageLabel);
		cardPanel.add(Box.createVerticalStrut(14));
		cardPanel.add(progressBar);
		cardPanel.add(Box.createVerticalStrut(16));
		cardPanel.add(cancelButton);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		boolean dark = FlatLaf.isLafDark();

		// 1. Voile modal d'arrière-plan (scrim semi-transparent)
		Color scrim = dark ? new Color(0, 0, 0, 155) : new Color(0, 0, 0, 65);
		g2.setColor(scrim);
		g2.fillRect(0, 0, getWidth(), getHeight());

		// 2. Ombre portée douce diffuse sous la carte centrale
		if (cardPanel != null && cardPanel.isVisible()) {
			int cx = cardPanel.getX();
			int cy = cardPanel.getY();
			int cw = cardPanel.getWidth();
			int ch = cardPanel.getHeight();
			int arc = 18;

			if (cw > 0 && ch > 0) {
				int shadowSize = 8;
				for (int i = shadowSize; i >= 1; i--) {
					int alpha = (dark ? 20 : 12) * (shadowSize - i + 1) / shadowSize;
					g2.setColor(new Color(0, 0, 0, Math.min(255, alpha)));
					g2.fillRoundRect(cx - i, cy - i + 2, cw + i * 2, ch + i * 2, arc + i * 2, arc + i * 2);
				}
			}
		}

		g2.dispose();
		super.paintComponent(g);
	}

	/**
	 * Carte centrale au style FlatLaf épuré (coins arrondis, fond clair/sombre, bordure subtile).
	 */
	private static class CardPanel extends JPanel {
		private static final int ARC = 18;

		public CardPanel() {
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			int w = Math.max(380, Math.min(520, d.width));
			int h = Math.max(180, d.height);
			return new Dimension(w, h);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			boolean dark = FlatLaf.isLafDark();
			int w = getWidth();
			int h = getHeight();

			// Fond de la carte (légèrement rehaussé)
			Color bg = dark ? new Color(38, 42, 50) : Color.WHITE;
			g2.setColor(bg);
			g2.fillRoundRect(0, 0, w, h, ARC, ARC);

			// Bordure délicate FlatLaf
			Color border = dark ? new Color(65, 72, 85) : new Color(218, 224, 233);
			g2.setColor(border);
			g2.setStroke(new BasicStroke(1.2f));
			g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

			g2.dispose();
			super.paintComponent(g);
		}
	}

	/**
	 * Spinner circulaire animé vectoriel (haute résolution, fluide et sans artefact).
	 */
	public static class ProgressSpinner extends JComponent {
		private double angle = 0;
		private final Timer timer;
		private final int diameter = 38;
		private final float strokeWidth = 3.5f;

		public ProgressSpinner() {
			setPreferredSize(new Dimension(46, 46));
			setMinimumSize(new Dimension(46, 46));
			setMaximumSize(new Dimension(46, 46));
			setOpaque(false);
			timer = new Timer(25, e -> {
				angle = (angle + 7) % 360;
				repaint();
			});
		}

		public void start() {
			if (!timer.isRunning()) {
				timer.start();
			}
		}

		public void stop() {
			timer.stop();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			int r = diameter / 2;

			boolean dark = FlatLaf.isLafDark();

			// Anneau discret d'arrière-plan (track)
			g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.setColor(dark ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 22));
			g2.drawOval(cx - r, cy - r, diameter, diameter);

			// Arc animé (couleur d'accentuation FlatLaf ou bleu CodeLab #2675EB)
			Color accent = UIManager.getColor("Component.accentColor");
			if (accent == null) {
				accent = new Color(38, 117, 235);
			}
			g2.setColor(accent);
			g2.drawArc(cx - r, cy - r, diameter, diameter, (int) -angle, 110);

			g2.dispose();
		}
	}
}

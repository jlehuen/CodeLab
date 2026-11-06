package codelab.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import codelab.AbstractCodeLab;
import codelab.CodeLab;

/**
 * Gestionnaire unifié des autorisations système macOS (TCC / Confidentialité & Sécurité).
 * Centralise l'explication didactique de chaque autorisation et permet l'ouverture directe
 * du bon panneau dans les Réglages Système en un seul clic.
 * 
 * @author Jérôme Lehuen
 * @version 06/11/26
 */
public class MacPermissions {

	public enum Permission {
		INPUT_MONITORING(
			"Privacy_ListenEvent",
			"MacPermissions_input_title",
			"MacPermissions_input_msg",
			"MacPermissions_input_name",
			"🎮"
		),
		LOCAL_NETWORK(
			"Privacy_LocalNetwork",
			"MacPermissions_network_title",
			"MacPermissions_network_msg",
			"MacPermissions_network_name",
			"🌐"
		),
		FILES_AND_FOLDERS(
			"Privacy_AllFiles",
			"MacPermissions_files_title",
			"MacPermissions_files_msg",
			"MacPermissions_files_name",
			"📁"
		),
		ACCESSIBILITY(
			"Privacy_Accessibility",
			"MacPermissions_access_title",
			"MacPermissions_access_msg",
			"MacPermissions_access_name",
			"♿"
		);

		private final String pane;
		private final String titleKey;
		private final String msgKey;
		private final String nameKey;
		private final String icon;

		Permission(String pane, String titleKey, String msgKey, String nameKey, String icon) {
			this.pane = pane;
			this.titleKey = titleKey;
			this.msgKey = msgKey;
			this.nameKey = nameKey;
			this.icon = icon;
		}

		public String getPane() { return pane; }
		public String getTitle() { return CodeLab.LABEL(titleKey); }
		public String getMessage() { return CodeLab.LABEL(msgKey); }
		public String getName() { return CodeLab.LABEL(nameKey); }
		public String getIcon() { return icon; }
	}

	/**
	 * Ouvre directement le sous-panneau ciblé dans Réglages Système > Confidentialité et sécurité.
	 */
	public static void openSettings(String pane) {
		if (!AbstractCodeLab.IS_OSX) return;
		try {
			String url = "x-apple.systempreferences:com.apple.preference.security";
			if (pane != null && !pane.trim().isEmpty()) {
				url += "?" + pane.trim();
			}
			Runtime.getRuntime().exec(new String[]{"open", url});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void openInputMonitoring() {
		openSettings(Permission.INPUT_MONITORING.getPane());
	}

	public static void openLocalNetwork() {
		openSettings(Permission.LOCAL_NETWORK.getPane());
	}

	public static void openFiles() {
		openSettings(Permission.FILES_AND_FOLDERS.getPane());
	}

	public static void openAccessibility() {
		openSettings(Permission.ACCESSIBILITY.getPane());
	}

	public static void openSecurityGeneral() {
		openSettings(null);
	}

	/**
	 * Affiche un dialogue explicatif et propose d'ouvrir immédiatement le panneau des Réglages Système.
	 * 
	 * @param perm L'autorisation concernée
	 * @param parent Le composant parent pour le dialogue
	 * @return true si l'utilisateur a choisi d'ouvrir les réglages, false sinon
	 */
	public static boolean prompt(Permission perm, Component parent) {
		if (!AbstractCodeLab.IS_OSX) return false;

		String title = perm.getTitle();
		String message = perm.getMessage();
		Object[] options = new Object[]{
			CodeLab.LABEL("MacPermissions_open_btn"),
			CodeLab.LABEL("MacPermissions_later_btn")
		};

		int choice = JOptionPane.showOptionDialog(
			parent != null ? parent : CodeLab.FRAME,
			message,
			title,
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE,
			CodeLab.ICON,
			options,
			options[0]
		);

		if (choice == JOptionPane.YES_OPTION) {
			openSettings(perm.getPane());
			return true;
		}
		return false;
	}

	/**
	 * Affiche le tableau de bord récapitulatif de toutes les autorisations système macOS pour CodeLab.
	 */
	public static void showAllPermissionsDialog(Component parent) {
		if (!AbstractCodeLab.IS_OSX) return;

		JDialog dialog = new JDialog(CodeLab.FRAME, CodeLab.LABEL("MacPermissions_dialog_title"), true);
		dialog.setLayout(new BorderLayout(15, 15));
		dialog.setResizable(false);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

		JLabel header = new JLabel("<html><b>" + CodeLab.LABEL("MacPermissions_dialog_header") + "</b><br><font color='#555555'>"
			+ CodeLab.LABEL("MacPermissions_dialog_desc") + "</font></html>");
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(header);
		content.add(Box.createVerticalStrut(15));
		content.add(new JSeparator());
		content.add(Box.createVerticalStrut(10));

		for (Permission p : Permission.values()) {
			JPanel row = new JPanel(new BorderLayout(10, 5));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			row.setMaximumSize(new Dimension(540, 55));
			row.setPreferredSize(new Dimension(540, 50));

			JLabel lbl = new JLabel("<html><b>" + p.getIcon() + " " + p.getName() + "</b><br><small><font color='#666666'>"
				+ p.getMessage() + "</font></small></html>");
			JButton btn = new JButton(CodeLab.LABEL("MacPermissions_open_btn"));
			btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			btn.addActionListener(e -> openSettings(p.getPane()));

			row.add(lbl, BorderLayout.CENTER);
			row.add(btn, BorderLayout.EAST);

			content.add(row);
			content.add(Box.createVerticalStrut(8));
		}

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));

		JButton btnGeneral = new JButton(CodeLab.LABEL("MacPermissions_open_general_btn"));
		btnGeneral.addActionListener(e -> openSecurityGeneral());

		JButton btnClose = new JButton(CodeLab.LABEL("Close"));
		btnClose.addActionListener(e -> dialog.dispose());

		bottom.add(btnGeneral, BorderLayout.WEST);
		bottom.add(btnClose, BorderLayout.EAST);

		dialog.add(content, BorderLayout.CENTER);
		dialog.add(bottom, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(parent != null ? parent : CodeLab.FRAME);
		dialog.setVisible(true);
	}
}

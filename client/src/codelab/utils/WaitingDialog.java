package codelab.utils;

import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import codelab.CodeLab;

/**
*   JDialog avec animation (singleton)
*	@author Jérôme Lehuen
*	@version 29/01/24
*/

public class WaitingDialog extends JDialog {

	private static WaitingDialog singleton = null;

	public static void open(String text) {
		if (singleton == null) singleton = new WaitingDialog();
		SwingUtilities.invokeLater(() -> {
			label1.setText(text);
			singleton.setLocation();
			singleton.setVisible(true);
		});
	}

	public static void close() {
		if (singleton == null) return;
		SwingUtilities.invokeLater(() -> {
			singleton.setVisible(false);
		});
	}

	public static void updateLocation() {
		if (singleton == null) return;
		SwingUtilities.invokeLater(() -> {
			singleton.setLocation();
			singleton.repaint();
		});
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	private static ImageIcon icon = ResourceUtils.loadImageIcon("loading.gif");
	private static Dimension dialogSize = new Dimension(400, 200);
	private static JLabel label1 = new JLabel();

	private WaitingDialog() {
		JLabel label2 = new JLabel(icon);
		label1.setFont(new Font("Sanserif", Font.PLAIN, 14));
		label1.setHorizontalAlignment(JLabel.CENTER);
		label2.setHorizontalAlignment(JLabel.CENTER);
		label2.setVerticalAlignment(JLabel.CENTER);
		setLayout(new GridLayout(2, 1));
		add(label1);
        add(label2);
		setLocation();
		setSize(dialogSize);
		setResizable(false);
		setUndecorated(true);
		setAlwaysOnTop(true);
	}

	private void setLocation() {
		Rectangle parentSize = CodeLab.FRAME.getBounds();
        //setLocation(
		//	parentSize.width/2 - (dialogSize.width/2) + parentSize.x,
		//	parentSize.height/2 - (dialogSize.height/2) + parentSize.y);
		setLocation(
			parentSize.width/2 - (dialogSize.width/2) + parentSize.x,
			parentSize.height - dialogSize.height + parentSize.y);
	}
}

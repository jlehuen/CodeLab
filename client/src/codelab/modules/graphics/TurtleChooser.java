package codelab.modules.graphics;

import java.awt.Font;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import codelab.CodeLab;
import codelab.utils.GraphUtils;
import codelab.utils.ResourceUtils;

/**
*	Classe du sélecteur de tortues
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class TurtleChooser extends JDialog {

	private ArrayList<TurtlePack> liste;
	private boolean empty = false;
	private int old_index;
	private int new_index;

	private int LARG = 400;
	private int HAUT = 300;

	private JLabel label;

	private BufferedImage icon_left = ResourceUtils.loadBufferedImageAsRessource("icons/icon_left.png");
	private BufferedImage icon_right = ResourceUtils.loadBufferedImageAsRessource("icons/icon_right.png");

	private void closeFrame() {
		setVisible(false);
		dispose();
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public TurtleChooser(ModuleGraphics module, int index) {
		super(CodeLab.FRAME, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		liste = module.getTurtles();
		old_index = index;
		new_index = index;

		label = new JLabel();
		label.setPreferredSize(new Dimension(LARG, HAUT));

		if (liste.size() == 0) {
			label.setText(CodeLab.LABEL("TurtleChooserEmpty"));
			label.setFont(new Font("Serif", Font.PLAIN, 14));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			empty = true;
		}
		else updateImage();

		JButton bouton_gauche = new JButton();
		JButton bouton_droite = new JButton();
		bouton_gauche.setIcon(new ImageIcon(icon_left));
		bouton_droite.setIcon(new ImageIcon(icon_right));
		bouton_gauche.setFocusable(false);
		bouton_droite.setFocusable(false);

		bouton_gauche.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (empty) return;
				if (--new_index < 0) new_index = liste.size() - 1;
				updateImage();
			}
		});

		bouton_droite.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (empty) return;
				if (++new_index == liste.size()) new_index = 0;
				updateImage();
			}
		});

		final MouseAdapter selectAdapter = new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (empty) return;
				if (e.getButton() == MouseEvent.BUTTON1) {
					final int selectedIndex = new_index;
					closeFrame();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (selectedIndex != old_index) {
								TurtlePack pack = liste.get(selectedIndex);
								module.setTurtleFromSelector(pack, selectedIndex);
							}
						}
					});
				}
			}
		};
		addMouseListener(selectAdapter);
		label.addMouseListener(selectAdapter);

		// Fermeture sur la touche Échap
		getRootPane().registerKeyboardAction(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeFrame();
				}
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);

		getContentPane().add(label, BorderLayout.CENTER);
		getContentPane().add(bouton_gauche, BorderLayout.WEST);
		getContentPane().add(bouton_droite, BorderLayout.EAST);
		pack();

		setTitle(CodeLab.LABEL("TurtleChooserTitle"));
		setLocationRelativeTo(CodeLab.FRAME);
		setResizable(false);
		setVisible(true);
	}

	private void updateImage() {
		BufferedImage image = liste.get(new_index).getImage();
		image = GraphUtils.rotate270(image);
		label.setIcon(new ImageIcon(image));
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setVerticalAlignment(JLabel.CENTER);
	}
}

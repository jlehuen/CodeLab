package codelab.modules.robotics;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import codelab.CodeLab;
import codelab.utils.ColorCode;

/**
*	Classe des menus popup du fond
*	@author Jérôme Lehuen
*	@version 25/09/23
*/

public class BackgroundPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public BackgroundPopupMenu(Simulator simulator, int x, int y, double scale) {

		int x_scaled = (int)(x/scale);
		int y_scaled = (int)(y/scale);

		AbstractDeplacable objet = simulator.findObject(new Point(x_scaled, y_scaled));
		boolean enabled = !simulator.isRunning();

		if (objet != null && objet.isRobot() && !simulator.isRunning()) {
			JMenuItem menuItem1 = new JMenuItem(CodeLab.LABEL("ChangeRobot"));
			menuItem1.setEnabled(enabled);
			menuItem1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new RobotChooser(simulator.module);
				}
			});
			add(menuItem1);
		}
		else if (objet != null && (objet.isObstacle() || objet.isColoredTag())) {
			JMenuItem menuItem4 = new JMenuItem(CodeLab.LABEL("RemoveObject"));
			menuItem4.setEnabled(enabled);
			menuItem4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					simulator.removeObject(objet);
				}
			});
			add(menuItem4);
		}
		else {
			JMenuItem menuItem2 = new JMenuItem(CodeLab.LABEL("ChangeBackground"));
			menuItem2.setEnabled(enabled);
			menuItem2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new BackgroundChooser(simulator.module);
				}
			});
			add(menuItem2);
			addSeparator();

			JMenuItem menuItem3 = new JMenuItem(CodeLab.LABEL("AddSquareObject"));
			menuItem3.setEnabled(enabled);
			menuItem3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ObstacleCarre(simulator, x_scaled, y_scaled, "obstacle_carre");
				}
			});
			add(menuItem3);

			JMenuItem menuItem4 = new JMenuItem(CodeLab.LABEL("AddRoundObject"));
			menuItem4.setEnabled(enabled);
			menuItem4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ObstacleRond(simulator, x_scaled, y_scaled, "obstacle_rond");
				}
			});
			add(menuItem4);

			JMenuItem menuItem5 = new JMenuItem(CodeLab.LABEL("AddLongObject"));
			menuItem5.setEnabled(enabled);
			menuItem5.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ObstacleCarre(simulator, x_scaled, y_scaled, "obstacle_mur");
				}
			});
			add(menuItem5);
			addSeparator();

			JMenuItem menuItem6 = new JMenuItem(CodeLab.LABEL("AddRedObject"));
			menuItem6.setEnabled(enabled);
			menuItem6.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ColoredTag(simulator, x_scaled, y_scaled, "tag_red", ColorCode.COLOR_RED);
				}
			});
			add(menuItem6);

			JMenuItem menuItem7 = new JMenuItem(CodeLab.LABEL("AddGreenObject"));
			menuItem7.setEnabled(enabled);
			menuItem7.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ColoredTag(simulator, x_scaled, y_scaled, "tag_green", ColorCode.COLOR_GREEN);
				}
			});
			add(menuItem7);

			JMenuItem menuItem8 = new JMenuItem(CodeLab.LABEL("AddBlueObject"));
			menuItem8.setEnabled(enabled);
			menuItem8.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ColoredTag(simulator, x_scaled, y_scaled, "tag_blue", ColorCode.COLOR_BLUE);
				}
			});
			add(menuItem8);

			JMenuItem menuItem9 = new JMenuItem(CodeLab.LABEL("AddYellowObject"));
			menuItem9.setEnabled(enabled);
			menuItem9.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ColoredTag(simulator, x_scaled, y_scaled, "tag_yellow", ColorCode.COLOR_YELLOW);
				}
			});
			add(menuItem9);

			JMenuItem menuItem10 = new JMenuItem(CodeLab.LABEL("AddBlackObject"));
			menuItem10.setEnabled(enabled);
			menuItem10.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					new ColoredTag(simulator, x_scaled, y_scaled, "tag_black", ColorCode.COLOR_BLACK);
				}
			});
			add(menuItem10);
		}
		show(simulator, x, y);
	}
}

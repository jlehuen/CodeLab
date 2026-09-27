package codelab.controllers.widgets;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.SwingPropertyChangeSupport;

/**
*	Classe du joystick
*	@author Jérôme Lehuen
*	@version 07/07/22
*/

public class JoystickPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final int outputMax;
	private final int thumbDiameter;
	private final int thumbRadius;
	private final int panelWidth;
	private final int arrowRadius;
	private final int BORDER_THICKNESS = 2;

	private final Point thumbPos = new Point();
	private Stroke lineStroke = new BasicStroke(20, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private int[] left_x, left_y, right_x, right_y, up_x, up_y, down_x, down_y;

	///////////////////////////////////////////////////
	// Property Change Support
	///////////////////////////////////////////////////

	private SwingPropertyChangeSupport propertySupporter = new SwingPropertyChangeSupport(this);

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupporter.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupporter.removePropertyChangeListener(listener);
	}

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public JoystickPanel(int output_max, int panel_width) {

		// @param output_max : valeur maximale à laquelle la sortie doit être mise à l'échelle
		// @param panel_width : taille du panel

		assert output_max > 0;
		assert panel_width > 0;

		outputMax = output_max;
		panelWidth = panel_width;
		thumbDiameter = panel_width / 4;
		thumbRadius = thumbDiameter / 2;
		arrowRadius = panel_width / 24;

		left_x = up_y = new int[] {thumbDiameter - arrowRadius, thumbDiameter + arrowRadius, thumbDiameter + arrowRadius};
		left_y = up_x = new int[] {panelWidth / 2, panelWidth / 2 + arrowRadius, panelWidth / 2 - arrowRadius};
		right_x = down_y = new int[] {panelWidth - thumbDiameter + arrowRadius, panelWidth - thumbDiameter - arrowRadius, panelWidth - thumbDiameter - arrowRadius};
		right_y = down_x = new int[] {panelWidth / 2, panelWidth / 2 + arrowRadius, panelWidth / 2 - arrowRadius};

		MouseAdapter mouseAdapter = new MouseAdapter() {

			private void repaintAndTriggerListeners(){
				SwingUtilities.getRoot(JoystickPanel.this).repaint();
				propertySupporter.firePropertyChange(null, null, getOutputPos());
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					updateThumbPos(e.getX(), e.getY());
					repaintAndTriggerListeners();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					updateThumbPos(e.getX(), e.getY());
					repaintAndTriggerListeners();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					centerThumbPad();
					repaintAndTriggerListeners();
				}
			}
		};
		addMouseMotionListener(mouseAdapter);
		addMouseListener(mouseAdapter);
		setPreferredSize(new java.awt.Dimension(panel_width, panel_width));
		setOpaque(false);
		centerThumbPad();
	}

	private void centerThumbPad() {
		thumbPos.x = panelWidth / 2;
		thumbPos.y = panelWidth / 2;
	}

	public void reset() {
		centerThumbPad();
		repaint();
	}

	private void updateThumbPos(int mouseX, int mouseY) {
		if (mouseX < thumbRadius)
			mouseX = thumbRadius;
		else if(mouseX > panelWidth - thumbRadius)
			mouseX = panelWidth - thumbRadius;

		if (mouseY < thumbRadius)
			mouseY = thumbRadius;
		else if(mouseY > panelWidth - thumbRadius)
			mouseY = panelWidth - thumbRadius;

		thumbPos.x = mouseX;
		thumbPos.y = mouseY;
	}

	Point getOutputPos(){
		Point result = new Point();
		result.x = outputMax * (thumbPos.x - panelWidth / 2) / (panelWidth / 2 - thumbDiameter / 2);
		result.y = -outputMax * (thumbPos.y - panelWidth / 2) / (panelWidth / 2 - thumbDiameter / 2);
		return result;
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		// Pour avoir un beau lissage des tracés en 2D
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, panelWidth, panelWidth);

		g2d.setColor(Color.GRAY);
		g2d.fillOval(thumbRadius, thumbRadius, panelWidth - thumbDiameter, panelWidth - thumbDiameter);

		g2d.setColor(Color.BLACK);
		g2d.fillOval(thumbRadius + BORDER_THICKNESS, thumbRadius + BORDER_THICKNESS, panelWidth - thumbDiameter - BORDER_THICKNESS * 2, panelWidth - thumbDiameter - BORDER_THICKNESS * 2);

		g2d.setColor(Color.GRAY);
		g2d.fillPolygon(left_x, left_y, 3);
		g2d.fillPolygon(right_x, right_y, 3);
		g2d.fillPolygon(up_x, up_y, 3);
		g2d.fillPolygon(down_x, down_y, 3);

		g2d.setStroke(lineStroke);
		g2d.drawLine(panelWidth / 2, panelWidth / 2, thumbPos.x, thumbPos.y);
		g2d.setColor(Color.RED);
		g2d.fillOval(thumbPos.x - thumbRadius, thumbPos.y - thumbRadius, thumbRadius * 2, thumbRadius * 2);
	}
}

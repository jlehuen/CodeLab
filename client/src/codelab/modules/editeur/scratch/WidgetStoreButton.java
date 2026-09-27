package codelab.modules.editeur.scratch;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
*	Classe des boutons du store
*	@author Jérôme Lehuen
*	@version $Date$
*/

public class WidgetStoreButton {

	private int x;
	private int y;
	private int width;
	private int height;
	private Color color;
	private String label;
	private String code;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public WidgetStoreButton(WidgetStore store, int x, int y, int width, int height, Color color, String label, String code) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.color = color;
		this.label = label;
		this.code = code;
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public boolean contains(Point point) {
		return (new Rectangle(x, y, width, height).contains(point));
	}

	public String getCode() { return code; }

	public void draw(Graphics2D g2d) {
		g2d.setColor(color);
		g2d.fillRect(x, y, width, height);

		FontMetrics fm = g2d.getFontMetrics();
		Rectangle2D r = fm.getStringBounds(label, g2d);
		int dx = (width - (int) r.getWidth()) / 2;
		int dy = (height - (int) r.getHeight()) / 2 + fm.getAscent();

		g2d.setColor(Color.BLACK);
		g2d.drawString(label, x + dx, y + dy);
	}
}

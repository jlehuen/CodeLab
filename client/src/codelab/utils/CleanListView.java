package codelab.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;

/**
 * Vue Swing personnalisée pour les listes non ordonnées HTML (&lt;ul&gt;).
 *
 * Remplace le ListPainter natif de Swing qui dessine de petits rectangles ou carrés
 * grossiers (notamment sous Linux ou dans les conteneurs imbriqués tels que blockquotes)
 * par de superbes puces circulaires lissées (anti-aliasing) et parfaitement alignées.
 */
public class CleanListView extends BlockView {

	public CleanListView(Element elem) {
		super(elem, View.Y_AXIS);
	}

	@Override
	public float getAlignment(int axis) {
		return 0.5f;
	}

	@Override
	protected void paintChild(Graphics g, Rectangle alloc, int index) {
		paintBullet(g, alloc, index);
		super.paintChild(g, alloc, index);
	}

	private int getNestLevel() {
		int level = 0;
		View p = getParent();
		while (p != null) {
			if (p instanceof CleanListView || (p.getElement() != null && "ul".equalsIgnoreCase(p.getElement().getName()))) {
				level++;
			}
			p = p.getParent();
		}
		return level;
	}

	private void paintBullet(Graphics g, Rectangle alloc, int index) {
		View child = getView(index);
		if (child == null) return;

		Element elem = child.getElement();
		if (elem != null) {
			AttributeSet as = elem.getAttributes();
			Object name = as != null ? as.getAttribute(StyleConstants.NameAttribute) : null;
			if (name != null && name != HTML.Tag.LI) {
				return;
			}
		}

		AttributeSet attrs = child.getAttributes();
		Font f = getStyleSheet().getFont(attrs);
		int fontSize = f != null ? f.getSize() : 13;
		float bulletDiam = Math.max(5.0f, Math.round(fontSize * 0.38f));

		// Positionnement horizontal : placé élégamment dans la marge gauche du <li>
		float bulletX = alloc.x - bulletDiam - 7.0f;

		// Positionnement vertical : aligné avec précision sur la première ligne de texte
		float bulletY = alloc.y + (fontSize - bulletDiam) / 2.0f;
		if (child.getViewCount() > 0) {
			Shape lineAlloc = child.getChildAllocation(0, alloc);
			if (lineAlloc != null) {
				Rectangle lr = lineAlloc.getBounds();
				bulletY = lr.y + (fontSize - bulletDiam) / 2.0f;
			}
		}

		Color c = getStyleSheet().getForeground(attrs);
		if (c == null) {
			c = new Color(0x24, 0x29, 0x2f);
		}

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(c);

			int nestLevel = getNestLevel();
			if (nestLevel == 0) {
				// Niveau 1 : disque plein et lissé
				g2.fill(new Ellipse2D.Float(bulletX, bulletY, bulletDiam, bulletDiam));
			} else if (nestLevel == 1) {
				// Niveau 2 : cercle évidé élégant
				g2.setStroke(new BasicStroke(1.2f));
				g2.draw(new Ellipse2D.Float(bulletX, bulletY, bulletDiam, bulletDiam));
			} else {
				// Niveau 3+ : petit carré plein
				float sqSize = Math.max(4.0f, bulletDiam - 1.0f);
				g2.fill(new Rectangle2D.Float(bulletX + (bulletDiam - sqSize) / 2.0f, bulletY + (bulletDiam - sqSize) / 2.0f, sqSize, sqSize));
			}
		} finally {
			g2.dispose();
		}
	}
}

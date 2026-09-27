package codelab.modules.robotics;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import codelab.utils.ColorCode;

/**
*	Classe des balises colorées
*	@author Jérôme Lehuen
*	@version 17/03/22
*/

public class ColoredTag extends AbstractDeplacable {

    private ColorCode color;

    ///////////////////////////////////////////////////
    // Constructeur
    ///////////////////////////////////////////////////

    public ColoredTag(Simulator simulator, int x, int y, String ident, ColorCode color) {
        super(simulator);
        this.x = x;
        this.y = y;
        this.color = color;
        setImage(ident);
        CENTER_X = largeur / 2;
        CENTER_Y = hauteur / 2;
        // Redéfinir le path qui doit être également circulaire
        Ellipse2D cercle = new Ellipse2D.Double(0, 0, largeur, hauteur);
        path = new Path2D.Double();
        path.append(cercle, false);
        simulator.addObject(this);
    }

    public boolean isColoredTag() { return true; }

    public String getInfo() {
        return "a colored tag";
    }

    public ColorCode getColor() {
        return color;
    }

    protected void draw(Graphics2D g2d) {
        super.draw(g2d);
    }
}

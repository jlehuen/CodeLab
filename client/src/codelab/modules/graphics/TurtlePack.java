package codelab.modules.graphics;

import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class TurtlePack {

	private BufferedImage image;
	private Path2D.Double path;

	public TurtlePack(BufferedImage image, Path2D.Double path) {
		this.image = image;
		this.path = path;
	}

	public BufferedImage getImage() {
		return image;
	}

	public Path2D.Double getPath() {
		return path;
	}
}

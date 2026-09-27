package codelab.modules.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;

import codelab.CodeLab;
import codelab.AbstractModule;
import codelab.utils.ResourceUtils;

/**
*	Classe du module graphique
*	@author Jérôme Lehuen
*	@version 17/01/24
*/

public class ModuleGraphics extends AbstractModule {

	public TurtleCanvas canvas;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public static ModuleGraphics INSTANCE;

	public ModuleGraphics(CodeLab codelab) {
		super(codelab);
		INSTANCE = this;
		System.out.print("   Loading module graphics... ");
		loadTurtles(); // Avant new TurtleCanvas()
		setTitle(LABEL("Tab_graphic"));
		setToolbar(new ToolbarGraph(this));
		setComponent(canvas = new TurtleCanvas(this));
		prepareUserDirectory(canvas); // Après new TurtleCanvas()
		System.out.println("DONE");
	}

	///////////////////////////////////////////////////
	// Gestion des turtles
	///////////////////////////////////////////////////

	private ArrayList<TurtlePack> turtles = new ArrayList<TurtlePack>();

	public ArrayList<TurtlePack> getTurtles() {
		return turtles;
	}

	private void loadTurtles() {
		List<String> liste = ResourceUtils.getFiles("data/turtles/");
		for (String name : liste) {
			if (name.endsWith(".png")) {
				//printlnToConsole(" - Loading " + name, Color.GRAY, false);
				turtles.add(TurtleUtils.loadTurtlePack(name));
			}
		}
	}

	public TurtlePack getTurtlePack(int index) {
		if (index < turtles.size())
			return turtles.get(index);
		else
			return turtles.get(0);

	}

	///////////////////////////////////////////////////
	// Gestion des backgrounds
	///////////////////////////////////////////////////

	public static final String dirname = CodeLab.getUserDataFolder() + "/mod_graphics/backgrounds";
	public static final String emptyBG = dirname + "/empty.png";

	private ArrayList<BufferedImage> backgrounds = new ArrayList<BufferedImage>();

	public ArrayList<BufferedImage> getBackgrounds() {
		return backgrounds;
	}

	private void prepareUserDirectory(TurtleCanvas canvas) {
		try {
			TurtleUtils.createDirectory(dirname); // If not exists
			Dimension size = new Dimension(canvas.WIDTH, canvas.HEIGHT);
			BufferedImage whiteBackground = TurtleUtils.createBackground(size, Color.WHITE);
			TurtleUtils.saveImage(whiteBackground, emptyBG);

			backgrounds.add(whiteBackground); // Add white background
			loadBackgrouds();
		}
		catch (IOException e) {
			printToConsole("ERROR: Can't create directory: " + dirname, Color.RED);
		}
	}

	private void loadBackgrouds() {
		if (!TurtleUtils.folderExists(dirname)) return;
		for (File file : TurtleUtils.getFolderFiles(dirname)) {
			if (file.getName().equals("empty.png")) continue;
			String ext = TurtleUtils.getExtension(file.getName());
			boolean isImage = (ext.equals(".png") || ext.equals(".jpg"));
			if (file.isFile() && isImage) {
				BufferedImage image = TurtleUtils.loadBufferedImage(file);
				backgrounds.add(image);
			}
		}
	}

	public void setBackgroundFromSelector(BufferedImage image, int index) {
		canvas.setBackgroundFromSelector(image, index);
	}

	public void setTurtleFromSelector(TurtlePack pack, int index) {
		canvas.setTurtleFromSelector(pack, index);
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface AbstractModule
	///////////////////////////////////////////////////

	public void init() {
		canvas.init();
	}

	public void reset() {
		canvas.reset();
	}

	public void start() {
		canvas.setWidth(1);
		canvas.setColor(1);
	}

	public void stop() {}
	public void exit() {}

	///////////////////////////////////////////////////
	// Gestionnaire d'instructions
	///////////////////////////////////////////////////

	public String handleRequest(Map<String, String> params) {
		String cmd = params.get("cmd");
		int color, width, distance, angle;
		int x1, y1, x2, y2, r;

		switch (cmd) {
			case "clearScreen":
				return String.valueOf(canvas.clearScreen());
			case "setColor":
				color = Integer.valueOf(params.get("color"));
				return String.valueOf(canvas.setColor(color));
			case "setWidth":
				width = Integer.valueOf(params.get("width"));
				return String.valueOf(canvas.setWidth(width));
			case "drawPoint":
				x1 = Integer.valueOf(params.get("x"));
				y1 = Integer.valueOf(params.get("y"));
				return String.valueOf(canvas.drawPoint(x1, y1));
			case "drawLine":
				x1 = Integer.valueOf(params.get("x1"));
				y1 = Integer.valueOf(params.get("y1"));
				x2 = Integer.valueOf(params.get("x2"));
				y2 = Integer.valueOf(params.get("y2"));
				return String.valueOf(canvas.drawLine(x1, y1, x2, y2));
			case "drawCircle":
				x1 = Integer.valueOf(params.get("x"));
				y1 = Integer.valueOf(params.get("y"));
				r = Integer.valueOf(params.get("r"));
				return String.valueOf(canvas.drawCircle(x1, y1, r));
			case "turtleForward":
				distance = Integer.valueOf(params.get("distance"));
				return String.valueOf(canvas.turtleForward(distance));
			case "turtleTurnRight":
				angle = Integer.valueOf(params.get("angle"));
				return String.valueOf(canvas.turtleTurnRight(angle));
			case "turtleTurnLeft":
				angle = Integer.valueOf(params.get("angle"));
				return String.valueOf(canvas.turtleTurnLeft(angle));
			case "turtleGoto":
				x1 = Integer.valueOf(params.get("x"));
				y1 = Integer.valueOf(params.get("y"));
				r = Integer.valueOf(params.get("theta"));
				return String.valueOf(canvas.turtleGoto(x1, y1, r));
			case "turtleShow":
				return String.valueOf(canvas.turtleShow());
			case "turtleHide":
				return String.valueOf(canvas.turtleHide());
			case "turtleReset":
				return String.valueOf(canvas.turtleReset());
			case "turtlePenUp":
				return String.valueOf(canvas.turtlePenUp());
			case "turtlePenDown":
				return String.valueOf(canvas.turtlePenDown());
			case "getBrightness":
				return String.valueOf(canvas.getBrightness());
			case "getColor":
				return String.valueOf(canvas.getColor());

			default:
				return unknownCommandError(cmd);
		}
	}
}

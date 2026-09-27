/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab.moduleGraphics;
import common.*;

public class Turtle extends SocketClient {

	private final String CLEARSCREEN_CMD = "module=MODULEGRAPHICS&cmd=clearScreen";
	private final String SETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=setColor&color=%d";
	private final String SETWIDTH_CMD = "module=MODULEGRAPHICS&cmd=setWidth&width=%d";

	private final String TURTLEFORWARD_CMD = "module=MODULEGRAPHICS&cmd=turtleForward&distance=%d";
	private final String TURTLETURNRIGHT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnRight&angle=%d";
	private final String TURTLETURNLEFT_CMD = "module=MODULEGRAPHICS&cmd=turtleTurnLeft&angle=%d";
	private final String TURTLEGOTO_CMD = "module=MODULEGRAPHICS&cmd=turtleGoto&x=%d&y=%d&theta=%d";
	private final String TURTLESHOW_CMD = "module=MODULEGRAPHICS&cmd=turtleShow";
	private final String TURTLEHIDE_CMD = "module=MODULEGRAPHICS&cmd=turtleHide";
	private final String TURTLERESET_CMD = "module=MODULEGRAPHICS&cmd=turtleReset";
	private final String TURTLEPENUP_CMD = "module=MODULEGRAPHICS&cmd=turtlePenUp";
	private final String TURTLEPENDOWN_CMD = "module=MODULEGRAPHICS&cmd=turtlePenDown";

	private final String GETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=getColor";
	private final String GETBRIGHTNESS_CMD = "module=MODULEGRAPHICS&cmd=getBrightness";

	public final static int COLOR_WHITE = 0;
	public final static int COLOR_BLACK = 1;
	public final static int COLOR_RED = 2;
	public final static int COLOR_GREEN = 3;
	public final static int COLOR_BLUE = 4;
	public final static int COLOR_CYAN = 5;
	public final static int COLOR_YELLOW = 6;
	public final static int COLOR_MAGENTA = 7;
	public final static int COLOR_ERROR = 8;

	public int clearScreen() {
		return Integer.parseInt(request(CLEARSCREEN_CMD));
	}

	public int setColor(int color) {
		String buffer = String.format(SETCOLOR_CMD, color);
		return Integer.parseInt(request(buffer));
	}

	public int setWidth(int width) {
		String buffer = String.format(SETWIDTH_CMD, width);
		return Integer.parseInt(request(buffer));
	}
	
	public int forward(int distance) {
		String buffer = String.format(TURTLEFORWARD_CMD, distance);
		return Integer.parseInt(request(buffer));
	}

	public int turnRight(int angle) {
		String buffer = String.format(TURTLETURNRIGHT_CMD, angle);
		return Integer.parseInt(request(buffer));
	}

	public int turnLeft(int angle) {
		String buffer = String.format(TURTLETURNLEFT_CMD, angle);
		return Integer.parseInt(request(buffer));
	}

	public int goto_(int x, int y, int theta) {
		String buffer = String.format(TURTLEGOTO_CMD, x, y, theta);
		return Integer.parseInt(request(buffer));
	}
	
	public int show() {
		return Integer.parseInt(request(TURTLESHOW_CMD));
	}

	public int hide() {
		return Integer.parseInt(request(TURTLEHIDE_CMD));
	}
	
	public int reset() {
		return Integer.parseInt(request(TURTLERESET_CMD));
	}
	
	public int penUp() {
		return Integer.parseInt(request(TURTLEPENUP_CMD));
	}
	
	public int penDown() {
		return Integer.parseInt(request(TURTLEPENDOWN_CMD));
	}

	public int getColor() {
		return Integer.parseInt(request(GETCOLOR_CMD));
	}

	public int getBrightness() {
		return Integer.parseInt(request(GETBRIGHTNESS_CMD));
	}
}

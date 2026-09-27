/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab.moduleGraphics;
import common.*;

public class Drawing extends SocketClient {

	private final String CLEARSCREEN_CMD = "module=MODULEGRAPHICS&cmd=clearScreen";
	private final String SETCOLOR_CMD = "module=MODULEGRAPHICS&cmd=setColor&color=%d";
	private final String SETWIDTH_CMD = "module=MODULEGRAPHICS&cmd=setWidth&width=%d";
	private final String DRAWLINE_CMD = "module=MODULEGRAPHICS&cmd=drawLine&x1=%d&y1=%d&x2=%d&y2=%d";
	private final String DRAWPOINT_CMD = "module=MODULEGRAPHICS&cmd=drawPoint&x=%d&y=%d";
	private final String DRAWCIRCLE_CMD = "module=MODULEGRAPHICS&cmd=drawCircle&x=%d&y=%d&r=%d";

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
	
	public int drawPoint(int x, int y) {
		String buffer = String.format(DRAWPOINT_CMD, x, y);
		return Integer.parseInt(request(buffer));
	}

	public int drawLine(int x1, int y1, int x2, int y2) {
		String buffer = String.format(DRAWLINE_CMD, x1, y1, x2, y2);
		return Integer.parseInt(request(buffer));
	}

	public int drawCircle(int x, int y, int r) {
		String buffer = String.format(DRAWCIRCLE_CMD, x, y, r);
		return Integer.parseInt(request(buffer));
	}
}

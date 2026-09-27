/*
 * Program _fileName_
 * Created by _author_ on _date_
 */

#include "codelab/moduleGraphics/graph2D.c"
#include "codelab/utils.c"

void main() {

	for (int i = 0 ; i < 4 ; i++) {
		printStrLn("go forward...");
		turtleForward(100);
		printStrLn("turn right...");
		turtleTurnRight(90);
	}
}

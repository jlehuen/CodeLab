/*
 * Program _fileName_
 * Created by _author_ on _date_
 */

#include "codelab/moduleRobotics/robotLego.c"
#include "codelab/utils.c"

void main() {
	setBackground("B01");
	setRobotConfiguration("NXT01");
	
	motorOn(OUT_BC, 50);
	waitFor(1000);
	motorOff(OUT_BC);
}

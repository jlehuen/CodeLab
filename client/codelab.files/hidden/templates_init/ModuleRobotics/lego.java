/**
 * Class _className_
 * Created by _author_ on _date_
 */

import codelab.moduleRobotics.RobotLego;
import codelab.Utils;

class _className_ extends RobotLego {

	_className_() {
		setBackground("B01");
		setRobotConfiguration("NXT01");
	}
	
	public void run() {
		motorOn(OUT_BC, 50);
		Utils.waitFor(1000);
		motorOff(OUT_BC);
	}
	
	public static void main(String args[]) {
		new _className_().run();
	}
}

/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab;
import common.*;

public class Joystick extends SocketClient {

	final String GETJOYSTICKX_CMD = "module=INOUT&cmd=getJoystickValueX";
	final String GETJOYSTICKY_CMD = "module=INOUT&cmd=getJoystickValueY";

	public int getValueX() {
		return Integer.parseInt(request(GETJOYSTICKX_CMD));
	}

	public int getValueY() {
		return Integer.parseInt(request(GETJOYSTICKY_CMD));
	}
}

/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab;
import common.*;

public class Numpad extends SocketClient {

	final String GETNUMPAD_CMD = "module=INOUT&cmd=getNumpadValue";

	public int getValue() {
		return Integer.parseInt(request(GETNUMPAD_CMD));
	}
}

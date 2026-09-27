/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab;
import common.*;

public class EventManager extends SocketClient {

	final String HASNEXTEVENT_CMD = "module=INOUT&cmd=hasNextEvent";
	final String GETNEXTEVENT_CMD = "module=INOUT&cmd=getNextEvent";
	final String RESETQUEUE_CMD = "module=INOUT&cmd=resetEventQueue";

	public boolean hasNextEvent() {
		return request(HASNEXTEVENT_CMD).equals("1");
	}

	public String getNextEvent() {
		return request(GETNEXTEVENT_CMD);
	}

	public int resetEventQueue() {
		return Integer.parseInt(request(RESETQUEUE_CMD));
	}
}

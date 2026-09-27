/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab.moduleRobotics;
import common.*;

public class RobotLego extends SocketClient {

	final String SETBACKGROUND_CMD = "module=MODULEROBOTICS&cmd=setBackground&name=%s";
	final String SETROBOTCONFIG_CMD = "module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s";

	final String MOTORON_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOn&port=%d&power=%d";
	final String MOTOROFF_CMD = "module=MODULEROBOTICS&robot=lego&cmd=motorOff&port=%d";
	final String GETARRAY_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorArray&port=%d&index=%d";
	final String GETSENSOR_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getSensorValue&port=%d";
	final String GETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=getMotorRotationCount&port=%d";
	final String RESETROTATION_CMD = "module=MODULEROBOTICS&robot=lego&cmd=resetMotorRotationCount&port=%d";

	public final static int OUT_A = 0;
	public final static int OUT_B = 1;
	public final static int OUT_C = 2;
	public final static int OUT_AB = 3;
	public final static int OUT_AC = 4;
	public final static int OUT_BC = 5;
	public final static int OUT_ABC = 6;

	public final static int IN_1 = 0;
	public final static int IN_2 = 1;
	public final static int IN_3 = 2;
	public final static int IN_4 = 3;

	public final static int COLOR_WHITE = 0;
	public final static int COLOR_BLACK = 1;
	public final static int COLOR_RED = 2;
	public final static int COLOR_GREEN = 3;
	public final static int COLOR_BLUE = 4;
	public final static int COLOR_CYAN = 5;
	public final static int COLOR_YELLOW = 6;
	public final static int COLOR_MAGENTA = 7;
	public final static int COLOR_ERROR = 8;

	public int motorOn(int port, int power) {
		String buffer;
		buffer = String.format(MOTORON_CMD, port, power);
		return Integer.parseInt(request(buffer));
	}

	public int motorOff(int port) {
		String buffer;
		buffer = String.format(MOTOROFF_CMD, port);
		return Integer.parseInt(request(buffer));
	}

	public int getSensorValue(int port) {
		String buffer;
		buffer = String.format(GETSENSOR_CMD, port);
		return Integer.parseInt(request(buffer));
	}

	public int[] getSensorArray(int port) {
		String buffer;
		int array[] = new int[8];
		for (int i = 0; i < 8; i++) {
			buffer = String.format(GETARRAY_CMD, port, i);
			array[i] = Integer.parseInt(request(buffer));
		}
		return array;
	}

	public int motorRotationCount(int port) {
		String buffer;
		buffer = String.format(GETROTATION_CMD, port);
		return Integer.parseInt(request(buffer));
	}

	public int resetMotorRotationCount(int port) {
		String buffer;
		buffer = String.format(RESETROTATION_CMD, port);
		return Integer.parseInt(request(buffer));
	}

	public int setBackground(String name) {
		String buffer;
		buffer = String.format(SETBACKGROUND_CMD, name);
		return Integer.parseInt(request(buffer));
	}

	public int setRobotConfiguration(String name) {
		String buffer;
		buffer = String.format(SETROBOTCONFIG_CMD, name);
		return Integer.parseInt(request(buffer));
	}
}

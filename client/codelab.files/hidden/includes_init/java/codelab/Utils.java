/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab;

import java.time.ZonedDateTime;
import java.util.Scanner;

public class Utils {

	public static void waitFor(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static long systemTime() {
		return ZonedDateTime.now().toInstant().toEpochMilli();
	}

	public static String readLine() {
		return new Scanner(System.in).nextLine();
	}
}

/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab;
import common.*;

public class ToneGenerator extends SocketClient {

	final String PLAYTONE_CMD = "module=AUDIO&cmd=playTone&freq=%d&time=%d&ampl=%d";
	final String PLAYTONEON_CMD = "module=AUDIO&cmd=playToneOn&freq=%d&ampl=%d";
	final String PLAYTONEOFF_CMD = "module=AUDIO&cmd=playToneOff";
	final String PLAYAUDIOFILE_CMD = "module=AUDIO&cmd=playAudioFile&filename=%s";
	final String LOOPAUDIOFILE_CMD = "module=AUDIO&cmd=loopAudioFile&filename=%s";
	final String STOPAUDIOPLAYER_CMD = "module=AUDIO&cmd=stopAudioPlayer";

	private int ampl = 25; // Amplitude dans [0, 100]

	public int play(int freq, int time) {
		String buffer;
		buffer = String.format(PLAYTONE_CMD, freq, time, ampl);
		return Integer.parseInt(request(buffer));
	}

	public int playToneOn(int freq) {
		String buffer;
		buffer = String.format(PLAYTONEON_CMD, freq, ampl);
		return Integer.parseInt(request(buffer));
	}

	public int playToneOff() {
		return Integer.parseInt(request(PLAYTONEOFF_CMD));
	}

	public int playAudioFile(String filename) {
		String buffer;
		buffer = String.format(PLAYAUDIOFILE_CMD, filename);
		return Integer.parseInt(request(buffer));
	}

	public int loopAudioFile(String filename) {
		String buffer;
		buffer = String.format(LOOPAUDIOFILE_CMD, filename);
		return Integer.parseInt(request(buffer));
	}

	public int stopAudioPlayer() {
		return Integer.parseInt(request(STOPAUDIOPLAYER_CMD));
	}
}

package codelab.utils.audio;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.SegmentedEnvelope;
import com.jsyn.engine.SynthesisEngine;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.SawtoothOscillator;
import com.jsyn.unitgen.SineOscillator;
import com.jsyn.unitgen.SquareOscillator;
import com.jsyn.unitgen.TriangleOscillator;
import com.jsyn.unitgen.UnitOscillator;
import com.jsyn.unitgen.VariableRateDataReader;
import com.jsyn.unitgen.VariableRateMonoReader;

/**
*	Classe utilitaire du synthétiseur
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

// http://www.softsynth.com/jsyn/
// http://www.softsynth.com/jsyn/docs/javadocs/
// http://www.softsynth.com/jsyn/docs/unitlist.php

// https://github.com/philburk/jsyn/blob/master/tests/com/jsyn/examples/PlayTone.java
// https://github.com/philburk/jsyn/blob/master/tests/com/jsyn/examples/PlayNotes.java
// https://github.com/philburk/jsyn/blob/master/tests/com/jsyn/examples/UseMidiKeyboard.java
// https://github.com/processing/processing-sound/issues/12

// https://www.javatips.net/api/vocobox-master/dev/java/vocobox-synth-jsyn/src/test/java/com/jsyn/examples/play/PlayChords.java

public class MiniSynth {

	private final Synthesizer synth;
	private UnitOscillator osc;
	private LineOut lineOut;

	// Envelope consisting of (duration,value) pairs
	private final double[] data1 = { 0.02, 1.0, 0.04, 0.0 };
	private VariableRateDataReader envelopePlayer;
	private SegmentedEnvelope envelope;
	private boolean noteOn = false;

	// Gestion de l'interruption / annulation immédiate de playTone
	private static volatile boolean cancelTone = false;
	private static volatile Thread activeToneThread = null;
	private static volatile boolean soundPlayed = false;

	// http://www.softsynth.com/jsyn/tutorial/TUT_HearEnvLoop.txt

	double[] data2 = {
			0.02, 1.0, // Take 0.02 seconds to go to value 1.0 (attack)
			0.01, 1.0, // Take 0.01 seconds to rise to value 1.0 (sustain)
			0.02, 0.0  // Take 0.02 seconds to drop to 0.0 (release)
	};

	///////////////////////////////////////////////////
	// Constructeur privé du singleton MiniSynth
	///////////////////////////////////////////////////

	private MiniSynth(String oscName) {

		// No logger message
		final Logger logger = Logger.getLogger(SynthesisEngine.class.getName());
		logger.setLevel(Level.OFF);

		// Create a context for the synthesizer
		synth = JSyn.createSynthesizer();

		// Add a tone generator
		switch (oscName) {
			case "SINE": osc = new SineOscillator(); break;
			case "SQUARE": osc = new SquareOscillator(); break;
			case "TRIANGLE": osc = new TriangleOscillator(); break;
			case "SAWTOOTH": osc = new SawtoothOscillator(); break;
			default: osc = new SineOscillator();
		}
		synth.add(osc);

		// Add an envelope player
		synth.add(envelopePlayer = new VariableRateMonoReader());
		envelopePlayer.output.connect(osc.amplitude);

		// Add an output mixer and connect the oscillator to the audio outputs
		synth.add(lineOut = new LineOut());
		osc.output.connect(0, lineOut.input, 0); // Connect to left channel
		osc.output.connect(0, lineOut.input, 1); // Connect to right channel

		// Start synthetizer
		synth.start();
		lineOut.start();
	}

	private void __playTone__(int hz, int ms, double amp) {
		cancelTone = false;
		activeToneThread = Thread.currentThread();
		soundPlayed = true;
		try {
			if (envelopePlayer != null && osc != null) {
				envelopePlayer.output.disconnectAll();
				envelopePlayer.output.connect(osc.amplitude);
			}
			if (lineOut != null) {
				lineOut.start();
			}
			osc.noteOn(hz, 1.0);
			data1[1] = amp; // Changer l'amplitude maximum
			data1[2] = Math.max(0.001, (double) ms / 1000 - 0.06); // 0.06 = 0.02 + 0.04 (évite durée négative si ms < 60)
			envelope = new SegmentedEnvelope(data1);
			envelopePlayer.dataQueue.clear();
			envelopePlayer.dataQueue.queue(envelope);

			long end = System.currentTimeMillis() + ms;
			while (!cancelTone && System.currentTimeMillis() < end) {
				long remaining = end - System.currentTimeMillis();
				if (remaining <= 0) break;
				try {
					Thread.sleep(Math.min(50, remaining));
				} catch (InterruptedException e) {
					break;
				}
			}
		} finally {
			try {
				if (osc != null) {
					osc.noteOff();
				}
			} catch (Exception ignored) {}
			if (activeToneThread == Thread.currentThread()) {
				activeToneThread = null;
			}
		}
	}

	private void __playToneOn__(int hz, double amp) {
		cancelTone = false;
		soundPlayed = true;
		if (envelopePlayer != null && osc != null) {
			envelopePlayer.output.disconnectAll();
			envelopePlayer.output.connect(osc.amplitude);
		}
		if (lineOut != null) {
			lineOut.start();
		}
		osc.noteOn(hz, 1.0);
		data2[1] = amp;
		data2[3] = amp;
		envelope = new SegmentedEnvelope(data2);
		envelopePlayer.dataQueue.clear();
		envelopePlayer.dataQueue.queue(envelope, 0, 1);
		envelopePlayer.dataQueue.queueLoop(envelope, 1, 1);
		noteOn = true;
	}

	private void __playToneOff__() {
		if (noteOn && envelope != null) {
			envelopePlayer.dataQueue.queue(envelope, 2, 1);
			osc.noteOff();
			noteOn = false;
		}
	}

	private void __stop__() {
		cancelTone = true;
		Thread t = activeToneThread;
		if (t != null) {
			t.interrupt();
		}
		try {
			if (envelopePlayer != null && envelopePlayer.dataQueue != null) {
				envelopePlayer.dataQueue.clear();
				envelopePlayer.dataQueue.queue(new SegmentedEnvelope(new double[]{0.001, 0.0}));
			}
			if (osc != null) {
				osc.noteOff();
			}
			noteOn = false;
		}
		catch (Exception ignored) {}
	}

	private void __reset__() {
		__stop__();
		try {
			synth.clearCommandQueue();
			synth.stop();
		} catch (Exception ignored) {}
	}

	///////////////////////////////////////////////////
	// Méthodes statiques publiques
	///////////////////////////////////////////////////

	// Instance unique pré-initialisée (singleton)
	private static volatile MiniSynth INSTANCE = new MiniSynth("SINE");

	public static synchronized void reset(String oscName) {
		INSTANCE.__reset__();
		INSTANCE = new MiniSynth(oscName);
		INSTANCE.__playTone__(1000, 10, 0);
		soundPlayed = false;
	}

	public static int stop() {
		INSTANCE.__stop__();
		return 1;
	}

	public static boolean hasSoundPlayed() {
		return soundPlayed;
	}

	public static int playTone(int hz, int ms, int amp) {
		if (ms <= 0 || hz <= 0) return 0;
		if (amp < 0) amp = 0;
		if (amp > 100) amp = 100;
		try {
			INSTANCE.__playTone__(hz, ms, (double) amp / 100);
		} catch (Exception e) {
			return 0;
		}
		return 1;
	}

	public static int playToneOn(int hz, int amp) {
		if (hz <= 0) return 0;
		if (amp < 0) amp = 0;
		if (amp > 100) amp = 100;
		try {
			INSTANCE.__playToneOn__(hz, (double)amp/100);
		} catch (Exception e) {
			return 0;
		}
		return 1;
	}

	public static int playToneOff() {
		try {
			INSTANCE.__playToneOff__();
		} catch (Exception e) {
			return 0;
		}
		return 1;
	}
}

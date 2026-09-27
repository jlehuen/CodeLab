package codelab.utils.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import codelab.ExceptionManager;

/**
*	Classe utilitaire du lecteur de fichiers audio
*	@author Jérôme Lehuen
*	@version 06/12/20
*/

// https://www.codejava.net/coding/how-to-play-back-audio-in-java-with-examples
// https://www3.ntu.edu.sg/home/ehchua/programming/java/J8c_PlayingSound.html

public class AudioPlayer_v1 implements Runnable {

	private static final int BUFFER_SIZE = 4096;

	private AudioInputStream audioStream;
	private SourceDataLine audioLine;
	private AudioFormat format;
	private boolean stop = false;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AudioPlayer_v1(InputStream is) throws IOException, UnsupportedAudioFileException {
		try {
			audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
			format = audioStream.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			audioLine = (SourceDataLine) AudioSystem.getLine(info);
			audioLine.open(format);
		}
		catch (LineUnavailableException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthodes de l'interface Runnable
	///////////////////////////////////////////////////

	public void run() {
		byte[] bytesBuffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;

		try {
			audioLine.start();
			while ((bytesRead = audioStream.read(bytesBuffer)) != -1 && !stop) {
				audioLine.write(bytesBuffer, 0, bytesRead);
			}
			audioLine.drain();
			audioLine.close();
			audioStream.close();
			MiniPlayer_v1.remove(this);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public void stop() {
		stop = true;
	}
}

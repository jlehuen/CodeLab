package codelab.utils.audio;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import codelab.ExceptionManager;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
*	Player audio (version 2)
*	@author Jérôme Lehuen
*	@version 08/03/21
*/

public class AudioPlayer_v2 implements LineListener, Runnable {

	private Clip clip;
	private boolean completed;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AudioPlayer_v2(File file) throws IOException, UnsupportedAudioFileException {
		try {
			clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(file));
			clip.addLineListener(this);
		}
		catch (LineUnavailableException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthode de l'interface LineListener
	///////////////////////////////////////////////////

	public void update(LineEvent event) {
		LineEvent.Type type = event.getType();
		if (type == LineEvent.Type.STOP) {
			completed = true;
		}
	}

	///////////////////////////////////////////////////
	// Méthode de l'interface Runnable
	///////////////////////////////////////////////////

	public void run() {
		clip.start();
		while (!completed) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				ExceptionManager.process(e);
			}
		}
	}

	///////////////////////////////////////////////////
	// Autres méthodes publiques
	///////////////////////////////////////////////////

	public void play() {
		completed = false;
		new Thread(this).start();
	}

	public void stop() {
		clip.stop();
		completed = true;
		//MiniPlayer_v2.remove(this);
	}

	public void loop() {
		clip.loop(Clip.LOOP_CONTINUOUSLY);
		play();
	}
}

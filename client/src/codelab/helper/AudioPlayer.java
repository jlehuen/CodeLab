package codelab.helper;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import codelab.AbstractModule;
import codelab.CodeLab;
import codelab.ExceptionManager;

/**
*	Player audio (version 2)
*	@author Jérôme Lehuen
*	@version 22/03/22
*/

public class AudioPlayer implements LineListener, Runnable {

	private Clip clip;
	private boolean completed;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public AudioPlayer(AbstractModule module, String filename) throws IOException, UnsupportedAudioFileException {
		try {
			URL url = module.getClass().getResource("/data/" + filename);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
			clip = AudioSystem.getClip();
			clip.open(audioStream);
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
		if (!CodeLab.SOUND_EFFECTS) return;
		completed = false;
		new Thread(this).start();
	}

	public void stop() {
		clip.stop();
		completed = true;
	}

	public void loop() {
		if (!CodeLab.SOUND_EFFECTS) return;
		clip.loop(Clip.LOOP_CONTINUOUSLY);
		play();
	}
}

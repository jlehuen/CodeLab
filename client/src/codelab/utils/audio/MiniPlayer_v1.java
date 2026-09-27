package codelab.utils.audio;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import codelab.CodeLab;

// https://www.soundsnap.com/search/audio

/**
*	Classe du lecteur de fichiers audio de CodeLab
*	@author Jérôme Lehuen
*	@version 10/10/23
*/

public class MiniPlayer_v1 {

	private static ArrayList<AudioPlayer_v1> players = new ArrayList<AudioPlayer_v1>();

	public static void remove(AudioPlayer_v1 player) {
		players.remove(player);
	}

	public static int stopAll() {
		for (AudioPlayer_v1 player : players) player.stop();
		players = new ArrayList<AudioPlayer_v1>();
		return 1;
	}

	///////////////////////////////////////////////////
	// Constructeur pour fichier dans folder data
	///////////////////////////////////////////////////

	// Usage: new MiniPlayer("dagobert.wav");

	public MiniPlayer_v1(String filename) {
		try {
			InputStream inputStream = getClass().getResourceAsStream("/data/" + filename);
			AudioPlayer_v1 player = new AudioPlayer_v1(inputStream);
			//players.add(player); Surtout pas ici car c'est pour le logiciel
			new Thread(player).start();
		}
		catch (IOException e) {
			System.out.format("MiniPlayer ERROR: Can't find file %s\n", filename);
		}
		catch (UnsupportedAudioFileException e) {
			System.out.format("MiniPlayer ERROR: Unsupported audio format %s\n", filename);
		}
		catch (Exception e) {
			System.out.format("MiniPlayer ERROR: Can't open audio player (%s)\n", e.getMessage());
		}
	}

	///////////////////////////////////////////////////
	// Méthode de classe pour fichier ailleurs
	///////////////////////////////////////////////////

	// Usage 1: MiniPlayer.play("audiofiles/dagobert.wav");
	// Usage 2: MiniPlayer.play("/path/to/music/sound.wav");

	public static int play(String filename) {
		filename = CodeLab.USER_DATA_FOLDER + "/audiofiles/" + filename;
		try {
			InputStream inputStream = new FileInputStream(filename);
			AudioPlayer_v1 player = new AudioPlayer_v1(inputStream);
			players.add(player);
			new Thread(player).start();
			return 0;
		}
		catch (IOException e) {
			System.out.format("MiniPlayer ERROR: Can't find file %s\n", filename);
			return -1;
		}
		catch (UnsupportedAudioFileException e) {
			System.out.format("MiniPlayer ERROR: Unsupported audio format %s\n", filename);
			return -2;
		}
		catch (Exception e) {
			System.out.format("MiniPlayer ERROR: Can't open audio player (%s)\n", e.getMessage());
			return -3;
		}
	}
}

package codelab.utils.audio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.UnsupportedAudioFileException;

import codelab.CodeLab;
import codelab.console.Console;

/**
*	Gestionnaire du player audio (version 2)
*	@author Jérôme Lehuen
*	@version 18/05/21
*/

public class MiniPlayer_v2 {

	private static ArrayList<AudioPlayer_v2> players = new ArrayList<AudioPlayer_v2>();
	private static int returnValue; // Valeur retournée par l'instruction

	public static void remove(AudioPlayer_v2 player) {
		players.remove(player);
	}

	///////////////////////////////////////////////////
	// Constructeur statique
	///////////////////////////////////////////////////

	private static AudioPlayer_v2 newPlayer(File file) {
		boolean isblocs = CodeLab.INSTANCE.getEditor().isBlocs();
		try {
			AudioPlayer_v2 player = new AudioPlayer_v2(file);
			players.add(player);
			returnValue = 0;
			return player;
		}
		catch (IOException e) {
			returnValue = -1;
			if (isblocs) return null; // Gestion de l'erreur par Scratch

			String msg = String.format("ERROR AudioPlayer: Can't find file (%s)\n", file);
			CodeLab.INSTANCE.printToConsole(msg, Console.COLOR_ERROR, false);
			CodeLab.INSTANCE.halt();
			return null;
		}
		catch (UnsupportedAudioFileException e) {
			returnValue = -2;
			if (isblocs) return null; // Gestion de l'erreur par Scratch

			String msg = String.format("ERROR AudioPlayer: Unsupported audio format (%s)\n", file);
			CodeLab.INSTANCE.printToConsole(msg, Console.COLOR_ERROR, false);
			CodeLab.INSTANCE.halt();
			return null;
		}
	}

	///////////////////////////////////////////////////
	// Méthodes statiques publiques
	///////////////////////////////////////////////////

	public static int play(File file) {
		AudioPlayer_v2 player = newPlayer(file);
		if (player == null) return returnValue;
		player.play();
		return 0;
	}

	public static int loop(File file) {
		AudioPlayer_v2 player = newPlayer(file);
		if (player == null) return returnValue;
		player.loop();
		return 0;
	}
	public static int stopAll() {
		for (AudioPlayer_v2 player : players) player.stop();
		players.clear();
		return 0;
	}
}

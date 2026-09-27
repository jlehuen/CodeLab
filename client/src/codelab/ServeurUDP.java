package codelab;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

/**
*	Classe du serveur de commandes
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 25/09/26
*/

public class ServeurUDP implements Runnable {

	private Thread thread;
	private CodeLab codelab;
	private DatagramSocket socket;
	private DatagramPacket input;
	private DatagramPacket output;
	private byte[] buffer;

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ServeurUDP(CodeLab codelab, int port) {
		this.codelab = codelab;
		try {
			buffer = new byte[256];
			socket = new DatagramSocket(port);
			input = new DatagramPacket(buffer, buffer.length);
		}
		catch (SocketException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Démarrage et arrêt du serveur
	///////////////////////////////////////////////////

	private boolean isRunning;

	public boolean isRunning() {
		return isRunning;
	}

	public void start() {
		isRunning = true;
		thread = new Thread(this);
		thread.start();
		//System.out.format("Internal execution server started on port %d\n", port);
	}

	public void stop() {
		isRunning = false;
		socket.close();
		//System.out.format("Internal execution server halted\n");
	}

	///////////////////////////////////////////////////
	// Méthode de Runnable
	///////////////////////////////////////////////////

	public void run() {
		String request, response;
		while (isRunning) {
			try {
				input.setLength(buffer.length); // Réinitialiser impérativement la capacité du paquet avant chaque réception
				socket.receive(input);
				request = new String(input.getData(), 0, input.getLength());
				response = codelab.handleRequest(request);
				if (response == null) response = "";
				output = new DatagramPacket(response.getBytes(), response.length(), input.getAddress(), input.getPort());
				socket.send(output);
			}
			catch (SocketException e) {
				// Socket closed lors de stop()
				if (isRunning) ExceptionManager.process(e);
			}
			catch (IOException e) {
				if (isRunning) ExceptionManager.process(e);
			}
			catch (Exception e) {
				// Protéger le thread du serveur UDP contre toute RuntimeException non gérée
				ExceptionManager.process(e);
				try {
					String err = "ERROR";
					output = new DatagramPacket(err.getBytes(), err.length(), input.getAddress(), input.getPort());
					socket.send(output);
				} catch (Exception ignored) {}
			}
		}
	}
}

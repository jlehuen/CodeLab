package codelab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
*	Classe du serveur de commandes
*	@author Jérôme Lehuen
*	@version 10/12/22
*/

public class ServeurTCP implements Runnable {

	private Thread thread;
	private ServerSocket listener;
	private CodeLab codelab;
	private Socket socket;
	private int port;

	private BufferedReader in;
	private PrintWriter out;

	// https://rom.developpez.com/java-synchronisation/ --> Executor ??

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public ServeurTCP(CodeLab codelab, int port) {
		this.codelab = codelab;
		this.port = port;
		try {
			listener = new ServerSocket(port);
			//listener.setSoTimeout(2000);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Démarrage et arrêt du serveur
	///////////////////////////////////////////////////

	private boolean isRunning;

	public void start() {
		isRunning = true;
		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		isRunning = false;
		try {
			listener.close();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	///////////////////////////////////////////////////
	// Méthode de Runnable
	///////////////////////////////////////////////////

	public void run() {
		String request, response;
		System.out.format("Internal server started on port %d...\n", port);
		while (isRunning) {
			try {
				socket = listener.accept();
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				request = in.readLine();
				response = codelab.handleRequest(request);
				out.println(response);
				in.close();
				out.close();
				socket.close();
			}
			catch (SocketTimeoutException e1) {
				// Utiliser avec listener.setSoTimeout
			}
			catch (SocketException e2) {
				if (isRunning) e2.printStackTrace();
			}
			catch (IOException e3) {
				if (isRunning) e3.printStackTrace();
			}
		}
		System.out.println("Internal server halted");
	}
}

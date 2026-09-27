/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package common;

import java.io.*;
import java.net.*;

public class TCPClient {

	private String HOST = "localhost";
	private int PORT = 8500;
	private int DELAY = 5; // Pour laisser le temps au serveur de ré-écouter

	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private String response;

	public String request(String msg) {
		try {
			open();
			out.println(msg+'\n');
			response = in.readLine();
			close();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		sleep(DELAY);
		return response;
	}

	private void open() throws UnknownHostException, IOException {
		socket = new Socket(HOST, PORT);
		out = new PrintWriter(socket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private void close() throws IOException {
		out.close();
		in.close();
		socket.close();
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

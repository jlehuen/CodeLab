/*
#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################
*/

// DO NOT DELETE OR MODIFY THIS FILE !!

package common;

import java.io.*;
import java.net.*;

public class UDPClient {

	private final int SIZE = 100; // Taille des packets en réception
	private final int DELAY = 5; // Pour laisser le temps au serveur de ré-écouter
	private int port; // Initialisé dans le constructeur de UDPClient

	private InetAddress addr;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private byte[] buffer;

	public UDPClient() {
		try {
			port = Integer.parseInt(System.getenv("INTERNAL_PORT"));
			addr = InetAddress.getByName("localhost");
			socket = new DatagramSocket();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String request(String msg) {
		try {
			buffer = msg.getBytes();
			packet = new DatagramPacket(buffer, buffer.length, addr, port);
			socket.send(packet);
			packet.setData(new byte[SIZE]);
			socket.receive(packet);
			sleep(DELAY);
			return new String(packet.getData(), 0, packet.getLength());
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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

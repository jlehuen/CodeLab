package codelab.utils.clips;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import codelab.ExceptionManager;
import jess.JessException;
import jess.RU;
import jess.Value;
import jess.ValueVector;

/**
*	Classe utilitaire pour CLIPS
*	@author Jérôme Lehuen
*	@version 28/11/20
*/

public class CLIPSTools {

	///////////////////////////////////////////////////
	// Implémentation statique de request
	///////////////////////////////////////////////////

	private static String HOST = "localhost";
	private static int PORT = 8500;

	private static InetAddress addr;
	private static DatagramSocket socket;
	private static DatagramPacket output;
	private static DatagramPacket input;
	private static byte[] buffer;

	public static String request(String msg) {
		try {
			addr = InetAddress.getByName(HOST);
			buffer = msg.getBytes();
			output = new DatagramPacket(buffer, buffer.length, addr, PORT);
			socket = new DatagramSocket();
			output.setData(buffer);
			socket.send(output);
			buffer = new byte[1024];
			input = new DatagramPacket(buffer, buffer.length, addr, PORT);
			socket.receive(input);
			return new String(input.getData(), 0, input.getLength());
		}
		catch (UnknownHostException e) {
			ExceptionManager.process(e);
			return null;
		}
		catch (SocketException e) {
			ExceptionManager.process(e);
			return null;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
	}

	///////////////////////////////////////////////////
	// Implémentation en Java de getSensorArray
	///////////////////////////////////////////////////

	private static String GETARRAY_CMD = "module=robot&robot=lego&commande=getSensorArray&port=%d&index=%d";

	public static Value getSensorArray(int port) {
		try {
			ValueVector vector = new ValueVector();
			String buffer;
			for (int i = 0; i < 8; i++) {
				buffer = String.format(GETARRAY_CMD, port, i);
				vector.add(new Value(request(buffer), RU.INTEGER));
			}
			return new Value(vector, RU.LIST);
		}
		catch (JessException e) {
			ExceptionManager.process(e);
			return null;
		}
	}
}

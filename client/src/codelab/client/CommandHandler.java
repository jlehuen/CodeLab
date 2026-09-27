package codelab.client;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;

import java.lang.reflect.Method;
import java.net.SocketException;
import java.lang.reflect.InvocationTargetException;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import codelab.CodeLab;

/**
*	Exécution des commandes reçues du serveur
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

public class CommandHandler implements Runnable {

	private CodelabClient client;
	private InputStream input;

	///////////////////////////////////////////////////
	// Constructeur invoqué dans ServerFacade
	///////////////////////////////////////////////////

	public CommandHandler(CodelabClient client, InputStream input) throws IOException {
		this.client = client;
		this.input = input;
		Thread thread = new Thread(this);
		thread.start();
	}

	///////////////////////////////////////////////////
	// Méthode de runnable
	///////////////////////////////////////////////////

	public void run() {
		while (client.isConnected()) {
			try {
				MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(input);
				Command command = new Command(unpacker);
				if (CodelabClient.DEBUG) command.describe();
				invoke(command);
			}
			catch (SocketException e) {
				if (client.isConnected()) {
					CodeLab.logger("SocketException while connected: " + e.getMessage());
					client.connectionBroken();
				} else {
					CodeLab.logger("Socket closed");
				}
				break;
			}
			catch (EOFException e) {
				client.connectionBroken();
				break;
			}
			catch (Exception e) {
				client.connectionBroken();
				break;
			}
		}
	}

	///////////////////////////////////////////////////
	// Invocation d'une méthode de CodelabClient
	///////////////////////////////////////////////////

	private void invoke(Command cmd) {
		Method m = null;
		Class<CodelabClient> classCodelabClient = CodelabClient.class;
		Method[] methods = classCodelabClient.getMethods();
		// Rechercher la méthode à partir de son nom et de son nombre d'arguments
		for (int i = 0 ; (i < methods.length) && (m == null) ; i++)
			if (methods[i].getName().equals(cmd.name) && (methods[i].getParameterTypes().length == cmd.nbargs))
				m = methods[i];

		if (m == null) {
			System.out.printf("ERROR: Method [%s] with %d args not implemented\n", cmd.name, cmd.nbargs);
			return;
		}
		try {
			m.invoke(client, cmd.args);
		}
		catch (InvocationTargetException e) { e.printStackTrace(); }
		catch (IllegalArgumentException e) { e.printStackTrace(); }
		catch (IllegalAccessException e) { e.printStackTrace(); }
	}
}

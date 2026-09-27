package codelab.client;

import java.io.*;
import java.net.Socket;

import java.nio.ByteBuffer;

import org.msgpack.core.MessagePack;

import codelab.CodeLab;

import org.msgpack.core.MessageBufferPacker;

/**
*	Classe qui exécute le CommandHandler et qui
*	sérialise et envoie les commandes MessagePack
*	@author Jérôme Lehuen + Gemini 3.8
*	@version 14/09/26
*/

abstract public class AbstractServerFacade {

	private Socket socket;
	private InputStream input;
	private OutputStream output;
	private boolean isConnected = false;
	private CodelabClient client;

	public boolean isConnected() {
		return isConnected;
	}

	///////////////////////////////////////////////////
	// Constructeur de AbstractServerFacade
	///////////////////////////////////////////////////

	AbstractServerFacade(Socket socket, CodelabClient client) {
		try {
			this.socket = socket;
			this.client = client;
			//socket.setSoTimeout(30000); // Ajouter un timeout de 30 secondes
			input = socket.getInputStream();
			output = socket.getOutputStream();
			new CommandHandler(client, input);
			isConnected = true;
		}
		catch (IOException e) {
			CodeLab.logger("Connection with server failed");
			closeConnection();
		}
	}

	///////////////////////////////////////////////////
	// Ferme la connexion avec le serveur
	///////////////////////////////////////////////////

	public void closeConnection() {
		if (!isConnected) return; // Éviter de fermer plusieurs fois
		isConnected = false; // Mettre à jour l'état AVANT de fermer les connexions
		try {
			if (input != null) {
				try { input.close(); } catch (IOException e) { /* ignore */ }
				input = null;
			}
			if (output != null) {
				try { output.close(); } catch (IOException e) { /* ignore */ }
				output = null;
			}
			if (socket != null) {
				try { socket.close(); } catch (IOException e) { /* ignore */ }
				socket = null;
			}
		} catch (Exception e) {
			CodeLab.logger("Error closing socket: " + e.getMessage());
		}
	}

	///////////////////////////////////////////////////
	// Envoie une commande asynchrone
	///////////////////////////////////////////////////

	protected void sendCommand(String cmd, Object[] data) {
		try {
			byte[] message = serializeMessage(cmd, data);
			byte[] length = ByteBuffer.allocate(4).putInt(message.length).array();
			if (CodelabClient.DEBUG) MessagePackUtils.printAsJson(message);
			output.write(length); // Envoi de la taille du message sur 4 octets
			output.write(message); // Envoi du message sérialisé en MessagePack
			output.flush();
			CodeLab.INSTANCE.serverActivity(); // LED clignotante
		}
		catch (IOException e) {
			CodeLab.logger("Connection with server broken (write failed)");
			closeConnection();
			if (client != null) client.connectionBroken();
		}
		catch (Exception e) {
			CodeLab.logger("Error in sendCommand: " + e.getMessage());
		}
	}

	///////////////////////////////////////////////////
	// Sérialise un objet en MessagePack
	///////////////////////////////////////////////////

	private byte[] serializeMessage(String cmd, Object[] data) throws Exception {
		try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packMapHeader(2);
			packer.packString("cmd");
			packer.packString(cmd);
			packer.packString("data");
			packer.packArrayHeader(data.length);

			for (Object object : data) {
				packer.packMapHeader(2);
				packer.packString("type");

				if (object instanceof String) {
					packer.packString("String");
					packer.packString("value");
					packer.packString((String) object);

				} else if (object instanceof Integer) {
					packer.packString("Number");
					packer.packString("value");
					packer.packInt((Integer) object);

				} else if (object instanceof Boolean) {
					packer.packString("Bool");
					packer.packString("value");
					packer.packBoolean((Boolean) object);

				} else if (object instanceof byte[]) {
					packer.packString("Binary");
					packer.packString("value");
					byte[] array = (byte[]) object;
					packer.packBinaryHeader(array.length);
					packer.writePayload(array);

				} else throw new IllegalArgumentException("Type de donnée non supporté : " + object.getClass());
			}
			return packer.toByteArray();
		}
	}
}

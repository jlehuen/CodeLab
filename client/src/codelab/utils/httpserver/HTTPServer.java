package codelab.utils.httpserver;

import com.sun.net.httpserver.HttpServer;

import codelab.CodeLab;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HTTPServer {

    ///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public HTTPServer(int port) throws IOException {

		HttpServer server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
		server.createContext("/", new MyHandler());
		server.setExecutor(null);
		server.start();
        CodeLab.INSTANCE.consoleLog("HTTP server started on port " + port);
	}

	static class MyHandler implements HttpHandler {

		public void handle(HttpExchange exchange) throws IOException {

			// Récupération de l'URI de la requête
			String requestUri = exchange.getRequestURI().toString();

			// Extrait le nom du fichier à partir de l'URI
			String fileName = requestUri.substring(1); // Ignorer le premier caractère "/" dans l'URI

			// Chemin du fichier HTML à renvoyer
			Path filePath = Paths.get(fileName);

			// Vérification de l'existence du fichier
			if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
				// Lecture du contenu du fichier HTML
				byte[] fileContent = Files.readAllBytes(filePath);

				// Spécification du code de réponse 200 (OK)
				exchange.sendResponseHeaders(200, fileContent.length);

				// Écriture du contenu du fichier dans la sortie
				OutputStream os = exchange.getResponseBody();
				os.write(fileContent);
				os.close();
			} else {
				String response = "ERROR 404: File not found";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}
	}
}
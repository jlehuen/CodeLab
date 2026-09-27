package codelab.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;

/**
*	Classe du téléchargeur de fichiers
*	@author Jérôme Lehuen
*	@version 20/09/23
*/

public class Downloader {

	private String filename;
	private JFrame parent;

	private int contentLength;
	private InputStream inputStream;
	private FileOutputStream outputStream;
	private HttpURLConnection httpConn;

	private boolean ready = false;
	private boolean completed = false;
	public boolean isReady() { return ready; }
	public boolean isCompleted() { return completed; }

	///////////////////////////////////////////////////
	// Constructeur
	///////////////////////////////////////////////////

	public Downloader(JFrame parent, String filename, String base_url, String dir, int timeout) {
		this.parent = parent;
		this.filename = filename;
		try {
			URL url = URI.create(base_url + filename).toURL();
			httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setConnectTimeout(timeout);
			httpConn.setReadTimeout(timeout);
			System.out.print("Connecting to server... ");
			int responseCode = httpConn.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				contentLength = httpConn.getContentLength();
				inputStream = httpConn.getInputStream();
				if (inputStream.available() > 0) {
					String saveFilePath = dir + File.separator + filename;
					outputStream = new FileOutputStream(saveFilePath);
					System.out.println("OK");
					ready = true;
				}
				else { System.out.println("ERROR 1"); closeAll(); return; }
			}
			else { System.out.println("ERROR 2"); closeAll(); return; }
		}
		catch (SocketTimeoutException e) { System.out.println("TIMEOUT"); closeAll(); }
		catch (IOException e) { System.out.println("IOERROR"); closeAll(); }
	}

	private void closeAll() {
		httpConn.disconnect();
		try { outputStream.close(); }
		catch (IOException e) {} // Peut arriver en cas de TIMEOUT
		try { inputStream.close(); }
		catch (IOException e) {} // Peut arriver en cas de TIMEOUT
	}

	///////////////////////////////////////////////////x
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void start() {
		int BUFFER_SIZE = 4096;
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;

		try {
			BufferedInputStream bis = new BufferedInputStream(inputStream);
			ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent, filename, bis);
			ProgressMonitor monitor = pmis.getProgressMonitor();
			monitor.setMillisToDecideToPopup(0);
			monitor.setMillisToPopup(0);
			monitor.setMaximum((int) contentLength);

			System.out.format("Downloading %s... ", filename);
			while(!monitor.isCanceled() && (bytesRead = pmis.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			monitor.close();
			pmis.close();
			bis.close();
			closeAll();
			System.out.println("DONE");
			completed = true;
		}
		catch (InterruptedIOException e) {
			System.out.println("CANCELLED");
		}
		catch (IOException e) {
			System.out.println("ERROR");
		}
	}
}

package codelab.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

public class SerializedFile implements Serializable {

	private static final long serialVersionUID = 1L;

	private Vector<Integer> contenu = new Vector<Integer>();
	private String filename = "empty.txt";

	public String getFilename() { return filename; }

	///////////////////////////////////////////////////
	// Constructeurs
	///////////////////////////////////////////////////

	public SerializedFile() {
		// Rien à faire
	}

	public SerializedFile(String filename) {
		this(filename, new File(filename));
	}

	public SerializedFile(String filename, File internalFile) {
		this.filename = filename;
		try {
			FileInputStream fileInputStream = new FileInputStream(internalFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			int octet;
			while ((octet = bufferedInputStream.read()) != -1) {
				contenu.add(Integer.valueOf(octet));
			}
			bufferedInputStream.close();
			fileInputStream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	///////////////////////////////////////////////////
	// Méthodes publiques
	///////////////////////////////////////////////////

	public void write1(String destination) throws IOException {
		// Pour écrire le fichier dans un dossier destination
		File file = new File(destination + File.separator + filename);
		write(file);
	}

	public void write2(String path) throws IOException {
		// Pour écrire le fichier dans un fichier destination
		File file = new File(path);
		write(file);
	}

	public void write2(File file) throws IOException {
		// Pour écrire le fichier dans un fichier destination
		write(file);
	}

	///////////////////////////////////////////////////
	// Méthodes privées
	///////////////////////////////////////////////////

	private void write(File file) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
		for (int i = 0 ; i < contenu.size() ; i++) {
			bufferedOutputStream.write((contenu.elementAt(i)).byteValue());
		}
		bufferedOutputStream.flush();
		bufferedOutputStream.close();
		fileOutputStream.close();
	}
}

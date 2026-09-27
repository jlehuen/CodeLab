package codelab.client;

import java.io.*;
import java.nio.file.Files;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
*	Classe qui décompresse un fichier Zstandard
*	et extrait le contenu d'une archive TAR
*	@author Jérôme Lehuen
*	@version 10/05/25
*/

public class TarZstExtractor {

	private static final int BUFFER_SIZE = 8192; // Taille du tampon de décompression (8 Ko)

	public static void extract(String tarZstFile, String outputDir) throws IOException {
		// Décompresser le fichier Zstandard pour obtenir le fichier TAR
		File tarFile = decompressZst(tarZstFile);
		// Extraire l'archive TAR
		extractTar(tarFile, outputDir);
		// Supprimer le fichier TAR temporaire
		Files.delete(tarFile.toPath());
	}

	private static File decompressZst(String zstFile) throws IOException {
		File tarFile = new File(zstFile.replace(".zst", ""));
		try (InputStream zstdInputStream = new ZstdInputStream(new FileInputStream(zstFile));
			 OutputStream tarOutputStream = new FileOutputStream(tarFile)) {

			byte[] buffer = new byte[BUFFER_SIZE]; 
			int bytesRead;
			while ((bytesRead = zstdInputStream.read(buffer)) != -1) {
				tarOutputStream.write(buffer, 0, bytesRead);
			}
		}
		return tarFile;
	}

	public static void extractTar(File tarFile, String outputDir) throws IOException {
		try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {

			TarArchiveEntry entry;
			while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
				File outputFile = new File(outputDir, entry.getName());

				if (entry.isDirectory()) {
					if (!outputFile.exists() && !outputFile.mkdirs()) {
						throw new IOException("Failed to create directory " + outputFile);
					}
				} else {
					File parentDir = outputFile.getParentFile();
					if (!parentDir.exists() && !parentDir.mkdirs()) {
						throw new IOException("Failed to create directory " + parentDir);
					}

					try (OutputStream outputStream = new FileOutputStream(outputFile)) {
						byte[] buffer = new byte[BUFFER_SIZE];
						int bytesRead;
						while ((bytesRead = tarInputStream.read(buffer)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
					}
				}
			}
		}
	}
}

package codelab.client;

import java.io.*;
import java.nio.file.Files;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;

/**
 *	Classe qui décompresse un fichier Zstandard et extrait le contenu d'une archive TAR
 *	avec réconciliation intelligente et préservation des horodatages.
 *	@author Jérôme Lehuen
 *	@version 29/09/26
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

	public static void extractAndReconcile(String tarZstFile, String outputDir) throws IOException {
		File tarFile = decompressZst(tarZstFile);
		extractTarAndReconcile(tarFile, outputDir);
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
				String entryName = entry.getName().replace('\\', '/');
				if (entryName.startsWith("./")) {
					entryName = entryName.substring(2);
				}
				if (entryName.isEmpty()) continue;

				File outputFile = new File(outputDir, entryName);

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
					if (entry.getLastModifiedDate() != null) {
						outputFile.setLastModified(entry.getLastModifiedDate().getTime());
					}
				}
			}
		}
	}

	public static void extractTarAndReconcile(File tarFile, String outputDir) throws IOException {
		try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(new FileInputStream(tarFile))) {
			TarArchiveEntry entry;
			while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
				String entryName = entry.getName().replace('\\', '/');
				if (entryName.startsWith("./")) {
					entryName = entryName.substring(2);
				}
				if (entryName.isEmpty()) continue;

				File outputFile = new File(outputDir, entryName);

				if (entry.isDirectory()) {
					if (!outputFile.exists() && !outputFile.mkdirs()) {
						throw new IOException("Failed to create directory " + outputFile);
					}
				} else {
					File parentDir = outputFile.getParentFile();
					if (!parentDir.exists() && !parentDir.mkdirs()) {
						throw new IOException("Failed to create directory " + parentDir);
					}

					// Extraire le contenu de l'entrée dans un fichier temporaire pour comparaison
					File tempServerFile = File.createTempFile("server_entry_", ".tmp");
					try (OutputStream os = new FileOutputStream(tempServerFile)) {
						byte[] buffer = new byte[BUFFER_SIZE];
						int bytesRead;
						while ((bytesRead = tarInputStream.read(buffer)) != -1) {
							os.write(buffer, 0, bytesRead);
						}
					}

					long serverMtime = (entry.getLastModifiedDate() != null) ? entry.getLastModifiedDate().getTime() : 0;

					if (outputFile.exists()) {
						long localMtime = outputFile.lastModified();
						boolean identical = FileUtils.contentEquals(outputFile, tempServerFile);

						if (identical) {
							if (serverMtime > 0) {
								outputFile.setLastModified(serverMtime);
							}
						} else if (localMtime > serverMtime + 1000) {
							// Conflit : le fichier local est PLUS RÉCENT que celui du serveur et différent
							// On préserve le code local de l'élève !
							File serverBak = new File(outputFile.getAbsolutePath() + ".server_bak");
							FileUtils.copyFile(tempServerFile, serverBak);
							System.out.println("[RECONCILIATION] Fichier local preservé (plus récent que serveur) : " + outputFile.getName());
						} else {
							// La version du serveur est plus récente ou égale : on met à jour
							FileUtils.copyFile(tempServerFile, outputFile);
							if (serverMtime > 0) {
								outputFile.setLastModified(serverMtime);
							}
						}
					} else {
						// Le fichier n'existe pas localement : on l'installe
						FileUtils.copyFile(tempServerFile, outputFile);
						if (serverMtime > 0) {
							outputFile.setLastModified(serverMtime);
						}
					}
					tempServerFile.delete();
				}
			}
		}
	}
}

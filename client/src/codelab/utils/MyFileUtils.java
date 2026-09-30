package codelab.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.regex.Matcher;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.FileUtils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import codelab.ExceptionManager;

/**
*	Classe utilitaire pour fichiers
*	@author Jérôme Lehuen
*	@version 03/10/25
*/

public class MyFileUtils {

	public static boolean isEqual(File file1, File file2) {
		try {
			return FileUtils.contentEquals(file1, file2);
		}
		catch (IOException e) {
			return false;
		}
	}

	/**
	 * Downloads a file from a URL
	 * @param url URL HTTP du fichier à télécharger
	 * @param dir chemin du répertoire pour enregistrer le fichier
	 * @param progress affichage de la progression dans un terminal
	 * @param monitor affichage de la progression dans un ProgressMonitor
	 * @throws IOException
	 */

	public static boolean download(String fileURL, String dir) {

		final String fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());

		try {
			final URL url = URI.create(fileURL).toURL();
			final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			final int responseCode = httpConn.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				//final String contentType = httpConn.getContentType();
				final int contentLength = httpConn.getContentLength();
				//System.out.println("Content-Type = " + contentType);
				//System.out.println("Content-Length = " + contentLength);

				// Opens input stream from the HTTP connection
				final InputStream inputStream = httpConn.getInputStream();
				final String saveFilePath = dir + File.separator + fileName;

				// Opens an output stream to save into file
				final FileOutputStream outputStream = new FileOutputStream(saveFilePath);

				int bytesRead = -1;
				int totalBytesRead = 0;
				int percent = 0;
				int previous = 0;
				boolean canceled = false;
				final int BUFFER_SIZE = 4096;
				final byte[] buffer = new byte[BUFFER_SIZE];
				while (!canceled && (bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
					totalBytesRead += bytesRead;
					percent = 100 * totalBytesRead / contentLength;
					if (percent > previous) {
						System.out.print(String.format("\rDownloading %s... [%s%%]", fileName, percent));
						previous = percent;
					}
				}
				outputStream.close();
				inputStream.close();
				httpConn.disconnect();
				System.out.println(String.format("\rDownloading %s... DONE", fileName));
				return true;
			}
			else {
				System.out.println(String.format("\rDownloading %s... ERROR %d", fileName, responseCode));
				httpConn.disconnect();
				return false;
			}
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return false;
		}
	}

	/**
	 * Zip a folder
	 * @param filename
	 * @param folder
	 */

	public static boolean zipFolder(String filename, File folder) {
		try {
			ZipFile zip = new ZipFile(filename);
			zip.addFolder(folder);
			zip.close();
			return true;
		}
		catch (ZipException e) {
			ExceptionManager.process(e);
			return false;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return false;
		}
	}

	/**
	 * Unzip a zipped file
	 * @param filename name the file to be unzipped
	 * @param dir path of the directory to save the file
	 */

	public static void unzip(String filename, String dir) {
		try {
			System.out.print(String.format("Extracting %s... ", filename));

			ZipFile zip = new ZipFile(dir + File.separator + filename);
			zip.extractAll(dir);
			zip.close();
			System.out.println("OK");
		}
		catch (ZipException e) {
			System.out.println("ERROR");
			ExceptionManager.process(e);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static void unzip(String filename) {
		try {
			System.out.print(String.format("Extracting %s... ", filename));
			File destination = new File(filename).getParentFile();
			ZipFile zip = new ZipFile(filename);
			zip.extractAll(destination.toString());
			zip.close();
			System.out.println("OK");
		}
		catch (ZipException e) {
			System.out.println("ERROR");
			ExceptionManager.process(e);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	/**
	 * Find and replace a string in a file
	 * @param filePath full name the file
	 * @param rexp regular expression to replace
	 * @param str the new string
	 */

	public static boolean findAndReplace(String filePath, String rexp, String str) {
		Path path = Paths.get(filePath);
		Charset charset = StandardCharsets.UTF_8;
		try {
			String content = new String(Files.readAllBytes(path), charset);
			content = content.replaceAll(rexp, Matcher.quoteReplacement(str));
			Files.write(path, content.getBytes(charset));
			return true;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return false;
		}
	}

	public static CharSequence readFile(File file) {
		try {
			FileInputStream input = new FileInputStream(file);
			FileChannel channel = input.getChannel();
			// Create a read-only CharBuffer on the file
			ByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
			CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
			input.close();
			return cbuf;
		}
		catch (IOException e) {
			ExceptionManager.process(e);
			return null;
		}
    }

	public static void writeToFile(CharSequence contents, File file) {
		try (PrintStream ps = new PrintStream(file)) {
			ps.print(contents);
			ps.close();
		} catch (FileNotFoundException e) {
			ExceptionManager.process(e);
		}
	}

	public static void printFile(String filename) {
		try {
			final InputStream flux = new FileInputStream(filename);
			final InputStreamReader lecture = new InputStreamReader(flux);
			final BufferedReader buffer = new BufferedReader(lecture);
			String ligne;
			while ((ligne = buffer.readLine()) != null) {
				System.out.println(ligne);
			}
			buffer.close();
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static void copyFile(File file1, File file2) {
		try {
			Files.copy(file1.toPath(), file2.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (FileNotFoundException e) {
			ExceptionManager.process(e);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static void copyFolder(String sourceDirectoryLocation, String destinationDirectoryLocation) {
		try {
			File sourceDirectory = new File(sourceDirectoryLocation);
			File destinationDirectory = new File(destinationDirectoryLocation);
			FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static boolean copy(String source, String destination) {
		try {
			Files.copy(Paths.get(source), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
			return true;
		}
		catch (FileNotFoundException e) {
			//ExceptionManager.process(e);
			return false;
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static boolean moveToTrashOrDelete(File file) {
		if (file == null || !file.exists()) return false;
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
				if (Desktop.getDesktop().moveToTrash(file)) {
					return true;
				}
			}
		} catch (Exception ignored) {}
		delete(file);
		return true;
	}

	public static void delete(File file) {
		if (file == null) return;
		if (!file.exists()) return;
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
				f.delete();
			}
			file.delete();
		}
		else file.delete();
	}

	public static void delete(String path) {
		delete(new File(path));
	}

	public static void deleteFiles(File dir, String filter) {
		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(filter).get();
		File[] files = dir.listFiles(fileFilter);
		for (File file : files) file.delete();
	}

	public static void deleteFilesRec(File dir, String filter) {
		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(filter).get();
		File[] files = dir.listFiles(fileFilter);
		for (File file : files) file.delete();
		// Appel récursif sur les dossiers
		for (File file : dir.listFiles())
			if (file.isDirectory())
				deleteFilesRec(file, filter);
	}

	public static boolean touch(File file) {
		try {
			file.delete();
			file.createNewFile();
			return true;
		}
		catch (IOException e) {
			//ExceptionManager.process(e);
			return false;
		}
	}

	public static void write(File file, String content) {
		try {
			Charset charset = StandardCharsets.UTF_8;
			Files.write(file.toPath(), content.getBytes(charset));
		}
		catch (IOException e) {
			ExceptionManager.process(e);
		}
	}

	public static void writeln(File file, String content) {
		write(file, content+"\n");
	}

	public static void consolidePath(String path) {
		// Vérifie un chemin et crée les dossiers absents
		String[] etapes = path.split("/");
		String[] folders = Arrays.copyOfRange(etapes, 1, etapes.length-1);
		String pathToBuild = "/";
		for (String folder : folders) {
			pathToBuild += folder + "/";
			File file = new File(pathToBuild);
			if (!file.exists()) file.mkdir();
		}
	}
/*
	public static boolean fileExists(File file) {
		final File parentFile = file.getParentFile();
		//final File[] otherFiles = parentFile.listFiles();
		for (File f : parentFile.listFiles())
			if (f.equals(file)) return true;
		return false;
	}

	public static boolean fileExists(String filename) {
		return fileExists(new File(filename));
	}

	public static boolean folderExists(String filename) {
		//final File file = new File(absolute(filename));
		final File file = new File(filename);
		return file.exists() && file.isDirectory();
	}
*/
	public static String getCurrentFolder() {
		final String folder = Paths.get(".").toAbsolutePath().normalize().toString();
		//System.out.println(folder);
		return folder;
	}

	public static String absolute(String dir) {
		final String path = String.format("%s%s%s", getCurrentFolder(), File.separator, dir);
		//System.out.println(path);
		return path;
	}

	public static File[] getFolderFiles(String folder) {
		//final File file = new File(absolute(folder));
		final File file = new File(folder);
		//System.out.println(file);
		return file.listFiles();
	}

	public static String getFilename(File file) {
		return file.toPath().getFileName().toString();
	}

	public static String getFilename(String path) {
		if (path.indexOf('/') == -1) return "";
		return path.substring(path.lastIndexOf('/') + 1, path.length());
	}

	public static String getExtension(String filename) {
		if (filename.indexOf('.') == -1) return "";
		return filename.substring(filename.lastIndexOf('.'), filename.length());
	}

	public static String getExtension(File file) {
		return getExtension(file.getName());
	}

	public static String noExtension(String filename) {
		if (filename.indexOf('.') == -1) return filename;
		return filename.substring(0, filename.lastIndexOf('.'));
	}

	public static String noExtension(File file) {
		return noExtension(file.toString());
	}

	public static void changeExtension(File file, String ext1, String ext2) {
		//System.out.println(file);
		String path = file.getPath().replace(ext1, ext2);
		file.renameTo(new File(path));
		//System.out.println(file);
	}

	public static File getDirectory(File file) {
		return new File(file.getAbsoluteFile().getParent());
	}

	public static long getSize(File file) {
		try {
			return Files.size(file.toPath());
		}
		catch (IOException e) {
			return -1;
		}
	}

	public static String getInfo(File file) {
		try {
			FileTime fileTime = Files.getLastModifiedTime(file.toPath());
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy - K:mm a");
			return dateFormat.format(fileTime.toMillis());
		}
		catch (IOException e) {
			return "Cannot get the last modified time - " + e;
		}
	}

	public static String normaliseFilename(String filename) {
		// Remplacer les espaces par des underscores
		filename = filename.replace(' ', '_');
		// Supprimer ou remplacer les caractères invalides pour les systèmes de fichiers
		// Supprimer également les apostrophes et autres caractères spéciaux
		// Caractères interdits sur Windows: < > : " / \ | ? *
		filename = filename.replaceAll("[<>:\"/\\\\|?*']", "");
		// Supprimer les caractères de contrôle (ASCII 0-31)
		filename = filename.replaceAll("[\\x00-\\x1F]", "");
		// Supprimer les points et espaces en fin de nom (problématique sur Windows)
		filename = filename.replaceAll("[.\\s]+$", "");
		return filename;
	}
}

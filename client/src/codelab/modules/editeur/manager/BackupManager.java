package codelab.modules.editeur.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import codelab.utils.MyFileUtils;

/**
*	Gestionnaire de backups locaux
*	@author Jérôme Lehuen
*	@version 11/06/25
*/

public class BackupManager {

    // Nom du fichier => hello.c
    // Nom du backup => hello_231228_203000_bak.c.bak
	// Affichage => 28/12/23 - 20:30:00

	private static final SimpleDateFormat dateFormater1 = new SimpleDateFormat("yyMMdd_HHmmss");
	private static final SimpleDateFormat dateFormater2 = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
	private static final String regex = ".*_(\\d\\d)(\\d\\d)(\\d\\d)_(\\d\\d)(\\d\\d)(\\d\\d)_bak\\..*";

	// ----------------------------------------------------------------------------
	// Pour créer une copie de sauvegarde datée d'un fichier (côté client)
	// Retourne le File du backup (ou null)
	// ----------------------------------------------------------------------------

	public synchronized static File backup(File file) {

		if (isBackup(file)) return null; // Déjà une sauvegarde

		if (!necessary(file)) {
			// Pas de modification depuis la dernière sauvegarde
			// CodeLab.INSTANCE.consoleLog("Backup not necessary");
			return null;
		}
		String date = dateFormater1.format(new Date()); // => yyMMdd_HHmmss
		String name = MyFileUtils.noExtension(file); // => ~/codelab.files/.hidden/programs/local/hello
		String ext = MyFileUtils.getExtension(file); // => .c
		String newfilename = String.format("%s_%s_bak%s.bak", name, date, ext); // => hello_231228_203000_bak.c.bak
		File newfile = new File(newfilename);
		try {
			FileUtils.copyFile(file, newfile);
			// CodeLab.INSTANCE.consoleLog("Backup done");
			return newfile;
		}
		catch (IOException e) {
			return null;
		}
	}

	// ----------------------------------------------------------------------------
	// Gestion des dates
	// ----------------------------------------------------------------------------

	private static Date getDate(File file) {
		// Retourne la date d'un backup à partir de son nom
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(file.getName());
		if (!matcher.find()) return null;
		String Y = matcher.group(1); // Année
		String M = matcher.group(2); // Mois
		String D = matcher.group(3); // Date
		String h = matcher.group(4); // Heures
		String m = matcher.group(5); // Minutes
		String s = matcher.group(6); // Secondes
		String str = String.format("%s%s%s_%s%s%s", Y, M, D, h, m, s);
		try {
			return dateFormater1.parse(str);
		}
		catch (ParseException e) {
			return null;
		}
	}

	public static String getDateFormat(File file) {
		Date date = getDate(file);
		if (date == null) return "(date error)";
		return dateFormater2.format(date);
	}

	// ----------------------------------------------------------------------------
	// Autres méthodes privées
	// ----------------------------------------------------------------------------

	private static boolean necessary(File file) {
		// Retourne true si un backup est requis
		File backup = lastBackup(file);
		if (backup == null) return true;
		return areDifferent(file, backup);
	}

	private static boolean isBackup(File file) {
		return file.getName().contains("_bak");
	}

	private static File lastBackup(File file) {
		// Retourne le dernier backup d'un fichier (ou null)
		List<File> files = backupsList(file);
		if (files.size() == 0) return null;
		return files.get(files.size() - 1);
	}

	private static boolean areDifferent(File file1, File file2) {
		// Retourne true si les fichiers sont différents
		long size1 = MyFileUtils.getSize(file1);
		long size2 = MyFileUtils.getSize(file2);
		if (size1 != size2) return true;

		try {
			List<String> lines1 = Files.readAllLines(file1.toPath());
			List<String> lines2 = Files.readAllLines(file2.toPath());
			if (lines1.size() != lines2.size()) return true;

			for (int i = 0; i < lines1.size(); i++) {
				if (!lines1.get(i).equals(lines2.get(i))) return true;
			}
			return false; // Les fichiers sont identiques
		}
		catch (IOException e) {
			return true;
		}
	}

	// ----------------------------------------------------------------------------
	// Pour récupérer la liste des backups d'un fichier
	// ----------------------------------------------------------------------------

	public static List<File> backupsList(File file) {

		List<File> res = new ArrayList<File>();

		String filename = file.getName();
		File directory = MyFileUtils.getDirectory(file);
		String prefix = MyFileUtils.noExtension(filename) + "_";
		String suffix = MyFileUtils.getExtension(filename);

		File[] files = directory.listFiles();
		Arrays.sort(files);
		for (File file1 : files) {
			if (!file1.isFile()) continue; // Pas un fichier
			String name1 = file1.getName();
			String ext1 = MyFileUtils.getExtension(file1);
			String name2 = MyFileUtils.noExtension(file1);
			String ext2 = MyFileUtils.getExtension(name2);

			if (ext1.equals(".bak") && ext2.equals(suffix) && name1.startsWith(prefix)) {
				//CodeLab.INSTANCE.consoleLog("--> " + file1);
				res.add(file1);
			}
		}
		return res;
	}

	// ----------------------------------------------------------------------------
	// Pour renommer tous les backups d'un fichier lors de son renommage
	// Utilisé dans rename_file() de FileManager.java
	// ----------------------------------------------------------------------------

	public static void renameBackups(File originalFile, String newFilename) {
		// Récupérer la liste des backups du fichier original
		List<File> backups = backupsList(originalFile);
		
		if (backups.isEmpty()) return; // Pas de backups à renommer
		
		// Extraire les parties du nouveau nom de fichier
		String newBaseName = MyFileUtils.noExtension(newFilename);
		String newExtension = MyFileUtils.getExtension(newFilename);
		
		// Renommer chaque backup
		for (File backup : backups) {
			try {
				// Extraire la date du backup depuis son nom
				// Format actuel: originalName_YYMMDD_HHMMSS_bak.ext.bak
				String backupName = backup.getName();
				
				// Utiliser la regex existante pour extraire la partie date
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(backupName);
				
				if (matcher.find()) {
					String Y = matcher.group(1); // Année
					String M = matcher.group(2); // Mois
					String D = matcher.group(3); // Date
					String h = matcher.group(4); // Heures
					String m = matcher.group(5); // Minutes
					String s = matcher.group(6); // Secondes
					String dateTimePart = String.format("%s%s%s_%s%s%s", Y, M, D, h, m, s);
					
					// Construire le nouveau nom du backup
					String newBackupName = String.format("%s_%s_bak%s.bak", newBaseName, dateTimePart, newExtension);
					
					// Créer le nouveau fichier de backup
					File newBackupFile = new File(backup.getParent() + File.separator + newBackupName);
					
					// Renommer le backup
					if (backup.renameTo(newBackupFile)) {
						// CodeLab.INSTANCE.consoleLog(String.format("Backup renamed: %s -> %s", backup.getName(), newBackupFile.getName()));
					} else {
						// CodeLab.INSTANCE.consoleLog(String.format("Failed to rename backup: %s", backup.getName()));
					}
				}
			} catch (Exception e) {
				// En cas d'erreur, continuer avec les autres backups
				// CodeLab.INSTANCE.consoleLog(String.format("Error renaming backup %s: %s", 
				//	backup.getName(), e.getMessage()));
			}
		}
	}

	// ----------------------------------------------------------------------------
	// Pour déplacer tous les backups d'un fichier lors de son déplacement
	// Utilisé dans move_file() de FileManager.java
	// ----------------------------------------------------------------------------

	public static void moveBackups(File originalFile, File targetDirectory) {
		// Récupérer la liste des backups du fichier original
		List<File> backups = backupsList(originalFile);
		
		if (backups.isEmpty()) return; // Pas de backups à déplacer
		
		// Déplacer chaque backup
		for (File backup : backups) {
			try {
				// Construire le nouveau chemin du backup
				String backupName = backup.getName();
				File newBackupFile = new File(targetDirectory + File.separator + backupName);
				
				// Déplacer le backup
				if (backup.renameTo(newBackupFile)) {
					// CodeLab.INSTANCE.consoleLog(String.format("Backup moved: %s -> %s", backup.getName(), newBackupFile.getName()));
				} else {
					// CodeLab.INSTANCE.consoleLog(String.format("Failed to move backup: %s", backup.getName()));
				}
			} catch (Exception e) {
				// En cas d'erreur, continuer avec les autres backups
				// CodeLab.INSTANCE.consoleLog(String.format("Error moving backup %s: %s", 
				//	backup.getName(), e.getMessage()));
			}
		}
	}
}

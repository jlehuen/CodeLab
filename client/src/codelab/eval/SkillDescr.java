package codelab.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * Représente un skill d'évaluation pédagogique disponible pour CodeLab.
 * Extrait automatiquement les métadonnées (ID, titre, description) depuis SKILL.md.
 *
 * @author Jérôme Lehuen
 * @version 09/10/26
 */
public class SkillDescr {

	private final String id;
	private final String title;
	private final String description;
	private final File directory;

	public SkillDescr(String id, String title, String description, File directory) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.directory = directory;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public File getDirectory() {
		return directory;
	}

	@Override
	public String toString() {
		return title;
	}

	/**
	 * Analyse un répertoire de compétence pour extraire ses métadonnées.
	 */
	public static SkillDescr fromDirectory(File dir) {
		if (dir == null || !dir.isDirectory()) return null;
		File skillMd = new File(dir, "SKILL.md");
		String id = dir.getName();
		String title = null;
		String desc = null;

		if (skillMd.exists()) {
			try (BufferedReader reader = new BufferedReader(new FileReader(skillMd, StandardCharsets.UTF_8))) {
				String line;
				boolean inFrontmatter = false;
				int dashes = 0;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.equals("---")) {
						dashes++;
						if (dashes == 1) inFrontmatter = true;
						else if (dashes == 2) inFrontmatter = false;
						continue;
					}
					if (inFrontmatter) {
						if (line.startsWith("name:")) {
							String val = line.substring(5).trim();
							if (!val.isEmpty()) id = val;
						} else if (line.startsWith("description:")) {
							String val = line.substring(12).trim();
							if (val.startsWith(">-") || val.startsWith(">") || val.startsWith("|")) {
								desc = "";
							} else if (!val.isEmpty()) {
								desc = val;
							}
						} else if (desc != null && desc.isEmpty() && !line.contains(":")) {
							desc = line.trim();
						}
					} else {
						if (line.startsWith("# ") && title == null) {
							title = line.substring(2).trim();
							if (title.toLowerCase().startsWith("skill d'évaluation :")) {
								title = title.substring("skill d'évaluation :".length()).trim();
							} else if (title.toLowerCase().startsWith("skill d'évaluation")) {
								title = title.substring("skill d'évaluation".length()).trim();
							}
						}
					}
				}
			} catch (Exception ignored) {}
		}

		if (title == null || title.isBlank()) {
			title = formatDefaultTitle(id);
		}
		if (desc == null || desc.isBlank()) {
			desc = "Skill d'évaluation : " + id;
		}

		return new SkillDescr(id, title, desc, dir);
	}

	private static String formatDefaultTitle(String id) {
		String s = id;
		if (s.startsWith("eval-") || s.startsWith("eval_")) s = s.substring(5);
		if (s.isEmpty()) return id;
		return s.substring(0, 1).toUpperCase() + s.substring(1).replace('-', ' ').replace('_', ' ');
	}
}

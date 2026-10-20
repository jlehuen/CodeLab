package codelab.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utilitaires pour la manipulation et la conversion bidirectionnelle
 * des carnets Jupyter (.ipynb) vers le format Percent Python (# %%) et vice-versa.
 * @author Jérôme Lehuen + Antigravity
 */
public class JupyterUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Pattern CELL_DELIMITER_PATTERN =
		Pattern.compile("^#\\s*%%\\s*(\\[([a-zA-Z0-9_-]+)\\])?.*$");

	public static boolean isIpynb(String filename) {
		return filename != null && filename.toLowerCase().endsWith(".ipynb");
	}

	public static boolean isIpynb(File file) {
		return file != null && isIpynb(file.getName());
	}

	/**
	 * Convertit un carnet Jupyter (.ipynb) en texte Python au format Percent (# %%).
	 */
	public static String ipynbToPercent(File ipynbFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists() || ipynbFile.length() == 0) {
			return defaultPercentTemplate();
		}

		String rawContent;
		try (BufferedReader reader = new BufferedReader(new FileReader(ipynbFile, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			rawContent = sb.toString().trim();
		}

		// Si le fichier ne commence pas par '{', ce n'est pas du JSON (peut-être déjà du texte percent)
		if (!rawContent.startsWith("{")) {
			return rawContent.isEmpty() ? defaultPercentTemplate() : rawContent;
		}

		JsonNode root;
		try {
			root = MAPPER.readTree(rawContent);
		} catch (Exception e) {
			// En cas de JSON malformé, repli sur le texte brut
			return rawContent;
		}

		if (root == null || !root.isObject()) {
			return defaultPercentTemplate();
		}

		JsonNode cellsNode = root.get("cells");
		if (cellsNode == null || !cellsNode.isArray() || cellsNode.size() == 0) {
			return defaultPercentTemplate();
		}

		StringBuilder out = new StringBuilder();
		boolean firstCell = true;

		for (JsonNode cell : cellsNode) {
			String cellType = cell.path("cell_type").asText("code").toLowerCase();
			List<String> lines = extractSourceLines(cell.get("source"));

			if (!firstCell) {
				out.append("\n");
			}
			firstCell = false;

			if ("markdown".equals(cellType)) {
				out.append("# %% [markdown]\n");
				for (String line : lines) {
					line = stripTrailingNewline(line);
					if (line.trim().isEmpty()) {
						out.append("#\n");
					} else {
						out.append("# ").append(line).append("\n");
					}
				}
			} else if ("raw".equals(cellType)) {
				out.append("# %% [raw]\n");
				for (String line : lines) {
					line = stripTrailingNewline(line);
					if (line.trim().isEmpty()) {
						out.append("#\n");
					} else {
						out.append("# ").append(line).append("\n");
					}
				}
			} else {
				// Code cell
				out.append("# %%\n");
				for (String line : lines) {
					line = stripTrailingNewline(line);
					out.append(line).append("\n");
				}
			}
		}

		return out.toString();
	}

	/**
	 * Convertit un texte Python au format Percent (# %%) en carnet Jupyter JSON (.ipynb)
	 * et l'enregistre de manière atomique dans targetFile.
	 */
	public static void percentToIpynb(String percentText, File targetFile) throws IOException {
		if (percentText == null) percentText = "";
		File parentDir = targetFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}

		// Conserver les métadonnées existantes du notebook si disponibles
		JsonNode existingMetadata = null;
		int nbformat = 4;
		int nbformatMinor = 5;

		if (targetFile.exists() && targetFile.length() > 0) {
			try {
				JsonNode existingRoot = MAPPER.readTree(targetFile);
				if (existingRoot != null && existingRoot.isObject()) {
					existingMetadata = existingRoot.get("metadata");
					if (existingRoot.has("nbformat")) {
						nbformat = existingRoot.get("nbformat").asInt(4);
					}
					if (existingRoot.has("nbformat_minor")) {
						nbformatMinor = existingRoot.get("nbformat_minor").asInt(5);
					}
				}
			} catch (Exception ignored) {
				// Pas de métadonnées valides récupérables
			}
		}

		// Découpage du texte percent en cellules
		List<CellEntry> cells = parsePercentCells(percentText);

		// Construction de l'arborescence JSON v4
		ObjectNode root = MAPPER.createObjectNode();
		ArrayNode cellsArray = root.putArray("cells");

		for (CellEntry entry : cells) {
			ObjectNode cellNode = MAPPER.createObjectNode();
			cellNode.put("cell_type", entry.type);
			cellNode.putObject("metadata");

			if ("code".equals(entry.type)) {
				cellNode.putNull("execution_count");
				cellNode.putArray("outputs");
			}

			ArrayNode sourceArray = cellNode.putArray("source");
			int count = entry.lines.size();
			for (int i = 0; i < count; i++) {
				String l = entry.lines.get(i);
				if (i < count - 1) {
					sourceArray.add(l + "\n");
				} else {
					sourceArray.add(l);
				}
			}
			cellsArray.add(cellNode);
		}

		if (existingMetadata != null && existingMetadata.isObject()) {
			root.set("metadata", existingMetadata);
		} else {
			ObjectNode meta = root.putObject("metadata");
			ObjectNode lang = meta.putObject("language_info");
			lang.put("name", "python");
			lang.put("file_extension", ".py");
		}

		root.put("nbformat", nbformat);
		root.put("nbformat_minor", nbformatMinor);

		// Écriture atomique
		File tempFile = new File(targetFile.getAbsolutePath() + ".tmp");
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(" ", DefaultIndenter.SYS_LF);
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);

		try {
			try (FileOutputStream fos = new FileOutputStream(tempFile);
				 Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
				MAPPER.writer(printer).writeValue(out, root);
			}
			try {
				Files.move(tempFile.toPath(), targetFile.toPath(),
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile.toPath(), targetFile.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			if (tempFile.exists()) tempFile.delete();
		}
	}

	/**
	 * Extrait le code Python exécutable d'un fichier .ipynb vers un script .py temporaire,
	 * en commentant les commandes magiques IPython (%matplotlib, !pip, etc.)
	 * afin d'éviter les erreurs de syntaxe Python standard.
	 */
	public static void extractRunnablePython(File ipynbFile, File targetPyFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists()) {
			return;
		}

		List<String> runnableCode = new ArrayList<>();
		String rawContent;
		try (BufferedReader reader = new BufferedReader(new FileReader(ipynbFile, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			rawContent = sb.toString().trim();
		}

		if (rawContent.startsWith("{")) {
			try {
				JsonNode root = MAPPER.readTree(rawContent);
				if (root != null && root.has("cells")) {
					for (JsonNode cell : root.get("cells")) {
						String type = cell.path("cell_type").asText("code").toLowerCase();
						if ("code".equals(type)) {
							List<String> lines = extractSourceLines(cell.get("source"));
							for (String l : lines) {
								runnableCode.add(sanitizePythonLine(l));
							}
							runnableCode.add(""); // Ligne vide séparatrice entre cellules
						}
					}
				}
			} catch (Exception e) {
				// Repli sur le parsing percent
				runnableCode = extractRunnableFromPercent(rawContent);
			}
		} else {
			runnableCode = extractRunnableFromPercent(rawContent);
		}

		File parentDir = targetPyFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}

		try (FileOutputStream fos = new FileOutputStream(targetPyFile);
			 Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
			writer.write("# -*- coding: utf-8 -*-\n");
			writer.write("## Generated by CodeLab from " + ipynbFile.getName() + "\n\n");
			for (String l : runnableCode) {
				writer.write(l);
				writer.write("\n");
			}
		}
	}

	private static List<String> extractRunnableFromPercent(String percentText) {
		List<String> res = new ArrayList<>();
		List<CellEntry> cells = parsePercentCells(percentText);
		for (CellEntry c : cells) {
			if ("code".equals(c.type)) {
				for (String l : c.lines) {
					res.add(sanitizePythonLine(l));
				}
				res.add("");
			}
		}
		return res;
	}

	public static String cleanCellCode(String cellCode) {
		if (cellCode == null) return "";
		String[] lines = cellCode.split("\r?\n", -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			sb.append(sanitizePythonLine(lines[i]));
			if (i < lines.length - 1) sb.append("\n");
		}
		return sb.toString();
	}

	public static String sanitizePythonLine(String line) {
		String trimmed = line.trim();
		if (trimmed.startsWith("%") || trimmed.startsWith("!")) {
			return "# [Jupyter magic] " + line;
		}
		return line;
	}

	private static List<CellEntry> parsePercentCells(String text) {
		String[] rawLines = text.split("\r?\n", -1);
		List<CellEntry> cells = new ArrayList<>();

		String currentType = "code";
		List<String> currentLines = new ArrayList<>();
		boolean headerSeen = false;

		for (String line : rawLines) {
			Matcher m = CELL_DELIMITER_PATTERN.matcher(line.trim());
			if (m.matches()) {
				if (headerSeen || !currentLines.isEmpty()) {
					cells.add(new CellEntry(currentType, cleanCellLines(currentType, currentLines)));
					currentLines.clear();
				}
				headerSeen = true;
				String tag = m.group(2);
				if (tag != null && tag.equalsIgnoreCase("markdown")) {
					currentType = "markdown";
				} else if (tag != null && tag.equalsIgnoreCase("raw")) {
					currentType = "raw";
				} else {
					currentType = "code";
				}
			} else {
				currentLines.add(line);
			}
		}

		if (headerSeen || !currentLines.isEmpty() || cells.isEmpty()) {
			cells.add(new CellEntry(currentType, cleanCellLines(currentType, currentLines)));
		}

		return cells;
	}

	private static List<String> cleanCellLines(String type, List<String> lines) {
		List<String> result = new ArrayList<>();
		if ("markdown".equals(type) || "raw".equals(type)) {
			for (String l : lines) {
				if (l.startsWith("# ")) {
					result.add(l.substring(2));
				} else if (l.equals("#")) {
					result.add("");
				} else if (l.startsWith("#")) {
					result.add(l.substring(1));
				} else {
					result.add(l);
				}
			}
		} else {
			result.addAll(lines);
		}

		// Supprimer les lignes vides en queue de cellule
		while (!result.isEmpty() && result.get(result.size() - 1).trim().isEmpty()) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	public static String extractSourceText(JsonNode sourceNode) {
		List<String> lines = extractSourceLines(sourceNode);
		return String.join("\n", lines);
	}

	public static List<String> extractSourceLines(JsonNode sourceNode) {
		List<String> lines = new ArrayList<>();
		if (sourceNode == null) return lines;

		if (sourceNode.isArray()) {
			for (JsonNode item : sourceNode) {
				String text = item.asText();
				String[] split = text.split("\r?\n", -1);
				for (int i = 0; i < split.length; i++) {
					// Si split se termine par vide à cause d'un saut de ligne terminal
					if (i == split.length - 1 && split[i].isEmpty()) continue;
					lines.add(split[i]);
				}
			}
		} else if (sourceNode.isTextual()) {
			String text = sourceNode.asText();
			String[] split = text.split("\r?\n", -1);
			for (int i = 0; i < split.length; i++) {
				if (i == split.length - 1 && split[i].isEmpty()) continue;
				lines.add(split[i]);
			}
		}
		return lines;
	}

	private static String stripTrailingNewline(String line) {
		if (line.endsWith("\r\n")) return line.substring(0, line.length() - 2);
		if (line.endsWith("\n") || line.endsWith("\r")) return line.substring(0, line.length() - 1);
		return line;
	}

	private static String defaultPercentTemplate() {
		return "# %% [markdown]\n" +
			   "# # _fileName_\n" +
			   "# Created by _author_ on _date_\n" +
			   "\n" +
			   "# %%\n" +
			   "print(\"Hello from Jupyter Notebook\")\n";
	}

	private static class CellEntry {
		final String type;
		final List<String> lines;

		CellEntry(String type, List<String> lines) {
			this.type = type;
			this.lines = lines;
		}
	}

	///////////////////////////////////////////////////
	// Support du Carnet Progressif en Langage C
	///////////////////////////////////////////////////

	public static boolean isCNotebook(File ipynbFile) {
		if (ipynbFile == null || !ipynbFile.exists()) return false;
		try {
			JsonNode root = MAPPER.readTree(ipynbFile);
			if (root == null || !root.isObject()) return false;
			String klang = root.path("metadata").path("kernelspec").path("language").asText("");
			String llang = root.path("metadata").path("language_info").path("name").asText("");
			if ("c".equalsIgnoreCase(klang) || "c".equalsIgnoreCase(llang)) {
				return true;
			}
			int cCount = 0;
			int pyCount = 0;
			JsonNode cells = root.get("cells");
			if (cells != null && cells.isArray()) {
				for (JsonNode c : cells) {
					if ("code".equals(c.path("cell_type").asText(""))) {
						String lang = c.path("metadata").path("language").asText("");
						if ("c".equalsIgnoreCase(lang)) cCount++;
						else pyCount++;
					}
				}
			}
			return cCount > 0 && cCount >= pyCount;
		} catch (Exception ignored) {
			return false;
		}
	}

	public static void extractRunnableC(File ipynbFile, File targetCFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists()) return;
		JsonNode root = MAPPER.readTree(ipynbFile);
		if (root == null || !root.has("cells")) return;

		List<String> cCellSources = new ArrayList<>();
		JsonNode cells = root.get("cells");
		if (cells != null && cells.isArray()) {
			for (JsonNode c : cells) {
				String type = c.path("cell_type").asText("code").toLowerCase();
				String lang = c.path("metadata").path("language").asText("c");
				if ("code".equals(type) && "c".equalsIgnoreCase(lang)) {
					cCellSources.add(extractSourceText(c.get("source")));
				}
			}
		}

		String cSource = generateProgressiveCFromSources(cCellSources, cCellSources.size() - 1);
		MyFileUtils.write(targetCFile, cSource);
	}

	public static String generateProgressiveC(List<codelab.modules.editeur.notebook.NotebookCellPane> cellPanes, int targetIndex) {
		List<String> cCellSources = new ArrayList<>();
		int targetCIndex = -1;

		for (int i = 0; i <= targetIndex && i < cellPanes.size(); i++) {
			codelab.modules.editeur.notebook.NotebookCellPane cp = cellPanes.get(i);
			if ("code".equals(cp.getType()) && "c".equalsIgnoreCase(cp.getLanguage())) {
				cCellSources.add(cp.getSource());
				if (i == targetIndex) {
					targetCIndex = cCellSources.size() - 1;
				}
			}
		}

		if (targetCIndex < 0) {
			targetCIndex = cCellSources.size() - 1;
		}

		return generateProgressiveCFromSources(cCellSources, targetCIndex);
	}

	public static String generateProgressiveCFromSources(List<String> cCellSources, int targetIndex) {
		StringBuilder includes = new StringBuilder();
		StringBuilder declarations = new StringBuilder();
		StringBuilder targetStatements = new StringBuilder();
		boolean hasTargetMain = false;

		includes.append("/* CodeLab - Carnet progressif C */\n");
		includes.append("#include <stdio.h>\n");
		includes.append("#include <stdlib.h>\n");
		includes.append("#include <string.h>\n");
		includes.append("#include <math.h>\n");

		for (int i = 0; i <= targetIndex && i < cCellSources.size(); i++) {
			String raw = cCellSources.get(i);
			if (raw == null || raw.trim().isEmpty()) continue;

			boolean isTarget = (i == targetIndex);
			declarations.append(String.format("\n/* --- Cellule C %d --- */\n", i + 1));

			String[] lines = raw.split("\r?\n");
			StringBuilder cellCode = new StringBuilder();

			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.startsWith("#include")) {
					if (!includes.toString().contains(trimmed)) {
						includes.append(line).append("\n");
					}
				} else {
					cellCode.append(line).append("\n");
				}
			}

			String codeWithoutIncludes = cellCode.toString().trim();
			if (codeWithoutIncludes.isEmpty()) continue;

			if (!isTarget) {
				// Renommer les fonctions main() des cellules antérieures
				String sanitized = codeWithoutIncludes.replaceAll("\\b(int|void)\\s+main\\s*\\(", "$1 __main_cell_" + (i + 1) + "(");
				declarations.append(sanitized).append("\n");
			} else {
				// Cellule cible
				Matcher mainMatcher = Pattern.compile("\\b(int|void)\\s+main\\s*\\(").matcher(codeWithoutIncludes);
				if (mainMatcher.find()) {
					hasTargetMain = true;
					declarations.append(codeWithoutIncludes).append("\n");
				} else {
					parseTargetCCell(codeWithoutIncludes, i + 1, declarations, targetStatements);
				}
			}
		}

		StringBuilder full = new StringBuilder();
		full.append(includes).append("\n");
		full.append(declarations).append("\n");

		if (!hasTargetMain) {
			full.append("int main(int argc, char *argv[]) {\n");
			full.append("    setvbuf(stdout, NULL, _IONBF, 0);\n");
			if (targetStatements.length() > 0) {
				full.append(targetStatements);
			} else {
				full.append(String.format("    printf(\"[Cellule %d : compilée avec succès]\\n\");\n", targetIndex + 1));
			}
			full.append("\n    return 0;\n");
			full.append("}\n");
		}

		return full.toString();
	}

	private static void parseTargetCCell(String code, int cellNum, StringBuilder declarations, StringBuilder targetStatements) {
		String[] lines = code.split("\r?\n");
		int braceDepth = 0;
		boolean inBlock = false;
		StringBuilder currentBlock = new StringBuilder();

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				if (inBlock) currentBlock.append(line).append("\n");
				else targetStatements.append(line).append("\n");
				continue;
			}

			if (braceDepth == 0) {
				if (trimmed.startsWith("#define") || trimmed.startsWith("#undef") ||
					trimmed.startsWith("#pragma") || trimmed.startsWith("typedef ") ||
					trimmed.startsWith("struct ") || trimmed.startsWith("enum ") ||
					trimmed.startsWith("union ")) {
					declarations.append(line).append("\n");
					continue;
				}

				// Début de fonction
				boolean isFunc = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_*\\s]+\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^;]*\\)\\s*(\\{)?$").matcher(trimmed).matches();
				if (isFunc) {
					inBlock = true;
					currentBlock.setLength(0);
					currentBlock.append(line).append("\n");
					braceDepth += countChar(line, '{') - countChar(line, '}');
					if (braceDepth <= 0 && line.contains("}")) {
						declarations.append(currentBlock);
						inBlock = false;
						braceDepth = 0;
					}
					continue;
				}
			}

			if (inBlock) {
				currentBlock.append(line).append("\n");
				braceDepth += countChar(line, '{') - countChar(line, '}');
				if (braceDepth <= 0) {
					declarations.append(currentBlock);
					inBlock = false;
					braceDepth = 0;
				}
			} else {
				targetStatements.append("    ").append(line).append("\n");
			}
		}

		if (inBlock && currentBlock.length() > 0) {
			declarations.append(currentBlock);
		}
	}

	private static int countChar(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) count++;
		}
		return count;
	}
}

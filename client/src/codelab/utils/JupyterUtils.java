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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
		List<GhciSourceLocation> sourceMap = new ArrayList<>();
		sourceMap.add(null); // # -*- coding: utf-8 -*-
		sourceMap.add(null); // ## Generated by CodeLab...
		sourceMap.add(null); // blank line

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
					int cellIdx = 0;
					int codeCellNum = 0;
					for (JsonNode cell : root.get("cells")) {
						String type = cell.path("cell_type").asText("code").toLowerCase();
						if ("code".equals(type)) {
							codeCellNum++;
							List<String> lines = extractSourceLines(cell.get("source"));
							for (int k = 0; k < lines.size(); k++) {
								runnableCode.add(sanitizePythonLine(lines.get(k)));
								sourceMap.add(new GhciSourceLocation(cellIdx, codeCellNum, k + 1));
							}
							runnableCode.add(""); // Ligne vide séparatrice entre cellules
							sourceMap.add(null);
						}
						cellIdx++;
					}
				}
			} catch (Exception e) {
				// Repli sur le parsing percent
				runnableCode = extractRunnableFromPercent(rawContent);
				sourceMap.clear();
			}
		} else {
			runnableCode = extractRunnableFromPercent(rawContent);
			sourceMap.clear();
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
		if (!sourceMap.isEmpty()) {
			GHCI_SOURCE_MAPS.put(ghciMapKey(targetPyFile), Collections.unmodifiableList(sourceMap));
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

	public static String detectNotebookLanguage(File ipynbFile) {
		if (ipynbFile == null || !ipynbFile.exists()) return "python";
		try {
			JsonNode root = MAPPER.readTree(ipynbFile);
			if (root == null || !root.isObject()) return "python";
			String klang = root.path("metadata").path("kernelspec").path("language").asText("");
			String llang = root.path("metadata").path("language_info").path("name").asText("");
			if ("c".equalsIgnoreCase(klang) || "c".equalsIgnoreCase(llang)) {
				return "c";
			}
			if ("haskell".equalsIgnoreCase(klang) || "haskell".equalsIgnoreCase(llang)) {
				return "haskell";
			}
			int cCount = 0;
			int hsCount = 0;
			int pyCount = 0;
			JsonNode cells = root.get("cells");
			if (cells != null && cells.isArray()) {
				for (JsonNode c : cells) {
					if ("code".equals(c.path("cell_type").asText(""))) {
						String lang = c.path("metadata").path("language").asText("");
						if ("c".equalsIgnoreCase(lang)) cCount++;
						else if ("haskell".equalsIgnoreCase(lang)) hsCount++;
						else pyCount++;
					}
				}
			}
			if (hsCount > 0 && hsCount >= cCount && hsCount >= pyCount) return "haskell";
			if (cCount > 0 && cCount >= pyCount) return "c";
			return "python";
		} catch (Exception ignored) {
			return "python";
		}
	}

	public static boolean isCNotebook(File ipynbFile) {
		return "c".equals(detectNotebookLanguage(ipynbFile));
	}

	public static boolean isHaskellNotebook(File ipynbFile) {
		return "haskell".equals(detectNotebookLanguage(ipynbFile));
	}

	public static void extractRunnableC(File ipynbFile, File targetCFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists()) return;
		JsonNode root = MAPPER.readTree(ipynbFile);
		if (root == null || !root.has("cells")) return;

		List<GhciCellInput> cCells = new ArrayList<>();
		JsonNode cells = root.get("cells");
		if (cells != null && cells.isArray()) {
			int cellIdx = 0;
			int codeCellNum = 0;
			for (JsonNode c : cells) {
				String type = c.path("cell_type").asText("code").toLowerCase();
				String lang = c.path("metadata").path("language").asText("c");
				if ("code".equals(type)) {
					codeCellNum++;
					if ("c".equalsIgnoreCase(lang)) {
						cCells.add(new GhciCellInput(cellIdx, codeCellNum, extractSourceText(c.get("source"))));
					}
				}
				cellIdx++;
			}
		}

		List<GhciSourceLocation> sourceMap = new ArrayList<>();
		String cSource = generateProgressiveCInternal(cCells, cCells.size() - 1, sourceMap);
		GHCI_SOURCE_MAPS.put(ghciMapKey(targetCFile), Collections.unmodifiableList(sourceMap));
		MyFileUtils.write(targetCFile, cSource);
	}

	public static String generateProgressiveC(List<codelab.modules.editeur.notebook.NotebookCellPane> cellPanes, int targetIndex) {
		return generateProgressiveC(cellPanes, targetIndex, null);
	}

	public static String generateProgressiveC(List<codelab.modules.editeur.notebook.NotebookCellPane> cellPanes, int targetIndex, File targetCFile) {
		List<GhciCellInput> cCells = new ArrayList<>();
		int targetCIndex = -1;
		int codeCellNum = 0;

		for (int i = 0; i <= targetIndex && i < cellPanes.size(); i++) {
			codelab.modules.editeur.notebook.NotebookCellPane cp = cellPanes.get(i);
			if ("code".equals(cp.getType())) {
				codeCellNum++;
				if ("c".equalsIgnoreCase(cp.getLanguage())) {
					cCells.add(new GhciCellInput(i, codeCellNum, cp.getSource()));
					if (i == targetIndex) {
						targetCIndex = cCells.size() - 1;
					}
				}
			}
		}

		if (targetCIndex < 0) {
			targetCIndex = cCells.size() - 1;
		}

		List<GhciSourceLocation> sourceMap = new ArrayList<>();
		String src = generateProgressiveCInternal(cCells, targetCIndex, sourceMap);
		if (targetCFile != null) {
			GHCI_SOURCE_MAPS.put(ghciMapKey(targetCFile), Collections.unmodifiableList(sourceMap));
		}
		return src;
	}

	public static String generateProgressiveCFromSources(List<String> cCellSources, int targetIndex) {
		List<GhciCellInput> cCells = new ArrayList<>();
		for (int i = 0; i < cCellSources.size(); i++) {
			cCells.add(new GhciCellInput(i, i + 1, cCellSources.get(i)));
		}
		return generateProgressiveCInternal(cCells, targetIndex, new ArrayList<>());
	}

	private static String generateProgressiveCInternal(List<GhciCellInput> cCells, int targetIndex, List<GhciSourceLocation> outSourceMap) {
		List<String> includeLines = new ArrayList<>();
		List<GhciSourceLocation> includeMap = new ArrayList<>();
		List<String> declLines = new ArrayList<>();
		List<GhciSourceLocation> declMap = new ArrayList<>();
		List<String> stmtLines = new ArrayList<>();
		List<GhciSourceLocation> stmtMap = new ArrayList<>();
		boolean hasTargetMain = false;

		includeLines.add("/* CodeLab - Carnet progressif C */"); includeMap.add(null);
		includeLines.add("#include <stdio.h>"); includeMap.add(null);
		includeLines.add("#include <stdlib.h>"); includeMap.add(null);
		includeLines.add("#include <string.h>"); includeMap.add(null);
		includeLines.add("#include <math.h>"); includeMap.add(null);

		for (int i = 0; i <= targetIndex && i < cCells.size(); i++) {
			GhciCellInput cell = cCells.get(i);
			String raw = cell.source;
			if (raw == null || raw.trim().isEmpty()) continue;

			boolean isTarget = (i == targetIndex);
			declLines.add(""); declMap.add(null);
			declLines.add(String.format("/* --- Cellule C %d --- */", i + 1)); declMap.add(null);

			String[] lines = raw.split("\r?\n", -1);
			List<NumberedLine> bodyLines = new ArrayList<>();

			for (int k = 0; k < lines.length; k++) {
				String line = lines[k];
				String trimmed = line.trim();
				if (trimmed.startsWith("#include")) {
					if (!includeLines.contains(trimmed) && !includeLines.contains(line)) {
						includeLines.add(line);
						includeMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, k + 1));
					}
				} else {
					bodyLines.add(new NumberedLine(k + 1, line));
				}
			}

			StringBuilder joinedBody = new StringBuilder();
			for (NumberedLine nl : bodyLines) joinedBody.append(nl.text).append("\n");
			String codeWithoutIncludes = joinedBody.toString().trim();
			if (codeWithoutIncludes.isEmpty()) continue;

			if (!isTarget) {
				for (NumberedLine nl : bodyLines) {
					String sanitized = nl.text.replaceAll("\\b(int|void)\\s+main\\s*\\(", "$1 __main_cell_" + (i + 1) + "(");
					declLines.add(sanitized);
					declMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber));
				}
			} else {
				Matcher mainMatcher = Pattern.compile("\\b(int|void)\\s+main\\s*\\(").matcher(codeWithoutIncludes);
				if (mainMatcher.find()) {
					hasTargetMain = true;
					for (NumberedLine nl : bodyLines) {
						declLines.add(nl.text);
						declMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber));
					}
				} else {
					parseTargetCCellNumbered(bodyLines, cell, declLines, declMap, stmtLines, stmtMap);
				}
			}
		}

		StringBuilder full = new StringBuilder();
		for (int i = 0; i < includeLines.size(); i++) {
			full.append(includeLines.get(i)).append("\n");
			outSourceMap.add(includeMap.get(i));
		}
		full.append("\n"); outSourceMap.add(null);

		for (int i = 0; i < declLines.size(); i++) {
			full.append(declLines.get(i)).append("\n");
			outSourceMap.add(declMap.get(i));
		}
		full.append("\n"); outSourceMap.add(null);

		if (!hasTargetMain) {
			full.append("int main(int argc, char *argv[]) {\n"); outSourceMap.add(null);
			full.append("    setvbuf(stdout, NULL, _IONBF, 0);\n"); outSourceMap.add(null);
			if (!stmtLines.isEmpty()) {
				for (int i = 0; i < stmtLines.size(); i++) {
					full.append(stmtLines.get(i)).append("\n");
					outSourceMap.add(stmtMap.get(i));
				}
			} else {
				full.append(String.format("    printf(\"[Cellule %d : compilée avec succès]\\n\");\n", targetIndex + 1));
				outSourceMap.add(null);
			}
			full.append("\n"); outSourceMap.add(null);
			full.append("    return 0;\n"); outSourceMap.add(null);
			full.append("}\n"); outSourceMap.add(null);
		}

		return full.toString();
	}

	private static void parseTargetCCellNumbered(
			List<NumberedLine> bodyLines, GhciCellInput cell,
			List<String> declLines, List<GhciSourceLocation> declMap,
			List<String> stmtLines, List<GhciSourceLocation> stmtMap) {
		int braceDepth = 0;
		boolean inBlock = false;
		List<NumberedLine> currentBlock = new ArrayList<>();

		for (NumberedLine nl : bodyLines) {
			String line = nl.text;
			String trimmed = line.trim();
			GhciSourceLocation loc = new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber);
			if (trimmed.isEmpty()) {
				if (inBlock) currentBlock.add(nl);
				else { stmtLines.add(line); stmtMap.add(loc); }
				continue;
			}

			if (braceDepth == 0) {
				if (trimmed.startsWith("#define") || trimmed.startsWith("#undef") ||
					trimmed.startsWith("#pragma") || trimmed.startsWith("typedef ") ||
					trimmed.startsWith("struct ") || trimmed.startsWith("enum ") ||
					trimmed.startsWith("union ")) {
					declLines.add(line);
					declMap.add(loc);
					continue;
				}

				boolean isFunc = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_*\\s]+\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^;]*\\)\\s*(\\{)?$").matcher(trimmed).matches();
				if (isFunc) {
					inBlock = true;
					currentBlock.clear();
					currentBlock.add(nl);
					braceDepth += countChar(line, '{') - countChar(line, '}');
					if (braceDepth <= 0 && line.contains("}")) {
						for (NumberedLine b : currentBlock) {
							declLines.add(b.text);
							declMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, b.lineNumber));
						}
						inBlock = false;
						braceDepth = 0;
					}
					continue;
				}
			}

			if (inBlock) {
				currentBlock.add(nl);
				braceDepth += countChar(line, '{') - countChar(line, '}');
				if (braceDepth <= 0) {
					for (NumberedLine b : currentBlock) {
						declLines.add(b.text);
						declMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, b.lineNumber));
					}
					inBlock = false;
					braceDepth = 0;
				}
			} else {
				stmtLines.add("    " + line);
				stmtMap.add(loc);
			}
		}

		if (inBlock && !currentBlock.isEmpty()) {
			for (NumberedLine b : currentBlock) {
				declLines.add(b.text);
				declMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, b.lineNumber));
			}
		}
	}

	private static int countChar(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) count++;
		}
		return count;
	}

	///////////////////////////////////////////////////
	// Support du Carnet Interactif en Langage Haskell (GHCi)
	///////////////////////////////////////////////////

	public static class GhciSourceLocation {
		public final int cellIndex;      // Index 0-based dans la liste des cellules du carnet
		public final int codeCellNumber; // Numéro 1-based de cellule de code (affichage utilisateur)
		public final int lineInCell;     // Numéro de ligne 1-based dans la cellule

		public GhciSourceLocation(int cellIndex, int codeCellNumber, int lineInCell) {
			this.cellIndex = cellIndex;
			this.codeCellNumber = codeCellNumber;
			this.lineInCell = lineInCell;
		}
	}

	private static class NumberedLine {
		final int lineNumber; // 1-based
		final String text;

		NumberedLine(int lineNumber, String text) {
			this.lineNumber = lineNumber;
			this.text = text;
		}
	}

	private static class GhciCellInput {
		final int cellIndex;
		final int codeCellNumber;
		final String source;

		GhciCellInput(int cellIndex, int codeCellNumber, String source) {
			this.cellIndex = cellIndex;
			this.codeCellNumber = codeCellNumber;
			this.source = source;
		}
	}

	private static final Map<String, List<GhciSourceLocation>> GHCI_SOURCE_MAPS = new ConcurrentHashMap<>();
	private static final Map<String, List<String>> GHCI_SCRIPT_LINES = new ConcurrentHashMap<>();
	private static final Pattern GHCI_ERROR_LINE_PATTERN =
		Pattern.compile("([^:\\r\\n]+\\.ghci):(\\d+):(\\d+)(?:-(\\d+))?:");
	private static final Pattern GHCI_INTERACTIVE_ERROR_PATTERN =
		Pattern.compile("^<interactive>:(\\d+):(\\d+)(?:-(\\d+))?:");
	private static final Pattern GHCI_NOT_IN_SCOPE_PATTERN =
		Pattern.compile("(?:Variable not in scope:\\s*([a-zA-Z0-9_']+)|Not in scope:[^‘'`]*[‘'`]([^’'`]+)[’'`])");
	private static final Pattern PYTHON_ERROR_LINE_PATTERN =
		Pattern.compile("File\\s+\"([^\"]+)\",\\s+line\\s+(\\d+)");
	private static final Pattern C_ERROR_LINE_PATTERN =
		Pattern.compile("(?:__temp\\.c|[^:\\r\\n]+\\.c):(\\d+):(\\d+):\\s*(?:fatal\\s+)?error:");
	private static final Pattern C_DIAG_LINE_PATTERN =
		Pattern.compile("(?:[^:\\r\\n]*__temp\\.c|[^:\\r\\n]+\\.c):(\\d+):(\\d+):");

	public static String rewriteNotebookErrorLine(File scriptFile, String line, int[] activeLineState) {
		if (scriptFile == null || line == null) return line;
		String name = scriptFile.getName().toLowerCase();
		if (name.endsWith(".ghci")) {
			return rewriteGhciErrorLine(scriptFile, line, activeLineState);
		}
		if (name.endsWith(".py")) {
			return rewritePythonErrorLine(scriptFile, line);
		}
		if (name.endsWith(".c")) {
			return rewriteCErrorLine(scriptFile, line, activeLineState);
		}
		return line;
	}

	public static String rewritePythonErrorLine(File pyFile, String line) {
		if (pyFile == null || line == null) return line;
		String expectedName = pyFile.getName();
		Matcher m = PYTHON_ERROR_LINE_PATTERN.matcher(line);
		if (m.find()) {
			String fileRef = m.group(1);
			if (fileRef.endsWith(expectedName) || expectedName.equals(new File(fileRef).getName())) {
				int pyLine = Integer.parseInt(m.group(2));
				GhciSourceLocation loc = lookupGhciLocation(pyFile, pyLine);
				if (loc != null) {
					String replacement = String.format("[Cellule %d, ligne %d]", loc.codeCellNumber, loc.lineInCell);
					return m.replaceFirst(Matcher.quoteReplacement(replacement));
				}
			}
		}
		return line;
	}

	public static String rewriteCErrorLine(File cFile, String line, int[] activeCAndCellLine) {
		if (cFile == null || line == null) return line;
		Matcher m = C_DIAG_LINE_PATTERN.matcher(line);
		if (m.find()) {
			int cLine = Integer.parseInt(m.group(1));
			String col = m.group(2);
			GhciSourceLocation loc = lookupGhciLocation(cFile, cLine);
			if (loc != null) {
				if (activeCAndCellLine != null && activeCAndCellLine.length >= 2) {
					activeCAndCellLine[0] = cLine;
					activeCAndCellLine[1] = loc.lineInCell;
				}
				String replacement = String.format("[Cellule %d, ligne %d, col %s]:", loc.codeCellNumber, loc.lineInCell, col);
				return m.replaceFirst(Matcher.quoteReplacement(replacement));
			}
		}
		if (activeCAndCellLine != null && activeCAndCellLine.length >= 2 && activeCAndCellLine[0] > 0) {
			String expectedPrefix = String.valueOf(activeCAndCellLine[0]);
			Matcher gm = Pattern.compile("^(\\s*)" + Pattern.quote(expectedPrefix) + "(\\s*\\|.*)$").matcher(line);
			if (gm.matches()) {
				String paddedCellLine = String.format("%" + expectedPrefix.length() + "d", activeCAndCellLine[1]);
				return gm.group(1) + paddedCellLine + gm.group(2);
			}
		}
		return line;
	}

	public static void registerSourceMap(File scriptFile, List<GhciSourceLocation> map) {
		if (scriptFile != null && map != null) {
			GHCI_SOURCE_MAPS.put(ghciMapKey(scriptFile), Collections.unmodifiableList(new ArrayList<>(map)));
		}
	}

	public static List<GhciSourceLocation> extractNotebookErrorLocations(File scriptFile, File errFile) {
		if (scriptFile == null || errFile == null || !errFile.exists()) return Collections.emptyList();
		String name = scriptFile.getName().toLowerCase();
		if (name.endsWith(".ghci")) {
			return extractGhciErrorLocations(scriptFile, errFile);
		}
		if (name.endsWith(".py")) {
			return extractPythonErrorLocations(scriptFile, errFile);
		}
		if (name.endsWith(".c")) {
			return extractCErrorLocations(scriptFile, errFile);
		}
		return Collections.emptyList();
	}

	public static List<GhciSourceLocation> extractPythonErrorLocations(File pyFile, File errFile) {
		List<GhciSourceLocation> result = new ArrayList<>();
		if (pyFile == null || errFile == null || !errFile.exists()) return result;
		String expectedName = pyFile.getName();
		try (BufferedReader br = new BufferedReader(new FileReader(errFile, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = PYTHON_ERROR_LINE_PATTERN.matcher(line);
				while (m.find()) {
					String fileRef = m.group(1);
					if (fileRef.endsWith(expectedName) || expectedName.equals(new File(fileRef).getName())) {
						int pyLine = Integer.parseInt(m.group(2));
						GhciSourceLocation loc = lookupGhciLocation(pyFile, pyLine);
						addUniqueLocation(result, loc);
					}
				}
			}
		} catch (Exception ignored) {}
		return result;
	}

	public static List<GhciSourceLocation> extractCErrorLocations(File cFile, File errFile) {
		List<GhciSourceLocation> result = new ArrayList<>();
		if (cFile == null || errFile == null || !errFile.exists()) return result;
		try (BufferedReader br = new BufferedReader(new FileReader(errFile, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = C_ERROR_LINE_PATTERN.matcher(line);
				if (m.find()) {
					int cLine = Integer.parseInt(m.group(1));
					GhciSourceLocation loc = lookupGhciLocation(cFile, cLine);
					addUniqueLocation(result, loc);
				}
			}
		} catch (Exception ignored) {}
		return result;
	}

	private static void addUniqueLocation(List<GhciSourceLocation> list, GhciSourceLocation loc) {
		if (loc == null) return;
		for (GhciSourceLocation existing : list) {
			if (existing.cellIndex == loc.cellIndex && existing.lineInCell == loc.lineInCell) {
				return;
			}
		}
		list.add(loc);
	}

	private static String ghciMapKey(File ghciFile) {
		if (ghciFile == null) return "";
		try {
			return ghciFile.getCanonicalPath();
		} catch (IOException e) {
			return ghciFile.getAbsolutePath();
		}
	}

	public static GhciSourceLocation lookupGhciLocation(File ghciFile, int ghciLine1Based) {
		if (ghciFile == null || ghciLine1Based <= 0) return null;
		List<GhciSourceLocation> map = GHCI_SOURCE_MAPS.get(ghciMapKey(ghciFile));
		if (map == null || ghciLine1Based > map.size()) return null;
		return map.get(ghciLine1Based - 1);
	}

	public static String rewriteGhciErrorLine(File ghciFile, String line, int[] activeGhciAndCellLine) {
		if (line == null || ghciFile == null) return line;
		Matcher m = GHCI_ERROR_LINE_PATTERN.matcher(line);
		if (m.find()) {
			int ghciLine = Integer.parseInt(m.group(2));
			String col = m.group(3);
			GhciSourceLocation loc = lookupGhciLocation(ghciFile, ghciLine);
			if (loc != null) {
				if (activeGhciAndCellLine != null && activeGhciAndCellLine.length >= 2) {
					activeGhciAndCellLine[0] = ghciLine;
					activeGhciAndCellLine[1] = loc.lineInCell;
				}
				String replacement = String.format("[Cellule %d, ligne %d, col %s]:", loc.codeCellNumber, loc.lineInCell, col);
				return m.replaceFirst(Matcher.quoteReplacement(replacement));
			}
		}
		if (activeGhciAndCellLine != null && activeGhciAndCellLine.length >= 2 && activeGhciAndCellLine[0] > 0) {
			String expectedPrefix = String.valueOf(activeGhciAndCellLine[0]);
			Matcher gm = Pattern.compile("^(\\s*)" + Pattern.quote(expectedPrefix) + "(\\s*\\|.*)$").matcher(line);
			if (gm.matches()) {
				String paddedCellLine = String.format("%" + expectedPrefix.length() + "d", activeGhciAndCellLine[1]);
				return gm.group(1) + paddedCellLine + gm.group(2);
			}
		}
		return line;
	}

	public static List<GhciSourceLocation> extractGhciErrorLocations(File ghciFile, File errFile) {
		List<GhciSourceLocation> result = new ArrayList<>();
		if (ghciFile == null || errFile == null || !errFile.exists()) return result;
		List<String> errLines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(errFile, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				errLines.add(line);
				Matcher m = GHCI_ERROR_LINE_PATTERN.matcher(line);
				while (m.find()) {
					int ghciLine = Integer.parseInt(m.group(2));
					GhciSourceLocation loc = lookupGhciLocation(ghciFile, ghciLine);
					addUniqueLocation(result, loc);
				}
			}
		} catch (Exception ignored) {}

		// Repli pour les erreurs GHCi sur commandes interactives (:t, :i, :k) rapportées sous <interactive>:1:1:
		for (int i = 0; i < errLines.size(); i++) {
			if (GHCI_INTERACTIVE_ERROR_PATTERN.matcher(errLines.get(i)).find()) {
				String missingSym = null;
				for (int j = i + 1; j < Math.min(errLines.size(), i + 5); j++) {
					Matcher nm = GHCI_NOT_IN_SCOPE_PATTERN.matcher(errLines.get(j));
					if (nm.find()) {
						missingSym = (nm.group(1) != null) ? nm.group(1) : nm.group(2);
						break;
					}
				}
				GhciSourceLocation loc = findGhciInteractiveCommandLocation(ghciFile, missingSym);
				addUniqueLocation(result, loc);
			}
		}
		return result;
	}

	private static GhciSourceLocation findGhciInteractiveCommandLocation(File ghciFile, String symbol) {
		String key = ghciMapKey(ghciFile);
		List<GhciSourceLocation> map = GHCI_SOURCE_MAPS.get(key);
		List<String> lines = GHCI_SCRIPT_LINES.get(key);
		if (map == null || lines == null) return null;

		GhciSourceLocation lastCmdLoc = null;
		int count = Math.min(map.size(), lines.size());
		for (int i = 0; i < count; i++) {
			GhciSourceLocation loc = map.get(i);
			String txt = lines.get(i);
			if (loc == null || txt == null) continue;
			String trimmed = txt.trim();
			if (trimmed.startsWith(":") && !trimmed.startsWith(":set") && !trimmed.equals(":{") && !trimmed.equals(":}")) {
				lastCmdLoc = loc;
				if (symbol != null && !symbol.isEmpty()
					&& Pattern.compile("(?<![a-zA-Z0-9_'])" + Pattern.quote(symbol) + "(?![a-zA-Z0-9_'])").matcher(trimmed).find()) {
					return loc;
				}
			}
		}
		return lastCmdLoc;
	}

	private static final Pattern HS_HEAD_NAME_PATTERN =
		Pattern.compile("^([a-z_][a-zA-Z0-9_']*|\\([^)]+\\))\\b.*");
	private static final Pattern HS_TYPE_SIG_PATTERN =
		Pattern.compile("^([a-z_][a-zA-Z0-9_']*|\\([^)]+\\))\\s*::.*");

	private static boolean isHaskellFuncOrBindLine(String trimmed) {
		if (trimmed.startsWith("if ") || trimmed.startsWith("case ")
			|| trimmed.startsWith("do ") || trimmed.equals("do")
			|| trimmed.startsWith("let ") || trimmed.startsWith("\\")) {
			return false;
		}
		if (!HS_HEAD_NAME_PATTERN.matcher(trimmed).matches()) {
			return false;
		}
		return hasTopLevelEqualsOrGuard(trimmed);
	}

	private static boolean hasTopLevelEqualsOrGuard(String line) {
		int paren = 0;
		int bracket = 0;
		int brace = 0;
		boolean inString = false;
		boolean inChar = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';
			char prev = (i > 0) ? line.charAt(i - 1) : '\0';

			if (inString) {
				if (c == '\\') { i++; continue; }
				if (c == '"') inString = false;
				continue;
			}
			if (inChar) {
				if (c == '\\') { i++; continue; }
				if (c == '\'') inChar = false;
				continue;
			}
			if (c == '-' && next == '-') break; // Commentaire fin de ligne
			if (c == '"') { inString = true; continue; }
			if (c == '\'' && (prev == ' ' || prev == '(' || prev == '[' || prev == ',' || prev == '=' || i == 0)) {
				inChar = true;
				continue;
			}
			if (c == '(') { paren++; continue; }
			if (c == ')') { if (paren > 0) paren--; continue; }
			if (c == '[') { bracket++; continue; }
			if (c == ']') { if (bracket > 0) bracket--; continue; }
			if (c == '{') { brace++; continue; }
			if (c == '}') { if (brace > 0) brace--; continue; }

			if (paren == 0 && bracket == 0 && brace == 0) {
				if (c == '=' && prev != '=' && prev != '/' && prev != '<' && prev != '>' && next != '=' && next != '>') {
					return true;
				}
				if (c == '|' && prev != '|' && next != '|') {
					return true;
				}
			}
		}
		return false;
	}

	public static void extractRunnableGhci(File ipynbFile, File targetGhciFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists()) return;
		JsonNode root = MAPPER.readTree(ipynbFile);
		if (root == null || !root.has("cells")) return;

		List<GhciCellInput> hsCells = new ArrayList<>();
		JsonNode cells = root.get("cells");
		if (cells != null && cells.isArray()) {
			int cellIdx = 0;
			int codeCellNum = 0;
			for (JsonNode c : cells) {
				String type = c.path("cell_type").asText("code").toLowerCase();
				String lang = c.path("metadata").path("language").asText("haskell");
				if ("code".equals(type)) {
					codeCellNum++;
					if ("haskell".equalsIgnoreCase(lang)) {
						hsCells.add(new GhciCellInput(cellIdx, codeCellNum, extractSourceText(c.get("source"))));
					}
				}
				cellIdx++;
			}
		}

		List<GhciSourceLocation> sourceMap = new ArrayList<>();
		String ghciScript = generateGhciScriptInternal(hsCells, hsCells.size() - 1, true, sourceMap);
		String key = ghciMapKey(targetGhciFile);
		GHCI_SOURCE_MAPS.put(key, Collections.unmodifiableList(sourceMap));
		GHCI_SCRIPT_LINES.put(key, Arrays.asList(ghciScript.split("\r?\n", -1)));
		MyFileUtils.writeln(targetGhciFile, ghciScript);
	}

	public static String generateGhciScript(List<codelab.modules.editeur.notebook.NotebookCellPane> cellPanes, int targetIndex, boolean runAll) {
		return generateGhciScript(cellPanes, targetIndex, runAll, null);
	}

	public static String generateGhciScript(List<codelab.modules.editeur.notebook.NotebookCellPane> cellPanes, int targetIndex, boolean runAll, File targetGhciFile) {
		List<GhciCellInput> hsCells = new ArrayList<>();
		int targetHsIndex = -1;
		int codeCellNum = 0;

		int limit = runAll ? cellPanes.size() - 1 : targetIndex;
		for (int i = 0; i <= limit && i < cellPanes.size(); i++) {
			codelab.modules.editeur.notebook.NotebookCellPane cp = cellPanes.get(i);
			if ("code".equals(cp.getType())) {
				codeCellNum++;
				if ("haskell".equalsIgnoreCase(cp.getLanguage())) {
					hsCells.add(new GhciCellInput(i, codeCellNum, cp.getSource()));
					if (i == targetIndex) {
						targetHsIndex = hsCells.size() - 1;
					}
				}
			}
		}

		if (targetHsIndex < 0) {
			targetHsIndex = hsCells.size() - 1;
		}

		List<GhciSourceLocation> sourceMap = new ArrayList<>();
		String script = generateGhciScriptInternal(hsCells, targetHsIndex, runAll, sourceMap);
		if (targetGhciFile != null) {
			String key = ghciMapKey(targetGhciFile);
			GHCI_SOURCE_MAPS.put(key, Collections.unmodifiableList(sourceMap));
			GHCI_SCRIPT_LINES.put(key, Arrays.asList(script.split("\r?\n", -1)));
		}
		return script;
	}

	public static String generateGhciScriptFromSources(List<String> hsCellSources, int targetIndex, boolean runAll) {
		List<GhciCellInput> hsCells = new ArrayList<>();
		for (int i = 0; i < hsCellSources.size(); i++) {
			hsCells.add(new GhciCellInput(i, i + 1, hsCellSources.get(i)));
		}
		return generateGhciScriptInternal(hsCells, targetIndex, runAll, new ArrayList<>());
	}

	private static String generateGhciScriptInternal(List<GhciCellInput> hsCells, int targetIndex, boolean runAll, List<GhciSourceLocation> sourceMap) {
		StringBuilder script = new StringBuilder();
		script.append(":set prompt \"\"\n");
		sourceMap.add(null);
		script.append(":set prompt-cont \"\"\n");
		sourceMap.add(null);

		for (int i = 0; i <= targetIndex && i < hsCells.size(); i++) {
			GhciCellInput cell = hsCells.get(i);
			String raw = cell.source;
			if (raw == null || raw.trim().isEmpty()) continue;

			boolean evaluateExpressions = runAll || (i == targetIndex);
			List<List<NumberedLine>> blocks = splitHaskellBlocksNumbered(raw);

			for (List<NumberedLine> block : blocks) {
				if (block.isEmpty()) continue;
				String firstTrimmed = block.get(0).text.trim();
				if (firstTrimmed.isEmpty()) continue;

				boolean isSetCmd = firstTrimmed.startsWith(":set ") || firstTrimmed.equals(":set");
				boolean isOtherGhciCmd = firstTrimmed.startsWith(":") && !isSetCmd;
				boolean isDecl = isHaskellDeclarationBlockNumbered(block);

				if (!evaluateExpressions && !isSetCmd && !isDecl) {
					// Ignorer les expressions et requêtes (:t, :i) des cellules antérieures
					continue;
				}

				if (isOtherGhciCmd || isSetCmd || firstTrimmed.startsWith("import ")) {
					for (NumberedLine nl : block) {
						script.append(nl.text).append("\n");
						sourceMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber));
					}
				} else if (block.size() > 1) {
					script.append(":{\n");
					sourceMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, block.get(0).lineNumber));
					for (NumberedLine nl : block) {
						script.append(nl.text).append("\n");
						sourceMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber));
					}
					script.append(":}\n");
					sourceMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, block.get(block.size() - 1).lineNumber));
				} else {
					NumberedLine nl = block.get(0);
					script.append(nl.text).append("\n");
					sourceMap.add(new GhciSourceLocation(cell.cellIndex, cell.codeCellNumber, nl.lineNumber));
				}
			}
		}

		return script.toString();
	}

	public static void extractRunnableHaskell(File ipynbFile, File targetHsFile) throws IOException {
		if (ipynbFile == null || !ipynbFile.exists()) return;
		JsonNode root = MAPPER.readTree(ipynbFile);
		if (root == null || !root.has("cells")) return;

		List<String> imports = new ArrayList<>();
		List<String> declarations = new ArrayList<>();
		List<String> expressions = new ArrayList<>();
		boolean hasMain = false;

		JsonNode cells = root.get("cells");
		if (cells != null && cells.isArray()) {
			for (JsonNode c : cells) {
				String type = c.path("cell_type").asText("code").toLowerCase();
				String lang = c.path("metadata").path("language").asText("haskell");
				if (!"code".equals(type) || !"haskell".equalsIgnoreCase(lang)) continue;

				String raw = extractSourceText(c.get("source"));
				if (raw == null || raw.trim().isEmpty()) continue;

				List<List<String>> blocks = splitHaskellBlocks(raw);
				for (List<String> block : blocks) {
					if (block.isEmpty()) continue;
					String firstTrimmed = block.get(0).trim();
					if (firstTrimmed.startsWith("import ")) {
						String imp = String.join("\n", block);
						if (!imports.contains(imp)) imports.add(imp);
					} else if (firstTrimmed.startsWith(":")) {
						declarations.add("-- [GHCi] " + String.join("\n-- ", block));
					} else if (isHaskellDeclarationBlock(block)) {
						String blockText = String.join("\n", block);
						if (firstTrimmed.startsWith("main ") || firstTrimmed.startsWith("main=")) {
							hasMain = true;
						}
						declarations.add(blockText);
					} else {
						expressions.add(String.join("\n", block));
					}
				}
			}
		}

		StringBuilder out = new StringBuilder();
		out.append("-- Generated by CodeLab from ").append(ipynbFile.getName()).append("\n\n");
		for (String imp : imports) {
			out.append(imp).append("\n");
		}
		if (!imports.isEmpty()) out.append("\n");

		for (String decl : declarations) {
			out.append(decl).append("\n\n");
		}

		if (!hasMain && !expressions.isEmpty()) {
			out.append("main :: IO ()\n");
			out.append("main = do\n");
			for (String expr : expressions) {
				String trimmed = expr.trim();
				if (trimmed.startsWith("putStr") || trimmed.startsWith("print ") || trimmed.startsWith("do")) {
					for (String line : expr.split("\r?\n")) {
						out.append("  ").append(line).append("\n");
					}
				} else {
					out.append("  print (").append(trimmed.replace("\n", " ")).append(")\n");
				}
			}
		} else if (hasMain && !expressions.isEmpty()) {
			out.append("-- Expressions interactives du carnet :\n");
			for (String expr : expressions) {
				for (String line : expr.split("\r?\n")) {
					out.append("-- ").append(line).append("\n");
				}
			}
		}

		MyFileUtils.writeln(targetHsFile, out.toString());
	}

	private static List<List<String>> splitHaskellBlocks(String raw) {
		List<List<NumberedLine>> numbered = splitHaskellBlocksNumbered(raw);
		List<List<String>> res = new ArrayList<>(numbered.size());
		for (List<NumberedLine> block : numbered) {
			List<String> lines = new ArrayList<>(block.size());
			for (NumberedLine nl : block) lines.add(nl.text);
			res.add(lines);
		}
		return res;
	}

	private static List<List<NumberedLine>> splitHaskellBlocksNumbered(String raw) {
		List<List<NumberedLine>> blocks = new ArrayList<>();
		String[] lines = raw.split("\r?\n", -1);
		List<NumberedLine> current = null;
		String currentFuncName = null;
		boolean inBlockComment = false;

		for (int idx = 0; idx < lines.length; idx++) {
			String line = lines[idx];
			int lineNum = idx + 1;
			String trimmed = line.trim();

			if (inBlockComment) {
				if (trimmed.contains("-}")) {
					inBlockComment = false;
				}
				continue;
			}
			if (trimmed.startsWith("{-")) {
				if (!trimmed.contains("-}")) {
					inBlockComment = true;
				}
				continue;
			}
			if (trimmed.isEmpty()) {
				if (current != null) {
					blocks.add(current);
					current = null;
					currentFuncName = null;
				}
				continue;
			}
			if (trimmed.startsWith("--")) {
				continue;
			}

			boolean isIndented = Character.isWhitespace(line.charAt(0));
			boolean isContinuationKeyword = trimmed.startsWith("where") || trimmed.startsWith("deriving") || trimmed.startsWith("|");

			if (current != null && (isIndented || isContinuationKeyword)) {
				current.add(new NumberedLine(lineNum, line));
				continue;
			}

			String lineFuncName = extractHaskellHeadName(trimmed);
			if (current != null && currentFuncName != null && currentFuncName.equals(lineFuncName)
				&& isHaskellFuncOrBindLine(trimmed)) {
				current.add(new NumberedLine(lineNum, line));
				continue;
			}

			if (current != null) {
				blocks.add(current);
			}
			current = new ArrayList<>();
			current.add(new NumberedLine(lineNum, line));
			currentFuncName = lineFuncName;
		}

		if (current != null) {
			blocks.add(current);
		}
		return blocks;
	}

	private static boolean isHaskellDeclarationBlockNumbered(List<NumberedLine> block) {
		if (block == null || block.isEmpty()) return false;
		List<String> texts = new ArrayList<>(block.size());
		for (NumberedLine nl : block) texts.add(nl.text);
		return isHaskellDeclarationBlock(texts);
	}

	private static String extractHaskellHeadName(String trimmed) {
		Matcher mSig = HS_TYPE_SIG_PATTERN.matcher(trimmed);
		if (mSig.matches()) return mSig.group(1);
		if (isHaskellFuncOrBindLine(trimmed)) {
			Matcher mHead = HS_HEAD_NAME_PATTERN.matcher(trimmed);
			if (mHead.matches()) return mHead.group(1);
		}
		return null;
	}

	private static boolean isHaskellDeclarationBlock(List<String> block) {
		if (block == null || block.isEmpty()) return false;
		String first = block.get(0).trim();
		if (first.startsWith("import ")) return true;
		if (first.startsWith("data ") || first.startsWith("newtype ") || first.startsWith("type ")
			|| first.startsWith("class ") || first.startsWith("instance ")
			|| first.startsWith("infix ") || first.startsWith("infixl ") || first.startsWith("infixr ")
			|| first.startsWith("default ")) {
			return true;
		}
		if (first.startsWith("let ")) {
			String joined = String.join(" ", block);
			return !Pattern.compile("\\bin\\b").matcher(joined).find();
		}
		if (HS_TYPE_SIG_PATTERN.matcher(first).matches()) return true;
		return isHaskellFuncOrBindLine(first);
	}
}

package codelab.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

/**
 * Moteur de rendu mathématique LaTeX vers images pour l'affichage dans les blocs Markdown.
 * Utilise la bibliothèque pure Java JLaTeXMath et un système de cache transparent.
 * 
 * @author Jérôme Lehuen + Antigravity
 */
public class LatexRenderer {

	private static final Pattern DISPLAY_MATH = Pattern.compile("\\$\\$([\\s\\S]+?)\\$\\$");
	private static final Pattern INLINE_MATH = Pattern.compile("(?<!\\$)\\$(?!\\$)([^\\$\\n\\r]+?)(?<!\\$)\\$(?!\\$)");

	public static class RenderResult {
		public final File file;
		public final int width;
		public final int height;
		public final float baseline;

		public RenderResult(File file, int width, int height, float baseline) {
			this.file = file;
			this.width = width;
			this.height = height;
			this.baseline = baseline;
		}
	}

	private static final Map<String, RenderResult> MEMORY_CACHE = new ConcurrentHashMap<>();
	private static final File CACHE_DIR;

	static {
		File tmp = new File(System.getProperty("java.io.tmpdir"), "codelab_latex_cache");
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		CACHE_DIR = tmp;
	}

	public static void clearCache() {
		MEMORY_CACHE.clear();
	}

	/**
	 * Analyse le texte Markdown et convertit les expressions mathématiques ($...$ et $$...$$)
	 * en balises <img> pointant vers des images PNG générées en mémoire/cache.
	 *
	 * @param markdown Texte markdown source
	 * @param baseFontSize Taille de police de base (ex: 13 ou 14)
	 * @return Texte avec les balises <img> prêtes pour le rendu HTML
	 */
	public static String renderMarkdownMath(String markdown, int baseFontSize) {
		return renderMarkdownMath(markdown, baseFontSize, 2.00f, 3.00f);
	}

	public static String renderMarkdownMath(String markdown, int baseFontSize, float inlineScale, float displayScale) {
		if (markdown == null || !markdown.contains("$")) {
			return markdown;
		}

		int size = baseFontSize > 0 ? baseFontSize : 13;
		float iScale = inlineScale > 0 ? inlineScale : 2.00f;
		float dScale = displayScale > 0 ? displayScale : 3.00f;

		// Utilisation directe des facteurs d'échelle sans plancher rigide bloquant
		int inlineSize = Math.max(8, (int) Math.round(size * iScale));
		int displaySize = Math.max(8, (int) Math.round(size * dScale));

		// 1. Traitement des formules hors-ligne / bloc ($$ ... $$)
		Matcher dm = DISPLAY_MATH.matcher(markdown);
		StringBuffer sb = new StringBuffer();
		while (dm.find()) {
			String latex = dm.group(1).trim();
			RenderResult res = renderFormula(latex, true, displaySize);
			if (res != null) {
				String rep = "<div class=\"math-display\" align=\"center\" style=\"margin: 8px 0;\"><img class=\"math-display\" src=\"" 
					+ res.file.toURI().toString() + "\" width=\"" + res.width + "\" height=\"" + res.height + "\""
					+ " alt=\"" + escapeXml(latex) + "\"></div>";
				dm.appendReplacement(sb, Matcher.quoteReplacement(rep));
			} else {
				dm.appendReplacement(sb, Matcher.quoteReplacement("$$" + latex + "$$"));
			}
		}
		dm.appendTail(sb);
		String step1 = sb.toString();

		// 2. Traitement des formules en ligne ($ ... $)
		Matcher im = INLINE_MATH.matcher(step1);
		StringBuffer sb2 = new StringBuffer();
		while (im.find()) {
			String latex = im.group(1).trim();
			// Ignorer si vide
			if (latex.isEmpty()) {
				im.appendReplacement(sb2, Matcher.quoteReplacement("$$"));
				continue;
			}
			RenderResult res = renderFormula(latex, false, inlineSize);
			if (res != null) {
				String rep = "<img class=\"math-inline\" src=\"" + res.file.toURI().toString() 
					+ "\" width=\"" + res.width + "\" height=\"" + res.height + "\""
					+ " align=\"middle\" data-baseline=\"" + String.format(java.util.Locale.US, "%.3f", res.baseline) + "\""
					+ " alt=\"" + escapeXml(latex) + "\">";
				im.appendReplacement(sb2, Matcher.quoteReplacement(rep));
			} else {
				im.appendReplacement(sb2, Matcher.quoteReplacement("$" + latex + "$"));
			}
		}
		im.appendTail(sb2);
		return sb2.toString();
	}

	/**
	 * Compile une expression LaTeX et renvoie son résultat complet (fichier, dimensions et ligne de base).
	 */
	public static RenderResult renderFormula(String latex, boolean isDisplay, int fontSize) {
		if (latex == null || latex.trim().isEmpty()) return null;

		String cleanLatex = latex.trim();
		String cacheKey = cleanLatex + "_" + (isDisplay ? "D" : "I") + "_" + fontSize;

		RenderResult cached = MEMORY_CACHE.get(cacheKey);
		if (cached != null && cached.file != null && cached.file.exists() && cached.file.length() > 0) {
			return cached;
		}

		try {
			int style = isDisplay ? TeXConstants.STYLE_DISPLAY : TeXConstants.STYLE_TEXT;
			TeXFormula formula = new TeXFormula(cleanLatex);
			TeXIcon icon = formula.createTeXIcon(style, fontSize);
			icon.setInsets(new Insets(1, 1, 1, 1));

			int width = Math.max(1, icon.getIconWidth());
			int height = Math.max(1, icon.getIconHeight());
			float baseline = icon.getBaseLine();

			String md5 = computeMd5(cacheKey);
			File diskFile = new File(CACHE_DIR, "tex_" + md5 + ".png");
			if (!diskFile.exists() || diskFile.length() == 0) {
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = image.createGraphics();
				g2.setColor(new Color(0, 0, 0, 0));
				g2.fillRect(0, 0, width, height);
				icon.paintIcon(null, g2, 0, 0);
				g2.dispose();
				ImageIO.write(image, "png", diskFile);
			}

			RenderResult result = new RenderResult(diskFile, width, height, baseline);
			MEMORY_CACHE.put(cacheKey, result);
			return result;
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Compile une expression LaTeX en fichier PNG transparent et renvoie son descripteur File.
	 */
	public static File renderFormulaToImage(String latex, boolean isDisplay, int fontSize) {
		RenderResult res = renderFormula(latex, isDisplay, fontSize);
		return res != null ? res.file : null;
	}

	private static String computeMd5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			return Integer.toHexString(input.hashCode());
		}
	}

	private static String escapeXml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;")
				   .replace("<", "&lt;")
				   .replace(">", "&gt;")
				   .replace("\"", "&quot;");
	}
}

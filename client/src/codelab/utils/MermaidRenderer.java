package codelab.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.aresstack.Mermaid;

/**
 * Moteur de rendu des diagrammes Mermaid (blocs ```mermaid) vers images PNG
 * pour l'affichage dans les vues Markdown (JEditorPane).
 *
 * Classe autonome : le rendu est assuré en Java par la bibliothèque mermaid-java
 * (com.aresstack:mermaid-java), qui embarque mermaid.js (exécuté par GraalJS) et utilise
 * Apache Batik pour la rastérisation. Aucun Node.js, navigateur ou service distant.
 *
 * Taille des diagrammes : un facteur d'échelle est appliqué à la taille naturelle du diagramme.
 * Il se règle dans la feuille de style CSS par la variable "--mermaid-scale" (voir cssScale).
 * L'image est rastérisée directement à la taille voulue (rendu vectoriel net) plutôt que réduite à l'affichage.
 *
 * Deux modes d'utilisation :
 *  - Asynchrone (affichage à l'écran) : renderMarkdownMermaid(...) renvoie immédiatement un placeholder
 *    pour les diagrammes pas encore rendus, puis le callback de la vue est invoqué sur l'EDT quand
 *    ils sont prêts. Plusieurs vues peuvent attendre le même diagramme ; un même propriétaire (owner)
 *    n'est notifié qu'une fois par diagramme, même s'il redemande le rendu plusieurs fois.
 *  - Bloquant (export) : renderMarkdownMermaidBlocking(...) attend la fin des rendus. À ne JAMAIS
 *    appeler depuis l'EDT.
 *
 * Les rendus sont sérialisés sur un unique thread (le contexte GraalJS de la bibliothèque n'est pas prévu
 * pour des accès concurrents). Les images sont mises en cache (mémoire + disque, par diagramme et par échelle).
 * Si le rendu échoue, le bloc est conservé sous forme de code, précédé d'une note explicative.
 *
 * @author Jérôme Lehuen + Antigravity
 */
public class MermaidRenderer {

	private static final Pattern MERMAID_BLOCK = Pattern.compile("(?m)^[ \\t]*```+[ \\t]*mermaid[ \\t]*\\r?\\n([\\s\\S]*?)^[ \\t]*```+[ \\t]*$");
	private static final Pattern CSS_SCALE = Pattern.compile("--mermaid-scale\\s*:\\s*([0-9]*\\.?[0-9]+)");

	private static final int DEFAULT_MAX_WIDTH = 700;

	/** Échelle par défaut (1.0 = taille naturelle du diagramme) quand le CSS ne définit pas --mermaid-scale. */
	public static final float DEFAULT_SCALE = 0.6f;
	private static final float MIN_SCALE = 0.1f;
	private static final float MAX_SCALE = 3.0f;

	/** Change quand le rendu change (version de la bibliothèque, réglages) pour invalider le cache disque. */
	private static final String CACHE_VERSION = "mermaid-java-2";

	private static final String PENDING_HTML = "\n\n<div class=\"mermaid-pending\" align=\"center\" style=\"color:#8c959f; font-style:italic;\">"
		+ "Rendu du diagramme Mermaid en cours…</div>\n\n";

	private static class Result {
		final File file;
		final int width;
		final int height;

		Result(File file, int width, int height) {
			this.file = file;
			this.width = width;
			this.height = height;
		}
	}

	private static final Map<String, Result> MEMORY_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, String> FAILURES = new ConcurrentHashMap<>();
	private static final File CACHE_DIR;

	/** Protège IN_FLIGHT et WAITERS. */
	private static final Object LOCK = new Object();
	private static final Map<String, Future<?>> IN_FLIGHT = new HashMap<>();
	private static final Map<String, Map<Object, Runnable>> WAITERS = new HashMap<>();

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "MermaidRenderer");
		t.setDaemon(true);
		return t;
	});

	static {
		File tmp = new File(System.getProperty("java.io.tmpdir"), "codelab_mermaid_cache");
		if (!tmp.exists()) {
			tmp.mkdirs();
		}
		CACHE_DIR = tmp;
	}

	public static void clearCache() {
		MEMORY_CACHE.clear();
		FAILURES.clear();
	}

	public static boolean containsMermaid(String markdown) {
		return markdown != null && markdown.contains("mermaid") && MERMAID_BLOCK.matcher(markdown).find();
	}

	/**
	 * Lit l'échelle des diagrammes dans une feuille de style CSS : variable "--mermaid-scale"
	 * (ex. "--mermaid-scale: 0.6;" dans un bloc :root). Les commentaires CSS sont ignorés.
	 * Renvoie DEFAULT_SCALE si la variable est absente ou invalide ; la valeur est bornée à [0.1 ; 3.0].
	 */
	public static float cssScale(String css) {
		if (css == null) return DEFAULT_SCALE;
		String clean = css.replaceAll("/\\*[\\s\\S]*?\\*/", "");
		Matcher m = CSS_SCALE.matcher(clean);
		if (m.find()) {
			try {
				return clampScale(Float.parseFloat(m.group(1)));
			} catch (NumberFormatException ignored) {
			}
		}
		return DEFAULT_SCALE;
	}

	/**
	 * Version asynchrone simple (échelle par défaut) : le callback sert aussi d'identifiant de propriétaire.
	 */
	public static String renderMarkdownMermaid(String markdown, int maxWidth, Runnable onReady) {
		return renderMarkdownMermaid(markdown, maxWidth, DEFAULT_SCALE, onReady, onReady);
	}

	/**
	 * Version asynchrone avec propriétaire (échelle par défaut).
	 */
	public static String renderMarkdownMermaid(String markdown, int maxWidth, Object owner, Runnable onReady) {
		return renderMarkdownMermaid(markdown, maxWidth, DEFAULT_SCALE, owner, onReady);
	}

	/**
	 * Remplace chaque bloc ```mermaid par une balise image (ou un placeholder / repli).
	 *
	 * @param markdown Texte Markdown source
	 * @param maxWidth Largeur maximale d'affichage en pixels (0 ou négatif : valeur par défaut)
	 * @param scale    Facteur d'échelle appliqué à la taille naturelle des diagrammes (voir cssScale)
	 * @param owner    Identifie la vue demandeuse (typiquement "this") : elle n'est notifiée qu'une fois par diagramme
	 * @param onReady  Appelé sur l'EDT quand un rendu asynchrone se termine (typiquement : rafraîchir la vue)
	 */
	public static String renderMarkdownMermaid(String markdown, int maxWidth, float scale, Object owner, Runnable onReady) {
		if (!containsMermaid(markdown)) {
			return markdown;
		}
		int maxW = maxWidth > 0 ? maxWidth : DEFAULT_MAX_WIDTH;
		float sc = clampScale(scale);

		Matcher m = MERMAID_BLOCK.matcher(markdown);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String original = m.group();
			String source = m.group(1).trim();
			String replacement = source.isEmpty() ? original : renderBlock(source, original, maxW, sc, owner, onReady);
			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Version bloquante (échelle par défaut).
	 */
	public static String renderMarkdownMermaidBlocking(String markdown, int maxWidth, long timeoutSeconds) {
		return renderMarkdownMermaidBlocking(markdown, maxWidth, DEFAULT_SCALE, timeoutSeconds);
	}

	/**
	 * Version bloquante : attend que tous les diagrammes soient rendus (ou que le délai soit écoulé)
	 * avant de renvoyer le Markdown transformé. Destinée aux exports (HTML, impression).
	 * Ne doit pas être appelée depuis l'EDT.
	 */
	public static String renderMarkdownMermaidBlocking(String markdown, int maxWidth, float scale, long timeoutSeconds) {
		if (!containsMermaid(markdown)) {
			return markdown;
		}
		float sc = clampScale(scale);

		List<Future<?>> pending = new ArrayList<>();
		Matcher m = MERMAID_BLOCK.matcher(markdown);
		while (m.find()) {
			String source = m.group(1).trim();
			if (source.isEmpty()) continue;
			String key = keyOf(source, sc);
			if (cached(key) != null || FAILURES.containsKey(key)) continue;
			pending.add(requestRender(key, source, sc, null, null));
		}

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(1, timeoutSeconds));
		for (Future<?> f : pending) {
			try {
				f.get(Math.max(1L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
			} catch (Exception ignored) {
				// délai dépassé ou interruption : le bloc restera sous forme de placeholder
			}
		}
		return renderMarkdownMermaid(markdown, maxWidth, sc, null, null);
	}

	private static String renderBlock(String source, String original, int maxW, float scale, Object owner, Runnable onReady) {
		String key = keyOf(source, scale);

		Result res = cached(key);
		if (res != null) {
			return imageHtml(res, maxW);
		}

		String error = FAILURES.get(key);
		if (error != null) {
			return note("Diagramme Mermaid invalide ou rendu impossible : " + escapeXml(error)) + original;
		}

		requestRender(key, source, scale, owner, onReady);
		return PENDING_HTML;
	}

	///////////////////////////////////////////////////
	// Rendu
	///////////////////////////////////////////////////

	/**
	 * Demande le rendu d'un diagramme (une seule tâche par diagramme et par échelle) et enregistre le callback du demandeur.
	 */
	private static Future<?> requestRender(String key, String source, float scale, Object owner, Runnable onReady) {
		synchronized (LOCK) {
			if (onReady != null) {
				WAITERS.computeIfAbsent(key, k -> new HashMap<>()).put(owner != null ? owner : onReady, onReady);
			}
			Future<?> f = IN_FLIGHT.get(key);
			if (f == null) {
				f = EXECUTOR.submit(() -> runJob(key, source, scale));
				IN_FLIGHT.put(key, f);
			}
			return f;
		}
	}

	private static void runJob(String key, String source, float scale) {
		try {
			// Un autre demandeur a pu terminer ce rendu entre-temps
			if (cached(key) == null) {
				renderToDisk(key, source, scale);
			}
		} catch (Throwable t) {
			FAILURES.put(key, describe(t));
		} finally {
			List<Runnable> toNotify;
			synchronized (LOCK) {
				IN_FLIGHT.remove(key);
				Map<Object, Runnable> waiting = WAITERS.remove(key);
				toNotify = waiting != null ? new ArrayList<>(waiting.values()) : new ArrayList<>();
			}
			for (Runnable r : toNotify) {
				SwingUtilities.invokeLater(r);
			}
		}
	}

	private static void renderToDisk(String key, String source, float scale) throws Exception {
		BufferedImage img = renderImage(source, scale);

		File out = new File(CACHE_DIR, "mermaid_" + key + ".png");
		File tmpOut = new File(CACHE_DIR, "mermaid_" + key + ".tmp.png");
		try {
			if (!ImageIO.write(img, "png", tmpOut)) {
				throw new Exception("écriture PNG impossible");
			}
			Files.move(tmpOut.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			tmpOut.delete();
		}
		MEMORY_CACHE.put(key, new Result(out, img.getWidth(), img.getHeight()));
	}

	/**
	 * Produit l'image du diagramme à l'échelle demandée. On mesure d'abord la taille naturelle,
	 * puis on rastérise de nouveau à la largeur cible (rendu vectoriel, traits nets). Si ce second rendu
	 * échoue, on se replie sur un redimensionnement de l'image naturelle.
	 */
	private static BufferedImage renderImage(String source, float scale) throws Exception {
		BufferedImage base = Mermaid.renderToImage(source);
		if (!valid(base)) {
			throw new Exception("rendu vide");
		}
		if (Math.abs(scale - 1f) < 0.01f) {
			return base;
		}

		int targetWidth = Math.max(40, Math.round(base.getWidth() * scale));
		try {
			BufferedImage scaled = Mermaid.renderToImage(source, targetWidth);
			// Garde-fou : le second rendu doit avoir les mêmes proportions que le rendu naturel
			// (sinon marges ou recadrage différents) ; sinon on préfère redimensionner l'image de base.
			if (valid(scaled)) {
				float expected = base.getHeight() / (float) base.getWidth();
				float actual = scaled.getHeight() / (float) scaled.getWidth();
				if (Math.abs(actual - expected) <= 0.05f * expected) {
					return scaled;
				}
			}
		} catch (Throwable ignored) {
			// repli ci-dessous
		}
		return resample(base, scale);
	}

	private static boolean valid(BufferedImage img) {
		return img != null && img.getWidth() > 1 && img.getHeight() > 1;
	}

	/** Redimensionnement de qualité (réductions successives par moitié, puis bicubique). */
	private static BufferedImage resample(BufferedImage src, float scale) {
		int tw = Math.max(1, Math.round(src.getWidth() * scale));
		int th = Math.max(1, Math.round(src.getHeight() * scale));
		BufferedImage cur = src;
		int cw = src.getWidth();
		int ch = src.getHeight();
		while (cw / 2 >= tw && ch / 2 >= th) {
			cw /= 2;
			ch /= 2;
			cur = resize(cur, cw, ch);
		}
		return resize(cur, tw, th);
	}

	private static BufferedImage resize(BufferedImage src, int w, int h) {
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return dst;
	}

	private static String describe(Throwable t) {
		if (t instanceof NoClassDefFoundError || t instanceof ClassNotFoundException) {
			return "bibliothèque mermaid-java (ou une de ses dépendances) absente du classpath : " + t.getMessage();
		}
		Throwable root = t;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String msg = root.getMessage();
		if (msg == null || msg.trim().isEmpty()) {
			msg = root.getClass().getSimpleName();
		}
		msg = msg.trim().replaceAll("\\s+", " ");
		return msg.length() > 300 ? msg.substring(0, 300) + "…" : msg;
	}

	///////////////////////////////////////////////////
	// Utilitaires
	///////////////////////////////////////////////////

	private static float clampScale(float scale) {
		if (Float.isNaN(scale)) return DEFAULT_SCALE;
		return Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
	}

	private static String keyOf(String source, float scale) {
		return computeMd5(CACHE_VERSION + "|" + String.format(Locale.US, "%.2f", scale) + "|" + source);
	}

	/** Résultat en cache (mémoire, puis disque), ou null. */
	private static Result cached(String key) {
		Result res = MEMORY_CACHE.get(key);
		if (res != null && res.file.exists() && res.file.length() > 0) {
			return res;
		}
		File f = new File(CACHE_DIR, "mermaid_" + key + ".png");
		if (!f.exists() || f.length() == 0) return null;
		try {
			BufferedImage img = ImageIO.read(f);
			if (img == null) return null;
			Result r = new Result(f, img.getWidth(), img.getHeight());
			MEMORY_CACHE.put(key, r);
			return r;
		} catch (Exception e) {
			return null;
		}
	}

	private static String imageHtml(Result r, int maxW) {
		int w = r.width;
		int h = r.height;
		if (w > maxW) {
			h = Math.max(1, Math.round(h * (maxW / (float) w)));
			w = maxW;
		}
		return "\n\n<div class=\"mermaid-diagram\" align=\"center\" style=\"margin: 8px 0;\"><img src=\""
			+ r.file.toURI().toString() + "\" width=\"" + w + "\" height=\"" + h + "\" alt=\"Diagramme Mermaid\"></div>\n\n";
	}

	private static String note(String html) {
		return "<div class=\"mermaid-note\" style=\"color:#9a6700; font-style:italic;\">" + html + "</div>\n\n";
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

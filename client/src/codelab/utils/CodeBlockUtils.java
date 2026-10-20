package codelab.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Permet d'appliquer des styles CSS différents selon le langage d'un bloc de code Markdown.
 *
 * Flexmark produit pour un bloc "```python" le HTML suivant :
 *     &lt;pre&gt;&lt;code class="language-python"&gt; ... &lt;/code&gt;&lt;/pre&gt;
 * La classe n'est portée que par la balise code. Or le moteur HTML de Swing (CSS 1 / 2.1) ne sait pas
 * styler un parent à partir de son enfant : on ne pourrait ni colorer le fond du bloc, ni poser un
 * liseré. Cette méthode recopie donc la classe sur la balise pre :
 *     &lt;pre class="language-python"&gt;&lt;code class="language-python"&gt; ... &lt;/code&gt;&lt;/pre&gt;
 * Le CSS peut alors cibler pre.language-python, pre.language-console, pre.language-c, etc.
 *
 * Le nom de classe est celui écrit après les ``` (en minuscules). Les caractères interdits dans un nom de
 * classe CSS sont remplacés : "c++" devient "cpp", "c#" devient "csharp", les autres caractères spéciaux deviennent "-".
 *
 * @author Jérôme Lehuen + Antigravity
 */
public class CodeBlockUtils {

	private static final String MARK = "<pre><code class=\"language-";
	private static final Pattern FENCED = Pattern.compile("<pre><code class=\"language-([^\"]*)\">");

	/**
	 * Ajoute la classe "language-xxx" sur les balises pre des blocs de code clôturés.
	 * Les blocs sans langage et les blocs indentés restent inchangés.
	 */
	public static String tagLanguage(String html) {
		if (html == null || !html.contains(MARK)) {
			return html;
		}
		Matcher m = FENCED.matcher(html);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String lang = cssName(m.group(1));
			String replacement = lang.isEmpty()
				? m.group()
				: "<pre class=\"language-" + lang + "\"><code class=\"language-" + lang + "\">";
			m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/** Nom de langage utilisable dans un sélecteur CSS (minuscules, sans caractère spécial). */
	static String cssName(String info) {
		if (info == null) return "";
		String s = info.trim().toLowerCase(Locale.ROOT);
		s = s.replace("c++", "cpp").replace("c#", "csharp");
		s = s.replaceAll("[^a-z0-9_-]+", "-");
		s = s.replaceAll("^-+|-+$", "");
		return s;
	}
}

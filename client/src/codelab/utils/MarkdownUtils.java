package codelab.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compléments au Markdown standard pour les vues CodeLab.
 *
 * Souligné : en Markdown standard, __texte__ signifie gras. Dans CodeLab il signifie souligné :
 * la méthode underline() le transforme en &lt;u&gt;texte&lt;/u&gt; avant l'analyse par flexmark
 * (le HTML en ligne est conservé, et le moteur HTML de Swing comprend &lt;u&gt;).
 * Le gras s'écrit donc **texte** et l'italique *texte* ou _texte_ (inchangés, gérés par flexmark).
 *
 * Ne sont jamais modifiés : les blocs de code clôturés, le code en ligne, les formules ($...$ et $$...$$),
 * les balises HTML, les destinations de liens "](...)" et les adresses http(s)://.
 * Un souligné ne s'ouvre ni ne se ferme au milieu d'un mot (snake__case__x reste intact), et "\__" échappe l'ouverture.
 * Les séquences de trois soulignés ou plus (___texte___) sont laissées à flexmark.
 *
 * @author Jérôme Lehuen + Antigravity
 */
public class MarkdownUtils {

	private static final char OPEN = '';
	private static final char CLOSE = '';

	/** Zones protégées, dans l'ordre où elles sont repérées. */
	private static final Pattern[] PROTECTED = {
		// blocs de code clôturés (jusqu'à la clôture, ou jusqu'à la fin du texte s'il n'y en a pas)
		Pattern.compile("(?m)^[ \\t]*(`{3,}|~{3,})[^\\n]*\\n[\\s\\S]*?(?:^[ \\t]*\\1[`~]*[ \\t]*$|\\z)"),
		// code en ligne (suite de ` identique à l'ouverture et à la fermeture, sans traverser de ligne vide)
		Pattern.compile("(?<!`)(`+)(?!`)(?:(?!\\n[ \\t]*\\n)[\\s\\S])+?(?<!`)\\1(?!`)"),
		// formules, comme dans LatexRenderer
		Pattern.compile("\\$\\$[\\s\\S]+?\\$\\$"),
		Pattern.compile("(?<!\\$)\\$(?!\\$)[^\\$\\n\\r]+?(?<!\\$)\\$(?!\\$)"),
		// balises HTML et liens automatiques <...>
		Pattern.compile("<[^>\\n]+>"),
		// destinations de liens et d'images : ](...)
		Pattern.compile("\\]\\([^)\\n]*\\)"),
		// adresses
		Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s<>)\\]]+")
	};

	private static final Pattern UNDERLINE = Pattern.compile(
		"(?<![\\p{L}\\p{N}_\\\\])__(?![\\s_])"
		+ "((?:(?!\\n[ \\t]*\\n)(?!__)[\\s\\S])+?)"
		+ "(?<![\\s\\\\])__(?![\\p{L}\\p{N}_])");

	/**
	 * Transforme __texte__ en &lt;u&gt;texte&lt;/u&gt; (voir la description de la classe).
	 */
	public static String underline(String markdown) {
		if (markdown == null || !markdown.contains("__")) {
			return markdown;
		}

		List<String> saved = new ArrayList<>();
		String text = markdown;
		for (Pattern p : PROTECTED) {
			Matcher m = p.matcher(text);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				saved.add(m.group());
				m.appendReplacement(sb, Matcher.quoteReplacement(OPEN + String.valueOf(saved.size() - 1) + CLOSE));
			}
			m.appendTail(sb);
			text = sb.toString();
		}

		text = UNDERLINE.matcher(text).replaceAll("<u>$1</u>");

		// Restauration dans l'ordre inverse : une zone protégée plus tardive peut en contenir une plus ancienne
		for (int i = saved.size() - 1; i >= 0; i--) {
			text = text.replace(OPEN + String.valueOf(i) + CLOSE, saved.get(i));
		}
		return text;
	}
}

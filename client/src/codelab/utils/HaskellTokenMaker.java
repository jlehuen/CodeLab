package codelab.utils;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMap;

/**
 * Colorateur syntaxique dédié au langage Haskell pour RSyntaxTextArea.
 * Gère les mots-clés Haskell, types/constructeurs, identifiants avec apostrophe (x'),
 * littéraux caractères/chaînes, nombres, opérateurs (::, ->, =>, <-), commandes GHCi (:t, :i),
 * commentaires en ligne (--) et commentaires blocs multi-lignes ({- ... -}).
 *
 * @author Jérôme Lehuen + Antigravity
 */
public class HaskellTokenMaker extends AbstractTokenMaker {

	public static final String SYNTAX_STYLE_HASKELL = "text/haskell";
	private static volatile boolean registered = false;

	public static void register() {
		if (registered) return;
		synchronized (HaskellTokenMaker.class) {
			if (registered) return;
			TokenMakerFactory factory = TokenMakerFactory.getDefaultInstance();
			if (factory instanceof AbstractTokenMakerFactory) {
				((AbstractTokenMakerFactory) factory).putMapping(
					SYNTAX_STYLE_HASKELL,
					HaskellTokenMaker.class.getName()
				);
			}
			registered = true;
		}
	}

	@Override
	public TokenMap getWordsToHighlight() {
		TokenMap map = new TokenMap();

		// Mots-clés Haskell 2010 + extensions courantes
		String[] keywords = {
			"as", "case", "class", "data", "default", "deriving", "do",
			"else", "forall", "foreign", "hiding", "if", "import", "in",
			"infix", "infixl", "infixr", "instance", "let", "mdo", "module",
			"newtype", "of", "proc", "qualified", "rec", "then", "type", "where"
		};
		for (String kw : keywords) {
			map.put(kw, Token.RESERVED_WORD);
		}

		// Booléens
		map.put("True", Token.LITERAL_BOOLEAN);
		map.put("False", Token.LITERAL_BOOLEAN);

		// Fonctions usuelles du Prelude
		String[] preludeFuncs = {
			"map", "filter", "foldl", "foldr", "foldl1", "foldr1", "head", "tail",
			"init", "last", "null", "length", "reverse", "concat", "concatMap",
			"zip", "zipWith", "take", "drop", "elem", "notElem", "sum", "product",
			"maximum", "minimum", "even", "odd", "not", "otherwise", "error",
			"undefined", "id", "const", "flip", "show", "read", "print",
			"putStr", "putStrLn", "getLine", "return", "pure", "fmap", "main"
		};
		for (String fn : preludeFuncs) {
			map.put(fn, Token.FUNCTION);
		}

		return map;
	}

	@Override
	public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
		if (tokenType == Token.IDENTIFIER) {
			int value = wordsToHighlight.get(segment, start, end);
			if (value != -1) {
				tokenType = value;
			} else if (start <= end && Character.isUpperCase(segment.array[start])) {
				tokenType = Token.DATA_TYPE;
			}
		}
		super.addToken(segment, start, end, tokenType, startOffset);
	}

	@Override
	public Token getTokenList(Segment text, int startTokenType, int startOffset) {
		resetTokenList();

		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;
		int newStartOffset = startOffset - offset;

		int i = offset;

		// Poursuite d'un commentaire bloc multi-ligne {- ... -}
		if (startTokenType == Token.COMMENT_MULTILINE) {
			int tokStart = i;
			while (i < end) {
				if (array[i] == '-' && i + 1 < end && array[i + 1] == '}') {
					i += 2;
					addToken(text, tokStart, i - 1, Token.COMMENT_MULTILINE, newStartOffset + tokStart);
					startTokenType = Token.NULL;
					break;
				}
				i++;
			}
			if (startTokenType == Token.COMMENT_MULTILINE) {
				if (tokStart < end) {
					addToken(text, tokStart, end - 1, Token.COMMENT_MULTILINE, newStartOffset + tokStart);
				}
				return firstToken;
			}
		}

		boolean lineStart = true;

		while (i < end) {
			char c = array[i];

			// Espaces blancs
			if (c == ' ' || c == '\t') {
				int tokStart = i;
				while (i < end && (array[i] == ' ' || array[i] == '\t')) i++;
				addToken(text, tokStart, i - 1, Token.WHITESPACE, newStartOffset + tokStart);
				continue;
			}

			// Commande GHCi en début de ligne (:t, :i, :set, etc.)
			if (lineStart && c == ':' && i + 1 < end && Character.isLetter(array[i + 1])) {
				int tokStart = i;
				i++;
				while (i < end && (Character.isLetterOrDigit(array[i]) || array[i] == '-')) i++;
				addToken(text, tokStart, i - 1, Token.PREPROCESSOR, newStartOffset + tokStart);
				lineStart = false;
				continue;
			}
			lineStart = false;

			// Commentaire en ligne (-- suivi d'espace, fin de ligne ou autre '-')
			if (c == '-' && i + 1 < end && array[i + 1] == '-') {
				int j = i + 2;
				while (j < end && array[j] == '-') j++;
				if (j >= end || !isSymbolChar(array[j])) {
					addToken(text, i, end - 1, Token.COMMENT_EOL, newStartOffset + i);
					addNullToken();
					return firstToken;
				}
			}

			// Commentaire bloc {- ... -}
			if (c == '{' && i + 1 < end && array[i + 1] == '-') {
				int tokStart = i;
				i += 2;
				boolean closed = false;
				while (i < end) {
					if (array[i] == '-' && i + 1 < end && array[i + 1] == '}') {
						i += 2;
						closed = true;
						break;
					}
					i++;
				}
				addToken(text, tokStart, i - 1, Token.COMMENT_MULTILINE, newStartOffset + tokStart);
				if (!closed) {
					return firstToken; // Propage l'état COMMENT_MULTILINE à la ligne suivante
				}
				continue;
			}

			// Chaîne de caractères "..."
			if (c == '"') {
				int tokStart = i;
				i++;
				boolean closed = false;
				while (i < end) {
					if (array[i] == '\\' && i + 1 < end) {
						i += 2;
						continue;
					}
					if (array[i] == '"') {
						i++;
						closed = true;
						break;
					}
					i++;
				}
				addToken(text, tokStart, i - 1,
					closed ? Token.LITERAL_STRING_DOUBLE_QUOTE : Token.ERROR_STRING_DOUBLE,
					newStartOffset + tokStart);
				continue;
			}

			// Littéral caractère 'a', '\n', '\''
			if (c == '\'') {
				int tokStart = i;
				int j = i + 1;
				if (j < end && array[j] == '\\' && j + 1 < end) {
					j += 2;
					while (j < end && array[j] != '\'' && !Character.isWhitespace(array[j])) j++;
				} else if (j < end && array[j] != '\'') {
					j++;
				}
				if (j < end && array[j] == '\'') {
					i = j + 1;
					addToken(text, tokStart, i - 1, Token.LITERAL_CHAR, newStartOffset + tokStart);
					continue;
				}
				// Apostrophe isolée : traiter comme opérateur/ponctuation sans corrompre la fin de ligne
				addToken(text, i, i, Token.OPERATOR, newStartOffset + i);
				i++;
				continue;
			}

			// Opérateur infixe entre accents graves (`mod`, `elem`)
			if (c == '`') {
				int tokStart = i;
				i++;
				while (i < end && array[i] != '`') i++;
				if (i < end && array[i] == '`') i++;
				addToken(text, tokStart, i - 1, Token.LITERAL_BACKQUOTE, newStartOffset + tokStart);
				continue;
			}

			// Nombres (entiers, flottants, hexadécimaux)
			if (Character.isDigit(c)) {
				int tokStart = i;
				int tokType = Token.LITERAL_NUMBER_DECIMAL_INT;
				if (c == '0' && i + 1 < end && (array[i + 1] == 'x' || array[i + 1] == 'X')) {
					i += 2;
					while (i < end && isHexDigit(array[i])) i++;
					tokType = Token.LITERAL_NUMBER_HEXADECIMAL;
				} else {
					while (i < end && Character.isDigit(array[i])) i++;
					if (i + 1 < end && array[i] == '.' && Character.isDigit(array[i + 1])) {
						tokType = Token.LITERAL_NUMBER_FLOAT;
						i++;
						while (i < end && Character.isDigit(array[i])) i++;
					}
					if (i < end && (array[i] == 'e' || array[i] == 'E')) {
						tokType = Token.LITERAL_NUMBER_FLOAT;
						i++;
						if (i < end && (array[i] == '+' || array[i] == '-')) i++;
						while (i < end && Character.isDigit(array[i])) i++;
					}
				}
				addToken(text, tokStart, i - 1, tokType, newStartOffset + tokStart);
				continue;
			}

			// Identifiants (autorisant les apostrophes comme x', fact', xs'')
			if (Character.isLetter(c) || c == '_') {
				int tokStart = i;
				i++;
				while (i < end && (Character.isLetterOrDigit(array[i]) || array[i] == '_' || array[i] == '\'')) {
					i++;
				}
				addToken(text, tokStart, i - 1, Token.IDENTIFIER, newStartOffset + tokStart);
				continue;
			}

			// Séparateurs
			if (c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' || c == ',' || c == ';') {
				addToken(text, i, i, Token.SEPARATOR, newStartOffset + i);
				i++;
				continue;
			}

			// Opérateurs symboliques (::, ->, =>, <-, .., ++, ==, /=, <=, >=, $, etc.)
			if (isSymbolChar(c)) {
				int tokStart = i;
				i++;
				while (i < end && isSymbolChar(array[i])) {
					if (array[i] == '-' && i + 1 < end && array[i + 1] == '-') break;
					i++;
				}
				addToken(text, tokStart, i - 1, Token.OPERATOR, newStartOffset + tokStart);
				continue;
			}

			// Autre caractère
			addToken(text, i, i, Token.IDENTIFIER, newStartOffset + i);
			i++;
		}

		addNullToken();
		return firstToken;
	}

	private static boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private static boolean isSymbolChar(char c) {
		switch (c) {
			case '!': case '#': case '$': case '%': case '&': case '*':
			case '+': case '.': case '/': case '<': case '=': case '>':
			case '?': case '@': case '\\': case '^': case '|': case '-':
			case '~': case ':':
				return true;
			default:
				return false;
		}
	}
}

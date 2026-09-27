package codelab.modules.editeur.manager;

public enum TypeLang {

	C, GO, JAVA, PROCESS, PYTHON, HASKELL, CLIPS, BLOCS, HEADER, TEXT, HTML, CSS, ERROR;

	public String extension() {
		switch (this) {
			case C:			return ".c";
			case GO:		return ".go";
			case JAVA:		return ".java";
			case PROCESS:	return ".pde";
			case HASKELL:	return ".hs";
			case PYTHON:	return ".py";
			case CLIPS:		return ".clp";
			case BLOCS:		return ".blocs";
			case TEXT:		return ".txt";
			case CSS:		return ".css";
			case HTML:		return ".html";
			case HEADER:	return ".h";
			default:		return "ERROR";
		}
	}

	public static TypeLang identify(String ext) {
		if (ext == null) return ERROR;
		switch (ext) {
			case ".c":		return C;
			case ".go":		return GO;
			case ".java":	return JAVA;
			case ".pde":	return PROCESS;
			case ".hs":		return HASKELL;
			case ".py":		return PYTHON;
			case ".clp":	return CLIPS;
			case ".blocs":	return BLOCS;
			case ".txt":	return TEXT;
			case ".css":	return CSS;
			case ".html":	return HTML;
			case ".h":		return HEADER;
			default:		return ERROR;
		}
	}

	public boolean isCompilable() {
		switch (this) {
			case C:
			case GO:
			case JAVA:
			case HASKELL:
			case PROCESS:
			case BLOCS:		return true;
			default:		return false;
		}
	}

	public boolean isExecutable() {
		switch (this) {
			case CSS:
			case TEXT:
			case HEADER:
			case ERROR:		return false;
			default:		return true;
		}
	}
}

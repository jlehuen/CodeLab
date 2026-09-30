package codelab.finders;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import codelab.CodeLab;

/**
 * Détecteur pour compilateur Haskell (ghc)
 * @author Gemini 3.8
 * 
 */

public class HaskellFinder extends AbstractFinder {

	@Override
	public String getPropertyKey() {
		return "HASKELL_CMD";
	}

	@Override
	public String getTitleLabelKey() {
		return "searchHaskellTitle";
	}

	@Override
	public String getWaitingLabelKey() {
		return "searchHaskellWaiting";
	}

	@Override
	public String getFoundLabelKey() {
		return "searchHaskellFound";
	}

	@Override
	public String getMultipleLabelKey() {
		return "searchHaskellMultiple";
	}

	@Override
	public String getSuccessLabelKey() {
		return "searchHaskellSuccess";
	}

	@Override
	public String getNotFoundLabelKey() {
		return "searchHaskellNotFound";
	}

	@Override
	public String getInvalidLabelKey() {
		return "searchHaskellInvalid";
	}

	@Override
	public List<String> getBinNames() {
		return Arrays.asList("ghc");
	}

	@Override
	public List<String> getVersionArgs() {
		return Arrays.asList("--version");
	}

	@Override
	public boolean isValidVersionOutput(String output) {
		return output.contains("the glorious glasgow haskell")
				|| output.contains("ghc")
				|| output.contains("version");
	}

	@Override
	public String getCurrentConfiguredCommand() {
		return CodeLab.HASKELL_CMD;
	}

	@Override
	public void updateCodeLabCommand(String path) {
		CodeLab.HASKELL_CMD = path;
	}

	@Override
	public void recheckLanguage() {
		CodeLab.check_Haskell();
	}

	@Override
	public String getAdvice() {
		if (CodeLab.IS_WINDOWS) return CodeLab.LABEL("searchHaskellAdviceWin");
		if (CodeLab.IS_OSX) return CodeLab.LABEL("searchHaskellAdviceMac");
		return CodeLab.LABEL("searchHaskellAdviceLinux");
	}

	@Override
	public void scanKnownPaths(Set<String> candidates) {
		String home = System.getenv("HOME");
		String userProfile = System.getenv("USERPROFILE");

		if (CodeLab.IS_WINDOWS) {
			checkAndAddExecutable(candidates, new File("C:/ghcup/bin/ghc.exe"));
			checkAndAddExecutable(candidates, new File("D:/ghcup/bin/ghc.exe"));
			checkAndAddExecutable(candidates, new File("C:/ProgramData/chocolatey/bin/ghc.exe"));

			if (userProfile != null && !userProfile.isBlank()) {
				checkAndAddExecutable(candidates, new File(userProfile, "AppData/Roaming/ghcup/bin/ghc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "AppData/Local/Programs/ghc/bin/ghc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "AppData/Roaming/cabal/bin/ghc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, ".ghcup/bin/ghc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, ".local/bin/ghc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/shims/ghc.exe"));
			}
		} else if (CodeLab.IS_OSX) {
			if (home != null && !home.isBlank()) {
				checkAndAddExecutable(candidates, new File(home, ".ghcup/bin/ghc"));
				checkAndAddExecutable(candidates, new File(home, ".local/bin/ghc"));
				checkAndAddExecutable(candidates, new File(home, ".cabal/bin/ghc"));
			}
			checkAndAddExecutable(candidates, new File("/opt/homebrew/bin/ghc"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/ghc"));
			checkAndAddExecutable(candidates, new File("/usr/bin/ghc"));
		} else if (CodeLab.IS_LINUX) {
			if (home != null && !home.isBlank()) {
				checkAndAddExecutable(candidates, new File(home, ".ghcup/bin/ghc"));
				checkAndAddExecutable(candidates, new File(home, ".local/bin/ghc"));
				checkAndAddExecutable(candidates, new File(home, ".cabal/bin/ghc"));
			}
			checkAndAddExecutable(candidates, new File("/usr/bin/ghc"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/ghc"));
		}
	}
}

package codelab.finders;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import codelab.CodeLab;

/**
 * Détecteur pour compilateur Go
 * @author Gemini 3.8
 * @version 30/09/26
 */

public class GoFinder extends AbstractFinder {

	@Override
	public String getPropertyKey() {
		return "GOLANG_CMD";
	}

	@Override
	public String getTitleLabelKey() {
		return "searchGoTitle";
	}

	@Override
	public String getWaitingLabelKey() {
		return "searchGoWaiting";
	}

	@Override
	public String getFoundLabelKey() {
		return "searchGoFound";
	}

	@Override
	public String getMultipleLabelKey() {
		return "searchGoMultiple";
	}

	@Override
	public String getSuccessLabelKey() {
		return "searchGoSuccess";
	}

	@Override
	public String getNotFoundLabelKey() {
		return "searchGoNotFound";
	}

	@Override
	public String getInvalidLabelKey() {
		return "searchGoInvalid";
	}

	@Override
	public List<String> getBinNames() {
		return Arrays.asList("go");
	}

	@Override
	public List<String> getVersionArgs() {
		return Arrays.asList("version");
	}

	@Override
	public boolean isValidVersionOutput(String output) {
		return output.contains("go version");
	}

	@Override
	public String getCurrentConfiguredCommand() {
		return CodeLab.GOLANG_CMD;
	}

	@Override
	public void updateCodeLabCommand(String path) {
		CodeLab.GOLANG_CMD = path;
	}

	@Override
	public void recheckLanguage() {
		CodeLab.check_GO();
	}

	@Override
	public String getAdvice() {
		if (CodeLab.IS_WINDOWS) return CodeLab.LABEL("searchGoAdviceWin");
		if (CodeLab.IS_OSX) return CodeLab.LABEL("searchGoAdviceMac");
		return CodeLab.LABEL("searchGoAdviceLinux");
	}

	@Override
	public void scanKnownPaths(Set<String> candidates) {
		if (CodeLab.IS_WINDOWS) {
			String[] drives = new String[]{"C:", "D:"};
			for (String drive : drives) {
				checkAndAddExecutable(candidates, new File(drive + "/Program Files/Go/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Program Files (x86)/Go/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Go/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/msys64/mingw64/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/msys64/ucrt64/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/ProgramData/chocolatey/bin/go.exe"));
			}
			String userProfile = System.getenv("USERPROFILE");
			if (userProfile != null && !userProfile.isBlank()) {
				checkAndAddExecutable(candidates, new File(userProfile, "go/bin/go.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/shims/go.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/apps/go/current/bin/go.exe"));
			}
			String localAppData = System.getenv("LOCALAPPDATA");
			if (localAppData != null && !localAppData.isBlank()) {
				checkAndAddExecutable(candidates, new File(localAppData, "Programs/Go/bin/go.exe"));
			}
		} else if (CodeLab.IS_OSX) {
			checkAndAddExecutable(candidates, new File("/usr/local/go/bin/go"));
			checkAndAddExecutable(candidates, new File("/opt/homebrew/bin/go"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/go"));
			checkAndAddExecutable(candidates, new File("/usr/bin/go"));
			String home = System.getenv("HOME");
			if (home != null && !home.isBlank()) {
				checkAndAddExecutable(candidates, new File(home, "go/bin/go"));
			}
		} else if (CodeLab.IS_LINUX) {
			checkAndAddExecutable(candidates, new File("/usr/local/go/bin/go"));
			checkAndAddExecutable(candidates, new File("/usr/bin/go"));
			checkAndAddExecutable(candidates, new File("/snap/bin/go"));
			String home = System.getenv("HOME");
			if (home != null && !home.isBlank()) {
				checkAndAddExecutable(candidates, new File(home, "go/bin/go"));
			}
		}
	}
}

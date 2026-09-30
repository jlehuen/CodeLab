package codelab.finders;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import codelab.CodeLab;

/**
 * Détecteur pour interpréteur Python
 * @author Gemini 3.8
 * @version 30/09/26
 */

public class PythonFinder extends AbstractFinder {

	@Override
	public String getPropertyKey() {
		return "PYTHON_CMD";
	}

	@Override
	public String getTitleLabelKey() {
		return "searchPythonTitle";
	}

	@Override
	public String getWaitingLabelKey() {
		return "searchPythonWaiting";
	}

	@Override
	public String getFoundLabelKey() {
		return "searchPythonFound";
	}

	@Override
	public String getMultipleLabelKey() {
		return "searchPythonMultiple";
	}

	@Override
	public String getSuccessLabelKey() {
		return "searchPythonSuccess";
	}

	@Override
	public String getNotFoundLabelKey() {
		return "searchPythonNotFound";
	}

	@Override
	public String getInvalidLabelKey() {
		return "searchPythonInvalid";
	}

	@Override
	public List<String> getBinNames() {
		if (CodeLab.IS_WINDOWS) {
			return Arrays.asList("python", "python3", "py");
		}
		return Arrays.asList("python3", "python");
	}

	@Override
	public List<String> getVersionArgs() {
		return Arrays.asList("--version");
	}

	@Override
	public boolean isValidVersionOutput(String output) {
		return output.contains("python");
	}

	@Override
	public String getCurrentConfiguredCommand() {
		return CodeLab.PYTHON_CMD;
	}

	@Override
	public void updateCodeLabCommand(String path) {
		CodeLab.PYTHON_CMD = path;
	}

	@Override
	public void recheckLanguage() {
		CodeLab.check_Python();
	}

	@Override
	public String getAdvice() {
		if (CodeLab.IS_WINDOWS) return CodeLab.LABEL("searchPythonAdviceWin");
		if (CodeLab.IS_OSX) return CodeLab.LABEL("searchPythonAdviceMac");
		return CodeLab.LABEL("searchPythonAdviceLinux");
	}

	@Override
	public void scanKnownPaths(Set<String> candidates) {
		if (CodeLab.IS_WINDOWS) {
			// Découverte via Windows Python Launcher "py -0p"
			discoverViaPyLauncher(candidates);

			String localAppData = System.getenv("LOCALAPPDATA");
			if (localAppData != null && !localAppData.isBlank()) {
				for (int v = 14; v >= 8; v--) {
					checkAndAddExecutable(candidates, new File(localAppData, String.format("Programs/Python/Python3%d/python.exe", v)));
					checkAndAddExecutable(candidates, new File(localAppData, String.format("Programs/Python/Python3%d-64/python.exe", v)));
					checkAndAddExecutable(candidates, new File(localAppData, String.format("Programs/Python/Python3%d-32/python.exe", v)));
				}
			}

			String[] drives = new String[]{"C:", "D:"};
			for (String drive : drives) {
				for (int v = 14; v >= 8; v--) {
					checkAndAddExecutable(candidates, new File(drive, String.format("/Python3%d/python.exe", v)));
					checkAndAddExecutable(candidates, new File(drive, String.format("/Program Files/Python3%d/python.exe", v)));
					checkAndAddExecutable(candidates, new File(drive, String.format("/Program Files (x86)/Python3%d/python.exe", v)));
				}
				checkAndAddExecutable(candidates, new File(drive, "/ProgramData/chocolatey/bin/python.exe"));
			}

			String userProfile = System.getenv("USERPROFILE");
			if (userProfile != null && !userProfile.isBlank()) {
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/shims/python.exe"));
			}
		} else if (CodeLab.IS_OSX) {
			checkAndAddExecutable(candidates, new File("/opt/homebrew/bin/python3"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/python3"));
			for (int v = 14; v >= 8; v--) {
				checkAndAddExecutable(candidates, new File(String.format("/Library/Frameworks/Python.framework/Versions/3.%d/bin/python3", v)));
			}
			checkAndAddExecutable(candidates, new File("/usr/bin/python3"));
			checkAndAddExecutable(candidates, new File("/usr/bin/python"));
		} else if (CodeLab.IS_LINUX) {
			checkAndAddExecutable(candidates, new File("/usr/bin/python3"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/python3"));
			checkAndAddExecutable(candidates, new File("/usr/bin/python"));
		}
	}

	private void discoverViaPyLauncher(Set<String> candidates) {
		try {
			ProcessBuilder pb = new ProcessBuilder("py", "-0p");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					// Format type: -V:3.12 * C:\Users\...\AppData\Local\Programs\Python\Python312\python.exe
					if (line.contains(":\\") || line.contains(":/")) {
						int driveIdx = line.indexOf(":");
						if (driveIdx > 0 && Character.isLetter(line.charAt(driveIdx - 1))) {
							String path = line.substring(driveIdx - 1).trim();
							checkAndAddExecutable(candidates, new File(path));
						}
					}
				}
			}
			p.waitFor(2, TimeUnit.SECONDS);
		} catch (Exception ignored) {}
	}
}

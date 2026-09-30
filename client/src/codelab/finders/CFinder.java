package codelab.finders;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import codelab.CodeLab;

/**
 * Détecteur pour compilateur C (gcc / clang)
 * @author Gemini 3.8
 * @version 30/09/26
 */

public class CFinder extends AbstractFinder {

	@Override
	public String getPropertyKey() {
		return "GCC_CMD";
	}

	@Override
	public String getTitleLabelKey() {
		return "searchGccTitle";
	}

	@Override
	public String getWaitingLabelKey() {
		return "searchGccWaiting";
	}

	@Override
	public String getFoundLabelKey() {
		return "searchGccFound";
	}

	@Override
	public String getMultipleLabelKey() {
		return "searchGccMultiple";
	}

	@Override
	public String getSuccessLabelKey() {
		return "searchGccSuccess";
	}

	@Override
	public String getNotFoundLabelKey() {
		return "searchGccNotFound";
	}

	@Override
	public String getInvalidLabelKey() {
		return "searchGccInvalid";
	}

	@Override
	public List<String> getBinNames() {
		return Arrays.asList("gcc", "clang");
	}

	@Override
	public List<String> getVersionArgs() {
		return Arrays.asList("--version");
	}

	@Override
	public boolean isValidVersionOutput(String output) {
		return output.contains("gcc") || output.contains("clang")
				|| output.contains("free software foundation")
				|| output.contains("version")
				|| output.contains("target:");
	}

	@Override
	public String getCurrentConfiguredCommand() {
		return CodeLab.GCC_CMD;
	}

	@Override
	public void updateCodeLabCommand(String path) {
		CodeLab.GCC_CMD = path;
	}

	@Override
	public void recheckLanguage() {
		CodeLab.check_C();
	}

	@Override
	public String getAdvice() {
		if (CodeLab.IS_WINDOWS) return CodeLab.LABEL("searchGccAdviceWin");
		if (CodeLab.IS_OSX) return CodeLab.LABEL("searchGccAdviceMac");
		return CodeLab.LABEL("searchGccAdviceLinux");
	}

	@Override
	public List<ToolInfo> detect() {
		// Priorité absolue aux compilateurs GCC
		List<ToolInfo> gccList = detectSpecific(Arrays.asList("gcc"), true);
		if (!gccList.isEmpty()) {
			return gccList;
		}
		// Repli sur Clang si aucun GCC n'a été trouvé
		return detectSpecific(Arrays.asList("clang"), false);
	}

	private List<ToolInfo> detectSpecific(List<String> bins, boolean isGcc) {
		Set<String> candidatePaths = new LinkedHashSet<>();

		// 1. Commande courante si elle correspond
		String current = getCurrentConfiguredCommand();
		if (current != null && !current.isBlank()
				&& !current.equals("undefined")
				&& !current.equals("DISABLED")) {
			String norm = normalizePath(current);
			if (isGcc && norm.toLowerCase().contains("gcc")) {
				File f = new File(norm);
				if (f.exists()) candidatePaths.add(normalizePath(f.getAbsolutePath()));
				else candidatePaths.add(norm);
			}
		}

		// 2. Commandes système
		for (String bin : bins) {
			if (CodeLab.IS_WINDOWS) runWhereCommand(candidatePaths, bin);
			else runWhichCommand(candidatePaths, bin);
		}

		// 3. Scan PATH
		String pathEnv = System.getenv("PATH");
		if (pathEnv != null && !pathEnv.isBlank()) {
			String[] dirs = pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator));
			for (String dir : dirs) {
				if (dir == null || dir.isBlank()) continue;
				File d = new File(dir.trim());
				if (!d.isDirectory()) continue;
				for (String bin : bins) {
					if (CodeLab.IS_WINDOWS) {
						checkAndAddExecutable(candidatePaths, new File(d, bin + ".exe"));
					} else {
						checkAndAddExecutable(candidatePaths, new File(d, bin));
						if (isGcc) {
							for (int v = 15; v >= 10; v--) {
								checkAndAddExecutable(candidatePaths, new File(d, "gcc-" + v));
							}
						}
					}
				}
			}
		}

		// 4. Chemins connus
		if (isGcc) {
			scanKnownGccPaths(candidatePaths);
		} else {
			scanKnownClangPaths(candidatePaths);
		}

		// 5. Probe
		List<ToolInfo> valid = new ArrayList<>();
		Set<String> testedKeys = new LinkedHashSet<>();
		Set<String> canonicalSeen = new LinkedHashSet<>();

		for (String rawPath : candidatePaths) {
			if (rawPath == null || rawPath.isBlank()) continue;
			String normPath = normalizePath(rawPath);
			String lookupKey = CodeLab.IS_WINDOWS ? normPath.toLowerCase() : normPath;
			if (testedKeys.contains(lookupKey)) continue;
			testedKeys.add(lookupKey);

			File file = new File(normPath);
			if (file.isAbsolute()) {
				try {
					String canon = file.getCanonicalPath();
					if (CodeLab.IS_WINDOWS) canon = canon.toLowerCase();
					if (canonicalSeen.contains(canon)) continue;
					canonicalSeen.add(canon);
				} catch (Exception ignored) {}
			}

			ToolInfo info = probeExecutable(normPath);
			if (info != null) {
				valid.add(info);
			}
		}

		return valid;
	}

	@Override
	public void scanKnownPaths(Set<String> candidates) {
		scanKnownGccPaths(candidates);
		scanKnownClangPaths(candidates);
	}

	private void scanKnownGccPaths(Set<String> candidates) {
		if (CodeLab.IS_WINDOWS) {
			String[] drives = new String[]{"C:", "D:"};
			for (String drive : drives) {
				checkAndAddExecutable(candidates, new File(drive + "/TDM-GCC-64/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/TDM-GCC-32/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/msys64/ucrt64/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/msys64/mingw64/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/msys64/usr/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/MinGW/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/mingw64/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/w64devkit/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/winlibs/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Program Files/CodeBlocks/MinGW/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Program Files (x86)/CodeBlocks/MinGW/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/ProgramData/chocolatey/bin/gcc.exe"));
			}
			String userProfile = System.getenv("USERPROFILE");
			if (userProfile != null && !userProfile.isBlank()) {
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/shims/gcc.exe"));
				checkAndAddExecutable(candidates, new File(userProfile, "scoop/apps/gcc/current/bin/gcc.exe"));
			}
			String localAppData = System.getenv("LOCALAPPDATA");
			if (localAppData != null && !localAppData.isBlank()) {
				checkAndAddExecutable(candidates, new File(localAppData, "Programs/w64devkit/bin/gcc.exe"));
			}
		} else if (CodeLab.IS_OSX) {
			checkAndAddExecutable(candidates, new File("/usr/bin/gcc"));
			checkAndAddExecutable(candidates, new File("/opt/homebrew/bin/gcc"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/gcc"));
			for (int v = 15; v >= 10; v--) {
				checkAndAddExecutable(candidates, new File("/opt/homebrew/bin/gcc-" + v));
				checkAndAddExecutable(candidates, new File("/usr/local/bin/gcc-" + v));
			}
			if (!new File("/usr/bin/gcc").exists()) {
				checkAndAddExecutable(candidates, new File("/Library/Developer/CommandLineTools/usr/bin/gcc"));
				checkAndAddExecutable(candidates, new File("/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/gcc"));
			}
		} else if (CodeLab.IS_LINUX) {
			checkAndAddExecutable(candidates, new File("/usr/bin/gcc"));
			checkAndAddExecutable(candidates, new File("/usr/local/bin/gcc"));
			checkAndAddExecutable(candidates, new File("/bin/gcc"));
			for (int v = 15; v >= 9; v--) {
				checkAndAddExecutable(candidates, new File("/usr/bin/gcc-" + v));
			}
		}
	}

	private void scanKnownClangPaths(Set<String> candidates) {
		if (CodeLab.IS_WINDOWS) {
			String[] drives = new String[]{"C:", "D:"};
			for (String drive : drives) {
				checkAndAddExecutable(candidates, new File(drive + "/msys64/clang64/bin/gcc.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Program Files/LLVM/bin/clang.exe"));
				checkAndAddExecutable(candidates, new File(drive + "/Program Files (x86)/LLVM/bin/clang.exe"));
			}
		} else if (CodeLab.IS_OSX) {
			checkAndAddExecutable(candidates, new File("/usr/bin/clang"));
		} else if (CodeLab.IS_LINUX) {
			checkAndAddExecutable(candidates, new File("/usr/bin/clang"));
		}
	}
}

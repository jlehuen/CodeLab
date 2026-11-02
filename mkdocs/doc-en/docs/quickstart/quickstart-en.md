<style>
	html {
		scroll-behavior: smooth;
	}
	img[src$="center"] {
		display: block;
		margin: 0 auto;
	}
	img[src$="noborder"] {
		border-style: none;
		background-color: none;
	}
	img[src$="center+margin"] {
		display: block;
		margin: 0 auto;
		margin-top: 25px;
		margin-bottom: 25px;
	}
	img[src$="center+noborder"] {
		display: block;
		margin: 0 auto;
		border-style: none;
		background-color: none;
	}
	pre code {
		background: transparent;
	}
	:not(pre) > code {
		background: yellow;
	}
</style>

# ⚡ Quickstart Guide

Welcome to the **CodeLab** express guide. This walkthrough is designed to get you up and running, writing, and executing your first program in **under 2 minutes**, with zero complicated prerequisites.

---

## 1. Download and Launch CodeLab

CodeLab is delivered as a self-contained application bundling its own runtime environment (no manual Java or compiler installation is required to get started).

Download the package tailored for your operating system from the [official Releases page](https://github.com/jlehuen/CodeLab/releases/latest):

| Operating System | Direct Download | First-Time Launch Instructions |
| :--- | :--- | :--- |
| **Windows** | [**`CodeLab-Win64-Setup.exe`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Win64-1.5.2-Setup.exe) | Run the installer (if the blue *Windows SmartScreen* alert appears, click *More info* then *Run anyway*). No admin rights required. |
| **macOS** | [**`CodeLab-MacOS-Silicon.dmg`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-MacOS-Silicon-1.5.2.dmg) (Apple Silicon)<br>[**`CodeLab-MacOS-Intel.dmg`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-MacOS-Intel-1.5.2.dmg) (Intel) | Open the `.dmg`, drag `CodeLab.app` into *Applications*. On first launch, **right-click > Open** to authorize opening. |
| **Linux** | [**`CodeLab-Linux.zip`**](https://github.com/jlehuen/CodeLab/releases/latest/download/CodeLab-Linux-1.5.2.zip) | Extract the archive, navigate into the folder, and double-click `codelab.sh` (or run `./codelab.sh` in a terminal). |

---

## 2. Interface at a Glance

When CodeLab opens, you are greeted with a clean and focused workspace:

![](../img/codelab_1.png?style=center+margin)

1. **Module Tabs (top)**: switch seamlessly between the main code editor and built-in interactive simulators (2D Lego Robotics, 3D Robotics, NXT Motor, 2D Vector Plotter, etc.).
2. **Toolbar (top)**: access vital actions, notably the green **▶️ Run** button (shortcut `F5`) and the red **⏹️ Stop** button.
3. **Editor Area (center)**: write and edit your source code with syntax highlighting, line numbers, and auto-completion.
4. **Execution Console (bottom)**: immediately view output messages (`print`, `printf`, error traces, interactive keyboard input).

---

## 3. Your First Program in 30 Seconds

Pick your language of choice and trigger your first run:

### Option A: Python
1. Click **File > New** and choose **Python** (or open any `.py` script).
2. Enter the following snippet:
```python
print("Welcome to CodeLab!")
for i in range(1, 6):
    print(f"Step {i}: running seamlessly.")
```
3. Click the green **▶️ Run** button (or press **`F5`**).
4. Watch the output print immediately in the bottom console!

### Option B: C Language
1. Click **File > New > C File**.
2. Type this minimal program:
```c
#include <stdio.h>

int main() {
    printf("Welcome to CodeLab!\n");
    for (int i = 1; i <= 5; i++) {
        printf("Step %d: running seamlessly.\n", i);
    }
    return 0;
}
```
3. Click **▶️ Run** (`F5`): CodeLab compiles and runs your code in one seamless step.

### Option C: Block-Based Programming
1. Select **Blocks** mode from the menu or sidebar.
2. Drag and drop a display block from the toolbox into the workspace.
3. Click **▶️ Run**: perfect for grasping algorithms without syntax friction.

---

## 4. Test an Interactive Simulator in 1 Click

CodeLab's standout feature is its suite of real-time simulators. To explore one without writing boilerplate code:

1. Click **File > Templates** (or *Modèles*).
2. Select a ready-to-run sample, for instance under **Mobile Robotics** or **NXT Motor**.
3. The sample code loads into the editor and the simulator panel activates.
4. Click **▶️ Run** (`F5`): observe the mobile robot or motor axle animate in real time according to your instructions!

---

## 5. Essential Keyboard Shortcuts

| Shortcut | Action |
| :--- | :--- |
| **`F5`** | **Run** active program |
| **`Shift + F5`** | **Stop** current execution |
| **`Ctrl + N`** (or `Cmd + N`) | Create a **new file** |
| **`Ctrl + S`** (ou `Cmd + S`) | **Save** active file |
| **`Ctrl + O`** (ou `Cmd + O`) | **Open** an existing file |

---

## Next Steps

- 📖 **API References**: explore detailed APIs for [C Language](../lang-c/doc-c-en.md), [Java](../lang-java/doc-java-en.md), [Python](../lang-python/doc-py-en.md), and [CLIPS](../lang-clips/doc-clips-en.md).
- 🛠️ **Build Your Own Plugins**: follow developer tutorials to [build a first module](../tuto1/tuto1-en.md), [design a 2D plotter](../tuto2/tuto2-en.md), or [develop a closed-loop simulator](../tuto3/tuto3-en.md).

<br/><center>
![](../img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2026 Jérôme Lehuen, Le Mans Université, France
<br/>![](../img/licence.png?style=center+noborder)
</center>

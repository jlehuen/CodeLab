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
		margin-top: 40px;
		margin-bottom: 40px;
	}
	img[src$="center+noborder"] {
		display: block;
		margin: 0 auto;
		border-style: none;
		background-color: none;
	}
	pre { 
		margin-top: 20px;
		margin-bottom: 20px;
		padding: 20px;
		margin: 10px;
	}
	pre code {
		background: transparent;
	}
	:not(pre) > code {
		background: yellow;
	}
	code[class=yellow] {
		background: yellow;
	}
</style>

![](img/logo-codelab.png?style=noborder)

# CodeLab IDE & Simulators
# Conception d'un module de tracé 2D
<br/>

En suivant ce tutoriel, vous allez réaliser le plugin "DrawingModule" qui permet de réaliser des tracés simples en 2D, avec n'importe lequel des langages inclus dans CodeLab. Le rendu ci-dessous est le résultat de l'exécution du programme Python suivant :

```python
from codelab.drawingmodule.canvas import *

clearScreen()
setColor(COLOR_RED)
drawLine(0, 0, 100, 100)
setColor(COLOR_BLUE)
drawCircle(100, 100, 50)
```

<br/>
![](./img/drawing-module.png?style=center+noborder)

La principale différence entre ce module et celui du tutoriel n°1 réside dans la réalisation du panel de rendu graphique. Les autres aspects de la conception sont très similaires et ne seront pas autant détaillés que dans le premier tutoriel. Il est donc nécessaire d'avoir terminé le premier avant de passer à celui-ci.

### Prérequis pour réaliser ce tutoriel :

- Avoir suivi le premier tutoriel
- Maîtriser les concepts objet et la POO en Java
- Connaître les API Java awt et Swing
- Comprendre la syntaxe des fichiers XML

<br/>![](./img/sep.png?style=center+noborder)

## 1. Création du plugin avec le CDK

Comme pour le premier tutoriel, ouvrez un terminal dans le dossier `CDK` et lancez le script de génération :
- Sous Linux / macOS : `./newmodule.sh DrawingModule`
- Sous Windows : `newmodule.bat DrawingModule`

Le script prépare automatiquement l'arborescence complète du projet dans `modules/DrawingModule/`, renomme le package (`codelab.modules.drawingmodule`), la classe principale `DrawingModule.java`, la barre d'outils (`DrawingModuleToolbar.java` que vous pouvez renommer en `DrawingToolbar.java`), le fichier d'API et le manuel Markdown.

Vous disposez ainsi de l'arborescence suivante :

<pre>
DrawingModule/
├── class/
├── data/
│   ├── icon_clear.png
│   └── icon_hello.png
├── includes/
│   ├── api.dtd
│   └── DrawingModuleAPI.xml
├── lib/
│   └── codelab.jar
├── man/
│   └── DrawingModule.md
├── src/
│   └── codelab/
│       └── modules/
│           └── drawingmodule/
│               ├── DrawingModule.java
│               └── DrawingToolbar.java
├── templates/
├── build.bat
└── build.sh
</pre>

La compilation via `./build.sh` (ou `build.bat`) produira le paquet **`DrawingModule.pac`** automatiquement déployé dans le dossier `modules/` de CodeLab.

<br/>![](./img/sep.png?style=center+noborder)

## 2. Classes principales du module

Le modèle objet du `DrawingModule` est bien évidemment similaire à celui du `HelloModule`. Nous utiliserons notamment une `DrawingToolbar` comportant un seul bouton supplémentaire, pour effacer les tracés. Consultez le précédent tutoriel si vous avez des doutes quant-à la justification de ces classes. Les trois classes que vous allez définir sont en rouge dans le diagramme suivant :

![](./img/drawing-uml.png?style=center+noborder)

Commençez par définir la classe `DrawingModule` comme ci-dessous. Les instructions que le gestionnaire d'instructions (handleRequest) devra prendre en compte sont les suivantes :

- `clearScreen()` pour effacer l'écran graphique
- `setColor(color)` pour définir la couleur des tracés à venir
- `drawLine(x1, y1, x2, y2)` pour tracer un segment entre 2 points
- `drawCircle(x, y, r)` pour tracer un cercle de centre (x, y) et de rayon r

Remarquez que toutes ces instructions correspondent aux méthodes publiques de la classe `DrawingPanel` (qui n'est pas encore définie) que vous pouvez voir dans le diagramme de classes.

Ci-dessous le code de la classe `DrawingModule` :

```java
package codelab.modules.drawingmodule;

import java.util.Map;
import codelab.CodeLab;
import codelab.AbstractModule;

public class DrawingModule extends AbstractModule {

	private DrawingPanel canvas;

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public DrawingModule(CodeLab codelab) {
		super(codelab);
		setTitle("Drawing Lab");
		setToolbar(new DrawingToolbar(this));
		setComponent(canvas = new DrawingPanel());
	}

	///////////////////////////////////////////////////
	// Abstract methods to implement (or not)
	///////////////////////////////////////////////////

	public void init() {
	}
	public void exit() {
	}
	public void start() {
	}
	public void stop() {
	}
	public void reset() {
	}

	///////////////////////////////////////////////////
	// Instruction handler
	///////////////////////////////////////////////////

	public String handleRequest(Map<String, String> params) {
		String cmd = params.get("cmd");
		switch (cmd) {

			case "clearScreen":
				return String.valueOf(canvas.clearScreen());

			case "setColor":
				int color = Integer.valueOf(params.get("color"));
				return String.valueOf(canvas.setColor(color));

			case "drawLine":
				int x1 = Integer.valueOf(params.get("x1"));
				int y1 = Integer.valueOf(params.get("y1"));
				int x2 = Integer.valueOf(params.get("x2"));
				int y2 = Integer.valueOf(params.get("y2"));
				return String.valueOf(canvas.drawLine(x1, y1, x2, y2));

			case "drawCircle":
				int x = Integer.valueOf(params.get("x"));
				int y = Integer.valueOf(params.get("y"));
				int r = Integer.valueOf(params.get("r"));
				return String.valueOf(canvas.drawCircle(x, y, r));

			default:
				return unknownCommandError(cmd);
		}
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 3. Définition de la classe DrawingPanel

Nous allons maintenant écrire le `DrawingPanel` qui est chargé de réaliser les tracés. Ce tutoriel n'a pas vocation à expliciter les API Java awt et Swing (classes graphiques, placement des composants, etc.), ces classes étant largement abordées sur le web.

Ci-dessous le code de la classe `DrawingPanel` :

```java
package codelab.modules.drawingmodule;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

public class DrawingPanel extends JPanel {

	private static final int WIDTH = 2000;
	private static final int HEIGHT = 1500;
	private static final int TYPE = BufferedImage.TYPE_INT_ARGB;

	private BufferedImage image;
	private Graphics2D g2d;

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public DrawingPanel() {
		image = new BufferedImage(WIDTH, HEIGHT, TYPE);
		g2d = getGraphics2D(image);
		clearScreen();
	}

	///////////////////////////////////////////////////
	// Component's paintComponent method
	///////////////////////////////////////////////////
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, 0, 0, null);
	}
	
	///////////////////////////////////////////////////
	// Public methods
	///////////////////////////////////////////////////
	
	public int drawLine(int x1, int y1, int x2, int y2) {
		g2d.drawLine(x1, y1, x2, y2);
		repaint();
		return 1;
	}

	public int drawCircle(int x, int y, int r) {
		g2d.drawOval(x-r, y-r, 2*r, 2*r);
		repaint();
		return 1;
	}

	public int setColor(int code) {
		switch (code) {
			case 0: g2d.setColor(Color.BLACK); return 1;
			case 1: g2d.setColor(Color.WHITE); return 1;
			case 2: g2d.setColor(Color.GREEN); return 1;
			case 3: g2d.setColor(Color.BLUE); return 1;
			case 4: g2d.setColor(Color.RED); return 1;
			default: return 0;
		}
	}

	public int clearScreen() {
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, WIDTH, HEIGHT);
		repaint();
		return 1;
	}

	///////////////////////////////////////////////////
	// Private methods
	///////////////////////////////////////////////////
	
	private Graphics2D getGraphics2D(BufferedImage image) {
		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		return g2d;
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 4. Définition de l'API du module

La cohérence de notre module repose sur une bonne correspondance entre :

- Le gestionnaire d'instructions de la classe `DrawingModule`
- Les méthodes publiques de la classe `DrawingPanel`
- Les définitions du fichier XML décrivant l'API du module

Ce dernier permet notamment de générer les fichiers à inclure pour chaque langage cible. Rappellons que le nom de ce fichier est important puisqu'il détermine les noms des fichiers à inclure. Supprimez ou remplacez le fichier d'exemple `DrawingModuleAPI.xml` généré initialement, puis écrivez le document XML suivant dans `canvas.xml` et placez-le dans le répertoire `includes` :

```xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='drawingmodule' name='canvas'>

	<description>API du module de tracé 2D</description>

	<!-- Constants section -->

	<constant name='COLOR_BLACK' type='int' value='0'/>
	<constant name='COLOR_WHITE' type='int' value='1'/>
	<constant name='COLOR_GREEN' type='int' value='2'/>
	<constant name='COLOR_BLUE' type='int' value='3'/>
	<constant name='COLOR_RED' type='int' value='4'/>

	<!-- Instructions section -->

	<instruction name='clearScreen' type='int'/>

	<instruction name='setColor' type='int'>
		<arg name='color' type='int'/>
	</instruction>

	<instruction name='drawLine' type='int'>
		<arg name='x1' type='int'/>
		<arg name='y1' type='int'/>
		<arg name='x2' type='int'/>
		<arg name='y2' type='int'/>
	</instruction>

	<instruction name='drawCircle' type='int'>
		<arg name='x' type='int'/>
		<arg name='y' type='int'/>
		<arg name='r' type='int'/>
	</instruction>
</api>
```

<br/>![](./img/sep.png?style=center+noborder)

## 5. Définition de la barre d'outils

L'écriture de la barre d'outils est très simple puisque le squelette de base a déjà été généré dans `DrawingToolbar.java` par le script `newmodule.sh`. Il suffit juste de procéder à quelques adaptations (icone, texte, tooltip, etc.) afin d'avoir un bouton `Clear` opérationnel :

```java
package codelab.modules.drawingmodule;

import codelab.AbstractToolbar;
import codelab.ToolButton;
import javax.swing.ImageIcon;

public class DrawingToolbar extends AbstractToolbar {

	private ImageIcon icon_clear = loadImageIcon("icon_clear.png");

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public DrawingToolbar(DrawingModule module) {
		super(module);
		addSeparator();
		add(new ToolButton("CLEAR", "Clear", "Clear the canvas", icon_clear, this));
		addButton("MAN");
		endToolbar();
	}

	///////////////////////////////////////////////////
	// Action handler
	///////////////////////////////////////////////////

	public void action(String ident) {
		switch (ident) {
			case "CLEAR": action_clear(); break;
			default: super.action(ident);
		}
	}

	private void action_clear() {
		DrawingPanel canvas = (DrawingPanel) module.getComponent();
		canvas.clearScreen();
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 6. Génération du module

Le module est maintenant prêt à être généré. Avant cela, vous pouvez éventuellement définir des templates, des exercices, voire internationnaliser votre module, comme cela est expliqué dans le tutoriel précédent.

Pour générer le module, exécutez le script de build, ce qui aura pour effet de placer un fichier `DrawingModule.pac` dans le dossier `modules/` de CodeLab :

![](./img/drawing-term.png?style=center+noborder)

Ce second tutoriel est terminé. Vous pouvez continuer à explorer le développement de plugins CodeLab en réalisant les tutoriels suivants.

<br/><center>
![](./img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2026 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center+noborder)
</center>

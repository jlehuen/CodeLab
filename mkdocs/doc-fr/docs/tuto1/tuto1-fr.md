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

![](./img/logo-codelab.png?style=noborder)

# CodeLab IDE & Simulators
# Conception d'un premier module CodeLab
<br/>

En suivant ce tutoriel, vous allez réaliser le plugin "HelloModule", un petit module très simple permettant d'afficher un texte à partir d'un programme en C, Java ou Python. Le rendu ci-dessous est le résultat de l'exécution du programme Python suivant :

```python
from codelab.hellomodule.hello import *

setFont('Times new Roman', 50)
setText('Hello World!')
```

<br/>
![](./img/hello-module.png?style=center+noborder)

La conception et le développement d'un module CodeLab "enfichable" (plugable) sont facilités par un ensemble de classes Java et d'outils qui assurent l'intégration du plugin au sein de l'architecture de CodeLab.

### Architecture logicielle : le modèle PAC (Présentation - Abstraction - Contrôle)

Les modules CodeLab reposent sur le patron d'architecture logicielle **PAC** (introduit par Joëlle Coutaz pour les systèmes interactifs), dont est directement issue l'extension de fichier **`.pac`** des plugins distribués (`HelloModule.pac`), qui fait également écho à **« Plugin Applicatif CodeLab »** :

- **P (Présentation)** : l'interface visuelle Swing vue par l'apprenant (les panneaux graphiques comme `JPanel` ou `JLabel`, et la barre d'outils `HelloToolbar`).
- **A (Abstraction)** : le modèle métier ou la simulation (les données internes, l'état applicatif ou le simulateur temps réel).
- **C (Contrôle)** : la classe centrale du module (héritant d'`AbstractModule`) qui agit comme pivot. Elle réceptionne les ordres des programmes étudiants via la méthode `handleRequest(Map<String, String> params)`, pilote l'Abstraction et synchronise la Présentation.

> 💡 **Différence avec MVC** : Contrairement au patron MVC classique où la Vue peut observer directement le Modèle, dans le modèle PAC **la Présentation et l'Abstraction ne communiquent jamais directement entre elles**. Tout échange transite impérativement par le Contrôleur. Cette étanchéité garantit une indépendance totale entre l'affichage graphique et le code exécuté dans les différents langages (C, Python, Java, etc.).

Ce kit de développement contient :

- Une arborescence pour préparer le développement ;
- Des classes Java à hériter pour écrire vos classes ;
- Des classes Java utilitaires pour faciliter certains aspects ;
- Des scripts de compilation et de packaging.

### Prérequis pour réaliser ce tutoriel :

- Avoir installé la dernière version de CodeLab
- Avoir utilisé CodeLab en tant qu'utilisateur final
- Maîtriser les concepts objet et la POO en Java
- Comprendre la syntaxe des fichiers XML

<br/>![](./img/sep.png?style=center+noborder)

## 1. Création du plugin avec le CDK

Pour développer un nouveau plugin, la création et la configuration de l'arborescence sont entièrement automatisées par le **CDK** (*CodeLab Development Kit*, situé dans `client/codelab.files/hidden/CDK/` ou à la racine de la distribution installée).

> **Important** : CodeLab doit préalablement avoir été exécuté au moins une fois sur la machine !

### Génération automatique du squelette

1. Ouvrez un terminal dans le répertoire `CDK`.
2. Lancez le script de création en indiquant le nom de votre module (`HelloModule`) :
   - Sous Linux / macOS : `./newmodule.sh HelloModule`
   - Sous Windows : `newmodule.bat HelloModule`

Ce script génère automatiquement le dossier du projet dans `modules/HelloModule/`. Tous les renommages (packages, classes Java, XML et manuel) sont déjà effectués automatiquement !
L'exécution du script `build.sh` (ou `build.bat` sous Windows) depuis ce dossier compile le code Java, génère les bibliothèques d'API et assemble le fichier **`.pac`** (`HelloModule.pac`) qui est automatiquement copié dans le répertoire `modules/` de CodeLab.

<pre>
HelloModule/
├── class/        &larr; Réservé à la compilation (ne pas modifier)
├── data/         &larr; Pour vos ressources (images, audio, etc.)
│   ├── icon_clear.png
│   └── icon_hello.png
├── includes/     &larr; Pour les fichiers d'API
│   ├── api.dtd
│   └── HelloModuleAPI.xml
├── lib/
│   └── codelab.jar &larr; Réservé à la compilation (ne pas supprimer)
├── man/          &larr; Manuel d'atelier en Markdown
│   └── HelloModule.md
├── src/          &larr; Sources Java de votre plugin
│   └── codelab/modules/hellomodule/
│       ├── HelloModule.java
│       └── HelloToolbar.java
├── templates/    &larr; Modèles de programmes pour chaque langage
│   ├── hello.c
│   ├── hello.clp
│   ├── hello.java
│   └── hello.py
├── build.sh      &larr; Script Bash de compilation et de packaging
└── build.bat     &larr; Script DOS de compilation et de packaging
</pre>

> 💡 *Note sur la barre d'outils* : le script génère initialement `HelloModuleToolbar.java`. Vous pouvez la renommer en `HelloToolbar.java` (en adaptant le nom de la classe et son appel dans `HelloModule.java`), convention plus concise que nous adoptons dans la suite de ce tutoriel.

L'exécution régulière du script `build.sh` (ou `build.bat` sous Windows) produit le paquet **`.pac`** (`HelloModule.pac`) vous permettant de tester votre module au fil de son développement.

La classe `HelloToolbar.java` ne sera pas modifiée dans un premier temps.

<br/>![](./img/sep.png?style=center+noborder)

## 2. Classes principales du module

La conception d'un plugin doit respecter le modèle objet suivant, les deux super-classes `AbstractModule` et `AbstractToolbar` assurant l'intégration du plugin dans l'architecture de CodeLab. La classe héritée `HelloModule` sera composée d'un JPanel chargé de la vue. Comme dans un premier temps nous n'ajoutons pas de boutons spécifiques à la barre d'outils, la classe `DefaultToolbar` sera implicitement utilisée :

![](./img/hello-uml.png?style=center+noborder)

Commençons par définir la classe `HelloModule` de la façon suivante :

```java
package codelab.modules.hellomodule;

import codelab.AbstractModule;
import codelab.CodeLab;
import java.util.Map;
import javax.swing.JPanel;

public class HelloModule extends AbstractModule {

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public HelloModule(CodeLab codelab) {
		super(codelab);
		setTitle("Hello Lab");
		setComponent(new JPanel());
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
			case "cmd1": return "Value to return";
			case "cmd2": return "Value to return";
			default: return unknownCommandError(cmd);
		}
	}
}
```

Vous pouvez d'ors-et-déjà générer votre plugin afin de vérifier qu'il apparaît bien en tant que nouvel onglet dans CodeLab. Pour ce faire, exécutez le script `build.sh` et vérifiez qu'un fichier `HelloModule.pac` a bien été placé dans le répertoire `modules` de CodeLab. Lancez CodeLab et constatez que vous avez un nouvel onglet intitulé "Hello Lab" mais rien de plus.

Voilà le contenu d'un terminal suite à l'exécution du script `build.bat` (si vous êtes sous Windows) ou `build.sh` (si vous être sous OSX ou Linux) lorsque tout s'est bien passé :

![](./img/hello-term.png?style=center+noborder)

<br/>![](./img/sep.png?style=center+noborder)

## 3. Méthodes "métier" de la classe principale

Nous allons maintenant compléter la classe `HelloModule` en ajoutant les méthodes qui devront répondre à la finalité de notre module (afficher un texte dans le JPanel) :

- Une méthode `int setText(String str)` pour afficher une chaîne de caractères
- Une méthode `int setFont(String name, int size)` pour définir la fonte et sa taille

Notez que ces méthodes retournent un entier. Ce n'est pas obligatoire mais cela permet de se souvenir qu'il est toujours possible de retourner une valeur. Le rendu final sera réalisé par un simple JLabel instancié et ajouté au JPanel à l'intérieur du constructeur. Voilà ci-dessous les modifications apportées à la classe `HelloModule` (ajoutez les imports awt et Swing nécessaires) :

```java
public class HelloModule extends AbstractModule {

	private JPanel panel;
	private JLabel label;

	///////////////////////////////////////////////////
	// Constructor
	///////////////////////////////////////////////////

	public HelloModule(CodeLab codelab) {
		super(codelab);
		setTitle("Hello Lab");
		setComponent(panel = new JPanel());
		panel.setLayout(new GridBagLayout());
		panel.add(label = new JLabel());
	}
	
	///////////////////////////////////////////////////
	// Public methods
	///////////////////////////////////////////////////
	
	public int setText(String str) {
		label.setText(str);
		return 1; // No error
	}

	public int setFont(String name, int size) {
		label.setFont(new Font(name, 1, size));
		return 1; // No error
	}
```

<br/>![](./img/sep.png?style=center+noborder)

## 4. Écriture du gestionnaire d'instructions

La classe `HelloModule` comporte un *gestionnaire d'instructions* qui reçoit les instructions en provenance des futurs programmes écrits en C, Java, Python, etc. Ce gestionnaire est implémenté par la méthode `handleRequest` qui reçoit une instruction sous la forme d'un dictionnaire `Map<String, String>` et qui retourne une chaîne de caractères. Le dictionnaire contient l'instruction et ses paramètres. La structure `switch-case` devra, pour chaque instruction à implémenter, extraire les paramètres, les *caster* vers les types attendus, invoquer la bonne méthode, et retourner un résultat sous la forme d'une chaîne de caractères :

```java
public String handleRequest(Map<String, String> params) {
    String cmd = params.get("cmd");
    switch (cmd) {
        case "cmd1": return "Value to return";
        case "cmd2": return "Value to return";
        default: return unknownCommandError(cmd);
    }
}
```

Voilà ci-dessous le gestionnaire de `HelloModule` complété avec les instructions `setText` et `setFont` :

```java
public String handleRequest(Map<String, String> params) {
	String cmd = params.get("cmd");
	switch (cmd) {
	
		case "setText":
			String str = params.get("str");
			return String.valueOf(setText(str));
		
		case "setFont":
			String name = params.get("name");
			int size = Integer.valueOf(params.get("size"));
			return String.valueOf(setFont(name, size));
		
		default:
			return unknownCommandError(cmd);
	}
}
```

<br/>![](./img/sep.png?style=center+noborder)

## 5. Écriture des includes pour chaque langage cible

Cette étape incontournable consiste à générer les fichiers à inclure pour chaque langage cible. Ce sont eux qui constituent l'interface entre un language de programmation et l'environnement CodeLab (Application Programming Interface). La génération de ces fichiers sera réalisée automatiquement à partir de documents XML qui comportent (dans cet ordre) :

- Des éléments "constante" (name + type + value)
- Des éléments "instruction" (protoypes des instuctions)
- Des éléments "include" (code à insérer directement)

Cette dernière section permet de placer dans les fichier à include des définitions qui sortent du cadre imposé par la grammaire des instructions. La DTD est la suivante :

```xml
<!ELEMENT api (constant*, instruction*, include*)>

<!ELEMENT constant EMPTY>
<!ATTLIST constant name CDATA #REQUIRED>
<!ATTLIST constant type (int|float|string) #REQUIRED>
<!ATTLIST constant value CDATA #REQUIRED>

<!ELEMENT instruction (arg*)>
<!ATTLIST instruction name CDATA #REQUIRED>
<!ATTLIST instruction type (int|float|string) #REQUIRED>

<!ELEMENT arg EMPTY>
<!ATTLIST arg name CDATA #REQUIRED>
<!ATTLIST arg type (int|float|string) #REQUIRED>

<!ELEMENT include (#PCDATA)>
<!ATTLIST include lang (c|java|python) #REQUIRED>
```

Le choix du nom de ce fichier est important puisqu'il détermine les noms des fichiers à inclure. Par exemple, si nous nommons ce fichier `hello.xml` avec l'attribut `name='hello'`, l'exécution du build générera les bibliothèques clientes `hello.c`, `hello.py`, `Hello.java`, `hello.clp` et `hello.go`. Supprimez ou remplacez le fichier d'exemple `HelloModuleAPI.xml` généré initialement par le CDK, et écrivez le document XML suivant dans `includes/hello.xml` :

```xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='hellomodule' name='hello'>

	<description>API du module Hello</description>

	<instruction name='setText' type='int'>
		<arg name='str' type='string'/>
	</instruction>

	<instruction name='setFont' type='int'>
		<arg name='name' type='string'/>
		<arg name='size' type='int'/>
	</instruction>

</api>
```

Générez une nouvelle fois votre plugin en exécutant le script de build et regardez les fichiers générés dans le répertoire `includes`. Examinons par exemple le fichier `hello.py` généré pour le langage Python :

```py
SETTEXT_CMD = 'module=HELLOMODULE&cmd=setText&str=%s'
SETFONT_CMD = 'module=HELLOMODULE&cmd=setFont&name=%s&size=%d'

def setText(str):
	buffer = SETTEXT_CMD % str
	return int(request(buffer))

def setFont(name, size):
	buffer = SETFONT_CMD % (name, size)
	return int(request(buffer))
```

Quel que soit le langage cible, ces fichiers sont construits de la même façon et contiennent :

- Un ensemble de chaînes de formattage (une par instruction)
- Une fonction (ou méthode) par instruction à implémenter
- Éventuellement des constantes et des fonctions utilitaires

Les chaînes de formattage sont construites sur le modèle des requêtes HTTP GET ou vers un socket-server : `module=...&cmd=...&arg1=...&arg2=...&etc.`. Chaque instruction implémentée devra insérer les paramètres effectifs dans la chaîne et envoyer cette dernière à CodeLab via la fonction `request`.

<br/>![](./img/sep.png?style=center+noborder)

## 6. Écriture de quatre templates

Cette étape (optionnelle) consiste à concevoir et à ajouter au module des *templates* (ou des exercices) qui seront proposé dans l'environnement CodeLab au moment de l'ajout de nouveaux programmes.

### Exemple de template "growing.py" pour le langage Python :

```python
from codelab.hellomodule.hello import *

## Program _fileName_
## Created by _author_ on _date_

setText('Hello World!')
for size in range(0, 50):
	setFont('Times new Roman', size)
```

### Exemple de template "growing.c" pour le langage C :

```c
#include "codelab/hellomodule/hello.c"

// Program _fileName_
// Created by _author_ on _date_

int main() {
	setText("Hello World!");
	for (int size = 0; size < 50; size++)
		setFont("Times new Roman", size);
	return 0;
}
```

### Exemple de template "growing.java" pour le langage Java :

```java
import codelab.hellomodule.Hello;

// Class _className_
// Created by _author_ on _date_

class _className_ extends Hello {

	private void run() {
		setText("Hello World!");
		for (int size = 0; size < 50; size++)
			setFont("Times new Roman", size);
	}
	
	public static void main(String args[]) {
		new _className_().run();
	}
}
```

### Exemple de template "growing.clp" pour le langage CLIPS :

```lisp
#requires codelab.hellomodule.hello

;; Program _fileName_
;; Created by _author_ on _date_

(defrule only-rule
	=>
	(setText "Hello World!")
	(loop-for-count (?size 0 50)
		(setFont "Times new Roman" ?size)))
```

Déposez ces fichiers dans le répertoire `templates` de votre arborescence, et vérifiez qu'elle correspond à ce qui suit, avant de générer à nouveau votre module :

<pre>
HelloModule/
├── data/
│   ├── icon_clear.png
│   └── icon_hello.png
├── lib/
│   └── codelab.jar
├── man/
│   └── HelloModule.md
├── src/
│   └── codelab/
│       └── modules/
│           └── hellomodule/
│               ├── HelloModule.java
│               └── HelloToolbar.java 
├── includes/
│   ├── api.dtd
│   ├── hello.c
│   ├── hello.clp
│   ├── Hello.java
│   ├── hello.py
│   └── hello.xml
├── templates/
│   ├── growing.c
│   ├── growing.clp
│   ├── growing.java
│   └── growing.py
├── build.bat
└── build.sh
</pre>

Générez votre plugin à nouveau, exécutez CodeLab et testez votre module...

<br/>![](./img/sep.png?style=center+noborder)

## 7. Ajout de boutons dans la barre d'outils

Vous allez maintenant personnaliser votre barre d'outils en lui rajoutant des boutons `Hello` et `Clear` :

![](./img/hello-toolbar.png?style=center+noborder)

Commencez par instancier une `HelloToolbar` dans le constructeur de `HelloModule`, de la façon suivante :

```java
public HelloModule(CodeLab codelab) {
	super(codelab);
	setTitle("Hello Lab");
	setToolbar(new HelloToolbar(this));
	setComponent(panel = new JPanel());
	panel.setLayout(new GridBagLayout());
	panel.add(label = new JLabel());
}
```

Vous pouvez déjà tester votre module ainsi modifié, et vous verrez apparaître une nouveau bouton `Hello` qui se contente d'afficher un "Hello World!" orange dans la console de CodeLab. Examinez les paramètres du `ToolButton` dans le constructeur de la `HelloToolbar` :

- `"HELLO"` : l'identifiant du bouton / de l'action à exécuter
- `"Hello"` : le texte affiché sous le bouton
- `"Say Hello World"` : le texte de l'info-bulle
- `icon_hello` : l'icone du bouton
- `this` : la barre de boutons

Regardez également le *gestionnaire d'actions* de la `HelloToolbar` ainsi que sa méthode `action_hello` invoquée par le gestionnaire. Modifiez cette méthode afin d'afficher le "Hello World!", non plus dans la console, mais dans le JPanel du module :

```java
private void action_hello() {
	((HelloModule) module).setFont("Times", 50);
	((HelloModule) module).setText("Hello World!");
}
```

Puis, sur le modèle du bouton `Hello`, ajoutez un bouton `Clear` qui devra effacer le contenu du JPanel. Cela implique de modifier le constructeur, le gestionnaire d'actions et d'écrire une méthode d'action `action_clear` :

```java
public class HelloToolbar extends AbstractToolbar {

	private ImageIcon icon_hello = loadImageIcon("icon_hello.png");
	private ImageIcon icon_clear = loadImageIcon("icon_clear.png");

	public HelloToolbar(HelloModule module) {
		super(module);
		addSeparator();
		add(new ToolButton("HELLO", "Hello", "Say Hello World", icon_hello, this));
		add(new ToolButton("CLEAR", "Clear", "Clear the panel", icon_clear, this));
		addButton("MAN");
		endToolbar();
	}
	
	public void action(String ident) {
		switch (ident) {
			case "HELLO": action_hello(); break;
			case "CLEAR": action_clear(); break;
			default: super.action(ident);
		}
	}

	private void action_hello() {
		((HelloModule) module).setFont("Times", 50);
		((HelloModule) module).setText("Hello World!");
	}

	private void action_clear() {
		((HelloModule) module).clear();
	}
}
```

Ce n'est pas tout : n'oubliez pas d'écrire la méthode `clear` du `HelloModule` :

```java
public int clear() {
	panel.removeAll();
	panel.add(label = new JLabel());
	panel.repaint();
	return 1;
}
```

Vous pouvez également ajouter une nouvelle instruction `clear()` dans l'API du module en complétant le fichier `hello.xml` et le gestionnaire d'instructions `handleRequest` :

```java
case "clear":
	return String.valueOf(clear());
```

<br/>![](./img/sep.png?style=center+noborder)

## 8. Internationnalisation du module

Commencez par ajoutez un fichier `lang.properties` dans le dossier `data` et copiez-y les lignes suivantes :

```
title_fr = Bonjour Lab
title_en = Hello Lab

btn_hello_fr = Bonjour
btn_hello_en = Hello

btn_hello_tooltip_fr = Dire Bonjour
btn_hello_tooltip_en = Say Hello World

btn_clear_fr = Effacer
btn_clear_en = Clear

btn_clear_tooltip_fr = Effacer le panel
btn_clear_tooltip_en = Clear the panel
```

Les classes `AbstractModule` et `AbstractToolbar` contiennent toutes deux les méthodes `getProperty(String key)` et `getLangProperty(String key)` qui recherchent une chaîne à partir d'une clé dans les fichiers de propriétés. La particularité de la seconde méthode est d'ajouter le suffixe qui correspond à la langue spécifié par l'entrée `LANG` dans le fichier `userconfig/user.properties` de CodeLab. Ainsi, si `LANG=fr` l'entrée `title` devient `title_fr`.

Chargez ce fichier au début du constructeur de `HelloModule` et utilisez cette facilité dans le `setTitle` :

```java
public HelloModule(CodeLab codelab) {
	super(codelab);
	loadProperties("lang.properties");
	setTitle(getLangProperty("title"));
	setToolbar(new HelloToolbar(this));
	setComponent(panel = new JPanel());
	panel.setLayout(new GridBagLayout());
	panel.add(label = new JLabel());
}
```

Pour terminer, modifiez le constructeur de la `HelloToolbar` comme suit :

```java
public HelloToolbar(HelloModule module) {
	super(module);
	addSeparator();

	add(new ToolButton("HELLO",
		getLangProperty("btn_hello"),
		getLangProperty("btn_hello_tooltip"), icon_hello, this));

	add(new ToolButton("CLEAR",
		getLangProperty("btn_clear"),
		getLangProperty("btn_clear_tooltip"), icon_clear, this));

	addButton("MAN");
	endToolbar();
}
```

> 💡 **Manuel d'atelier intégré (`man/`)** : Le fichier Markdown `man/HelloModule.md` est automatiquement compilé en HTML (`HelloModule.html`) lors de chaque exécution de `./build.sh` (ou `build.bat`), packagé directement au sein de `HelloModule.pac`, et consultable dans le navigateur système via le bouton `MAN` de la barre d'outils ou le menu *Plugins > Manuel de l'atelier*.

Ce tutoriel est terminé. Vous pouvez continuer à explorer le développement de plugins CodeLab en réalisant les tutoriels suivants.

<br/><center>
![](./img/fin.png?style=center+noborder)
<br/>Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2022 Jérôme Lehuen, Le Mans Université, France
<br/>![](./img/licence.png?style=center+noborder)
<br/>Version 04-05-2022
</center>
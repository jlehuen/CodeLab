<link href='../css/style-md.css' rel='stylesheet'>

<header id='header'>
	<div class='left'>
		<a href='../index.html#documentation'>Codelab Home page</a><br/>
		<a href='#top'>Return to the top of this page</a><br/>
		<a href='#toc'>Go to table of contents</a><br/>
	</div>
	<div class='center'>
		<h1>CodeLab Tutorial #1</h1>
	</div>
	<div class='right'>
		<button type='button' onclick="setlang('fr'); location.href='tuto1-fr.html'">FR</button>
		<button type='button' onclick="setlang('en'); location.href='tuto1-en.html'">EN</button>
	</div>
</header>

<script type='text/javascript'>
	function setlang(lang) {
		const languages = ['fr','en'];
		if (!languages.includes(lang)) lang = 'en';
		sessionStorage.setItem('LANG', lang);
	}
</script>

<a name=0></a>
<br/>
<br/>
<center>
# Designing a first CodeLab module
</center>

By following this tutorial, you will develop the "HelloModule" plugin, a small and simple module to display a text from a C, Java or Python program. The rendering below is the result of the execution of the following Python program:

~~~python
from HelloModule.hello import *

setFont('Times new Roman', 50)
setText('Hello World!')
~~~

![](./img/hello-module.png?style=center+margin)

The design and development of a CodeLab plugin is facilitated by a set of Java classes and tools that ensure the integration of the plugin into the CodeLab architecture. This development kit contains:

- A tree structure to prepare the development;
- Some Java classes to be inherited to write your classes;
- Some Java utility classes to facilitate certain aspects;
- Compilation and packaging scripts.

### Prerequisite for this tutorial:

- Having the latest version of CodeLab installed
- Having used CodeLab as an end-user
- Mastering the object concepts and OOP in Java
- Understanding the syntax of XML files

<a name=toc></a>
### Table of contents :

1. [Development tree](#1)
2. [Main classes of the module](#2)
3. [Business methods of the main class](#3)
4. [Writing the instruction handler](#4)
5. [Writing the "includes" for each target language](#5)
6. [Writing four templates] (#6)
7. [Adding buttons to the toolbar](#7)
8. [Internationalization of the module](#8)

<a name=1></a>
## 1. Creating the plugin with the CDK

To develop a new plugin, folder generation and configuration are fully automated by the **CDK** (*CodeLab Development Kit*, located in `client/codelab.files/hidden/CDK/` or at the root of the installed distribution).

> **Important**: CodeLab must have been run at least once on your machine beforehand!

### Automatic skeleton generation

1. Open a terminal in the `CDK` directory.
2. Run the creation script specifying your module name (`HelloModule`):
   - Under Linux / macOS: `./newmodule.sh HelloModule`
   - Under Windows: `newmodule.bat HelloModule`

This script automatically creates the `HelloModule/` folder in `CDK/` as well as an initial ready-to-use `HelloModule.pac` package in the `modules/` directory of CodeLab. All renamings (packages, Java classes, XML, and manual) are performed automatically!

<pre>
HelloModule/
├── class/        &larr; Reserved for compilation (do not modify)
├── data/         &larr; For your resources (images, audio, etc.)
│   ├── icon_clear.png
│   └── icon_hello.png
├── includes/     &larr; For API specification files
│   ├── api.dtd
│   └── HelloModuleAPI.xml
├── lib/
│   └── codelab.jar &larr; Reserved for compilation (do not delete)
├── man/          &larr; Workshop manual in Markdown
│   └── HelloModule.md
├── src/          &larr; Java sources of your plugin
│   └── codelab/modules/hellomodule/
│       ├── HelloModule.java
│       └── HelloToolbar.java
├── templates/    &larr; Program templates for each language
│   ├── hello.c
│   ├── hello.clp
│   ├── hello.java
│   └── hello.py
├── build.sh      &larr; Bash compilation and packaging script
└── build.bat     &larr; DOS compilation and packaging script
</pre>

Regularly executing the `build.sh` script (or `build.bat` on Windows) will produce a **`.pac`** file (`HelloModule.pac`) that is automatically copied into the `modules/` directory of CodeLab, allowing you to test your module as you develop it.

The `HelloToolbar.java` class will not be modified at first.

<a name=2></a>
## 2. Main classes of the module

The design of a plugin must respect the following object model, with the two superclasses `AbstractModule` and `AbstractToolbar` ensuring the integration of the plugin into the CodeLab architecture. The inherited class `HelloModule` will consist of a JPanel in charge of the view. As we don't add specific buttons to the toolbar at first, the `DefaultToolbar` class will be used implicitly:

![](./img/hello-uml.png?style=center+margin)

Let's start by defining the `HelloModule` class as follows:

~~~java
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
~~~

You can already generate your plugin to verify that it appears as a new tab in CodeLab. To do this, run the `build.sh` script and check that a `HelloModule.pac` file has been placed in the `modules` directory of CodeLab. Start CodeLab and notice that you have a new tab called `Hello Lab` but nothing more.

Here is the content of a terminal after the execution of the `build.bat` script (under Windows) or `build.sh` (under OSX or Linux) when everything has gone well:

![](./img/hello-term.png?style=center+margin)

<a name=3></a>
## 3. Business methods of the main class

We are now going to complete the `HelloModule` class by adding the methods that will answer the purpose of our module (display a text in the JPanel):

- An `int setText(String str)` method to display a string
- An `int setFont(String name, int size)` method to set the font and its size

Note that these methods return an integer. This is not mandatory but it helps to remember that it is always possible to return a value. The final rendering will be done by a simple JLabel instantiated and added to the JPanel inside the constructor. Here are the modifications made to the `HelloModule` class (add the necessary awt and Swing imports):

~~~java
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
~~~

<a name=4></a>
## 4. Writing the instruction handler

The `HelloModule` class includes an *instruction handler* that receives instructions from future programs written in C, Java, Python, etc. This handler is implemented by the `handleRequest` method which receives an instruction in the form of a `Map<String, String>` dictionary and returns a string. The dictionary contains the instruction and its parameters. The `switch-case` structure will, for each instruction to be implemented, extract the parameters, *cast* them to the expected types, invoke the correct method, and return a result in the form of a string:

~~~java
public String handleRequest(Map<String, String> params) {
    String cmd = params.get("cmd");
    switch (cmd) {
        case "cmd1": return "Value to return";
        case "cmd2": return "Value to return";
        default: return unknownCommandError(cmd);
    }
}
~~~

Here is the `HelloModule` handler completed with the `setText` and `setFont` instructions:

~~~java
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
~~~

<a name=5></a>
## 5. Writing the "includes" for each target language

This essential step consists in generating the files to be included for each target language. These files are the interface between a programming language and the CodeLab environment (Application Programming Interface). The generation of these files will be done automatically from XML documents which include (in this order):

- Constant elements (name + type + value)
- Instruction elements (protoypes of instructions)
- Include elements (code to be inserted directly)

This last section allows to place in the include files definitions that go beyond the framework imposed by the instruction grammar. The DTD is as follows:

~~~xml
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
~~~

The choice of the name of this file is important because it determines the names of the files to be included. For example, if we name this file `hello.xml`, running the build script will handle the files `hello.c`, `hello.py` and `Hello.java`. Write the following XML document and place it in the `includes` directory:

~~~xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE api SYSTEM 'api.dtd'>
<api module='HelloModule'>

	<instruction name='setText' type='int'>
		<arg name='str' type='string'/>
	</instruction>

	<instruction name='setFont' type='int'>
		<arg name='name' type='string'/>
		<arg name='size' type='int'/>
	</instruction>

</api>
~~~

Generate your plugin again by running the build script and look at the generated files in the `includes` directory. For example, let's look at the `hello.py` file generated for the Python language:

~~~py
SETTEXT_CMD = 'module=HelloModule&cmd=setText&str=%s'
SETFONT_CMD = 'module=HelloModule&cmd=setFont&name=%s&size=%d'

def setText(str):
	buffer = SETTEXT_CMD % str
	return int(request(buffer))

def setFont(name, size):
	buffer = SETFONT_CMD % (name, size)
	return int(request(buffer))
~~~

Whatever the target language, these files are built in the same way and contain:

- A set of formatting strings (one per instruction)
- One function (or method) per instruction to be implemented
- Maybe constants and utility functions

The formatting strings are built on the model of HTTP GET requests, or requests to a socket-server: `module=...&cmd=...&arg1=...&arg2=...&etc.`. Each implemented statement will have to insert the actual parameters in the string and send the latter to CodeLab via the `request` function.

<a name=6></a>
## 6. Writing four templates

This optional step consists in designing and adding *templates* (or exercises) to the module, that will be proposed in the CodeLab environment when adding new programs.

### Example of "growing.py" template for the Python language:

~~~python
from HelloModule.hello import *

## Program _fileName_
## Created by _author_ on _date_

setText('Hello World!')
for size in range(0, 50):
	setFont('Times new Roman', size)
~~~

### Example of "growing.c" template for the C language:

~~~c
#include "HelloModule/hello.c"

/*
 * Program _fileName_
 * Created by _author_ on _date_
 */

int main() {
	setText("Hello World!");
	for (int size = 0; size < 50; size++)
		setFont("Times new Roman", size);
	return 0;
}
~~~

### Example of "growing.java" template for the Java language:

~~~java
import HelloModule.Hello;

/*
 * Class _className_
 * Created by _author_ on _date_
 */

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
~~~

### Example of "growing.clp" template for the CLIPS language:

~~~scheme
#requires HelloModule/hello

;; Program _fileName_
;; Created by _author_ on _date_

(defrule only-rule
	=>
	(setText "Hello World!")
	(loop-for-count (?size 0 50)
		(setFont "Times new Roman" ?size)))
~~~

Put these files in the `templates` directory of your tree, and check that it matches the following, before generating your module again:

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

Generate your plugin again, run CodeLab and test your module...

<a name=7></a>
## 7. Adding buttons to the toolbar

You will now customize your toolbar by adding `Hello` and `Clear` buttons to it:

![](./img/hello-toolbar.png?style=center+margin)

Start by instantiating a `HelloToolbar` in the `HelloModule` constructor, as follows:

~~~java
public HelloModule(CodeLab codelab) {
	super(codelab);
	setTitle("Hello Lab");
	setToolbar(new HelloToolbar(this));
	setComponent(panel = new JPanel());
	panel.setLayout(new GridBagLayout());
	panel.add(label = new JLabel());
}
~~~

You can already test your modified module, and you will see a new `Hello` button that just displays an orange "Hello World!" in the CodeLab console. Look at the `ToolButton` parameters in the `HelloToolbar` constructor:

- `"HELLO"` : the identifier of the button/action to execute
- `"Hello"` : the text displayed under the button
- `"Say Hello World"` : the text of the tooltip
- `icon_hello` : the button icon
- `this` : the button bar itself

Also look at the `HelloToolbar`'s *action manager* and its `action_hello` method invoked by the manager. Modify this method to display the "Hello World!" not in the console, but in the JPanel of the module:

~~~java
private void action_hello() {
	((HelloModule) module).setFont("Times", 50);
	((HelloModule) module).setText("Hello World!");
}
~~~

Then, based on the `Hello` button, add a `Clear` button that should clear the contents of the JPanel. This involves modifying the constructor, the action handler and writing an action method `action_clear`:

~~~java
public class HelloToolbar extends AbstractToolbar {

	private ImageIcon icon_hello = loadImageIcon("icon_hello.png");
	private ImageIcon icon_clear = loadImageIcon("icon_clear.png");

	public HelloToolbar(HelloModule module) {
		super(module);
		addSeparator();
		add(new ToolButton("HELLO", "Hello", "Say Hello World", icon_hello, this));
		add(new ToolButton("CLEAR", "Clear", "Clear the panel", icon_clear, this));
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
~~~

That's not all: don't forget to write the `clear` method of the `HelloModule`:

~~~java
public int clear() {
	panel.removeAll();
	panel.add(label = new JLabel());
	panel.repaint();
	return 1;
}
~~~

You can also add a new `clear()` statement to the module's API by completing the `hello.xml` file and the statement handler.

<a name=8></a>
## 8. Internationalization of the module

Start by adding a `lang.properties` file in the `data` folder and copy the following lines into it:

~~~
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
~~~

The `AbstractModule` and `AbstractToolbar` classes both contain `getProperty(String key)` and `getLangProperty(String key)` methods which look for a string from a key in property files. The particularity of the second method is to add the suffix that corresponds to the language specified by the `LANG` entry in the CodeLab `userconfig/user.properties` file. Thus, if `LANG=en` then the `title` entry becomes `title_en`.

Load this file at the beginning of the `HelloModule` constructor and use this facility in the `setTitle`:

~~~java
public HelloModule(CodeLab codelab) {
	super(codelab);
	loadProperties("lang.properties");
	setTitle(getLangProperty("title"));
	setToolbar(new HelloToolbar(this));
	setComponent(panel = new JPanel());
	panel.setLayout(new GridBagLayout());
	panel.add(label = new JLabel());
}
~~~

Finally, modify the `HelloToolbar` constructor as follows:

~~~java
public HelloToolbar(HelloModule module) {
	super(module);
	addSeparator();

	add(new ToolButton("HELLO",
		getLangProperty("btn_hello"),
		getLangProperty("btn_hello_tooltip"), icon_hello, this));

	add(new ToolButton("CLEAR",
		getLangProperty("btn_clear"),
		getLangProperty("btn_clear_tooltip"), icon_clear, this));

	endToolbar();
}
~~~

> 💡 **Built-in workshop manual (`man/`)**: The Markdown file `man/HelloModule.md` is automatically compiled into HTML (`HelloModule.html`) whenever `./build.sh` (or `build.bat`) is executed, packaged directly inside `HelloModule.pac`, and can be opened in the system browser via the toolbar `MAN` button or the *Plugins > Workshop Manual* menu.

This tutorial is over. You can continue to explore CodeLab plugin development by completing the next tutorials.

<center>
![](./img/fin.png?style=center)
Document under Creative Commons License CC-BY-NC-ND
<br/>Copyright &copy; 2021 Jérôme Lehuen, Le Mans Université, France
![](./img/licence.png?style=center)
Version 25-04-2021
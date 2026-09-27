#####################################################################
## This file is part of the software CodeLab IDE and Simulators    ##
## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
#####################################################################

This directory contains the CDK (CodeLab Development Kit)
that helps you to build CodeLab plugins as .pac files

>> DO NOT DELETE, MODIFY OR MOVE THE FOLOWING FILES:

- newmodule.zip : compressed pac template
- newmodule.bat : script for MS-DOS systems
- newmodule.sh  : script for Linux and OSX systems
- the bin folder contains MS-DOS tools used by newmodule.bat

Procedure for building a "Foo" plugin:
--------------------------------------

>> CodeLab must have been run at least once!

1. Open a Bash (or MS-DOS) terminal into the CDK directory
2. Run the command "newmodule.sh Foo" (or newmodule.bat Foo)

	- This creates a Foo folder in the CDK folder
	- and a first version of Foo.pac in the modules folder

3. You can already test your new "Module Foo" by launching CodeLab

	- A new template "Hello" has been added for each of the supported languages

4. You can now view and edit the following 3 files:

	- Foo/src/codelab/modules/foo/Foo.java
	- Foo/src/codelab/modules/foo/FooToolbar.java
	- Foo/includes/FooAPI.xml to describe your API in XML format

5. Run the command build.sh (or build.bat) to build and test your plugin

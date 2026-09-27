:: #####################################################################
:: ## This file is part of the software CodeLab IDE and Simulators    ##
:: ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
:: #####################################################################

:: DO NOT DELETE, MODIFY OR MOVE THIS SCRIPT !!

@ECHO OFF & SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

IF [%~1]==[] GOTO blank

ECHO    Unzipping the plugin template...
bin\unzip.exe -q newmodule.zip

SET NAME=%1
SET PACKAGE=%1
CALL :tolower PACKAGE
SET MODULEPATH=YourModule\src\codelab\modules\yourmodule

SET FILE1=%MODULEPATH%\YourModule.java
SET FILE2=%MODULEPATH%\YourToolbar.java
SET FILE3=YourModule\includes\yourAPI.xml

SET HELLO_C=YourModule\templates\hello.c
SET HELLO_GO=YourModule\templates\hello.go
SET HELLO_PY=YourModule\templates\hello.py
SET HELLO_CLP=YourModule\templates\hello.clp
SET HELLO_JAVA=YourModule\templates\hello.java

:: #########################################################
:: # Replace some strings in files
:: #########################################################

ECHO    Replacing some strings...

bin\fart.exe -q "%FILE1%" yourmodule %PACKAGE%
bin\fart.exe -q "%FILE2%" yourmodule %PACKAGE%
bin\fart.exe -q "%FILE3%" yourmodule %PACKAGE%

bin\fart.exe -q "%FILE1%" YourModule %NAME%
bin\fart.exe -q "%FILE2%" YourModule %NAME%

bin\fart.exe -q "%FILE1%" YourToolbar %NAME%Toolbar
bin\fart.exe -q "%FILE2%" YourToolbar %NAME%Toolbar

bin\fart.exe -q "%FILE3%" yourAPI %NAME%API

bin\fart.exe -q "%HELLO_C%" yourmodule %PACKAGE%
bin\fart.exe -q "%HELLO_GO%" yourmodule %PACKAGE%
bin\fart.exe -q "%HELLO_PY%" yourmodule %PACKAGE%
bin\fart.exe -q "%HELLO_CLP%" yourmodule %PACKAGE%
bin\fart.exe -q "%HELLO_JAVA%" yourmodule %PACKAGE%

bin\fart.exe -q "%HELLO_C%" yourAPI %NAME%API
bin\fart.exe -q "%HELLO_GO%" yourAPI %NAME%API
bin\fart.exe -q "%HELLO_PY%" yourAPI %NAME%API
bin\fart.exe -q "%HELLO_CLP%" yourAPI %NAME%API
bin\fart.exe -q "%HELLO_JAVA%" yourAPI %NAME%API

:: #########################################################
:: # Rename the project files
:: #########################################################

ECHO    Renaming the files...

RENAME %MODULEPATH%\YourModule.java %NAME%.java
RENAME %MODULEPATH%\YourToolbar.java %NAME%Toolbar.java
RENAME %MODULEPATH% %PACKAGE%

RENAME YourModule\includes\yourAPI.xml %NAME%API.xml
MOVE YourModule ..\..\modules\%NAME% >nul

:: #########################################################
:: # Display some information
:: #########################################################

ECHO       Package name : codelab.modules.%PACKAGE%
ECHO       Module file  : %NAME%.java
ECHO       Toolbar file : %NAME%Toolbar.java
ECHO       API file     : %NAME%API.xml
EXIT

:tolower
	FOR %%L IN (^^ a b c d e f g h i j k l m n o p q r s t u v w x y z) DO SET %1=!%1:%%L=%%L!
GOTO :EOF

:blank
    ECHO Incorrect number of parameters
    ECHO Usage: newmodule.bat ModuleName
	PAUSE
    EXIT

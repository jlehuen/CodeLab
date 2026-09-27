:: #####################################################################
:: ##  This file is part of the software CodeLab IDE and Simulators   ##
:: ##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
:: #####################################################################

:: DO NOT DELETE OR MODIFY THIS FILE !!

@ECHO OFF
SET HOME=%CD%
SET JAVA=%HOME%\.rsc\languages\JDK-11.0.9.1+1\bin\javaw
SET DLIBPATH=%HOME%\.rsc\natives
SET CODELAB=%HOME%\.rsc\codelab\CodeLab.jar
SET NEWCODELAB=%HOME%\.rsc\codelab\CodeLab-new.jar
ATTRIB +H +S .rsc
CLS

IF EXIST %NEWCODELAB% (
	DEL %CODELAB%
	RENAME %NEWCODELAB% CodeLab.jar
)

START %JAVA% -Xmx1024m ^
	-Djava.library.path=%DLIBPATH% ^
	-Dfile.encoding=UTF-8 ^
	-jar %CODELAB% --log

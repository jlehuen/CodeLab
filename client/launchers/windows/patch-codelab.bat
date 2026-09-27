@echo off
cd /d "%~dp0"
if exist ".hidden\codelab" (
    copy /Y codelab.jar .hidden\codelab\
    del codelab.jar
) else if exist "CodeLab-Win64\.hidden\codelab" (
    copy /Y codelab.jar CodeLab-Win64\.hidden\codelab\
    del codelab.jar
)

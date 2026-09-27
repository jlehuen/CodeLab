@echo off
cd /d "%~dp0"

:: Si aucun argument n'est fourni, lance CodeLab normalement
if "%~1"=="" (
    start "" "%~dp0codelab.exe"
    exit /b
)

:: Parametres optionnels disponibles :
::   codelab.bat --dir "C:\mon\chemin"  : specifie l'emplacement du dossier codelab.files
::   codelab.bat --load                 : force le telechargement de la derniere version
::   codelab.bat --restore              : force la reinitialisation du dossier codelab.files

"%~dp0codelab.exe" %*

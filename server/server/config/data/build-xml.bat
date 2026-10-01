@echo off
setlocal EnableDelayedExpansion

:: -----------------------------------------------------------------------------
:: CodeLab - Generateur de fragments XML utilisateurs pour Windows
:: Lance build-xml.py avec detection automatique de Python et support UTF-8.
:: -----------------------------------------------------------------------------

:: Activation du jeu de caracteres UTF-8 pour la console Windows
chcp 65001 >nul 2>&1

:: Positionnement dans le dossier du script
cd /d "%~dp0"

:: Detection du mode d'invocation (double-clic ou ligne de commande)
set "IS_INTERACTIVE=0"
if "%~1"=="" set "IS_INTERACTIVE=1"

:: Recherche du lanceur ou binaire Python
set "PY_BIN="
where py >nul 2>&1
if %errorlevel% equ 0 (
    set "PY_BIN=py -3"
) else (
    where python >nul 2>&1
    if %errorlevel% equ 0 (
        set "PY_BIN=python"
    ) else (
        where python3 >nul 2>&1
        if %errorlevel% equ 0 (
            set "PY_BIN=python3"
        )
    )
)

:: Erreur si Python n'est pas installe
if "%PY_BIN%"=="" (
    echo.
    echo =======================================================================
    echo [ERREUR] Python est introuvable sur ce systeme.
    echo.
    echo Veuillez installer Python depuis https://www.python.org/downloads/
    echo Pensez a bien cocher la case :
    echo   "Add python.exe to PATH"
    echo lors de l'installation.
    echo =======================================================================
    echo.
    pause
    exit /b 1
)

:: Execution du script Python avec les arguments passes
%PY_BIN% "%~dp0build-xml.py" %*
set "EXIT_CODE=%errorlevel%"

:: Maintien de la fenetre ouverte en cas de double-clic ou glisser-deposer Explorer
if "%IS_INTERACTIVE%"=="1" (
    echo.
    pause
) else (
    echo %cmdcmdline% | findstr /i /c:"cmd /c" >nul 2>&1
    if %errorlevel% equ 0 (
        echo.
        pause
    )
)

exit /b %EXIT_CODE%

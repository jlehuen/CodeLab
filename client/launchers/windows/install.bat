@echo off
setlocal
cd /d "%~dp0"

echo ---------------------------------------------------------
echo Installation du raccourci CodeLab pour Windows
echo ---------------------------------------------------------

:: Creation du raccourci natif .lnk sur le Bureau via PowerShell (SANS droit administrateur)
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$desktop = [Environment]::GetFolderPath('Desktop'); " ^
  "$s = $ws.CreateShortcut([System.IO.Path]::Combine($desktop, 'CodeLab.lnk')); " ^
  "$s.TargetPath = '%~dp0codelab.exe'; " ^
  "$s.WorkingDirectory = '%~dp0'; " ^
  "$s.IconLocation = '%~dp0codelab.ico'; " ^
  "$s.Description = 'CodeLab IDE & Simulators'; " ^
  "$s.Save()"

if %ERRORLEVEL% equ 0 (
    echo [OK] Raccourci CodeLab cree avec succes sur votre Bureau.
) else (
    echo [ERREUR] Impossible de creer le raccourci sur le Bureau.
)

echo ---------------------------------------------------------
echo Vous pouvez desormais lancer CodeLab depuis votre Bureau !
echo ---------------------------------------------------------
pause

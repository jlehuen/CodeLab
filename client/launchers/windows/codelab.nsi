; =========================================================================
; Script NSIS moderne pour CodeLab (Windows 64-bit)
; Packaging sans privilèges administrateur (Per-User / %LOCALAPPDATA%)
; =========================================================================

Unicode True
SetCompressor /SOLID lzma

!include "MUI2.nsh"
!include "FileFunc.nsh"

; Définitions par défaut si non fournies via la ligne de commande (-D)
!ifndef VERSION
  !define VERSION "1.4.1"
!endif

!ifndef BUILD
  !define BUILD "0000000000"
!endif

!ifndef PAYLOAD_DIR
  !define PAYLOAD_DIR "CodeLab-Win64"
!endif

!ifndef OUTPUT_EXE
  !define OUTPUT_EXE "CodeLab-Win64-${VERSION}-Setup.exe"
!endif

!ifndef ICON_FILE
  !define ICON_FILE "codelab.ico"
!endif

; Métadonnées générales
Name "CodeLab"
Caption "CodeLab ${VERSION} (build ${BUILD}) - Installation"
OutFile "${OUTPUT_EXE}"

; Installation moderne par utilisateur (aucun droit administrateur requis)
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\CodeLab"
InstallDirRegKey HKCU "Software\CodeLab" "InstallDir"

; Configuration visuelle MUI2
!define MUI_ABORTWARNING
!define MUI_ICON "${ICON_FILE}"
!define MUI_UNICON "${ICON_FILE}"

; Pages de l'installeur
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

; Option pour lancer l'application à la fin de l'installation
!define MUI_FINISHPAGE_RUN "$INSTDIR\codelab.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Lancer CodeLab maintenant"
!insertmacro MUI_PAGE_FINISH

; Pages du désinstalleur
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

; Langues (Français par défaut, Anglais en secours)
!insertmacro MUI_LANGUAGE "French"
!insertmacro MUI_LANGUAGE "English"

; =========================================================================
; Section principale d'installation
; =========================================================================
Section "CodeLab" SecCodeLab
    SectionIn RO

    SetOutPath "$INSTDIR"

    ; Copie des lanceurs et fichiers de premier niveau
    File "${PAYLOAD_DIR}\codelab.exe"
    File "${PAYLOAD_DIR}\codelab.bat"
    File "${PAYLOAD_DIR}\kill-codelab.bat"
    File "${PAYLOAD_DIR}\patch-codelab.bat"
    File "${PAYLOAD_DIR}\codelab.ico"
    File "${PAYLOAD_DIR}\README.TXT"

    ; Copie récursive de l'arborescence .hidden
    SetOutPath "$INSTDIR\.hidden"
    File /r "${PAYLOAD_DIR}\.hidden\*"

    ; Attributs Caché et Système sur le dossier technique .hidden
    SetFileAttributes "$INSTDIR\.hidden" HIDDEN|SYSTEM

    ; Génération du désinstalleur officiel
    SetOutPath "$INSTDIR"
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    ; Mémorisation de l'installation dans le registre utilisateur
    WriteRegStr HKCU "Software\CodeLab" "InstallDir" "$INSTDIR"
    WriteRegStr HKCU "Software\CodeLab" "Version" "${VERSION}"
    WriteRegStr HKCU "Software\CodeLab" "Build" "${BUILD}"

    ; Déclaration du désinstalleur dans "Paramètres > Applications installées"
    !define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\CodeLab"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayName" "CodeLab IDE & Simulators"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\codelab.ico"
    WriteRegStr HKCU "${UNINST_KEY}" "DisplayVersion" "${VERSION}"
    WriteRegStr HKCU "${UNINST_KEY}" "Publisher" "Jérôme Lehuen - Le Mans Université"
    WriteRegStr HKCU "${UNINST_KEY}" "URLInfoAbout" "https://codelab.univ-lemans.fr"
    WriteRegStr HKCU "${UNINST_KEY}" "HelpLink" "https://codelab.univ-lemans.fr"
    WriteRegStr HKCU "${UNINST_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegStr HKCU "${UNINST_KEY}" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoModify" 1
    WriteRegDWORD HKCU "${UNINST_KEY}" "NoRepair" 1

    ; Calcul automatique de la taille installée pour l'affichage Windows
    ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
    IntFmt $0 "0x%08X" $0
    WriteRegDWORD HKCU "${UNINST_KEY}" "EstimatedSize" "$0"

    ; Définir le répertoire de travail pour les raccourcis
    SetOutPath "$INSTDIR"

    ; Création du raccourci sur le Bureau
    CreateShortcut "$DESKTOP\CodeLab.lnk" "$INSTDIR\codelab.exe" "" "$INSTDIR\codelab.ico" 0 SW_SHOWNORMAL "" "CodeLab IDE & Simulators"

    ; Création du groupe et des raccourcis dans le Menu Démarrer
    CreateDirectory "$SMPROGRAMS\CodeLab"
    CreateShortcut "$SMPROGRAMS\CodeLab\CodeLab.lnk" "$INSTDIR\codelab.exe" "" "$INSTDIR\codelab.ico" 0 SW_SHOWNORMAL "" "CodeLab IDE & Simulators"
    CreateShortcut "$SMPROGRAMS\CodeLab\Désinstaller CodeLab.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
SectionEnd

; =========================================================================
; Section de désinstallation
; =========================================================================
Section "Uninstall"
    ; Suppression des raccourcis
    Delete "$DESKTOP\CodeLab.lnk"
    Delete "$SMPROGRAMS\CodeLab\CodeLab.lnk"
    Delete "$SMPROGRAMS\CodeLab\Désinstaller CodeLab.lnk"
    RMDir "$SMPROGRAMS\CodeLab"

    ; Suppression intégrale du répertoire d'installation
    RMDir /r "$INSTDIR"

    ; Nettoyage des clés de registre
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CodeLab"
    DeleteRegKey HKCU "Software\CodeLab"
SectionEnd

; #####################################################################
; ##  This file is part of the software CodeLab IDE and Simulators   ##
; ##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defglobal ?*LOCAL_PORT* = (__getVarEnv "INTERNAL_PORT"))

(deffunction request (?msg)
	(return (__requestUDP ?msg ?*LOCAL_PORT*)))

(deffunction getvarenv (?var)
	(return (__getVarEnv ?var)))

(deffunction version ()
	(return (__getVersion)))


; --------------------------------------------------------------
; Fonctions utilitaires sur les chaînes

(deffunction str2int (?str)
	(return (integer (string-to-field ?str))))

(deffunction str2float (?str)
	(return (float (string-to-field ?str))))

(deffunction str-trim (?str)
	(bind ?len (str-length ?str))
	(if (eq " " (sub-string 1 1 ?str)) then
		(return (str-trim (sub-string 2 ?len ?str)))
		else
		(if (eq " " (sub-string ?len ?len ?str)) then
			(return (str-trim (sub-string 1 (- ?len 1) ?str)))
			else (return ?str))))

(deffunction str-replace (?str ?c1 ?c2)
	(if (eq ?str "") then (return ?str)
	else
		(bind ?len (str-length ?str))
		(bind ?car (sub-string 1 1 ?str))
		(bind ?cdr (sub-string 2 ?len ?str))
		(if (eq ?car ?c1) then
			(return (str-cat ?c2 (str-replace ?cdr ?c1 ?c2)))
		else
			(return (str-cat ?car (str-replace ?cdr ?c1 ?c2))))))

; --------------------------------------------------------------
; Fonctions utilitaires sur les fichiers

(deffunction __exists (?path)
	(if (open ?path file)
		then (close file) (return TRUE)
		else (return FALSE)))

(deffunction __require (?lib)
	(bind ?CLIPSLIB (__getVarEnv "CLIPSLIB"))
	(bind ?lib (str-cat (str-replace ?lib "." "/") ".clp"))
	(bind ?path (str-cat ?CLIPSLIB "/" ?lib))
	(if (__exists ?path)
		then (load* ?path) (return TRUE)
		else (return FALSE)))

(deffunction __include (?file)
	(bind ?DIRECTORY (__getVarEnv "DIRECTORY"))
	(bind ?path (str-cat ?DIRECTORY "/" ?file))
	(if (__exists ?path)
		then (load* ?path) (return TRUE)
		else (return FALSE)))

; --------------------------------------------------------------
; Configuration du simulateur de robots NXT

(defglobal ?*SETBACKGROUND_CMD* = "module=MODULEROBOTICS&cmd=setBackground&name=%s")
(defglobal ?*SETROBOTCONFIG_CMD* = "module=MODULEROBOTICS&cmd=setRobotConfiguration&name=%s")

(deffunction setBackground (?name)
	(str2int (request (format nil ?*SETBACKGROUND_CMD* ?name))))

(deffunction setRobotConfiguration (?name)
	(str2int (request (format nil ?*SETROBOTCONFIG_CMD* ?name))))

; --------------------------------------------------------------
; Chargement des librairies et configurations

(progn
	(bind ?FILENAME (__getVarEnv "FILENAME"))
	(if (open ?FILENAME clipsfile) then
		(while TRUE
			(bind ?line (readline clipsfile))
			(if (eq ?line EOF) then (break))

			; Importation des librairies
			(bind ?pos (str-index "#requires " ?line))
			(if ?pos then
				(bind ?name (str-trim (sub-string (+ ?pos 10) (str-length ?line) ?line)))
				(if (not (__require ?name)) then
					(format werror "IOERROR: Library \"%s\" does not exist%n" ?name)))

			; Importation des fichiers
			(bind ?pos (str-index "#includes " ?line))
			(if ?pos then
				(bind ?name (str-trim (sub-string (+ ?pos 10) (str-length ?line) ?line)))
				(if (not (__include ?name)) then
					(format werror "IOERROR: File \"%s\" does not exist%n" ?name)))

			; Changement de fond
			(bind ?pos (str-index "#setBackground " ?line))
			(if ?pos then
				(bind ?name (str-trim (sub-string (+ ?pos 15) (str-length ?line) ?line)))
				(setBackground ?name))

			; Changement de robot
			(bind ?pos (str-index "#setRobotConfiguration " ?line))
			(if ?pos then
				(bind ?name (str-trim (sub-string (+ ?pos 23) (str-length ?line) ?line)))
				(setRobotConfiguration ?name)))

		(close source)
		else (format werror "IOERROR: File \"%s\" does not exist%n" ?FILENAME)))


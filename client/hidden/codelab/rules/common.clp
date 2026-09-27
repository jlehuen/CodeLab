; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defglobal ?*BASE* = (sym-cat (fetch BASE)))
(defglobal ?*LANG* = (sym-cat (fetch LANG)))

(defglobal ?*RED* = (get-member codelab.console.Console COLOR_ERROR))
(defglobal ?*ORANGE* = (get-member java.awt.Color ORANGE))

; ---------------------------------------------------------------------
; Fonctions utilitaires

(deffunction LABEL (?key)
	(call codelab.CodeLab LABEL ?key))

(deffunction printToConsole (?str ?color)
	(call codelab.CodeLab directPrint ?str ?color))

(deffunction printlnToConsole (?str ?color)
	(call codelab.CodeLab directPrintln ?str ?color))

(deffunction matches (?str ?regex)
	; Retourne un booléen
	(return (call codelab.CodeLabRete matches ?str ?regex)))

(deffunction groups (?str ?regex)
	; Retourne un multivalué de chaînes
	(return (call codelab.CodeLabRete groups ?str ?regex)))

(deffunction group_nth (?i ?str ?regex)
	; Retourne une chaîne de caractères
	(return (nth$ ?i (groups ?str ?regex))))

; ---------------------------------------------------------------------
; Base de faits initiaux

(deffacts initial-facts
	(nothing)
)

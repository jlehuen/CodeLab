; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(deffunction console (?str)
	(printlnToConsole ?str ?*RED*))

(defrule ERRC_MODULE
	(lang C)
	?fact1 <- (error_comp ?txt1)
	?fact2 <- (error_comp ?txt2)
	(test (neq ?fact1 ?fact2))
	(test (?txt1 contains "file not found"))
	(test (?txt2 contains "#include codelab"))
	=>
	(printlnToConsole (LABEL COMP_MODULE_ERROR) ?*RED*))

(defrule ERRC_UNDECLARED_IDENT
	(lang C)
	(error_comp ?txt)
	(test (?txt contains "use of undeclared identifier"))
	=>
	(bind ?lst1 (groups ?txt "(.*)(use of undeclared identifier .*)"))
	(bind ?lst2 (groups ?txt "(.*)__temp.c:(\\d+):(\\d+):(.*)identifier '(.*)'"))
	(console "--> Regardez les lignes en rouge dans la console...")
	(console (format nil "--> Le message d'erreur est : %s" (nth$ 2 ?lst1)))
	(console (format nil "--> Vous utilisez en ligne %s un identificateur '%s' que vous n'avez pas déclaré" (nth$ 2 ?lst2) (nth$ 5 ?lst2)))
	(console "--> Peut-être est-ce un oubli ou une faute de frappe ?"))

(defrule ERRC_COMPARING_FLOATS
	(lang C)
	(error_comp ?txt)
	(test (?txt contains "comparing floating point"))
	=>
	(bind ?lst (groups ?txt "(.*)__temp.c:(\\d+):(\\d+):(.*)(comparing .*)"))
	(console "--> Regardez les lignes en rouge dans la console...")
	(console (format nil "--> Le message d'avertissement est : %s" (nth$ 5 ?lst)))
	(console (format nil "--> Vous comparez en ligne %s deux nombres à virgule flottante avec == (égaux) ou != (différents)" (nth$ 2 ?lst)))
	(console "--> Cette pratique est risquée, vous pouvez consulter [[ces explications](https://floating-point-gui.de/errors/comparison/)] (cliquez sur le lien)"))


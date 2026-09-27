; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defrule ERROR_JAVA_MODULE
	(lang JAVA)
	?fact <- (error_comp ?msg)
	(test (matches ?msg "(.*)error: package codelab.(.*) does not exist"))
	=>
	(retract ?fact)
	(printlnToConsole (LABEL COMP_MODULE_ERROR) ?*RED*))

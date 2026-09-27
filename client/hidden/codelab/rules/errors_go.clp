; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defrule ERROR_GO_MODULE
	(lang GO)
	?fact <- (error_comp ?msg)
	(test (matches ?msg "(.*)cannot find package codelab/(.*)"))
	=>
	(retract ?fact)
	(printlnToConsole (LABEL COMP_MODULE_ERROR) ?*RED*))

; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

; http://alvarestech.com/temp/fuzzyjess/Jess60/docs/

; http://sametmax.com/tableau-de-reference-des-exceptions-en-python/
; String code = error_msg.substring(0, error_msg.indexOf(':'));

;(defrule FILE_ERROR
;	?error <- (error_exe ?msg)
;	(test (or
;		(?msg contains "FileNotFoundError")
;		(?msg contains "Could not load sound")
;		(?msg contains "no such file or directory")))
;	=>
;	(retract ?error)
;	(printError ?msg FILE_ERROR))

(defrule ERROR_PY_MODULE
	(lang PYTHON)
	?error <- (error_exe ?msg)
	(test (?msg contains "ModuleNotFoundError"))
	=>
	(retract ?error)
	(bind ?list (groups ?msg "(.*)No module named (.*)"))	
	(bind ?name (nth$ 2 ?list))
	(bind ?msg1 (format nil "Python ne retrouve pas le module %s" ?name))
	(bind ?msg2 "--> Peut-être avez-vous mal orthographié le nom du module")
	(bind ?msg3 "--> Peut-être avez-vous oublié d'installer un module Python")
	(bind ?msg4 "--> Peut-être avez-vous oublié de charger un plugin CodeLab")
	(printlnToConsole ?msg1 ?*ORANGE*)
	(printlnToConsole ?msg2 ?*ORANGE*)
	(printlnToConsole ?msg3 ?*ORANGE*)
	(printlnToConsole ?msg4 ?*ORANGE*))

; #####################################################################
; ## This file is part of the software CodeLab IDE and Simulators    ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

; http://sametmax.com/tableau-de-reference-des-exceptions-en-python/
; String code = error_msg.substring(0, error_msg.indexOf(':'));

(deffunction printError (?msg ?explication)
	(printlnToConsole (format nil "%s %s" (LABEL printError2) ?msg) ?*RED*)
	(printlnToConsole (format nil "%s %s" (LABEL printError3) (LABEL ?explication)) ?*RED*)
	(printlnToConsole (LABEL printError4) ?*RED*))

(defrule red_bloc
	(declare (salience 1000))
	?f1 <- (blocs)
	?f2 <- (instruction ?instr)
	=>
	(retract ?f1 ?f2)
	(printlnToConsole (LABEL printError0) ?*RED*)
	(printlnToConsole (format nil "%s %s" (LABEL printError1) ?instr) ?*RED*))

(defrule IO_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "Exception: No file named"))
	=>
	(retract ?error)
	(printError ?msg IO_ERROR))

(defrule AUDIO_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "Exception: Unsupported audio format"))
	=>
	(retract ?error)
	(printError ?msg AUDIO_ERROR)
	(printlnToConsole (LABEL AUDIO_FORMAT) ?*RED*))

(defrule EOL_ERROR
	(declare (salience 10)) ; Devant SYNTAX_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "SyntaxError: EOL while scanning string literal"))
	=>
	(retract ?error)
	(printError ?msg EOL_ERROR))

(defrule SYNTAX_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "SyntaxError"))
	=>
	(retract ?error)
	(printError ?msg SYNTAX_ERROR))

(defrule NULL_ERROR
	(declare (salience 10)) ; Devant NAME_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "NameError: name 'NULL' is not defined"))
	=>
	(retract ?error)
	(printError ?msg NULL_ERROR))

(defrule NAME_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "NameError"))
	=>
	(retract ?error)
	(printError ?msg NAME_ERROR))

(defrule ZERO_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "ZeroDivisionError"))
	=>
	(retract ?error)
	(printError ?msg ZERO_ERROR))

(defrule INDEX_ERROR
	?error <- (error_blocs ?msg)
	(test (?msg contains "IndexError"))
	=>
	(retract ?error)
	(printError ?msg INDEX_ERROR))

(defrule DEFAULT_MESSAGE
	(declare (salience -1000))
	?error <- (error_blocs ?msg)
	=>
	(retract ?error)
	(printError ?msg NO_EXPLANATION))

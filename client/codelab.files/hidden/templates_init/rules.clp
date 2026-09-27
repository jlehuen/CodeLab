;; Program _fileName_
;; Created by _author_ on _date_

(deffacts initial-facts
	(sentence "This is the first initial fact")
	(sentence "This is the second initial fact"))

(defrule initialisation
	(declare (salience 999)) ; First things to do
	=>
	(watch facts) ; View the evolution of the facts
	(watch rules) ; View the triggering of the rules
	(watch activations)) ; View the evolution of the agenda

(defrule ending
	(declare (salience -999)) ; Last things to do
	=>
	(printout t "-----------------------------" crlf)
	(printout t " Fact base" crlf)
	(printout t "-----------------------------" crlf)
	(facts))

(defrule rule-1
	(sentence ?str)
	=>
	(printout t "-----------------------------> " ?str crlf)
	(assert (done)))

(defrule rule-2
	(declare (salience 100))
	?p <- (done)
	=>
	(retract ?p)
	(printout t "-----------------------------> DONE!" crlf))

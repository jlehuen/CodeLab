;; Program _fileName_
;; Created by _author_ on _date_

#requires codelab.moduleGraphics.graph2D

(defrule only-rule
	=>
	(loop-for-count 4
		(printout t "go forward..." crlf)
		(turtleForward 100)
		(printout t "turn right..." crlf)
		(turtleTurnRight 90)))

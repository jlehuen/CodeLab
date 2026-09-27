;; Program _fileName_
;; Created by _author_ on _date_

#requires codelab.moduleRobotics.robotLego
#requires codelab.utils

#setBackground B01
#setRobotConfiguration NXT01

(defrule only-rule
	=>
	(motorOn ?*OUT_BC* 50)
	(waitFor 1000)
	(motorOff ?*OUT_BC*))

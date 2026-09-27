; #####################################################################
; ## This file is part of the software CodeLab IDE & Simulators      ##
; ## For any question about CodeLab e-mail to codelab@univ-lemans.fr ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defglobal ?*CLEARSCREEN_CMD* = "module=MODULEGRAPHICS&cmd=clearScreen")
(defglobal ?*SETCOLOR_CMD* = "module=MODULEGRAPHICS&cmd=setColor&color=%d")
(defglobal ?*SETWIDTH_CMD* = "module=MODULEGRAPHICS&cmd=setWidth&width=%d")
(defglobal ?*DRAWLINE_CMD* = "module=MODULEGRAPHICS&cmd=drawLine&x1=%d&y1=%d&x2=%d&y2=%d")
(defglobal ?*DRAWPOINT_CMD* = "module=MODULEGRAPHICS&cmd=drawPoint&x=%d&y=%d")
(defglobal ?*DRAWCIRCLE_CMD* = "module=MODULEGRAPHICS&cmd=drawCircle&x=%d&y=%d&r=%d")

(defglobal ?*TURTLEFORWARD_CMD* = "module=MODULEGRAPHICS&cmd=turtleForward&distance=%d")
(defglobal ?*TURTLETURNRIGHT_CMD* = "module=MODULEGRAPHICS&cmd=turtleTurnRight&angle=%d")
(defglobal ?*TURTLETURNLEFT_CMD* = "module=MODULEGRAPHICS&cmd=turtleTurnLeft&angle=%d")
(defglobal ?*TURTLEGOTO_CMD* = "module=MODULEGRAPHICS&cmd=turtleGoto&x=%d&y=%d&theta=%d")
(defglobal ?*TURTLESHOW_CMD* = "module=MODULEGRAPHICS&cmd=turtleShow")
(defglobal ?*TURTLEHIDE_CMD* = "module=MODULEGRAPHICS&cmd=turtleHide")
(defglobal ?*TURTLERESET_CMD* = "module=MODULEGRAPHICS&cmd=turtleReset")
(defglobal ?*TURTLEPENUP_CMD* = "module=MODULEGRAPHICS&cmd=turtlePenUp")
(defglobal ?*TURTLEPENDOWN_CMD* = "module=MODULEGRAPHICS&cmd=turtlePenDown")

(defglobal ?*GETCOLOR_CMD* = "module=MODULEGRAPHICS&cmd=getColor")
(defglobal ?*GETBRIGHTNESS_CMD* = "module=MODULEGRAPHICS&cmd=getBrightness")

(defglobal ?*COLOR_WHITE* = 0)
(defglobal ?*COLOR_BLACK* = 1)
(defglobal ?*COLOR_RED* = 2)
(defglobal ?*COLOR_GREEN* = 3)
(defglobal ?*COLOR_BLUE* = 4)
(defglobal ?*COLOR_CYAN* = 5)
(defglobal ?*COLOR_YELLOW* = 6)
(defglobal ?*COLOR_MAGENTA* = 7)
(defglobal ?*COLOR_ERROR* = 8)

(deffunction clearScreen ()
	(bind ?buffer (format nil ?*CLEARSCREEN_CMD*))
	(str2int (request ?buffer)))

(deffunction setColor (?color)
	(bind ?buffer (format nil ?*SETCOLOR_CMD* ?color))
	(str2int (request ?buffer)))

(deffunction setWidth (?width)
	(bind ?buffer (format nil ?*SETWIDTH_CMD* ?width))
	(str2int (request ?buffer)))
	
(deffunction drawPoint (?x ?y)
	(bind ?buffer (format nil ?*DRAWPOINT_CMD* ?x ?y))
	(str2int (request ?buffer)))

(deffunction drawLine (?x1 ?y1 ?x2 ?y2)
	(bind ?buffer (format nil ?*DRAWLINE_CMD* ?x1 ?y1 ?x2 ?y2))
	(str2int (request ?buffer)))

(deffunction drawCircle (?x ?y ?r)
	(bind ?buffer (format nil ?*DRAWCIRCLE_CMD* ?x ?y ?r))
	(str2int (request ?buffer)))

(deffunction turtleForward (?distance)
	(bind ?buffer (format nil ?*TURTLEFORWARD_CMD* ?distance))
	(str2int (request ?buffer)))

(deffunction turtleTurnRight (?angle)
	(bind ?buffer (format nil ?*TURTLETURNRIGHT_CMD* ?angle))
	(str2int (request ?buffer)))

(deffunction turtleTurnLeft (?angle)
	(bind ?buffer (format nil ?*TURTLETURNLEFT_CMD* ?angle))
	(str2int (request ?buffer)))

(deffunction turtleGoto (?x ?y ?theta)
	(bind ?buffer (format nil ?*TURTLEGOTO_CMD* ?x ?y ?theta))
	(str2int (request ?buffer)))

(deffunction turtleShow ()
	(bind ?buffer (format nil ?*TURTLESHOW_CMD*))
	(str2int (request ?buffer)))

(deffunction turtleHide ()
	(bind ?buffer (format nil ?*TURTLEHIDE_CMD*))
	(str2int (request ?buffer)))

(deffunction turtleReset ()
	(bind ?buffer (format nil ?*TURTLERESET_CMD*))
	(str2int (request ?buffer)))

(deffunction turtlePenUp ()
	(bind ?buffer (format nil ?*TURTLEPENUP_CMD*))
	(str2int (request ?buffer)))
	
(deffunction turtlePenDown ()
	(bind ?buffer (format nil ?*TURTLEPENDOWN_CMD*))
	(str2int (request ?buffer)))
	
(deffunction getColor ()
	(bind ?buffer (format nil ?*GETCOLOR_CMD*))
	(request buffer))

(deffunction getBrightness ()
	(bind ?buffer (format nil ?*GETBRIGHTNESS_CMD*))
	(str2int (request ?buffer)))

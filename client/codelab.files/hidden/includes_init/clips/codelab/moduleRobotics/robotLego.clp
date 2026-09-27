; #####################################################################
; ##  This file is part of the software CodeLab IDE and Simulators   ##
; ##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

(defglobal ?*MOTORON_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=motorOn&port=%d&power=%d")
(defglobal ?*MOTOROFF_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=motorOff&port=%d")
(defglobal ?*GETARRAY_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=getSensorArray&port=%d&index=%d")
(defglobal ?*GETSENSOR_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=getSensorValue&port=%d")
(defglobal ?*GETROTATION_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=getMotorRotationCount&port=%d")
(defglobal ?*RESETROTATION_CMD* = "module=MODULEROBOTICS&robot=lego&cmd=resetMotorRotationCount&port=%d")

(defglobal ?*OUT_A* = 0)
(defglobal ?*OUT_B* = 1)
(defglobal ?*OUT_C* = 2)
(defglobal ?*OUT_AB* = 3)
(defglobal ?*OUT_AC* = 4)
(defglobal ?*OUT_BC* = 5)
(defglobal ?*OUT_ABC* = 6)

(defglobal ?*IN_1* = 0)
(defglobal ?*IN_2* = 1)
(defglobal ?*IN_3* = 2)
(defglobal ?*IN_4* = 3)

(defglobal ?*COLOR_WHITE* = 0)
(defglobal ?*COLOR_BLACK* = 1)
(defglobal ?*COLOR_RED* = 2)
(defglobal ?*COLOR_GREEN* = 3)
(defglobal ?*COLOR_BLUE* = 4)
(defglobal ?*COLOR_CYAN* = 5)
(defglobal ?*COLOR_YELLOW* = 6)
(defglobal ?*COLOR_MAGENTA* = 7)
(defglobal ?*COLOR_ERROR* = 8)

(deffunction motorOn (?output ?power)
	(str2int (request (format nil ?*MOTORON_CMD* ?output ?power))))

(deffunction motorOff (?output)
	(str2int (request (format nil ?*MOTOROFF_CMD* ?output))))

(deffunction getSensorValue (?input)
	(str2int (request (format nil ?*GETSENSOR_CMD* ?input))))

(deffunction motorRotationCount (?output)
	(str2int (request (format nil ?*GETROTATION_CMD* ?output))))

(deffunction resetMotorRotationCount (?output)
	(str2int (request (format nil ?*RESETROTATION_CMD* ?output))))

(deffunction getSensorArray (?input)
	(bind ?liste (create$))
	(foreach ?i (create$ 0 1 2 3 4 5 6 7)
		(bind ?liste (create$ ?liste (str2int (request (format nil ?*GETARRAY_CMD* ?input ?i))))))
	(return ?liste))

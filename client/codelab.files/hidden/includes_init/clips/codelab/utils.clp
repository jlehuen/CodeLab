; #####################################################################
; ##  This file is part of the software CodeLab IDE and Simulators   ##
; ##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
; #####################################################################

;; DO NOT DELETE OR MODIFY THIS FILE !!

; --------------------------------------------------------------
; Numpad et Joystick

(defglobal ?*GETNUMPAD_CMD* = "module=INOUT&cmd=getNumpadValue")
(defglobal ?*GETJOYSTICKX_CMD* = "module=INOUT&cmd=getJoystickValueX")
(defglobal ?*GETJOYSTICKY_CMD* = "module=INOUT&cmd=getJoystickValueY")

(deffunction getNumpadValue ()
	(str2int (request ?*GETNUMPAD_CMD*)))

(deffunction getJoystickValueX ()
	(str2int (request ?*GETJOYSTICKX_CMD*)))

(deffunction getJoystickValueY ()
	(str2int (request ?*GETJOYSTICKY_CMD*)))

; --------------------------------------------------------------
; Gestionnaire d'évènements

(defglobal ?*HASNEXTEVENT_CMD* = "module=INOUT&cmd=hasNextEvent")
(defglobal ?*GETNEXTEVENT_CMD* = "module=INOUT&cmd=getNextEvent")
(defglobal ?*RESETQUEUE_CMD* = "module=INOUT&cmd=resetEventQueue")

(deffunction hasNextEvent ()
	(eq (request ?*HASNEXTEVENT_CMD*) "1"))

(deffunction getNextEvent ()
	(request ?*GETNEXTEVENT_CMD*))

(deffunction resetEventQueue ()
	(request ?*RESETQUEUE_CMD*))

; --------------------------------------------------------------
; Bibliothèque sonore

(defglobal ?*PLAYTONE_CMD* = "module=AUDIO&cmd=playTone&freq=%d&time=%d&ampl=%d")
(defglobal ?*PLAYTONEON_CMD* = "module=AUDIO&cmd=playToneOn&freq=%d&ampl=%d")
(defglobal ?*PLAYTONEOFF_CMD* = "module=AUDIO&cmd=playToneOff")
(defglobal ?*PLAYAUDIOFILE_CMD* = "module=AUDIO&cmd=playAudioFile&filename=%s")
(defglobal ?*LOOPAUDIOFILE_CMD* = "module=AUDIO&cmd=loopAudioFile&filename=%s")
(defglobal ?*STOPAUDIOPLAYER_CMD* = "module=AUDIO&cmd=stopAudioPlayer")

(defglobal ?*AMPL* = 25) ; Amplitude dans [0, 100]

(deffunction playTone (?freq ?time)
	(str2int (request (format nil ?*PLAYTONE_CMD* ?freq ?time ?*AMPL*))))

(deffunction playToneOn (?freq)
	(str2int (request (format nil ?*PLAYTONEON_CMD* ?freq ?*AMPL*))))

(deffunction playToneOff ()
	(str2int (request ?*PLAYTONEOFF_CMD*)))

(deffunction playAudioFile (?filename)
	(str2int (request (format nil ?*PLAYAUDIOFILE_CMD* ?filename))))

(deffunction loopAudioFile (?filename)
	(str2int (request (format nil ?*LOOPAUDIOFILE_CMD* ?filename))))

(deffunction stopAudioPlayer ()
	(str2int (request ?*STOPAUDIOPLAYER_CMD*)))

; --------------------------------------------------------------
; Temporisation

(deffunction waitFor (?ms) (__waitFor ?ms))
(deffunction systemTime () (__systemTime))

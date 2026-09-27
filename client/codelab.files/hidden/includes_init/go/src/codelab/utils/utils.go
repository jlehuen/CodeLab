// #####################################################################
// ##  This file is part of the software CodeLab IDE and Simulators   ##
// ##  Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
// #####################################################################

// DO NOT DELETE OR MODIFY THIS FILE !!

package utils

import (
	"codelab"
	"fmt"
)

const GETNUMPAD_CMD = "module=INOUT&cmd=getNumpadValue"
const GETJOYSTICKX_CMD = "module=INOUT&cmd=getJoystickValueX"
const GETJOYSTICKY_CMD = "module=INOUT&cmd=getJoystickValueY"

const HASNEXTEVENT_CMD = "module=INOUT&cmd=hasNextEvent"
const GETNEXTEVENT_CMD = "module=INOUT&cmd=getNextEvent"
const RESETQUEUE_CMD = "module=INOUT&cmd=resetEventQueue"

// --------------------------------------------------------------
// Numpad et Joystick

func GetNumpadValue() int {
	return codelab.RequestToi(GETNUMPAD_CMD)
}

func GetJoystickValueX() int {
	return codelab.RequestToi(GETJOYSTICKX_CMD)
}

func GetJoystickValueY() int {
	return codelab.RequestToi(GETJOYSTICKY_CMD)
}

// --------------------------------------------------------------
// Gestionnaire d"évènements

func HasNextEvent() int {
	return codelab.RequestToi(HASNEXTEVENT_CMD)
}

func GetNextEvent() string {
	return codelab.Request(GETNEXTEVENT_CMD)
}

func ResetEventQueue() int {
	return codelab.RequestToi(RESETQUEUE_CMD)
}

// --------------------------------------------------------------
// Bibliothèque sonore

const PLAYTONE_CMD = "module=AUDIO&cmd=playTone&freq=%d&time=%d&ampl=%d"
const PLAYTONEON_CMD = "module=AUDIO&cmd=playToneOn&freq=%d&ampl=%d"
const PLAYTONEOFF_CMD = "module=AUDIO&cmd=playToneOff"
const PLAYAUDIOFILE_CMD = "module=AUDIO&cmd=playAudioFile&filename=%s"
const LOOPAUDIOFILE_CMD = "module=AUDIO&cmd=loopAudioFile&filename=%s"
const STOPAUDIOPLAYER_CMD = "module=AUDIO&cmd=stopAudioPlayer"

const ampl = 25 // Amplitude dans [0, 100]

func PlayTone(freq int, time int) int {
	return codelab.RequestToi(fmt.Sprintf(PLAYTONE_CMD, freq, time, ampl))
}

func PlayToneOn(freq int) int {
	return codelab.RequestToi(fmt.Sprintf(PLAYTONEON_CMD, freq, ampl))
}

func PlayToneOff() int {
	return codelab.RequestToi(PLAYTONEOFF_CMD)
}

func PlayAudioFile(filename string) int {
	return codelab.RequestToi(fmt.Sprintf(PLAYAUDIOFILE_CMD, filename))
}

func LoopAudioFile(filename string) int {
	return codelab.RequestToi(fmt.Sprintf(LOOPAUDIOFILE_CMD, filename))
}

func StopAudioPlayer() int {
	return codelab.RequestToi(STOPAUDIOPLAYER_CMD)
}

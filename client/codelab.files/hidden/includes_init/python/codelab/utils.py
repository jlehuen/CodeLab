#####################################################################
##	This file is part of the software CodeLab IDE and Simulators   ##
##	Copyright © Jérôme Lehuen 2021 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################

## DO NOT DELETE OR MODIFY THIS FILE !!

from __init import *

import time

GETNUMPAD_CMD = 'module=INOUT&cmd=getNumpadValue'
GETJOYSTICKX_CMD = 'module=INOUT&cmd=getJoystickValueX'
GETJOYSTICKY_CMD = 'module=INOUT&cmd=getJoystickValueY'

HASNEXTEVENT_CMD = 'module=INOUT&cmd=hasNextEvent'
GETNEXTEVENT_CMD = 'module=INOUT&cmd=getNextEvent'
RESETQUEUE_CMD = 'module=INOUT&cmd=resetEventQueue'

# --------------------------------------------------------------
# Numpad et Joystick

def getNumpadValue():
	return int(request(GETNUMPAD_CMD))

def getJoystickValueX():
	return int(request(GETJOYSTICKX_CMD))

def getJoystickValueY():
	return int(request(GETJOYSTICKY_CMD))

# --------------------------------------------------------------
# Gestionnaire d'évènements

def hasNextEvent():
	return int(request(HASNEXTEVENT_CMD))

def getNextEvent():
	return request(GETNEXTEVENT_CMD)

def resetEventQueue():
	return int(request(RESETQUEUE_CMD))

# --------------------------------------------------------------
# Bibliothèque sonore

PLAYTONE_CMD = 'module=AUDIO&cmd=playTone&freq=%d&time=%d&ampl=%d'
PLAYTONEON_CMD = 'module=AUDIO&cmd=playToneOn&freq=%d&ampl=%d'
PLAYTONEOFF_CMD = 'module=AUDIO&cmd=playToneOff'
PLAYAUDIOFILE_CMD = 'module=AUDIO&cmd=playAudioFile&filename=%s'
LOOPAUDIOFILE_CMD = 'module=AUDIO&cmd=loopAudioFile&filename=%s'
STOPAUDIOPLAYER_CMD = 'module=AUDIO&cmd=stopAudioPlayer'

PLAYCHORD2_CMD = 'module=AUDIO&cmd=playChord2&f1=%d&f2=%d&time=%d&ampl=%d'

ampl = 25 # Amplitude dans [0, 100]

def playTone(freq, time):
	buffer = PLAYTONE_CMD % (freq, time, ampl)
	return int(request(buffer))

def playChord2(f1, f2, time):
	buffer = PLAYCHORD2_CMD % (f1, f2, time, ampl)
	return int(request(buffer))

def playToneOn(freq):
	buffer = PLAYTONEON_CMD % (freq, ampl)
	return int(request(buffer))

def playToneOff():
	return int(request(PLAYTONEOFF_CMD))

def playAudioFile(filename):
	buffer = PLAYAUDIOFILE_CMD % filename
	result = int(request(buffer))
	if result == -1: raise Exception('No file named %s' % filename)
	if result == -2: raise Exception('Unsupported audio format')
	return result

def loopAudioFile(filename):
	buffer = LOOPAUDIOFILE_CMD % filename
	result = int(request(buffer))
	if result == -1: raise Exception('No file named %s' % filename)
	if result == -2: raise Exception('Unsupported audio format')
	return result

def stopAudioPlayer():
	return int(request(STOPAUDIOPLAYER_CMD))

# --------------------------------------------------------------
# Temporisation et temps système

def waitFor(ms):
	time.sleep(ms / 1000)

def systemTime():
	return int(time.time() * 1000.0)

"""
def systemTime():
	return int(round(time.time() * 1000))
"""

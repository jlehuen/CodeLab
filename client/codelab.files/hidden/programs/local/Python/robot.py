##
## Program robot.py
##

from codelab.moduleRobotics.robotLego import *

setBackground('B02')
setRobotConfiguration('NXT02')

motorOn(OUT_BC, 50)
while not getSensorValue(IN_1): pass
motorOff(OUT_BC)

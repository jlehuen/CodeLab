## Program _fileName_
## Created by _author_ on _date_

from codelab.moduleRobotics.robotLego import *
from codelab.utils import *

setBackground('B01')
setRobotConfiguration('NXT01')

motorOn(OUT_BC, 50)
waitFor(1000)
motorOff(OUT_BC)

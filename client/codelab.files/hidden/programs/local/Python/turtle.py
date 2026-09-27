##
## Program turtle.py
##

from codelab.moduleGraphics.graph2D import *

setWidth(2)
setColor(COLOR_RED)
turtlePenDown()
turtleShow()

for i in range(0, 4):
	print('go forward...')
	turtleForward(100)
	print('turn right...')
	turtleTurnRight(90)

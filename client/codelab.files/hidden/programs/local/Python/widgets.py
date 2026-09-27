##
## Program widgets.py
##

import codelab.utils as codelab

while True:
	x = codelab.getJoystickValueX()
	y = codelab.getJoystickValueY()
	z = codelab.getNumpadValue()
	print("x=%i, y=%i, key=%i" % (x, y, z))
	codelab.waitFor(50)

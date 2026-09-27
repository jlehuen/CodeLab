##
## Program sinus.py
##

import numpy as np
import matplotlib.pyplot as plt

x = np.linspace(-np.pi, np.pi, 100)
y = np.sin(x)

plt.title('Sine function')
plt.plot(x, y)
plt.grid()
plt.show()

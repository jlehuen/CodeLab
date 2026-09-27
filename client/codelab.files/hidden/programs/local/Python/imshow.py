##
## Program imshow.py
##

# https://pypi.org/project/matplotlib/
# pip install -U matplotlib

import numpy as np
import matplotlib.pyplot as plt

lenna = np.load('./data/lenna.npy')
plt.imshow(lenna)
plt.show()

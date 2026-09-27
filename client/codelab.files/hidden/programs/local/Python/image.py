##
## Program window.py
##

import tkinter as tk
import os

filename = './data/board.png'

window = tk.Tk()
window.title('This is CodeLab')
window.resizable(False, False)

hello = tk.PhotoImage(file=filename)
label = tk.Label(window, image=hello)
label.pack()

window.mainloop()

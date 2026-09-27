## Program _fileName_
## Created by _author_ on _date_

import tkinter as tk
import os

CODELAB_FILES = os.getenv('CODELAB_FILES')
filename = CODELAB_FILES + '/userdata/media/hello.png'

window = tk.Tk()
window.title('Window Title')
window.resizable(False, False)
hello = tk.PhotoImage(file=filename)
label = tk.Label(window, image=hello)
label.pack()
window.mainloop()

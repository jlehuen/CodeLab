##
## Program window.py
##

import tkinter as tk

root = tk.Tk()
root.title('Tkinter Demo')
root.geometry('300x100+400+400')
root.resizable(False, False)
root.wm_attributes('-topmost', 1)

label = tk.Label(root, text='Hello World!', font='Helvetica 30', pady=60)
label.pack()

root.mainloop()

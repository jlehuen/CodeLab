##
## Program readfile.py
##

filename = './data/lorem.txt'

with open(filename, 'r') as fhand:
	for line in fhand:
		print(line.rstrip())

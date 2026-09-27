##
## Program writefile.py
##

filename = 'test.txt'

with open(filename, 'w') as fhand:
	fhand.write('This is the first line\n')
	fhand.write('This is the second line\n')

with open(filename, 'r') as fhand:
	for line in fhand:
		print(line.rstrip())

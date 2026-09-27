##
## Program hello.py
##

from sys import version_info

maj, min = version_info[0], version_info[1]
version = '%s.%s' % (maj, min)
print('Hello World from Python version', version)

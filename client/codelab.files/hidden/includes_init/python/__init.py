#####################################################################
##  This file is part of the software CodeLab IDE and Simulators   ##
##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
#####################################################################

## DO NOT DELETE OR MODIFY THIS FILE !!

import socket
import time
import sys
import os

sys.path.append(os.path.abspath('.'))

DELAY = 0.005 # Pour laisser le temps au serveur de ré-écouter
PORT = None # Initialisé avec une variable d'environnement

if PORT is None:
	PORT = int(os.getenv('INTERNAL_PORT'))

def UDPClient(msg):
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	sock.sendto(msg.encode(), ('localhost', PORT))
	response = sock.recvfrom(100)
	time.sleep(DELAY)
	return response[0].decode('utf8')

def TCPClient(msg):
	msg+='\n'
	sock = socket.socket()
	sock.connect(('localhost', SERVER_PORT))
	sock.sendall(msg.encode())
	data = sock.recv(100)
	sock.close()
	time.sleep(DELAY)
	return data.decode('utf8')

def request(msg):
	return UDPClient(msg)

// #####################################################################
// ##  This file is part of the software CodeLab IDE and Simulators   ##
// ##  Copyright © Jérôme Lehuen 2023 - Jerome.Lehuen@univ-lemans.fr  ##
// #####################################################################

// DO NOT DELETE OR MODIFY THIS FILE !!

package codelab

import (
	"os"
	"net"
	"time"
	"strconv"
)

const BUFSIZE = 1024
const DELAY = 5 // Pour laisser le temps au serveur de ré-écouter

func first[T, U any](val T, _ U) T {
	return val
}

var UDPSERVER string

func getServerInfo() string {
	if UDPSERVER == "" {
		UDPSERVER = "localhost:" + os.Getenv("INTERNAL_PORT")
	}
	return UDPSERVER
}

func Request(msg string) string {
	socket := first(net.Dial("udp", getServerInfo()))
	socket.Write([]byte(msg))
	buffer := make([]byte, BUFSIZE)
	size := first(socket.Read(buffer))
	time.Sleep(DELAY * time.Millisecond)
	return string(buffer[:size])
}

func RequestToi(msg string) int {
	return first(strconv.Atoi(Request(msg)))
}

func RequestTof(msg string) float64 {
	return first(strconv.ParseFloat(Request(msg), 8))
}

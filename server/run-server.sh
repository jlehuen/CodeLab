#!/bin/bash

PORT=7878

start_server () {
	cd server
	cargo run &
	echo Server on port $PORT started with PID $!
}

if PID=$(lsof -t -i TCP:$PORT)
then
	echo WARNING: A server is already running on port $PORT with the PID $PID
else
	start_server
fi

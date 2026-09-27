#!/bin/bash

PORT=9988

if kill $(lsof -t -i TCP:$PORT) 2>/dev/null
then
	echo Server on port $PORT was killed
else
	echo ERROR: No server running on port $PORT
fi

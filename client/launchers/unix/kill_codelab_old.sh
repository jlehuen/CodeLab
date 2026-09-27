#!/bin/bash

NAME=codelab.jar
PID=$(ps ax | grep $NAME | grep -v grep | awk '{ print $1 }')

if test -z "$PID"
then
      echo "No process $NAME to kill"
else
      kill -9 $PID
      echo "Process $NAME killed (pid $PID)"
fi

#!/bin/bash
pgrep -fl codelab.jar | cut -d " " -f1 | xargs kill
pkill -9 -fi codelab

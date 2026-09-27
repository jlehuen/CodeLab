#!/bin/bash
#pgrep -fl codelab | cut -d " " -f1 | xargs kill
pkill -9 -fi "codelab"

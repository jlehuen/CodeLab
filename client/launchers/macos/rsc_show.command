#!/bin/bash
cd $(dirname $0)
mv .hidden hidden
osascript -e 'tell application "Terminal" to close first window' & exit

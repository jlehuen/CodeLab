#!/bin/bash
cd $(dirname $0)
defaults write com.apple.Terminal NSQuitAlwaysKeepsWindows -bool false
rm -rf "$HOME/Desktop/CodeLab.app"

./build.sh
./distrib-test.sh

killall Finder
open -n "$HOME/Desktop/CodeLab.app"
killall Terminal

#!/bin/bash
BASE=$(pwd)

#export HTTP_PROXY="proxy.univ-lemans.fr:3128"
#export HTTPS_PROXY="proxy.univ-lemans.fr:3128"

echo 'Compiling server...'
cd $BASE/server
cargo build

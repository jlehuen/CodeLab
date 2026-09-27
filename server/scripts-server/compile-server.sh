#!/bin/bash
BASE=$(pwd)

echo 'Compiling server...'
cd $BASE/server
cargo build

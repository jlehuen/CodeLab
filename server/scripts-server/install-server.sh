#!/bin/bash
cd "$(dirname "$0")"

echo "Uncompressing the package..."
rm -rf server
tar --warning=no-unknown-keyword -xzf server.tgz

echo "Compiling the server..."
./compile_server.sh

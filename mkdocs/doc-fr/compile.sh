#!/bin/bash
cd "$(dirname "$0")"
ROOT="$(cd ../.. && pwd)"
TARGET="${TARGET:-$ROOT/www/www/$(basename $PWD)}"

mkdocs build
rm -rf "$TARGET"
mkdir -p "$TARGET"
cp -r site/* "$TARGET/"

cd "$TARGET"
echo "--> site deployed into $PWD"

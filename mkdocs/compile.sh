#!/bin/bash
cd $(dirname $0)
export BASE=$(pwd)

sh $BASE/doc-fr/compile.sh
sh $BASE/doc-en/compile.sh

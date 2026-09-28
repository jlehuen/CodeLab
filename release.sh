#!/bin/bash
set -e
exec "$(dirname "$0")/client/release.sh" "$@"

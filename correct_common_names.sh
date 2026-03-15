#!/bin/bash
# Correct Common Name values in spec-extractor JSON output files.
# Run from anywhere; script dir is used to find correct_common_names.py.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$SCRIPT_DIR/scripts/correct_common_names.py" "$@"

#!/bin/bash

find . -maxdepth 1 -type d | grep -v '^\.$' | while read d; do
    pushd "$d" >/dev/null
    if make >/dev/null; then
        if ./run.bash >/dev/null; then
            echo "$d passed"
        else
            echo "$d failed" >&2
        fi
    else
        echo "failed to make $d" >&2
    fi
    popd >/dev/null
done

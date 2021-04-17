#!/bin/bash

find . -maxdepth 1 -type d | grep -v '^\.$' | while read d; do
    pushd "$d"
    make clean
    popd 
done

#!/bin/bash

[[ ! -z "$(python3 ../../concolic/concolic.py config.json | grep 'Found error')" ]]

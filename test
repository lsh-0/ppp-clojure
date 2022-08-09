#!/bin/bash
set -e

thing=${1:-}

if [ ! -z "$thing" ]; then
    poly test brick:"$thing" :dev
fi

poly test :all :dev

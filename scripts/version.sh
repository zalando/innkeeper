#!/usr/bin/env bash

cat build.sbt | grep "^version[ ]*:=[ ]*" | cut -f2 -d\"
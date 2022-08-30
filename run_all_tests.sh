#!/usr/bin/env bash

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxMetaspaceSize=1G"

sbt --mem 4000 clean coverage test acceptance:test coverageOff coverageReport

unset SBT_OPTS

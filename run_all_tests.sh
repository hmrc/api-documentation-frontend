#!/usr/bin/env bash

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxMetaspaceSize=1G"

sbt clean coverage test acceptance:test coverageOff coverageReport
python dependencyReport.py

unset SBT_OPTS

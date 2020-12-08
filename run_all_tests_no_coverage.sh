#!/usr/bin/env bash

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxMetaspaceSize=1G"

sbt clean compile test acceptance:test

unset SBT_OPTS

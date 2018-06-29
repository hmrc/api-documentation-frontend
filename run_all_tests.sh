#!/usr/bin/env bash

sbt clean coverage test acceptance:test coverageOff coverageReport
python dependencyReport.py 

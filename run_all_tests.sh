#!/usr/bin/env bash

sbt clean coverage test acceptance:test sandbox:test coverageOff coverageReport
python dependencyReport.py 

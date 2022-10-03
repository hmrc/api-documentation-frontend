#!/usr/bin/env bash
sbt -Dbrowser=chrome -Daccessibility.test='true' acceptance:compile acceptance:test
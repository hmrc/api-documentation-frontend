#!/usr/bin/env bash

# See https://github.com/hmrc/accessibility-assessment#running-accessibility-assessment-tests-locally for details of preparation required
#
sbt -Dbrowser=chrome -Daccessibility.test='true' acceptance:compile acceptance:test
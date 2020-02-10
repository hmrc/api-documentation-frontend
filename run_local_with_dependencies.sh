#!/usr/bin/env bash

sm --start COMBINED_API_DEFINITION API_DEFINITION API_EXAMPLE_MICROSERVICE -f
sm --start ASSETS_FRONTEND -r 3.11.0 -f

./run_local.sh

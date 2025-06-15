#!/bin/bash
set -a
source docker/.env
set +a
tilt up

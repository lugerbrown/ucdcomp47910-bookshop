#!/bin/bash
# This script loads environment variables from docker/.env and runs the Spring Boot application

set -a
source docker/.env
set +a
./mvnw spring-boot:run 
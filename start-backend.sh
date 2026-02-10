#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Start the Spring Boot application
mvn spring-boot:run -Dmaven.test.skip=true

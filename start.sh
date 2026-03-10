#!/bin/bash

# Load environment variables from .env file
export $(cat .env | grep -v '^#' | xargs)

# Start Spring Boot application (skip tests)
mvn spring-boot:run -Dmaven.test.skip=true

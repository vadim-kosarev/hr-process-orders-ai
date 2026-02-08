#!/bin/bash

# Build script for Spring Boot application
# This script builds the application on the host and creates a Docker image

set -e

echo "Building Spring Boot application..."

# Build using gradle
./gradlew clean build -x test

echo "Application built successfully!"

# Get the JAR file path
JAR_FILE=$(find build/libs -name "*.jar" -type f)

if [ -z "$JAR_FILE" ]; then
    echo "Error: JAR file not found in build/libs directory"
    exit 1
fi

echo "Found JAR file: $JAR_FILE"

# Build Docker image
echo "Building Docker image 'orders-app:latest'..."
docker build -t orders-app:latest -f docker/Dockerfile.runtime .

echo "Docker image built successfully!"
echo "You can now run: docker-compose up -d"


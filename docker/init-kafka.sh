#!/bin/bash

# Kafka initialization script
# This script creates initial topics for the Orders Service

set -e

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if kafka-broker-api-versions --bootstrap-server localhost:9092 2>/dev/null; then
        echo "Kafka is ready!"
        break
    fi
    echo "Kafka not ready yet, waiting... ($((attempt + 1))/$max_attempts)"
    sleep 2
    attempt=$((attempt + 1))
done

if [ $attempt -eq $max_attempts ]; then
    echo "Failed to connect to Kafka after $max_attempts attempts"
    exit 1
fi

# Create topics
echo "Creating Kafka topics..."

kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic orders-events \
    --partitions 16 \
    --replication-factor 1 \
    --if-not-exists

kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic orders-commands \
    --partitions 16 \
    --replication-factor 1 \
    --if-not-exists

echo "Kafka initialization completed successfully!"
kafka-topics --list --bootstrap-server localhost:9092


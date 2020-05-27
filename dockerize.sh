#!/bin/bash
mvn clean package -DskipTests
docker build -f Dockerfile -t market4.0/execution_core_container_business .
docker-compose down
docker-compose up -d
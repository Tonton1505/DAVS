#!/usr/bin/env bash
set -euo pipefail
DOCKER_USER="${DOCKER_USER:-my-docker-id}"
docker build -t "${DOCKER_USER}/davs-backend:v1" ./backend-springboot
docker build -t "${DOCKER_USER}/davs-frontend:v1" ./frontend-react
docker tag "${DOCKER_USER}/davs-backend:v1" "${DOCKER_USER}/davs-backend:v2"
docker tag "${DOCKER_USER}/davs-frontend:v1" "${DOCKER_USER}/davs-frontend:v2"
docker push "${DOCKER_USER}/davs-backend:v1"
docker push "${DOCKER_USER}/davs-backend:v2"
docker push "${DOCKER_USER}/davs-frontend:v1"
docker push "${DOCKER_USER}/davs-frontend:v2"

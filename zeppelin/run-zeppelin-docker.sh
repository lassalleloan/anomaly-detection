#!/bin/bash
#
# run-zeppelin-docker.sh - Run Zeppelin container in Docker
#
#                          To stop the container, type `docker stop zeppelin`.

# After running this script point your browser to
# http://localhost:8080
# If port 8080 is already taken by another program, change the PORT
# variable below to another port.
PORT=8080

# Create directories for sharing data between the container and the
# host running the container.

mkdir -p logs      # Host directory for Zeppelin logs
mkdir -p notebooks # Host directory for Zeppelin notebooks
mkdir -p data      # Host directory for datasets

# For each shared directory we tell Docker to mount the directory
# inside the container with the -v option. We tell Zeppelin where the
# directories are by setting environment variables via the Docker -e
# option.

docker run \
       --name zeppelin \
       -p $PORT:8080 \
       --rm \
       -v "$PWD"/logs:/logs \
       -e ZEPPELIN_LOG_DIR='/logs' \
       -v "$PWD"/notebooks:/notebooks \
       -e ZEPPELIN_NOTEBOOK_DIR='/notebooks' \
       -v "$PWD"/data:/data \
       apache/zeppelin:0.7.3

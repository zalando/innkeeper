#!/usr/bin/env bash

CONTAINERS=$(docker ps --format="{{.ID}}")
if [ -n "${CONTAINERS}" ]; then
    echo "CONTAINERS"
    echo $CONTAINERS
    docker kill $CONTAINERS
    docker rm $CONTAINERS
fi

#!/usr/bin/env bash

CONTAINERS=$(docker ps --format="{{.ID}}")
echo "CONTAINERS"
echo $CONTAINERS
docker kill $CONTAINERS
docker rm $CONTAINERS

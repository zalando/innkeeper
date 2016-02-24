#!/usr/bin/env sh

sbt assembly
docker --version
docker-compose --version
docker-compose build
docker-compose up -d
docker-compose ps
sbt it:test
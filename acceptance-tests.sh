#!/usr/bin/env sh

docker run -d --name oauth -t danpersa/rusty-oauth:latest
docker run -d --name db -p 5433:5432 -e POSTGRES_PASSWORD= -e POSTGRES_USER=innkeepertest -t postgres:9.4
sbt assembly
docker build -t pierone.stups.zalan.do/pathfinder/innkeeper:latest-SNAPSHOT .
docker run -d --link oauth:oauth --link db:db -p 8080:8080 \
    -e INNKEEPER_ENV=acceptance-test -e SCHEMA_RECREATE=true \
    -t pierone.stups.zalan.do/pathfinder/innkeeper:latest-SNAPSHOT
sbt it:test

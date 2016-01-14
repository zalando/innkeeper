#!/usr/bin/env sh

docker run -d -h oauth --name oauth -t danpersa/rusty-oauth:latest
docker run -d -h postgres -e POSTGRES_PASSWORD= -e POSTGRES_USER=innkeepertest --name db -t postgres:9.4
sleep 3
docker run -d --link oauth:oauth --link db:db -p 8080:8080 \
    -e INNKEEPER_ENV=test -e SCHEMA_RECREATE=true \
    -t pierone.stups.zalan.do/pathfinder/innkeeper:latest-SNAPSHOT
sbt it:test

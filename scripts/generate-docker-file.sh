#!/usr/bin/env bash

echo "FROM registry.opensource.zalan.do/stups/openjdk:1.8.0-131-5" > Dockerfile
echo "MAINTAINER Team Pathfinder <team-pathfinder@zalando.de>" >> Dockerfile
echo "" >> Dockerfile
echo "EXPOSE 9080" >> Dockerfile
echo "" >> Dockerfile
echo "RUN mkdir -p /opt/innkeeper" >> Dockerfile
echo "" >> Dockerfile
echo "WORKDIR /opt/innkeeper" >> Dockerfile
echo "" >> Dockerfile
echo "ENTRYPOINT java \$(java-dynamic-memory-opts) \\" >> Dockerfile
echo "    -Dinnkeeper.env=\$INNKEEPER_ENV \\" >> Dockerfile
echo "    -server -jar innkeeper-assembly-$1.jar" >> Dockerfile
echo "" >> Dockerfile
echo "ADD target/scala-2.11/innkeeper-assembly-$1.jar /opt/innkeeper/" >> Dockerfile

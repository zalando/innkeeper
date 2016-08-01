#!/usr/bin/env bash

if [ "$1" != "-fast" ]; then
  echo "full build"

  sbt scapegoat
  echo

  sbt assembly
  echo

  docker --version
  docker-compose --version
  echo

  docker-compose build
fi

if [ "$1" == "-nat" ]; then
  echo "init nat"

  VBoxManage controlvm "default" natpf1 "tcp-port6767,tcp,,6767,,6767"
  VBoxManage controlvm "default" natpf1 "tcp-port6768,tcp,,6768,,6768"
  VBoxManage controlvm "default" natpf1 "tcp-port5433,tcp,,5433,,5433"
  VBoxManage controlvm "default" natpf1 "tcp-port9080,tcp,,9080,,9080"
fi

docker-compose up -d
docker-compose ps

echo "waiting for innkeeper to start"
until $(curl --output /dev/null --silent --head --fail http://localhost:9080/status); do
    printf '.'
    sleep 5
done
echo

sbt it:test

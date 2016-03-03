#!/usr/bin/env bash

if [ "$1" != "-fast" ]; then
  echo "full build"

  sbt assembly
  docker --version
  docker-compose --version
  docker-compose build
fi

if [ "$1" == "-nat" ]; then
  echo "init nat"

  VBoxManage controlvm "default" natpf1 "tcp-port6767,tcp,,6767,,6767"
  VBoxManage controlvm "default" natpf1 "tcp-port6768,tcp,,6768,,6768"
  VBoxManage controlvm "default" natpf1 "tcp-port5433,tcp,,5433,,5433"
  VBoxManage controlvm "default" natpf1 "tcp-port8080,tcp,,8080,,8080"
fi

docker-compose up -d
docker-compose ps
sbt it:test

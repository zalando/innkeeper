#!/usr/bin/env bash

IMAGE=${1:-zalando/innkeeper} # First script argument is an image name
VERSION=${2:-latest}          # Second script argument is an image version

source generate-scm-source.sh

SOURCE=${IMAGE}:${VERSION}

echo "building docker image: \"${SOURCE}\" ..."
docker build -t ${SOURCE} .

rm scm-source.json

echo
echo "Do not forget to push the newly created docker image:"
echo "docker push $SOURCE"

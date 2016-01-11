# Docker Details

## How to build the docker image

    docker build -t zalando/innkeeper:latest .

## How to run a docker image

    docker run -p 8080:8080 -e INNKEEPER_ENV=test -e SCHEMA_RECREATE=true zalando/innkeeper:latest
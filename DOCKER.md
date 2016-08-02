# Docker Details

## How to build the docker image

    ./docker-build.sh
    
It is possible to specify the docker image name and the version. Image name is the first script parameter and image version is the second script parameter. 
    
    ./docker-build.sh registry.opensource.zalan.do/pathfinder/innkeeper 1.0
    
If parameters are not specified then default values are used. Default image name: ```zalando/innkeeper```, default version is: ```latest```.    

## How to run a docker image

    docker run -p 9080:9080 -e INNKEEPER_ENV=test zalando/innkeeper:latest

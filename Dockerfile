FROM registry.opensource.zalan.do/stups/openjdk:8-26
MAINTAINER Team Pathfinder <team-pathfinder@zalando.de>

EXPOSE 8080

RUN mkdir -p /opt/innkeeper

ADD target/scala-2.11/innkeeper-assembly-0.3.1.jar /opt/innkeeper/

WORKDIR /opt/innkeeper

ENTRYPOINT java $(java-dynamic-memory-opts) \
                -Dinnkeeper.env=$INNKEEPER_ENV -D$INNKEEPER_ENV.schema.recreate=$SCHEMA_RECREATE \
                -server -jar innkeeper-assembly-0.3.1.jar

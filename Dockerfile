FROM zalando/openjdk:8u66-b17-1-3
MAINTAINER Team Pathfinder <team-pathfinder@zalando.de>

EXPOSE 8080 8080

RUN mkdir -p /opt/innkeeper

ADD target/scala-2.11/innkeeper-assembly-0.0.1.jar /opt/innkeeper/

WORKDIR /opt/innkeeper

ENTRYPOINT java $(java-dynamic-memory-opts) -Dinnkeeper.env=$INNKEEPER_ENV -D$(INNKEEPER_ENV).schema.recreate=$SCHEMA_RECREATE -server -jar innkeeper-assembly-0.0.1.jar

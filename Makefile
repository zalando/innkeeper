version=0.0.7

docker:
	sbt assembly
	./docker-build.sh pierone.stups.zalan.do/pathfinder/innkeeper $(version)

docker-push:
	docker push pierone.stups.zalan.do/pathfinder/innkeeper:$(version)

docker-run:
	docker run -p 8080:8080 \
	 -e INNKEEPER_ENV=dev -e SCHEMA_RECREATE=true \
	 -t pierone.stups.zalan.do/pathfinder/innkeeper:$(version)

docker-snapshot:
	sbt assembly
	./docker-build.sh pierone.stups.zalan.do/pathfinder/innkeeper latest-SNAPSHOT

docker-push-snapshot:
	docker push pierone.stups.zalan.do/pathfinder/innkeeper:latest-SNAPSHOT

docker-run-snapshot:
	docker run -p 8080:8080 \
	-e INNKEEPER_ENV=test -e SCHEMA_RECREATE=true \
	-t pierone.stups.zalan.do/pathfinder/innkeeper:latest-SNAPSHOT

test-db:
	docker run \
	-e POSTGRES_PASSWORD=innkeeper-test \
	-e POSTGRES_USER=innkeeper-test \
	-p 5433:5432 postgres:9.4

docker-remove:
	docker rm $(docker ps -q -f status=exited)
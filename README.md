# Innkeeper

Innkeeper is a simple route management API for [Skipper](https://github.com/zalando/skipper)

When a new instance of Skipper (configured to fetch the routes from Innkeeper) is started, it will connect to Innkeeper, ask for all the routes and initialize its own data structures.

Then, at every x minutes will ask innkeeper for the modified routes and update its internal data structures.

[![Build Status](https://travis-ci.org/zalando/innkeeper.svg)](https://travis-ci.org/zalando/innkeeper)

## Getting started

First, create your application.conf file. One way to do it is by using the sample one:

    cp src/main/resources/sample.application.conf src/main/resources/application.conf

Set the `oauth.url` with your OAuth provider url.

Innkeeper has three different OAuth scopes, configured in the application.conf file also. For more info, see the OAuth chapter.

Innkeeper requires a Postgres DB for operation. For local development, docker can be used to spawn a DB (see below the Postgres chapter).

To run Innkeeper, execute `sbt run`.

## Running the tests

To run the unit test suite, run `sbt test`.
To run the integration test suite, run `sbt it:test`.

### Inserting a new route manually

```bash    
curl -i -XPOST localhost:8080/routes -d '{
    "route": {
      "matcher": {
        "path_matcher": {
          "match": "/hello-*",
          "type": "REGEX"
        },
        "header_matchers": []
      },
      "filters": []
    },
    "activate_at": "2015-10-10T10:10:10",
    "description": "this is a route",
    "name": "THE_ROUTE"
    }' -H 'Content-Type: application/json' -H 'Authorization: Bearer oauth-token'
```

### Getting all routes

    curl http://localhost:8080/routes -H 'Authorization: Bearer oauth-token'
    
To see it streaming:

    curl -i --limit-rate 3000 http://localhost:8080/routes -H 'Authorization: oauth-token'

### Getting last modified routes

    curl -i http://localhost:8080/updated-routes/2015-08-21T15:23:05.731 -H 'Authorization: Bearer oauth-token'

# OAuth

A client can have different scopes when calling Innkeeper:

  - read -> the client is allowed to read the routes
  - writeStrict -> the client is allowed to create only routes with a full path matcher
  - writeRegex -> the client with this scope is allowed to create routes with a regex matcher

# Postgres

For localhost

    CREATE ROLE innkeeper superuser login createdb;
    ALTER ROLE innkeeper WITH PASSWORD 'innkeeper';

## Postgres via docker

It is possible to simply start a docker container with a postgres ready for innkeeper by running:

```bash
$ docker run -e POSTGRES_PASSWORD=innkeeper -e POSTGRES_USER=innkeeper -p 5432:5432 postgres:9.4
```

For the tests, a different DB is used:

```bash
$ docker run -e POSTGRES_PASSWORD= -e POSTGRES_USER=innkeepertest -p 5432:5432 postgres:9.4
```

For users of `boot2docker` or `docker-machine` it is also necessary to create a port forwarding.
Assuming the docker-machine is named `default` this can be achieved via:

```bash
$ VBoxManage controlvm "default" natpf1 "tcp-port5432,tcp,,5432,,5432"
$ VBoxManage controlvm "default" natpf1 "tcp-port5433,tcp,,5433,,5433"
$ VBoxManage controlvm "default" natpf1 "tcp-port8080,tcp,,8080,,8080"
```

### License

Copyright 2015 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

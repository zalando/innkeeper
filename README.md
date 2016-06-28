<p align="center"><img width="400" alt="Innkeeper" src="https://rawgithub.com/zalando/innkeeper/master/logo.svg"></p>

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
# create a path for that route
curl -i -XPOST localhost:9080/paths -d '{
    "uri": "/the-uri",
    "host_ids": [1, 2, 3, 4]
}' -H 'Content-Type: application/json' -H 'Authorization: Bearer oauth-token-with-write-scope'

# the response will look like this:
HTTP/1.1 200 OK
{
  "created_by": "user~1",
  "owned_by_team": "team1",
  "host_ids": [1, 2, 3, 4],
  "uri": "/the-uri",
  "id": 6,
  "created_at": "2016-05-30T15:01:48.018"
}

# create a route for that path

curl -i -XPOST localhost:9080/routes -d '{
  "name": "theRoute1",
  "description": "this is a route",
  "activate_at": "2015-10-10T10:10:10",
  "disable_at": "2016-11-11T11:11:11",
  "predicates": [{
    "name": "method",
    "args": [{
        "value": "GET",
        "type": "string"
      }]
  }],
  "path_id": 6,
  "uses_common_filters": true
}' -H 'Content-Type: application/json' -H 'Authorization: Bearer oauth-token-with-write-scope'
```

### Getting all routes

    curl http://localhost:9080/routes -H 'Authorization: Bearer oauth-token-with-read-scope'
    
To see it streaming:

    curl -i --limit-rate 3000 http://localhost:9080/routes -H 'Authorization: oauth-token'

### Getting last modified routes

    curl -i http://localhost:9080/updated-routes/2015-08-21T15:23:05.731 -H 'Authorization: Bearer oauth-token'

Here are more [examples](EXAMPLES.md)

## OAuth

A client can have different scopes when calling Innkeeper:

  - read -> the client is allowed to read the routes
  - write -> the client is allowed to create only routes with a full path matcher
  - admin -> the client with this scope is allowed to create routes with a regex matcher

## Postgres

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
$ docker run -e POSTGRES_PASSWORD= -e POSTGRES_USER=innkeepertest -p 5433:5432 postgres:9.4
```

For users of `boot2docker` or `docker-machine` it is also necessary to create a port forwarding.
Assuming the docker-machine is named `default` this can be achieved via:

```bash
$ VBoxManage controlvm "default" natpf1 "tcp-port6767,tcp,,6767,,6767"
$ VBoxManage controlvm "default" natpf1 "tcp-port6768,tcp,,6768,,6768"
$ VBoxManage controlvm "default" natpf1 "tcp-port5433,tcp,,5433,,5433"
$ VBoxManage controlvm "default" natpf1 "tcp-port9080,tcp,,9080,,9080"
```

## Acceptance Tests

For `docker-machine`, on the first run, use the `-nat` option, to set up the port forwarding.

Copy the `sample.application.conf` file to the `application.conf` file.

    cp src/main/resources/sample.application.conf src/main/resources/application.conf

To run the acceptance tests, use the `acceptance-tests.sh` script.

There is a '-fast' option, which will skip the building of the docker image.

## Client Script

For development time, it is possible to use scripts/ikc.sh to view/modify innkeeper data. It is simply a wrapper
around listed curl commands.

Common flags are:
- innkeeper host: -h innkeeper.example.org
- jq selector: -s '.endpoint'

The script supports the following operations:

### List routes

```bash
scripts/ikc.sh routes
```

### List the currently active routes

```bash
scripts/ikc.sh current-routes
```

### List the updated/deleted routes

```bash
scripts/ikc.sh updated-routes "2016-06-28T12:38:50"
```

### Create a new route, for path with id 42

```bash
scripts/ikc.sh mkroute -n route1 -p 42 -E https://www.example.org
```

##### Optional flags:

- predicates: -P '[{"name": "Traffic", "args": [{"value": 0.1, "type": "number"}]}]'
- filters: -F '[{"name": "status", "args": [{"value": 200, "type": "number"}]}]'

The possible argument types are:

- number
- string
- regexp

### Delete a route, with id 42

```bash
scripts/ikc.sh delete-route 42
```

### List the installed paths

```bash
scripts/ikc.sh paths
```

### List the installed hosts

```bash
scripts/ikc.sh hosts
```

### Create a new path

```bash
scripts/ikc.sh mkpath /foo/bar
```

### Update an existing path, with id 42

```bash
scripts/ikc.sh update-path 42
```

## License

Copyright 2015 Zalando SE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

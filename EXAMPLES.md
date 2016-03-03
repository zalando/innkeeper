```bash
curl -ik -XPOST https://innkeeper.pathfinder-staging.zalan.do./routes -d '{
    "route": {
      "matcher": {
        "path_matcher": {
          "match": "/hello-*_",
          "type": "REGEX"
        },
        "header_matchers": []
      },
      "predicates": [],
      "filters": []
    },
    "activate_at": "2015-10-10T10:10:10",
    "description": "this is a route",
    "name": "THE_ROUTE"
    }' -H 'Content-Type: application/json' -H 'Authorization: Bearer $token'

curl -ik -XDELETE https://innkeeper.pathfinder-staging.zalan.do./routes/1 -H 'Authorization: $token'

curl -vk https://innkeeper.pathfinder-staging.zalan.do./routes -H 'Authorization: Bearer $token'

curl -ik -XPOST https://innkeeper.pathfinder-staging.zalan.do./routes -d '{
  "route": {
    "matcher": {
      "host_matcher": "example.com",
      "path_matcher": {
        "match": "/hello-*",
        "type": "REGEX"
      },
      "method_matcher": "POST",
      "header_matchers": [{
        "name": "X-Host",
        "value": "www.*_",
        "type": "REGEX"
      }, {
        "name": "X-Port",
        "value": "8080",
        "type": "STRICT"
      }]
    },
    "predicates": [{
          "name": "somePredicate",
          "args": ["Hello", 123]
        }, {
          "name": "someOtherPredicate",
          "args": ["Hello", 123, "World"]
    }],
    "filters": [{
      "name": "someFilter",
      "args": ["Hello", 123]
    }, {
      "name": "someOtherFilter",
      "args": ["Hello", 123, "World"]
    }],
    "endpoint": "https://www.endpoint.com:8080/endpoint"
  },
  "activate_at": "2015-10-10T10:10:10",
  "description": "this is a route",
  "name": "THE_ROUTE"
}' -H 'Content-Type: application/json' -H 'Authorization: Bearer $token'
```
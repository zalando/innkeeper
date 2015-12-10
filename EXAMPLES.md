curl -i -XPOST localhost:8080/routes -d '{
  "name": "THE_ROUTE",
  "description": "this is a route",
  "activate_at": "2015-09-28T16:58:56.957",
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
        "value": "www.*",
        "type": "REGEX"
      }, {
        "name": "X-Port",
        "value": "8080",
        "type": "STRICT"
      }]
    },
    "filters": [{
      "name": "someFilter",
      "args": ["Hello", 123]
    }, {
      "name": "someOtherFilter",
      "args": ["Hello", 123, "World"]
    }],
    "endpoint": "https://www.endpoint.com:8080/endpoint"
  }
}' -H 'Content-Type: application/json' -H 'Authorization: oauth-token'
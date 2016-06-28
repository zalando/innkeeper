# Client script - ikc.sh

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

##### Full example:

```bash
scripts/ikc.sh mkroute -n route1 -p 42 -E https://www.example.org -P '[{
	"name": "Traffic", "args": [{
		"value": 0.1, "type": "number"
	}, {
		"value": "fck1", "type": "string"
	}]
}, {
	"name": "Host", "args": [{
		"value": "www.example.org", "type": "regexp"
	}]
}]' -F '[{
	"name": "status", "args": [{
		"value": 200, "type": "number"
	}]
}, {
	"name": "cookie", "args": [{
		"value": "test", "type": "string"
	}, {
		"value": "true", "type": "string"
	}]
}]'
```

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

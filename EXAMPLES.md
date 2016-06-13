```bash

curl -i -XDELETE http://localhost:9080/routes/1 -H 'Authorization: Bearer $token'

curl -v http://localhost:9080/routes -H 'Authorization: Bearer $token'

curl http://localhost:9080/current-routes -H 'Authorization: Bearer $token'
```

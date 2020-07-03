# akka-http-apm

Example integrating Elastic APM Java agent with akka-http.

_Note: Example does currently not work correctly, as the spans are not activated/deactivated at the right time/thread.

## How to run

```
docker-compose up -d

sbt run
```


## Run outside of sbt

```
sbt assembly

java -jar target/scala-2.13/akka-http-apm-assembly-0.1.0-SNAPSHOT.jar
```


You can override settings by adding e.g.:
`-Delastic.apm.service_name=my-cool-service -Delastic.apm.server_urls=http://localhost:8200 -Delastic.apm.log_level=DEBUG`


## Example API calls

`curl -X POST -H 'Content-Type: application/json' http://localhost:8080/users -d '{"name": "foo", "age": 30, "countryOfResidence": "Oman"}'`
`curl -X GET -H 'Content-Type: application/json' http://localhost:8080/users`

Go to http://localhost:5601/app/apm to see trace.

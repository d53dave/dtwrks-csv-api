# File Uploader Service

This Play! 2.5 Application is offers the file uploader service functionality for the Backend Java/Scala Developer Test.

While being an API, it offers tiny JavaScript SPA that shows the functionality. The server will serve the app at `/`, i.e. `localhost:9000/` with default settings.

# API Workflow

The basic workflow for uploading a file is to `POST` a form containing the file name (i.e. POST body should be `filename=<name>`) to the endpoint `/csv`.

This will return a JSON object of the form 
```JSON
{
"id": <UUID>,
"filename": <filename>,
"path": <path on server>
}
```

Using this `id`, do a `POST` request against `/csv/{id}/content` with raw data as the POST Body. 

After the successful upload of the file, an event will be emitted into the message broker (Kafka, in this case). 

For an overview over all the endpoints, see `conf/routes`.

# Configuration

The following parameters are configurable

```properties
storage.filesystem.basepath="/tmp/dtwrks-upload-test"

messagebroker.topic="dtwrks.test"
messagebroker.urls="localhost:9092"
```

The basepath prop is optional. If it is missing, the app will generate a random temporary folder and use it to store uploads.

# Running
Start the application using `sbt run` or build a docker image with `sbt docker` and spin it up with `docker run -i -t <hash>` (see [docker run documentation](https://docs.docker.com/engine/reference/run/) for more info).

If running in docker, make sure that the application can reach the address that Kafka is bound to (e.g. by specifying `--net="bridge"` and resolving the proper IP addresses, if everything is running on localhost)

# Tests
Run the test suite using `sbt test`

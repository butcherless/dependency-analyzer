# Dependency Analyzer

## Goals:

- Get the updates of the dependencies of a project.
- Verify [Semver](https://semver.org/) compliance.

## Run the standalone application

Execute the following commands:

    sbt "assembly" "dependencyList/toFile /tmp/dep-list.log -f"
    export DL_FILENAME=/tmp/dep-list.log
    export DL_EXCLUSIONS=com.cmartin.learn #comma separated
    java -jar application/target/scala-2.13/depAnalyzerApp.jar

## Run the application with Docker

Build the image

    sbt Docker/publishLocal 

Run the app

    sbt "dependencyList/toFile /tmp/dep-list.log -f"
    # TODO set environment variables
    docker run --rm --name depanalyzer \
      --env DL_FILENAME=/tmp/dep-list.log \
      --env DL_EXCLUSIONS=com.cmartin.learn \
      --volume "/tmp:/tmp" dependency-analyzer-app:1.0.0-SNAPSHOT
    

## Maven query examples

    http -v "https://search.maven.org/solrsearch/select?q=g:dev.zio%20AND%20a:zio_2.13&wt=json"

    http -v "https://search.maven.org/solrsearch/select?q=g:com.typesafe.akka%20AND%20a:akka-actor_2.13%20AND%20v:2.5.25%20AND%20p:jar&rows=1&wt=json"

## TODOes

- manage `java.io.FileNotFoundException`
- manage `429 Too Many Requests` HTTP status code

## Modules

The App has the following modules:

- FileManager: access to filesystem
- LogicManager: collection processing, filtering, statistics, regex, etc.
- HttpManager: HTTP client
- LoggingManager: logging app operations

Get dependencies using bash commands

```bash
 gw dep | \
 grep -e '([0-9a-z\.-]*):([a-z-]*):([a-zA-Z0-9\.-]*)' | \
 sed 's/[+\]---//g; s/|//g' | \
 awk '{print $1}' | \
 sort -u
```

**curl**

    curl -s "https://search.maven.org/solrsearch/select?q=g:dev.zio+AND+a:zio_2.13&wt=json" | jq

Regex

([0-9a-z.]+):([0-9a-z-]+):([0-9.]+)?.*

http://queirozf.com/entries/scala-regular-expressions-examples-reference

https://logback.qos.ch/manual/configuration.html

Obtener dependencias y generar un archivo

gw dep --configuration default > /tmp/gradle-deps-default

Lista de tareas previas al caso de uso:

- sbt dependencyList > sbt-dependencies.log
- regex para filtrar líneas con deps: `^[a-z]([a-z0-9-_\.]+:){2}([0-9A-Z-\.]+)`
- log con los positivos y negativos para verificar si la regex filtra correctamente
- split del GAV por el caracter ":" (dos puntos)
- añadir las deps a un Set para eliminar duplicados
- obtener del repository todas las dependencias para el par (G,A)
- obtener la última versión
- consultar con maven central para verificar si ha cambiado
- elaborar el report con las dependencias que tienen nueva versión

## Secuencia

- Se obtiene la colección de dependencias de un archivo de texto en el que cada línea de texto contiene un dependencia.
- Se realiza el parsing de cada línea mediante una `regex` y se obtiene una colección de resultados coincidentes o no.
- Se filtran los casos coincidentes de la colección anterior.
- Se filtran las dependencias que pertenecen a determinados `groupId'.
- Se consultan las versiones de la colección de dependencias final.
- Se obtiene una colección de resultados que contiene la dependencia _local_ y la dependencia _remota_.

## Links

- ZIO Error Management: https://www.youtube.com/watch?v=mGxcaQs3JWI
- Functional Concurrency in Scala with ZIO: https://www.youtube.com/watch?v=m5nas4Hndqo&t=
- Application Modules / ZLayer / Dependency injection
    - https://twitter.com/jdegoes/status/1462758239418867714
    - https://twitter.com/jdegoes/status/1463261876150849547
 

## Stream based solution - Kafka

https://github.com/zio/zio-kafka

Confluentinc version: `latest`

Kafka version: `3.4.1`

Start Kafka:

    docker-compose -f docker-kafka/docker-compose.yml -d

Stop Kafka

    docker-compose -f docker-kafka/docker-compose.yml down

Show Kafka logs

    docker-compose -f docker-kafka/docker-compose.yml logs -f

List topics:

    bin/kafka-topics.sh --bootstrap-server localhost:29092 --list

Consume topic random

    bin/kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic random --from-beginning
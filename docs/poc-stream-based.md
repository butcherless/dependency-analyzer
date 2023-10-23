# Stream based solution

- define ADT (domain + error)
- define main process flow
- define use cases
- add use case examples

## Domain concepts

**Dependency** software dependency defined by the [Maven artifact](https://maven.apache.org/repositories/artifacts.html)
format.

**DependencyLine** text line coming from a file or a topic with a candidate *Dependency* in the maven short
format `group:artifact:version`. It needs to be parsed. For instance:

    ch.qos.logback:logback-classic:1.4.8

## [Draft] From text line to dependency

- parse the text line
- case ok:
    - create a Dependency object `MavenDependency(g,a,v)`
    - retrieve remote dependency group and artifact
    - check if remote dependency is newer than project dependency
- case ko:
    - create an InvalidDependency object `InvalidDependency(line,parse-error)`
    - log the invalid dependency



## Kafka commands


Confluentinc version: `latest`

Kafka version: `3.4.1`

Start Kafka:

    docker-compose -f docker-kafka/docker-compose.yml up -d

Stop Kafka

    docker-compose -f docker-kafka/docker-compose.yml down

Show Kafka logs

    docker-compose -f docker-kafka/docker-compose.yml logs -f

List topics:

    bin/kafka-topics.sh --bootstrap-server localhost:29092 --list

Consume topic `dependency-line-topic`

    bin/kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic dependency-line-topic --from-beginning

Consume topic `dependency-line-topic`

    docker run -it --network=host edenhill/kcat:1.7.1 -b 192.168.1.133:29092 -t dependency-line-topic

Consume topic `dependency-line-topic`

    docker run -it --rm \
        --network=host edenhill/kcat:1.7.1 \
        -b 192.168.1.133:29092 \
        -C \
        -f '\nKey (%K bytes): %k\t\nValue (%S bytes): %s\n\Partition: %p\tOffset: %o\n--\n' \
        -t dependency-line-topic

## Links

- https://github.com/zio/zio-kafka
- https://kafka.apache.org/downloads
- https://github.com/edenhill/kcat
- https://www.baeldung.com/ops/kafka-list-topics

## Rendering source code examples

This is scala source code rendering

https://github.com/butcherless/dependency-analyzer/blob/8714c5fab749f9148ed2272c14a4dd66919166bc/application/src/main/scala/com/cmartin/utils/http/ZioHttpManager.scala?plain=1
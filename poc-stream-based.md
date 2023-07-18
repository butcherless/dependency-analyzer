# Stream based solution

- define ADT (domain + error)
- define main process flow
- define use cases
- add use case examples

## Domain concepts

**Dependency** software dependency defined by the [Maven artifact](https://maven.apache.org/repositories/artifacts.html) format.

**DependencyLine** text line coming from a file or a topic with a candidate *Dependency* in the maven short format `group:artifact:version`. It needs to be parsed. For instance:

    ch.qos.logback:logback-classic:1.4.8

## [Draft] From text line to dependency

- parse the text line
- case ok:
  - create a Dependency object `LocalDependency(g,a,v)`
- case ko: 
  - create an InvalidDependency object `InvalidDependency(line,parse-error)`
  - log the invalid dependency

## Links

- https://www.baeldung.com/ops/kafka-list-topics
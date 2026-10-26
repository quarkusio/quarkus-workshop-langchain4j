# Documentation

This directory contains the workshop documentation, built with [Roq](https://github.com/quarkiverse/quarkus-roq) and [Quarkus](https://quarkus.io/).

## Prerequisites

- Java 21 or higher
- Maven 3.8+ (or use the root `./mvnw` wrapper)

## Running the Documentation Locally

To start the documentation in development mode with live reload:

```bash
# From the docs directory:
mvn quarkus:dev
```

Or from the project root using the Maven wrapper:

```bash
./mvnw -f docs quarkus:dev
```

Once started, the documentation will be available at [http://localhost:8080](http://localhost:8080).

## Building the Documentation

To build the documentation:

```bash
# From the docs directory:
mvn clean package
```

Or from the project root:

```bash
./mvnw -f docs clean package
```

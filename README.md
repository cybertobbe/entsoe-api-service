# entsoe-api-service

A Spring Boot service that periodically fetches day-ahead electricity prices from the [ENTSO-E Transparency Platform](https://transparency.entsoe.eu/) and publishes them to an Apache Artemis (ActiveMQ) message queue for downstream consumption.

## Features

- Automatic price fetching on a configurable timer (default every 6 minutes)
- Manual fetch trigger via REST endpoint
- Publishes XML responses to an ActiveMQ queue (`entsoe.prices`)
- Monitoring via Spring Boot Admin and Jolokia (JMX)

## Tech Stack

- Java 21
- Spring Boot 3.5.11
- Apache Camel 4.18.0 (HTTP, Timer, JMS, Direct)
- Apache Artemis (ActiveMQ)
- Spring Boot Admin client

## Prerequisites

- Java 21
- A running Apache Artemis broker
- An [ENTSO-E API token](https://transparency.entsoe.eu/content/static_content/Static%20content/web%20api/Guide.html#_authentication_and_authorisation)

## Configuration

All settings are defined in `src/main/resources/application.properties`. The following values can be overridden via environment variables:

| Environment Variable              | Default                        | Description                                        |
|-----------------------------------|--------------------------------|----------------------------------------------------|
| `ENTSOE_API_TOKEN`                | *(see properties file)*        | ENTSO-E API security token                         |
| `ENTSOE_AREA`                     | `10Y1001A1001A47J`             | Bidding zone / area code (default: SE4)            |
| `ARTEMIS_USER`                    | `admin`                        | ActiveMQ broker username                           |
| `ARTEMIS_PASSWORD`                | `admin`                        | ActiveMQ broker password                           |
| `SPRING_SECURITY_USER_NAME`       | `user`                         | Spring Boot Admin client username                  |
| `SPRING_SECURITY_USER_PASSWORD`   | `yourpassword`                 | Spring Boot Admin client password                  |

Additional settings in `application.properties`:

```properties
server.port=8086
spring.artemis.broker-url=tcp://192.168.0.6:61616
spring.boot.admin.client.url=http://192.168.0.6:8085
```

## Building

```bash
./mvnw clean package
```

## Running

```bash
./mvnw spring-boot:run
```

Or with environment variables:

```bash
ENTSOE_API_TOKEN=your-token ENTSOE_AREA=10Y1001A1001A47J ./mvnw spring-boot:run
```

The service starts on port **8086**.

## API

### Manual Fetch

Trigger an immediate fetch of day-ahead prices:

```
GET http://localhost:8086/api/entsoe/fetch
```

Response: `Fetch triggered`

## How It Works

1. A Camel timer route fires every 360,000 ms (6 minutes) and queries the ENTSO-E API for day-ahead prices (document type `A44`) for the current day.
2. The XML response is logged and forwarded to the ActiveMQ queue **`entsoe.prices`**.
3. The `/api/entsoe/fetch` endpoint provides a manual trigger for the same flow via a Camel direct route.

## Monitoring

- **Spring Boot Actuator**: all endpoints exposed at `http://localhost:8086/actuator`
- **Spring Boot Admin**: registers with the admin server configured in `spring.boot.admin.client.url`
- **Jolokia**: JMX access available at `http://localhost:8086/actuator/jolokia`
- **Logs**: written to `/app/logs/application.log`

## Testing

```bash
./mvnw test
```

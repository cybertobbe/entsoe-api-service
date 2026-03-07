# entsoe-api-service

## Overview
- Spring Boot service that fetches ENTSO-E day-ahead prices and publishes the result to a message queue.
- Application port: `8086`.
- Main output queue: `entsoe.prices`.

## How It Works
- A scheduled Apache Camel route runs every day with cron: `0 15 13 * * ?`.
- The route builds a `periodStart` and `periodEnd` window from today to tomorrow (`yyyyMMdd0000`).
- It calls the ENTSO-E API endpoint with:
  - `documentType=A44`
  - `in_Domain` and `out_Domain` from the configured area code
  - API token from configuration
- The XML response body is sent to JMS queue `entsoe.prices`.

## Manual Trigger API
- Endpoint: `GET /api/entsoe/fetch`
- Effect: triggers the same fetch flow immediately through the Camel `direct:fetch-now` route.
- Response text: `Fetch triggered`.

## Configuration (Environment Variables)
- `ENTSOE_API_TOKEN`: ENTSO-E security token.
- `ENTSOE_AREA`: ENTSO-E area code (used for both input and output domain).
- `ARTEMIS_USER`: Artemis broker username.
- `ARTEMIS_PASSWORD`: Artemis broker password.

## Monitoring and Operations
- Spring Boot Actuator web endpoints are exposed.
- Health endpoint shows details.
- Jolokia endpoint is enabled.
- Spring Boot Admin client is configured.

## Logging
- Log file path: `/app/logs/application.log`.
- Actuator logfile endpoint points to the same file path.

Overview – fetches ENTSO-E day-ahead electricity prices (document type A44) once a day (00.10) via Apache Camel, publishes XML responses to ActiveMQ queue entsoe.prices<br>

Configuration – environment variable reference table (ENTSOE_API_TOKEN, ENTSOE_AREA, ARTEMIS_*, broker/admin URLs)<br>

REST API – GET /api/entsoe/fetch manual trigger
Monitoring – Actuator, Spring Boot Admin, Jolokia endpoints, log file path

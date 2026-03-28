package tech.sjostrom.entsoeapiservice;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class EntsoeRoute extends RouteBuilder {

    @Value("${entsoe.api.token}")
    private String apiToken;

    @Value("${entsoe.api.area}")
    private String areaCode;

    @Override
    public void configure() throws Exception {


            // Retry-strategi för HTTP-fel (503, 502, 500, etc.)
            onException(HttpOperationFailedException.class)
                    .maximumRedeliveries(5)               // Max 5 retries
                    .redeliveryDelay(300000)              // Wait 10 min between retries
                    .backOffMultiplier(1.5)               // Increase sleep time with 50% between each retry
                    .maximumRedeliveryDelay(1800000)      // Max 30 min between retries
                    .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN)
                    .retriesExhaustedLogLevel(org.apache.camel.LoggingLevel.ERROR)
                    .logRetryAttempted(true)
                    .logExhausted(true)
                    .logExhaustedMessageHistory(true)
                    .onWhen(exchange -> {
                        HttpOperationFailedException ex = exchange.getProperty(
                                org.apache.camel.Exchange.EXCEPTION_CAUGHT,
                                HttpOperationFailedException.class
                        );
                        // Retry on 5xx-fel (server-error) only
                        return ex != null && ex.getStatusCode() >= 500;
                    });

        //Trigger 13.15 every day
        from("quartz:entsoe/fetch-prices?cron=0+15+13+*+*+?")
                .routeId("entsoe-fetch-route")
                .process(exchange -> {
                    LocalDate today = LocalDate.now();
                    LocalDate tomorrow = today.plusDays(1);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

                    String periodStart = today.format(fmt) + "0000";
                    String periodEnd = tomorrow.format(fmt) + "0000";

                    exchange.setProperty("periodStart", periodStart);
                    exchange.setProperty("periodEnd", periodEnd);
                })
                .log("Fetching ENTSOE prices for ${exchangeProperty.periodStart} to ${exchangeProperty.periodEnd}")
                .setHeader("securityToken", constant(apiToken))
                .toD("https://web-api.tp.entsoe.eu/api?"
                        + "securityToken=${header.securityToken}"
                        + "&documentType=A44"
                        + "&in_Domain=" + areaCode
                        + "&out_Domain=" + areaCode
                        + "&periodStart=${exchangeProperty.periodStart}"
                        + "&periodEnd=${exchangeProperty.periodEnd}")
                .log("Received response from ENTSOE API: ${body}")
                .to("jms:queue:entsoe.prices")
                .log("Sent prices to ActiveMQ queue entsoe.prices");

        // Manual trigger endpoint
        from("direct:fetch-now")
                .routeId("entsoe-manual-fetch")
                .process(exchange -> {
                    LocalDate today = LocalDate.now();
                    LocalDate tomorrow = today.plusDays(1);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

                    String periodStart = today.format(fmt) + "0000";
                    String periodEnd = tomorrow.format(fmt) + "0000";

                    exchange.setProperty("periodStart", periodStart);
                    exchange.setProperty("periodEnd", periodEnd);
                })
                .log("Manual fetch: ENTSOE prices for ${exchangeProperty.periodStart} to ${exchangeProperty.periodEnd}")
                .setHeader("securityToken", constant(apiToken))
                .toD("https://web-api.tp.entsoe.eu/api?"
                        + "securityToken=${header.securityToken}"
                        + "&documentType=A44"
                        + "&in_Domain=" + areaCode
                        + "&out_Domain=" + areaCode
                        + "&periodStart=${exchangeProperty.periodStart}"
                        + "&periodEnd=${exchangeProperty.periodEnd}")
                .to("jms:queue:entsoe.prices")
                .log("Sent prices to ActiveMQ");
    }
}
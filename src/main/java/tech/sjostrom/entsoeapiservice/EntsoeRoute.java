package tech.sjostrom.entsoeapiservice;

import org.apache.camel.builder.RouteBuilder;
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

        // Fetch prices every 30 seconds (for testing - change to 3600000 for hourly)
        from("timer:fetch-prices?period=360000")
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
package io.renatofreire.transaction_manager.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    @Value("${exchange.api.url}")
    private String exchangeRateApiUrl;

    @Bean
    public RestClient createRestClient() {
        return RestClient.builder().baseUrl(exchangeRateApiUrl).build();
    }
}

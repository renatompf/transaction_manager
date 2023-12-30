package io.renatofreire.transaction_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.renatofreire.transaction_manager.dto.ExchangeRateAPIResponse;
import io.renatofreire.transaction_manager.exceptions.NoExchangeRateBetweenCurrenciesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExchangeRateService {

    @Value("${exchange.api.secret}")
    private String apiKey;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeRateService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);

    public ExchangeRateAPIResponse getExchangeRate(String baseCurrency) {
        return restClient.get()
                .uri("/{api_key}/latest/{base}", apiKey, baseCurrency)
                .exchange((restRequest, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new NoExchangeRateBetweenCurrenciesException(String.format("Currency %s not supported", baseCurrency));
                    } else {
                        return objectMapper.readValue(response.getBody(), ExchangeRateAPIResponse.class);
                    }
                });
    }

}

package io.renatofreire.transaction_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.renatofreire.transaction_manager.dto.ExchangeRateAPIResponse;
import io.renatofreire.transaction_manager.enums.Currencies;
import io.renatofreire.transaction_manager.exceptions.NoExchangeRateBetweenCurrenciesException;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExchangeRateServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${exchange.api.secret}")
    private String apiKey;

    public static MockWebServer server;
    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start(8088);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @BeforeAll
    static void beforeAll() {
        postgres.withInitScript("src/main/resources/db/migration/V1.0.0__init.sql");
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Test
    void connectionDB(){
        AssertionsForClassTypes.assertThat(postgres.isCreated()).isTrue();
        AssertionsForClassTypes.assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void itShould_GetExchangeRate() throws IOException {
        //Given
        String base = Currencies.USD.name();
        ExchangeRateAPIResponse exchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "https://example.com/docs",
                "https://example.com/terms",
                1640995200L,
                "2023-12-31 00:00:00",
                1641081600L,
                "2024-01-01 00:00:00",
                "USD",
                Map.of("EUR", 0.85, "GBP", 0.73)
        );

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/json"))
                .setBody(objectMapper.writeValueAsString(exchangeRateResponse))
        );
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + base);

        //When
        ExchangeRateAPIResponse exchangeRate = exchangeRateService.getExchangeRate(base);

        //Then
        assertThat(exchangeRate).usingRecursiveComparison().isEqualTo(exchangeRateResponse);
    }

    @Test
    void itShould_Not_GetExchangeRate() throws IOException {
        //Given
        String base = Currencies.USD.name();
        ExchangeRateAPIResponse exchangeRateResponse = new ExchangeRateAPIResponse(
                "error",
                "https://example.com/docs",
                "https://example.com/terms",
                null,
                null,
                null,
                null,
                null,
                null
        );

        server.enqueue(new MockResponse()
                .setHeaders(Headers.of("Content-Type", "application/json"))
                .setBody(objectMapper.writeValueAsString(exchangeRateResponse))
                .setResponseCode(400));

        HttpUrl baseUrl = server.url(apiKey + "/latest/" + base);

        //When

        //Then
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(base))
                .isInstanceOf(NoExchangeRateBetweenCurrenciesException.class)
                .hasMessageContaining("Currency " + base + " not supported");
    }
}
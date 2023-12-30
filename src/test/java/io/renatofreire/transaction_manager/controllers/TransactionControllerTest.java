package io.renatofreire.transaction_manager.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.renatofreire.transaction_manager.dto.*;
import io.renatofreire.transaction_manager.enums.Currencies;
import io.renatofreire.transaction_manager.repository.AccountRepository;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import io.renatofreire.transaction_manager.repository.TransactionRepository;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${exchange.api.secret}")
    private String apiKey;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

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

    @BeforeEach
    void clearDatabase() {
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void connectionDB(){
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void itShould_CreateNewTransaction() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

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
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        MvcResult transactionResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(amountToTransfer))
                .andReturn();

        TransactionDTO createdTransaction = objectMapper.readValue(transactionResult.getResponse().getContentAsString(), TransactionDTO.class);

        mockMvc.perform(get("/bank-accounts/{id}", fromBankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(fromBankAccountDTO.balance()-amountToTransfer));

        mockMvc.perform(get("/bank-accounts/{id}", toBankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(fromBankAccountDTO.balance()+(amountToTransfer*createdTransaction.exchangeRate())));

    }

    @Test
    void itShould_CreateNewTransaction_WithTheSameCurrency() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

        MvcResult transactionResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(amountToTransfer))
                .andReturn();

        TransactionDTO createdTransaction = objectMapper.readValue(transactionResult.getResponse().getContentAsString(), TransactionDTO.class);

        mockMvc.perform(get("/bank-accounts/{id}", fromBankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(fromBankAccountDTO.balance()-amountToTransfer));

        mockMvc.perform(get("/bank-accounts/{id}", toBankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(fromBankAccountDTO.balance()+(amountToTransfer*createdTransaction.exchangeRate())));
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfTransactionIsDoneToSameAccount() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();


        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), fromBankAccountDTO.id(), amountToTransfer);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Impossible to send money to same account"));
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfNotEnoughBalance() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 10000d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("There is no enough balance in the account to make the transaction"));
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfNotOneAccountIsNotPresent() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();


        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 10000d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), 2L, amountToTransfer);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(String.format("Account with id %s does not exist", fromAccountDTO.id())));
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfNoExchangeRateAPIResponse() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

        server.enqueue(new MockResponse().setResponseCode(400));

        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(String.format("Currency %s not supported", baseCurrency)));

    }

    @Test
    void itShould_Not_CreateNewTransaction_IfNoRatesMap() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

        ExchangeRateAPIResponse exchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "https://example.com/docs",
                "https://example.com/terms",
                1640995200L,
                "2023-12-31 00:00:00",
                1641081600L,
                "2024-01-01 00:00:00",
                "USD",
                null
        );

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/json"))
                .setBody(objectMapper.writeValueAsString(exchangeRateResponse))
        );
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(String.format("Exchange rate between %s and %s not found", baseCurrency, Currencies.EUR.name())));

    }

    @Test
    void itShould_Not_CreateNewTransaction_IfToCurrencyNotPresentInRatesMap() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

        ExchangeRateAPIResponse exchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "https://example.com/docs",
                "https://example.com/terms",
                1640995200L,
                "2023-12-31 00:00:00",
                1641081600L,
                "2024-01-01 00:00:00",
                "USD",
                Map.of("GBP", 0.73)
        );
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeaders(Headers.of("Content-Type", "application/json"))
                .setBody(objectMapper.writeValueAsString(exchangeRateResponse))
        );
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(String.format("Exchange rate between %s and %s not found", baseCurrency, Currencies.EUR.name())));

    }

    @Test
    void itShould_GetAllTransactions() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

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
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(amountToTransfer));

        mockMvc.perform(get("/transactions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    void itShould_GetAllTransactions_EmptyListIfNone() throws Exception {
        mockMvc.perform(get("/transactions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void itShould_GetTransactionById() throws Exception {
        //Create Accounts
        CreateAccountRequest createFromAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest createToAccountRequest = new CreateAccountRequest("Emily", "Johnson", "emilyjohnson@example.com", LocalDate.of(1988, 11, 7));

        MvcResult fromAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        MvcResult toAccountResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("emilyjohnson@example.com"))
                .andReturn();

        AccountDTO fromAccountDTO = objectMapper.readValue(fromAccountResult.getResponse().getContentAsString(), AccountDTO.class);
        AccountDTO toAccountDTO = objectMapper.readValue(toAccountResult.getResponse().getContentAsString(), AccountDTO.class);

        //Create Bank Accounts (One for each account)
        String baseCurrency = Currencies.USD.name();
        CreateBankAccountRequest createFromBankAccountRequest = new CreateBankAccountRequest(baseCurrency, 1000d, fromAccountDTO.id());
        CreateBankAccountRequest createToBankAccountRequest = new CreateBankAccountRequest(Currencies.EUR.name(), 1000d, toAccountDTO.id());

        MvcResult fromBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createFromBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(fromAccountDTO.id()))
                .andReturn();

        MvcResult toBankAccountResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createToBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(toAccountDTO.id()))
                .andReturn();

        BankAccountDTO fromBankAccountDTO = objectMapper.readValue(fromBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);
        BankAccountDTO toBankAccountDTO = objectMapper.readValue(toBankAccountResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // Create transaction request
        double amountToTransfer = 100d;
        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(fromBankAccountDTO.id(), toBankAccountDTO.id(), amountToTransfer);

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
        HttpUrl baseUrl = server.url(apiKey + "/latest/" + baseCurrency);

        MvcResult transactionResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createNewTransactionRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(amountToTransfer))
                .andReturn();

        TransactionDTO createdTransaction = objectMapper.readValue(transactionResult.getResponse().getContentAsString(), TransactionDTO.class);

        mockMvc.perform(get("/transactions/{id}", createdTransaction.id()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTransaction.id()));
    }

    @Test
    void itShould_Not_GetTransactionById_IfNoTransactionPresent() throws Exception {
        long transactionId = 1L;
        mockMvc.perform(get("/transactions/{id}", transactionId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(String.format("Transaction with id %s does not exist", transactionId)));
    }

    private String objectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            fail("Failed to convert object to json");
            return null;
        }
    }
}

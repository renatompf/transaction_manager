package io.renatofreire.transaction_manager.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.dto.BankAccountDTO;
import io.renatofreire.transaction_manager.dto.CreateAccountRequest;
import io.renatofreire.transaction_manager.dto.CreateBankAccountRequest;
import io.renatofreire.transaction_manager.repository.AccountRepository;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1");

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
        bankAccountRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void connectionDB(){
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void itShould_CreateBankAccount_Successfully() throws Exception {
        // Given
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult result = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDTO.class);

        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", 1000d, accountDTO.id());

        // When
        // Then
        mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(accountDTO.id()));

    }

    @Test
    void itShould_Not_CreateBankAccount_IfBalanceIsNegative() throws Exception {
        // Given

        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", -1000d, 1L);

        // When
        // Then
        mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Balance cannot be negative at the moment of account creation"));
    }

    @Test
    void itShould_Not_CreateBankAccount_IfCurrencyDoesNotExists() throws Exception {
        // Given
        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("RMF", 1000d, 1L);

        // When
        // Then
        mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The currency passed by the user does not exist"));
    }

    @Test
    void itShould_Not_CreateBankAccount_IfOwnerAccountNotPresent() throws Exception {
        // Given
        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", 1000d, 1000000L);

        // When
        // Then
        mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User with id " + 1000000L + " not found"));
    }

    @Test
    void itShould_ListAllBankAccounts_Successfully() throws Exception {
        // Given
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult result = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDTO.class);

        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", 1000d, accountDTO.id());

        mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(accountDTO.id()));

        // When
        // Then

        mockMvc.perform(get("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty());

    }

    @Test
    void itShould_ListNoBankAccounts_IfNone() throws Exception {
        // Given
        // When
        // Then
        mockMvc.perform(get("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

    }

    @Test
    void itShould_GetBankAccountById_Successfully() throws Exception {
        // Given
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult accountCreationResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(accountCreationResult.getResponse().getContentAsString(), AccountDTO.class);

        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", 1000d, accountDTO.id());

        MvcResult bankAccountCreationResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(accountDTO.id()))
                .andReturn();

        BankAccountDTO bankAccountDTO = objectMapper.readValue(bankAccountCreationResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // When
        // Then
        mockMvc.perform(get("/bank-accounts/{id}", bankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bankAccountDTO.id()));

    }

    @Test
    void itShould_Not_GetBankAccountById_IfNotPresent() throws Exception {
        // Given
        Long bankAccountId = 1L;

        // When
        // Then
        mockMvc.perform(get("/bank-accounts/{id}", bankAccountId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Bank account with id 1 does not exist"));

    }

    @Test
    void itShould_DeleteBankAccountById_Successfully() throws Exception {
        // Given
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        MvcResult accountCreationResult = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(accountCreationResult.getResponse().getContentAsString(), AccountDTO.class);

        CreateBankAccountRequest createBankAccountRequest = new CreateBankAccountRequest("EUR", 1000d, accountDTO.id());

        MvcResult bankAccountCreationResult = mockMvc.perform(post("/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(createBankAccountRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(accountDTO.id()))
                .andReturn();

        BankAccountDTO bankAccountDTO = objectMapper.readValue(bankAccountCreationResult.getResponse().getContentAsString(), BankAccountDTO.class);

        // When
        // Then
        mockMvc.perform(delete("/bank-accounts/{id}", bankAccountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

    }

    @Test
    void itShould_Not_DeleteBankAccountById_IfNotPresent() throws Exception {
        // Given
        int id = 100000;

        // When
        // Then
        mockMvc.perform(delete("/bank-accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Bank account with id " + id + " does not exist"));

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

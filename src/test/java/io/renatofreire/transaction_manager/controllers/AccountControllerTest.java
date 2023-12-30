package io.renatofreire.transaction_manager.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.dto.CreateAccountRequest;
import io.renatofreire.transaction_manager.repository.AccountRepository;
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
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

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
        accountRepository.deleteAll();
    }

    @Test
    void connectionDB(){
        assertThat(postgres.isCreated()).isTrue();
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    void itShould_CreateAccount_Successfully() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        // When
        // Then
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"));

    }

    @Test
    void itShould_Not_CreateAccount_IfFieldsAreMissing_ReturnBadRequest() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", null, LocalDate.of(1995, 5, 15));

        // When
        // Then
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Necessary data is missing to create a new account"));

    }

    @Test
    void itShould_Not_CreateAccount_IfFieldsEmailAlreadyTaken_ReturnBadRequest() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));
        CreateAccountRequest request2 = new CreateAccountRequest("John2", "Doe2", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        // When
        // Then
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request2))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Email is already taken"));
    }

    @Test
    void itShould_ListAllAccounts_Successfully() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        // When
        // Then
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty());

    }

    @Test
    void itShould_ListNoAccounts_IfNone() throws Exception {
        // Given
        // When
        // Then
        mockMvc.perform(get("/accounts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

    }

    @Test
    void itShould_GetAccountById_Successfully() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        // When
        // Then
        MvcResult result = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDTO.class);

        mockMvc.perform(get("/accounts/{id}", accountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(accountDTO.email()));

    }

    @Test
    void itShould_Not_GetAccountById_IfNotPresent() throws Exception {
        // Given
        int id = 100000;

        // When
        // Then
        mockMvc.perform(get("/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account with id " + id + " does not exist"));

    }

    @Test
    void itShould_DeleteAccountById_Successfully() throws Exception {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        // When
        // Then
        MvcResult result = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectToJson(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("johndoe@example.com"))
                .andReturn();

        AccountDTO accountDTO = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDTO.class);

        mockMvc.perform(delete("/accounts/{id}", accountDTO.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

    }

    @Test
    void itShould_Not_DeleteAccountById_IfNotPresent() throws Exception {
        // Given
        int id = 100000;

        // When
        // Then
        mockMvc.perform(delete("/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account with id " + id + " does not exist"));

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

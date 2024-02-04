package io.renatofreire.transaction_manager.service;

import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.dto.CreateAccountRequest;
import io.renatofreire.transaction_manager.enums.Currencies;
import io.renatofreire.transaction_manager.exceptions.EmailTakenException;
import io.renatofreire.transaction_manager.exceptions.NotPossibleCreateAccountException;
import io.renatofreire.transaction_manager.mapper.AccountMapper;
import io.renatofreire.transaction_manager.model.Account;
import io.renatofreire.transaction_manager.model.BankAccount;
import io.renatofreire.transaction_manager.repository.AccountRepository;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShould_CreateAccount() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        Account expected = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );

        // When
        when(accountRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(expected);

        AccountDTO createdAccount = accountService.createAccount(request);

        // Then
        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount).usingRecursiveComparison().isEqualTo(AccountMapper.INSTANCE.toDTOOut(expected));
    }


    @Test
    void itShould_Not_CreateAccount_IfEmailTaken() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", "Doe", "johndoe@example.com", LocalDate.of(1995, 5, 15));

        Account expected = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );

        // When
        when(accountRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(expected));

        // Then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(EmailTakenException.class)
                .hasMessageContaining("Email is already taken");
    }

    @Test
    void itShould_Not_CreateAccount_IfFieldsAreNotMissing() {
        // Given
        CreateAccountRequest request = new CreateAccountRequest("John", null, "johndoe@example.com", null);

        // When

        // Then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(NotPossibleCreateAccountException.class)
                .hasMessageContaining("Necessary data is missing to create a new account");
    }

    @Test
    void itShould_GetAccountById_IfExists() {
        // Given
        Long accountId = 1L;
        Account mockAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );

        // When
        when(accountRepository.findByIdAndDeletedIsFalse(accountId)).thenReturn(Optional.of(mockAccount));


        // Then
        AccountDTO accountDTO = accountService.getAccountById(accountId);

        assertThat(accountDTO).isNotNull();
        assertThat(accountDTO).usingRecursiveComparison().isEqualTo(AccountMapper.INSTANCE.toDTOOut(mockAccount));

    }

    @Test
    void itShould_Not_GetAccountById_IfNotPresent() {
        // Given
        Long nonExistingId = 2L;

        // When
        when(accountRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> accountService.getAccountById(nonExistingId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account with id 2 does not exist");
    }

    @Test
    void itShould_deleteAccountById_If_AccountExists() {
        // Given
        Long accountId = 1L;
        Account mockAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );

        BankAccount mockBankAccount = new BankAccount(mockAccount, Currencies.EUR, BigDecimal.valueOf(5000d));

        // When
        when(bankAccountRepository.findAllByOwnerId(accountId)).thenReturn(List.of(mockBankAccount));
        when(accountRepository.findByIdAndDeletedIsFalse(accountId)).thenReturn(Optional.of(mockAccount));

        // Then
        accountService.deleteAccountById(accountId);

        verify(accountRepository, times(1)).save(any());
        verify(bankAccountRepository, times(1)).saveAll(any());
    }

    @Test
    void itShould_Not_DeleteAccountById_If_AccountNotPresent() {
        // Given
        Long nonExistingId = 2L;

        // When
        when(accountRepository.findByIdAndDeletedIsFalse(nonExistingId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> accountService.deleteAccountById(nonExistingId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account with id 2 does not exist");

    }

}

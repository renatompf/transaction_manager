package io.renatofreire.transaction_manager.service;

import io.renatofreire.transaction_manager.dto.BankAccountDTO;
import io.renatofreire.transaction_manager.dto.CreateBankAccountRequest;
import io.renatofreire.transaction_manager.enums.Currencies;
import io.renatofreire.transaction_manager.exceptions.CurrencyDoesNotExistException;
import io.renatofreire.transaction_manager.exceptions.NotPossibleCreateBankAccountException;
import io.renatofreire.transaction_manager.mapper.BankAccountMapper;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class BankAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShould_CreateBankAccount() {
        // Given
        long ownerId = 1L;
        double balance = 10000d;

        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);

        CreateBankAccountRequest request = new CreateBankAccountRequest(Currencies.EUR.name(), balance, ownerId);
        BankAccount expected = new BankAccount(ownerAccount, Currencies.EUR, balance);

        // When
        when(accountRepository.findByIdAndDeletedIsFalse(anyLong())).thenReturn(Optional.of(ownerAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(expected);

        BankAccountDTO createdBankAccount = bankAccountService.createBankAccount(request);

        // Then
        assertThat(createdBankAccount).isNotNull();
        assertThat(createdBankAccount).usingRecursiveComparison().isEqualTo(BankAccountMapper.INSTANCE.toDTOOut(expected));
    }

    @Test
    void itShould_Not_CreateBankAccount_If_BalanceIsNegative() {
        // Given
        long ownerId = 1L;
        double balance = -100d;

        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);

        CreateBankAccountRequest request = new CreateBankAccountRequest(Currencies.EUR.name(), balance, ownerId);

        // When
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(ownerAccount));

        // Then
        assertThatThrownBy(() -> bankAccountService.createBankAccount(request))
                .isInstanceOf(NotPossibleCreateBankAccountException.class)
                .hasMessageContaining("Balance cannot be negative at the moment of account creation");
    }

    @Test
    void itShould_Not_CreateBankAccount_If_CurrencyDoesNotExist() {
        // Given
        long ownerId = 1L;
        double balance = 100d;

        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);

        CreateBankAccountRequest request = new CreateBankAccountRequest("RPF", balance, ownerId);

        // When
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(ownerAccount));

        // Then
        assertThatThrownBy(() -> bankAccountService.createBankAccount(request))
                .isInstanceOf(CurrencyDoesNotExistException.class)
                .hasMessageContaining("The currency passed by the user does not exist");
    }

    @Test
    void itShould_Not_CreateBankAccount_If_OwnerIdNotPresent() {
        // Given
        long ownerId = 1L;
        double balance = 10000d;

        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);

        CreateBankAccountRequest request = new CreateBankAccountRequest(Currencies.EUR.name(), balance, null);

        // When
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(ownerAccount));


        // Then
        assertThatThrownBy(() -> bankAccountService.createBankAccount(request))
                .isInstanceOf(NotPossibleCreateBankAccountException.class)
                .hasMessageContaining("Owner id is mandatory");
    }

    @Test
    void itShould_Not_CreateBankAccount_If_OwnerAccountNotFound() {
        // Given
        long ownerId = 1L;
        double balance = 10000d;

        CreateBankAccountRequest request = new CreateBankAccountRequest(Currencies.EUR.name(), balance, ownerId);

        // When
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> bankAccountService.createBankAccount(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User with id 1 not found");
    }

    @Test
    void itShould_GetBankAccountById() {
        // Given
        long accountId = 1L;
        long ownerId = 1L;
        double balance = 10000d;

        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);
        BankAccount expected = new BankAccount(ownerAccount, Currencies.EUR, balance);

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(accountId)).thenReturn(Optional.of(expected));

        BankAccountDTO bankAccountDTO = bankAccountService.getBankAccountById(accountId);

        // Then
        assertThat(bankAccountDTO).isNotNull();
        assertThat(bankAccountDTO).usingRecursiveComparison().isEqualTo(BankAccountMapper.INSTANCE.toDTOOut(expected));
    }

    @Test
    void itShould_Not_GetBankAccountById_IfNotPresent() {
        // Given
        long accountId = 1L;

        // When
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> bankAccountService.getBankAccountById(accountId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Bank account with id 1 does not exist");
    }

    @Test
    void itShould_DeleteBankAccount() {
        // Given
        long ownerId = 1L;
        long accountId = 1L;
        double balance = 10000d;
        Account ownerAccount = new Account(
                "John",
                "Doe",
                "johndoe@example.com",
                LocalDate.of(1995, 5, 15)
        );
        ownerAccount.setId(ownerId);
        BankAccount mockAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(accountId)).thenReturn(Optional.of(mockAccount));

        // Then
        bankAccountService.deleteAccountById(accountId);

        verify(bankAccountRepository, times(1)).save(any());
    }

    @Test
    void itShould_Not_DeleteBankAccount_IfNotPresent() {
        // Given
        long accountId = 1L;

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(accountId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> bankAccountService.getBankAccountById(accountId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Bank account with id 1 does not exist");

        verify(bankAccountRepository, never()).delete(any());
    }

}

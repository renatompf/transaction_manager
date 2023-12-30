package io.renatofreire.transaction_manager.service;

import io.renatofreire.transaction_manager.dto.CreateNewTransactionRequest;
import io.renatofreire.transaction_manager.dto.ExchangeRateAPIResponse;
import io.renatofreire.transaction_manager.dto.TransactionDTO;
import io.renatofreire.transaction_manager.enums.Currencies;
import io.renatofreire.transaction_manager.exceptions.ImpossibleToRealiseTransactionException;
import io.renatofreire.transaction_manager.exceptions.NoExchangeRateBetweenCurrenciesException;
import io.renatofreire.transaction_manager.mapper.TransactionMapper;
import io.renatofreire.transaction_manager.model.Account;
import io.renatofreire.transaction_manager.model.BankAccount;
import io.renatofreire.transaction_manager.model.Transaction;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import io.renatofreire.transaction_manager.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShould_GetTransactionById() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.USD, balance);

        Long transactionId = 1L;
        Transaction expected = new Transaction(fromBankAccount, toBankAccount, 100d, 0.01, ZonedDateTime.now());

        // When
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(expected));

        TransactionDTO transaction = transactionService.getTransactionById(transactionId);

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction).usingRecursiveComparison().isEqualTo(TransactionMapper.INSTANCE.toDTOOut(expected));
    }

    @Test
    void itShould_Not_GetTransactionById_IfNotPresent() {
        // Given
        Long transactionId = 1L;

        // When
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Transaction with id 1 does not exist");
    }

    @Test
    void itShould_CreateNewTransaction_DifferentCurrencies() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        fromBankAccount.setId(1L);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.USD, balance);
        toBankAccount.setId(2L);

        Transaction expected = new Transaction(fromBankAccount, toBankAccount, 100d, 0.82, ZonedDateTime.now());

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(1L, 2L, 100d);

        ExchangeRateAPIResponse mockedExchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "Some documentation",
                "Some terms of use",
                1643548800L,
                "2023-01-30 00:00:00",
                1643552400L,
                "2023-01-30 01:00:00",
                "EUR",
                Map.of("USD", 0.82, "GBP", 0.74, "JPY", 103.45)
        );

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(fromBankAccount));
        when(bankAccountRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(toBankAccount));
        when(exchangeRateService.getExchangeRate(anyString())).thenReturn(mockedExchangeRateResponse);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expected);

        TransactionDTO transaction = transactionService.createNewTransaction(createNewTransactionRequest);

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction).usingRecursiveComparison().isEqualTo(TransactionMapper.INSTANCE.toDTOOut(expected));
    }


    @Test
    void itShould_Not_CreateNewTransaction_DifferentCurrencies_IfResponseBodyDoesNotContainsRates() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        fromBankAccount.setId(1L);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.USD, balance);
        toBankAccount.setId(2L);

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(1L, 2L, 100d);

        ExchangeRateAPIResponse mockedExchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "Some documentation",
                "Some terms of use",
                1643548800L,
                "2023-01-30 00:00:00",
                1643552400L,
                "2023-01-30 01:00:00",
                "EUR",
                null
        );

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(fromBankAccount));
        when(bankAccountRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(toBankAccount));
        when(exchangeRateService.getExchangeRate(anyString())).thenReturn(mockedExchangeRateResponse);

        // Then
        assertThatThrownBy(() -> transactionService.createNewTransaction(createNewTransactionRequest))
                .isInstanceOf(NoExchangeRateBetweenCurrenciesException.class)
                .hasMessageContaining("Exchange rate between EUR and USD not found");
    }

    @Test
    void itShould_Not_CreateNewTransaction_DifferentCurrencies_IfRatesNotContainsToBankAccountCurrency() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        fromBankAccount.setId(1L);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.USD, balance);
        toBankAccount.setId(2L);

        Transaction expected = new Transaction(fromBankAccount, toBankAccount, 100d, 0.82, ZonedDateTime.now());

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(1L, 2L, 100d);

        ExchangeRateAPIResponse mockedExchangeRateResponse = new ExchangeRateAPIResponse(
                "success",
                "Some documentation",
                "Some terms of use",
                1643548800L,
                "2023-01-30 00:00:00",
                1643552400L,
                "2023-01-30 01:00:00",
                "EUR",
                Map.of( "GBP", 0.74, "JPY", 103.45)
        );

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(fromBankAccount));
        when(bankAccountRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(toBankAccount));
        when(exchangeRateService.getExchangeRate(anyString())).thenReturn(mockedExchangeRateResponse);

        // Then
        assertThatThrownBy(() -> transactionService.createNewTransaction(createNewTransactionRequest))
                .isInstanceOf(NoExchangeRateBetweenCurrenciesException.class)
                .hasMessageContaining("Exchange rate between EUR and USD not found");
    }

    @Test
    void itShould_CreateNewTransaction_SameCurrencies() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        fromBankAccount.setId(1L);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        toBankAccount.setId(2L);

        Transaction expected = new Transaction(fromBankAccount, toBankAccount, 100d, 0.82, ZonedDateTime.now());

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(1L, 2L, 100d);

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(fromBankAccount));
        when(bankAccountRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(toBankAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(expected);

        TransactionDTO transaction = transactionService.createNewTransaction(createNewTransactionRequest);

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction).usingRecursiveComparison().isEqualTo(TransactionMapper.INSTANCE.toDTOOut(expected));
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfNotEnoughBalance() {
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
        BankAccount fromBankAccount = new BankAccount(ownerAccount, Currencies.EUR, balance);
        fromBankAccount.setId(1L);
        BankAccount toBankAccount = new BankAccount(ownerAccount, Currencies.USD, balance);
        toBankAccount.setId(2L);

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(1L, 2L, 100000d);

        // When
        when(bankAccountRepository.findByIdAndDeletedIsFalse(1L)).thenReturn(Optional.of(fromBankAccount));
        when(bankAccountRepository.findByIdAndDeletedIsFalse(2L)).thenReturn(Optional.of(toBankAccount));

        // Then
        assertThatThrownBy(() -> transactionService.createNewTransaction(createNewTransactionRequest))
                .isInstanceOf(ImpossibleToRealiseTransactionException.class)
                .hasMessageContaining("There is no enough balance in the account to make the transaction");
    }

    @Test
    void itShould_Not_CreateNewTransaction_IfTransactionIsMadeToSameBankAccount() {
        // Given
        long bankAccountId = 1L;
        double balance = 1000d;

        CreateNewTransactionRequest createNewTransactionRequest = new CreateNewTransactionRequest(bankAccountId, bankAccountId, balance);

        // When
        // Then
        assertThatThrownBy(() -> transactionService.createNewTransaction(createNewTransactionRequest))
                .isInstanceOf(ImpossibleToRealiseTransactionException.class)
                .hasMessageContaining("Impossible to send money to same account");
    }

}

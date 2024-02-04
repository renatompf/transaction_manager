package io.renatofreire.transaction_manager.service;

import io.renatofreire.transaction_manager.dto.CreateNewTransactionRequest;
import io.renatofreire.transaction_manager.dto.ExchangeRateAPIResponse;
import io.renatofreire.transaction_manager.dto.TransactionDTO;
import io.renatofreire.transaction_manager.exceptions.ImpossibleToRealiseTransactionException;
import io.renatofreire.transaction_manager.exceptions.NoExchangeRateBetweenCurrenciesException;
import io.renatofreire.transaction_manager.mapper.TransactionMapper;
import io.renatofreire.transaction_manager.model.BankAccount;
import io.renatofreire.transaction_manager.model.Transaction;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import io.renatofreire.transaction_manager.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;

    private final TransactionRepository transactionRepository;

    private final ExchangeRateService exchangeRateService;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(BankAccountRepository bankAccountRepository, TransactionRepository transactionRepository, ExchangeRateService exchangeRateService) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
    }

    public Page<TransactionDTO> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);
        return transactions.map(TransactionMapper.INSTANCE::toDTOOut);
    }

    public TransactionDTO getTransactionById(Long id) {
        Transaction transactionById = transactionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(String.format("Transaction with id %s does not exist", id)));
        return TransactionMapper.INSTANCE.toDTOOut(transactionById);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionDTO createNewTransaction(CreateNewTransactionRequest request) {
        // Check if "from" and "to" are not the same bank account id
        if (request.from().equals(request.to())) {
            throw new ImpossibleToRealiseTransactionException("Impossible to send money to same account");
        }

        // Check if "from" and "to" accounts exist
        BankAccount fromBankAccount = bankAccountRepository.findByIdAndDeletedIsFalse(request.from()).orElseThrow(() -> new EntityNotFoundException(String.format("Account with id %s does not exist", request.from())));
        BankAccount toBankAccount = bankAccountRepository.findByIdAndDeletedIsFalse(request.to()).orElseThrow(() -> new EntityNotFoundException(String.format("Account with id %s does not exist", request.from())));

        // Check if "from" bank account has enough balance
        if (fromBankAccount.getBalance().subtract(request.amount()).signum() == -1) {
            throw new ImpossibleToRealiseTransactionException("There is no enough balance in the account to make the transaction");
        }

        BigDecimal exchangeRate;

        // If both accounts use the same currency, there's no need to check the exchange rate
        if (fromBankAccount.getCurrency().equals(toBankAccount.getCurrency())) {
            logger.info("The currency of this transaction is the same. No need to get it from the exchangerate API");
            exchangeRate = BigDecimal.valueOf(1.0);
        } else {
            logger.info("The currency of this transaction is not the same. Will get the exchange rate from exchangerate API");
            ExchangeRateAPIResponse exchangeRateApiResponse = exchangeRateService.getExchangeRate(fromBankAccount.getCurrency().name());

            if(exchangeRateApiResponse.rates() == null || exchangeRateApiResponse.rates().isEmpty()){
                throw new NoExchangeRateBetweenCurrenciesException(String.format("Exchange rate between %s and %s not found", fromBankAccount.getCurrency().name(), toBankAccount.getCurrency().name()));
            }

            Double exchangeRateResponse = exchangeRateApiResponse.rates().get(toBankAccount.getCurrency().name());
            exchangeRate = exchangeRateResponse != null ? BigDecimal.valueOf(exchangeRateResponse) : null;
            if(exchangeRate == null){
                throw new NoExchangeRateBetweenCurrenciesException(String.format("Exchange rate between %s and %s not found", fromBankAccount.getCurrency().name(), toBankAccount.getCurrency().name()));
            }

            logger.info("Exchange rate extracted. Value: {}", exchangeRate);
        }

        // Convert amount in request from "from" Currency into "to" currency
        BigDecimal valueToTransfer = request.amount().multiply(exchangeRate) ;

        logger.info("Executing transaction. Amount: {} with exchange rate of {}. Final value: {} ", request.amount(), exchangeRate, valueToTransfer);
        // Update balance in both accounts
        fromBankAccount.setBalance(fromBankAccount.getBalance().subtract(request.amount()));
        toBankAccount.setBalance(toBankAccount.getBalance().add(valueToTransfer));

        bankAccountRepository.saveAll(List.of(fromBankAccount, toBankAccount));

        //Create new transaction
        Transaction newTransaction = new Transaction(fromBankAccount, toBankAccount, request.amount(), exchangeRate, ZonedDateTime.now());
        Transaction savedNewTransaction = transactionRepository.save(newTransaction);
        logger.info("Transaction successfully made and saved");

        return TransactionMapper.INSTANCE.toDTOOut(savedNewTransaction);
    }

}

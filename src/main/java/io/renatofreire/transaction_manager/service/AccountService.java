package io.renatofreire.transaction_manager.service;

import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.dto.CreateAccountRequest;
import io.renatofreire.transaction_manager.exceptions.EmailTakenException;
import io.renatofreire.transaction_manager.exceptions.NotPossibleCreateAccountException;
import io.renatofreire.transaction_manager.mapper.AccountMapper;
import io.renatofreire.transaction_manager.model.Account;
import io.renatofreire.transaction_manager.model.BankAccount;
import io.renatofreire.transaction_manager.repository.AccountRepository;
import io.renatofreire.transaction_manager.repository.BankAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    private final BankAccountRepository bankAccountRepository;

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    public AccountService(AccountRepository accountRepository, BankAccountRepository bankAccountRepository) {
        this.accountRepository = accountRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public AccountDTO createAccount(CreateAccountRequest request){
        // Check if the balance is negative
        if (request.firstName() == null || request.lastName() == null || request.email() == null || request.dateOfBirth() == null) {
            throw new NotPossibleCreateAccountException("Necessary data is missing to create a new account");
        }

        // Check if email is already taken
        Optional<Account> ownerEmail = accountRepository.findByEmailIgnoreCase(request.email());
        if(ownerEmail.isPresent()){
            throw new EmailTakenException("Email is already taken");
        }

        // Create new Account
        Account newAccount = new Account(request.firstName(), request.lastName(), request.email(), request.dateOfBirth());
        Account savedNewAccount = accountRepository.save(newAccount);
        logger.info("New account created");

        return AccountMapper.INSTANCE.toDTOOut(savedNewAccount);
    }

    public Page<AccountDTO> getAllAccounts(Pageable pageable){
        Page<Account> accountList = accountRepository.findAllByDeletedIsFalse(pageable);
        return accountList.map(AccountMapper.INSTANCE::toDTOOut);
    }

    public AccountDTO getAccountById(Long id){
        Account accountById = accountRepository.findByIdAndDeletedIsFalse(id).orElseThrow(() -> new EntityNotFoundException(String.format("Account with id %s does not exist", id)));
        return AccountMapper.INSTANCE.toDTOOut(accountById);
    }

    public void deleteAccountById(Long id){
        Account accountById = accountRepository.findByIdAndDeletedIsFalse(id).orElseThrow(() -> new EntityNotFoundException(String.format("Account with id %s does not exist", id)));

        List<BankAccount> allBankAccountsByOwnerId = bankAccountRepository.findAllByOwnerId(accountById.getId());
        allBankAccountsByOwnerId.forEach(bankAccount -> bankAccount.setDeleted(true));

        bankAccountRepository.saveAll(allBankAccountsByOwnerId);

        accountById.setDeleted(true);
        accountRepository.save(accountById);

        logger.info("Account with id {} and respective bank accounts deleted", id);
    }

}

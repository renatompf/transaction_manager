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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;

    private static final Logger logger = LoggerFactory.getLogger(BankAccountService.class);

    public BankAccountService(BankAccountRepository bankAccountRepository, AccountRepository accountRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.accountRepository = accountRepository;
    }

    public BankAccountDTO createBankAccount(CreateBankAccountRequest request){
        // Check if the balance is negative
        if (request.balance() != null && request.balance() < 0) {
            throw new NotPossibleCreateBankAccountException("Balance cannot be negative at the moment of account creation");
        }

        // Check if the currency exists really
        Currencies currency = Currencies.currencyExists(request.currency());
        if(currency == null){
            throw new CurrencyDoesNotExistException("The currency passed by the user does not exist");
        }

        // Check if the owner's account exists
        if (request.ownerId() == null ) {
            throw new NotPossibleCreateBankAccountException("Owner id is mandatory");
        }

        Account ownerAccount = accountRepository.findByIdAndDeletedIsFalse(request.ownerId()).orElseThrow(() -> new EntityNotFoundException(String.format("User with id %s not found", request.ownerId())));

        BankAccount bankAccount = new BankAccount(ownerAccount, currency, request.balance() == null ? 0 : request.balance());

        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);

        logger.info("New bank account created");
        return BankAccountMapper.INSTANCE.toDTOOut(savedBankAccount);
    }

    public Page<BankAccountDTO> getAllBankAccount(Pageable pageable){
        Page<BankAccount> bankAccountList = bankAccountRepository.findAllByDeletedIsFalse(pageable);
        return bankAccountList.map(BankAccountMapper.INSTANCE::toDTOOut);
    }

    public BankAccountDTO getBankAccountById(Long id){
        BankAccount bankAccountById = bankAccountRepository.findByIdAndDeletedIsFalse(id).orElseThrow(() -> new EntityNotFoundException(String.format("Bank account with id %s does not exist", id)));
        return BankAccountMapper.INSTANCE.toDTOOut(bankAccountById);
    }

    public boolean deleteAccountById(Long id){
        BankAccount bankAccountById = bankAccountRepository.findByIdAndDeletedIsFalse(id).orElseThrow(() -> new EntityNotFoundException(String.format("Bank account with id %s does not exist", id)));

        bankAccountById.setDeleted(true);
        bankAccountRepository.save(bankAccountById);
        logger.info("Bank account with id {} deleted", id);
        return true;
    }

}

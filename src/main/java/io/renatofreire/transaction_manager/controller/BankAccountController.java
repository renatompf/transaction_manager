package io.renatofreire.transaction_manager.controller;

import io.renatofreire.transaction_manager.dto.BankAccountDTO;
import io.renatofreire.transaction_manager.dto.CreateBankAccountRequest;
import io.renatofreire.transaction_manager.service.BankAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<BankAccountDTO> createNewBankAccount(@RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createBankAccount(request));
    }

    @GetMapping
    public ResponseEntity<Page<BankAccountDTO>> getAllBankAccounts(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(bankAccountService.getAllBankAccount(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountDTO> getBankAccountById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(bankAccountService.getBankAccountById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteBankAccountById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(bankAccountService.deleteAccountById(id));
    }

}

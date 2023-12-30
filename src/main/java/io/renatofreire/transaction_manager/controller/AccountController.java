package io.renatofreire.transaction_manager.controller;

import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.dto.CreateAccountRequest;
import io.renatofreire.transaction_manager.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountDTO> createNewAccount(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping
    public ResponseEntity<Page<AccountDTO>> getAllAccounts(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(accountService.getAllAccounts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(accountService.getAccountById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccountById(@PathVariable("id") Long id) {
        accountService.deleteAccountById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}

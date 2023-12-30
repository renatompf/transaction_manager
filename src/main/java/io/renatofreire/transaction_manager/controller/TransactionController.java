package io.renatofreire.transaction_manager.controller;

import io.renatofreire.transaction_manager.dto.CreateNewTransactionRequest;
import io.renatofreire.transaction_manager.dto.TransactionDTO;
import io.renatofreire.transaction_manager.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createNewTransaction(@RequestBody CreateNewTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createNewTransaction(request));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDTO>> getAllTransactions(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.getAllTransactions(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionById(id));
    }

}

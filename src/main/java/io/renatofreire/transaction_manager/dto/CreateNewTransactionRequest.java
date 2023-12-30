package io.renatofreire.transaction_manager.dto;

public record CreateNewTransactionRequest(
        Long from,
        Long to,
        Double amount
) {
}

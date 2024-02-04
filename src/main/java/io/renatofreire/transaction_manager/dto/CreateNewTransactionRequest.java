package io.renatofreire.transaction_manager.dto;

import java.math.BigDecimal;

public record CreateNewTransactionRequest(
        Long from,
        Long to,
        BigDecimal amount
) {
}

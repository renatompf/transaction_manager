package io.renatofreire.transaction_manager.dto;

import java.time.ZonedDateTime;

public record TransactionDTO(
        Long id,
        AccountTransactionDTO from,
        AccountTransactionDTO to,
        Double amount,
        Double exchangeRate,
        ZonedDateTime timestamp
) {
}

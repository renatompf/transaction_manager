package io.renatofreire.transaction_manager.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record TransactionDTO(
        Long id,
        AccountTransactionDTO from,
        AccountTransactionDTO to,
        BigDecimal amount,
        BigDecimal exchangeRate,
        ZonedDateTime timestamp
) {
}

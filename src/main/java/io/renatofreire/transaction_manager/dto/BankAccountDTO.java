package io.renatofreire.transaction_manager.dto;

import java.math.BigDecimal;

public record BankAccountDTO(
        Long id,
        String currency,
        BigDecimal balance,
        Long ownerId
) {
}

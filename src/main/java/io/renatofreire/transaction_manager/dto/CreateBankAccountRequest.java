package io.renatofreire.transaction_manager.dto;

import java.math.BigDecimal;

public record CreateBankAccountRequest(
    String currency,
    BigDecimal balance,
    Long ownerId
) {
}

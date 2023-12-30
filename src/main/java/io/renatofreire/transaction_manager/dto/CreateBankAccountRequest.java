package io.renatofreire.transaction_manager.dto;

public record CreateBankAccountRequest(
    String currency,
    Double balance,
    Long ownerId
) {
}

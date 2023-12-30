package io.renatofreire.transaction_manager.dto;

public record BankAccountDTO(
        Long id,
        String currency,
        Double balance,
        Long ownerId
) {
}

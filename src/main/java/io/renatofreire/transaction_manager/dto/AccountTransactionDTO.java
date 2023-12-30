package io.renatofreire.transaction_manager.dto;

public record AccountTransactionDTO(
        Long accountId,
        String currency
) {

    @Override
    public Long accountId() {
        return accountId;
    }

    public String currency() {
        return currency;
    }
}

package io.renatofreire.transaction_manager.exceptions;

public class ImpossibleToRealiseTransactionException extends RuntimeException {
    public ImpossibleToRealiseTransactionException(String message) {
        super(message);
    }
}

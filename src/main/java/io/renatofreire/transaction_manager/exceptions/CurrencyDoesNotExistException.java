package io.renatofreire.transaction_manager.exceptions;

public class CurrencyDoesNotExistException extends RuntimeException {
    public CurrencyDoesNotExistException(String message) {
        super(message);
    }
}

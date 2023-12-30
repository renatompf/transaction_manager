package io.renatofreire.transaction_manager.exceptions;

public class NoExchangeRateBetweenCurrenciesException extends RuntimeException {
    public NoExchangeRateBetweenCurrenciesException(String message) {
        super(message);
    }
}

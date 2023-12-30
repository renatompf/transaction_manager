package io.renatofreire.transaction_manager.exceptions;

public class NotPossibleCreateAccountException extends RuntimeException {
    public NotPossibleCreateAccountException(String message) {
        super(message);
    }
}

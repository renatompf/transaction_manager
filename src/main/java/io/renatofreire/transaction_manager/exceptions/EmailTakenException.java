package io.renatofreire.transaction_manager.exceptions;

public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String message) {
        super(message);
    }
}

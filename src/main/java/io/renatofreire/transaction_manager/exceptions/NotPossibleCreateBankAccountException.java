package io.renatofreire.transaction_manager.exceptions;

public class NotPossibleCreateBankAccountException extends RuntimeException {
    public NotPossibleCreateBankAccountException(String exception) {
        super(exception);
    }
}

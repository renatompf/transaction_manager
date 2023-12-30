package io.renatofreire.transaction_manager.exceptionHandlers;

import io.renatofreire.transaction_manager.dto.ErrorDTO;
import io.renatofreire.transaction_manager.exceptions.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            NoExchangeRateBetweenCurrenciesException.class,
            NoSuchElementException.class,
            EntityNotFoundException.class
    })
    public ResponseEntity<ErrorDTO> handleNoExchangeRateBetweenCurrenciesException(Exception ex) {
        return new ResponseEntity<>(new ErrorDTO(ex.getMessage()), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler({
            ImpossibleToRealiseTransactionException.class,
            NotPossibleCreateAccountException.class,
            NotPossibleCreateBankAccountException.class,
            CurrencyDoesNotExistException.class
    })
    public ResponseEntity<ErrorDTO> handleImpossibleToRealiseTransactionException(Exception ex) {
        return new ResponseEntity<>(new ErrorDTO(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler({ EmailTakenException.class })
    public ResponseEntity<ErrorDTO> handleEmailTakenException(Exception ex) {
        return new ResponseEntity<>(new ErrorDTO(ex.getMessage()), HttpStatus.FORBIDDEN);
    }


}

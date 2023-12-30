package io.renatofreire.transaction_manager.mapper;

import io.renatofreire.transaction_manager.dto.AccountTransactionDTO;
import io.renatofreire.transaction_manager.dto.TransactionDTO;
import io.renatofreire.transaction_manager.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransactionMapper {

    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    default TransactionDTO toDTOOut(Transaction transaction) {
        return new TransactionDTO (
                transaction.getId(),
                new AccountTransactionDTO(transaction.getFromAccount().getId(), transaction.getFromAccount().getCurrency().getFullName()),
                new AccountTransactionDTO(transaction.getToAccount().getId(), transaction.getToAccount().getCurrency().getFullName()),
                transaction.getAmount(),
                transaction.getExchangeRate(),
                transaction.getTimestamp()
        );
    }

}


package io.renatofreire.transaction_manager.mapper;

import io.renatofreire.transaction_manager.dto.AccountDTO;
import io.renatofreire.transaction_manager.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AccountMapper {

    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    AccountDTO toDTOOut (Account account);
}

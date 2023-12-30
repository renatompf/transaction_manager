package io.renatofreire.transaction_manager.mapper;

import io.renatofreire.transaction_manager.dto.BankAccountDTO;
import io.renatofreire.transaction_manager.model.BankAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BankAccountMapper {

    BankAccountMapper INSTANCE = Mappers.getMapper(BankAccountMapper.class);

    @Mappings({
            @Mapping(source="owner.id", target="ownerId"),
            @Mapping(source="currency.fullName", target="currency"),
    })
    BankAccountDTO toDTOOut (BankAccount bankAccount);
}

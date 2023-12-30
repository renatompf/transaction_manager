package io.renatofreire.transaction_manager.dto;

import java.time.LocalDate;

public record AccountDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth
) {
}

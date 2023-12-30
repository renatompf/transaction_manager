package io.renatofreire.transaction_manager.dto;

import java.time.LocalDate;

public record CreateAccountRequest(
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth
) {
}

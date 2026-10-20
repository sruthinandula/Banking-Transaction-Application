package com.example.bank.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class AccountDtos {

    private AccountDtos() {}

    public record CreateAccountRequest(@NotBlank @Size(max = 100) String holderName) {}

    public record AccountResponse(String accountNumber, String holderName, BigDecimal balance, Instant createdAt) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getAccountNumber(), a.getHolderName(), a.getBalance(), a.getCreatedAt());
        }
    }
}

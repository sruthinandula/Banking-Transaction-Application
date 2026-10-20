package com.example.bank.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {}

    public record DepositRequest(
        @NotBlank String accountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {}

    public record WithdrawRequest(
        @NotBlank String accountNumber,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {}

    public record TransferRequest(
        @NotBlank String fromAccount,
        @NotBlank String toAccount,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount) {}

    public record TransactionResponse(
        Long id,
        String type,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String status,
        Instant createdAt,
        boolean idempotentReplay) {

        public static TransactionResponse from(BankTransaction t, boolean replay) {
            return new TransactionResponse(t.getId(), t.getType().name(), t.getFromAccount(), t.getToAccount(),
                t.getAmount(), t.getStatus().name(), t.getCreatedAt(), replay);
        }
    }

    public record PagedResponse<T>(List<T> content, int page, int size, long totalElements) {}
}

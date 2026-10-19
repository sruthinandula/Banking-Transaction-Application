package com.example.bank.exception;

public final class BankingExceptions {

    private BankingExceptions() {}

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String accountNumber) {
            super("Insufficient funds in account " + accountNumber);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class ForbiddenOperationException extends RuntimeException {
        public ForbiddenOperationException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    /** Same Idempotency-Key re-used with a different request body or by a different user */
    public static class IdempotencyConflictException extends RuntimeException {
        public IdempotencyConflictException(String message) {
            super(message);
        }
    }
}

package com.example.bank.transaction;

import com.example.bank.account.Account;
import com.example.bank.account.AccountRepository;
import com.example.bank.event.TransactionEvent;
import com.example.bank.exception.BankingExceptions.AccountNotFoundException;
import com.example.bank.exception.BankingExceptions.ForbiddenOperationException;
import com.example.bank.exception.BankingExceptions.IdempotencyConflictException;
import com.example.bank.exception.BankingExceptions.InvalidRequestException;
import com.example.bank.outbox.OutboxEvent;
import com.example.bank.outbox.OutboxRepository;
import com.example.bank.transaction.TransactionDtos.DepositRequest;
import com.example.bank.transaction.TransactionDtos.PagedResponse;
import com.example.bank.transaction.TransactionDtos.TransactionResponse;
import com.example.bank.transaction.TransactionDtos.TransferRequest;
import com.example.bank.transaction.TransactionDtos.WithdrawRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final int MAX_KEY_LENGTH = 100;

    private final BankTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(BankTransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              OutboxRepository outboxRepository,
                              ObjectMapper objectMapper,
                              TransactionTemplate transactionTemplate) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionResponse deposit(String username, String idempotencyKey, DepositRequest request) {
        validateKey(idempotencyKey);
        BigDecimal amount = normalize(request.amount());
        String fingerprint = fingerprint(TransactionType.DEPOSIT, username, null, request.accountNumber(), amount);

        return execute(idempotencyKey, fingerprint, () -> {
            BankTransaction tx = insert(idempotencyKey, fingerprint, TransactionType.DEPOSIT,
                null, request.accountNumber(), amount);
            Account account = lockOwned(request.accountNumber(), username);
            account.credit(amount);
            recordEvent(tx);
            return tx;
        });
    }

    public TransactionResponse withdraw(String username, String idempotencyKey, WithdrawRequest request) {
        validateKey(idempotencyKey);
        BigDecimal amount = normalize(request.amount());
        String fingerprint = fingerprint(TransactionType.WITHDRAWAL, username, request.accountNumber(), null, amount);

        return execute(idempotencyKey, fingerprint, () -> {
            BankTransaction tx = insert(idempotencyKey, fingerprint, TransactionType.WITHDRAWAL,
                request.accountNumber(), null, amount);
            Account account = lockOwned(request.accountNumber(), username);
            account.debit(amount); // throws InsufficientFundsException -> whole transaction rolls back
            recordEvent(tx);
            return tx;
        });
    }

    public TransactionResponse transfer(String username, String idempotencyKey, TransferRequest request) {
        validateKey(idempotencyKey);
        String from = request.fromAccount();
        String to = request.toAccount();
        if (from.equals(to)) {
            throw new InvalidRequestException("Cannot transfer to the same account");
        }
        BigDecimal amount = normalize(request.amount());
        String fingerprint = fingerprint(TransactionType.TRANSFER, username, from, to, amount);

        return execute(idempotencyKey, fingerprint, () -> {
            BankTransaction tx = insert(idempotencyKey, fingerprint, TransactionType.TRANSFER, from, to, amount);

            Map<String, Account> locked = accountRepository.lockByAccountNumbers(List.of(from, to)).stream()
                .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));
            Account source = locked.get(from);
            Account target = locked.get(to);
            if (source == null) throw new AccountNotFoundException(from);
            if (target == null) throw new AccountNotFoundException(to);
            if (!source.getOwnerUsername().equals(username)) {
                throw new ForbiddenOperationException("You do not own account " + from);
            }

            source.debit(amount);
            target.credit(amount);
            recordEvent(tx);
            return tx;
        });
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> history(String username, String accountNumber, int page, int size) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerUsername().equals(username)) {
            throw new ForbiddenOperationException("You do not own account " + accountNumber);
        }
        Page<BankTransaction> result = transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc(
            accountNumber, accountNumber, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PagedResponse<>(
            result.getContent().stream().map(t -> TransactionResponse.from(t, false)).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements());
    }


    private TransactionResponse execute(String key, String fingerprint, Supplier<BankTransaction> work) {
        Optional<BankTransaction> existing = transactionRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), fingerprint);
        }
        try {
            BankTransaction created = transactionTemplate.execute(status -> work.get());
            return TransactionResponse.from(created, false);
        } catch (DataIntegrityViolationException e) {
            return transactionRepository.findByIdempotencyKey(key)
                .map(tx -> replay(tx, fingerprint))
                .orElseThrow(() -> e);
        }
    }

    private TransactionResponse replay(BankTransaction existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(
                "Idempotency-Key was already used with a different request");
        }
        log.info("Idempotent replay for key {}", existing.getIdempotencyKey());
        return TransactionResponse.from(existing, true);
    }

    private BankTransaction insert(String key, String fingerprint, TransactionType type,
                                   String from, String to, BigDecimal amount) {
        return transactionRepository.saveAndFlush(new BankTransaction(key, fingerprint, type, from, to, amount));
    }

    private Account lockOwned(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerUsername().equals(username)) {
            throw new ForbiddenOperationException("You do not own account " + accountNumber);
        }
        return account;
    }


    private void recordEvent(BankTransaction tx) {
        TransactionEvent event = new TransactionEvent(
            UUID.randomUUID().toString(), tx.getType().name(), tx.getId(),
            tx.getFromAccount(), tx.getToAccount(), tx.getAmount(), Instant.now());
        String key = tx.getFromAccount() != null ? tx.getFromAccount() : tx.getToAccount();
        outboxRepository.save(new OutboxEvent(event.eventId(), key, toJson(event)));
    }

    private String toJson(TransactionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event " + event.eventId(), e);
        }
    }


    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new InvalidRequestException(
                "Idempotency-Key header is required (max " + MAX_KEY_LENGTH + " characters)");
        }
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private String fingerprint(TransactionType type, String username, String from, String to, BigDecimal amount) {
        return type + "|" + username + "|" + from + "|" + to + "|" + amount.toPlainString();
    }
}

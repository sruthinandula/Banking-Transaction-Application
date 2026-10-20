package com.example.bank.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.bank.account.Account;
import com.example.bank.account.AccountRepository;
import com.example.bank.exception.BankingExceptions.ForbiddenOperationException;
import com.example.bank.exception.BankingExceptions.IdempotencyConflictException;
import com.example.bank.exception.BankingExceptions.InsufficientFundsException;
import com.example.bank.exception.BankingExceptions.InvalidRequestException;
import com.example.bank.outbox.OutboxEvent;
import com.example.bank.outbox.OutboxRepository;
import com.example.bank.transaction.TransactionDtos.DepositRequest;
import com.example.bank.transaction.TransactionDtos.TransactionResponse;
import com.example.bank.transaction.TransactionDtos.TransferRequest;
import com.example.bank.transaction.TransactionDtos.WithdrawRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String ALICE_ACC = "111111111111";
    private static final String BOB_ACC = "222222222222";

    @Mock BankTransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock PlatformTransactionManager txManager;

    private TransactionService service;
    private final AtomicReference<BankTransaction> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new TransactionService(transactionRepository, accountRepository, outboxRepository,
            objectMapper, new TransactionTemplate(txManager));
    }

    private void stubInsert() {
        when(transactionRepository.saveAndFlush(any(BankTransaction.class))).thenAnswer(inv -> {
            BankTransaction tx = inv.getArgument(0);
            saved.set(tx);
            return tx;
        });
    }

    private Account account(String number, String owner, String balance) {
        Account a = new Account(number, owner, owner);
        a.credit(new BigDecimal(balance));
        return a;
    }

    @Test
    void deposit_creditsAccountAndWritesOutboxEvent() {
        stubInsert();
        Account alice = account(ALICE_ACC, "alice", "0");
        when(accountRepository.findByAccountNumberForUpdate(ALICE_ACC)).thenReturn(Optional.of(alice));

        TransactionResponse response = service.deposit("alice", "key-1",
            new DepositRequest(ALICE_ACC, new BigDecimal("100.00")));

        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
        assertThat(response.type()).isEqualTo("DEPOSIT");
        assertThat(response.idempotentReplay()).isFalse();

        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().getPayload()).contains("\"eventType\":\"DEPOSIT\"");
        assertThat(outbox.getValue().getEventKey()).isEqualTo(ALICE_ACC);
    }

    @Test
    void withdraw_withInsufficientFunds_throwsAndPublishesNothing() {
        stubInsert();
        Account alice = account(ALICE_ACC, "alice", "50.00");
        when(accountRepository.findByAccountNumberForUpdate(ALICE_ACC)).thenReturn(Optional.of(alice));

        assertThrows(InsufficientFundsException.class, () -> service.withdraw("alice", "key-2",
            new WithdrawRequest(ALICE_ACC, new BigDecimal("80.00"))));

        assertThat(alice.getBalance()).isEqualByComparingTo("50.00");
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void withdraw_fromSomeoneElsesAccount_isForbidden() {
        stubInsert();
        Account bob = account(BOB_ACC, "bob", "500.00");
        when(accountRepository.findByAccountNumberForUpdate(BOB_ACC)).thenReturn(Optional.of(bob));

        assertThrows(ForbiddenOperationException.class, () -> service.withdraw("alice", "key-3",
            new WithdrawRequest(BOB_ACC, new BigDecimal("10.00"))));

        assertThat(bob.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void transfer_movesMoneyBetweenAccountsAndEmitsOneEvent() {
        stubInsert();
        Account alice = account(ALICE_ACC, "alice", "100.00");
        Account bob = account(BOB_ACC, "bob", "0");
        when(accountRepository.lockByAccountNumbers(anyCollection())).thenReturn(List.of(alice, bob));

        TransactionResponse response = service.transfer("alice", "key-4",
            new TransferRequest(ALICE_ACC, BOB_ACC, new BigDecimal("40.00")));

        assertThat(alice.getBalance()).isEqualByComparingTo("60.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("40.00");
        assertThat(response.type()).isEqualTo("TRANSFER");
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void transfer_toSameAccount_isRejectedBeforeAnyWork() {
        assertThrows(InvalidRequestException.class, () -> service.transfer("alice", "key-5",
            new TransferRequest(ALICE_ACC, ALICE_ACC, new BigDecimal("10.00"))));

        verifyNoInteractions(transactionRepository, accountRepository, outboxRepository);
    }

    @Test
    void missingIdempotencyKey_isRejected() {
        assertThrows(InvalidRequestException.class, () -> service.deposit("alice", " ",
            new DepositRequest(ALICE_ACC, new BigDecimal("10.00"))));
    }

    @Test
    void sameKeyAndSamePayload_returnsOriginalResultWithoutMovingMoneyAgain() {
        stubInsert();
        Account alice = account(ALICE_ACC, "alice", "0");
        when(accountRepository.findByAccountNumberForUpdate(ALICE_ACC)).thenReturn(Optional.of(alice));
        DepositRequest request = new DepositRequest(ALICE_ACC, new BigDecimal("100.00"));

        service.deposit("alice", "key-6", request);
        when(transactionRepository.findByIdempotencyKey("key-6")).thenReturn(Optional.of(saved.get()));
        TransactionResponse second = service.deposit("alice", "key-6", request);

        assertThat(second.idempotentReplay()).isTrue();
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00"); // credited once, not twice
        verify(accountRepository, times(1)).findByAccountNumberForUpdate(ALICE_ACC);
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void sameKeyWithDifferentPayload_isConflict() {
        stubInsert();
        Account alice = account(ALICE_ACC, "alice", "0");
        when(accountRepository.findByAccountNumberForUpdate(ALICE_ACC)).thenReturn(Optional.of(alice));

        service.deposit("alice", "key-7", new DepositRequest(ALICE_ACC, new BigDecimal("100.00")));
        when(transactionRepository.findByIdempotencyKey("key-7")).thenReturn(Optional.of(saved.get()));

        assertThrows(IdempotencyConflictException.class, () -> service.deposit("alice", "key-7",
            new DepositRequest(ALICE_ACC, new BigDecimal("999.00"))));
        assertThat(alice.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void concurrentDuplicate_uniqueViolationFallsBackToWinnersResult() {
        BankTransaction winner = new BankTransaction("key-8",
            "DEPOSIT|alice|null|" + ALICE_ACC + "|100.00", TransactionType.DEPOSIT,
            null, ALICE_ACC, new BigDecimal("100.00"));
        when(transactionRepository.findByIdempotencyKey("key-8"))
            .thenReturn(Optional.empty(), Optional.of(winner));
        when(transactionRepository.saveAndFlush(any(BankTransaction.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        TransactionResponse response = service.deposit("alice", "key-8",
            new DepositRequest(ALICE_ACC, new BigDecimal("100.00")));

        assertThat(response.idempotentReplay()).isTrue();
        verify(outboxRepository, never()).save(any());
    }
}

package com.example.bank.transaction;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<BankTransaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(
        String fromAccount, String toAccount, Pageable pageable);
}

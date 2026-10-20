package com.example.bank.account;

import com.example.bank.exception.BankingExceptions.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(name = "owner_username", nullable = false, length = 50)
    private String ownerUsername;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {}

    public Account(String accountNumber, String ownerUsername, String holderName) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.holderName = holderName;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber);
        }
        this.balance = this.balance.subtract(amount);
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}

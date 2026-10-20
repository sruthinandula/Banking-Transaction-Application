package com.example.bank.account;

import com.example.bank.account.AccountDtos.AccountResponse;
import com.example.bank.exception.BankingExceptions.AccountNotFoundException;
import com.example.bank.exception.BankingExceptions.ForbiddenOperationException;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(String username, String holderName) {
        Account account = accountRepository.save(new Account(generateUniqueNumber(), username, holderName));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listMine(String username) {
        return accountRepository.findByOwnerUsernameOrderByIdAsc(username).stream()
            .map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getMine(String username, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!account.getOwnerUsername().equals(username)) {
            throw new ForbiddenOperationException("You do not own account " + accountNumber);
        }
        return AccountResponse.from(account);
    }

    private String generateUniqueNumber() {
        String number;
        do {
            number = String.format("%012d", random.nextLong(1_000_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}

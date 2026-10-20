package com.example.bank.account;

import com.example.bank.account.AccountDtos.AccountResponse;
import com.example.bank.account.AccountDtos.CreateAccountRequest;
import com.example.bank.transaction.TransactionDtos.PagedResponse;
import com.example.bank.transaction.TransactionDtos.TransactionResponse;
import com.example.bank.transaction.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(Authentication auth,
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(accountService.create(auth.getName(), request.holderName()));
    }

    @GetMapping
    public List<AccountResponse> list(Authentication auth) {
        return accountService.listMine(auth.getName());
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(Authentication auth, @PathVariable String accountNumber) {
        return accountService.getMine(auth.getName(), accountNumber);
    }

    @GetMapping("/{accountNumber}/transactions")
    public PagedResponse<TransactionResponse> history(Authentication auth, @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return transactionService.history(auth.getName(), accountNumber, page, size);
    }
}

package com.example.bank.transaction;

import com.example.bank.transaction.TransactionDtos.DepositRequest;
import com.example.bank.transaction.TransactionDtos.TransactionResponse;
import com.example.bank.transaction.TransactionDtos.TransferRequest;
import com.example.bank.transaction.TransactionDtos.WithdrawRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(Authentication auth,
            @RequestHeader(IDEMPOTENCY_HEADER) String key, @Valid @RequestBody DepositRequest request) {
        return respond(transactionService.deposit(auth.getName(), key, request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(Authentication auth,
            @RequestHeader(IDEMPOTENCY_HEADER) String key, @Valid @RequestBody WithdrawRequest request) {
        return respond(transactionService.withdraw(auth.getName(), key, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(Authentication auth,
            @RequestHeader(IDEMPOTENCY_HEADER) String key, @Valid @RequestBody TransferRequest request) {
        return respond(transactionService.transfer(auth.getName(), key, request));
    }

    private ResponseEntity<TransactionResponse> respond(TransactionResponse response) {
        return ResponseEntity.status(response.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED)
            .body(response);
    }
}

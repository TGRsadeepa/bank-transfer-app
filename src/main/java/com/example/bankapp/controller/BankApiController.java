package com.example.bankapp.controller;

import java.math.BigDecimal;
import java.util.List;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BankApiController {

    private final BankService bankService;

    @GetMapping("/accounts")
    public List<Customer> getAccounts() {
        return bankService.getCustomers();
    }

    @GetMapping("/accounts/{accountNumber}")
    public Customer getAccount(@PathVariable String accountNumber) {
        return bankService.getCustomer(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Account " + accountNumber + " was not found."));
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public List<BankTransaction> getAccountHistory(@PathVariable String accountNumber) {
        if (bankService.getCustomer(accountNumber).isEmpty()) {
            throw new ResponseStatusException(
                    NOT_FOUND, "Account " + accountNumber + " was not found.");
        }
        return bankService.getTransactionHistory(accountNumber);
    }

    @GetMapping("/transfers/large")
    public List<BankTransaction> getLargeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min) {
        if (min.signum() < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "Minimum amount cannot be negative.");
        }
        return bankService.getSuccessfulTransfersAbove(min);
    }
}

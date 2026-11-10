package com.example.bankapp.config;

import java.math.BigDecimal;
import java.util.List;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads sample customers once and provisions the documented internal fee ledger.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final String FEE_ACCOUNT_EMAIL = "bankfees@example.com";

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    customer("Kamal", "100001", "kamal@example.com", "50000.00"),
                    customer("Nimali", "100002", "nimali@example.com", "20000.00"),
                    customer("Saman", "100003", "saman@example.com", "10000.00")
            ));
        }

        Customer feeAccount = customerRepository
                .getByAccountNumber(BankService.FEE_ACCOUNT_NUMBER)
                .orElse(null);
        if (feeAccount != null) {
            if (!BankService.FEE_ACCOUNT_NAME.equals(feeAccount.getFullName())) {
                throw new IllegalStateException(
                        "Account 999999 is reserved for the bank fee ledger. "
                                + "Please resolve the account conflict before starting the application.");
            }
            return;
        }

        if (customerRepository.countEmails(FEE_ACCOUNT_EMAIL) > 0) {
            throw new IllegalStateException(
                    "The bank fee ledger email is already used by another account.");
        }

        customerRepository.save(customer(
                BankService.FEE_ACCOUNT_NAME,
                BankService.FEE_ACCOUNT_NUMBER,
                FEE_ACCOUNT_EMAIL,
                "0.00"));
    }

    private Customer customer(
            String fullName,
            String accountNumber,
            String email,
            String balance) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setAccountNumber(accountNumber);
        customer.setEmail(email);
        customer.setBalance(new BigDecimal(balance));
        return customer;
    }
}

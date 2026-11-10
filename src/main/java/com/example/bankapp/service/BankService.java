package com.example.bankapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    public static final String FEE_ACCOUNT_NUMBER = "999999";
    public static final String FEE_ACCOUNT_NAME = "Bank Fee Account";
    private static final String CUSTOMER_TRANSFER_DESCRIPTION = "Customer transfer";
    private static final String TRANSFER_FEE_DESCRIPTION = "Transfer fee (1%)";
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public List<Customer> getCustomers() {
        return customerRepository.findAllOrdered().stream()
                .filter(customer -> !FEE_ACCOUNT_NUMBER.equals(customer.getAccountNumber()))
                .toList();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getCustomers();
        }
        return customerRepository.searchByName(keyword.trim()).stream()
                .filter(customer -> !FEE_ACCOUNT_NUMBER.equals(customer.getAccountNumber()))
                .toList();
    }

    public Optional<Customer> getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber);
    }

    public BigDecimal getTotalBalance() {
        return Optional.ofNullable(customerRepository.getTotalBalance())
                .orElse(BigDecimal.ZERO);
    }

    public List<BankTransaction> getLatestTransactions() {
        return bankTransactionRepository.findAllLatest();
    }

    public List<BankTransaction> getTransactionHistory(String accountNumber) {
        return bankTransactionRepository.findHistory(accountNumber);
    }

    public long getSuccessfulTransactionCount() {
        return bankTransactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedTransactionCount() {
        return bankTransactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public BigDecimal getTotalAmountSent(String accountNumber) {
        return Optional.ofNullable(bankTransactionRepository.getTotalAmountSent(
                        accountNumber, TransactionStatus.SUCCESS))
                .orElse(BigDecimal.ZERO);
    }

    public List<BankTransaction> getSuccessfulTransfersAbove(BigDecimal minimum) {
        return bankTransactionRepository.findSuccessfulTransfersAbove(
                minimum, TransactionStatus.SUCCESS);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer == null
                || customer.getAccountNumber() == null
                || customer.getAccountNumber().isBlank()
                || customer.getEmail() == null
                || customer.getEmail().isBlank()
                || customer.getBalance() == null
                || customer.getBalance().signum() < 0) {
            throw new BankException("Customer details and a non-negative balance are required.");
        }
        if (FEE_ACCOUNT_NUMBER.equals(customer.getAccountNumber())) {
            throw new BankException("Account 999999 is reserved for the bank fee ledger.");
        }
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("An account with that account number already exists.");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("A customer with that email already exists.");
        }
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transferMoney(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {
        if (fromAccount != null && fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver accounts must be different.");
        }

        requireCustomerAccount(fromAccount);
        requireCustomerAccount(toAccount);

        validatePositiveAmount(amount, "Transfer");

        requireFeeAccount();
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

        if (customerRepository.debit(fromAccount, totalDebit) == 0) {
            throw new InsufficientBalanceException(
                    "The sender does not have enough balance for the transfer and fee.");
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure after withdrawal.");
        }

        if (customerRepository.credit(toAccount, amount) == 0) {
            throw new AccountNotFoundException(
                    "Receiver account " + toAccount + " was not found.");
        }

        if (fee.signum() > 0 && customerRepository.credit(FEE_ACCOUNT_NUMBER, fee) == 0) {
            throw new AccountNotFoundException(
                    "The bank fee account " + FEE_ACCOUNT_NUMBER + " was not found.");
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(CUSTOMER_TRANSFER_DESCRIPTION);
        BankTransaction savedTransfer = bankTransactionRepository.save(transaction);

        if (fee.signum() > 0) {
            BankTransaction feeTransaction = new BankTransaction();
            feeTransaction.setFromAccount(fromAccount);
            feeTransaction.setToAccount(FEE_ACCOUNT_NUMBER);
            feeTransaction.setAmount(fee);
            feeTransaction.setStatus(TransactionStatus.SUCCESS);
            feeTransaction.setDescription(TRANSFER_FEE_DESCRIPTION);
            bankTransactionRepository.save(feeTransaction);
        }
        return savedTransfer;
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        requireCustomerAccount(accountNumber);
        validatePositiveAmount(amount, "Deposit");

        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Account " + accountNumber + " was not found.");
        }
        return saveOperation(null, accountNumber, amount, "Deposit");
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        requireCustomerAccount(accountNumber);
        validatePositiveAmount(amount, "Withdrawal");

        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(
                    "The account does not have enough balance for this withdrawal.");
        }
        return saveOperation(accountNumber, null, amount, "Withdrawal");
    }

    private void validatePositiveAmount(BigDecimal amount, String operation) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankException(operation + " amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2
                || amount.precision() - amount.scale() > 12) {
            throw new BankException(
                    operation + " amount must fit within 12 whole digits and 2 decimal places.");
        }
    }

    private BankTransaction saveOperation(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        return bankTransactionRepository.save(transaction);
    }

    private void requireFeeAccount() {
        Customer feeAccount = requireAccount(FEE_ACCOUNT_NUMBER);
        if (!FEE_ACCOUNT_NAME.equals(feeAccount.getFullName())) {
            throw new BankException(
                    "Account " + FEE_ACCOUNT_NUMBER
                            + " is not configured as the bank fee account.");
        }
    }

    private Customer requireCustomerAccount(String accountNumber) {
        if (FEE_ACCOUNT_NUMBER.equals(accountNumber)) {
            throw new BankException("Account 999999 is reserved for the bank fee ledger.");
        }
        return requireAccount(accountNumber);
    }

    private Customer requireAccount(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account " + accountNumber + " was not found."));
    }
}

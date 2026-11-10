package com.example.bankapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @InjectMocks
    private BankService bankService;

    private void setUpAccounts() {
        when(customerRepository.getByAccountNumber("100001"))
                .thenReturn(Optional.of(new Customer()));
        when(customerRepository.getByAccountNumber("100002"))
                .thenReturn(Optional.of(new Customer()));
        Customer feeAccount = new Customer();
        feeAccount.setFullName(BankService.FEE_ACCOUNT_NAME);
        when(customerRepository.getByAccountNumber(BankService.FEE_ACCOUNT_NUMBER))
                .thenReturn(Optional.of(feeAccount));
    }

    @Test
    void transferMoneyDebitsCreditsAndSavesSuccessfulTransaction() {
        setUpAccounts();
        BigDecimal amount = new BigDecimal("5000.00");
        when(customerRepository.debit("100001", new BigDecimal("5050.00"))).thenReturn(1);
        when(customerRepository.credit("100002", amount)).thenReturn(1);
        when(customerRepository.credit(
                BankService.FEE_ACCOUNT_NUMBER, new BigDecimal("50.00"))).thenReturn(1);
        when(bankTransactionRepository.save(org.mockito.ArgumentMatchers.any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = bankService.transferMoney(
                "100001", "100002", amount, false);

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals("100001", result.getFromAccount());
        assertEquals("100002", result.getToAccount());
        assertEquals(amount, result.getAmount());
        verify(customerRepository).debit("100001", new BigDecimal("5050.00"));
        verify(customerRepository).credit("100002", amount);
        verify(customerRepository).credit(
                BankService.FEE_ACCOUNT_NUMBER, new BigDecimal("50.00"));
        verify(bankTransactionRepository, times(2))
                .save(org.mockito.ArgumentMatchers.any(BankTransaction.class));
    }

    @Test
    void simulatedFailureStopsBeforeCreditAndSuccessRecord() {
        setUpAccounts();
        BigDecimal amount = new BigDecimal("1000.00");
        when(customerRepository.debit("100001", new BigDecimal("1010.00"))).thenReturn(1);

        assertThrows(BankException.class, () -> bankService.transferMoney(
                "100001", "100002", amount, true));

        verify(customerRepository).debit("100001", new BigDecimal("1010.00"));
        verify(customerRepository, never()).credit("100002", amount);
        verify(bankTransactionRepository, never())
                .save(org.mockito.ArgumentMatchers.any(BankTransaction.class));
    }

    @Test
    void insufficientBalanceStopsTransfer() {
        setUpAccounts();
        BigDecimal amount = new BigDecimal("20000.00");
        when(customerRepository.debit("100001", new BigDecimal("20200.00"))).thenReturn(0);

        assertThrows(InsufficientBalanceException.class, () -> bankService.transferMoney(
                "100001", "100002", amount, false));

        verify(customerRepository, never()).credit("100002", amount);
        verify(bankTransactionRepository, never())
                .save(org.mockito.ArgumentMatchers.any(BankTransaction.class));
    }

    @Test
    void transferUsesRequiredRollbackConfiguration() throws NoSuchMethodException {
        Transactional transaction = BankService.class
                .getMethod("transferMoney", String.class, String.class, BigDecimal.class, boolean.class)
                .getAnnotation(Transactional.class);

        assertTrue(transaction.rollbackFor().length > 0);
        assertEquals(Exception.class, transaction.rollbackFor()[0]);
    }

    @Test
    void serviceUsesReadOnlyTransactionsByDefault() {
        Transactional transaction = BankService.class.getAnnotation(Transactional.class);
        assertTrue(transaction.readOnly());
    }
}

package com.example.bankapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class TransactionAuditServiceTest {

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @InjectMocks
    private TransactionAuditService auditService;

    @Test
    void recordsFailedTransfer() {
        BigDecimal amount = new BigDecimal("1000.00");
        when(bankTransactionRepository.save(org.mockito.ArgumentMatchers.any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankTransaction result = auditService.recordFailedTransaction(
                "100001", "100003", amount, "Simulated system failure");

        assertEquals("100001", result.getFromAccount());
        assertEquals("100003", result.getToAccount());
        assertEquals(amount, result.getAmount());
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Simulated system failure", result.getDescription());
    }

    @Test
    void startsAnIndependentTransaction() throws NoSuchMethodException {
        Transactional transaction = TransactionAuditService.class
                .getMethod("recordFailedTransaction", String.class, String.class, BigDecimal.class, String.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transaction.propagation());
    }
}

package com.example.bankapp.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

@Repository
public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. Get all transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC")
    List<BankTransaction> findAllLatest();

    // 2. Get transaction history for an account
    @Query("SELECT t FROM BankTransaction t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.createdAt DESC")
    List<BankTransaction> findHistory(
            @Param("accountNumber") String accountNumber
    );

    // 3. Get transactions by status
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status
    );

    // 4. Count transactions by status
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(
            @Param("status") TransactionStatus status
    );

    @Query("SELECT t FROM BankTransaction t "
            + "WHERE t.status = :status "
            + "AND t.description = 'Customer transfer' "
            + "AND t.amount > :minimum "
            + "ORDER BY t.createdAt DESC")
    List<BankTransaction> findSuccessfulTransfersAbove(
            @Param("minimum") BigDecimal minimum,
            @Param("status") TransactionStatus status
    );

    @Query("SELECT SUM(t.amount) FROM BankTransaction t "
            + "WHERE t.fromAccount = :accountNumber "
            + "AND t.status = :status "
            + "AND t.description IN ('Customer transfer', 'Transfer fee (1%)')")
    BigDecimal getTotalAmountSent(
            @Param("accountNumber") String accountNumber,
            @Param("status") TransactionStatus status
    );
}

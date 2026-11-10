# Assignment 01 Final Report Guide

## Personal and submission details

- Assignment: Complete Full-Stack Software Development Assignment 01
- Name: `[YOUR NAME]`
- Student ID: `[YOUR STUDENT ID]`
- Date: `[SUBMISSION DATE]`
- Course code: COSC 32133 / BECS 32263
- Course name: Full-Stack Software Development
- GitHub repository: https://github.com/TGRsadeepa/bank-transfer-app
- Required PDF filename: `Assignment01_YOUR_STUDENT_ID.pdf`

Replace the placeholders before exporting the final PDF. Include selected source
code and the screenshots listed below. No screenshots or manual database results
have been fabricated in this guide.

## Technologies

Java 21, Spring Boot, Spring MVC, Spring Data JPA, MySQL, Thymeleaf, HTML, CSS,
Jakarta Bean Validation, Lombok, Maven, and Git.

## Implementation summary

The official assignment PDF was not present in the supplied materials. The
following status is based on the project inspection and the task requirements
provided in the shared text; exact Task 1-5 wording and the three assignment
questions still need to be checked against the official PDF.

| Task | Current status |
|---|---|
| 1-5 | Existing Maven/Spring Boot setup, JPA entities, repositories, exceptions, and UI assets were present. They were not verified against the missing PDF's exact task wording. |
| 6 | `BankService` and `TransactionAuditService` implemented. Focused service tests verify transfer steps, simulated failure behavior, rollback annotation settings, and `REQUIRES_NEW`. |
| 7 | Transfer DTO validation, dashboard/customer/transfer/history routes, separate failed-transaction auditing, and customer uniqueness checks implemented. |
| 8 | Sample customers are inserted only when the customer table is empty. The documented fee ledger account `999999` is also provisioned when missing. |
| 9 | Focused service tests passed. Manual MySQL scenarios and application startup could not be completed because no MySQL server was listening on localhost:3306. |
| 10 | The final code uses rollback protection. The unsafe `SUPPORTS` experiment was not run because MySQL was unavailable; do not report an observed result until you perform it and capture evidence. |
| 11 | The current branch is `feature/bank-transfer`. No push, pull request, merge, or commit was performed. |
| 12 | Large-transfer search, successful amount-sent query, deposit/withdraw operations, and a 1% fee ledger were implemented. Database behavior still requires live-MySQL verification. |

### Important balance discrepancy

The three specified sample balances total **LKR 80,000.00**
(50,000 + 20,000 + 10,000), not LKR 85,000.00. The application seeds the
provided amounts as written; it does not invent an extra customer or balance.
The internal fee ledger starts at zero and is included in the bank-wide total.

The later 1% fee requirement also changes the expected successful-transfer
balances. A LKR 5,000 transfer debits Kamal LKR 5,050, credits Nimali LKR 5,000,
and credits LKR 50 to account `999999`. The resulting balances are Kamal
LKR 44,950, Nimali LKR 25,000, Saman LKR 10,000, and the fee ledger LKR 50;
the total remains LKR 80,000. If the assignment's Task 9 expected balance of
LKR 45,000 is intended to take precedence over Task 12's fee, clarify that with
the course instructions before submitting.

## Transaction concepts

### `@Transactional`

The service class uses `@Transactional(readOnly = true)` for read operations.
Money-changing methods start writable transactions. The transfer method uses
`@Transactional(rollbackFor = Exception.class)`, so the sender debit, receiver
credit, fee credit, and transaction rows are one unit of work.

### Rollback

If an error occurs after the debit, the transfer transaction is rolled back.
This restores the sender's balance and prevents partial receiver or fee credits.
The final service also throws on simulated failure immediately after withdrawal
to exercise this boundary.

### `REQUIRES_NEW`

`TransactionAuditService` is a separate Spring bean. Its
`recordFailedTransaction` method uses `Propagation.REQUIRES_NEW`, so a failed
operation can be committed in its own transaction after the main service
transaction has rolled back.

### `@Query`, `@Param`, and `@Modifying`

The repositories use JPQL `@Query` methods to retrieve customers and transaction
history, calculate totals, and find large successful transfers. `@Param`
connects named query parameters to method arguments. The debit and credit
`UPDATE` queries use `@Modifying`; debit also checks that the account has enough
balance.

## ACID properties

- **Atomicity:** Sender, receiver, fee ledger, and success records commit or roll
  back together.
- **Consistency:** Positive monetary amounts, non-overdraft debit checks, unique
  account/email constraints, and entity validations protect valid state.
- **Isolation:** Database transactions prevent partial operations from being
  observed as completed transfers.
- **Durability:** Committed records and balances are persisted in MySQL.

## Task 9 scenario checklist

These are expected outcomes from the implementation, not observed live-MySQL
results. The MySQL server was unavailable during validation.

| Test | Expected result | Run/result |
|---|---|---|
| Successful transfer | Transfer, fee, and success records commit atomically. See the balance discrepancy above. | Not run against MySQL |
| Insufficient balance | No balance changes; failed operation recorded. | Not run against MySQL |
| Same sender and receiver | Error; no balance changes; failed operation recorded. | Not run against MySQL |
| Simulated failure | Debit rolls back; a FAILED record is written independently. | Not run against MySQL |
| Empty/zero transfer amount | Validation error; no database write. | Not run against MySQL |
| Add customer and duplicate | First valid customer is saved; duplicate account/email is rejected. | Not run against MySQL |
| Search and account history | Search is name-only; history includes relevant success/failure rows and sent total. | Not run against MySQL |

The focused Maven service suite ran 7 tests successfully. The full suite compiled,
but its existing Spring context test failed because MySQL refused connections on
localhost:3306.

### SQL checks to run in MySQL

Run these after the application has started and the manual scenarios have been
executed. Capture the actual output for the report.

```sql
SELECT account_number, full_name, balance
FROM customers
ORDER BY account_number;

SELECT SUM(balance) AS total_bank_balance
FROM customers;

SELECT status, COUNT(*) AS transaction_count
FROM bank_transactions
GROUP BY status;

SELECT id, from_account, to_account, amount, status, description, created_at
FROM bank_transactions
ORDER BY created_at DESC;
```

## Task 10 experiment record

**Status: not performed.** Do not fill in an observed outcome until the
experiment is actually run and verified against MySQL.

For the experiment, temporarily change the transfer method's protection to
`@Transactional(propagation = Propagation.SUPPORTS)`, run only the simulated
failure case, record the sender balance and transaction rows, then immediately
restore `@Transactional(rollbackFor = Exception.class)`. With no outer
transaction, the repository debit can commit in its own transaction before the
simulated exception. After restoring the protected method, restart the
application and verify rollback. Record any database repair and its SQL only
after it is actually needed. The checked-in implementation must remain
protected.

## Answers to the three official assignment questions

The three question texts were not included in the supplied materials. Copy each
question from the official PDF and write its answer here before submission:

1. `[PASTE OFFICIAL QUESTION 1 AND ANSWER]`
2. `[PASTE OFFICIAL QUESTION 2 AND ANSWER]`
3. `[PASTE OFFICIAL QUESTION 3 AND ANSWER]`

## Screenshot checklist

Capture real screenshots after MySQL is running and the scenarios have been
executed. Save them with these suggested names:

1. `01-dashboard-initial.png` — browser at `http://localhost:8080`, showing
   seeded customers and initial totals.
2. `02-successful-transfer.png` — dashboard after the successful transfer,
   including updated balances and successful records.
3. `03-failed-transfer-rollback.png` — dashboard after a simulated failure,
   with the error message and unchanged balances.
4. `04-failed-audit-history.png` — an account history page showing successful
   and failed transaction rows.
5. `05-large-transfers.png` — browser at
   `http://localhost:8080/large-transfers?min=10000`.
6. `06-deposit-withdraw.png` — deposit and withdrawal pages with completed
   operations.
7. `07-mysql-verification.png` — MySQL client showing balance and transaction
   queries/results.
8. `08-transaction-annotations.png` — VS Code showing the transfer and audit
   transaction annotations.
9. `09-git-branch-status.png` — VS Code terminal showing branch
   `feature/bank-transfer` and the final local status.
10. `10-supports-experiment.png` — only after the Task 10 experiment is safely
    run, restored, and verified.

Include the code and selected screenshots in
`Assignment01_YOUR_STUDENT_ID.pdf`. Do not include credentials or the ignored
`secrets.properties` file.

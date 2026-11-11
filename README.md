# Bank Transfer System (Assignment 01)

Spring Boot + Thymeleaf + Spring Data JPA + MySQL + Lombok.

Demonstrates:
* `@Transactional` - a money transfer between two customers either fully succeeds or is fully rolled back
* `@Query` - JPQL, native SQL and UPDATE queries written in the repository interfaces
* A 1% transfer fee, credited atomically to the internal bank fee ledger

## Run
1. Start MySQL (XAMPP -> MySQL -> Start). The database `bank_db` is created automatically.
2. Run `mvn spring-boot:run` from the project directory.
3. Open http://localhost:8081  (3 sample customers are created on first start)

If your MySQL root user has a password, copy `secrets.properties.example` to
`secrets.properties` and edit it, or set `DB_USERNAME` and `DB_PASSWORD` in
your environment.

Account `999999` is reserved for the application’s internal **Bank Fee Account**.
It is provisioned with a zero balance so transfer fees have a dedicated ledger
destination. The dashboard hides this system account from customer forms and
account selection.

## JSON API (Postman)
The web pages return HTML. For JSON data, use these GET endpoints instead:

* `GET http://localhost:8081/api/accounts` - list customer accounts
* `GET http://localhost:8081/api/accounts/123456` - get one account
* `GET http://localhost:8081/api/accounts/123456/history` - get an account's transactions
* `GET http://localhost:8081/api/transfers/large?min=10000` - successful transfers at or above the minimum

These endpoints return JSON. For an account lookup or history request, replace
`123456` with an account number that exists in your database.
"# Assignment_Grading" 
"# Bank_Tranfer_App" 

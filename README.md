# Bank Transfer System (Assignment 01)

Spring Boot + Thymeleaf + Spring Data JPA + MySQL + Lombok.

Demonstrates:
* `@Transactional` - a money transfer between two customers either fully succeeds or is fully rolled back
* `@Query` - JPQL, native SQL and UPDATE queries written in the repository interfaces
* A 1% transfer fee, credited atomically to the internal bank fee ledger

## Run
1. Start MySQL (XAMPP -> MySQL -> Start). The database `bank_db` is created automatically.
2. Run `mvn spring-boot:run` from the project directory.
3. Open http://localhost:8080  (3 sample customers are created on first start)

If your MySQL root user has a password, copy `secrets.properties.example` to
`secrets.properties` and edit it, or set `DB_USERNAME` and `DB_PASSWORD` in
your environment.

Account `999999` is reserved for the application’s internal **Bank Fee Account**.
It is provisioned with a zero balance so transfer fees have a dedicated ledger
destination. The dashboard hides this system account from customer forms and
account selection.
"# Assignment_Grading" 
"# Bank_Tranfer_App" 

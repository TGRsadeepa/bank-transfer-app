package com.example.bankapp.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    @NotBlank(message = "Account number is required.")
    @Pattern(regexp = "\\d{6}", message = "Account number must contain six digits.")
    private String accountNumber;

    @Column(name = "full_name", nullable = false)
    @NotBlank(message = "Full name is required.")
    private String fullName;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private String email;

    @Column(precision = 14, scale = 2, nullable = false)
    @NotNull(message = "Opening balance is required.")
    @DecimalMin(value = "0.00", message = "Opening balance cannot be negative.")
    @Digits(integer = 12, fraction = 2, message = "Balance must have up to 12 digits and 2 decimal places.")
    private BigDecimal balance;
}

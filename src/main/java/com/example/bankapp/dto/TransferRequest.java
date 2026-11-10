package com.example.bankapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Holds the data submitted by the transfer form. */
@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Choose a sender account.")
    private String fromAccount;

    @NotBlank(message = "Choose a receiver account.")
    private String toAccount;

    @NotNull(message = "Transfer amount is required.")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero.")
    @Digits(integer = 12, fraction = 2,
            message = "Transfer amount must have up to 12 digits and 2 decimal places.")
    private BigDecimal amount;

    private boolean simulateFailure;
}

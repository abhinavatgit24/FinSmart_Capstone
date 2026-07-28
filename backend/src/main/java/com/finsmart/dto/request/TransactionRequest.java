package com.finsmart.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "income|expense", message = "Type must be 'income' or 'expense'")
    private String type;

    private String category;   // optional — auto-assigned if blank

    private String description;

    @NotNull(message = "Date is required")
    private LocalDate date;
}

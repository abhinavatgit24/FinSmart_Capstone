package com.finsmart.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BudgetRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Limit amount is required")
    @Positive(message = "Limit amount must be positive")
    private Double limitAmount;

    @NotBlank(message = "Period is required")
    @Pattern(regexp = "monthly|weekly", message = "Period must be 'monthly' or 'weekly'")
    private String period;
}

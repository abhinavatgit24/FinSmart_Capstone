package com.finsmart.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BudgetUtilisationResponse {

    private String id;
    private String category;
    private Double limitAmount;
    private String period;
    private Double spent;           // actual spend for current period
    private Double utilisationPct;  // spent / limitAmount * 100
    private String alertLevel;      // "none" | "warning" (≥80%) | "exceeded" (≥100%)
}

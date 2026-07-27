package com.finsmart.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialHealthResponse {

    private Integer score;               // null = not enough data to compute
    private String  band;                // "Poor" | "Average" | "Good" | "Excellent" | "N/A"

    private Double savingsRatioScore;        // null when no data
    private Double budgetAdherenceScore;
    private Double spendingConsistencyScore;

    private Double savingsRatioPct;
    private Double budgetAdherencePct;
    private Double spendingConsistencyPct;

    private String insight;
}
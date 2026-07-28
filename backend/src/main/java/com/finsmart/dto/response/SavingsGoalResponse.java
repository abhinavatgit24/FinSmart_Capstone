package com.finsmart.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavingsGoalResponse {

    private String id;
    private String name;
    private Double targetAmount;
    private Double savedAmount;
    private LocalDate deadline;
    private String status;

    // Derived / computed fields
    private Double progressPct;             // savedAmount / targetAmount * 100
    private Double remaining;               // targetAmount - savedAmount
    private LocalDate predictedCompletion;  // null if not enough data
    private String predictionNote;          // human-readable explanation
    private Boolean onTrack;                // will they hit deadline?
}

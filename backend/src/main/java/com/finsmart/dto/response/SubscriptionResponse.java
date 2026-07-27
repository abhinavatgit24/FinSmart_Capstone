package com.finsmart.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionResponse {
    private String      merchant;          // normalised merchant name
    private Double      amount;            // recurring amount
    private String      frequency;         // "monthly" | "weekly" | "irregular"
    private LocalDate   lastCharged;       // most recent transaction date
    private LocalDate   nextExpected;      // predicted next charge date
    private int         occurrences;       // how many times detected
    private List<LocalDate> chargeDates;   // all observed dates
    private String      category;
    private Double      annualisedCost;    // amount * annual frequency estimate
}

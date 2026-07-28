package com.finsmart.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnalyticsResponse {

    // ── Category breakdown (pie / bar) ───────────────────────────────────────
    private Map<String, Double> categoryExpenses;       // { "Food": 4200, ... }
    private Map<String, Double> categoryIncome;

    // ── Month-over-month comparative ─────────────────────────────────────────
    // List of { month: "Jan", income: X, expense: Y, savings: Z }
    private List<Map<String, Object>> monthlyComparison;

    // ── Day-of-week heatmap ───────────────────────────────────────────────────
    // { "Monday": 1200, "Tuesday": 800, ... }
    private Map<String, Double> dayOfWeekExpenses;

    // ── Weekend vs weekday ────────────────────────────────────────────────────
    private Double weekendExpensePct;
    private Double weekdayExpensePct;

    // ── Top spending days ─────────────────────────────────────────────────────
    private List<Map<String, Object>> topSpendingDays; // { date, total }

    // ── Smart insights ────────────────────────────────────────────────────────
    private List<InsightResponse> insights;
}

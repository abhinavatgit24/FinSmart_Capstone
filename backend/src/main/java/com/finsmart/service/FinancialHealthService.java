package com.finsmart.service;

import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.dto.response.FinancialHealthResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FinancialHealthService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    private static final double W_SAVINGS = 0.40;
    private static final double W_BUDGET = 0.40;
    private static final double W_CONSISTENCY = 0.20;

    public FinancialHealthResponse compute(String userId) {
        // Health is a long-term indicator. Unlike dashboards and budgets, it
        // should not drop to "Poor" just because the current month happens to
        // contain expenses before the next income entry.
        List<Transaction> history = transactionRepository.findByUserIdOrderByDateDesc(userId);

        // A score without any financial activity is not meaningful. The UI already
        // knows how to render this null-score state.
        if (history.isEmpty()) {
            return noData("Add income or expense transactions to calculate your financial health score.");
        }

        List<Double> componentScores = new ArrayList<>();
        List<Double> componentWeights = new ArrayList<>();

        Double savingsRatioPct = null;
        Double savingsRatioScore = null;
        double income = sum(history, "income");
        double expense = sum(history, "expense");
        if (income > 0 || expense > 0) {
            savingsRatioPct = income > 0
                    ? Math.max(0, ((income - expense) / income) * 100.0)
                    : 0.0;
            savingsRatioScore = Math.min(100.0, (savingsRatioPct / 30.0) * 100.0);
            componentScores.add(savingsRatioScore);
            componentWeights.add(W_SAVINGS);
        }

        List<BudgetUtilisationResponse> utilisations = budgetService.getUtilisation(userId);
        Double budgetAdherencePct = null;
        Double budgetAdherenceScore = null;
        if (!utilisations.isEmpty()) {
            long onTrack = utilisations.stream()
                    .filter(u -> !"exceeded".equals(u.getAlertLevel()))
                    .count();
            budgetAdherencePct = ((double) onTrack / utilisations.size()) * 100.0;
            budgetAdherenceScore = budgetAdherencePct;
            componentScores.add(budgetAdherenceScore);
            componentWeights.add(W_BUDGET);
        }

        Double consistencyScore = null;
        Double consistencyPct = null;
        Set<String> monthsWithExpenses = new HashSet<>();
        Map<String, Double> monthlyExpense = new LinkedHashMap<>();
        for (Transaction txn : history) {
            if (!"expense".equalsIgnoreCase(txn.getType()) || txn.getDate() == null) continue;
            String month = txn.getDate().getYear() + "-" + txn.getDate().getMonthValue();
            monthsWithExpenses.add(month);
            monthlyExpense.merge(month, txn.getAmount(), Double::sum);
        }
        if (monthsWithExpenses.size() >= 2) {
            double[] values = monthlyExpense.values().stream().mapToDouble(Double::doubleValue).toArray();
            double mean = Arrays.stream(values).average().orElse(0);
            double stdDev = Math.sqrt(Arrays.stream(values)
                    .map(value -> Math.pow(value - mean, 2)).average().orElse(0));
            double coefficientOfVariation = mean > 0 ? (stdDev / mean) * 100.0 : 0;
            consistencyScore = Math.max(0, 100.0 - (coefficientOfVariation / 50.0) * 100.0);
            consistencyPct = Math.max(0, 100.0 - coefficientOfVariation);
            componentScores.add(consistencyScore);
            componentWeights.add(W_CONSISTENCY);
        }

        double weightedTotal = 0;
        double activeWeight = 0;
        for (int i = 0; i < componentScores.size(); i++) {
            weightedTotal += componentScores.get(i) * componentWeights.get(i);
            activeWeight += componentWeights.get(i);
        }
        int score = (int) Math.round(weightedTotal / activeWeight);

        return FinancialHealthResponse.builder()
                .score(score)
                .band(band(score))
                .savingsRatioScore(round2(savingsRatioScore))
                .budgetAdherenceScore(round2(budgetAdherenceScore))
                .spendingConsistencyScore(round2(consistencyScore))
                .savingsRatioPct(round2(savingsRatioPct))
                .budgetAdherencePct(round2(budgetAdherencePct))
                .spendingConsistencyPct(round2(consistencyPct))
                .insight(buildInsight(income, savingsRatioPct, budgetAdherencePct, consistencyScore))
                .build();
    }

    private FinancialHealthResponse noData(String insight) {
        return FinancialHealthResponse.builder().score(null).band("N/A").insight(insight).build();
    }

    private double sum(List<Transaction> transactions, String type) {
        return transactions.stream().filter(t -> type.equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();
    }

    private String band(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Average";
        return "Poor";
    }

    private String buildInsight(double income, Double savingsRatio, Double budgetAdherence, Double consistency) {
        if (income <= 0) return "Add income transactions to measure your savings ratio accurately.";
        if (savingsRatio != null && savingsRatio < 10) return "Boost your savings — aim to save at least 20% of your recorded income.";
        if (budgetAdherence == null) return "Set category budgets to make your health score more complete.";
        if (budgetAdherence < 70) return "Several budgets were exceeded. Review your top spending categories.";
        if (consistency == null) return "Add expenses in at least two different months to measure spending consistency.";
        if (consistency < 50) return "Your spending varies substantially month to month. Try following a monthly plan.";
        return "Good progress! Keep expenses below budget and maintain your savings habit.";
    }

    private Double round2(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}

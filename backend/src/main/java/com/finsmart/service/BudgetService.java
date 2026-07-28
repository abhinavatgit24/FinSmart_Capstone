package com.finsmart.service;

import com.finsmart.dto.request.BudgetRequest;
import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.model.Budget;
import com.finsmart.model.Transaction;
import com.finsmart.repository.BudgetRepository;
import com.finsmart.repository.TransactionRepository;
import com.finsmart.util.DateQueryHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository      budgetRepository;
    private final TransactionRepository transactionRepository;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public Budget createOrUpdate(String userId, BudgetRequest req) {
        Budget budget = budgetRepository
                .findByUserIdAndCategoryAndPeriod(userId, req.getCategory(), req.getPeriod())
                .orElse(Budget.builder().userId(userId).build());

        budget.setCategory(req.getCategory());
        budget.setLimitAmount(req.getLimitAmount());
        budget.setPeriod(req.getPeriod());
        budget.setUpdatedAt(LocalDateTime.now());

        return budgetRepository.save(budget);
    }

    public List<Budget> getAll(String userId) {
        return budgetRepository.findByUserId(userId);
    }

    @Transactional
    public void delete(String userId, String id) {
        if (!budgetRepository.existsByIdAndUserId(id, userId)) {
            throw new RuntimeException("Budget not found");
        }
        budgetRepository.deleteByIdAndUserId(id, userId);
    }

    // ── Utilisation ───────────────────────────────────────────────────────────

    public List<BudgetUtilisationResponse> getUtilisation(String userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(b -> buildUtilisation(userId, b))
                .collect(Collectors.toList());
    }

    public BudgetUtilisationResponse getUtilisationForBudget(String userId, String budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        if (!budget.getUserId().equals(userId)) throw new RuntimeException("Budget not found");
        return buildUtilisation(userId, budget);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private BudgetUtilisationResponse buildUtilisation(String userId, Budget budget) {
        // Use expanded window to catch timezone-shifted dates
        LocalDate[] range = periodRange(budget.getPeriod());
        LocalDate from = DateQueryHelper.periodStart(range[0]);
        LocalDate to   = DateQueryHelper.periodEnd(range[1]);

        List<Transaction> periodTxns =
                transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);

        double spent = periodTxns.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType())
                          && budget.getCategory().equalsIgnoreCase(t.getCategory()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double utilisationPct = budget.getLimitAmount() > 0
                ? (spent / budget.getLimitAmount()) * 100.0
                : 0.0;

        String alertLevel;
        if      (utilisationPct >= 100) alertLevel = "exceeded";
        else if (utilisationPct >= 80)  alertLevel = "warning";
        else                            alertLevel = "none";

        return BudgetUtilisationResponse.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .limitAmount(budget.getLimitAmount())
                .period(budget.getPeriod())
                .spent(round2(spent))
                .utilisationPct(round2(utilisationPct))
                .alertLevel(alertLevel)
                .build();
    }

    private LocalDate[] periodRange(String period) {
        LocalDate today = LocalDate.now();
        if ("weekly".equalsIgnoreCase(period)) {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            return new LocalDate[]{ monday, monday.plusDays(6) };
        }
        LocalDate first = today.withDayOfMonth(1);
        return new LocalDate[]{ first, first.plusMonths(1).minusDays(1) };
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
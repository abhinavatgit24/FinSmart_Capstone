package com.finsmart.service;

import com.finsmart.dto.request.SavingsGoalRequest;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.SavingsGoal;
import com.finsmart.model.Transaction;
import com.finsmart.repository.SavingsGoalRepository;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository  transactionRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public SavingsGoal create(String userId, SavingsGoalRequest req) {
        SavingsGoal goal = SavingsGoal.builder()
                .userId(userId)
                .name(req.getName())
                .targetAmount(req.getTargetAmount())
                .savedAmount(req.getSavedAmount() != null ? req.getSavedAmount() : 0.0)
                .deadline(req.getDeadline())
                .build();
        return goalRepository.save(goal);
    }

    @Transactional
    public SavingsGoal update(String userId, String id, SavingsGoalRequest req) {
        SavingsGoal goal = findOwned(userId, id);
        goal.setName(req.getName());
        goal.setTargetAmount(req.getTargetAmount());
        goal.setSavedAmount(req.getSavedAmount() != null ? req.getSavedAmount() : goal.getSavedAmount());
        goal.setDeadline(req.getDeadline());
        goal.setUpdatedAt(LocalDateTime.now());
        if (goal.getSavedAmount() >= goal.getTargetAmount()) goal.setStatus("completed");
        return goalRepository.save(goal);
    }

    @Transactional
    public void delete(String userId, String id) {
        if (!goalRepository.existsByIdAndUserId(id, userId)) throw new RuntimeException("Goal not found");
        goalRepository.deleteByIdAndUserId(id, userId);
    }

    // ── Rich response list ────────────────────────────────────────────────────

    public List<SavingsGoalResponse> getAllWithPrediction(String userId) {
        double avgMonthlySavings = computeAvgMonthlySavings(userId);

        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(g -> enrich(g, avgMonthlySavings))
                .collect(Collectors.toList());
    }

    public SavingsGoalResponse getWithPrediction(String userId, String id) {
        SavingsGoal goal = findOwned(userId, id);
        double avg = computeAvgMonthlySavings(userId);
        return enrich(goal, avg);
    }

    // ── Prediction logic ──────────────────────────────────────────────────────

    /**
     * Computes average monthly net savings (income - expense) over the last 6 months.
     * Returns 0 if there is insufficient data.
     */
    public double computeAvgMonthlySavings(String userId) {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        List<Transaction> history = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, sixMonthsAgo, LocalDate.now());

        if (history.isEmpty()) return 0.0;

        // Group net savings per month
        Map<String, Double> monthlyNet = new LinkedHashMap<>();
        for (Transaction t : history) {
            if (t.getDate() == null) continue;
            String key = t.getDate().getYear() + "-" + t.getDate().getMonthValue();
            double delta = "income".equalsIgnoreCase(t.getType())
                    ?  t.getAmount()
                    : -t.getAmount();
            monthlyNet.merge(key, delta, Double::sum);
        }

        if (monthlyNet.isEmpty()) return 0.0;

        double totalNet = monthlyNet.values().stream().mapToDouble(Double::doubleValue).sum();
        return totalNet / monthlyNet.size();
    }

    private SavingsGoalResponse enrich(SavingsGoal g, double avgMonthlySavings) {
        double remaining    = Math.max(0, g.getTargetAmount() - g.getSavedAmount());
        double progressPct  = g.getTargetAmount() > 0
                ? Math.min(100.0, (g.getSavedAmount() / g.getTargetAmount()) * 100.0)
                : 0.0;

        LocalDate predictedCompletion = null;
        String    predictionNote;
        Boolean   onTrack = null;

        if ("completed".equals(g.getStatus())) {
            predictionNote = "Goal completed! 🎉";
            onTrack = true;
        } else if (avgMonthlySavings <= 0) {
            predictionNote = "Save consistently to unlock completion prediction.";
        } else {
            double monthsNeeded = remaining / avgMonthlySavings;
            if (monthsNeeded <= 0) {
                predictedCompletion = LocalDate.now();
                predictionNote = "You're already there!";
                onTrack = true;
            } else {
                long daysNeeded = Math.round(monthsNeeded * 30.44);
                predictedCompletion = LocalDate.now().plusDays(daysNeeded);
                onTrack = !predictedCompletion.isAfter(g.getDeadline());
                long daysAway = ChronoUnit.DAYS.between(LocalDate.now(), predictedCompletion);

                if (onTrack) {
                    predictionNote = String.format(
                        "At ₹%.0f/month you'll hit this goal ~%d days before deadline.",
                        avgMonthlySavings, ChronoUnit.DAYS.between(predictedCompletion, g.getDeadline()));
                } else {
                    long overBy = ChronoUnit.DAYS.between(g.getDeadline(), predictedCompletion);
                    predictionNote = String.format(
                        "At current pace you'll miss the deadline by ~%d days. Increase savings to stay on track.",
                        overBy);
                }
            }
        }

        return SavingsGoalResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .targetAmount(g.getTargetAmount())
                .savedAmount(g.getSavedAmount())
                .deadline(g.getDeadline())
                .status(g.getStatus())
                .progressPct(round2(progressPct))
                .remaining(round2(remaining))
                .predictedCompletion(predictedCompletion)
                .predictionNote(predictionNote)
                .onTrack(onTrack)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SavingsGoal findOwned(String userId, String id) {
        SavingsGoal g = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (!g.getUserId().equals(userId)) throw new RuntimeException("Goal not found");
        return g;
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}

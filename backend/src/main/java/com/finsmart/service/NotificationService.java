package com.finsmart.service;

import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.Notification;
import com.finsmart.model.Transaction;
import com.finsmart.repository.NotificationRepository;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TransactionRepository  transactionRepository;
    private final BudgetService          budgetService;
    private final SavingsGoalService     goalService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<Notification> getAll(String userId) {
        // Refresh alerts before returning
        refreshAlerts(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markRead(String userId, String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ── Alert generation (called on-demand when fetching notifications) ───────

    public void refreshAlerts(String userId) {
        checkBudgetAlerts(userId);
        checkGoalMilestones(userId);
        checkUnusualSpending(userId);
    }

    // ── Budget alerts ─────────────────────────────────────────────────────────

    private void checkBudgetAlerts(String userId) {
        List<BudgetUtilisationResponse> utils = budgetService.getUtilisation(userId);
        for (BudgetUtilisationResponse u : utils) {
            if ("warning".equals(u.getAlertLevel())) {
                deduplicatedSave(userId, u.getId(), "budget_warning",
                        "⚠️ " + u.getCategory() + " budget at " + u.getUtilisationPct().intValue() + "%",
                        "You've used " + u.getUtilisationPct().intValue() + "% of your " + u.getPeriod() +
                        " " + u.getCategory() + " budget. Limit: ₹" + u.getLimitAmount().intValue() + ".");
            } else if ("exceeded".equals(u.getAlertLevel())) {
                deduplicatedSave(userId, u.getId(), "budget_exceeded",
                        "🚨 " + u.getCategory() + " budget exceeded",
                        "You've exceeded your " + u.getPeriod() + " " + u.getCategory() +
                        " budget by ₹" + String.format("%.0f", (u.getSpent() - u.getLimitAmount())) + ".");
            }
        }
    }

    // ── Goal milestone alerts ─────────────────────────────────────────────────

    private void checkGoalMilestones(String userId) {
        List<SavingsGoalResponse> goals = goalService.getAllWithPrediction(userId);
        for (SavingsGoalResponse g : goals) {
            if ("completed".equals(g.getStatus())) {
                deduplicatedSave(userId, g.getId(), "goal_completed",
                        "🎉 Goal completed: " + g.getName(),
                        "Congratulations! You've reached your savings goal of ₹" +
                        String.format("%.0f", g.getTargetAmount()) + " for \"" + g.getName() + "\"!");
            } else if (g.getProgressPct() >= 50 && g.getProgressPct() < 51) {
                deduplicatedSave(userId, g.getId() + "_50", "goal_milestone",
                        "🏁 50% of \"" + g.getName() + "\" achieved",
                        "You're halfway to your \"" + g.getName() + "\" goal! Keep going!");
            } else if (Boolean.FALSE.equals(g.getOnTrack())) {
                deduplicatedSave(userId, g.getId() + "_offtrack", "goal_off_track",
                        "📉 \"" + g.getName() + "\" may miss deadline",
                        g.getPredictionNote() != null ? g.getPredictionNote()
                                : "Increase monthly savings to meet your goal deadline.");
            }
        }
    }

    // ── Unusual spending alert ────────────────────────────────────────────────

    private void checkUnusualSpending(String userId) {
        LocalDate today     = LocalDate.now();
        LocalDate thisMonth = today.withDayOfMonth(1);
        LocalDate lastMonth = thisMonth.minusMonths(1);

        List<Transaction> thisM = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, thisMonth, today);
        List<Transaction> lastM = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, lastMonth.withDayOfMonth(1), thisMonth.minusDays(1));

        double thisTotal = sum(thisM, "expense");
        double lastTotal = sum(lastM, "expense");

        if (lastTotal > 0 && thisTotal > lastTotal * 1.4) {
            String refId = "unusual_" + today.getYear() + "_" + today.getMonthValue();
            deduplicatedSave(userId, refId, "unusual_spending",
                    "📊 Unusual spending detected",
                    String.format("Your expenses this month are %.0f%% higher than last month. Review your spending.",
                            ((thisTotal - lastTotal) / lastTotal) * 100));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Only creates a notification if one with the same (userId, refId, type) doesn't exist.
     * Prevents duplicate alerts on every poll.
     */
    private void deduplicatedSave(String userId, String refId, String type, String title, String body) {
        if (!notificationRepository.existsByUserIdAndRefIdAndType(userId, refId, type)) {
            notificationRepository.save(Notification.builder()
                    .userId(userId)
                    .type(type)
                    .refId(refId)
                    .title(title)
                    .body(body)
                    .build());
        }
    }

    private double sum(List<Transaction> txns, String type) {
        return txns.stream().filter(t -> type.equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();
    }
}

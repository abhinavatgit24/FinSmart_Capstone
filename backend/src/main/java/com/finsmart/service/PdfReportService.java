package com.finsmart.service;

import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.dto.response.FinancialHealthResponse;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.Transaction;
import com.finsmart.model.User;
import com.finsmart.repository.TransactionRepository;
import com.finsmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a monthly report as an HTML string.
 * The frontend converts this to a printable PDF via the browser's print API.
 * This avoids adding a heavyweight PDF library (iText/OpenPDF) to the classpath
 * while producing a professional, print-ready document.
 */
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;
    private final BudgetService         budgetService;
    private final SavingsGoalService    goalService;
    private final FinancialHealthService healthService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public String generateMonthlyReportHtml(String userId, int year, int month) {
        // ── Gather data ───────────────────────────────────────────────────────
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        String monthLabel = from.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                + " " + year;

        List<Transaction> txns = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);

        double income  = txns.stream().filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();
        double expense = txns.stream().filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();
        double savings = income - expense;

        Map<String, Double> catBreakdown = txns.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Other",
                        Collectors.summingDouble(Transaction::getAmount)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        List<BudgetUtilisationResponse> budgets = budgetService.getUtilisation(userId);
        List<SavingsGoalResponse> goals = goalService.getAllWithPrediction(userId);
        FinancialHealthResponse health  = healthService.compute(userId);

        String userName = userRepository.findByEmail(userId)
                .map(User::getName).orElse("User");

        // ── Build HTML ────────────────────────────────────────────────────────
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8"/>
                <title>FinSmart Monthly Report</title>
                <style>
                  * { margin: 0; padding: 0; box-sizing: border-box; }
                  body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 13px;
                         color: #1a1a2e; background: #fff; padding: 40px; }
                  h1 { font-size: 22px; color: #444ce7; margin-bottom: 2px; }
                  h2 { font-size: 15px; color: #444ce7; margin: 24px 0 10px;
                       border-bottom: 2px solid #e8eaff; padding-bottom: 4px; }
                  h3 { font-size: 13px; margin-bottom: 6px; color: #444; }
                  .subtitle { font-size: 12px; color: #888; margin-bottom: 28px; }
                  .summary-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; margin-bottom: 8px; }
                  .stat-box { background: #f5f6ff; border-radius: 10px; padding: 16px; text-align: center; }
                  .stat-box .label { font-size: 11px; color: #888; text-transform: uppercase;
                                     letter-spacing: .5px; margin-bottom: 6px; }
                  .stat-box .value { font-size: 20px; font-weight: 700; }
                  .income  { color: #12b76a; }
                  .expense { color: #f04438; }
                  .savings { color: #444ce7; }
                  table { width: 100%; border-collapse: collapse; margin-top: 4px; }
                  th { background: #f5f6ff; font-size: 11px; text-transform: uppercase;
                       letter-spacing: .4px; padding: 8px 10px; text-align: left;
                       color: #555; border-bottom: 1px solid #e0e0f0; }
                  td { padding: 7px 10px; border-bottom: 1px solid #f0f0f8; font-size: 12px; }
                  tr:last-child td { border-bottom: none; }
                  .badge { display: inline-block; padding: 2px 8px; border-radius: 20px;
                           font-size: 10px; font-weight: 600; }
                  .badge-exc { background: #fee2e2; color: #b91c1c; }
                  .badge-warn { background: #fef3c7; color: #b45309; }
                  .badge-ok { background: #dcfce7; color: #15803d; }
                  .health-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; }
                  .health-box { border: 1px solid #e0e0f0; border-radius: 10px; padding: 14px; }
                  .score-big { font-size: 36px; font-weight: 800; color: #444ce7;
                               text-align: center; margin: 6px 0 2px; }
                  .band { text-align: center; font-size: 12px; font-weight: 600;
                          padding: 2px 10px; border-radius: 20px;
                          display: inline-block; width: 100%; margin-bottom: 4px; }
                  .bar-wrap { background: #eee; border-radius: 4px; height: 6px; margin-top: 4px; }
                  .bar-fill { height: 6px; border-radius: 4px; }
                  .footer { margin-top: 36px; font-size: 11px; color: #aaa; text-align: center; }
                  @media print {
                    body { padding: 20px; }
                    .no-print { display: none; }
                  }
                </style>
                </head>
                <body>
                """);

        // Header
        sb.append("<h1>FinSmart Monthly Report</h1>");
        sb.append("<p class=\"subtitle\">").append(monthLabel).append(" &nbsp;·&nbsp; ").append(userName).append("</p>");

        // Summary
        sb.append("<h2>Financial Summary</h2>");
        sb.append("<div class=\"summary-grid\">");
        sb.append(statBox("Total Income", "₹" + fmt(income), "income"));
        sb.append(statBox("Total Expenses", "₹" + fmt(expense), "expense"));
        sb.append(statBox("Net Savings", "₹" + fmt(savings), savings >= 0 ? "savings" : "expense"));
        sb.append("</div>");

        // Category breakdown
        if (!catBreakdown.isEmpty()) {
            sb.append("<h2>Expense Breakdown by Category</h2>");
            sb.append("<table><thead><tr><th>Category</th><th>Amount</th><th>% of Expenses</th></tr></thead><tbody>");
            catBreakdown.forEach((cat, amt) -> {
                double pct = expense > 0 ? (amt / expense) * 100 : 0;
                sb.append("<tr><td>").append(cat).append("</td>")
                  .append("<td>₹").append(fmt(amt)).append("</td>")
                  .append("<td>").append(String.format("%.1f%%", pct)).append("</td></tr>");
            });
            sb.append("</tbody></table>");
        }

        // Transactions
        if (!txns.isEmpty()) {
            sb.append("<h2>All Transactions (").append(txns.size()).append(")</h2>");
            sb.append("<table><thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Type</th><th>Amount</th></tr></thead><tbody>");
            txns.forEach(t -> {
                String cls = "income".equalsIgnoreCase(t.getType()) ? "income" : "expense";
                sb.append("<tr>")
                  .append("<td>").append(t.getDate() != null ? t.getDate().format(DATE_FMT) : "").append("</td>")
                  .append("<td>").append(esc(t.getDescription())).append("</td>")
                  .append("<td>").append(esc(t.getCategory())).append("</td>")
                  .append("<td class=\"").append(cls).append("\">").append(t.getType()).append("</td>")
                  .append("<td class=\"").append(cls).append("\">₹").append(fmt(t.getAmount())).append("</td>")
                  .append("</tr>");
            });
            sb.append("</tbody></table>");
        }

        // Budgets
        if (!budgets.isEmpty()) {
            sb.append("<h2>Budget Utilisation</h2>");
            sb.append("<table><thead><tr><th>Category</th><th>Period</th><th>Limit</th><th>Spent</th><th>Usage</th><th>Status</th></tr></thead><tbody>");
            budgets.forEach(b -> {
                String badgeCls = "exceeded".equals(b.getAlertLevel()) ? "badge-exc"
                        : "warning".equals(b.getAlertLevel()) ? "badge-warn" : "badge-ok";
                String badgeLabel = "exceeded".equals(b.getAlertLevel()) ? "Exceeded"
                        : "warning".equals(b.getAlertLevel()) ? "Warning" : "On Track";
                sb.append("<tr>")
                  .append("<td>").append(esc(b.getCategory())).append("</td>")
                  .append("<td>").append(b.getPeriod()).append("</td>")
                  .append("<td>₹").append(fmt(b.getLimitAmount())).append("</td>")
                  .append("<td>₹").append(fmt(b.getSpent())).append("</td>")
                  .append("<td>").append(String.format("%.1f%%", b.getUtilisationPct())).append("</td>")
                  .append("<td><span class=\"badge ").append(badgeCls).append("\">").append(badgeLabel).append("</span></td>")
                  .append("</tr>");
            });
            sb.append("</tbody></table>");
        }

        // Goals
        if (!goals.isEmpty()) {
            sb.append("<h2>Savings Goals Progress</h2>");
            sb.append("<table><thead><tr><th>Goal</th><th>Target</th><th>Saved</th><th>Progress</th><th>Deadline</th><th>On Track</th></tr></thead><tbody>");
            goals.forEach(g -> {
                sb.append("<tr>")
                  .append("<td>").append(esc(g.getName())).append("</td>")
                  .append("<td>₹").append(fmt(g.getTargetAmount())).append("</td>")
                  .append("<td>₹").append(fmt(g.getSavedAmount())).append("</td>")
                  .append("<td>").append(String.format("%.1f%%", g.getProgressPct())).append("</td>")
                  .append("<td>").append(g.getDeadline() != null ? g.getDeadline().toString() : "—").append("</td>")
                  .append("<td>").append(Boolean.TRUE.equals(g.getOnTrack()) ? "✅ Yes" : "⚠️ No").append("</td>")
                  .append("</tr>");
            });
            sb.append("</tbody></table>");
        }

        // Health Score
        sb.append("<h2>Financial Health Score</h2>");
        String bandColor = health.getScore() >= 80 ? "#12b76a"
                : health.getScore() >= 60 ? "#444ce7"
                : health.getScore() >= 40 ? "#f59e0b" : "#f04438";
        sb.append("<div style=\"text-align:center;margin-bottom:20px;\">");
        sb.append("<div style=\"font-size:48px;font-weight:800;color:").append(bandColor).append(";\">")
          .append(health.getScore()).append("</div>");
        sb.append("<div class=\"band\" style=\"background:").append(bandColor).append("22;color:").append(bandColor).append(";\">")
          .append(health.getBand()).append("</div>");
        sb.append("<div style=\"font-size:12px;color:#666;margin-top:8px;\">").append(esc(health.getInsight())).append("</div>");
        sb.append("</div>");
        sb.append("<div class=\"health-grid\">");
        sb.append(healthBox("Savings Ratio", health.getSavingsRatioScore(), "#12b76a", "40% weight"));
        sb.append(healthBox("Budget Adherence", health.getBudgetAdherenceScore(), "#444ce7", "40% weight"));
        sb.append(healthBox("Spending Consistency", health.getSpendingConsistencyScore(), "#f59e0b", "20% weight"));
        sb.append("</div>");

        // Footer
        sb.append("<div class=\"footer\">Generated by FinSmart · ").append(LocalDate.now().format(DATE_FMT)).append("</div>");
        sb.append("</body></html>");

        return sb.toString();
    }

    // ── HTML helpers ──────────────────────────────────────────────────────────

    private String statBox(String label, String value, String cls) {
        return "<div class=\"stat-box\"><div class=\"label\">" + label + "</div>" +
               "<div class=\"value " + cls + "\">" + value + "</div></div>";
    }

    private String healthBox(String label, double score, String color, String sub) {
        double w = Math.min(score, 100);
        return "<div class=\"health-box\">" +
               "<div style=\"font-size:11px;color:#888;\">" + sub + "</div>" +
               "<h3>" + label + "</h3>" +
               "<div style=\"font-size:20px;font-weight:700;color:" + color + ";\">" +
                 String.format("%.0f", score) + "/100</div>" +
               "<div class=\"bar-wrap\"><div class=\"bar-fill\" style=\"width:" + w + "%;background:" + color + ";\"></div></div>" +
               "</div>";
    }

    private String fmt(double v) {
        return String.format("%,.0f", v);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

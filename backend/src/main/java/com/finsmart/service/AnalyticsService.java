package com.finsmart.service;

import com.finsmart.dto.response.AnalyticsResponse;
import com.finsmart.dto.response.InsightResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    private static final List<String> MONTH_ORDER = List.of(
            "Jan","Feb","Mar","Apr","May","Jun",
            "Jul","Aug","Sep","Oct","Nov","Dec"
    );

    private static final List<String> DOW_ORDER = List.of(
            "Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"
    );

    public AnalyticsResponse compute(String userId, int lookbackMonths) {
        LocalDate from = LocalDate.now().minusMonths(lookbackMonths).withDayOfMonth(1);
        List<Transaction> all = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, from, LocalDate.now());

        List<Transaction> expenses = all.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());
        List<Transaction> incomes = all.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        // ── 1. Category breakdown ──────────────────────────────────────────────
        Map<String, Double> catExpenses = groupByCategory(expenses);
        Map<String, Double> catIncome   = groupByCategory(incomes);

        // ── 2. Month-over-month comparison ────────────────────────────────────
        Map<String, Double> monthlyExpense = groupByMonth(expenses);
        Map<String, Double> monthlyIncome  = groupByMonth(incomes);

        Set<String> allMonths = new LinkedHashSet<>();
        allMonths.addAll(monthlyExpense.keySet());
        allMonths.addAll(monthlyIncome.keySet());

        List<Map<String, Object>> monthly = allMonths.stream()
                .sorted(Comparator.comparingInt(m -> MONTH_ORDER.indexOf(m)))
                .map(m -> {
                    double exp = monthlyExpense.getOrDefault(m, 0.0);
                    double inc = monthlyIncome.getOrDefault(m, 0.0);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month",   m);
                    row.put("income",  round2(inc));
                    row.put("expense", round2(exp));
                    row.put("savings", round2(inc - exp));
                    return row;
                })
                .collect(Collectors.toList());

        // ── 3. Day-of-week heatmap ────────────────────────────────────────────
        Map<String, Double> dowMap = new LinkedHashMap<>();
        for (String d : DOW_ORDER) dowMap.put(d, 0.0);
        for (Transaction t : expenses) {
            if (t.getDate() == null) continue;
            String dow = t.getDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            dowMap.merge(dow, t.getAmount(), Double::sum);
        }

        // ── 4. Weekend vs weekday split ───────────────────────────────────────
        double totalExpense = expenses.stream().mapToDouble(Transaction::getAmount).sum();
        double weekendTotal = expenses.stream()
                .filter(t -> t.getDate() != null && isWeekend(t.getDate()))
                .mapToDouble(Transaction::getAmount).sum();
        double weekdayTotal = totalExpense - weekendTotal;

        double weekendPct = totalExpense > 0 ? round2((weekendTotal / totalExpense) * 100) : 0;
        double weekdayPct = totalExpense > 0 ? round2((weekdayTotal / totalExpense) * 100) : 0;

        // ── 5. Top spending days ──────────────────────────────────────────────
        Map<LocalDate, Double> dailyTotals = new LinkedHashMap<>();
        for (Transaction t : expenses) {
            if (t.getDate() != null) dailyTotals.merge(t.getDate(), t.getAmount(), Double::sum);
        }
        List<Map<String, Object>> topDays = dailyTotals.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date",  e.getKey().toString());
                    m.put("total", round2(e.getValue()));
                    return m;
                })
                .collect(Collectors.toList());

        // ── 6. Smart Insights Engine ──────────────────────────────────────────
        List<InsightResponse> insights = generateInsights(
                expenses, catExpenses, monthlyExpense, weekendPct, totalExpense, all
        );

        return AnalyticsResponse.builder()
                .categoryExpenses(sortByValueDesc(catExpenses))
                .categoryIncome(sortByValueDesc(catIncome))
                .monthlyComparison(monthly)
                .dayOfWeekExpenses(dowMap)
                .weekendExpensePct(weekendPct)
                .weekdayExpensePct(weekdayPct)
                .topSpendingDays(topDays)
                .insights(insights)
                .build();
    }

    // ── Smart Insights ────────────────────────────────────────────────────────

    private List<InsightResponse> generateInsights(
            List<Transaction> expenses,
            Map<String, Double> catExpenses,
            Map<String, Double> monthlyExpense,
            double weekendPct,
            double totalExpense,
            List<Transaction> allTxns
    ) {
        List<InsightResponse> insights = new ArrayList<>();

        LocalDate now        = LocalDate.now();
        LocalDate thisMonth  = now.withDayOfMonth(1);
        LocalDate lastMonth  = thisMonth.minusMonths(1);
        String    thisKey    = abbr(now);
        String    lastKey    = abbr(lastMonth.withDayOfMonth(1));

        // ── Rule 1: Category month-over-month spike ───────────────────────────
        Map<String, Double> thisMonthCat = groupByCategory(
                expenses.stream()
                        .filter(t -> t.getDate() != null && !t.getDate().isBefore(thisMonth))
                        .collect(Collectors.toList())
        );
        Map<String, Double> lastMonthCat = groupByCategory(
                expenses.stream()
                        .filter(t -> t.getDate() != null
                                && !t.getDate().isBefore(lastMonth.withDayOfMonth(1))
                                && t.getDate().isBefore(thisMonth))
                        .collect(Collectors.toList())
        );

        for (Map.Entry<String, Double> e : thisMonthCat.entrySet()) {
            String cat = e.getKey();
            double curr = e.getValue();
            double prev = lastMonthCat.getOrDefault(cat, 0.0);
            if (prev > 0) {
                double changePct = ((curr - prev) / prev) * 100;
                if (changePct >= 30) {
                    insights.add(InsightResponse.builder()
                            .type("warning")
                            .title(cat + " spending spiked")
                            .body(String.format("%s spending is up %.0f%% this month (₹%.0f vs ₹%.0f last month).",
                                    cat, changePct, curr, prev))
                            .category(cat)
                            .value(round2(changePct))
                            .build());
                } else if (changePct <= -25) {
                    insights.add(InsightResponse.builder()
                            .type("positive")
                            .title(cat + " spending reduced")
                            .body(String.format("Great job! %s spending dropped %.0f%% vs last month.",
                                    cat, Math.abs(changePct)))
                            .category(cat)
                            .value(round2(changePct))
                            .build());
                }
            }
        }

        // ── Rule 2: Weekend spending ──────────────────────────────────────────
        if (weekendPct >= 40) {
            insights.add(InsightResponse.builder()
                    .type("info")
                    .title("Weekend spending is high")
                    .body(String.format("%.0f%% of your total expenses occur on weekends. " +
                            "Consider setting a weekend budget.", weekendPct))
                    .value(weekendPct)
                    .build());
        }

        // ── Rule 3: Top category dominance ───────────────────────────────────
        catExpenses.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(top -> {
                    if (totalExpense > 0) {
                        double pct = (top.getValue() / totalExpense) * 100;
                        if (pct >= 40) {
                            insights.add(InsightResponse.builder()
                                    .type("info")
                                    .title(top.getKey() + " dominates spending")
                                    .body(String.format("%s accounts for %.0f%% of all expenses. " +
                                            "It might be worth reviewing.", top.getKey(), pct))
                                    .category(top.getKey())
                                    .value(round2(pct))
                                    .build());
                        }
                    }
                });

        // ── Rule 4: Month-over-month total expense change ─────────────────────
        double thisTotal = monthlyExpense.getOrDefault(thisKey, 0.0);
        double lastTotal = monthlyExpense.getOrDefault(lastKey, 0.0);
        if (lastTotal > 0 && thisTotal > 0) {
            double change = ((thisTotal - lastTotal) / lastTotal) * 100;
            if (change >= 20) {
                insights.add(InsightResponse.builder()
                        .type("warning")
                        .title("Overall spending up this month")
                        .body(String.format("Total expenses are %.0f%% higher than last month " +
                                "(₹%.0f vs ₹%.0f).", change, thisTotal, lastTotal))
                        .value(round2(change))
                        .build());
            } else if (change <= -15) {
                insights.add(InsightResponse.builder()
                        .type("positive")
                        .title("Spending down this month 🎉")
                        .body(String.format("You spent %.0f%% less this month compared to last. Keep it up!",
                                Math.abs(change)))
                        .value(round2(change))
                        .build());
            }
        }

        // ── Rule 5: Savings rate ──────────────────────────────────────────────
        double thisIncome = allTxns.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType())
                          && t.getDate() != null && !t.getDate().isBefore(thisMonth))
                .mapToDouble(Transaction::getAmount).sum();

        if (thisIncome > 0 && thisTotal > 0) {
            double savingsPct = ((thisIncome - thisTotal) / thisIncome) * 100;
            if (savingsPct < 10) {
                insights.add(InsightResponse.builder()
                        .type("warning")
                        .title("Low savings rate this month")
                        .body(String.format("You're saving only %.0f%% of income this month. " +
                                "Aim for at least 20%%.", savingsPct))
                        .value(round2(savingsPct))
                        .build());
            } else if (savingsPct >= 30) {
                insights.add(InsightResponse.builder()
                        .type("positive")
                        .title("Excellent savings rate!")
                        .body(String.format("You've saved %.0f%% of your income this month. Excellent discipline!",
                                savingsPct))
                        .value(round2(savingsPct))
                        .build());
            }
        }

        // ── Rule 6: No-spend streak ───────────────────────────────────────────
        int streak = computeNoSpendStreak(expenses);
        if (streak >= 3) {
            insights.add(InsightResponse.builder()
                    .type("positive")
                    .title(streak + "-day no-spend streak!")
                    .body(String.format("You haven't logged any expenses for %d consecutive days. " +
                            "That's a great saving streak!", streak))
                    .value((double) streak)
                    .build());
        }

        return insights;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int computeNoSpendStreak(List<Transaction> expenses) {
        if (expenses.isEmpty()) return 0;
        Set<LocalDate> expenseDates = expenses.stream()
                .map(Transaction::getDate).filter(Objects::nonNull).collect(Collectors.toSet());
        int streak = 0;
        LocalDate day = LocalDate.now().minusDays(1); // start from yesterday
        while (!expenseDates.contains(day)) {
            streak++;
            day = day.minusDays(1);
            if (streak > 365) break; // safety cap
        }
        return streak;
    }

    private Map<String, Double> groupByCategory(List<Transaction> txns) {
        return txns.stream().collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory() : "Other",
                Collectors.summingDouble(Transaction::getAmount)
        ));
    }

    private Map<String, Double> groupByMonth(List<Transaction> txns) {
        return txns.stream()
                .filter(t -> t.getDate() != null)
                .collect(Collectors.groupingBy(
                        t -> abbr(t.getDate()),
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    private String abbr(LocalDate d) {
        return d.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
    }

    private boolean isWeekend(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private Map<String, Double> sortByValueDesc(Map<String, Double> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> round2(e.getValue()),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}

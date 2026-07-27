package com.finsmart.service;

import com.finsmart.dto.response.AnalyticsResponse;
import com.finsmart.dto.response.InsightResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock  TransactionRepository transactionRepository;
    @InjectMocks AnalyticsService service;

    private Transaction txn(String type, String category, double amount, LocalDate date) {
        return Transaction.builder()
                .userId("u1").type(type).category(category)
                .amount(amount).date(date).build();
    }

    @Test
    void returnsEmptyForNoData() {
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(List.of());

        AnalyticsResponse r = service.compute("u1", 6);
        assertThat(r.getCategoryExpenses()).isEmpty();
        assertThat(r.getMonthlyComparison()).isEmpty();
        assertThat(r.getInsights()).isEmpty();
    }

    @Test
    void groupsCategoryExpensesCorrectly() {
        List<Transaction> txns = List.of(
                txn("expense", "Food",    2000, LocalDate.now().minusDays(5)),
                txn("expense", "Food",    1000, LocalDate.now().minusDays(10)),
                txn("expense", "Travel",  3000, LocalDate.now().minusDays(3)),
                txn("income",  "Salary", 50000, LocalDate.now().minusDays(1))
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        AnalyticsResponse r = service.compute("u1", 6);
        assertThat(r.getCategoryExpenses()).containsEntry("Food", 3000.0);
        assertThat(r.getCategoryExpenses()).containsEntry("Travel", 3000.0);
        assertThat(r.getCategoryIncome()).containsEntry("Salary", 50000.0);
    }

    @Test
    void detectsWeekendSpend() {
        // Use a known Saturday
        LocalDate saturday = LocalDate.now();
        while (saturday.getDayOfWeek().getValue() != 6) saturday = saturday.minusDays(1);

        List<Transaction> txns = List.of(
                txn("expense", "Shopping", 8000, saturday),
                txn("expense", "Food",     2000, saturday.minusDays(1)) // Friday weekday
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        AnalyticsResponse r = service.compute("u1", 6);
        assertThat(r.getWeekendExpensePct()).isGreaterThan(0);
    }

    @Test
    void generatesCategorySpikInsight() {
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastMonth = thisMonth.minusMonths(1);

        List<Transaction> txns = new ArrayList<>();
        // Last month: Food = 1000
        txns.add(txn("expense", "Food", 1000, lastMonth.plusDays(5)));
        // This month: Food = 2000 (100% increase → should trigger warning insight)
        txns.add(txn("expense", "Food", 2000, thisMonth.plusDays(5)));

        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        AnalyticsResponse r = service.compute("u1", 6);
        List<InsightResponse> warnings = r.getInsights().stream()
                .filter(i -> "warning".equals(i.getType()))
                .toList();
        assertThat(warnings).anyMatch(i -> i.getCategory() != null && i.getCategory().equals("Food"));
    }

    @Test
    void buildsMonthlyComparisonRows() {
        LocalDate now = LocalDate.now();
        List<Transaction> txns = List.of(
                txn("income",  "Salary", 50000, now.minusDays(5)),
                txn("expense", "Food",    3000, now.minusDays(3))
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        AnalyticsResponse r = service.compute("u1", 6);
        assertThat(r.getMonthlyComparison()).isNotEmpty();
        // Each row must have month, income, expense, savings keys
        r.getMonthlyComparison().forEach(row -> {
            assertThat(row).containsKeys("month", "income", "expense", "savings");
        });
    }
}

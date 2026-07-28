package com.finsmart.service;

import com.finsmart.dto.request.TransactionRequest;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private final String USER_ID = "user123";

    private TransactionRequest buildRequest(Double amount, String type, String category, String desc) {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(amount);
        req.setType(type);
        req.setCategory(category);
        req.setDescription(desc);
        req.setDate(LocalDate.of(2026, 4, 15));
        return req;
    }

    // ── autoCategory() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("autoCategory: swiggy description → Food")
    void autoCategory_swiggy_returnsFood() {
        assertThat(transactionService.autoCategory("Swiggy order lunch")).isEqualTo("Food");
    }

    @Test
    @DisplayName("autoCategory: uber description → Travel")
    void autoCategory_uber_returnsTravel() {
        assertThat(transactionService.autoCategory("Uber ride to office")).isEqualTo("Travel");
    }

    @Test
    @DisplayName("autoCategory: netflix description → Entertainment")
    void autoCategory_netflix_returnsEntertainment() {
        assertThat(transactionService.autoCategory("Netflix monthly subscription")).isEqualTo("Entertainment");
    }

    @Test
    @DisplayName("autoCategory: unknown description → Other")
    void autoCategory_unknown_returnsOther() {
        assertThat(transactionService.autoCategory("Random payment xyz")).isEqualTo("Other");
    }

    @Test
    @DisplayName("autoCategory: null description → Other")
    void autoCategory_null_returnsOther() {
        assertThat(transactionService.autoCategory(null)).isEqualTo("Other");
    }

    @Test
    @DisplayName("autoCategory: blank description → Other")
    void autoCategory_blank_returnsOther() {
        assertThat(transactionService.autoCategory("   ")).isEqualTo("Other");
    }

    @Test
    @DisplayName("autoCategory: case-insensitive — SWIGGY → Food")
    void autoCategory_caseInsensitive() {
        assertThat(transactionService.autoCategory("SWIGGY ORDER")).isEqualTo("Food");
    }

    // ── addTransaction() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("addTransaction: with auto-category when category is blank")
    void addTransaction_withAutoCategory() {
        TransactionRequest req = buildRequest(500.0, "expense", "", "Swiggy dinner");

        Transaction saved = Transaction.builder()
                .id("txn1").userId(USER_ID).amount(500.0)
                .type("expense").category("Food")
                .description("Swiggy dinner").date(LocalDate.of(2026, 4, 15))
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        Transaction result = transactionService.addTransaction(USER_ID, req);

        assertThat(result.getCategory()).isEqualTo("Food");
        verify(transactionRepository).save(argThat(t ->
                t.getCategory().equals("Food") && t.getUserId().equals(USER_ID)
        ));
    }

    @Test
    @DisplayName("addTransaction: with manual category — skips auto-detect")
    void addTransaction_withManualCategory_skipsAutoDetect() {
        TransactionRequest req = buildRequest(1000.0, "expense", "Bills", "Swiggy");
        // Even though description contains 'swiggy', manual category 'Bills' wins

        Transaction saved = Transaction.builder()
                .id("txn2").userId(USER_ID).amount(1000.0)
                .type("expense").category("Bills")
                .description("Swiggy").date(LocalDate.of(2026, 4, 15))
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        Transaction result = transactionService.addTransaction(USER_ID, req);

        assertThat(result.getCategory()).isEqualTo("Bills");
        verify(transactionRepository).save(argThat(t -> t.getCategory().equals("Bills")));
    }

    @Test
    @DisplayName("addTransaction: null date defaults to today")
    void addTransaction_nullDate_defaultsToToday() {
        TransactionRequest req = buildRequest(200.0, "expense", "Food", "lunch");
        req.setDate(null);  // explicitly null

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.addTransaction(USER_ID, req);

        verify(transactionRepository).save(argThat(t ->
                t.getDate() != null && t.getDate().equals(LocalDate.now())
        ));
    }

    // ── getTransaction() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getTransaction: not owned — throws RuntimeException")
    void getTransaction_notOwned_throws() {
        Transaction other = Transaction.builder()
                .id("txn1").userId("other_user").build();

        when(transactionRepository.findById("txn1")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> transactionService.getTransaction(USER_ID, "txn1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    @DisplayName("getTransaction: not found — throws RuntimeException")
    void getTransaction_notFound_throws() {
        when(transactionRepository.findById("bad_id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(USER_ID, "bad_id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction not found");
    }

    // ── deleteTransaction() ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTransaction: not found — throws RuntimeException")
    void deleteTransaction_notFound_throws() {
        when(transactionRepository.existsByIdAndUserId("bad_id", USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.deleteTransaction(USER_ID, "bad_id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction not found");

        verify(transactionRepository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("deleteTransaction: success — calls deleteByIdAndUserId")
    void deleteTransaction_success() {
        when(transactionRepository.existsByIdAndUserId("txn1", USER_ID)).thenReturn(true);

        transactionService.deleteTransaction(USER_ID, "txn1");

        verify(transactionRepository).deleteByIdAndUserId("txn1", USER_ID);
    }

    // ── getDashboardSummary() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboardSummary: correctly aggregates income, expense, balance")
    void getDashboardSummary_correctAggregation() {
        List<Transaction> txns = List.of(
                Transaction.builder().userId(USER_ID).type("income").amount(50000.0)
                        .category("Salary").date(LocalDate.now()).build(),
                Transaction.builder().userId(USER_ID).type("expense").amount(1200.0)
                        .category("Food").date(LocalDate.now()).build(),
                Transaction.builder().userId(USER_ID).type("expense").amount(800.0)
                        .category("Travel").date(LocalDate.now()).build()
        );

        when(transactionRepository.findByUserIdOrderByDateDesc(USER_ID)).thenReturn(txns);

        Map<String, Object> summary = transactionService.getDashboardSummary(USER_ID);

        assertThat(summary.get("totalIncome")).isEqualTo(50000.0);
        assertThat(summary.get("totalExpense")).isEqualTo(2000.0);
        assertThat(summary.get("balance")).isEqualTo(48000.0);
        assertThat(summary.get("transactionCount")).isEqualTo(3);

        @SuppressWarnings("unchecked")
        Map<String, Double> breakdown = (Map<String, Double>) summary.get("categoryBreakdown");
        assertThat(breakdown.get("Food")).isEqualTo(1200.0);
        assertThat(breakdown.get("Travel")).isEqualTo(800.0);
        // Income category should NOT appear in expense breakdown
        assertThat(breakdown).doesNotContainKey("Salary");
    }

    @Test
    @DisplayName("getDashboardSummary: no transactions — all zeros, empty collections")
    void getDashboardSummary_noTransactions_returnsZeros() {
        when(transactionRepository.findByUserIdOrderByDateDesc(USER_ID)).thenReturn(List.of());

        Map<String, Object> summary = transactionService.getDashboardSummary(USER_ID);

        assertThat(summary.get("totalIncome")).isEqualTo(0.0);
        assertThat(summary.get("totalExpense")).isEqualTo(0.0);
        assertThat(summary.get("balance")).isEqualTo(0.0);
        assertThat(summary.get("transactionCount")).isEqualTo(0);
    }
}

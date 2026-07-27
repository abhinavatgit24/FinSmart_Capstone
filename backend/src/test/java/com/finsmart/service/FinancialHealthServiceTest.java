package com.finsmart.service;

import com.finsmart.dto.response.FinancialHealthResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialHealthServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock BudgetService budgetService;

    @Test
    void returnsNoDataInsteadOfAnArtificialPoorScore() {
        when(transactionRepository.findByUserIdOrderByDateDesc("u1")).thenReturn(List.of());

        FinancialHealthResponse result = new FinancialHealthService(transactionRepository, budgetService).compute("u1");

        assertThat(result.getScore()).isNull();
        assertThat(result.getBand()).isEqualTo("N/A");
    }

    @Test
    void usesOverallTransactionHistoryInsteadOfOnlyTheCurrentMonth() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        Transaction income = Transaction.builder().type("income").amount(10_000.0).date(lastMonth).build();
        Transaction expense = Transaction.builder().type("expense").amount(2_000.0).date(lastMonth.plusDays(1)).build();
        when(transactionRepository.findByUserIdOrderByDateDesc("u1"))
                .thenReturn(List.of(income, expense));
        when(budgetService.getUtilisation("u1")).thenReturn(List.of());

        FinancialHealthResponse result = new FinancialHealthService(transactionRepository, budgetService).compute("u1");

        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSavingsRatioScore()).isEqualTo(100.0);
    }
}

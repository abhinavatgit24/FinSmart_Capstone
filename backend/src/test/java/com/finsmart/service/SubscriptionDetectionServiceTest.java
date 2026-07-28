package com.finsmart.service;

import com.finsmart.dto.response.SubscriptionResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionDetectionServiceTest {

    @Mock  TransactionRepository transactionRepository;
    @InjectMocks SubscriptionDetectionService service;

    private Transaction expense(String desc, double amount, LocalDate date) {
        return Transaction.builder()
                .userId("u1").type("expense").description(desc)
                .amount(amount).category("Entertainment").date(date).build();
    }

    @Test
    void detectsMonthlySubscription() {
        LocalDate base = LocalDate.now().minusMonths(3);
        List<Transaction> txns = List.of(
                expense("Netflix subscription", 649, base),
                expense("Netflix subscription", 649, base.plusMonths(1)),
                expense("Netflix subscription", 649, base.plusMonths(2))
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        List<SubscriptionResponse> result = service.detect("u1");

        assertThat(result).isNotEmpty();
        SubscriptionResponse sub = result.get(0);
        assertThat(sub.getMerchant()).containsIgnoringCase("netflix");
        assertThat(sub.getFrequency()).isEqualTo("monthly");
        assertThat(sub.getOccurrences()).isEqualTo(3);
        assertThat(sub.getAnnualisedCost()).isEqualTo(649 * 12);
    }

    @Test
    void ignoresSingleOccurrence() {
        List<Transaction> txns = List.of(
                expense("One-time purchase", 999, LocalDate.now().minusDays(10))
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        assertThat(service.detect("u1")).isEmpty();
    }

    @Test
    void ignoresSameMonthDuplicates() {
        // Two transactions same merchant same month — should NOT be flagged as subscription
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        List<Transaction> txns = List.of(
                expense("Spotify premium", 119, thisMonth.plusDays(1)),
                expense("Spotify premium", 119, thisMonth.plusDays(15))
        );
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(txns);

        assertThat(service.detect("u1")).isEmpty();
    }

    @Test
    void returnsEmptyForNoTransactions() {
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(List.of());

        assertThat(service.detect("u1")).isEmpty();
    }
}

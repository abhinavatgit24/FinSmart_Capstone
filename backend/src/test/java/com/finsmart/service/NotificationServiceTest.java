package com.finsmart.service;

import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.Notification;
import com.finsmart.repository.NotificationRepository;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock TransactionRepository  transactionRepository;
    @Mock BudgetService          budgetService;
    @Mock SavingsGoalService     goalService;
    @InjectMocks NotificationService service;

    @Test
    void createsBudgetWarningNotification() {
        BudgetUtilisationResponse u = BudgetUtilisationResponse.builder()
                .id("b1").category("Food").limitAmount(5000.0)
                .spent(4200.0).utilisationPct(84.0).alertLevel("warning").period("monthly").build();

        when(budgetService.getUtilisation("u1")).thenReturn(List.of(u));
        when(goalService.getAllWithPrediction("u1")).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(notificationRepository.existsByUserIdAndRefIdAndType(any(), any(), any())).thenReturn(false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndReadFalse("u1")).thenReturn(List.of());

        service.getAll("u1");

        verify(notificationRepository).save(argThat(n ->
                "budget_warning".equals(n.getType()) && "b1".equals(n.getRefId())
        ));
    }

    @Test
    void createsBudgetExceededNotification() {
        BudgetUtilisationResponse u = BudgetUtilisationResponse.builder()
                .id("b2").category("Shopping").limitAmount(3000.0)
                .spent(3500.0).utilisationPct(116.0).alertLevel("exceeded").period("monthly").build();

        when(budgetService.getUtilisation("u1")).thenReturn(List.of(u));
        when(goalService.getAllWithPrediction("u1")).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(notificationRepository.existsByUserIdAndRefIdAndType(any(), any(), any())).thenReturn(false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndReadFalse("u1")).thenReturn(List.of());

        service.getAll("u1");

        verify(notificationRepository).save(argThat(n ->
                "budget_exceeded".equals(n.getType())
        ));
    }

    @Test
    void doesNotDuplicateExistingNotification() {
        BudgetUtilisationResponse u = BudgetUtilisationResponse.builder()
                .id("b1").category("Food").limitAmount(5000.0)
                .spent(4200.0).utilisationPct(84.0).alertLevel("warning").period("monthly").build();

        when(budgetService.getUtilisation("u1")).thenReturn(List.of(u));
        when(goalService.getAllWithPrediction("u1")).thenReturn(List.of());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any()))
                .thenReturn(List.of());
        // Already exists
        when(notificationRepository.existsByUserIdAndRefIdAndType(eq("u1"), eq("b1"), eq("budget_warning")))
                .thenReturn(true);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("u1")).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndReadFalse("u1")).thenReturn(List.of());

        service.getAll("u1");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markReadUpdatesFlag() {
        Notification n = Notification.builder().id("n1").userId("u1").read(false).build();
        when(notificationRepository.findById("n1")).thenReturn(java.util.Optional.of(n));

        service.markRead("u1", "n1");

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllReadUpdatesAllUnread() {
        Notification n1 = Notification.builder().id("n1").userId("u1").read(false).build();
        Notification n2 = Notification.builder().id("n2").userId("u1").read(false).build();
        when(notificationRepository.findByUserIdAndReadFalse("u1")).thenReturn(List.of(n1, n2));

        service.markAllRead("u1");

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }
}

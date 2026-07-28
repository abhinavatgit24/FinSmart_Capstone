package com.finsmart.controller;

import com.finsmart.dto.response.ApiResponse;
import com.finsmart.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;

    /**
     * GET /api/dashboard/summary
     * Returns: totalIncome, totalExpense, balance,
     *          categoryBreakdown, recentTransactions,
     *          monthlyTrend, transactionCount
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @AuthenticationPrincipal UserDetails ud) {
        Map<String, Object> summary = transactionService.getDashboardSummary(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Dashboard fetched", summary));
    }
}

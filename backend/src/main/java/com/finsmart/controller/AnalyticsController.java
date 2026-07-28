package com.finsmart.controller;

import com.finsmart.dto.response.AnalyticsResponse;
import com.finsmart.dto.response.ApiResponse;
import com.finsmart.dto.response.SubscriptionResponse;
import com.finsmart.service.AnalyticsService;
import com.finsmart.service.SubscriptionDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService            analyticsService;
    private final SubscriptionDetectionService subscriptionService;

    /**
     * GET /api/analytics?months=6
     * Returns full analytics payload: charts, heatmap, insights.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(value = "months", defaultValue = "6") int months) {

        int capped = Math.min(Math.max(months, 1), 24);
        return ResponseEntity.ok(ApiResponse.ok("Analytics computed",
                analyticsService.compute(ud.getUsername(), capped)));
    }

    /**
     * GET /api/analytics/subscriptions
     * Detects recurring transactions in the last 6 months.
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getSubscriptions(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Subscriptions detected",
                subscriptionService.detect(ud.getUsername())));
    }
}

package com.finsmart.controller;

import com.finsmart.dto.response.ApiResponse;
import com.finsmart.dto.response.FinancialHealthResponse;
import com.finsmart.service.FinancialHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class FinancialHealthController {

    private final FinancialHealthService healthService;

    /**
     * GET /api/health/score
     * Returns the user's Financial Health Score (0–100) with band and component breakdown.
     */
    @GetMapping("/score")
    public ResponseEntity<ApiResponse<FinancialHealthResponse>> getScore(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Health score computed",
                healthService.compute(ud.getUsername())));
    }
}

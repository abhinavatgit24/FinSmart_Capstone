package com.finsmart.controller;

import com.finsmart.dto.request.BudgetRequest;
import com.finsmart.dto.response.ApiResponse;
import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.model.Budget;
import com.finsmart.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /** Create or update a budget (upserts by category+period) */
    @PostMapping
    public ResponseEntity<ApiResponse<Budget>> createOrUpdate(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody BudgetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Budget saved",
                        budgetService.createOrUpdate(ud.getUsername(), req)));
    }

    /** List all budgets (raw, no utilisation) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Budget>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Budgets fetched",
                budgetService.getAll(ud.getUsername())));
    }

    /** List all budgets WITH real-time utilisation + alert levels */
    @GetMapping("/utilisation")
    public ResponseEntity<ApiResponse<List<BudgetUtilisationResponse>>> getUtilisation(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisation fetched",
                budgetService.getUtilisation(ud.getUsername())));
    }

    /** Single budget utilisation */
    @GetMapping("/{id}/utilisation")
    public ResponseEntity<ApiResponse<BudgetUtilisationResponse>> getBudgetUtilisation(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                budgetService.getUtilisationForBudget(ud.getUsername(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        budgetService.delete(ud.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Budget deleted", null));
    }
}

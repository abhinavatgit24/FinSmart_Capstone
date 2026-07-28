package com.finsmart.controller;

import com.finsmart.dto.request.SavingsGoalRequest;
import com.finsmart.dto.response.ApiResponse;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.SavingsGoal;
import com.finsmart.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoal>> create(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody SavingsGoalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Goal created",
                        goalService.create(ud.getUsername(), req)));
    }

    /** All goals with prediction & progress */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok("Goals fetched",
                goalService.getAllWithPrediction(ud.getUsername())));
    }

    /** Single goal with prediction */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoalResponse>> getOne(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                goalService.getWithPrediction(ud.getUsername(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> update(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id,
            @Valid @RequestBody SavingsGoalRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Goal updated",
                goalService.update(ud.getUsername(), id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        goalService.delete(ud.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Goal deleted", null));
    }
}

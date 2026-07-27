package com.finsmart.controller;

import com.finsmart.dto.request.TransactionRequest;
import com.finsmart.dto.response.ApiResponse;
import com.finsmart.model.Transaction;
import com.finsmart.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> add(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction added",
                        transactionService.addTransaction(ud.getUsername(), req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Transaction>>> getAll(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String uid = ud.getUsername();
        List<Transaction> result;

        if (from != null && to != null)         result = transactionService.getByDateRange(uid, from, to);
        else if (category != null)              result = transactionService.getByCategory(uid, category);
        else if (type != null)                  result = transactionService.getByType(uid, type);
        else                                    result = transactionService.getAllTransactions(uid);

        return ResponseEntity.ok(ApiResponse.ok("Transactions fetched", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getOne(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("OK",
                transactionService.getTransaction(ud.getUsername(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> update(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id,
            @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Transaction updated",
                transactionService.updateTransaction(ud.getUsername(), id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable String id) {
        transactionService.deleteTransaction(ud.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction deleted", null));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok("OK", transactionService.getCategories()));
    }
}
